package net.tokyoenvious.droid.pictumblr

import java.net.URL
import java.net.HttpURLConnection
import java.io.OutputStreamWriter
import scala.io.Source
import scala.xml.XML

object Tumblr {
    val API_ROOT = "http://www.tumblr.com/api/"

    // とりあえずタイトルを返す
    def authenticate (email : String, password : String) : Option[String] = {
        val url = new URL("http://www.tumblr.com/api/authenticate")
        val http = url.openConnection.asInstanceOf[HttpURLConnection]
        http.setRequestMethod("POST")
        http.setDoOutput(true)
        http.connect

        try {
            val writer = new OutputStreamWriter(http.getOutputStream)
            writer.write("email=" + email + "&password=" + password) // FIXME
            writer.close

            return (XML.load(http.getInputStream) \ "tumblelog").elements.next.attribute("title").map(_.text)
        } catch {
            case _ => return None
        }
    }
}
