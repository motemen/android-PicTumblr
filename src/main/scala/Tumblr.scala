package net.tokyoenvious.droid.pictumblr

import android.util.Log
import scala.xml.XML
import scala.io.Source
import scala.collection.JavaConversions._
import util.control.Exception
import java.net.URL
import java.net.URLEncoder

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.params.BasicHttpParams
import org.apache.http.entity.BufferedHttpEntity
import org.apache.http.entity.StringEntity
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.HTTP
import org.apache.commons.lang.StringEscapeUtils

class Tumblr (email : String, password : String) {
    type MaybeError[A] = Either[String, A]

    val API_ROOT = "http://www.tumblr.com/api/"
    val maxWidth = 500 // TODO make configurable

    class PhotoPost (val id : Long, val reblogKey : String, val urlWithSlug : String, val photoUrl : String, val photoLinkUrl : Option[String], val photoCaption : String) {
        lazy val plainCaption : String = {
            val plainText = photoCaption.replaceAll("<.*?>", "").replaceAll("""\s+""", " ")
            val entity    = new StringEntity(plainText, HTTP.UTF_8)
            StringEscapeUtils.unescapeHtml(Source.fromInputStream(entity.getContent).mkString)
        }
    }

    def dashboard (params : (String, String)*) : MaybeError[Seq[PhotoPost]] = {
        makeApiRequest("dashboard", params ++ Seq("type" -> "photo") : _*).right map {
            xml => for {
                postElem <- ( xml \ "posts" \ "post" )
                photoUrl <- ( postElem \ "photo-url" ).reduceLeftOption {
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
    }

    // XXX no 'comment', neither 'as'
    def reblog (post : Tumblr#PhotoPost) {
        makeRawApiRequest("reblog", "post-id" -> post.id.toString(), "reblog-key" -> post.reblogKey)
    }

    def like (post : Tumblr#PhotoPost) {
        makeRawApiRequest("like", "post-id" -> post.id.toString(), "reblog-key" -> post.reblogKey)
    }

    private def getPhotoUrlNodeMaxWidth (node : scala.xml.Node) : Int =
        ( node \ "@max-width" ).headOption map { _.text.toInt } filter { _ < maxWidth } getOrElse(0)

    def makeApiRequest (function : String, params : (String, String)*) : MaybeError[scala.xml.Elem] = {
        makeRawApiRequest(function, params : _*).right map { XML.load(_) }
    }

    def makeRawApiRequest (function : String, params : (String, String)*) : MaybeError[java.io.InputStream]
        = Exception.allCatch.either {
            Log.d("Tumblr#makeRawApiRequest", "Requesting " + API_ROOT + function)

            val httpClient = new DefaultHttpClient
            val httpPost   = new HttpPost(API_ROOT + function)
            val httpParams = for ((key, value) <- params ++ Seq("email" -> email, "password" -> password)) yield {
                new BasicNameValuePair(key, value)
            }
            httpPost.setEntity(new UrlEncodedFormEntity(httpParams))

            val httpResponse = httpClient.execute(httpPost)
            val statusLine   = httpResponse.getStatusLine
            Log.d("Tumblr#makeRawApiRequest", "Status: " + statusLine)

            // TODO error on non-2xx
            val statusCode = statusLine.getStatusCode
            val in = new BufferedHttpEntity(httpResponse.getEntity).getContent
            if (200 <= statusCode && statusCode <= 299) {
                Right { in }
            } else {
                Left { Source.fromInputStream(in).mkString }
            }
        }
        .left.map { _.toString }
        .right.flatMap { x => x }
}
