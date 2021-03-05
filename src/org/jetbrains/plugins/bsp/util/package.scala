package org.jetbrains.plugins.bsp

package object util {

  type CompilationId = Long

  object CompilationId {
    def generate(): CompilationId =
      System.nanoTime()
  }

}
