package vit.khudenko.android.backgrounddetector.internal

import android.app.Activity
import android.os.Bundle
import com.nhaarman.mockitokotlin2.*
import org.junit.Before
import org.junit.Test
import vit.khudenko.android.backgrounddetector.BackgroundDetectorConfig

class ActivityLifecycleCallbacksImplTest {

    private lateinit var callback: ActivityLifecycleCallbacksImpl.Callback
    private lateinit var config: BackgroundDetectorConfig

    @Before
    fun setUp() {
        callback = mock()
        config = mock {
            on { shouldActivityBeProcessed(any()) } doReturn true
        }
    }

    @Test
    fun `instantiation must not have side effects`() {
        ActivityLifecycleCallbacksImpl(callback, config)
        verifyNoMoreInteractions(callback, config)
    }

    @Test
    fun `single activity started`() {
        val activityLifecycleCallbacks = ActivityLifecycleCallbacksImpl(callback, config)

        val activity = mock<Activity>()
        activityLifecycleCallbacks.onActivityStarted(activity)
        verifyFirstActivityStartedReported(activity)
    }

    @Test
    fun `single activity started and stopped`() {
        val activityLifecycleCallbacks = ActivityLifecycleCallbacksImpl(callback, config)

        val activity = mock<Activity>()
        activityLifecycleCallbacks.onActivityStarted(activity)
        verifyFirstActivityStartedReported(activity)

        activityLifecycleCallbacks.onActivityStopped(activity)
        verifyLastActivityStoppedReported(activity)
    }

    @Test
    fun `two activities started`() {
        val activityLifecycleCallbacks = ActivityLifecycleCallbacksImpl(callback, config)

        val activityA = mock<Activity>()
        val activityB = mock<Activity>()

        activityLifecycleCallbacks.onActivityStarted(activityA)
        verifyFirstActivityStartedReported(activityA)

        activityLifecycleCallbacks.onActivityStarted(activityB)
        verifyActivityLifecycleChangeIgnored(activityB)
        activityLifecycleCallbacks.onActivityStopped(activityA)
        verifyActivityLifecycleChangeIgnored(activityA)
    }

    @Test
    fun `two activities started and then second activity stopped`() {
        val activityLifecycleCallbacks = ActivityLifecycleCallbacksImpl(callback, config)

        val activityA = mock<Activity>()
        val activityB = mock<Activity>()

        activityLifecycleCallbacks.onActivityStarted(activityA)
        verifyFirstActivityStartedReported(activityA)

        activityLifecycleCallbacks.onActivityStarted(activityB)
        verifyActivityLifecycleChangeIgnored(activityB)
        activityLifecycleCallbacks.onActivityStopped(activityA)
        verifyActivityLifecycleChangeIgnored(activityA)

        activityLifecycleCallbacks.onActivityStarted(activityA)
        verifyActivityLifecycleChangeIgnored(activityA)
        activityLifecycleCallbacks.onActivityStopped(activityB)
        verifyActivityLifecycleChangeIgnored(activityB)
    }

    @Test
    fun `two activities started and then both stopped`() {
        val activityLifecycleCallbacks = ActivityLifecycleCallbacksImpl(callback, config)

        val activityA = mock<Activity>()
        val activityB = mock<Activity>()

        activityLifecycleCallbacks.onActivityStarted(activityA)
        verifyFirstActivityStartedReported(activityA)

        activityLifecycleCallbacks.onActivityStarted(activityB)
        verifyActivityLifecycleChangeIgnored(activityB)
        activityLifecycleCallbacks.onActivityStopped(activityA)
        verifyActivityLifecycleChangeIgnored(activityA)

        activityLifecycleCallbacks.onActivityStarted(activityA)
        verifyActivityLifecycleChangeIgnored(activityA)
        activityLifecycleCallbacks.onActivityStopped(activityB)
        verifyActivityLifecycleChangeIgnored(activityB)

        activityLifecycleCallbacks.onActivityStopped(activityA)
        verifyLastActivityStoppedReported(activityA)
    }

    @Test
    fun `two activities started and then a config change happened`() {
        val activityLifecycleCallbacks = ActivityLifecycleCallbacksImpl(callback, config)

        val activityA = mock<Activity>()
        val activityB = mock<Activity>()

        activityLifecycleCallbacks.onActivityStarted(activityA)
        verifyFirstActivityStartedReported(activityA)

        activityLifecycleCallbacks.onActivityStarted(activityB)
        verifyActivityLifecycleChangeIgnored(activityB)
        activityLifecycleCallbacks.onActivityStopped(activityA)
        verifyActivityLifecycleChangeIgnored(activityA)

        activityLifecycleCallbacks.onActivityStopped(activityB)
        verifyLastActivityStoppedReported(activityB)
        activityLifecycleCallbacks.onActivityStarted(activityB)
        verifyFirstActivityStartedReported(activityB)
    }

