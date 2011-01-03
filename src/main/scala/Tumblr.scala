package net.tokyoenvious.droid.pictumblr

import android.util.Log
import scala.xml.XML
import java.net.URL
import java.net.URLEncoder

// TODO 
// import org.apache.http.impl.client.DefaultHttpClient

class Tumblr (email : String, password : String) {
    val API_ROOT = "http://www.tumblr.com/api/"
    val maxWidth = 500

    abstract class Post(id : Long, reblogKey : String)
    case class PhotoPost (id : Long, reblogKey : String, urlWithSlug : String, photoUrl : String, photoLinkUrl : Option[String], photoCaption : String)
            extends Post(id, reblogKey) {

        def plainCaption () : String = {
            return """\s+""".r.replaceAllIn("<.*?>".r.replaceAllIn(photoCaption, ""), " ")
        }
    }

    // とりあえずタイトルを返す
    def authenticate () : Option[String] = {
        try {
            // API lv7 でえらる…… (SDK のバグ？)
            return makeApiRequest("authenticate").iterator.next.attribute("title").map(_.text)
        } catch {
            case e => {
                Log.d("Tumblr.authenticate", e.toString)
                return None
            }
        }
    }

    def dashboard (params : (Symbol, String)*) : Seq[Post] = {
        for {
            postElem <- ( makeApiRequest("dashboard", params ++ Seq('type -> "photo") : _*) \ "posts" \ "post" )
            photoUrl <- (postElem \ "photo-url").reduceLeftOption {
                (node1, node2) => {
                    val w1 = getPhotoUrlNodeMaxWidth(node1)
                    val w2 = getPhotoUrlNodeMaxWidth(node2)
                    if (w1 > w2) node1 else node2
                }
            } map { _.text }
            id           <- ( postElem \ "@id" )           .headOption map { _.text.toLong }
            reblogKey    <- ( postElem \ "@reblog-key" )   .headOption map { _.text }
            photoCaption <- ( postElem \ "photo-caption" ) .headOption map { _.text }
            urlWithSlug  <- ( postElem \ "@url-with-slug" ).headOption map { _.text }
            photoLinkUrl  = ( postElem \ "photo-link-url" ).headOption map { _.text }
        } yield {
            new PhotoPost (id, reblogKey, urlWithSlug, photoUrl, photoLinkUrl, photoCaption)
        }
    }

    // XXX no comment, neither as
    def reblog (post : PhotoPost) { // FIXME Post だとダメ (value id is not a member of Tumblr.this.Post)
        makeRawApiRequest("reblog", Symbol("post-id") -> post.id.toString(), Symbol("reblog-key") -> post.reblogKey) // FIXME Symbol()
    }

    private def getPhotoUrlNodeMaxWidth (node : scala.xml.Node) : Int =
        ( node \ "@max-width" ).headOption map { _.text.toInt } filter { _ < maxWidth } getOrElse(0)

    private def makeApiRequest (function : String, params : (Symbol, String)*) : scala.xml.Elem = {
        return XML.load(makeRawApiRequest(function, params : _*))
    }

    // 200 以外だとしぬのをなんとか
    private def makeRawApiRequest (function : String, params : (Symbol, String)*) : java.io.InputStream = {
        Log.d("Tumblr.makeRawApiRequest", "Requesting " + API_ROOT + function)

        var url = new URL(API_ROOT + function)
        val http = url.openConnection.asInstanceOf[java.net.HttpURLConnection]
        http.setRequestMethod("POST")
        http.setDoOutput(true)
        http.connect

        val writer = new java.io.OutputStreamWriter(http.getOutputStream)
        writer.write(mkPostString(params : _*))
        writer.close

        return http.getInputStream
    }

    private def mkPostString (params : (Symbol, String)*) : String = {
        ( Map('email -> email, 'password -> password) ++ params )
            .map { case (key, value) => key.name + "=" + URLEncoder.encode(value) }
                .mkString("&")
    }
}
