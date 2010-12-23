package net.tokyoenvious.droid.pictumblr

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import android.view._
import android.widget.ImageView
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import android.graphics._

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

        val layout = findViewById(R.id.layout_main).asInstanceOf[android.view.ViewGroup]
        assert(layout != null, "layout defined")

        try {
            val task = new LoadDashboardTask(tumblr, layout)
            task.execute("1")
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
            }
        )
    }

    def toast (message : String) {
        Log.i("toast", message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show
    }
}

// FIXME AsyncTask[Int, ...] だと落ちる
class LoadDashboardTask (tumblr : Tumblr, viewGroup : ViewGroup)
        extends AsyncTask[String, java.lang.Void, Seq[Tumblr#Post]] {

    override def doInBackground (page : String) : Seq[Tumblr#Post] = {
        Log.d("LoadDashboardTask", "doInBackground")
        return tumblr.dashboard()
    }

    override def onPostExecute (posts : Seq[Tumblr#Post]) {
        // FIXME post match { case Tumblr#Post(url) => ... } できない
        posts foreach (
            post => {
                Log.d("LoadDashboardTask", "post: " + post.toString())

                val url = post.asInstanceOf[Tumblr#PhotoPost].photoUrl
                Log.d("LoadDashboardTask", "photoUrl: " + url)

                val imageView = new ImageView(viewGroup.getContext())
                viewGroup.addView(imageView)

                val task = new LoadPhotoTask(imageView)
                task.execute(url)
            }
        )
    }
}

class LoadPhotoTask (imageView : ImageView) extends AsyncTask[String, java.lang.Void, Bitmap] {
    override def doInBackground (url : String) : Bitmap = {
        val options = new BitmapFactory.Options
        options.inPreferredConfig = Bitmap.Config.RGB_565
        return BitmapFactory.decodeStream(
            new java.net.URL(url).openConnection.getInputStream, null, options
        )
    }

    override def onPostExecute (bitmap : Bitmap) {
        imageView.setImageBitmap(bitmap)
    }
}
