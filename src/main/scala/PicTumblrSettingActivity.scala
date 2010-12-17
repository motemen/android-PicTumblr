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
                    val thisActivity = PicTumblrSettingActivity.this
                    val email    = thisActivity.findViewById(R.id.email).asInstanceOf[widget.EditText].getText.toString
                    val password = thisActivity.findViewById(R.id.password).asInstanceOf[widget.EditText].getText.toString
                    try {
                        Tumblr.authenticate(email, password) match {
                            case Some(title) => {
                                widget.Toast.makeText(thisActivity, "authentication succeeded: " + title, widget.Toast.LENGTH_SHORT).show
                                // TODO そして保存しメイン画面に戻るとかね
                            }
                            case None => {
                                widget.Toast.makeText(thisActivity, "authentication failed", widget.Toast.LENGTH_SHORT).show
                            }
                        }
                    } catch {
                        case _ => {
                            widget.Toast.makeText(thisActivity, "something went wrong", widget.Toast.LENGTH_SHORT).show
                        }
                    }
                }
            }
        )
    }
}
