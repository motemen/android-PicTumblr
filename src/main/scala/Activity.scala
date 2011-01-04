package net.tokyoenvious.droid.pictumblr

import android.app.Activity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.{ View, ViewGroup, Menu, MenuItem, ContextMenu, GestureDetector, MotionEvent }
import android.graphics.{ Bitmap, BitmapFactory }
import android.widget.{ Toast, ImageView, ProgressBar, LinearLayout, RelativeLayout }
import android.content.{ Intent, Context }

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.entity.BufferedHttpEntity

import scala.collection.mutable.Queue

class PicTumblrActivity extends Activity {
    val MENU_ITEM_ID_REFRESH = Menu.FIRST + 1
    val MENU_ITEM_ID_SETTING = Menu.FIRST + 2

    val CONTEXT_MENU_ID_ITEM_OPEN_TUMBLR     = Menu.FIRST + 3
    val CONTEXT_MENU_ID_ITEM_OPEN_PHOTO_LINK = Menu.FIRST + 4
    val CONTEXT_MENU_ID_ITEM_REBLOG          = Menu.FIRST + 5

    val BACKWARD_OFFSET = 3
    val FORWARD_OFFSET  = 4

    lazy val horizontalScrollView = findViewById(R.id.layout_scrollview).asInstanceOf[android.widget.HorizontalScrollView]
    lazy val imagesContainer = findViewById(R.id.images_container).asInstanceOf[LinearLayout]
    lazy val gestureDetector = new GestureDetector(
        new GestureDetector.SimpleOnGestureListener() {
            override def onFling (e1 : MotionEvent, e2 : MotionEvent, vx : Float, vy : Float) : Boolean = {
                if (e1 == null || e2 == null) {
                    return true
                }

                purgeOldAndLoadNewPosts()

                if (e1.getX() - e2.getX() < 0) {
                    horizontalScrollView.smoothScrollBy(-horizontalScrollView.getWidth(), 0)
                } else {
                    horizontalScrollView.smoothScrollBy(+horizontalScrollView.getWidth(), 0)
                }

                return true
            }

            override def onLongPress (e : MotionEvent) {
                Log.d("PicTumblrActivity", "onLongPress")
                PicTumblrActivity.this.openContextMenu(horizontalScrollView)
            }

            override def onDoubleTap (e : MotionEvent) : Boolean = {
                Log.d("PicTumblrActivity", "onDoubleTap")
                return false
            }
        }
    )

    lazy val displayWidth = getSystemService(Context.WINDOW_SERVICE)
                .asInstanceOf[android.view.WindowManager].getDefaultDisplay().getWidth

    val posts = new Queue[Tumblr#PhotoPost]();

    var page : Int = 0
    var dashboardLoading = false
    var index : Int = 0

    override def onCreate (savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main)

        horizontalScrollView.setOnCreateContextMenuListener(this)

        gestureDetector.setIsLongpressEnabled(true)

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

    override def onCreateContextMenu (menu : ContextMenu, v : View, menuInfo : ContextMenu.ContextMenuInfo) {
        Log.d("PicTumblrActivity", "onCreateContextMenu")

        for (
            post <- currentPost
        ) {
            menu.setHeaderTitle(post.plainCaption)
            
            var itemOpenTumblr    = menu.add(Menu.NONE, CONTEXT_MENU_ID_ITEM_OPEN_TUMBLR,     Menu.NONE, "Open Tumblr Page")
            var itemOpenPhotoLink = menu.add(Menu.NONE, CONTEXT_MENU_ID_ITEM_OPEN_PHOTO_LINK, Menu.NONE, "Open Photo Link")
            var itemReblog        = menu.add(Menu.NONE, CONTEXT_MENU_ID_ITEM_REBLOG,          Menu.NONE, "Reblog")

            super.onCreateContextMenu(menu, v, menuInfo)
        }
    }

    override def onContextItemSelected (menuItem : MenuItem) : Boolean = {
        menuItem.getItemId() match {
            case CONTEXT_MENU_ID_ITEM_OPEN_TUMBLR     => openTumblr
            case CONTEXT_MENU_ID_ITEM_OPEN_PHOTO_LINK => openPhotoLink
            case CONTEXT_MENU_ID_ITEM_REBLOG          => doReblogPost
        }

        return true
    }

    // def index () がいいかも
    def updateIndex () {
        val scrollX = horizontalScrollView.getScrollX()
        index = scrollX / displayWidth
        Log.d("PicTumblrActivity", "updateIndex: " + index)
    }

    def purgeOldAndLoadNewPosts () {
        Log.d("PicTumblrActivity", "purgeOldAndLoadNewPosts")

        updateIndex()

        for (i <- 1 to (posts.size min (index - BACKWARD_OFFSET))) {
            Log.d("PicTumblrActivity", "dequeue post")
            posts.dequeue()
            imagesContainer.removeViewAt(0)
            horizontalScrollView.scrollBy(-horizontalScrollView.getWidth(), 0)
            index -= 1
        }

        if (index >= posts.size - FORWARD_OFFSET) {
            goBackDashboard()
        }
    }

    def currentPost () : Option[Tumblr#PhotoPost] = {
        updateIndex()
        return posts.get(index)
    }

    def startSettingActivity () {
        val intent = new Intent(this, classOf[PicTumblrPrefernceActivity])
        startActivity(intent)
    }

    def showPostInfoDialog () {
        Log.d("PicTumblrActivity", "showPostInfoDialog: " + currentPost)

        val dialog = new android.app.Dialog(this)
        dialog.show()
    }

    def openTumblr () {
        for (
            post <- currentPost
        ) {
            val intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(post.urlWithSlug))
            startActivity(intent)
        }
    }

