package net.tokyoenvious.droid.pictumblr

import java.net.URL
import java.net.HttpURLConnection
import java.io.OutputStreamWriter
import scala.io.Source

object Tumblr {
    val API_ROOT = "http://www.tumblr.com/api/"

    def authenticate (email : String, password : String) : Boolean = {
        val url = new URL("http://www.tumblr.com/api/authenticate")
        val http = url.openConnection.asInstanceOf[HttpURLConnection]
        http.setRequestMethod("POST")
        http.setDoOutput(true)
        http.connect

        val writer = new OutputStreamWriter(http.getOutputStream)
        writer.write("email=" + email + "&password=" + password) // FIXME
        writer.close

        // TODO

        val in = Source.fromInputStream(http.getInputStream)
        println(in.getLines.toList)

        return false
    }
}
