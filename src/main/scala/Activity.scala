package net.tokyoenvious.droid.pictumblr

import android.app.Activity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.{ Window, WindowManager, View, ViewGroup, Menu, MenuItem, ContextMenu, GestureDetector, MotionEvent, KeyEvent }
import android.graphics.{ Bitmap, BitmapFactory }
import android.widget.{ Toast, ImageView, ProgressBar, LinearLayout, RelativeLayout }
import android.content.{ Intent, Context }

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.entity.BufferedHttpEntity

import scala.collection.mutable.Queue

class PicTumblrActivity extends TypedActivity {
    val MENU_ITEM_ID_REFRESH = Menu.FIRST + 1
    val MENU_ITEM_ID_SETTING = Menu.FIRST + 2

    val CONTEXT_MENU_ID_ITEM_OPEN_TUMBLR     = Menu.FIRST + 3
    val CONTEXT_MENU_ID_ITEM_OPEN_PHOTO_LINK = Menu.FIRST + 4
    val CONTEXT_MENU_ID_ITEM_REBLOG          = Menu.FIRST + 5
    val CONTEXT_MENU_ID_ITEM_LIKE            = Menu.FIRST + 6

    val BACKWARD_OFFSET = 3
    val FORWARD_OFFSET  = 10

    lazy val horizontalScrollView = findView(TR.layout_scrollview)
    lazy val imagesContainer      = findView(TR.images_container)
    lazy val captionTextView      = findView(TR.textview_caption)

    lazy val vibrator = getSystemService(Context.VIBRATOR_SERVICE).asInstanceOf[android.os.Vibrator]
    lazy val displayWidth = getSystemService(Context.WINDOW_SERVICE)
                .asInstanceOf[WindowManager].getDefaultDisplay().getWidth

    lazy val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    lazy val intent = getIntent

    lazy val gestureDetector = new GestureDetector(
        new GestureDetector.SimpleOnGestureListener() {
            override def onFling (e1 : MotionEvent, e2 : MotionEvent, vx : Float, vy : Float) : Boolean = {
                if (e1 == null || e2 == null) {
                    return true
                }

                if (e1.getX() - e2.getX() < 0) {
                    toPreviousPost()
                } else {
                    toNextPost()
                }

                return true
            }

            override def onLongPress (e : MotionEvent) {
                Log.d("PicTumblrActivity", "onLongPress")
                vibrator.vibrate(25)
                PicTumblrActivity.this.openContextMenu(horizontalScrollView)
            }

            override def onDoubleTap (e : MotionEvent) : Boolean = {
                Log.d("PicTumblrActivity", "onDoubleTap")
                doReblogPost
                return true
            }
        }
    )

    lazy val globalTasks = new TaskGroup(
        { setProgressBarIndeterminateVisibility(false) },
        { setProgressBarIndeterminateVisibility(true) }
    )

