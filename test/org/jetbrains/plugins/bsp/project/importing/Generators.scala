package org.jetbrains.plugins.bsp.project.importing

import java.io.File
import java.nio.file.{Path, Paths}

import ch.epfl.scala.bsp.testkit.gen.Bsp4jGenerators._
import ch.epfl.scala.bsp.testkit.gen.UtilGenerators.{genFileUriString, genPath}
import ch.epfl.scala.bsp.testkit.gen.bsp4jArbitrary._
import ch.epfl.scala.bsp4j._
import com.google.gson.{Gson, GsonBuilder}
import com.intellij.pom.java.LanguageLevel
import org.jetbrains.plugins.bsp.data.{JdkData, ScalaSdkData}
import org.jetbrains.plugins.bsp.project.importing.BspResolverDescriptors.{ModuleDescription, SourceDirectory, _}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

import scala.jdk.CollectionConverters._

object Generators {

  implicit val gson: Gson = new GsonBuilder().setPrettyPrinting().create()

  implicit val arbFile: Arbitrary[File] = Arbitrary(genPath.map(_.toFile))
  implicit val arbModuleKind: Arbitrary[ModuleKind] = Arbitrary(genModuleKind)
  implicit val arbModuleDescription: Arbitrary[ModuleDescription] = Arbitrary(genModuleDescription)
  implicit val arbVersion: Arbitrary[String] = Arbitrary(genVersion)
  implicit val arbPath: Arbitrary[Path] = Arbitrary(genPath)
  implicit val arbLanguageLevel: Arbitrary[LanguageLevel] = Arbitrary(genLanguageLevel)
  implicit val arbSourceDirectory: Arbitrary[SourceDirectory] = Arbitrary(genSourceDirectory)



  lazy val genBuildTargetTagWithoutTest: Gen[String] = Gen.oneOf(
    BuildTargetTag.APPLICATION,
    BuildTargetTag.BENCHMARK,
    BuildTargetTag.INTEGRATION_TEST,
    BuildTargetTag.LIBRARY,
    BuildTargetTag.NO_IDE,
    BuildTargetTag.MANUAL
  )

  /** A system-dependent file path. */
  def genPathBelow(root: Path): Gen[Path] = for {
    segmentCount <- Gen.choose(0, 10)
    segments <- Gen.listOfN(segmentCount, Gen.identifier)
  } yield {
    // truncate string in case it's too long for macOS file system paths
    val combined = segments.mkString("/")
    val toTruncate = 250 - root.toString.length
    val truncated =
      if (toTruncate < combined.length) combined.substring(0, toTruncate)
      else combined
    val sub = Paths.get(truncated)
    root.resolve(sub)
  }

  def genLanguageLevel: Gen[LanguageLevel] = for {
     n <- Gen.chooseNum(0, LanguageLevel.values().length - 1)
     value = LanguageLevel.values()(n)
  } yield value

  def genVersion: Gen[String] = for {
    n <- Gen.chooseNum(0,4)
    v <- Gen.listOfN(n, Gen.posNum[Int])
    s <- Gen.identifier.optional
  } yield v.mkString(".") + s.fold("")("-" + _)

  def genSourceDirectoryUnder(root: Path): Gen[SourceDirectory] = for {
    path <- genPathBelow(root)
    generated <- arbitrary[Boolean]
  } yield SourceDirectory(path.toFile, generated, None)

  def genSourceDirectory: Gen[SourceDirectory] = for {
    path <- arbitrary[Path]
    generated <- arbitrary[Boolean]
  } yield SourceDirectory(path.toFile, generated, None)

  def genSourceDirs(root: Option[Path]): Gen[List[SourceDirectory]] = Gen.sized { size =>
    for {
      size1 <- Gen.choose(0,size)
      size2 = size - size1
      free <- Gen.listOfN(size1, genSourceDirectory)
      underRoot <- root
        .map(p => Gen.listOfN(size2, genSourceDirectoryUnder(p)))
        .getOrElse(Gen.const[List[SourceDirectory]](List.empty))
    } yield {
      free ++ underRoot
    }
  }

