import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import net.tokyoenvious.droid.pictumblr.Tumblr

class TumblrSpec extends FlatSpec with ShouldMatchers {
    "Valid authentication" should "succeed" in {
        val email    = System.getenv("TUMBLR_EMAIL")
        val password = System.getenv("TUMBLR_PASSWORD")
        val tumblr = new Tumblr(email, password)

        tumblr.authenticate().isDefined should equal (true)
    }

    "Invalid authentication" should "fail" in {
        val tumblr = new Tumblr("", "")

        tumblr.authenticate() should equal (None)
    }

    "Reading dashboard" should "succeed" in {
        val email    = System.getenv("TUMBLR_EMAIL")
        val password = System.getenv("TUMBLR_PASSWORD")
        val tumblr = new Tumblr(email, password)

        val res = tumblr.dashboard
        println(res)
    }
}
