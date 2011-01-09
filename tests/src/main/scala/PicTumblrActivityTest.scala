package net.tokyoenvious.droid.pictumblr.tests

import android.test.ActivityInstrumentationTestCase2
import android.content.Intent
import org.scalatest.junit.ShouldMatchersForJUnit

import net.tokyoenvious.droid.pictumblr.PicTumblrActivity

class PicTumblrActivityTest
    extends ActivityInstrumentationTestCase2[PicTumblrActivity]("net.tokyoenvious.droid.pictumblr", classOf[PicTumblrActivity])
    with ShouldMatchersForJUnit  {

    // なんか毎回 onCreate が走っている気がする…… → そういうものです
    lazy val activity = getActivity()

    lazy val preferences = android.preference.PreferenceManager.getDefaultSharedPreferences(activity)
    lazy val originalEmail    = preferences.getString("email", "")
    lazy val originalPassword = preferences.getString("password", "")

    override def setUp () {
        super.setUp

        val intent = new Intent(Intent.ACTION_MAIN)
        intent.putExtra("net.tokyoenvious.droid.pictumblr.tests.noLoadOnCreate", true)

        setActivityIntent(intent)

        originalEmail
        originalPassword
    }

    override def tearDown () {
        val editor = preferences.edit
        editor.putString("email", originalEmail)
        editor.putString("password", originalPassword)
        editor.commit

        super.tearDown
    }

    def testTaskGroupCallback {
        var called = false
        val taskGroup = new activity.TaskGroup({ called = true })

        called should be (false)
        taskGroup.count should be (0)

        taskGroup.begin()
        called should be (false)
        taskGroup.count should be (1)

        taskGroup.begin()
        called should be (false)
        taskGroup.count should be (2)

        taskGroup.end()
        called should be (false)
        taskGroup.count should be (1)

        taskGroup.begin()
        called should be (false)
        taskGroup.count should be (2)

        taskGroup.end()
        called should be (false)
        taskGroup.count should be (1)

        taskGroup.end()
        called should be (true)
        taskGroup.count should be (0)
    }

    def testTumblrAuthFailure {
        val editor = preferences.edit
        editor.putString("email", "bad email")
        editor.putString("password", "bad password")
        editor.commit

        val tumblrOption = activity.getTumblr()
        tumblrOption.isDefined should be (true)

        val tumblr = tumblrOption.get
        val result = tumblr.dashboard()

        result.isLeft should be (true)
        result.left.get should equal ("Invalid credentials.")
    }
}
