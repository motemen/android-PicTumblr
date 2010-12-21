package net.tokyoenvious.droid.pictumblr

import android.util.Log
import scala.xml.XML
import java.net.URL
import java.net.URLEncoder

class Tumblr (email : String, password : String) {
    val API_ROOT = "http://www.tumblr.com/api/"
    val maxWidth = 500

    abstract class Post
    case class PhotoPost (photoUrl : String) extends Post

    // とりあえずタイトルを返す
    def authenticate () : Option[String] = {
        val url = new URL(API_ROOT + "authenticate")
        val http = url.openConnection.asInstanceOf[java.net.HttpURLConnection]
        http.setRequestMethod("POST")
        http.setDoOutput(true)
        http.connect

        try {
            val writer = new java.io.OutputStreamWriter(http.getOutputStream)
            writer.write(mkPostString())
            writer.close

            // API lv7 でえらる…… (SDK のバグ？)
            return (XML.load(http.getInputStream) \ "tumblelog").elements.next.attribute("title").map(_.text)
        } catch {
            case e => {
                Log.d("Tumblr.authenticate", e.toString)
                return None
            }
        }
    }

    def dashboard () : Seq[Post] = {
        Log.d("Tumblr.dashboard", "Requesting " + API_ROOT + "dashboard")

        var url = new URL(API_ROOT + "dashboard")
        val http = url.openConnection.asInstanceOf[java.net.HttpURLConnection]
        http.setRequestMethod("POST")
        http.setDoOutput(true)
        http.connect

        val writer = new java.io.OutputStreamWriter(http.getOutputStream)
        writer.write(mkPostString('type -> "photo"))
        writer.close

        ( XML.load(http.getInputStream) \ "posts" \ "post" )
            .map {
                postElem => (postElem \ "photo-url").foldLeft(None.asInstanceOf[Option[scala.xml.Node]]) {
                    (node1_, node2) => node1_ match {
                        case None => Some(node2)
                        case Some(node1) => {
                            val w1 = getPhotoUrlNodeMaxWidth(node1)
                            val w2 = getPhotoUrlNodeMaxWidth(node2)
                            if (w1 > w2) Some(node1) else Some(node2)
                        }
                    }
                }
            }
            .filter { _ isDefined } .map { _.get }
            .map {
                photoUrlNode => PhotoPost(photoUrlNode.text)
            }
    }

    private def getPhotoUrlNodeMaxWidth (node : scala.xml.Node) =
        ( node \ "@max-width" ).firstOption map { _.text.toInt } filter { _ < maxWidth } getOrElse(0)

    /*
    private def makeApiRequest (function : String, params : (Symbol, String)*) : scala.xml.Elem = {
        var url = new URL(API_ROOT + function)
        val http = url.openConnection.asInstanceOf[java.net.HttpURLConnection]
        http.setRequestMethod("POST")
        http.setDoOutput(true)
        http.connect

        val writer = new java.io.OutputStreamWriter(http.getOutputStream)
        writer.write(mkPostString(params))
        writer.close

        return XML.load(http.getInputStream)
    }
    */

    private def mkPostString (params : (Symbol, String)*) : String = {
        ( Map('email -> email, 'password -> password) ++ params )
            .map { case (key, value) => key.name + "=" + URLEncoder.encode(value) }
                .mkString("&")
    }
}
