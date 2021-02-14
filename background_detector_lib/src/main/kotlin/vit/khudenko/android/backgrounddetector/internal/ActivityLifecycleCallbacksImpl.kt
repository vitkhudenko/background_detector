package vit.khudenko.android.backgrounddetector.internal

import android.app.Activity
import vit.khudenko.android.backgrounddetector.BackgroundDetectorConfig

internal class ActivityLifecycleCallbacksImpl(
    private val callback: Callback,
    private val config: BackgroundDetectorConfig
) : BaseActivityLifecycleCallbacks() {

    interface Callback {
        fun onStartedActivityPresent()
        fun onStartedActivityAbsent()
    }

    private var startedActivitiesCount = 0

    override fun onActivityStarted(activity: Activity) {
        if (config.shouldActivityBeProcessed(activity)) {
            if (startedActivitiesCount == 0) {
                callback.onStartedActivityPresent()
            }
            startedActivitiesCount += 1
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (config.shouldActivityBeProcessed(activity)) {
            startedActivitiesCount -= 1
            if (startedActivitiesCount == 0) {
                callback.onStartedActivityAbsent()
            }
        }
    }
}
