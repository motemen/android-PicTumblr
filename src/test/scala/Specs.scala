import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import net.tokyoenvious.droid.pictumblr.Tumblr

class TumblrSpec extends FlatSpec with ShouldMatchers {
    "Valid authentication" should "succeed" in {
        val email    = System.getenv("TUMBLR_EMAIL")
        val password = System.getenv("TUMBLR_PASSWORD")

        Tumblr.authenticate(email, password).isDefined should equal (true)
    }
    "Invalid authentication" should "fail" in {
        Tumblr.authenticate("", "") should equal (None)
    }
}
