name := "intellij-bsp"
organization := "JetBrains"
version := "0.1"

scalaVersion in ThisBuild := "2.13.4"
resolvers in ThisBuild += Resolver.sonatypeRepo("snapshots")

intellijPluginName in ThisBuild := "BSP"
intellijBuild in ThisBuild := "LATEST-EAP-SNAPSHOT"
intellijPlatform in ThisBuild := IntelliJPlatform.IdeaCommunity
//packageOutputDir := target.value / "plugin" / "BSP"

val bspVersion = "2.0.0-M13+25-fcc73a35-SNAPSHOT"

val bspPlugin = (project in file("."))
  .settings(
    unmanagedSourceDirectories in Compile += baseDirectory.value / "src",
    unmanagedSourceDirectories in Test += baseDirectory.value / "test",
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
    unmanagedResourceDirectories in Test += baseDirectory.value / "testResources",
    libraryDependencies ++= Seq(
      "com.propensive" %% "mercator" % "0.3.0",
      "ch.epfl.scala" %% "bsp-testkit" % bspVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.0" % Test,
      "org.scalatestplus" %% "scalacheck-1-14" % "3.2.1.0" % Test,
      "org.scalatestplus" %% "junit-4-12" % "3.1.2.0" % Test,
      "com.novocode" % "junit-interface" % "0.11" % Test,
      ("ch.epfl.scala" % "bsp4j" % bspVersion)
        .exclude("com.google.code.gson", "gson") // included in IDEA platform
        .exclude("com.google.guava", "guava"), // included in IDEA platform
    ),
    intellijPlugins += "com.intellij.java".toPlugin,
  )
  .enablePlugins(SbtIdeaPlugin)


