package net.tokyoenvious.droid.pictumblr

import android.util.Log
import java.net.URL
import scala.xml.XML

object Tumblr {
    val API_ROOT = "http://www.tumblr.com/api/"

    // とりあえずタイトルを返す
    def authenticate (email : String, password : String) : Option[String] = {
        val url = new URL(API_ROOT + "authenticate")
        val http = url.openConnection.asInstanceOf[java.net.HttpURLConnection]
        http.setRequestMethod("POST")
        http.setDoOutput(true)
        http.connect

        try {
            val writer = new java.io.OutputStreamWriter(http.getOutputStream)
            writer.write("email=" + email + "&password=" + password) // FIXME
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
}
