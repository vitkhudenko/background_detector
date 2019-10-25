package vit.khudenko.android.backgrounddetector

import android.app.Activity

/**
 * @author Vitaliy Khudenko
 */
interface BackgroundDetectorConfig {

    /**
     * If method returns __`false`__, then lifecycle changes of this activity will not affect
     * application foreground/background status reported by [`BackgroundDetector`][BackgroundDetector]
     * (as if this activity would have not existed at all).
     *
     * If method returns __`true`__, then lifecycle changes of this activity will affect
     * application foreground/background status reported by [`BackgroundDetector`][BackgroundDetector].
     *
     * __IMPORTANT:__ For a proper `BackgroundDetector` functioning, value being returned for a particular
     * activity must remain consistent across invocations.
     */
    fun shouldActivityBeProcessed(activity: Activity): Boolean

    /**
     * Default config instructs [`BackgroundDetector`][BackgroundDetector] to process
     * lifecycle changes of all activities in your app.
     */
    class Default : BackgroundDetectorConfig {

        /**
         * Always returns `true`, which instructs [`BackgroundDetector`][BackgroundDetector] to
         * take under account lifecycle changes of all activities in your app.
         */
        override fun shouldActivityBeProcessed(activity: Activity) = true
    }
}