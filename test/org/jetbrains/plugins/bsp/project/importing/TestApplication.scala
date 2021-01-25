package org.jetbrains.plugins.bsp.project.importing

import com.intellij.mock.MockApplication
import com.intellij.openapi.Disposable

final protected class MockDisposable extends Disposable {
  private var disposed = false

  override def dispose(): Unit = {
    disposed = true
  }

  def isDisposed: Boolean = disposed

  override def toString: String =
    "MockDisposable"
}

object TestApplication {
  def mockApplication(): Unit = MockApplication.setUp(new MockDisposable())

}
