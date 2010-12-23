package net.tokyoenvious.droid.pictumblr

import android.app.Activity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.{ View, ViewGroup, Menu, MenuItem, Display, LayoutInflater }
import android.graphics.{ Bitmap, BitmapFactory }
import android.widget.{ Toast, ImageView, ProgressBar, ArrayAdapter, ListView, AbsListView }
import android.content.{ Intent, Context }
import android.util.Log

class PicTumblrActivity extends Activity {
    val MENU_ITEM_ID_REFRESH = Menu.FIRST + 1
    val MENU_ITEM_ID_SETTING = Menu.FIRST + 2

    var page : Int = 0
    var dashboardLoading = false

    override def onCreate (savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        val listView = findViewById(R.id.layout_list).asInstanceOf[ListView]
        val display  = getWindowManager().getDefaultDisplay()
        val adapter  = new TumblrPostAdapter(this, R.layout.list_row, display)

        listView.setAdapter(adapter)
        listView.setOnScrollListener(
            new AbsListView.OnScrollListener {
                def onScroll (view : AbsListView, firstVisibleItem : Int, visibleItemCount : Int, totalItemCount : Int) {
                    // Log.d("PicTumblrActivity.listView", "onScroll " + (firstVisibleItem, visibleItemCount, totalItemCount))
                    if (!PicTumblrActivity.this.dashboardLoading && firstVisibleItem + visibleItemCount >= totalItemCount - 1) {
                        Log.d("PicTumblrActivity.listView", "goBackDashboard start: " + (firstVisibleItem, visibleItemCount, totalItemCount))
                        PicTumblrActivity.this.goBackDashboard()
                    }
                }
                def onScrollStateChanged (view : AbsListView, scrollState : Int) {
                }
            }
        )

        // goBackDashboard()
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
            case MENU_ITEM_ID_REFRESH => goBackDashboard
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

    def goBackDashboard () {
        if (dashboardLoading) {
            Log.d("PicTumblrActivity", "goBackDashboard: Already loading")
            return
        }

        val tumblr = getTumblr()
        val listView = findViewById(R.id.layout_list).asInstanceOf[ListView]

        try {
            // XXX クロージャなんか渡して大丈夫なんだろうか…
            dashboardLoading = true

            val task = new LoadDashboardTask(
                tumblr, page + 1, listView.getAdapter().asInstanceOf[TumblrPostAdapter],
                { Log.d("PicTumblrActivity", "LoadDashboardTask callback"); dashboardLoading = false },
                this.toast(_)
            )
            task.execute()

            page = page + 1
        } catch {
            case e => {
                Log.e("PicTumblrActivity.goBackDashboard", e.toString)
                val stackTrace = e.getStackTrace
                stackTrace foreach { s => Log.d("PicTumblrActivity.goBackDashboard", s.toString()) }
                toast("Something went wrong: " + e.getMessage)
            }
        }
    }

    def toast (message : String) {
        Log.i("PicTumblrActivity", "toast: " + message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show
    }
}

class TumblrPostAdapter (context : Context, textVeiwResourceId : Int, display : Display)
        extends ArrayAdapter[Bitmap](context, textVeiwResourceId) {

    // convertView は他のアイテムの場合もあるらしい
    override def getView (position : Int, convertView: View, parent : ViewGroup) : View = {
        val bitmap = getItem(position)
        if (bitmap == null) {
            return new ProgressBar(parent.getContext())
        } else {
            // TODO reuse view
            val imageView = LayoutInflater.from(context).inflate(R.layout.list_row_image, null).asInstanceOf[ImageView]
            imageView.setImageBitmap(bitmap)
            return imageView
        }
    }
}

// AsyncTask[Int, ...] だと落ちる → java.lang.Integer に
// FIXME no toast here
class LoadDashboardTask (tumblr : Tumblr, page : Int, adapter : TumblrPostAdapter, callback : => Unit, toast : String => Unit)
        extends AsyncTask0[java.lang.Void, List[Tumblr#Post]] {

    val perPage = 10

    override def onPreExecute () {
        toast("Loading dashboard " + ((page - 1) * perPage + 1) + "-" + (page * perPage))
    }

    // 可変長引数でやりとりできないのは AsyncTask1.java にブリッジさせる
    override def doInBackground () : List[Tumblr#Post] = {
        Log.d("LoadDashboardTask", "doInBackground")
        // FIXME ここでエラーおきたときのハンドリング ふつうはどうするんだろう
        return tumblr.dashboard('start -> ((page - 1) * perPage).toString, 'num -> perPage.toString)
    }

    override def onPostExecute (posts : List[Tumblr#Post]) {
        toast("Dashboard loaded.")

        val counter = new Counter(posts.count { _.isInstanceOf[tumblr.PhotoPost] }, callback)

        // post match { case Tumblr#PhotoPost(url) => ... } できない件は
        // post match { case tumblr.PhotoPost(url) => ... } でいける
        // ref. http://stackoverflow.com/questions/1812695/scala-case-class-matching-compile-error-with-aliased-inner-types
        posts foreach {
            case post : tumblr.PhotoPost => {
                Log.d("LoadDashboardTask", "PhotoPost: " + post.toString())

                val task = new LoadPhotoTask(adapter, counter.up())
                task.execute(post.photoUrl)
            }
            case post => {
                Log.d("LoadDashboardTask", "cannot handle post: " + post.toString())
            }
        }
    }
}

class LoadPhotoTask (adapter : TumblrPostAdapter, callback : => Unit)
        extends AsyncTask1[String, java.lang.Void, Bitmap] {

    // 単純に Drawable.createFromStream() するとメモリを食うので Bitmap.Config.RGB_565 を指定
    override def doInBackground (url : String) : Bitmap = {
        val options = new BitmapFactory.Options
        options.inPreferredConfig = Bitmap.Config.RGB_565

        val bitmap = BitmapFactory.decodeStream(
            new java.net.URL(url).openConnection.getInputStream, null, options
        )
        Log.d("LoadPhotoTask", "doInBackground: loaded " + url)
        return bitmap
    }

    override def onPostExecute (bitmap : Bitmap) {
        // TODO order
        adapter.add(bitmap)
        callback
    }
}

// FIXME naming
class Counter (bound: Int, callback: => Unit) {
    var count : Int = 0;

    def up () {
        count = count + 1
        Log.d("PicTumblrActivity", "Counter: " + count + "/" + bound)
        if (count == bound) {
            callback
        }
    }
}
