package org.jetbrains.plugins.bsp.util

import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library

import java.net.URL

object project {

  implicit class LibraryExt(private val library: Library) extends AnyVal {

      import LibraryExt._

      def isScalaSdk: Boolean = library match {
        case libraryEx: LibraryEx => libraryEx.isScalaSdk
        case _ => false
      }

      def compilerVersion: Option[String] = name.flatMap(LibraryVersion.findFirstIn)

      def hasRuntimeLibrary: Boolean = name.exists(isRuntimeLibrary)

      private def name: Option[String] = Option(library.getName)

      def jarUrls: Set[URL] =
        library
          .getFiles(OrderRootType.CLASSES)
          .map(_.getPath)
          .map(path => new URL(s"jar:file://$path"))
          .toSet
    }

    object LibraryExt {

      private val LibraryVersion = "(?<=[:\\-])\\d+\\.\\d+\\.\\d+[^:\\s]*".r

      private[this] val RuntimeLibrary = "((?:scala|dotty|scala3)-library).+".r

      private[this] val JarVersion = "(?<=-)\\d+\\.\\d+\\.\\d+\\S*(?=\\.jar$)".r

      def isRuntimeLibrary(name: String): Boolean = RuntimeLibrary.findFirstIn(name).isDefined

      def runtimeVersion(input: String): Option[String] = JarVersion.findFirstIn(input)
    }
}
