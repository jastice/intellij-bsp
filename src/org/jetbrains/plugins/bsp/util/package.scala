package org.jetbrains.plugins.bsp

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.plugins.cl.PluginClassLoader

package object util {

  type CompilationId = Long

  object CompilationId {
    def generate(): CompilationId =
      System.nanoTime()
  }

}
