package net.tokyoenvious.droid.pictumblr

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import android.view.{ View, ViewGroup, Menu, MenuItem }
import android.graphics.{ Bitmap, BitmapFactory }
import android.widget.{ Toast, ImageView, ProgressBar, ArrayAdapter, ListView }
import android.content.Context

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
        val tumblr = getTumblr()
        val listView = findViewById(R.id.layout_list).asInstanceOf[ListView]
        val adapter = new TumblrPostAdapter(this, R.layout.list_raw)
        listView.setAdapter(adapter)

        try {
            // XXX クロージャなんか渡して大丈夫なんだろうか…
            val task = new LoadDashboardTask(tumblr, this.toast(_), adapter)
            task.execute(1)
        } catch {
            case e => {
                Log.e("PicTumblrActivity.updateDashboard", e.toString)
                val stackTrace = e.getStackTrace
                stackTrace foreach { s => Log.d("PicTumblrActivity.updateDashboard", s.toString()) }
                toast("Something went wrong: " + e.getMessage)
            }
        }
    }

    def toast (message : String) {
        Log.i("toast", message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show
    }
}

class TumblrPostAdapter (context : Context, textVeiwResourceId : Int)
        extends ArrayAdapter[Bitmap](context, textVeiwResourceId) {

    // contentView は他のアイテムの場合もあるらしい
    override def getView (position : Int, contentView: View, parent : ViewGroup) : View = {
        val bitmap = getItem(position)
        if (bitmap == null) {
            return new ProgressBar(parent.getContext())
        } else {
            // TODO reuse
            val imageView = new ImageView(parent.getContext())
            imageView.setImageBitmap(bitmap)
            return imageView
        }
    }
}

// AsyncTask[Int, ...] だと落ちる → java.lang.Integer に
class LoadDashboardTask (tumblr : Tumblr, toast : String => Unit, adapter : TumblrPostAdapter)
        extends AsyncTask1[java.lang.Integer, java.lang.Void, Seq[Tumblr#Post]] {

    override def onPreExecute () {
        toast("Loading dashboard...")
    }

    // 可変長引数でやりとりできないのは AsyncTask1.java にブリッジさせる
    override def doInBackground (page : java.lang.Integer) : Seq[Tumblr#Post] = {
        Log.d("LoadDashboardTask", "doInBackground")
        // FIXME ここでエラーおきたときのハンドリング ふつうはどうするんだろう
        return tumblr.dashboard()
    }

    override def onPostExecute (posts : Seq[Tumblr#Post]) {
        toast("Dashboard loaded.")

        // post match { case Tumblr#PhotoPost(url) => ... } できない件は
        // post match { case tumblr.PhotoPost(url) => ... } でいける
        // ref. http://stackoverflow.com/questions/1812695/scala-case-class-matching-compile-error-with-aliased-inner-types
        posts foreach {
            case tumblr.PhotoPost(url) => {
                Log.d("LoadDashboardTask", "photoUrl: " + url)

                val task = new LoadPhotoTask(adapter)
                task.execute(url)
            }
            case post => {
                Log.d("LoadDashboardTask", "cannot handle post: " + post.toString())
            }
        }
    }
}

class LoadPhotoTask (adapter : TumblrPostAdapter)
        extends AsyncTask1[String, java.lang.Void, Bitmap] {

    // 単純に Drawable.createFromStream() するとメモリを食うので Bitmap.Config.RGB_565 を指定
    override def doInBackground (url : String) : Bitmap = {
        val options = new BitmapFactory.Options
        options.inPreferredConfig = Bitmap.Config.RGB_565

        return BitmapFactory.decodeStream(
            new java.net.URL(url).openConnection.getInputStream, null, options
        )
    }

    override def onPostExecute (bitmap : Bitmap) {
        adapter.add(bitmap)
    }
}
