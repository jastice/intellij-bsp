package org.jetbrains.plugins.bsp.project.importing.preimport

/** A PreImporter is run before every import of a project */
abstract class PreImporter {
  def cancel(): Unit
}
