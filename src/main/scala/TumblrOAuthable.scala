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

    // val PREFERENCE_NAME_OAUTH        = "OAuth"
    val PREFERENCE_KEY_TOKEN         = "token"
    val PREFERENCE_KEY_TOKEN_SECRET  = "token_secret"
    val PREFERENCE_KEY_BASE_HOSTNAME = "base_hostname"

    var tumblr : Tumblr2 = null

    private lazy val oauthConsumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET)
    private lazy val oauthProvider = new CommonsHttpOAuthProvider(
        "http://www.tumblr.com/oauth/request_token",
        "http://www.tumblr.com/oauth/access_token",
        "http://www.tumblr.com/oauth/authorize"
    )

    private def getPrefs ()
        = android.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext())

    // background
    def oauthAuthorize () {
        val uri = getIntent().getData()
        Log.v(TAG, "Got intent: " + uri)

        val prefs        = getPrefs()
        val token        = prefs.getString(PREFERENCE_KEY_TOKEN, null)
        val tokenSecret  = prefs.getString(PREFERENCE_KEY_TOKEN_SECRET, null)
        var baseHostname = prefs.getString(PREFERENCE_KEY_BASE_HOSTNAME, null)
        Log.v(TAG, "saved token: " + token + " tokenSecret: " + tokenSecret + " baseHostname: " + baseHostname)

        if (token != null && tokenSecret != null) {
            oauthConsumer.setTokenWithSecret(token, tokenSecret)
        }

        val editor = prefs.edit()

        if (uri != null && uri.toString().startsWith(CALLBACK_URL)) {
            val verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER)
            oauthProvider.setOAuth10a(true)
            oauthProvider.retrieveAccessToken(oauthConsumer, verifier)

            editor.putString(PREFERENCE_KEY_TOKEN,        oauthConsumer.getToken())
            editor.putString(PREFERENCE_KEY_TOKEN_SECRET, oauthConsumer.getTokenSecret())

            Log.d(TAG, "Got token: " + oauthConsumer.getToken())
            Log.d(TAG, "Got token_secret: " + oauthConsumer.getTokenSecret())
        }

        tumblr = new Tumblr2(oauthConsumer, baseHostname)
        tumblr.fetchBaseHostname()

        if (tumblr.baseHostname != baseHostname) {
            editor.putString(PREFERENCE_KEY_BASE_HOSTNAME, tumblr.baseHostname)
            Log.v(TAG, "Got baseHostname: " + tumblr.baseHostname)
        }

        editor.commit()
    }

    // foreground
    def startOAuth () {
        eraseAuthTokens()

        val url = oauthProvider.retrieveRequestToken(oauthConsumer, CALLBACK_URL)

        Log.v(TAG, "Generated token: "        + oauthConsumer.getToken())
        Log.v(TAG, "Generated token_secret: " + oauthConsumer.getTokenSecret())

        val prefs  = getPrefs()
        val editor = prefs.edit()
        editor.putString(PREFERENCE_KEY_TOKEN,        oauthConsumer.getToken())
        editor.putString(PREFERENCE_KEY_TOKEN_SECRET, oauthConsumer.getTokenSecret())
        editor.commit()

        val intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
        startActivity(intent) // tumblr.com -> (user allows) -[callback]-> this Activity ( -> oauthConsumer)
        /*
        startActivity(Intent.parseUri(url, 0)) // tumblr.com -> (user allows) -[callback]-> this Activity ( -> oauthConsumer)
        */
    }

    // foreground
    def eraseAuthTokens () {
        val prefs  = getPrefs()
        val editor = prefs.edit()
        editor.remove(PREFERENCE_KEY_TOKEN)
        editor.remove(PREFERENCE_KEY_TOKEN_SECRET)
        editor.remove(PREFERENCE_KEY_BASE_HOSTNAME)
        editor.commit()
    }
}
