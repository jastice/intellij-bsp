package org.jetbrains.plugins.bsp.project.importing.preimport

import com.intellij.openapi.projectRoots.{JavaSdk, ProjectJdkTable}
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.plugins.bsp.project.importing.setup.SbtStructureDump
import org.jetbrains.plugins.bsp.project.importing.utils
import org.jetbrains.plugins.bsp.project.importing.utils.sbt.{SbtFileExtension, detectSbtVersion, sbtVersionParam, upgradedSbtVersion}
import org.jetbrains.plugins.bsp.util.extensions.invokeAndWait
import java.io.File
import org.jetbrains.plugins.bsp.BspBundle
import org.jetbrains.plugins.bsp.reporter.{BuildMessages, BuildReporter}
import scala.util.Try


class BloopPreImporter(dumper: SbtStructureDump, runDump: SbtStructureDump => Try[BuildMessages]) extends PreImporter {

  override def cancel(): Unit = dumper.cancel()

  def run(): Try[BuildMessages] = runDump(dumper)
}

object BloopPreImporter {

  val bloopVersion = "1.4.5"

  def apply(baseDir: File)(implicit reporter: BuildReporter): BloopPreImporter = {
    invokeAndWait(ProjectJdkTable.getInstance.preconfigure())
    val jdkType = JavaSdk.getInstance()
    val jdk = ProjectJdkTable.getInstance().findMostRecentSdkOfType(jdkType)
    val jdkExe = new File(jdkType.getVMExecutablePath(jdk))
    val sbtLauncher = utils.sbt.sbtLauncher

    val injectedPlugins = s"""addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "${bloopVersion}")"""
    val pluginFile = FileUtil.createTempFile("idea", SbtFileExtension, true)
    val pluginFilePath = utils.sbt.normalizePath(pluginFile)
    FileUtil.writeToFile(pluginFile, injectedPlugins)

    val injectedSettings = """bloopExportJarClassifiers in Global := Some(Set("sources"))"""
    val settingsFile = FileUtil.createTempFile(baseDir, "idea-bloop", SbtFileExtension, true)
    FileUtil.writeToFile(settingsFile, injectedSettings)

    val sbtCommandArgs = List(
      "early(addPluginSbtFile=\"\"\"" + pluginFilePath + "\"\"\")"
    )
    val sbtCommands = "bloopInstall"

    val projectSbtVersion = detectSbtVersion(baseDir, sbtLauncher)
    val sbtVersion = upgradedSbtVersion(projectSbtVersion)
    val upgradeParam =
      if (sbtVersion > projectSbtVersion)
        List(sbtVersionParam(sbtVersion))
      else List.empty

    val vmArgs = upgradeParam

    try {
      val dumper = new SbtStructureDump()
      val runDump = (dumper: SbtStructureDump) => dumper.runSbt(
        baseDir, jdkExe, vmArgs,
        Map.empty, sbtLauncher, sbtCommandArgs, sbtCommands,
        BspBundle.message("bsp.resolver.creating.bloop.configuration.from.sbt"),
      )
      new BloopPreImporter(dumper, runDump)
    } finally {
      settingsFile.delete()
    }
  }
}
