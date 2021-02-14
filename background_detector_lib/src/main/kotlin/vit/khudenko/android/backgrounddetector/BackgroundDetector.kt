package vit.khudenko.android.backgrounddetector

import android.app.Application
import android.os.Handler
import vit.khudenko.android.backgrounddetector.internal.ActivityLifecycleCallbacksImpl
import java.util.Collections

/**
 * App is assigned with `background` status when none of app's activities are in `started` state.
 * And vice versa - if at least one activity in your app is in `started` state, then app is assigned
 * with `foreground` status.
 *
 * This approach has an undesired side effect: configuration changes, such as screen orientation changes,
 * would cause a short (typically 200-500 millis) period of `background` status followed by regaining
 * the `foreground` status. To mitigate this issue `BackgroundDetector` applies a status change debounce
 * delay (see [`debounceDelayMillis`][debounceDelayMillis] param).
 *
 * In order to track `started` state of activities, `BackgroundDetector` uses an implementation of
 * [`Application.ActivityLifecycleCallbacks`][Application.ActivityLifecycleCallbacks] reacting on
 * [`onActivityStarted(activity: Activity)`][Application.ActivityLifecycleCallbacks.onActivityStarted] and
 * [`onActivityStopped(activity: Activity)`][Application.ActivityLifecycleCallbacks.onActivityStopped].
 * By default `BackgroundDetector` takes under account all activities in your application, however
 * if your app requires some activities to be ignored, then a custom implementation of
 * [`BackgroundDetectorConfig`][BackgroundDetectorConfig] can do the trick (see
 * [`BackgroundDetectorConfig.shouldActivityBeProcessed(activity: Activity): Boolean`]
 * [BackgroundDetectorConfig.shouldActivityBeProcessed]).
 *
 * __IMPORTANT:__ For a proper functioning `BackgroundDetector` must be instantiated at your app's
 * [`Application.onCreate()`][Application.onCreate] callback. `BackgroundDetector` does not validate
 * correctness of its instantiation, so if this requirement is not met, then there will be no
 * warnings despite `BackgroundDetector` may report incorrect status.
 *
 * The implementation is not thread-safe. `BackgroundDetector` must be accessed from the main thread only.
 *
 * @param application [`Application`][Application]
 * @param uiHandler [`Handler`][Handler], must use the main thread [`Looper`][android.os.Looper]
 *        (see [`Looper.getMainLooper()`][android.os.Looper.getMainLooper]).
 * @param config [`BackgroundDetectorConfig`][BackgroundDetectorConfig]. If not passed, then
 *        [`BackgroundDetectorConfig.Default`][BackgroundDetectorConfig.Default] is used.
 * @param debounceDelayMillis [`Long`][Long], this is a number of milliseconds, which will be used
 *        as a status change debounce delay. Zero or negative value causes immediate status change propagation.
 *        If not passed, then [`DEFAULT_DEBOUNCE_DELAY_MILLIS`][DEFAULT_DEBOUNCE_DELAY_MILLIS] is used.
 *
 * @throws IllegalArgumentException if [uiHandler] does not use the main thread
 *        [`Looper`][android.os.Looper] (see [`Looper.getMainLooper()`][android.os.Looper.getMainLooper])
 *
 * @author Vitaliy Khudenko
 */
class BackgroundDetector(
    application: Application,
    private val uiHandler: Handler,
    private val config: BackgroundDetectorConfig = BackgroundDetectorConfig.Default(),
    private val debounceDelayMillis: Long = DEFAULT_DEBOUNCE_DELAY_MILLIS
) {

    companion object {
        /**
         * 700 ms
         */
        const val DEFAULT_DEBOUNCE_DELAY_MILLIS: Long = 700
    }

    interface Listener {
        /**
         * This is called when app is assigned with __`background`__ status.
         *
         * Always called on the main thread.
         */
        fun onBackground()

        /**
         * This is called when app is assigned with __`foreground`__ status.
         *
         * Always called on the main thread.
         */
        fun onForeground()
    }

    private var foregroundStatus = false // assume initially app is in background
    private val listeners: MutableSet<Listener> = Collections.synchronizedSet(LinkedHashSet())

    init {
        require(application.mainLooper == uiHandler.looper) {
            "uiHandler must use the main thread android.os.Looper (see android.os.Looper.getMainLooper())"
        }

        application.registerActivityLifecycleCallbacks(
            ActivityLifecycleCallbacksImpl(
                object : ActivityLifecycleCallbacksImpl.Callback {
                    override fun onStartedActivityPresent() {
                        scheduleStatusChange(foreground = true)
                    }

                    override fun onStartedActivityAbsent() {
                        scheduleStatusChange(foreground = false)
                    }
                },
                config
            )
        )
    }

    fun isAppInForeground() = foregroundStatus

    fun isAppInBackground() = foregroundStatus.not()

    /**
     * If this `listener` has been already added, then this call is no op.
     */
    fun addListener(listener: Listener) {
        if (listeners.contains(listener).not()) {
            listeners.add(listener)
        }
    }

    fun removeAllListeners() {
        listeners.clear()
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    private fun scheduleStatusChange(foreground: Boolean) {
        uiHandler.removeCallbacks(setForegroundStatusAction)
        uiHandler.removeCallbacks(setBackgroundStatusAction)

        val action = if (foreground) {
            setForegroundStatusAction
        } else {
            setBackgroundStatusAction
        }

        if (debounceDelayMillis > 0) {
            uiHandler.postDelayed(action, debounceDelayMillis)
        } else {
            action.run()
        }
    }

    private val setForegroundStatusAction = Runnable {
        notifyStatusChange(foreground = true)
    }

    private val setBackgroundStatusAction = Runnable {
        notifyStatusChange(foreground = false)
    }

    private fun notifyStatusChange(foreground: Boolean) {
        foregroundStatus = foreground
        for (listener in ArrayList(listeners)) {
            if (foreground) {
                listener.onForeground()
            } else {
                listener.onBackground()
            }
        }
    }
}
