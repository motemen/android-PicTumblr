package net.tokyoenvious.droid.pictumblr.tests

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class TumblrSpec extends FlatSpec with ShouldMatchers {
    "Valid authentication" should "succeed" in {
        val email = System.getenv("TUMBLR_EMAIL")
        val password = System.getenv("TUMBLR_PASSWORD")
        Tumblr.authenticate(email, password).isDefined should equal (true)
    }

    "Invalid authentication" should "fail" in {
        Tumblr.authentication("", "") should equal (None)
    }
}
