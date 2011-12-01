package net.tokyoenvious.droid.pictumblr

import android.app.Activity
import android.content.Intent
import android.util.Log

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider
import oauth.signpost.OAuth

trait TumblrOAuthable extends Activity {
    private val TAG = "TumblrOAuthable"

    val CONSUMER_KEY    = LocalDefs.TUMBLR_OAUTH_CONSUMER_KEY
    val CONSUMER_SECRET = LocalDefs.TUMBLR_OAUTH_CONSUMER_SECRET

    val CALLBACK_URL = "pictumblr://oauth/callback"

    val PREFERENCE_NAME_OAUTH       = "OAuth"
    val PREFERENCE_KEY_TOKEN        = "token"
    val PREFERENCE_KEY_TOKEN_SECRET = "token_secret"

    private lazy val oauthConsumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET)
    private lazy val oauthProvider = new CommonsHttpOAuthProvider(
        "http://www.tumblr.com/oauth/request_token",
        "http://www.tumblr.com/oauth/access_token",
        "http://www.tumblr.com/oauth/authorize"
    )

    def oauthAuthorize () : CommonsHttpOAuthConsumer = {
        val uri = getIntent().getData()
        Log.v(TAG, "Got intent: " + uri)

        val prefs       = getSharedPreferences(PREFERENCE_NAME_OAUTH, 0)
        val token       = prefs.getString(PREFERENCE_KEY_TOKEN, null)
        val tokenSecret = prefs.getString(PREFERENCE_KEY_TOKEN_SECRET, null)
        Log.v(TAG, "token: " + token + " tokenSecret: " + tokenSecret)

        if (token != null && tokenSecret != null) {
            oauthConsumer.setTokenWithSecret(token, tokenSecret)
        }

        if (uri != null && uri.toString().startsWith(CALLBACK_URL)) {
            val verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER)
            oauthProvider.setOAuth10a(true)
            oauthProvider.retrieveAccessToken(oauthConsumer, verifier)

            val prefs = getSharedPreferences(PREFERENCE_NAME_OAUTH, 0)
            val editor = prefs.edit()
            editor.putString(PREFERENCE_KEY_TOKEN,        oauthConsumer.getToken())
            editor.putString(PREFERENCE_KEY_TOKEN_SECRET, oauthConsumer.getTokenSecret())
            editor.commit()

            Log.d(TAG, "Got token: " + oauthConsumer.getToken())
            Log.d(TAG, "Got token_secret: " + oauthConsumer.getTokenSecret())
        }

        return oauthConsumer
    }

    def startOAuth () {
        val url = oauthProvider.retrieveRequestToken(oauthConsumer, CALLBACK_URL)

        Log.v(TAG, "Generated token: "        + oauthConsumer.getToken())
        Log.v(TAG, "Generated token_secret: " + oauthConsumer.getTokenSecret())

        val prefs = getSharedPreferences(PREFERENCE_NAME_OAUTH, 0)
        val editor = prefs.edit()
        editor.putString(PREFERENCE_KEY_TOKEN,        oauthConsumer.getToken())
        editor.putString(PREFERENCE_KEY_TOKEN_SECRET, oauthConsumer.getTokenSecret())
        editor.commit()

        val intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
        startActivity(intent) // tumblr.com -> (user allows) -[callback]-> this Activity
    }
}
