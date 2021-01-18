package org.jetbrains.plugins.bsp.protocol.session

import java.io.File
import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.intellij.execution.configurations.JavaParameters
import com.intellij.openapi.projectRoots.{JavaSdk, ProjectJdkTable}
import org.jetbrains.plugins.bsp.{BspBundle, BspError}
import org.jetbrains.plugins.bsp.protocol.session.BspServerConnector.BspCapabilities
import org.jetbrains.plugins.bsp.protocol.session.BspSession.Builder
import org.jetbrains.plugins.bsp.{BspBundle, BspError}
import org.jetbrains.plugins.bsp.protocol.session.BspServerConnector.BspCapabilities
import org.jetbrains.plugins.bsp.protocol.session.BspSession.Builder
import org.jetbrains.plugins.bsp.reporter.BuildReporter

import scala.jdk.CollectionConverters._

class BloopLauncherConnector(base: File, compilerOutput: File, capabilities: BspCapabilities) extends BspServerConnector {

  val bloopVersion = "1.4.5"
  val bspVersion = "2.0.0"

  override def connect(reporter: BuildReporter): Either[BspError, Builder] = {

    throw new UnsupportedOperationException("implementation not yet migrated")

//    val dependencies = Seq(
//      ("ch.epfl.scala" % "bloop-launcher_2.12" % bloopVersion).transitive(),
//      "org.scala-lang" % "scala-library" % "2.12.12"
//    )
//
//    val launcherClasspath = DependencyManager.resolve(dependencies: _*)
//      .map(_.file.getCanonicalPath)
//      .asJava

    val launcherClasspath = ???


//    val launcher = new File(SbtUtil.getLauncherDir, "bloop-launcher.jar")
//    val scalaSdk = new File(SbtUtil.getLibDir, "scala-library.jar")
//    val jna = new File(SbtUtil.getLibDir, "jna-4.5.0.jar") // TODO ensure it's the version that is packaged
//    val jnaPlatform = new File(SbtUtil.getLibDir, "jna-platform-4.5.0.jar")
//    val scalaXml = new File(SbtUtil.getLibDir, "scala-xml.jar")
//    val launcherClasspath = List(launcher, scalaSdk, jna, jnaPlatform, scalaXml).map(_.getCanonicalPath).asJava

    // TODO handle no available jdk case
    val jdk = ProjectJdkTable.getInstance().findMostRecentSdkOfType(JavaSdk.getInstance())

    val javaParameters: JavaParameters = new JavaParameters
    javaParameters.setJdk(jdk)
    javaParameters.setWorkingDirectory(base)
    javaParameters.getClassPath.addAll(launcherClasspath)
    javaParameters.setMainClass("bloop.launcher.Launcher")

    val cmdLine = javaParameters.toCommandLine
    cmdLine.addParameter(bloopVersion)

    val argv = cmdLine.getCommandLineList(null)

    reporter.log(BspBundle.message("bsp.protocol.starting.bloop"))
    //noinspection ReferencePassedToNls
    reporter.log(cmdLine.getCommandLineString)

    val details = new BspConnectionDetails("Bloop", argv, bloopVersion, bspVersion, List("java","scala").asJava)
    Right(prepareBspSession(details))
  }

  private def prepareBspSession(details: BspConnectionDetails): Builder = {

    val processBuilder = new java.lang.ProcessBuilder(details.getArgv).directory(base)
    val process = processBuilder.start()

    val cleanup = () => {
      process.destroy()
    }

    val rootUri = base.getCanonicalFile.toURI
    val compilerOutputUri = compilerOutput.getCanonicalFile.toURI
    val initializeBuildParams = BspServerConnector.createInitializeBuildParams(rootUri, compilerOutputUri, capabilities)

    BspSession.builder(process.getInputStream, process.getErrorStream, process.getOutputStream, initializeBuildParams, cleanup)
  }

}
