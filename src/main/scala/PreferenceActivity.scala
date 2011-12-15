package net.tokyoenvious.droid.pictumblr

import android.preference
import android.preference.PreferenceActivity
import android.preference.Preference
import android.content.SharedPreferences
import android.app.AlertDialog;

class PicTumblrPrefernceActivity extends PreferenceActivity
        with TumblrOAuthable {

    override def onCreate (savedInstanceState : android.os.Bundle) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.layout.preference)

        var baseHostnamePref = getPreferenceScreen().findPreference("base_hostname")
        baseHostnamePref.setSummary(baseHostnamePref.getSharedPreferences().getString("base_hostname", ""))
        baseHostnamePref.setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener () {
                override def onPreferenceClick (preference : Preference) = {
                    PicTumblrPrefernceActivity.this.showEraseAuthTokensDialog()
                    true
                }
            }
        )
    }

    def showEraseAuthTokensDialog () {
        new AlertDialog.Builder(this)
            // .setTitle("Erase tokens")
            .setMessage("Erase auth tokens?")
            .setPositiveButton("Erase", new android.content.DialogInterface.OnClickListener () {
                override def onClick (diaglog : android.content.DialogInterface, which : Int) {
                    PicTumblrPrefernceActivity.this.eraseAuthTokens()
                }
            })
            .setNegativeButton("Cancel", new android.content.DialogInterface.OnClickListener () {
                override def onClick (diaglog : android.content.DialogInterface, which : Int) {
                }
            })
            .show()
    }
}