    def openPhotoLink () {
        for (
            post <- currentPost;
            photoLinkUrl <- post.photoLinkUrl
        ) {
            val intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(photoLinkUrl))
            startActivity(intent)
        }
    }

    def doReblogPost () {
        for (
            post <- currentPost;
            tumblr <- getTumblr
        ) {
            // TODO AsyncTask
            Log.d("PicTumblrActivity", "doReblogPost: " + post)
            toast("Reblogging...")
            tumblr.reblog(post.asInstanceOf[tumblr.PhotoPost])
            toast("Reblogged.")
        }
    }

    def getTumblr () : Option[Tumblr] = {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val email    = prefs.getString("email", "")
        val password = prefs.getString("password", "")

        if (email.length == 0 || password.length == 0) {
            // TODO 戻ってきたら再アクセスとかすべきだが…
            startSettingActivity()
            return None
        }

        return Some(new Tumblr(email, password))
    }

    def goBackDashboard () {
        if (dashboardLoading) {
            Log.d("PicTumblrActivity", "goBackDashboard: Already loading")
            return
        }

        getTumblr() match {
            case None
                => return

            case Some(tumblr) => {
                try {
                    dashboardLoading = true

                    // XXX クロージャなんか渡して大丈夫なんだろうか…
                    val task = new LoadDashboardTask(
                        tumblr, page + 1, imagesContainer, posts,
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
        }
    }

    def toast (message : String) {
        Log.i("PicTumblrActivity", "toast: " + message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show
    }
}

// AsyncTask[Int, ...] だと落ちる → java.lang.Integer に
// FIXME no toast here
class LoadDashboardTask (tumblr : Tumblr, page : Int, imagesContainer : LinearLayout, posts : Queue[Tumblr#PhotoPost], callback : => Unit, toast : String => Unit)
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

    override def onPostExecute (loadedPosts : Seq[Tumblr#Post]) {
        toast("Dashboard loaded.")

        val tasks = new TaskGroup(callback)

        // post match { case Tumblr#PhotoPost(url) => ... } できない件は
        // post match { case tumblr.PhotoPost(url) => ... } でいける
        // ref. http://stackoverflow.com/questions/1812695/scala-case-class-matching-compile-error-with-aliased-inner-types
        loadedPosts.toList foreach {
            case post : tumblr.PhotoPost => {
                Log.d("LoadDashboardTask", "PhotoPost: " + post.toString())

                posts += post

                val context = imagesContainer.getContext()
                val displayWidth = context.getSystemService(Context.WINDOW_SERVICE)
                        .asInstanceOf[android.view.WindowManager].getDefaultDisplay().getWidth
                val layout = new RelativeLayout(context)
                layout.setGravity(android.view.Gravity.CENTER)
                imagesContainer.addView(layout, new ViewGroup.LayoutParams(displayWidth, ViewGroup.LayoutParams.FILL_PARENT))

                tasks.begin()
                val task = new LoadPhotoTask(layout, { tasks.end() })
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

    override def doInBackground (photoPost : Tumblr#PhotoPost) : Bitmap = {
        // 単純に Drawable.createFromStream() するとメモリを食うので Bitmap.Config.RGB_565 を指定
        val options = new BitmapFactory.Options
        options.inPreferredConfig = Bitmap.Config.RGB_565

        // これだと読み込みに失敗することが多い
        // ref. http://stackoverflow.com/questions/1630258/android-problem-bug-with-threadsafeclientconnmanager-downloading-images
        /*
        val bitmap = BitmapFactory.decodeStream(
            new java.net.URL(photoPost.photoUrl).openConnection.getInputStream, null, options
        )
        */

        var httpClient = new DefaultHttpClient
        val httpGet = new HttpGet(photoPost.photoUrl)
        val httpResponse = httpClient.execute(httpGet)

        val bitmap  = BitmapFactory.decodeStream(
            new BufferedHttpEntity(httpResponse.getEntity).getContent()
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

        imageContainer.addView(imageView)

        callback
    }
}

class ReblogPostTask (tumblr : Tumblr, callback : => Unit)
        extends AsyncTask1[Tumblr#Post, java.lang.Void, Int] { // FIXME Unit は無理なのかな

    override def doInBackground (post : Tumblr#Post) : Int = {
        tumblr.reblog(post.asInstanceOf[tumblr.PhotoPost])
        0
    }

    override def onPostExecute (dummy : Int) {
        callback
    }
}

class TaskGroup (callback: => Unit) {
    var count : Int = 0;

    def begin () {
        count = count + 1
        Log.d("PicTumblrActivity", "TaskGroup: begin: " + count)
    }

    def end () {
        count = count - 1
        Log.d("PicTumblrActivity", "TaskGroup: end: " + count)

        if (count == 0) {
            callback
        }
    }
}
