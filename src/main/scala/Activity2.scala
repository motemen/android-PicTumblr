package net.tokyoenvious.droid.pictumblr

import android.content.Intent
import android.view.ViewGroup
import android.view.{Menu, ContextMenu, MenuItem}
import android.widget.ImageView
import android.widget.Toast
import android.graphics.Bitmap
import android.util.Log

import scala.collection.mutable.Queue

case class Entry (post : TumblrPhotoPost, var task : LoadPhotoTask2, var bitmap : Bitmap, var id : Long = 0) {
    def reblogged : Boolean = { id != 0 }
}

class PicTumblrActivity2 extends TypedActivity with TumblrOAuthable {
    val TAG = "PicTumblrActivity2"

    lazy val steppedHorizontalScrollView = findView(TR.layout_scrollview)
    lazy val imagesContainer             = findView(TR.images_container)
    lazy val captionTextView             = findView(TR.textview_caption)

    lazy val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE).asInstanceOf[android.os.Vibrator]

    lazy val displayWidth = getSystemService(android.content.Context.WINDOW_SERVICE)
                .asInstanceOf[android.view.WindowManager].getDefaultDisplay().getWidth()

    lazy val maxWidth = displayWidth // TODO make configurable

    val forwardOffset  = 5 // TODO make configurable
    val backwardOffset = 3 // TODO make configurable

    val entries = new Queue[Entry]();

    var offset : Int = 0
    var dashboardLoading : Boolean = false

    override def onCreate (savedInstanceState : android.os.Bundle) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main)

        setupSteppedHorizontalScrollView()
        
        startLoadingDashboard()
    }

    def startLoadingDashboard () {
        val loadingDialog = android.app.ProgressDialog.show(this, null, "Loading dashboard")

        val startTask = new AsyncTask0[java.lang.Void, Either[Throwable, Unit]] {
            override def doInBackground() : Either[Throwable, Unit] = {
                util.control.Exception.allCatch.either {
                    oauthAuthorize()
                }
            }

            override def onPostExecute (result : Either[Throwable, Unit]) = {
                result match {
                    case Right(_) => {
                        runLoadDashboardTask(offset = 0, dialog = loadingDialog)
                    }

                    // TODO: check error type
                    case Left(error) => {
                        error.printStackTrace()
                        Log.w(TAG, error.toString())
                        Toast.makeText(PicTumblrActivity2.this, error.toString(), Toast.LENGTH_SHORT).show()
                        if (error.isInstanceOf[TumblrAuthException]) {
                            startOAuth()
                        }
                    }
                }
            }
        }
        startTask.execute()
    }

    def setupSteppedHorizontalScrollView () {
        registerForContextMenu(steppedHorizontalScrollView)

        steppedHorizontalScrollView.onNext = () => {
            updateCaption(+1)
            loadNewPosts()
            for (i <- 0 to getCurrentIndex - backwardOffset; entry <- entries.get(i)) {
                if (entry.bitmap != null) {
                    entry.bitmap.recycle()
                }
            }
        }

        steppedHorizontalScrollView.onPrev = () => {
            updateCaption(-1)
        }

        steppedHorizontalScrollView.onLongPress = () => {
            vibrator.vibrate(25)
            openContextMenu(steppedHorizontalScrollView)
        }

        steppedHorizontalScrollView.onDoubleTap = () => {
            doReblogPost()
        }
    }

    def addNewImageContainer () = {
        val layout = new android.widget.RelativeLayout(PicTumblrActivity2.this)
        layout.setGravity(android.view.Gravity.CENTER)

        imagesContainer.addView(
            layout,
            new ViewGroup.LayoutParams(
                displayWidth,
                ViewGroup.LayoutParams.FILL_PARENT
            )
        )
        
        layout
    }

    def onDashboardLoad (loadedPosts : Seq[TumblrPhotoPost]) {
        Log.d(TAG, "loaded: " + loadedPosts.map { _.id }.mkString(","))
        Toast.makeText(this, "Dashboard loaded.", Toast.LENGTH_SHORT).show()

        val newEntries = loadedPosts.map { Entry(_, null, null) }

        val it = newEntries.toIterator

        def loadNextPhoto () {
            if (it.hasNext) {
                val entry = it.next()
                val imageContainer = addNewImageContainer()
                entry.task = createLoadPhotoTask(entry, imageContainer, loadNextPhoto)
                entry.task.execute()
            }
        }

        loadNextPhoto()
        loadNextPhoto()
        loadNextPhoto()
        loadNextPhoto()

        entries ++= newEntries
        offset += newEntries.length
        Log.d(TAG, "offset=" + offset)

        updateCaption()
    }

    def runLoadDashboardTask (offset : Int, dialog : android.app.ProgressDialog = null) {
        if (dashboardLoading == false) {
            dashboardLoading = true
            if (dialog == null) {
                Toast.makeText(this, "Loading dashboard " + (offset + 1) + "-" + (offset + 20), Toast.LENGTH_SHORT).show()
            }
            createLoadDashboardTask(dialog = dialog).execute(offset)
        }
    }

    def createLoadDashboardTask (dialog : android.app.ProgressDialog = null) : LoadDashboardTask2 = {
        new LoadDashboardTask2(
            tumblr  = tumblr,
            onLoad  = (loadedPosts : Seq[TumblrPhotoPost]) => {
                onDashboardLoad(loadedPosts)
            },
            onError = (error : Throwable) => {
                error.printStackTrace()
                Log.w(TAG, error.toString())
                Toast.makeText(PicTumblrActivity2.this, error.toString(), Toast.LENGTH_SHORT).show()
                if (error.isInstanceOf[TumblrAuthException]) {
                    startOAuth()
                }
            },
            onComplete = () => {
                dashboardLoading = false
                if (dialog != null) dialog.dismiss()
            }
        )
    }

    def createLoadPhotoTask (entry : Entry, imageContainer : ViewGroup, onLoad : () => Unit = null) : LoadPhotoTask2 = {
        new LoadPhotoTask2(
            maxWidth = maxWidth,
            imageContainer = imageContainer,
            photoPost = entry.post,
            onLoad = (bitmap : Bitmap) => {
                entry.bitmap = bitmap

                val imageView = new ForgetfulBitmapImageView(entry, PicTumblrActivity2.this)
                imageContainer.addView(imageView)

                if (onLoad != null) {
                    onLoad()
                }
            }
        )
    }

    def getCurrentIndex () : Int = {
        val scrollX = steppedHorizontalScrollView.getScrollX()
        val index = scrollX / displayWidth
        val delta = scrollX % displayWidth

        Log.v(TAG, "getCurrentIndex: index=" + index + " delta=" + delta)

        if (delta != 0) {
            if (delta * 2 < displayWidth) {
                steppedHorizontalScrollView.smoothScrollBy(-delta - displayWidth, 0)
                return index
            } else {
                steppedHorizontalScrollView.smoothScrollBy(-delta + displayWidth, 0)
                return index + 1
            }
        } else {
            return index
        }
 
        return index
    }

    def getCurrentEntry (delta : Int = 0) : Option[Entry]
        = entries.get(getCurrentIndex() + delta)

    def getCurrentPost (delta : Int = 0) : Option[TumblrPhotoPost]
        = getCurrentEntry(delta).map { _.post }

    def updateCaption (delta : Int = 0) {
        captionTextView.setText(
            getCurrentPost(delta).map { _.plainCaption }.getOrElse(getText(R.string.default_caption))
        )
    }

    def loadNewPosts () {
        var index = getCurrentIndex()
        if (entries.length - index <= forwardOffset) {
            runLoadDashboardTask(offset = offset)
        }
    }

    /****** MENUS ******/

    val MENU_ITEM_ID_REFRESH = Menu.FIRST + 1
    val MENU_ITEM_ID_SETTING = Menu.FIRST + 2

    val CONTEXT_MENU_ID_ITEM_OPEN_TUMBLR     = Menu.FIRST + 3
    val CONTEXT_MENU_ID_ITEM_OPEN_PHOTO_LINK = Menu.FIRST + 4
    val CONTEXT_MENU_ID_ITEM_REBLOG          = Menu.FIRST + 5
    val CONTEXT_MENU_ID_ITEM_LIKE            = Menu.FIRST + 6
    val CONTEXT_MENU_ID_ITEM_UNDO_REBLOG     = Menu.FIRST + 7

    def TODO (what : String) {
        Log.w(TAG, "TODO: " + what)
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
            case MENU_ITEM_ID_REFRESH => clearAndGoBackDashboard()
            case MENU_ITEM_ID_SETTING => startActivity(new Intent(this, classOf[PicTumblrPrefernceActivity]))
        }

        return true
    }

    override def onCreateContextMenu (menu : ContextMenu, v : android.view.View, menuInfo : ContextMenu.ContextMenuInfo) {
        for ( entry <- getCurrentEntry() ) {
            menu.setHeaderTitle(entry.post.plainCaption)
            
            val itemOpenTumblr     = menu.add(Menu.NONE, CONTEXT_MENU_ID_ITEM_OPEN_TUMBLR,     Menu.NONE, "Open Tumblr Page")
            val itemOpenPhotoLink  = menu.add(Menu.NONE, CONTEXT_MENU_ID_ITEM_OPEN_PHOTO_LINK, Menu.NONE, "Open Photo Link")
            if (!entry.reblogged) {
                val itemReblog     = menu.add(Menu.NONE, CONTEXT_MENU_ID_ITEM_REBLOG,          Menu.NONE, "Reblog")
            } else {
                val itemUndoReblog = menu.add(Menu.NONE, CONTEXT_MENU_ID_ITEM_UNDO_REBLOG,     Menu.NONE, "Undo Reblog")
            }
            val itemLike           = menu.add(Menu.NONE, CONTEXT_MENU_ID_ITEM_LIKE,            Menu.NONE, "Like")

            super.onCreateContextMenu(menu, v, menuInfo)
        }
    }

    private def openUrl (url : String) {
        startActivity(Intent.parseUri(url, 0))
        // startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    }

    override def onContextItemSelected (menuItem : MenuItem) : Boolean = {
        menuItem.getItemId() match {
            case CONTEXT_MENU_ID_ITEM_OPEN_TUMBLR => {
                for ( post <- getCurrentPost() ) {
                    openUrl(post.postUrl)
                }
            }
            case CONTEXT_MENU_ID_ITEM_OPEN_PHOTO_LINK => {
                for ( post <- getCurrentPost() ) {
                    openUrl(post.linkUrl)
                }
            }
            case CONTEXT_MENU_ID_ITEM_REBLOG => {
                doReblogPost()
            }
            case CONTEXT_MENU_ID_ITEM_LIKE => {
                doLikePost()
            }
            case CONTEXT_MENU_ID_ITEM_UNDO_REBLOG => {
                doUndoReblogPost()
            }
        }

        return true
    }

    def doReblogPost () {
        Toast.makeText(this, "Reblogging...", Toast.LENGTH_SHORT).show()

        for ( entry <- getCurrentEntry() ) {
            val task = new ReblogPostTask2(tumblr, { (id : Long) =>
                Toast.makeText(this, "Reblogged " + entry.post.plainCaption + ".", Toast.LENGTH_SHORT).show()
                entry.id = id
            })
            task.execute(entry.post)
        }
    }

    def doLikePost () {
        Toast.makeText(this, "Liking...", Toast.LENGTH_SHORT).show()

        for ( post <- getCurrentPost() ) {
            val task = new LikePostTask2(tumblr, {
                Toast.makeText(this, "Liked.", Toast.LENGTH_SHORT).show()
            })
            task.execute(post)
        }
    }

    def doUndoReblogPost () {
        Toast.makeText(this, "Undoing...", Toast.LENGTH_SHORT).show()

        for ( entry <- getCurrentEntry() ) {
            val task = new DeletePostTask2(tumblr, {
                Toast.makeText(this, "Undone.", Toast.LENGTH_SHORT).show()
                entry.id = 0
            })
            task.execute(entry.id)
        }
    }

    def clearAndGoBackDashboard () {
        captionTextView.setText(getText(R.string.default_caption))
        offset = 0
        entries.clear()
        imagesContainer.removeAllViews()
        dashboardLoading = false

        startLoadingDashboard()
    }
}
