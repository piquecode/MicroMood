package com.piquecode.micromood.handlers.error

import com.piquecode.micromood.BuildConfig
import com.piquecode.micromood.data.PreferenceDao
import com.piquecode.micromood.dependencies.CoroutineDispatchers
import io.sentry.Sentry
import io.sentry.SentryOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface ErrorHandler {
    fun init()
}

class SentryErrorHandler(
    private val prefs: PreferenceDao,
    dispatchers: CoroutineDispatchers
) : ErrorHandler {
    private val scope = CoroutineScope(dispatchers.io)
    
    override fun init() {
        scope.launch {
            prefs.shouldReportCrashes.collect { shouldReportCrashes ->
                enableSentry(shouldReportCrashes)
            }
        }
    }
    
    private fun enableSentry(shouldReportCrashes: Boolean?) {
        Sentry.init {
            it.dsn = BuildConfig.SENTRY_DSN
            it.isEnabled = shouldReportCrashes ?: false
            it.environment = "production"
            it.release = "micromood@${BuildConfig.VERSION_NAME}"
            it.setTag("app_name", "MicroMood")
            it.beforeSend = SentryOptions.BeforeSendCallback { event, _ -> event }
        }
    }
}