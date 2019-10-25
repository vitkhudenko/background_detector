package vit.khudenko.android.backgrounddetector

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.nhaarman.mockitokotlin2.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.util.concurrent.atomic.AtomicReference

class BackgroundDetectorTest {

    @get:Rule
    val expectedExceptionRule: ExpectedException = ExpectedException.none()

    private lateinit var uiHandler: Handler
    private lateinit var mainLooperMock: Looper
    private lateinit var application: Application
    private lateinit var config: BackgroundDetectorConfig

    private val activityLifecycleCallbacksRef = AtomicReference<Application.ActivityLifecycleCallbacks>()
    private val statusChangeActionRef = AtomicReference<Runnable>()

    @Before
    fun setUp() {
        statusChangeActionRef.set(null)
        activityLifecycleCallbacksRef.set(null)
        mainLooperMock = mock()
        application = mock {
            on { registerActivityLifecycleCallbacks(any()) } doAnswer { invocationOnMock ->
                activityLifecycleCallbacksRef.set(invocationOnMock.getArgument<Application.ActivityLifecycleCallbacks>(0))
                Unit
            }
            on { mainLooper } doReturn mainLooperMock
        }
        config = mock {
            on { shouldActivityBeProcessed(any()) } doReturn true
        }
        uiHandler = mock {
            on { looper } doReturn mainLooperMock
            on { postDelayed(any(), any()) } doAnswer { invocationOnMock ->
                statusChangeActionRef.set(invocationOnMock.getArgument<Runnable>(0))
                true
            }
            on { removeCallbacks(any()) } doAnswer { invocationOnMock ->
                if (invocationOnMock.getArgument<Runnable>(0) === statusChangeActionRef.get()) {
                    statusChangeActionRef.set(null)
                }
                Unit
            }
        }
    }

    @Test
    fun `instantiation must fail if uiHandler param is not bound to main thread`() {
        uiHandler = mock {
            on { looper } doReturn mock()
        }

        expectedExceptionRule.expect(IllegalArgumentException::class.java)
        expectedExceptionRule.expectMessage("uiHandler must use the main thread android.os.Looper (see android.os.Looper.getMainLooper())")

        BackgroundDetector(application, uiHandler, config)
    }

    @Test
    fun `newly instantiated detector should report background status`() {
        val detector = BackgroundDetector(application, uiHandler, config)
        verifyBackground(detector)

        inOrder(application, uiHandler).also {
            verify(application).mainLooper
            verify(uiHandler).looper
            verify(application).registerActivityLifecycleCallbacks(any())
        }

        verifyNoMoreInteractions(application, uiHandler)
        verifyZeroInteractions(config)
    }

    @Test
    fun `detector should report foreground status after user starts a single activity`() {
        val activity = mock<Activity>()
        val detector = BackgroundDetector(application, uiHandler, config)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        verifyBackground(detector)
        clearInvocations(application, uiHandler, config)

        // user starts an activity
        activityLifecycleCallbacks.onActivityStarted(activity)
        verifyBackground(detector)
        verifyStatusChangeScheduled(activity)
        runStatusChangeAction()
        verifyForeground(detector)

        verifyNoMoreInteractions(application, uiHandler, config)
    }

    @Test
    fun `detector should report background status user presses back having a single activity`() {
        val activity = mock<Activity>()
        val detector = BackgroundDetector(application, uiHandler, config)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        verifyBackground(detector)
        clearInvocations(application, uiHandler, config)

        // user starts an activity
        activityLifecycleCallbacks.onActivityStarted(activity)
        verifyStatusChangeScheduled(activity)
        verifyBackground(detector)
        runStatusChangeAction()
        verifyForeground(detector)

        // user presses Back (or puts the app to background)
        activityLifecycleCallbacks.onActivityStopped(activity)
        verifyStatusChangeScheduled(activity)
        verifyForeground(detector)
        runStatusChangeAction()
        verifyBackground(detector)

        verifyNoMoreInteractions(application, uiHandler, config)
    }

    @Test
    fun `detector should still report foreground status after user starts another activity`() {
        val activityA = mock<Activity>()
        val activityB = mock<Activity>()

        val detector = BackgroundDetector(application, uiHandler, config)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        clearInvocations(application, uiHandler, config)
        verifyBackground(detector)

        // user starts an activity
        activityLifecycleCallbacks.onActivityStarted(activityA)
        verifyStatusChangeScheduled(activityA)
        verifyBackground(detector)
        runStatusChangeAction()
        verifyForeground(detector)

        // user starts yet another activity
        activityLifecycleCallbacks.onActivityStarted(activityB)
        verifyStatusChangeNotScheduled(activityB)
        verifyForeground(detector)
        activityLifecycleCallbacks.onActivityStopped(activityA)
        verifyStatusChangeNotScheduled(activityA)
        verifyForeground(detector)

        verifyNoMoreInteractions(application, uiHandler, config)
    }

