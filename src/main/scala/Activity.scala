package net.tokyoenvious.droid.pictumblr

import android.app.Activity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.{ View, ViewGroup, Menu, MenuItem, LayoutInflater }
import android.graphics.{ Bitmap, BitmapFactory }
import android.widget.{ Toast, ImageView, ProgressBar, ArrayAdapter, ListView, AbsListView, AdapterView, LinearLayout, RelativeLayout }
import android.content.{ Intent, Context }
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent

class PicTumblrActivity extends Activity {
    val MENU_ITEM_ID_REFRESH = Menu.FIRST + 1
    val MENU_ITEM_ID_SETTING = Menu.FIRST + 2

    val OLD_POST_OFFSET = 5

    lazy val horizontalScrollView = findViewById(R.id.layout_scrollview).asInstanceOf[android.widget.HorizontalScrollView]
    lazy val imagesContainer = findViewById(R.id.images_container).asInstanceOf[LinearLayout]
    lazy val gestureDetector = new GestureDetector(
        new GestureDetector.SimpleOnGestureListener() {
            override def onFling (e1 : MotionEvent, e2 : MotionEvent, vx : Float, vy : Float) : Boolean = {
                if (e1.getX() - e2.getX() < 0) {
                    horizontalScrollView.smoothScrollBy(-horizontalScrollView.getWidth(), 0)
                } else {
                    horizontalScrollView.smoothScrollBy(+horizontalScrollView.getWidth(), 0)
                }

                return true
            }

            /*
            override def onScroll (e1 : MotionEvent, e2 : MotionEvent, dx : Float, dy : Float) : Boolean = {
                // discard all scroll
                return true
            }
            */
        }
    )

    var page : Int = 0
    var dashboardLoading = false

    override def onCreate (savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        horizontalScrollView.setOnTouchListener(
            new View.OnTouchListener() {
                override def onTouch (v : View, event : MotionEvent) : Boolean = {
                    if (gestureDetector.onTouchEvent(event)) {
                        return true
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        // do not move
                        return true
                    } else {
                        return false
                    }
                }
            }
        )

        goBackDashboard()
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

        try {
            // XXX クロージャなんか渡して大丈夫なんだろうか…
            dashboardLoading = true

            val task = new LoadDashboardTask(
                tumblr, page + 1, imagesContainer,
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

// AsyncTask[Int, ...] だと落ちる → java.lang.Integer に
// FIXME no toast here
class LoadDashboardTask (tumblr : Tumblr, page : Int, imagesContainer : LinearLayout, callback : => Unit, toast : String => Unit)
        extends AsyncTask0[java.lang.Void, Seq[Tumblr#Post]] {

    val perPage = 10

    override def onPreExecute () {
        toast("Loading dashboard " + ((page - 1) * perPage + 1) + "-" + (page * perPage))
    }

    // 可変長引数でやりとりできないのは AsyncTask1.java にブリッジさせる
    override def doInBackground () : Seq[Tumblr#Post] = {
        Log.d("LoadDashboardTask", "doInBackground")
        // FIXME ここでエラーおきたときのハンドリング ふつうはどうするんだろう
        return tumblr.dashboard('start -> ((page - 1) * perPage).toString, 'num -> perPage.toString)
    }

    override def onPostExecute (posts : Seq[Tumblr#Post]) {
        toast("Dashboard loaded.")

        val postsList = posts.toList
        val counter = new Counter(postsList.count { _.isInstanceOf[tumblr.PhotoPost] }, callback)

        // post match { case Tumblr#PhotoPost(url) => ... } できない件は
        // post match { case tumblr.PhotoPost(url) => ... } でいける
        // ref. http://stackoverflow.com/questions/1812695/scala-case-class-matching-compile-error-with-aliased-inner-types
        postsList foreach {
            case post : tumblr.PhotoPost => {
                Log.d("LoadDashboardTask", "PhotoPost: " + post.toString())

                val context = imagesContainer.getContext()
                val displayWidth = context.getSystemService(Context.WINDOW_SERVICE)
                        .asInstanceOf[android.view.WindowManager].getDefaultDisplay().getWidth
                val layout = new android.widget.RelativeLayout(context)
                layout.setGravity(android.view.Gravity.CENTER)
                imagesContainer.addView(layout, new ViewGroup.LayoutParams(displayWidth, ViewGroup.LayoutParams.FILL_PARENT))

                val task = new LoadPhotoTask(layout, counter.up())
                task.execute(post)
            }
            case post => {
                Log.d("LoadDashboardTask", "cannot handle post: " + post.toString())
            }
        }
    }
}

class LoadPhotoTask (imageContainer : RelativeLayout, callback : => Unit)
        extends AsyncTask1[Tumblr#PhotoPost, java.lang.Void, Bitmap] {

    // 単純に Drawable.createFromStream() するとメモリを食うので Bitmap.Config.RGB_565 を指定
    override def doInBackground (photoPost : Tumblr#PhotoPost) : Bitmap = {
        val options = new BitmapFactory.Options
        options.inPreferredConfig = Bitmap.Config.RGB_565

        val bitmap = BitmapFactory.decodeStream(
            new java.net.URL(photoPost.photoUrl).openConnection.getInputStream, null, options
        )
        Log.d("LoadPhotoTask", "doInBackground: loaded " + photoPost.photoUrl)
        return bitmap
    }

    override def onPreExecute () {
        val progressBar = new ProgressBar(imageContainer.getContext())
        imageContainer.addView(progressBar)
    }

    override def onPostExecute (bitmap : Bitmap) {
        imageContainer.removeViewAt(0)

        val imageView = new ImageView(imageContainer.getContext())
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE)
        imageView.setImageBitmap(bitmap)
        imageView.setAdjustViewBounds(true)
        // imageView.setMaxWidth(displayWidth)
        // imageView.setMinimumWidth(displayWidth)

        imageContainer.addView(imageView)

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
