package net.tokyoenvious.droid.pictumblr

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.view.View
import android.view.View.OnClickListener

class PicTumblrSettingActivity extends Activity {
    override def onCreate(savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.setting)

        val loginButton = findViewById(R.id.login_button).asInstanceOf[Button]
        loginButton.setOnClickListener(
            new OnClickListener () {
                def onClick (v : View) {
                    v.asInstanceOf[Button].setText("Clicked")
                }
            }
        )
    }
}
