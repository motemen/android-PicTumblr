package net.tokyoenvious.droid.pictumblr

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import android.graphics.drawable.Drawable

// import android.os.AsyncTask
import net.tokyoenvious.droid.pictumblr.AsyncTask

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
        itemRefresh.setIcon(R.drawable.ic_menu_refresh)

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

    def getTumblr () : Tumblr = {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val email    = prefs.getString("email", "")
        val password = prefs.getString("password", "")

        if (email.length == 0 || password.length == 0) {
            startSettingActivity()
            // TODO どうすべき？
        }

        return new Tumblr(email, password)
    }

    def login () {
        toast("Logging in...")

        val tumblr = getTumblr()

        try {
            tumblr.authenticate() match {
                case Some(title) => {
                    toast("Authentication succeeded: " + title)
                }
                case None => {
                    toast("Authentication failed.")
                }
            }
        } catch {
            case e => {
                Log.e("PicTumblrActivity.login", e.toString)
                toast("Something went wrong: " + e.getMessage)
            }
        }
    }

    def updateDashboard () {
        toast("Updating dashboard...")

        val tumblr = getTumblr()

        try {
            val posts = tumblr.dashboard()
            enqueuePostsToView(posts)
            toast("Updated.")
        } catch {
            case e => {
                Log.e("PicTumblrActivity.updateDashboard", e.toString)
                val stackTrace = e.getStackTrace
                stackTrace foreach { s => Log.d("PicTumblrActivity.updateDashboard", s.toString()) }
                toast("Something went wrong: " + e.getMessage)
            }
        }
    }

    def enqueuePostsToView(posts : Seq[Tumblr#Post]) {
        val layout = findViewById(R.id.layout_main).asInstanceOf[android.view.ViewGroup]
        assert(layout != null, "layout defined")

        posts foreach (
            post => {
                val url = post.asInstanceOf[Tumblr#PhotoPost].photoUrl
                Log.d("PicTumblrActivity.enqueuePostsToView", "photoUrl: " + url)

                val imageView = new ImageView(this)
                layout.addView(imageView)

                val task = new LoadPhotoTask(imageView)
                task.execute(url)

                /*
                val drawable = Drawable.createFromStream(
                    new java.net.URL(url).openConnection.getInputStream, url
                )
                if (drawable == null) {
                    Log.i("PicTumblrActivity.enqueuePostsToView", "drawable is null: " + url)
                } else {
                    val imageView = new ImageView(this)
                    imageView.setImageDrawable(drawable)
                    layout.addView(imageView)
                }
                */
            }
        )
    }

    def toast (message : String) {
        Log.i("toast", message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show
    }
}

class LoadPhotoTask (imageView : ImageView) extends AsyncTask[String, java.lang.Void, Drawable] {
    override def doInBackground (url : String) : Drawable = {
        return Drawable.createFromStream(
            new java.net.URL(url).openConnection.getInputStream, url
        )
    }

    override def onPostExecute (drawable : Drawable) {
        imageView.setImageDrawable(drawable)
    }
}
