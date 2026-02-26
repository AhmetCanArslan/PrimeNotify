package com.arslan.primenotify.data

/**
 * Lightweight singleton that carries pre-fill data from the Logging screen
 * to the Add/Edit Rule screen. Consumed once and then cleared.
 */
object RulePrefillData {
    var packageName: String? = null
        private set
    var appName: String? = null
        private set
    var titleKeyword: String? = null
        private set
    var bodyKeyword: String? = null
        private set

    fun set(
        packageName: String,
        appName: String,
        titleKeyword: String?,
        bodyKeyword: String?
    ) {
        this.packageName = packageName
        this.appName = appName
        this.titleKeyword = titleKeyword
        this.bodyKeyword = bodyKeyword
    }

    /** Returns all fields and resets them to null. Call exactly once from the consumer. */
    fun consume(): Prefill? {
        val pkg = packageName ?: return null
        val app = appName ?: return null
        val result = Prefill(pkg, app, titleKeyword, bodyKeyword)
        packageName = null
        appName = null
        titleKeyword = null
        bodyKeyword = null
        return result
    }

    data class Prefill(
        val packageName: String,
        val appName: String,
        val titleKeyword: String?,
        val bodyKeyword: String?
    )
}