    @Test
    fun `detector should still report foreground status after user goes back from activity B to activity A`() {
        val activityA = mock<Activity>()
        val activityB = mock<Activity>()

        val detector = BackgroundDetector(application, uiHandler, config)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        clearInvocations(application, uiHandler, config)
        verifyBackground(detector)

        // user starts an activity
        activityLifecycleCallbacks.onActivityStarted(activityA)

        verifyStatusChangeScheduled(activityA)
        verifyBackground(detector)
        runStatusChangeAction()
        verifyForeground(detector)

        // user starts yet another activity
        activityLifecycleCallbacks.onActivityStarted(activityB)
        verifyStatusChangeNotScheduled(activityB)
        verifyForeground(detector)
        activityLifecycleCallbacks.onActivityStopped(activityA)
        verifyStatusChangeNotScheduled(activityA)
        verifyForeground(detector)

        // user presses Back (returns back to the first activity)
        activityLifecycleCallbacks.onActivityStarted(activityA)
        verifyStatusChangeNotScheduled(activityA)
        verifyForeground(detector)
        activityLifecycleCallbacks.onActivityStopped(activityB)
        verifyStatusChangeNotScheduled(activityB)
        verifyForeground(detector)

        verifyNoMoreInteractions(application, uiHandler, config)
    }

    //////////////////////// config without status change debouncing ////////////////////////

    @Test
    fun `detector should report foreground status after user starts a single activity - without status change debouncing`() {
        val activity = mock<Activity>()
        val detector = BackgroundDetector(application, uiHandler, config, 0 /* debounceDelayMillis */)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        verifyBackground(detector)
        clearInvocations(application, uiHandler, config)

        // user starts an activity
        activityLifecycleCallbacks.onActivityStarted(activity)
        verifyStatusChangedWithoutDebouncing(activity)
        verifyForeground(detector)

        verifyNoMoreInteractions(application, uiHandler, config)
    }

    @Test
    fun `detector should report background status user presses back having a single activity - without status change debouncing`() {
        val activity = mock<Activity>()
        val detector = BackgroundDetector(application, uiHandler, config, 0 /* debounceDelayMillis */)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        verifyBackground(detector)
        clearInvocations(application, uiHandler, config)

        // user starts an activity
        activityLifecycleCallbacks.onActivityStarted(activity)
        verifyStatusChangedWithoutDebouncing(activity)
        verifyForeground(detector)

        // user presses Back (or puts the app to background)
        activityLifecycleCallbacks.onActivityStopped(activity)
        verifyStatusChangedWithoutDebouncing(activity)
        verifyBackground(detector)

        verifyNoMoreInteractions(application, uiHandler, config)
    }

    @Test
    fun `detector should still report foreground status after user starts another activity - without status change debouncing`() {
        val activityA = mock<Activity>()
        val activityB = mock<Activity>()

        val detector = BackgroundDetector(application, uiHandler, config, 0 /* debounceDelayMillis */)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        clearInvocations(application, uiHandler, config)
        verifyBackground(detector)

        // user starts an activity
        activityLifecycleCallbacks.onActivityStarted(activityA)
        verifyStatusChangedWithoutDebouncing(activityA)
        verifyForeground(detector)

        // user starts yet another activity
        activityLifecycleCallbacks.onActivityStarted(activityB)
        verifyStatusChangeNotScheduled(activityB)
        verifyForeground(detector)
        activityLifecycleCallbacks.onActivityStopped(activityA)
        verifyStatusChangeNotScheduled(activityA)
        verifyForeground(detector)

        verifyNoMoreInteractions(application, uiHandler, config)
    }

    @Test
    fun `detector should still report foreground status after user goes back from activity B to activity A - without status change debouncing`() {
        val activityA = mock<Activity>()
        val activityB = mock<Activity>()

        val detector = BackgroundDetector(application, uiHandler, config, 0 /* debounceDelayMillis */)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        clearInvocations(application, uiHandler, config)
        verifyBackground(detector)

        // user starts an activity
        activityLifecycleCallbacks.onActivityStarted(activityA)

        verifyStatusChangedWithoutDebouncing(activityA)
        verifyForeground(detector)

        // user starts yet another activity
        activityLifecycleCallbacks.onActivityStarted(activityB)
        verifyStatusChangeNotScheduled(activityB)
        verifyForeground(detector)
        activityLifecycleCallbacks.onActivityStopped(activityA)
        verifyStatusChangeNotScheduled(activityA)
        verifyForeground(detector)

        // user presses Back (returns back to the first activity)
        activityLifecycleCallbacks.onActivityStarted(activityA)
        verifyStatusChangeNotScheduled(activityA)
        verifyForeground(detector)
        activityLifecycleCallbacks.onActivityStopped(activityB)
        verifyStatusChangeNotScheduled(activityB)
        verifyForeground(detector)

        verifyNoMoreInteractions(application, uiHandler, config)
    }

