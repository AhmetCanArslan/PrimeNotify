package com.arslan.primenotify.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class PrimeNotifyListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (!isPrimeNotifyServiceEnabled(this)) {
            return
        }
        super.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
