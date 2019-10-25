package vit.khudenko.android.backgrounddetector

import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultBackgroundDetectorConfigTest {

    private val config: BackgroundDetectorConfig = BackgroundDetectorConfig.Default()

    @Test
    fun shouldActivityBeProcessed() {
        assertTrue(config.shouldActivityBeProcessed(mock()))
    }
}