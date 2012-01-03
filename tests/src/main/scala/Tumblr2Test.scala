package net.tokyoenvious.droid.pictumblr.tests

import android.test.AndroidTestCase
import junit.framework.Assert._

import org.json.JSONObject

import net.tokyoenvious.droid.pictumblr.Tumblr2
import net.tokyoenvious.droid.pictumblr.TumblrPhotoPost

class Tumblr2Test extends AndroidTestCase {

    def testPackageIsCorrect {
        assertEquals(getContext().getPackageName(), "net.tokyoenvious.droid.pictumblr")
    }

    def testTumblrPhotoPost {

         val jsonString = """
         {
            "blog_name": "derekg",
            "id": 7431599279,
            "post_url": "http:\/\/derekg.org\/post\/7431599279",
            "type": "photo",
            "date": "2011-07-09 22:09:47 GMT",
            "timestamp": 1310249387,
            "format": "html",
            "reblog_key": "749amggU",
            "tags": [],
            "note_count": 18,
            "caption": "<p>my arm is getting tired.<\/p>",
            "photos": [
               {
                  "caption": "",
                  "alt_sizes": [
                     {
                        "width": 1280,
                        "height": 722,
                        "url": "http:\/\/derekg.org\/photo\/1280\/7431599279\/1\/
                           tumblr_lo36wbWqqq1qanqww"
                     },
                     {
                        "width": 500,
                        "height": 282,
                        "url": "http:\/\/30.media.tumblr.com\/
                           tumblr_lo36wbWqqq1qanqwwo1_500.jpg"
                     },
                     {
                        "width": 400,
                        "height": 225,
                        "url": "http:\/\/29.media.tumblr.com\/
                           tumblr_lo36wbWqqq1qanqwwo1_400.jpg"
                     },
                     {
                        "width": 250,
                        "height": 141,
                        "url": "http:\/\/26.media.tumblr.com\/
                           tumblr_lo36wbWqqq1qanqwwo1_250.jpg"
                     },
                     {
                        "width": 100,
                        "height": 56,
                        "url": "http:\/\/24.media.tumblr.com\/
                           tumblr_lo36wbWqqq1qanqwwo1_100.jpg"
                     },
                     {
                        "width": 75,
                        "height": 75,
                        "url": "http:\/\/30.media.tumblr.com\/
                           tumblr_lo36wbWqqq1qanqwwo1_75sq.jpg"
                     }
                  ]
               }
            ]
         }
         """ // "

         val json = new JSONObject(jsonString)
         val post = new TumblrPhotoPost(json)

         assertEquals (post.plainCaption, "my arm is getting tired.")
    }

}

