PicTumblr - Android app for Tumblr picture reblogging
=========

WHAT IS THIS
------------

PicTumblr is an Android app for browsing and reblogging Tumblr dashboard pictures. Flick to navigate posts, and double-tap to reblog.

HOW TO BUILD FROM SOURCE
------------------------

Requirements:

* [Android SDK](http://developer.android.com/sdk/index.html)
* [sbt](https://github.com/harrah/xsbt) 0.11.1


1. Visit [http://www.tumblr.com/oauth/apps](www.tumblr.com/oauth/apps) and register your app to aquire OAuth tokens.

2. Then place src/main/scala/LocalDefs.scala with following content:

	package net.tokyoenvious.droid.pictumblr
	
	object LocalDefs {
	    val TUMBLR_OAUTH_CONSUMER_KEY    = {your oauth consumer key}
	    val TUMBLR_OAUTH_CONSUMER_SECRET = {your oauth consumer secret}
	}

3. Run `sbt install-device`. Or `sbt android:package-debug` to simply build.
