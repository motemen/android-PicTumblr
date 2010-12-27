package net.tokyoenvious.droid.pictumblr.tests

import junit.framework.Assert._
import _root_.android.test.AndroidTestCase

class UnitTests extends AndroidTestCase {
    def testPackageIsCorrect {
        assertEquals("net.tokyoenvious.droid.pictumblr", getContext.getPackageName)
    }

    override def $tag() : Int = {
        try {
            return super.$tag()
        } catch {
            case e: Exception => throw new RuntimeException(e)
        }
    }
}
