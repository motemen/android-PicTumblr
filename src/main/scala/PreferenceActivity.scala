package net.tokyoenvious.droid.pictumblr

import android.preference
import android.preference.PreferenceActivity
import android.preference.EditTextPreference
import android.content.SharedPreferences
import android.os.Bundle
import android.widget
import android.view.View
import android.view.View.OnClickListener

class PicTumblrPrefernceActivity extends PreferenceActivity
    with SharedPreferences.OnSharedPreferenceChangeListener {

    override def onCreate (savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.layout.preference)
        updateSummaries()
    }

    override def onSharedPreferenceChanged (sharedPreferences : SharedPreferences, key : String) {
        updateSummaries()
    }

    override def onResume () {
        super.onResume()
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this)
    }

    override def onPause () {
        super.onPause()
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this)
    }

    def updateSummaries () {
        val email = getPreferenceScreen().findPreference("email").asInstanceOf[EditTextPreference]
        email.setSummary(email.getText())

        val password = getPreferenceScreen().findPreference("password").asInstanceOf[EditTextPreference]
        val passwordValue = password.getText()
        password.setSummary(if (passwordValue != null && passwordValue.length > 0) "*******" else "")
    }
}

