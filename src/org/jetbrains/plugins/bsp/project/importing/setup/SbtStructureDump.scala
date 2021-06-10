package org.jetbrains.plugins.bsp.project.importing.setup

import com.intellij.build.events.impl.{FailureResultImpl, SkippedResultImpl, SuccessResultImpl}
import com.intellij.execution.process.{OSProcessHandler, ProcessAdapter, ProcessEvent, ProcessOutputTypes}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import org.jetbrains.annotations.{Nls, NonNls}
import org.jetbrains.plugins.bsp.project.importing.BspProjectResolver.ImportCancelledException
import org.jetbrains.plugins.bsp.project.importing.setup.SbtStructureDump.{ListenerAdapter, OutputType, reportEvent}
import org.jetbrains.plugins.bsp.project.importing.utils.sbt.normalizePath
import org.jetbrains.plugins.bsp.reporter.BuildMessages.EventId
import org.jetbrains.plugins.bsp.reporter.{BuildMessages, BuildReporter, ExternalSystemNotificationReporter}

import java.io.{BufferedWriter, File, OutputStreamWriter, PrintWriter}
import java.nio.charset.Charset
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try, Using}


class SbtStructureDump {

  private val Log = Logger.getInstance(classOf[SbtStructureDump])

  private val cancellationFlag: AtomicBoolean = new AtomicBoolean(false)

  private val SBT_PROCESS_CHECK_TIMEOUT_MSEC = 100

  def cancel(): Unit = cancellationFlag.set(true)

  def runSbt(directory: File,
             vmExecutable: File,
             vmOptions: Seq[String],
             environment: Map[String, String],
             sbtLauncher: File,
             sbtCommandLineArgs: List[String],
             @NonNls sbtCommands: String,
             @Nls reportMessage: String,
            )
            (implicit reporter: BuildReporter)
  : Try[BuildMessages] = {
    val startTime = System.currentTimeMillis()

    val processCommandsRaw =
      List(
        normalizePath(vmExecutable),
        "-Djline.terminal=jline.UnsupportedTerminal",
        "-Dsbt.log.noformat=true",
        "-Dfile.encoding=UTF-8") ++
        vmOptions ++
        List("-jar", normalizePath(sbtLauncher)) ++
        sbtCommandLineArgs // :+ "--debug"

    val processCommands = processCommandsRaw.filterNot(_.isEmpty)

    val dumpTaskId = EventId(s"dump:${UUID.randomUUID()}")
    reporter.startTask(dumpTaskId, None, reportMessage, startTime)

    val resultMessages = Try {
      val processBuilder = new ProcessBuilder(processCommands.asJava)
      processBuilder.directory(directory)
      processBuilder.environment().putAll(environment.asJava)
      val procString = processBuilder.command().asScala.mkString(" ")
      reporter.log(procString)
      processBuilder.start()
    }
      .flatMap { process =>
        Using.resource(new PrintWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream, "UTF-8")))) { writer =>
          writer.println(sbtCommands)
          // exit needs to be in a separate command, otherwise it will never execute when a previous command in the chain errors
          writer.println("exit")
          writer.flush()
          // handle(process, dumpTaskId, reporter)
          Success(BuildMessages.empty)
        }
      }
      .recoverWith {
        case _: ImportCancelledException =>
          Success(BuildMessages.empty.status(BuildMessages.Canceled))
        case fail =>
          Failure(ImportCancelledException(fail))
      }

    val eventResult = resultMessages match {
      case Success(messages) =>
        messages.status match {
          case BuildMessages.OK =>
            new SuccessResultImpl(true)
          case BuildMessages.Canceled =>
            new SkippedResultImpl()
          case BuildMessages.Error | BuildMessages.Indeterminate =>
            new FailureResultImpl(messages.errors.asJava)
        }

      case Failure(x) =>
        new FailureResultImpl(x)
    }

    reporter.finishTask(dumpTaskId, reportMessage, eventResult)

    resultMessages
  }

  private def handle(process: Process,
                     dumpTaskId: EventId,
                     reporter: BuildReporter
                    ): Try[BuildMessages] = {

    var messages = BuildMessages.empty

    def update(typ: OutputType, textRaw: String): Unit = {
      val text = textRaw.trim

      if (text.nonEmpty) {
        messages = reportEvent(messages, reporter, text)
        reporter.progressTask(dumpTaskId, 1, -1, "", text)
        (typ, reporter) match {
          case (OutputType.StdErr, reporter: ExternalSystemNotificationReporter) =>
            reporter.logErr(text)
          case _ => reporter.log(text)
        }
      }
    }

    val processListener: (OutputType, String) => Unit = (typ, line) => {
      (typ, line) match {
        case (typ@OutputType.StdOut, text) =>
          if (text.contains("(q)uit")) {
            val writer = new PrintWriter(process.getOutputStream)
            writer.println("q")
            writer.close()
          } else {
            update(typ, text)
          }
        case (typ@OutputType.StdErr, text) =>
          update(typ, text)
        case _ => // ignore
      }
    }

    val handler = new OSProcessHandler(process, "sbt import", Charset.forName("UTF-8"))
    // TODO: rewrite this code, do not use try, throw
    val result = Try {
      handler.addProcessListener(new ListenerAdapter(processListener))
      Log.debug("handler.startNotify()")
      handler.startNotify()

      val start = System.currentTimeMillis()

      var processEnded = false
      while (!processEnded && !cancellationFlag.get()) {
        processEnded = handler.waitFor(SBT_PROCESS_CHECK_TIMEOUT_MSEC)
      }

      val exitCode = handler.getExitCode
      Log.debug(s"processEnded: $processEnded, exitCode: $exitCode")
      if (!processEnded)
        throw ImportCancelledException(new Exception("sbt task cancelled"))
      else if (exitCode != 0)
        messages.status(BuildMessages.Error)
      else if (messages.status == BuildMessages.Indeterminate)
        messages.status(BuildMessages.OK)
      else
        messages
    }
    if (!handler.isProcessTerminated) {
      Log.debug(s"sbt process has not terminated, destroying the process...")
      handler.setShouldDestroyProcessRecursively(false) // TODO: why not `true`?
      handler.destroyProcess()
    }
    result
  }


}

object SbtStructureDump {

  private def reportEvent(messages: BuildMessages,
                          reporter: BuildReporter,
                          text: String): BuildMessages = {

    if (ApplicationManager.getApplication.isUnitTestMode && (text.startsWith("[warn]") || text.startsWith("[error]")))
      System.err.println(text)

    if (text.startsWith("[error] Total time")) {
      val msg = "sbt task failed. See log for details."
      reporter.error(msg, None)
      messages
        .addError(msg)
        .status(BuildMessages.Error)
    } else messages
  }

  sealed abstract class OutputType

  object OutputType {
    object StdOut extends OutputType
    object StdErr extends OutputType
    object MySystem extends OutputType
    final case class Other(key: Key[_]) extends OutputType
  }

  class ListenerAdapter(listener: (OutputType, String) => Unit) extends ProcessAdapter {
    override def onTextAvailable(event: ProcessEvent, outputType: Key[_]): Unit = {
      val textType = outputType match {
        case ProcessOutputTypes.STDOUT => Some(OutputType.StdOut)
        case ProcessOutputTypes.STDERR => Some(OutputType.StdErr)
        case ProcessOutputTypes.SYSTEM => Some(OutputType.MySystem)
        case other                     => Some(OutputType.Other(other))
      }
      textType.foreach(t => listener(t, event.getText))
    }
  }
}