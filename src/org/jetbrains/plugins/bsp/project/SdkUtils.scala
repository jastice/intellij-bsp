package org.jetbrains.plugins.bsp.project

import com.intellij.openapi.projectRoots.{JavaSdk, ProjectJdkTable, Sdk}
import com.intellij.openapi.util.io.FileUtil
import com.intellij.pom.java.LanguageLevel
import org.jetbrains.plugins.bsp.util.extensions.inReadAction

import scala.jdk.CollectionConverters._

object SdkUtils {

  def findProjectSdk(sdkRef: SdkReference) =
    sdkRef match {
      case JdkByVersion(version) => findMostRecentJdk(sdk => Option(sdk.getVersionString).exists(_.contains(version)))
      case JdkByName(version)    => findMostRecentJdk(_.getName == version).orElse(findMostRecentJdk(_.getName.contains(version)))
      case JdkByHome(homeFile)   => findMostRecentJdk(sdk => FileUtil.comparePaths(homeFile.getCanonicalPath, sdk.getHomePath) == 0)
      case _                     => None
    }

  private def findMostRecentJdk(condition: Sdk => Boolean): Option[Sdk] = {
    import scala.math.Ordering.comparatorToOrdering
    val sdkType = JavaSdk.getInstance()

    inReadAction {
      val jdks = ProjectJdkTable.getInstance()
        .getSdksOfType(JavaSdk.getInstance())
        .asScala
        .filter(condition)

      if (jdks.isEmpty) None
      else Option(jdks.max(comparatorToOrdering(sdkType.versionComparator())))
    }
  }

  def mostRecentJdk: Option[Sdk] =
    findMostRecentJdk(_ => true)

  def defaultJavaLanguageLevelIn(jdk: Sdk): Option[LanguageLevel] =
    Option(LanguageLevel.parse(jdk.getVersionString))

  def javaLanguageLevelFrom(javacOptions: collection.Seq[String]): Option[LanguageLevel] = {
    for {
      sourcePos <- Option(javacOptions.indexOf("-source")).filterNot(_ == -1)
      sourceValue <- javacOptions.lift(sourcePos + 1)
      languageLevel <- Option(LanguageLevel.parse(sourceValue))
    } yield languageLevel
  }
}