    ///////////////////////////////////// custom config /////////////////////////////////////

    @Test
    fun `detector should still report background status after user starts a single activity that is configured to be ignored`() {
        val activity = mock<Activity>()
        config = mock {
            on { shouldActivityBeProcessed(any()) } doReturn false
        }
        val detector = BackgroundDetector(application, uiHandler, config)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        verifyBackground(detector)
        clearInvocations(application, uiHandler, config)

        // user starts an activity
        activityLifecycleCallbacks.onActivityStarted(activity)
        verifyStatusChangeNotScheduled(activity)
        verifyBackground(detector)

        verifyNoMoreInteractions(application, uiHandler, config)
    }

    @Test
    fun `detector should still report background status user presses back having a single activity that is configured to be ignored`() {
        val activity = mock<Activity>()
        config = mock {
            on { shouldActivityBeProcessed(any()) } doReturn false
        }
        val detector = BackgroundDetector(application, uiHandler, config)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        verifyBackground(detector)
        clearInvocations(application, uiHandler, config)

        // user starts an activity
        activityLifecycleCallbacks.onActivityStarted(activity)
        verifyStatusChangeNotScheduled(activity)
        verifyBackground(detector)

        // user presses Back (or puts the app to background)
        activityLifecycleCallbacks.onActivityStopped(activity)
        verifyStatusChangeNotScheduled(activity)
        verifyBackground(detector)

        verifyNoMoreInteractions(application, uiHandler, config)
    }

    @Test
    fun `detector should report foreground status after user starts another activity that is not configured to be ignored`() {
        val activityA = mock<Activity>()
        val activityB = mock<Activity>()
        config = mock {
            on { shouldActivityBeProcessed(any()) } doAnswer { invocationOnMock ->
                invocationOnMock.getArgument<Activity>(0) === activityB
            }
        }

        val detector = BackgroundDetector(application, uiHandler, config)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        clearInvocations(application, uiHandler, config)
        verifyBackground(detector)

        // user starts an activity
        activityLifecycleCallbacks.onActivityStarted(activityA)
        verifyStatusChangeNotScheduled(activityA)
        verifyBackground(detector)

        // user starts yet another activity
        activityLifecycleCallbacks.onActivityStarted(activityB)
        verifyStatusChangeScheduled(activityB)
        verifyBackground(detector)
        runStatusChangeAction()
        verifyForeground(detector)

        activityLifecycleCallbacks.onActivityStopped(activityA)
        verifyStatusChangeNotScheduled(activityA)
        verifyForeground(detector)

        verifyNoMoreInteractions(application, uiHandler, config)
    }

    /////////////////////////////////////// listeners ///////////////////////////////////////

