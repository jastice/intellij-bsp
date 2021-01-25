package org.jetbrains.plugins.bsp

import com.intellij.notification.NotificationGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import javax.swing.Icon
import org.jetbrains.annotations.{Nls, Nullable}

import scala.util.{Failure, Success, Try}

object BSP {
  @Nls
  //noinspection ScalaExtractStringToBundle
  val Name = "BSP"
  val Icon: Icon = Icons.BSP

  val ProjectSystemId = new ProjectSystemId("BSP", Name)

  @Nullable
  val balloonNotification: NotificationGroup = Try(BspNotificationGroup.balloon) match {
    case Failure(exception) => if(!ApplicationManager.getApplication.isUnitTestMode) throw exception else null
    case Success(value) => value
  }
}
