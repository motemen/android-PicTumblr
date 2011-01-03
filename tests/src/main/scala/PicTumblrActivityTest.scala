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
}
