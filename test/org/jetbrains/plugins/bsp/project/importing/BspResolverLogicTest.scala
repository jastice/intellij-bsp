package org.jetbrains.plugins.bsp.project.importing

import ch.epfl.scala.bsp4j.{BuildTarget, BuildTargetCapabilities, BuildTargetIdentifier}
import org.jetbrains.plugins.bsp.data.{JdkData, ScalaSdkData}
import org.jetbrains.plugins.bsp.project.importing.BspResolverDescriptors.{ScalaModule, UnspecifiedModule}
import org.junit.Test

import scala.jdk.CollectionConverters._

class BspResolverLogicTest {
  TestApplication.mockApplication()

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

  @Test
  def testMergeModulesWhenScalaDataNotPresent(): Unit = {
    val nullModule1 = ScalaModule(JdkData(null, null), ScalaSdkData("org.scala", null, List.empty.asJava, List.empty.asJava))
    val nullModule2 = ScalaModule(JdkData(null, null), ScalaSdkData("ch.epfl.scala", null, List.empty.asJava, List.empty.asJava))
    val filledModule = ScalaModule(JdkData(null, null), ScalaSdkData("org.scala", "2.11", List.empty.asJava, List.empty.asJava))

    //When one of the modules is null
    val module1 = BspResolverLogic.mergeModuleKind(nullModule1, filledModule)
    assert(filledModule == module1)
    val module2 = BspResolverLogic.mergeModuleKind(filledModule, nullModule1)
    assert(filledModule == module2)

    //When one both modules are null
    val module3 = BspResolverLogic.mergeModuleKind(nullModule1, nullModule2)
    assert(module3 == nullModule1)
  }

}
