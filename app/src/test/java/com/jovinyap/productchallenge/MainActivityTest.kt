package com.jovinyap.productchallenge

import android.content.Intent
import android.view.ViewGroup
import androidx.core.view.WindowInsetsControllerCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23, 35], qualifiers = "night", application = MainActivityTest.TestApplication::class)
class MainActivityTest {
    /** Keep the launcher smoke test independent of the live catalogue. */
    class TestApplication : ProductApplication() {
        override val productRepository = object : ProductRepository {
            override suspend fun fetchProducts(): List<Product> = emptyList()
        }
    }

    @Test
    fun launcherResolvesToMainActivityAndCreatesContent() {
        val application = RuntimeEnvironment.getApplication()
        val launchIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(application.packageName)
        val resolved = application.packageManager.resolveActivity(launchIntent, 0)
        assertNotNull(resolved)
        assertEquals(MainActivity::class.java.name, resolved?.activityInfo?.name)

        Robolectric.buildActivity(MainActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            assertFalse(activity.isFinishing)
            // The catalogue uses a light theme, so status-bar icons must remain readable.
            assertTrue(WindowInsetsControllerCompat(activity.window, activity.window.decorView).isAppearanceLightStatusBars)
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            assertTrue(content.childCount > 0)
        }
    }
}