    val posts = new Queue[Tumblr#PhotoPost]();

    var page : Int = 0
    var dashboardLoading = false
    var index : Int = 0

    private def makeSteppedScroller () = {
        new android.widget.Scroller(this) {
            override def startScroll (startX : Int, startY : Int, dx : Int, dy : Int) {
                Log.d("Scroller", "startScroll: " + (startX, startY, dx, dy))

                // XXX ここでやるとずれたとき変になる
                // PicTumblrActivity.this.purgeOldAndLoadNewPosts
                
                // FIXME 連続してスクロールしてるとなんか変？

                val newX = if (dx > 0) {
                    (scala.math.floor(startX / displayWidth.toDouble) + 1) * displayWidth
                } else {
                    (scala.math.ceil (startX / displayWidth.toDouble) - 1) * displayWidth
                }
                super.startScroll(startX, startY, newX.toInt - startX, dy)
            }

            var lastNotFinished = false
            override def computeScrollOffset () : Boolean = {
                val notFinished = super.computeScrollOffset
                // XXX ちょっと重かったりしないか？
                if (notFinished == false && lastNotFinished != notFinished) {
                    horizontalScrollView.post(
                        new java.lang.Thread() {
                            override def run() {
                                val activity = PicTumblrActivity.this
                                activity.purgeOldAndLoadNewPosts
                                // TODO スクロール始まったタイミングで設定したい
                                activity.updateCaption
                            }
                        }
                    )
                }
                lastNotFinished = notFinished
                return notFinished
            }
        }
    }

    // XXX できれば上と実装を共通化したい…
    private def makeSteppedOverScroller () = {
        new android.widget.OverScroller(this) {
            override def startScroll (startX : Int, startY : Int, dx : Int, dy : Int) {
                Log.d("Scroller", "startScroll: " + (startX, startY, dx, dy))

                // XXX ここでやるとずれたとき変になる
                // PicTumblrActivity.this.purgeOldAndLoadNewPosts
                
                // FIXME 連続してスクロールしてるとなんか変？

                val newX = if (dx > 0) {
                    (scala.math.floor(startX / displayWidth.toDouble) + 1) * displayWidth
                } else {
                    (scala.math.ceil (startX / displayWidth.toDouble) - 1) * displayWidth
                }
                super.startScroll(startX, startY, newX.toInt - startX, dy)
            }

            var lastNotFinished = false
            override def computeScrollOffset () : Boolean = {
                val notFinished = super.computeScrollOffset
                // XXX ちょっと重かったりしないか？
                if (notFinished == false && lastNotFinished != notFinished) {
                    horizontalScrollView.post(
                        new java.lang.Thread() {
                            override def run() {
                                val activity = PicTumblrActivity.this
                                activity.purgeOldAndLoadNewPosts
                                // TODO スクロール始まったタイミングで設定したい
                                activity.updateCaption
                            }
                        }
                    )
                }
                lastNotFinished = notFinished
                return notFinished
            }
        }
    }

    override def onCreate (savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.main)

        horizontalScrollView.setOnCreateContextMenuListener(this)
        horizontalScrollView.setHorizontalScrollBarEnabled(false)

        // XXX using reflection
        val scrollerField = horizontalScrollView.getClass.getDeclaredField("mScroller")
        scrollerField.setAccessible(true)
        if (scrollerField.getType.getName == "android.widget.Scroller") {
            scrollerField.set(horizontalScrollView, makeSteppedScroller)
        } else {
            // API lv >= 9
            scrollerField.set(horizontalScrollView, makeSteppedOverScroller)
        }

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

        val noLoadOnCreate = intent.getBooleanExtra("net.tokyoenvious.droid.pictumblr.tests.noLoadOnCreate", false)
        if (!noLoadOnCreate) {
            goBackDashboard()
        }
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
            case MENU_ITEM_ID_REFRESH => {
                page = 0
                goBackDashboard
            }
            case MENU_ITEM_ID_SETTING => startPreferenceActivity
        }

