package fr.antenia.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import java.util.concurrent.ConcurrentHashMap

object AnteniaNotifications {
    private const val GROUP_ID = "Antenia Projects"
    private const val DEDUPLICATION_WINDOW_MS = 60_000L
    private val lastShown = ConcurrentHashMap<String, Long>()

    fun failure(project: Project?, key: String, title: String, message: String) {
        if (project?.isDisposed == true) return
        val now = System.currentTimeMillis()
        lastShown.entries.removeIf { now - it.value >= DEDUPLICATION_WINDOW_MS }
        val notificationKey = "${project?.locationHash ?: "application"}:$key"
        val previous = lastShown.put(notificationKey, now)
        if (previous != null && now - previous < DEDUPLICATION_WINDOW_MS) return

        Notification(
            GROUP_ID,
            StringUtil.escapeXmlEntities(title),
            StringUtil.escapeXmlEntities(message),
            NotificationType.ERROR,
        )
            .notify(project)
    }
}
