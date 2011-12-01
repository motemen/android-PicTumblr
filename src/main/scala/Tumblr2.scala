package net.tokyoenvious.droid.pictumblr

import util.control.Exception

import android.util.Log

import org.json.{JSONObject, JSONArray}

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.entity.BufferedHttpEntity
import org.apache.http.entity.StringEntity
import org.apache.http.protocol.HTTP

import org.apache.http.client.utils.URIUtils
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.commons.lang.StringEscapeUtils
import org.apache.commons.io.IOUtils

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer

import scala.collection.JavaConversions._

class Tumblr2 (oauthConsumer : CommonsHttpOAuthConsumer) {
    val TAG = "Tumblr2"

    implicit def jsonArray2Seq (array : JSONArray) = {
        for (i <- 0 until array.length())
            yield array.getJSONObject(i)
    }

    class Photo (val photoJSON : JSONObject) {
        lazy val url    = photoJSON.getString("url")
        lazy val width  = photoJSON.getInt("width")
        lazy val height = photoJSON.getInt("height")
    }

    class PhotoPost (val postJSON : JSONObject) {
        type AltPhotos = Seq[Photo]

        lazy val id        = postJSON.getLong("id")
        lazy val reblogKey = postJSON.getLong("reblog_key")
        lazy val postUrl   = postJSON.getString("post_url")
        lazy val caption   = postJSON.getString("caption")

        lazy val plainCaption : String = {
            val plainText = caption.replaceAll("<.*?>", "").replaceAll("""\s+""", " ")
            val entity    = new StringEntity(plainText, HTTP.UTF_8)
            StringEscapeUtils.unescapeHtml(IOUtils.toString(entity.getContent))
        }

        lazy val photos : Seq[AltPhotos] = try {
            for (photo <- postJSON.getJSONArray("photos"))
                yield Seq(new Photo(photo.getJSONObject("original_size"))) ++
                    photo.getJSONArray("alt_sizes").map { new Photo(_) }
        } catch {
            case e : org.json.JSONException => {
                Log.w(TAG, e.toString())
                Log.w(TAG, "json=" + postJSON.toString())
                Seq()
            }
        }

        def largestPhotoWithMaxWidth (width : Int) : Photo = {
            photos(0).filter { p => p.width <= width } maxBy { p => p.width }
        }
    }

    def dashboardJSON (params : (String, String)*) : Either[Throwable, JSONObject] = Exception.allCatch.either {
        val httpClient  = new DefaultHttpClient()
        val queryString = URLEncodedUtils.format(
            for ((k, v) <- params) yield new org.apache.http.message.BasicNameValuePair(k, v),
            HTTP.UTF_8
        )
        val uri     = URIUtils.createURI("http", "api.tumblr.com", -1, "/v2/user/dashboard", queryString, null)
        val request = new HttpGet(uri)

        oauthConsumer.sign(request)

        Log.v(TAG, "dashboard requestLine=" + request.getRequestLine())

        val response   = httpClient.execute(request)
        val statusLine = response.getStatusLine()
        val statusCode = statusLine.getStatusCode()

        Log.v(TAG, "dashboard statusLine=" + statusLine)

        if (200 <= statusCode && statusCode < 300) {
            val is = new BufferedHttpEntity(response.getEntity()).getContent()
            val string = IOUtils.toString(is)
            val json = new JSONObject(string)
            Log.v(TAG, "dashboard JSON=" + json.toString(2))
            json
        } else {
            throw new Exception(statusLine.toString())
        }
    }

    def dashboardPhotoPosts (params : (String, String)*) : Either[Throwable, Seq[PhotoPost]]
        = dashboardJSON((Seq("type" -> "photo") ++ params) : _*).right.map {
            _.getJSONObject("response").getJSONArray("posts").map { new PhotoPost(_) }
        }

}