        return true
    }

    override def onCreateContextMenu (menu : ContextMenu, v : View, menuInfo : ContextMenu.ContextMenuInfo) {
        Log.d("PicTumblrActivity", "onCreateContextMenu")

        for (
            post <- currentPost
        ) {
            menu.setHeaderTitle(post.plainCaption)
            
            val itemOpenTumblr    = menu.add(Menu.NONE, CONTEXT_MENU_ID_ITEM_OPEN_TUMBLR,     Menu.NONE, "Open Tumblr Page")
            val itemOpenPhotoLink = menu.add(Menu.NONE, CONTEXT_MENU_ID_ITEM_OPEN_PHOTO_LINK, Menu.NONE, "Open Photo Link")
            val itemReblog        = menu.add(Menu.NONE, CONTEXT_MENU_ID_ITEM_REBLOG,          Menu.NONE, "Reblog")
            val itemLike          = menu.add(Menu.NONE, CONTEXT_MENU_ID_ITEM_LIKE,            Menu.NONE, "Like")

            super.onCreateContextMenu(menu, v, menuInfo)
        }
    }

    override def onContextItemSelected (menuItem : MenuItem) : Boolean = {
        menuItem.getItemId() match {
            case CONTEXT_MENU_ID_ITEM_OPEN_TUMBLR     => openTumblr
            case CONTEXT_MENU_ID_ITEM_OPEN_PHOTO_LINK => openPhotoLink
            case CONTEXT_MENU_ID_ITEM_REBLOG          => doReblogPost
            case CONTEXT_MENU_ID_ITEM_LIKE            => doLikePost
        }

        return true
    }

    override def onKeyDown (keyCode : Int, event : KeyEvent) : Boolean = {
        keyCode match {
            case KeyEvent.KEYCODE_VOLUME_DOWN => {
                toNextPost
                return true
            }
            case KeyEvent.KEYCODE_VOLUME_UP => {
                toPreviousPost
                return true
            }
            case _ => {
                return false
            }
        }
    }

    def purgeOldAndLoadNewPosts () {
        Log.d("PicTumblrActivity", "purgeOldAndLoadNewPosts")

        for (i <- 1 to ((currentIndex - BACKWARD_OFFSET) min (posts.size))) {
            Log.d("PicTumblrActivity", "dequeue post")
            posts.dequeue()
            imagesContainer.removeViewAt(0)
            horizontalScrollView.scrollBy(-horizontalScrollView.getWidth(), 0)
        }

        if (currentIndex >= posts.size - FORWARD_OFFSET) {
            goBackDashboard()
        }
    }

    def updateCaption () {
        captionTextView.setText(
            currentPost match {
                case Some(post) => post.plainCaption
                case None       => ""
            }
        )
    }

    def currentIndex () : Int = {
        val scrollX = horizontalScrollView.getScrollX()
        val index = scrollX / displayWidth
        val delta = scrollX % displayWidth

        if (delta != 0) {
            if (delta * 2 < displayWidth) {
                horizontalScrollView.smoothScrollBy(-delta - displayWidth, 0)
                return index
            } else {
                horizontalScrollView.smoothScrollBy(-delta + displayWidth, 0)
                return index + 1
            }
        } else {
            return index
        }
 
        return index
    }

    def toNextPost () {
        // purgeOldAndLoadNewPosts()

        val scrollX = horizontalScrollView.getScrollX()
        horizontalScrollView.smoothScrollTo(
            scala.math.floor(scrollX / displayWidth.toDouble + 1.0).toInt * displayWidth, 0
        )
    }

    def toPreviousPost () {
        // purgeOldAndLoadNewPosts()

        val scrollX = horizontalScrollView.getScrollX()
        horizontalScrollView.smoothScrollTo(
            scala.math.floor(scrollX / displayWidth.toDouble - 1.0).toInt * displayWidth, 0
        )
    }

    def currentPost () : Option[Tumblr#PhotoPost] = {
        return posts.get(currentIndex)
    }

    def startPreferenceActivity () {
        val intent = new Intent(this, classOf[PicTumblrPrefernceActivity])
        startActivityForResult(intent, 0)
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
            Log.d("PicTumblrActivity", "doReblogPost: " + post)

            // TODO なんかもっと見目よく、何リブログしてるか分かるように
            globalTasks.begin()
            toast("Reblogging...")

            val task = new ReblogPostTask(
                tumblr,
                { globalTasks.end(); toast("Reblogged.") }
            )
            task.execute(post)
        }
    }

    def doLikePost () {
        for (
            post <- currentPost;
            tumblr <- getTumblr
        ) {
            Log.d("PicTumblrActivity", "doLikePost: " + post)

            // TODO なんかもっと見目よく、何リブログしてるか分かるように
            globalTasks.begin()
            toast("Liking...")

            val task = new LikePostTask(
                tumblr,
                { globalTasks.end(); toast("Liked.") }
            )
            task.execute(post)
        }
    }

    def getTumblr () : Option[Tumblr] = {
        val email    = preferences.getString("email", "")
        val password = preferences.getString("password", "")

        if (email.length == 0 || password.length == 0) {
            toast("Press menu and enter your Tumblr account.")
            return None
        } else {
            return Some(new Tumblr(email, password))
        }
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

                    globalTasks.begin()
                    val task = new LoadDashboardTask(
                        tumblr, page + 1,
                        new TaskGroup({
                            Log.d("PicTumblrActivity", "LoadDashboardTask callback")
                            dashboardLoading = false
                            globalTasks.end()
                        })
                    )
                    task.execute()

                    page = page + 1 // FIXME 成功したときだけ

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

    var currentToast : Toast = null
    def toast (message : String) {
        Log.i("PicTumblrActivity", "toast: " + message)

        if (currentToast != null) {
            currentToast.cancel()
        }

        currentToast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        currentToast.show()
    }

    class TaskGroup (callback: => Unit, preCallback : => Unit = {}) {
        var count : Int = 0

        def begin () {
            count = count + 1
            Log.d("PicTumblrActivity", "TaskGroup: begin: " + count)
            preCallback
        }

        def end () {
            count = count - 1
            Log.d("PicTumblrActivity", "TaskGroup: end:   " + count)

            if (count == 0) {
                callback
            }
        }
    }

    class LoadDashboardTask (tumblr : Tumblr, page : Int, tasks : PicTumblrActivity#TaskGroup)
            extends AsyncTask0[java.lang.Void, Tumblr#MaybeError[Seq[Tumblr#PhotoPost]]] {

        val perPage = 20 // TODO make configurable

        val imagesContainer = PicTumblrActivity.this.imagesContainer
        val posts = PicTumblrActivity.this.posts
        var dialog : android.app.ProgressDialog = null

        def toast (message : String) = PicTumblrActivity.this.toast(message)

        override def onPreExecute () {
            val message = "Loading dashboard " + ((page - 1) * perPage + 1) + "-" + (page * perPage)
            if (posts.length == 0) {
                // 最初の一回だけはダイアログを表示する
                dialog = android.app.ProgressDialog.show(PicTumblrActivity.this, null, message)
            } else {
                toast(message)
            }
        }

        // 可変長引数でやりとりできないのは AsyncTask1.java にブリッジさせる
        override def doInBackground () : Tumblr#MaybeError[Seq[Tumblr#PhotoPost]] = {
            Log.d("LoadDashboardTask", "doInBackground")

            return tumblr.dashboard("start" -> ((page - 1) * perPage).toString, "num" -> perPage.toString)
        }

        override def onPostExecute (result : Tumblr#MaybeError[Seq[Tumblr#PhotoPost]]) {
            if (dialog != null) dialog.dismiss()

            result match {
                case Left(error) => {
                    toast("error: " + error)
                    tasks.begin()
                    tasks.end()
                }

                case Right(loadedPosts) => {
                    toast("Dashboard loaded.")

                    for (post <- loadedPosts) {
                        Log.d("LoadDashboardTask", "PhotoPost: " + post.toString())

                        posts += post

                        val layout = new RelativeLayout(imagesContainer.getContext())
                        layout.setGravity(android.view.Gravity.CENTER)

                        imagesContainer.addView(
                            layout,
                            new ViewGroup.LayoutParams(
                                PicTumblrActivity.this.displayWidth,
                                ViewGroup.LayoutParams.FILL_PARENT
                            )
                        )

                        tasks.begin()
                        val task = new LoadPhotoTask(layout, { tasks.end() })
                        task.execute(post)
                    }

                    PicTumblrActivity.this.updateCaption
                }
            }
        }
    }
}

