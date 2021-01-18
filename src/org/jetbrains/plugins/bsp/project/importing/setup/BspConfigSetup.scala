package org.jetbrains.plugins.bsp.project.importing.setup


import org.jetbrains.plugins.bsp.reporter.{BuildMessages, BuildReporter}

import scala.util.Try

abstract class BspConfigSetup {
  def cancel(): Unit
  def run(implicit reporter: BuildReporter): Try[BuildMessages]
}
