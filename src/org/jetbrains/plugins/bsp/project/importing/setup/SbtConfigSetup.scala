package org.jetbrains.plugins.bsp.project.importing.setup

import java.io.File
import com.intellij.openapi.projectRoots.{JavaSdk, ProjectJdkTable}
import org.jetbrains.plugins.bsp.BspBundle
import org.jetbrains.plugins.bsp.project.importing.utils
import org.jetbrains.plugins.bsp.project.importing.utils.sbt.{detectSbtVersion, getLauncher, upgradedSbtVersion}
import org.jetbrains.plugins.bsp.reporter.{BuildMessages, BuildReporter}
import org.jetbrains.plugins.bsp.util.Version
import org.jetbrains.plugins.bsp.util.extensions.invokeAndWait
//import org.jetbrains.sbt.SbtUtil
//import org.jetbrains.sbt.SbtUtil.{detectSbtVersion, getDefaultLauncher, sbtVersionParam, upgradedSbtVersion}
//import org.jetbrains.sbt.project.SbtExternalSystemManager
//import org.jetbrains.sbt.project.structure.SbtStructureDump

import scala.util.Try

//class SbtConfigSetup(dumper: SbtStructureDump, runInit: BuildReporter => Try[BuildMessages])
class SbtConfigSetup(runInit: BuildReporter => Try[BuildMessages])
  extends BspConfigSetup {

//  override def cancel(): Unit = dumper.cancel()
//  override def run(implicit reporter: BuildReporter): Try[BuildMessages] =
//    runInit(reporter)

  override def cancel(): Unit = ???
  override def run(implicit reporter: BuildReporter): Try[BuildMessages] = ???
}

object SbtConfigSetup {

  /** Runs sbt with a dummy command so that the project is initialized and .bsp/sbt.json is created. */
  def apply(baseDir: File): SbtConfigSetup = {
    ???
//    invokeAndWait(ProjectJdkTable.getInstance.preconfigure())
//    val jdkType = JavaSdk.getInstance()
//    val jdk = ProjectJdkTable.getInstance().findMostRecentSdkOfType(jdkType)
//    val jdkExe = new File(jdkType.getVMExecutablePath(jdk))
//    val jdkHome = Option(jdk.getHomePath).map(new File(_))
//    val sbtLauncher = utils.sbt.getLauncher
//
//    // dummy command so that sbt will run, init and exit
//    val sbtCommandLineArgs = List("early(startServer)")
//    val sbtCommands = ""
//
//    val projectSbtVersion = Version(detectSbtVersion(baseDir, getLauncher))
//    val sbtVersion = upgradedSbtVersion(projectSbtVersion)
//    val upgradeParam =
//      if (sbtVersion > projectSbtVersion)
//        List(sbtVersionParam(sbtVersion))
//      else List.empty
//
//    val vmArgs = SbtExternalSystemManager.getVmOptions(Seq.empty, jdkHome) ++ upgradeParam
//
//    val dumper = new SbtStructureDump()
//    val runInit = (reporter: BuildReporter) => dumper.runSbt(
//      baseDir, jdkExe, vmArgs,
//      Map.empty, sbtLauncher, sbtCommandLineArgs, sbtCommands,
//      BspBundle.message("bsp.resolver.creating.sbt.configuration"),
//    )(reporter)
//    new SbtConfigSetup(dumper, runInit)
  }

}
