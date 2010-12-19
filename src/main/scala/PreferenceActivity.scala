package net.tokyoenvious.droid.pictumblr

import android.preference.PreferenceActivity
import android.os.Bundle
import android.widget
import android.view.View
import android.view.View.OnClickListener

class PicTumblrPrefernceActivity extends PreferenceActivity {

    override def onCreate(savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.layout.preference)
    }
}