    @Test
    fun `two activities started, then a config change happened and then second activity stopped`() {
        val activityLifecycleCallbacks = ActivityLifecycleCallbacksImpl(callback, config)

        val activityA = mock<Activity>()
        val activityB = mock<Activity>()

        activityLifecycleCallbacks.onActivityStarted(activityA)
        verifyFirstActivityStartedReported(activityA)

        activityLifecycleCallbacks.onActivityStarted(activityB)
        verifyActivityLifecycleChangeIgnored(activityB)
        activityLifecycleCallbacks.onActivityStopped(activityA)
        verifyActivityLifecycleChangeIgnored(activityA)

        activityLifecycleCallbacks.onActivityStopped(activityB)
        verifyLastActivityStoppedReported(activityB)
        activityLifecycleCallbacks.onActivityStarted(activityB)
        verifyFirstActivityStartedReported(activityB)

        activityLifecycleCallbacks.onActivityStarted(activityA)
        verifyActivityLifecycleChangeIgnored(activityA)
        activityLifecycleCallbacks.onActivityStopped(activityB)
        verifyActivityLifecycleChangeIgnored(activityB)
    }

    @Test
    fun `two activities started, then a config change happened and then both activities stopped`() {
        val activityLifecycleCallbacks = ActivityLifecycleCallbacksImpl(callback, config)

        val activityA = mock<Activity>()
        val activityB = mock<Activity>()

        activityLifecycleCallbacks.onActivityStarted(activityA)
        verifyFirstActivityStartedReported(activityA)

        activityLifecycleCallbacks.onActivityStarted(activityB)
        verifyActivityLifecycleChangeIgnored(activityB)
        activityLifecycleCallbacks.onActivityStopped(activityA)
        verifyActivityLifecycleChangeIgnored(activityA)

        activityLifecycleCallbacks.onActivityStopped(activityB)
        verifyLastActivityStoppedReported(activityB)
        activityLifecycleCallbacks.onActivityStarted(activityB)
        verifyFirstActivityStartedReported(activityB)

        activityLifecycleCallbacks.onActivityStarted(activityA)
        verifyActivityLifecycleChangeIgnored(activityA)
        activityLifecycleCallbacks.onActivityStopped(activityB)
        verifyActivityLifecycleChangeIgnored(activityB)

        activityLifecycleCallbacks.onActivityStopped(activityA)
        verifyLastActivityStoppedReported(activityA)
    }

    @Test
    fun `unrelated activity lifecycle changes must be ignored`() {
        val activityLifecycleCallbacks = ActivityLifecycleCallbacksImpl(callback, config)

        val activity = mock<Activity>()
        val outState = mock<Bundle>()
        val savedInstanceState = mock<Bundle>()

        activityLifecycleCallbacks.onActivityCreated(activity, savedInstanceState)
        // activityLifecycleCallbacks.onActivityStarted(activity)
        activityLifecycleCallbacks.onActivityResumed(activity)
        activityLifecycleCallbacks.onActivityPaused(activity)
        activityLifecycleCallbacks.onActivitySaveInstanceState(activity, outState)
        // activityLifecycleCallbacks.onActivityStopped(activity)
        activityLifecycleCallbacks.onActivityDestroyed(activity)

        verifyNoMoreInteractions(callback, config, activity, outState, savedInstanceState)
    }

    private fun verifyFirstActivityStartedReported(activity: Activity) {
        inOrder(callback, config).also {
            verify(config).shouldActivityBeProcessed(activity)
            verify(callback).onStartedActivityPresent()
        }
        verifyNoMoreInteractions(callback, config)
        clearInvocations(callback, config)
    }

    private fun verifyLastActivityStoppedReported(activity: Activity) {
        inOrder(callback, config).also {
            verify(config).shouldActivityBeProcessed(activity)
            verify(callback).onStartedActivityAbsent()
        }
        verifyNoMoreInteractions(callback, config)
        clearInvocations(callback, config)
    }

    private fun verifyActivityLifecycleChangeIgnored(activity: Activity) {
        verify(config).shouldActivityBeProcessed(activity)
        verifyNoMoreInteractions(config, callback)
        clearInvocations(config)
    }
}