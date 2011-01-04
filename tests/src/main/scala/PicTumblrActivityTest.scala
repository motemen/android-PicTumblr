package net.tokyoenvious.droid.pictumblr.tests

import android.test.ActivityInstrumentationTestCase2
import junit.framework.Assert._
import net.tokyoenvious.droid.pictumblr.PicTumblrActivity

class PicTumblrActivityTest
    extends ActivityInstrumentationTestCase2[PicTumblrActivity]("net.tokyoenvious.droid.pictumblr", classOf[PicTumblrActivity])
    {

    // なんか毎回 onCreate が走っている気がする……
    lazy val activity = getActivity()

    def testTumblr {
        val tumblrOption = activity.getTumblr()
        assertTrue(tumblrOption isDefined)

        val tumblr = tumblrOption.get
        val posts  = tumblr.dashboard()
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
}
