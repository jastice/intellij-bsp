package org.jetbrains.plugins.bsp.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.{Computable, Disposer}
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.annotations.NonNls

import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import scala.runtime.NonLocalReturnControl

object extensions {

  implicit def toComputable[T](action: => T): Computable[T] = () => action

  def inReadAction[T](body: => T): T = ApplicationManager.getApplication match {
    case application if application.isReadAccessAllowed => body
    case application => application.runReadAction(body)
  }

  def invokeAndWait[T](body: => T): T = {
    val result = new AtomicReference[T]()
    preservingControlFlow {
      ApplicationManager.getApplication.invokeAndWait(() => result.set(body))
    }
    result.get()
  }

  def invokeOnDispose(parentDisposable: Disposable)(body: => Unit): Unit =
    Disposer.register(parentDisposable, () => body)

  private def preservingControlFlow(body: => Unit): Unit =
    try {
      body
    } catch {
      case e: InvocationTargetException => e.getTargetException match {
        case control: NonLocalReturnControl[_] => throw control
        case _ => throw e
      }
    }

  object executionContext {
    private val appExecutorService = AppExecutorUtil.getAppExecutorService
    implicit val appExecutionContext: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(appExecutorService)
  }

  implicit class ProjectExt(private val project: Project) extends AnyVal {
    def unloadAwareDisposable: Disposable =
      UnloadAwareDisposable.forProject(project)
  }

  implicit class VirtualFileExt(private val entry: VirtualFile) extends AnyVal {
    def containsDirectory(@NonNls name: String): Boolean = find(name).exists(_.isDirectory)

    def containsFile(@NonNls name: String): Boolean = find(name).exists(_.isFile)

    def find(@NonNls name: String): Option[VirtualFile] = Option(entry.findChild(name))

    def isFile: Boolean = !entry.isDirectory
  }


}
