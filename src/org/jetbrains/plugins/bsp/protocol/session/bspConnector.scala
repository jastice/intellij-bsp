package org.jetbrains.plugins.bsp.protocol.session

import java.io.File
import java.net.URI
import ch.epfl.scala.bsp4j.{BspConnectionDetails, BuildClientCapabilities, InitializeBuildParams}
import com.google.gson.{JsonArray, JsonObject}
import org.jetbrains.plugins.bsp.protocol.session.BspSession.Builder
import org.jetbrains.plugins.bsp.{BspBundle, BspError, BspErrorMessage}
import org.jetbrains.plugins.bsp.protocol.session.BspSession.Builder
import org.jetbrains.plugins.bsp.reporter.BuildReporter
import org.jetbrains.plugins.bsp.util.PluginVersion.getPluginVersion
import org.jetbrains.plugins.bsp.{BspBundle, BspError, BspErrorMessage}

import scala.jdk.CollectionConverters._

object BspServerConnector {
  sealed abstract class BspConnectionMethod
  final case class ProcessBsp(details: BspConnectionDetails) extends BspConnectionMethod

  case class BspCapabilities(languageIds: List[String])

  private[session] def createInitializeBuildParams(rootUri: URI, compilerOutput: URI, capabilities: BspCapabilities) = {
    val buildClientCapabilities = new BuildClientCapabilities(capabilities.languageIds.asJava)
    val pluginVersion = getPluginVersion.map(_.presentation).getOrElse("N/A")
    val dataJson = new JsonObject()
    dataJson.addProperty("clientClassesRootDir", compilerOutput.toString)
    dataJson.add("supportedScalaVersions", new JsonArray()) // shouldn't need this, but bloop may not parse the data correctly otherwise
    val initializeBuildParams = new InitializeBuildParams("IntelliJ-BSP", pluginVersion, "2.0", rootUri.toString, buildClientCapabilities)
    initializeBuildParams.setData(dataJson)

    initializeBuildParams
  }
}

abstract class BspServerConnector() {
  /**
    * Connect to a bsp server with one of the given methods.
    * @return a BspError if no compatible method is found.
    */
  def connect(reporter: BuildReporter): Either[BspError, Builder]
}

class DummyConnector(rootUri: URI) extends BspServerConnector() {
  override def connect(reporter: BuildReporter): Left[BspErrorMessage, Nothing] =
    Left(BspErrorMessage(BspBundle.message("bsp.protocol.no.way.to.connect.to.bsp.server", rootUri)))
}


