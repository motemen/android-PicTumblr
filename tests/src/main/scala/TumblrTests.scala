package net.tokyoenvious.droid.pictumblr.tests

import junit.framework.Assert._
import _root_.android.test.AndroidTestCase

import net.tokyoenvious.droid.pictumblr.Tumblr

class TumblrTests extends AndroidTestCase {

    def testPackageIsCorrect {
        assertEquals("net.tokyoenvious.droid.pictumblr", getContext.getPackageName)
    }

    def testTumblrPhotoPost {
        val tumblr = new Tumblr("", "")
        val post = new tumblr.PhotoPost(
            666,
            "reblog-key",
            "http://localhost/url-with-slug",
            "http://localhost/photo-url",
            Some("http://localhost/photo-link-url"),
            "photo-caption&lt;&#100;"
        )
        assertNotNull(post)
        assertEquals("photo-caption<d", post.plainCaption)
    }

}
