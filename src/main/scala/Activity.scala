package net.tokyoenvious.droid.pictumblr

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log

class PicTumblrActivity extends Activity {
    val MENU_ITEM_ID_REFRESH = Menu.FIRST + 1
    val MENU_ITEM_ID_SETTING = Menu.FIRST + 2

    override def onCreate (savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        updateDashboard()
    }

    override def onCreateOptionsMenu (menu : Menu) : Boolean = {
        val itemRefresh = menu.add(Menu.NONE, MENU_ITEM_ID_REFRESH, Menu.NONE, "Refresh")
        // itemRefresh.setIcon(android.R.drawable.ic_menu_refresh)

        val itemSetting = menu.add(Menu.NONE, MENU_ITEM_ID_SETTING, Menu.NONE, "Setting")
        itemSetting.setIcon(android.R.drawable.ic_menu_preferences)

        return super.onCreateOptionsMenu(menu)
    }

    override def onOptionsItemSelected (menuItem : MenuItem) : Boolean = {
        menuItem.getItemId() match {
            case MENU_ITEM_ID_REFRESH => updateDashboard
            case MENU_ITEM_ID_SETTING => startSettingActivity
        }

        return true
    }

    def startSettingActivity () {
        val intent = new Intent(this, classOf[PicTumblrPrefernceActivity])
        startActivity(intent)
    }

    def updateDashboard () {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val email    = prefs.getString("email", "")
        val password = prefs.getString("password", "")

        if (email.length == 0 || password.length == 0) {
            startSettingActivity()
            return // FIXME どうすべき?
        }

        toast("Logging in...")

        try {
            Tumblr.authenticate(email, password) match {
                case Some(title) => {
                    toast("authentication succeeded: " + title)
                    // TODO
                }
                case None => {
                    toast("authentication failed.")
                }
            }
        } catch {
            case e => {
                Log.e("PicTumblrActivity.updateDashboard", e.toString)
                toast("something went wrong: " + e.getMessage)
            }
        }
    }

    def toast (message : String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show
    }
}