  def genScalaBuildTargetWithoutTags(withoutTags: List[String]): Gen[BuildTarget] = for {
    target <- genBuildTargetWithScala
    baseDir <- genFileUriString
  } yield {
    val newTags = target.getTags.asScala.filterNot(withoutTags.contains)
    target.setTags(newTags.asJava)
    if (target.getBaseDirectory == null)
      target.setBaseDirectory(baseDir.toString)
    target
  }

  def genScalaSdkData: Gen[ScalaSdkData] = for {
    scalaOrganization <- arbitrary[String]
    scalaVersion <- Gen.listOf(arbitrary[String])
    scalacClasspath <- arbitrary[File].list
    scalacOptions <- arbitrary[String].list
  } yield ScalaSdkData(scalaOrganization, if(scalaVersion.nonEmpty) scalaVersion.mkString("-") else null, scalacClasspath, scalacOptions)

  def genJdkData: Gen[JdkData] = for {
    javaHome <- genFileUri
    javaVersion <- arbitrary[String]
  } yield JdkData(javaHome, javaVersion)

  def genModuleKind: Gen[ModuleKind] = for {
    jdkData <- genJdkData
    scalaSdkData <- genScalaSdkData
  } yield ScalaModule(jdkData, scalaSdkData)

  def genModuleDescription: Gen[ModuleDescription] = for {
    id <- arbitrary[String]
    name <- arbitrary[String]
    targets <- arbitrary[List[BuildTarget]]
    targetDependencies <- arbitrary[Seq[BuildTargetIdentifier]]
    targetTestDependencies <- arbitrary[Seq[BuildTargetIdentifier]]
    basePath <- arbitrary[Path].optional
    output <- arbitrary[Option[File]]
    testOutput <- arbitrary[Option[File]]
    sourceDirs <- genSourceDirs(basePath)
    testSourceDirs <- genSourceDirs(basePath)
    resourceDirs <- genSourceDirs(basePath)
    testResourceDirs <- genSourceDirs(basePath)
    classPath <- arbitrary[Seq[File]]
    classPathSources <- arbitrary[Seq[File]]
    testClassPath <- arbitrary[Seq[File]]
    testClassPathSources <- arbitrary[Seq[File]]
    moduleKind <- genModuleKind
  } yield {
    val data = ModuleDescriptionData(id, name, targets, targetDependencies, targetTestDependencies, basePath.map(_.toFile), output, testOutput,
      sourceDirs, testSourceDirs, resourceDirs, testResourceDirs, classPath, classPathSources, testClassPath, testClassPathSources, None)
    ModuleDescription(data, moduleKind)
  }

  def genScalaBuildTargetWithData(buildTargets: List[BuildTarget]): Gen[(List[ScalacOptionsItem], List[JavacOptionsItem], List[SourcesItem], List[ResourcesItem], List[DependencySourcesItem])] = for {
    scalacOptionsItems <- arbitrary[List[ScalacOptionsItem]]
    javacOptionsItems <- arbitrary[List[JavacOptionsItem]]
    sourceItems <- arbitrary[List[SourceItem]]
    resourcesItems <- arbitrary[List[ResourcesItem]]
    dependencySourcesItems <- arbitrary[List[DependencySourcesItem]]
  } yield {
    val size = if (buildTargets.isEmpty || sourceItems.isEmpty) 1 else math.max(sourceItems.size / buildTargets.size, 1)
    val splittedSourceItems = sourceItems.grouped(size).toList
    val sourcesItems = buildTargets.zipWithIndex.map { case (target, i) => new SourcesItem(target.getId, splittedSourceItems.lift(i).getOrElse(List.empty).asJava) }
    (scalacOptionsItems, javacOptionsItems, sourcesItems, resourcesItems, dependencySourcesItems)
  }

}
