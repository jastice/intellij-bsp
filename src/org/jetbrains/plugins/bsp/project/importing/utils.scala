package org.jetbrains.plugins.bsp.project.importing

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.bsp.util.Version
import org.jetbrains.plugins.bsp.util.extensions.VirtualFileExt

import java.io.{BufferedInputStream, File, FileInputStream}
import java.util.Properties
import java.util.jar.JarFile
import scala.util.Using

object utils {

  object sbt {
    val Latest_0_12: Version = Version("0.12.4")
    val Latest_0_13: Version = Version("0.13.18")
    val Latest_1_0: Version = Version("1.4.5")
    val LatestVersion: Version = Latest_1_0

    val ProjectDirectory = "project"
    val PropertiesFile = "build.properties"
    val SbtFileExtension = "sbt"

    def canImport(file: VirtualFile): Boolean = file match {
      case null => false
      case directory if directory.isDirectory =>
        directory.getName == ProjectDirectory ||
          containsSbtProjectDirectory(directory) ||
          containsSbtBuildFile(directory)
      case _ => isSbtFile(file)
    }

    private def containsSbtProjectDirectory(directory: VirtualFile) =
      directory.findChild(ProjectDirectory) match {
        case null => false
        case projectDirectory =>
          projectDirectory.containsFile(PropertiesFile) ||
            containsSbtBuildFile(projectDirectory)
      }



    def detectSbtVersion(directory: File, sbtLauncher: => File): Version =
      sbtVersionIn(directory)
        .orElse(sbtVersionInBootPropertiesOf(sbtLauncher))
        .orElse(readManifestAttributeFrom(sbtLauncher, "Implementation-Version"))
        .map(Version.apply)
        .getOrElse(LatestVersion)

    def isSbtFile(file: VirtualFile): Boolean =
      file.getExtension == SbtFileExtension

    def sbtBuildPropertiesFile(base: File): File =
      new File(new File(base, ProjectDirectory), PropertiesFile)

    def canUpgradeSbtVersion(sbtVersion: Version): Boolean =
      sbtVersion >= MayUpgradeSbtVersion &&
        sbtVersion < latestCompatibleVersion(sbtVersion)

    def upgradedSbtVersion(sbtVersion: Version): Version =
      if (canUpgradeSbtVersion(sbtVersion))
        latestCompatibleVersion(sbtVersion)
      else sbtVersion

    def latestCompatibleVersion(version: Version): Version = {
      val major = version.major(2)

      val latestInSeries =
        if (major.inRange(Version("0.12"), Version("0.13"))) Latest_0_12
        else if (major.inRange(Version("0.13"), Version("1.0"))) Latest_0_13
        else if (major.inRange(Version("1.0"), Version("2.0"))) Latest_1_0
        else LatestVersion // needs to be updated for sbt versions >= 2.0

      if (version < latestInSeries) latestInSeries
      else version
    }

    def getLauncher: File = ??? // FIXME implement getting a launcher. download on demand via coursier?

    private val MayUpgradeSbtVersion = Version("0.13.0")

    private def sbtVersionIn(directory: File): Option[String] =
      sbtBuildPropertiesFile(directory) match {
        case propertiesFile if propertiesFile.exists => readPropertyFrom(propertiesFile, "sbt.version")
        case _ => None
      }

    private def containsSbtBuildFile(directory: VirtualFile) =
      directory.getChildren.exists(isSbtFile)

    private def readManifestAttributeFrom(file: File, name: String): Option[String] = {
      val jar = new JarFile(file)
      try {
        Option(jar.getJarEntry("META-INF/MANIFEST.MF")).flatMap { entry =>
          val input = new BufferedInputStream(jar.getInputStream(entry))
          val manifest = new java.util.jar.Manifest(input)
          val attributes = manifest.getMainAttributes
          Option(attributes.getValue(name))
        }
      }
      finally {
        jar.close()
      }
    }

    private def sbtVersionInBootPropertiesOf(jar: File): Option[String] = {
      val appProperties = readSectionFromBootPropertiesOf(jar, sectionName = "app")
      for {
        name <- appProperties.get("name")
        if name == "sbt"
        versionStr <- appProperties.get("version")
        version <- "\\d+(\\.\\d+)+".r.findFirstIn(versionStr)
      } yield version
    }

    private def readSectionFromBootPropertiesOf(launcherFile: File, sectionName: String): Map[String, String] = {
      val Property = "^\\s*(\\w+)\\s*:(.+)".r.unanchored

      def findProperty(line: String): Option[(String, String)] = {
        line match {
          case Property(name, value) => Some((name, value.trim))
          case _ => None
        }
      }

      val jar = new JarFile(launcherFile)
      try {
        Option(jar.getEntry("sbt/sbt.boot.properties")).fold(Map.empty[String, String]) { entry =>
          val lines = scala.io.Source.fromInputStream(jar.getInputStream(entry)).getLines()
          val sectionLines = lines
            .dropWhile(_.trim != s"[$sectionName]").drop(1)
            .takeWhile(!_.trim.startsWith("["))
          sectionLines.flatMap(findProperty).toMap
        }
      } finally {
        jar.close()
      }
    }

    private def readPropertyFrom(file: File, name: String): Option[String] =
      Using.resource(new BufferedInputStream(new FileInputStream(file))) { input =>
        val properties = new Properties()
        properties.load(input)
        Option(properties.getProperty(name))
      }

  }
}
