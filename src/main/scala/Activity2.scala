package net.tokyoenvious.droid.pictumblr

import android.content.Intent
import android.view.ViewGroup
import android.view.{Menu, ContextMenu, MenuItem}
import android.widget.ImageView
import android.graphics.Bitmap
import android.util.Log

import scala.collection.mutable.Queue

case class Entry (post : TumblrPhotoPost, var task : LoadPhotoTask2, var bitmap : Bitmap)

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

    override def onCreate (savedInstanceState : android.os.Bundle) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main)

        setupSteppedHorizontalScrollView()

        try {
            oauthAuthorize()
        } catch {
            case e => {
                e.printStackTrace()
                Log.w(TAG, e.toString())
                startOAuth() // TODO check error type
            }
        }

        createLoadDashboardTask().execute(0)
    }

    def setupSteppedHorizontalScrollView () {
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
            TODO("doReblogPost")
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

        val newEntries = loadedPosts.map { Entry(_, null, null) }

        for ((entry, i) <- newEntries.zipWithIndex) {
            val imageContainer = addNewImageContainer()
            entry.task = createLoadPhotoTask(entry, imageContainer)
            entry.task.execute()
        }

        entries ++= newEntries
        offset += entries.length
        Log.d(TAG, "offset=" + offset)

        updateCaption()
    }

    def createLoadDashboardTask () = {
        new LoadDashboardTask2(
            tumblr  = tumblr,
            onLoad  = onDashboardLoad,
            onError = (error : Throwable) => {
                error.printStackTrace()
                Log.w(TAG, error.getMessage())
                startOAuth() // TODO check error type
            }
        )
    }

    def createLoadPhotoTask (entry : Entry, imageContainer : ViewGroup) : LoadPhotoTask2 = {
        new LoadPhotoTask2(
            maxWidth = maxWidth,
            imageContainer = imageContainer,
            photoPost = entry.post,
            onLoad = (bitmap : Bitmap) => {
                entry.bitmap = bitmap

                val imageView = new ForgetfulBitmapImageView(entry, PicTumblrActivity2.this)
                imageContainer.addView(imageView)
            }
        )
    }

    def getCurrentIndex () : Int = {
        val scrollX = steppedHorizontalScrollView.getScrollX()
        val index = scrollX / displayWidth
        val delta = scrollX % displayWidth

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

    def getCurrentPost (delta : Int = 0) : Option[TumblrPhotoPost]
        = entries.get(getCurrentIndex() + delta) map { _.post }

    def updateCaption (delta : Int = 0) {
        captionTextView.setText(
            getCurrentPost(delta).map { _.plainCaption }.getOrElse(getText(R.string.default_caption))
        )
    }

    def loadNewPosts () {
        var index = getCurrentIndex()
        if (entries.length - index <= forwardOffset) {
            createLoadDashboardTask().execute(offset)
        }
    }

    /****** MENUS ******/

    val MENU_ITEM_ID_REFRESH = Menu.FIRST + 1
    val MENU_ITEM_ID_SETTING = Menu.FIRST + 2

    val CONTEXT_MENU_ID_ITEM_OPEN_TUMBLR     = Menu.FIRST + 3
    val CONTEXT_MENU_ID_ITEM_OPEN_PHOTO_LINK = Menu.FIRST + 4
    val CONTEXT_MENU_ID_ITEM_REBLOG          = Menu.FIRST + 5
    val CONTEXT_MENU_ID_ITEM_LIKE            = Menu.FIRST + 6

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
            case MENU_ITEM_ID_REFRESH => TODO("clearAndGoBackDashboard")
            case MENU_ITEM_ID_SETTING => startActivity(new Intent(this, classOf[PicTumblrPrefernceActivity]))
        }

        return true
    }

    override def onCreateContextMenu (menu : ContextMenu, v : android.view.View, menuInfo : ContextMenu.ContextMenuInfo) {
        for ( post <- getCurrentPost() ) {
            menu.setHeaderTitle(post.plainCaption)
            
            val itemOpenTumblr    = menu.add(Menu.NONE, CONTEXT_MENU_ID_ITEM_OPEN_TUMBLR,     Menu.NONE, "Open Tumblr Page")
            val itemOpenPhotoLink = menu.add(Menu.NONE, CONTEXT_MENU_ID_ITEM_OPEN_PHOTO_LINK, Menu.NONE, "Open Photo Link")
            val itemReblog        = menu.add(Menu.NONE, CONTEXT_MENU_ID_ITEM_REBLOG,          Menu.NONE, "Reblog")
            val itemLike          = menu.add(Menu.NONE, CONTEXT_MENU_ID_ITEM_LIKE,            Menu.NONE, "Like")

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
                for ( post <- getCurrentPost(); photo <- post.photos.headOption; largestPhoto <- photo.headOption ) {
                    openUrl(largestPhoto.url)
                }
            }
            case CONTEXT_MENU_ID_ITEM_REBLOG => {
                for ( post <- getCurrentPost() ) {
                    tumblr.reblog(post)
                }
            }
            case CONTEXT_MENU_ID_ITEM_LIKE => TODO("doLikePost")
        }

        return true
    }

}
