name := "bspPlugin"
organization := "JetBrains"
version := "0.1"

ThisBuild / scalaVersion := "2.13.4"
ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")

ThisBuild / intellijPluginName := "BSP"
ThisBuild / intellijBuild := "212.3724.25"
ThisBuild / intellijPlatform := IntelliJPlatform.IdeaCommunity
//packageOutputDir := target.value / "plugin" / "BSP"

val bspVersion = "2.0.0-M13+28-2ab51d83-SNAPSHOT"

val bspPlugin = (project in file("."))
  .settings(
    Compile / unmanagedSourceDirectories += baseDirectory.value / "src",
    Test / unmanagedSourceDirectories += baseDirectory.value / "test",
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Test/ unmanagedResourceDirectories += baseDirectory.value / "testResources",
    libraryDependencies ++= Seq(
      "com.propensive" %% "mercator" % "0.3.0",
      "io.get-coursier" %% "coursier" % "2.0.16",
      "ch.epfl.scala" %% "bsp-testkit" % bspVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.0" % Test,
      "org.scalatestplus" %% "scalacheck-1-14" % "3.2.1.0" % Test,
      "org.scalatestplus" %% "junit-4-12" % "3.1.2.0" % Test,
      "com.novocode" % "junit-interface" % "0.11" % Test,
      ("ch.epfl.scala" % "bsp4j" % bspVersion)
        .exclude("com.google.code.gson", "gson") // included in IDEA platform
        .exclude("com.google.guava", "guava") // included in IDEA platform
    ),
    intellijPlugins += "com.intellij.java".toPlugin
  )
  .enablePlugins(SbtIdeaPlugin)
