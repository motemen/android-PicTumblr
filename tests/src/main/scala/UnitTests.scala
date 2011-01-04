package net.tokyoenvious.droid.pictumblr.tests

import junit.framework.Assert._
import _root_.android.test.AndroidTestCase

import net.tokyoenvious.droid.pictumblr.TaskGroup

class UnitTests extends AndroidTestCase {

    def testPackageIsCorrect {
        assertEquals("net.tokyoenvious.droid.pictumblr", getContext.getPackageName)
    }

    def testTaskGroupCallback {
        var called = false
        val taskGroup = new TaskGroup({ called = true })

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
