package com.jovinyap.productchallenge

import android.content.Intent
import android.view.ViewGroup
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
@Config(sdk = [23, 35])
class MainActivityTest {
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
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            assertTrue(content.childCount > 0)
        }
    }
}