    @Test
    fun `addListener - one listener`() {
        val detector = BackgroundDetector(application, uiHandler, config)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        val listener = mock<BackgroundDetector.Listener>()
        detector.addListener(listener)

        activityLifecycleCallbacks.onActivityStarted(mock())
        runStatusChangeAction()

        verifyForeground(detector)

        verify(listener).onForeground()
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun `addListener - adding twice the same listener`() {
        val detector = BackgroundDetector(application, uiHandler, config)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        val listener = mock<BackgroundDetector.Listener>()
        detector.addListener(listener)
        detector.addListener(listener)

        activityLifecycleCallbacks.onActivityStarted(mock())
        runStatusChangeAction()

        verifyForeground(detector)

        verify(listener).onForeground()
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun `addListener - one listener - notification on background`() {
        val detector = BackgroundDetector(application, uiHandler, config)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        val activity = mock<Activity>()
        val listener = mock<BackgroundDetector.Listener>()

        activityLifecycleCallbacks.onActivityStarted(activity)
        runStatusChangeAction()
        verifyForeground(detector)

        detector.addListener(listener)
        verifyZeroInteractions(listener)

        activityLifecycleCallbacks.onActivityStopped(activity)
        runStatusChangeAction()
        verifyBackground(detector)

        verify(listener).onBackground()
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun `addListener - two listeners`() {
        val detector = BackgroundDetector(application, uiHandler, config)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        val listener1 = mock<BackgroundDetector.Listener>()
        val listener2 = mock<BackgroundDetector.Listener>()

        detector.addListener(listener1)
        detector.addListener(listener2)

        activityLifecycleCallbacks.onActivityStarted(mock())
        runStatusChangeAction()

        verifyForeground(detector)

        inOrder(listener1, listener2).also {
            verify(listener1).onForeground()
            verify(listener2).onForeground()
        }
        verifyNoMoreInteractions(listener1, listener2)
    }

    @Test
    fun removeAllListeners() {
        val listener1 = mock<BackgroundDetector.Listener>()
        val listener2 = mock<BackgroundDetector.Listener>()

        val detector = BackgroundDetector(application, uiHandler, config)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        detector.addListener(listener1)
        detector.addListener(listener2)

        detector.removeAllListeners()

        activityLifecycleCallbacks.onActivityStarted(mock())
        runStatusChangeAction()

        verifyForeground(detector)

        verifyZeroInteractions(listener1, listener2)
    }

    @Test
    fun `removeListener - remove all listeners one by one`() {
        val listener1 = mock<BackgroundDetector.Listener>()
        val listener2 = mock<BackgroundDetector.Listener>()

        val detector = BackgroundDetector(application, uiHandler, config)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        detector.addListener(listener1)
        detector.addListener(listener2)

        detector.removeListener(listener1)
        detector.removeListener(listener2)

        activityLifecycleCallbacks.onActivityStarted(mock())
        runStatusChangeAction()

        verifyForeground(detector)

        verifyZeroInteractions(listener1, listener2)
    }

    @Test
    fun `removeListener - remove one of the two listeners`() {
        val listener1 = mock<BackgroundDetector.Listener>()
        val listener2 = mock<BackgroundDetector.Listener>()

        val detector = BackgroundDetector(application, uiHandler, config)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()

        detector.addListener(listener1)
        detector.addListener(listener2)

        detector.removeListener(listener1)

        activityLifecycleCallbacks.onActivityStarted(mock())
        runStatusChangeAction()

        verifyForeground(detector)

        verifyZeroInteractions(listener1)
        verify(listener2).onForeground()
    }

    ///////////////////// unrelated activity lifecycle changes //////////////////////

    @Test
    fun `unrelated activity lifecycle changes must be ignored`() {
        val detector = BackgroundDetector(application, uiHandler, config)
        val activityLifecycleCallbacks = activityLifecycleCallbacksRef.get()
        clearInvocations(application, uiHandler, config)

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

        verifyZeroInteractions(application, uiHandler, config, activity, outState, savedInstanceState)
        verifyBackground(detector)
        assertNull(statusChangeActionRef.get())
    }

    private fun verifyForeground(detector: BackgroundDetector) {
        assertTrue(detector.isAppInForeground())
        assertFalse(detector.isAppInBackground())
    }

    private fun verifyBackground(detector: BackgroundDetector) {
        assertTrue(detector.isAppInBackground())
        assertFalse(detector.isAppInForeground())
    }

    private fun verifyStatusChangeScheduled(activity: Activity) {
        inOrder(uiHandler, config).also {
            verify(config).shouldActivityBeProcessed(activity)
            verify(uiHandler, times(2)).removeCallbacks(any())
            verify(uiHandler).postDelayed(any(), eq(BackgroundDetector.DEFAULT_DEBOUNCE_DELAY_MILLIS))
        }

        verifyNoMoreInteractions(uiHandler, config)
        verifyZeroInteractions(application)

        clearInvocations(application, uiHandler, config)
    }

    private fun verifyStatusChangeNotScheduled(activity: Activity) {
        verify(config).shouldActivityBeProcessed(activity)
        verifyNoMoreInteractions(config)
        verifyZeroInteractions(application, uiHandler)

        clearInvocations(application, uiHandler, config)

        assertNull(statusChangeActionRef.get())
    }

    private fun verifyStatusChangedWithoutDebouncing(activity: Activity) {
        assertNull(statusChangeActionRef.get())

        inOrder(config, uiHandler).also {
            verify(config).shouldActivityBeProcessed(activity)
            verify(uiHandler, times(2)).removeCallbacks(any())
        }

        verifyNoMoreInteractions(uiHandler, config)
        verifyZeroInteractions(application)

        clearInvocations(application, uiHandler, config)
    }

    private fun runStatusChangeAction() {
        statusChangeActionRef.get().run()
        statusChangeActionRef.set(null)
    }
}