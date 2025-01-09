package org.kaorun.diary.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import org.kaorun.diary.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

	private lateinit var binding: ActivityRegisterBinding
	private lateinit var auth: FirebaseAuth

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		binding = ActivityRegisterBinding.inflate(layoutInflater)
		setContentView(binding.root)

		auth = FirebaseAuth.getInstance()

		binding.registerButton.setOnClickListener {
			val username = binding.username.text.toString()
			val password = binding.password.text.toString()

			if (username.isNotEmpty() && password.isNotEmpty()) {
				auth.createUserWithEmailAndPassword(username, password).addOnCompleteListener {
					if (it.isSuccessful) {
						startActivity(Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY))
						finish()
					} else {
						Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
					}
				}
			}
		}
    }
}
