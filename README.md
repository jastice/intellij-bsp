# Build Server Protocol for IntelliJ IDEA

**WORK IN PROGRESS**

Stand-alone plugin support for the [Build Server Protocol](https://build-server-protocol.github.io/) in IntelliJ IDEA. The goal of this plugin is to replace the BSP support bundled with the [Scala plugin for IntelliJ IDEA](https://github.com/JetBrains/intellij-scala).

This project is built using plugin [sbt-idea-plugin](https://github.com/JetBrains/sbt-idea-plugin).

### Requirements

To be able to develop this, you need:
- The latest version of IntelliJ IDEA
- JDK 11

### Setup

1. Clone this repository to your computer
```
$ git clone https://github.com/jastice/intellij-bsp.git
```
2. Open IntelliJ IDEA, select `File -> New -> Project from existing sources`, point to the directory where the plugin repository is and then import it as `sbt` project.
3. Select JDK 11 as project JDK (create it from an installed JDK if necessary).
4. Select the `bspPlugin` run configuration and select the `Run` or `Debug` button to build and start a development version of the plugin.

When starting the development version of the plugin, make sure Scala plugin is not installed in the new Intellij instance.