class LoadPhotoTask (imageContainer : ViewGroup, callback : => Unit)
        extends AsyncTask1[Tumblr#PhotoPost, java.lang.Void, Bitmap] {

    override def doInBackground (photoPost : Tumblr#PhotoPost) : Bitmap = {
        // これだと読み込みに失敗することが多い
        // ref. http://stackoverflow.com/questions/1630258/android-problem-bug-with-threadsafeclientconnmanager-downloading-images
        /*
        val bitmap = BitmapFactory.decodeStream(
            new java.net.URL(photoPost.photoUrl).openConnection.getInputStream, null, options
        )
        */

        try {
            val httpClient   = new DefaultHttpClient
            val httpGet      = new HttpGet(photoPost.photoUrl)
            val httpResponse = httpClient.execute(httpGet)

            // 単純に Drawable.createFromStream() するとメモリを食うので Bitmap.Config.RGB_565 を指定
            val options = new BitmapFactory.Options
            options.inPreferredConfig = Bitmap.Config.RGB_565

            val bitmap = BitmapFactory.decodeStream(
                new BufferedHttpEntity(httpResponse.getEntity).getContent(), null, options
            )
            Log.d("LoadPhotoTask", "doInBackground: loaded " + photoPost.photoUrl)

            return bitmap
        } catch {
            // TODO 何かしら表示する
            case e => {
                e.printStackTrace
                return null
            }
        }
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
        extends AsyncTask1[Tumblr#PhotoPost, java.lang.Void, Unit] {

    override def doInBackground (post : Tumblr#PhotoPost) : Unit = {
        tumblr.reblog(post)
    }

    override def onPostExecute (u : Unit) {
        callback
    }
}

class LikePostTask (tumblr : Tumblr, callback : => Unit)
        extends AsyncTask1[Tumblr#PhotoPost, java.lang.Void, Unit] {

    override def doInBackground (post : Tumblr#PhotoPost) : Unit = {
        tumblr.like(post)
    }

    override def onPostExecute (u : Unit) {
        callback
    }
}
