package net.tokyoenvious.droid.pictumblr

import util.control.Exception

import android.util.Log

import org.json.{JSONObject, JSONArray}

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.entity.BufferedHttpEntity
import org.apache.http.entity.StringEntity
import org.apache.http.protocol.HTTP
import org.apache.http.message.BasicNameValuePair

import org.apache.http.client.utils.URIUtils
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.commons.lang.StringEscapeUtils
import org.apache.commons.io.IOUtils

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer

import scala.collection.JavaConversions._

object JSONArray2Seq {
    implicit def jsonArray2Seq (array : JSONArray) = {
        for (i <- 0 until array.length())
            yield array.getJSONObject(i)
    }
}

import JSONArray2Seq._

class Tumblr2 (oauthConsumer : CommonsHttpOAuthConsumer, var baseHostname : String = null) {
    private val TAG = "Tumblr2"
    private lazy val httpClient  = new DefaultHttpClient()

    def dashboardJSON (params : (String, String)*) : Either[Throwable, JSONObject] = Exception.allCatch.either {
        val queryString = URLEncodedUtils.format(
            for ((k, v) <- params) yield new org.apache.http.message.BasicNameValuePair(k, v),
            HTTP.UTF_8
        )
        val uri     = URIUtils.createURI("http", "api.tumblr.com", -1, "/v2/user/dashboard", queryString, null)
        val request = new HttpGet(uri)
        /*
        val builder = new android.net.Uri.Builder()
        builder.scheme("http").authority("api.tumblr.com").path("/v2/user/dashboard")
        for ((k, v) <- params) builder.appendQueryParameter(k, v)
        val request = new HttpGet(builder.build().toString())
        */

        oauthConsumer.sign(request)

        Log.d(TAG, "dashboard requestLine=" + request.getRequestLine())

        val response   = httpClient.execute(request)
        val statusLine = response.getStatusLine()
        val statusCode = statusLine.getStatusCode()

        Log.d(TAG, "dashboard statusLine=" + statusLine)

        if (200 <= statusCode && statusCode < 300) {
            val is = new BufferedHttpEntity(response.getEntity()).getContent()
            val string = IOUtils.toString(is)
            val json = new JSONObject(string)
            Log.v(TAG, "dashboard JSON=" + json.toString(2))
            json
        } else {
            throw new TumblrAuthException(statusLine.toString())
        }
    }

    def dashboardPhotoPosts (params : (String, String)*) : Either[Throwable, Seq[TumblrPhotoPost]]
        = dashboardJSON((Seq("type" -> "photo") ++ params) : _*).right.map {
            _.getJSONObject("response").getJSONArray("posts").map { new TumblrPhotoPost(_) }
        }

    def reblog (post : TumblrPhotoPost) : Long = {
        val request = new HttpPost("http://api.tumblr.com/v2/blog/" + baseHostname + "/post/reblog")
        val httpParams = Seq(
            new BasicNameValuePair("id", post.id.toString()),
            new BasicNameValuePair("reblog_key", post.reblogKey)
        )
        request.setEntity(new UrlEncodedFormEntity(httpParams))

        oauthConsumer.sign(request)

        val response   = httpClient.execute(request)
        val statusLine = response.getStatusLine()
        val statusCode = statusLine.getStatusCode()

        Log.d(TAG, "reblog statusLine=" + statusLine)

        if (200 <= statusCode && statusCode < 300) {
            val is = new BufferedHttpEntity(response.getEntity()).getContent()
            val string = IOUtils.toString(is)
            val json = new JSONObject(string)
            Log.v(TAG, "reblog JSON=" + json.toString(2))
            json.getJSONObject("response").getLong("id")
        } else {
            throw new TumblrAuthException(statusLine.toString())
        }
    }

