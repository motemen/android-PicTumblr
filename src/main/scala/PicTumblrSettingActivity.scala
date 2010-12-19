package net.tokyoenvious.droid.pictumblr

// import net.tokyoenvious.droid.pictumblr._

import android.app.Activity
import android.os.Bundle
import android.widget
import android.view.View
import android.view.View.OnClickListener

class PicTumblrSettingActivity extends Activity {

    override def onCreate(savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.setting)

        val loginButton = findViewById(R.id.login_button).asInstanceOf[widget.Button]
        loginButton.setOnClickListener(
            new OnClickListener () {
                def onClick (v : View) {
                    PicTumblrSettingActivity.this.doLogin()
                }
            }
        )

        val settings = getSharedPreferences("PicTumblr.Auth", 0)
        findViewByIdAs[widget.EditText](R.id.email).setText(settings.getString("email", ""))
        findViewByIdAs[widget.EditText](R.id.password).setText(settings.getString("password", ""))
    }

    def doLogin () {
        val email    = findViewByIdAs[widget.EditText](R.id.email).getText.toString
        val password = findViewByIdAs[widget.EditText](R.id.password).getText.toString

        try {
            Tumblr.authenticate(email, password) match {
                case Some(title) => {
                    widget.Toast.makeText(this, "authentication succeeded: " + title, widget.Toast.LENGTH_SHORT).show

                    val settings = getSharedPreferences("PicTumblr.Auth", 0)
                    val editor   = settings.edit
                    editor.putString("email", email)
                    editor.putString("password", password)
                    editor.commit

                    // TODO
                }
                case None => {
                    widget.Toast.makeText(this, "authentication failed.", widget.Toast.LENGTH_SHORT).show
                }
            }
        } catch {
            case e => {
                widget.Toast.makeText(this, "something went wrong: " + e, widget.Toast.LENGTH_SHORT).show
            }
        }
    }

    def findViewByIdAs[V <: android.view.View](id : Int)
        = findViewById(id).asInstanceOf[V]
        
}
