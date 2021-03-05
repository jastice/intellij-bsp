package org.jetbrains.plugins.bsp.project.importing

import ch.epfl.scala.bsp.testkit.gen.UtilGenerators._
import org.jetbrains.plugins.bsp.BspUtil.{StringOps, URIOps}
import org.junit.{Ignore, Test}
import org.scalacheck.Prop.forAll
import org.scalatestplus.junit.AssertionsForJUnit
import org.scalatestplus.scalacheck.Checkers

class BspUtilProperties extends AssertionsForJUnit with Checkers {

  @Test
  def stringOpsToUri(): Unit = check(
    forAll(genUri) { uri =>
      uri.toURI.toString == uri
    }
  )

  @Test @Ignore
  def uriOpsToFile(): Unit = check(
    forAll(genPath) { path =>
      path.toUri.toFile == path.toFile
    }
  )
}
