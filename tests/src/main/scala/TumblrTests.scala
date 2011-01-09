package net.tokyoenvious.droid.pictumblr.tests

import _root_.android.test.AndroidTestCase
import org.scalatest.junit.ShouldMatchersForJUnit

import net.tokyoenvious.droid.pictumblr.Tumblr

class TumblrTests extends AndroidTestCase with ShouldMatchersForJUnit {

    def testPackageIsCorrect {
        getContext.getPackageName should be ("net.tokyoenvious.droid.pictumblr")
    }

    def testTumblrPhotoPost {
        val tumblr = new Tumblr("", "")
        val post = new tumblr.PhotoPost(
            666,
            "reblog-key",
            "http://localhost/url-with-slug",
            "http://localhost/photo-url",
            Some("http://localhost/photo-link-url"),
            "photo-caption&lt;&#100;&gt;"
        )
        post should not be (null)
        post.plainCaption should be ("photo-caption<d>")
    }

}
