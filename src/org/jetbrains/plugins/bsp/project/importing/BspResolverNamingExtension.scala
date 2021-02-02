package org.jetbrains.plugins.bsp.project.importing

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.plugins.bsp.project.importing.BspResolverDescriptors.ModuleDescription

import scala.util.{Failure, Success, Try}

/**
 * This extension allows to customize names created during BSP project import/refresh.
 * These names are displayed in class search window, as well as used to group
 * libraries under External Libraries node in project view.
 * You may for example use it to apply custom shortening logic for long names.
 **/
trait BspResolverNamingExtension {
  def libraryData(moduleDescription: ModuleDescription): Option[String]

  def libraryTestData(moduleDescription: ModuleDescription): Option[String]
}

object BspResolverNamingExtension {
  val EP_NAME =
    ExtensionPointName.create[BspResolverNamingExtension]("com.intellij.bspResolverNamingExtension")

  def libraryData(moduleDescription: ModuleDescription): Option[String] = {
    get(_.libraryData(moduleDescription))
  }

  def libraryTestData(moduleDescription: ModuleDescription): Option[String] = {
    get(_.libraryTestData(moduleDescription))
  }

  private def get(apply: BspResolverNamingExtension => Option[String]): Option[String] = {
    Try(EP_NAME.getExtensions.iterator.map(apply).collectFirst { case Some(r) => r }) match {
      case Failure(exception) => if (ApplicationManager.getApplication.isUnitTestMode) None else throw exception
      case Success(value) => value
    }
  }

}
