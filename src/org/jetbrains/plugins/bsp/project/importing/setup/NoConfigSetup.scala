package org.jetbrains.plugins.bsp.project.importing.setup

import org.jetbrains.plugins.bsp.reporter.{BuildMessages, BuildReporter}

import scala.util.{Success, Try}

object NoConfigSetup extends BspConfigSetup {
  override def cancel(): Unit = ()
  override def run(implicit reporter: BuildReporter): Try[BuildMessages] =
    Success(BuildMessages.empty)
}
