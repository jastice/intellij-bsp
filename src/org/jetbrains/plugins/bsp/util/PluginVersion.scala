package org.jetbrains.plugins.bsp.util

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.plugins.cl.PluginClassLoader

object PluginVersion {

  class Version(private val major: Int, private val minor: Int, private val build: Int) extends Ordered[Version] with Serializable {
    override def compare(that: Version): Int = implicitly[Ordering[(Int, Int, Int)]]
      .compare((major, minor, build), (that.major, that.minor, that.build))

    val presentation: String = if (major == Int.MaxValue) "SNAPSHOT" else s"$major.$minor.$build"

    def isSnapshot: Boolean = presentation == "SNAPSHOT"

    override def equals(that: Any): Boolean = compare(that.asInstanceOf[Version]) == 0

    override def toString: String = presentation
  }

  object Snapshot extends Version(Int.MaxValue, Int.MaxValue, Int.MaxValue)
  object Zero extends Version(0,0,0)

  def parse(version: String): Option[Version] = {
    val VersionRegex = "(\\d+)[.](\\d+)[.](\\d+)".r
    version match {
      case "VERSION" | "SNAPSHOT" => Some(Snapshot)
      case VersionRegex(major: String, minor: String, build: String) => Some(new Version(major.toInt, minor.toInt, build.toInt))
      case _ => None
    }
  }

  lazy val getPluginVersion: Option[Version] = {
    getClass.getClassLoader match {
      case pluginLoader: PluginClassLoader =>
        parse(PluginManagerCore.getPlugin(pluginLoader.getPluginId).getVersion)
      case _ => Some(Snapshot)
    }
  }

}
