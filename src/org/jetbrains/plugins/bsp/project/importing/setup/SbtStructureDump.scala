package org.jetbrains.plugins.bsp.project.importing.setup

import com.intellij.build.events.impl.{FailureResultImpl, SkippedResultImpl, SuccessResultImpl}
import org.jetbrains.annotations.{Nls, NonNls}
import org.jetbrains.plugins.bsp.project.importing.utils.sbt.normalizePath
import org.jetbrains.plugins.bsp.reporter.BuildMessages.EventId
import org.jetbrains.plugins.bsp.reporter.{BuildMessages, BuildReporter}

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try, Using}
import java.io.{BufferedWriter, File, OutputStreamWriter, PrintWriter}
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import coursier._


class SbtStructureDump {

  private val cancellationFlag: AtomicBoolean = new AtomicBoolean(false)

  def cancel(): Unit = cancellationFlag.set(true)

  def runSbt(directory: File,
             vmExecutable: File,
             vmOptions: Seq[String],
             environment0: Map[String, String],
             sbtCommandLineArgs: List[String],
             @NonNls sbtCommands: String,
             @Nls reportMessage: String,
            )
            (implicit reporter: BuildReporter)
  : Try[BuildMessages] = {


    //    val environment = if (ApplicationManager.getApplication.isUnitTestMode && SystemInfo.isWindows) {
    //      val extraEnvs = defaultCoursierDirectoriesAsEnvVariables()
    //      environment0 ++ extraEnvs
    //    }
    //    else environment0
    val environment = environment0

    val startTime = System.currentTimeMillis()
    // assuming here that this method might still be called without valid project

    val jvmOptions = vmOptions
    //    val jvmOptions = SbtOpts.loadFrom(directory) ++ JvmOpts.loadFrom(directory) ++ vmOptions

    val resolution = "xxxx"
//    val resolution = Fetch()
//      .addDependencies(dep"org.scala-sbt:sbt-launch:1.5.2")
//      .run()

    //    val sbtLauncher = ???

    val processCommandsRaw =
      List(
        normalizePath(vmExecutable),
        "-Djline.terminal=jline.UnsupportedTerminal",
        "-Dsbt.log.noformat=true",
        "-Dfile.encoding=UTF-8") ++
        jvmOptions ++
        //        List("-jar", normalizePath(sbtLauncher)) ++
        List("-jar", resolution) ++
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
          //          handle(process, dumpTaskId, reporter)
          Success(BuildMessages.empty)
        }
      }
    //      .recoverWith {
    //        case _: ImportCancelledException =>
    //          Success(BuildMessages.empty.status(BuildMessages.Canceled))
    //        case fail =>
    //          Failure(ImportCancelledException(fail))
    //      }

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
}
