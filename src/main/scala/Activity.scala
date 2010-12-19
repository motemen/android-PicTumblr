package net.tokyoenvious.droid.pictumblr

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import android.preference.PreferenceManager

class PicTumblrActivity extends Activity {
    val MENU_ITEM_ID_SETTING = Menu.FIRST + 1

    override def onCreate(savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
    }

    override def onCreateOptionsMenu(menu : Menu) : Boolean = {
        val itemSetting = menu.add(Menu.NONE, MENU_ITEM_ID_SETTING, Menu.NONE, "Setting")
        itemSetting.setIcon(android.R.drawable.ic_menu_preferences)

        return super.onCreateOptionsMenu(menu)
    }

    override def onOptionsItemSelected(menuItem : MenuItem) : Boolean = {
        menuItem.getItemId() match {
            case MENU_ITEM_ID_SETTING => {
                val intent = new Intent(this, classOf[PicTumblrPrefernceActivity])
                startActivity(intent)
            }
        }

        return true
    }
}
