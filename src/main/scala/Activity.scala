package net.tokyoenvious.droid.pictumblr

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import android.preference.PreferenceManager

class PicTumblrActivity extends Activity {
    override def onCreate(savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val email = prefs.getString("email", "")
        val password = prefs.getString("password", "")
        Toast.makeText(this, email+":"+password, Toast.LENGTH_SHORT).show
    }
}
