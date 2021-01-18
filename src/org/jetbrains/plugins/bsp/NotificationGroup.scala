package org.jetbrains.plugins.bsp

import com.intellij.notification.{NotificationGroup, NotificationGroupManager}

object BspNotificationGroup {

  private val BALLOON_GROUP_ID = "BSP Balloon Notifications"

  def balloon: NotificationGroup =
    NotificationGroupManager.getInstance().getNotificationGroup(BALLOON_GROUP_ID)
}
