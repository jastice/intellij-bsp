package org.jetbrains.plugins.bsp.project.importing

import ch.epfl.scala.bsp4j.{BuildTarget, BuildTargetCapabilities, BuildTargetIdentifier}
import org.jetbrains.plugins.bsp.project.importing.BspResolverDescriptors.UnspecifiedModule
import org.junit.Test

import scala.jdk.CollectionConverters._

class BspResolverLogicTest {
  val uri = "ePzqj://jqke:540/n/ius7/jDa/t/z78"
  val target = new BuildTarget(
    new BuildTargetIdentifier(uri),
    List("bla").asJava, null, List.empty.asJava,
    new BuildTargetCapabilities(true,true,true)
  )

  /** When base dir is empty, only root module is created */
  @Test
  def testCalculateModuleDescriptionsEmptyBaseDir(): Unit = {

    val descriptions = BspResolverLogic.calculateModuleDescriptions(List(target), Nil, Nil, Nil, Nil, Nil)

    assert(descriptions.synthetic.isEmpty)
    assert(descriptions.modules.size == 1)
    val rootModule = descriptions.modules.head
    assert(rootModule.moduleKindData == UnspecifiedModule())
    assert(rootModule.data.targets.head == target)
  }

  @Test
  def testSharedModuleIdWhenTargetIsNull(): Unit = {
    val id = BspResolverLogic.sharedModuleId(List(target))
    assert(id == uri)
  }

}
