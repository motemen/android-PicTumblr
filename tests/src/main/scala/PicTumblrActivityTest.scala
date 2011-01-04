package net.tokyoenvious.droid.pictumblr.tests

import android.test.ActivityInstrumentationTestCase2
import junit.framework.Assert._
import net.tokyoenvious.droid.pictumblr.PicTumblrActivity

class PicTumblrActivityTest
    extends ActivityInstrumentationTestCase2[PicTumblrActivity]("net.tokyoenvious.droid.pictumblr", classOf[PicTumblrActivity])
    {

    lazy val activity = getActivity()

    def testActivity {
        assertNotNull(activity)
    }

    def testTumblr {
        assertTrue(activity.getTumblr() isDefined)
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