    def like (post : TumblrPhotoPost) {
        val request = new HttpPost("http://api.tumblr.com/v2/user/like")
        val httpParams = Seq(
            new BasicNameValuePair("id", post.id.toString()),
            new BasicNameValuePair("reblog_key", post.reblogKey)
        )
        request.setEntity(new UrlEncodedFormEntity(httpParams))

        oauthConsumer.sign(request)

        val response   = httpClient.execute(request)
        val statusLine = response.getStatusLine()
        val statusCode = statusLine.getStatusCode()

        Log.d(TAG, "like statusLine=" + statusLine)

        if (200 <= statusCode && statusCode < 300) {
        } else {
            throw new TumblrAuthException(statusLine.toString())
        }
    }

    def deletePost (postId : Long) {
        val request = new HttpPost("http://api.tumblr.com/v2/blog/" + baseHostname + "/post/delete")
        val httpParams = Seq(
            new BasicNameValuePair("id", postId.toString())
        )
        request.setEntity(new UrlEncodedFormEntity(httpParams))

        oauthConsumer.sign(request)

        val response   = httpClient.execute(request)
        val statusLine = response.getStatusLine()
        val statusCode = statusLine.getStatusCode()

        Log.d(TAG, "delete statusLine=" + statusLine)

        if (200 <= statusCode && statusCode < 300) {
        } else {
            throw new TumblrAuthException(statusLine.toString())
        }
    }

    case class Blog (url : android.net.Uri, name : String)

    def fetchBaseHostname () {
        if (baseHostname == null) {
            baseHostname = userInfoPrimaryBlog().url.getHost()
        }
    }

    def userInfoPrimaryBlog () : Blog = {
        val request = new HttpPost("http://api.tumblr.com/v2/user/info")

        oauthConsumer.sign(request)

        val response   = httpClient.execute(request)
        val statusLine = response.getStatusLine()
        val statusCode = statusLine.getStatusCode()

        Log.d(TAG, "userInfo statusLine=" + statusLine)

        if (200 <= statusCode && statusCode < 300) {
            val is = new BufferedHttpEntity(response.getEntity()).getContent()
            val string = IOUtils.toString(is)
            val json = new JSONObject(string)
            json.getJSONObject("response").getJSONObject("user").getJSONArray("blogs").find {
                _.getBoolean("primary")
            }.map { (blogJSON) => 
                Blog(android.net.Uri.parse(blogJSON.getString("url")), blogJSON.getString("name"))
            }.get
        } else {
            throw new TumblrAuthException(statusLine.toString())
        }
    }
}

class TumblrPhoto (val photoJSON : JSONObject) {
    lazy val url    = photoJSON.getString("url")
    lazy val width  = photoJSON.getInt("width")
    lazy val height = photoJSON.getInt("height")
}

class TumblrPhotoPost (val postJSON : JSONObject) {
    type AltPhotos = Seq[TumblrPhoto]

    val TAG = "TumblrPhotoPost"

    lazy val id        = postJSON.getLong("id")
    lazy val reblogKey = postJSON.getString("reblog_key")
    lazy val postUrl   = postJSON.getString("post_url")
    lazy val linkUrl   = postJSON.getString("link_url")
    lazy val caption   = postJSON.getString("caption")

    lazy val plainCaption : String = {
        val plainText = caption.replaceAll("<.*?>", "").replaceAll("""\s+""", " ")
        val entity    = new StringEntity(plainText, HTTP.UTF_8)
        StringEscapeUtils.unescapeHtml(IOUtils.toString(entity.getContent))
    }

    lazy val photos : Seq[AltPhotos] = try {
        for (photo <- postJSON.getJSONArray("photos"))
            yield Seq(new TumblrPhoto(photo.getJSONObject("original_size"))) ++
                photo.getJSONArray("alt_sizes").map { new TumblrPhoto(_) }
    } catch {
        case e : org.json.JSONException => {
            Log.w(TAG, e.toString())
            Log.w(TAG, "json=" + postJSON.toString())
            Seq()
        }
    }

    def largestPhotoWithMaxWidth (width : Int) : TumblrPhoto = {
        photos(0).filter { p => p.width <= width } maxBy { p => p.width }
    }
}

class TumblrAuthException (message : String) extends Exception(message) {
}
