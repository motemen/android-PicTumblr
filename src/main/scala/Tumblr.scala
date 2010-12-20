package net.tokyoenvious.droid.pictumblr

import android.util.Log
import java.net.URL
import java.net.URLEncoder
import scala.xml.XML

class Tumblr (email : String, password : String) {
    val API_ROOT = "http://www.tumblr.com/api/"

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
        var url = new URL(API_ROOT + "dashboard")
        val http = url.openConnection.asInstanceOf[java.net.HttpURLConnection]
        http.setRequestMethod("POST")
        http.setDoOutput(true)
        http.connect

        val writer = new java.io.OutputStreamWriter(http.getOutputStream)
        writer.write(mkPostString("type" -> "photo"))
        writer.close

        (XML.load(http.getInputStream) \ "posts" \ "post")
            .map(postElem => PhotoPost((postElem \ "photo-url").first.text))
    }

    def mkPostString (params : (String, String)*) : String = {
        ( Map("email" -> email, "password" -> password) ++ params )
            .map { case (key, value) => key + "=" + URLEncoder.encode(value) }
                .mkString("&")
    }
}
