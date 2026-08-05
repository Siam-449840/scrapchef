package com.scrapchef.android

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainActivityTest {

    @Test
    fun testActivityStarts() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
        activity.create().start().resume()
    }
}
