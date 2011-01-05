package net.tokyoenvious.droid.pictumblr.tests

import android.test.ActivityInstrumentationTestCase2
import junit.framework.Assert._
import net.tokyoenvious.droid.pictumblr.PicTumblrActivity

class PicTumblrActivityTest
    extends ActivityInstrumentationTestCase2[PicTumblrActivity]("net.tokyoenvious.droid.pictumblr", classOf[PicTumblrActivity])
    {

    // なんか毎回 onCreate が走っている気がする……
    lazy val activity = getActivity()

    lazy val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(activity)
    lazy val originalEmail    = prefs.getString("email", "")
    lazy val originalPassword = prefs.getString("password", "")

    override def setUp () {
        originalEmail
        originalPassword
    }

    override def tearDown () {
        val editor = prefs.edit
        editor.putString("email", originalEmail)
        editor.putString("password", originalPassword)
        editor.commit
    }

    def testTumblr {
        val tumblrOption = activity.getTumblr()
        assertTrue(tumblrOption isDefined)

        val tumblr = tumblrOption.get
        val result = tumblr.dashboard()
        assertTrue(result isRight)
    }

    def testTaskGroupCallback {
        var called = false
        val taskGroup = new activity.TaskGroup({ called = true })

        assertFalse(called)
        assertEquals(taskGroup.count, 0)

        taskGroup.begin()
        assertFalse(called)
        assertEquals(taskGroup.count, 1)

        taskGroup.begin()
        assertFalse(called)
        assertEquals(taskGroup.count, 2)

        taskGroup.end()
        assertFalse(called)
        assertEquals(taskGroup.count, 1)

        taskGroup.begin()
        assertFalse(called)
        assertEquals(taskGroup.count, 2)

        taskGroup.end()
        assertFalse(called)
        assertEquals(taskGroup.count, 1)

        taskGroup.end()
        assertTrue(called)
        assertEquals(taskGroup.count, 0)
    }

    def testTumblrAuthFailure {
        val editor = prefs.edit
        editor.putString("email", "bad email")
        editor.putString("password", "bad password")
        editor.commit

        val tumblrOption = activity.getTumblr()
        assertTrue(tumblrOption isDefined)

        val tumblr = tumblrOption.get
        val result = tumblr.dashboard()

        assertTrue(result isLeft)
        assertEquals("Invalid credentials.", result.left.get)
    }
}
