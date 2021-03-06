package org.jetbrains.plugins.bsp.project.importing.setup

import java.io.File
import com.intellij.openapi.projectRoots.{JavaSdk, ProjectJdkTable}
import org.jetbrains.plugins.bsp.BspBundle
import org.jetbrains.plugins.bsp.project.importing.utils
import org.jetbrains.plugins.bsp.project.importing.utils.sbt.{detectSbtVersion, sbtLauncher, sbtVersionParam, upgradedSbtVersion}
import org.jetbrains.plugins.bsp.reporter.{BuildMessages, BuildReporter}
import org.jetbrains.plugins.bsp.util.extensions.invokeAndWait

import scala.util.Try


class SbtConfigSetup(dumper: SbtStructureDump, runInit: BuildReporter => Try[BuildMessages]) extends BspConfigSetup {

  override def cancel(): Unit = dumper.cancel()

  override def run(implicit reporter: BuildReporter): Try[BuildMessages] =
    runInit(reporter)
}

object SbtConfigSetup {

  /** Runs sbt with a dummy command so that the project is initialized and .bsp/sbt.json is created. */
  def apply(baseDir: File): SbtConfigSetup = {
    invokeAndWait(ProjectJdkTable.getInstance.preconfigure())
    val jdkType = JavaSdk.getInstance()
    val jdk = ProjectJdkTable.getInstance().findMostRecentSdkOfType(jdkType)
    val jdkExe = new File(jdkType.getVMExecutablePath(jdk))
    val sbtLauncher = utils.sbt.sbtLauncher

    // dummy command so that sbt will run, init and exit
    val sbtCommandLineArgs = List("early(startServer)")
    val sbtCommands = ""

    val projectSbtVersion = detectSbtVersion(baseDir, sbtLauncher)
    val sbtVersion = upgradedSbtVersion(projectSbtVersion)
    val upgradeParam =
      if (sbtVersion > projectSbtVersion)
        List(sbtVersionParam(sbtVersion))
      else List.empty

    val vmArgs = upgradeParam

    val dumper = new SbtStructureDump()
    val runInit = (reporter: BuildReporter) => dumper.runSbt(
      baseDir, jdkExe, vmArgs, Map.empty,
      sbtLauncher, sbtCommandLineArgs, sbtCommands,
      BspBundle.message("bsp.resolver.creating.sbt.configuration"),
    )(reporter)
    new SbtConfigSetup(dumper, runInit)
  }

}
