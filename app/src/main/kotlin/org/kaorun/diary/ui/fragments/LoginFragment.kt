package org.kaorun.diary.ui.fragments

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import org.kaorun.diary.R
import org.kaorun.diary.databinding.FragmentLoginBinding
import org.kaorun.diary.ui.activities.MainActivity
import org.kaorun.diary.utils.InsetsHandler

class LoginFragment : BaseFragment() {

	private lateinit var auth: FirebaseAuth
	private lateinit var username: String
	private lateinit var password: String
	private var _binding: FragmentLoginBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentLoginBinding.inflate(inflater, container, false)
		InsetsHandler.applyViewInsets(binding.root, isTopPadding = true)

		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		auth = FirebaseAuth.getInstance()

		liftOnKeyboardOpen()

		val loginTextWatcher = object : TextWatcher {
			override fun afterTextChanged(s: Editable?) {
				username = binding.username.text.toString()
				password = binding.password.text.toString()
				binding.login.isEnabled = username.isNotEmpty() && password.isNotEmpty()
			}

			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
		}

		binding.username.addTextChangedListener(loginTextWatcher)
		binding.password.addTextChangedListener(loginTextWatcher)

		binding.toolbar.setNavigationOnClickListener {
			val supportFragmentManager = requireActivity().supportFragmentManager
			supportFragmentManager.popBackStack()
		}

		binding.login.setOnClickListener{
			auth.signInWithEmailAndPassword(username, password).addOnCompleteListener{
				if (it.isSuccessful){
					if (auth.currentUser!!.isEmailVerified) {
						activity?.let { context ->
							val intent = Intent(context, MainActivity::class.java)
							startActivity(intent)

							requireActivity().finish()
						}
					}
					else {
						auth.currentUser!!.sendEmailVerification()
						Toast.makeText(requireContext(), getString(R.string.verify_email), Toast.LENGTH_SHORT).show()
					}
				}
				else {
					val errorMessage = it.exception?.message ?: "Login failed. Please try again"
					Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
				}
			}
		}
	}

	private fun liftOnKeyboardOpen() {
		binding.root.viewTreeObserver.addOnPreDrawListener {
			val rect = Rect()
			binding.root.getWindowVisibleDisplayFrame(rect)

			val screenHeight = binding.root.height
			val visibleHeight = rect.bottom

			if (screenHeight - visibleHeight > 100) {
				if (binding.titleBar.visibility != View.GONE) {
					animateViewTranslation(binding.titleBar, -100f)
					binding.titleBar.visibility = View.GONE

					animateViewTranslation(binding.imageView, -100f)
					animateViewTranslation(binding.usernameContainer, -100f)
					animateViewTranslation(binding.passwordContainer, -100f)
				}
			} else {
				if (binding.titleBar.visibility != View.VISIBLE) {
					animateViewTranslation(binding.titleBar, 0f)
					binding.titleBar.visibility = View.VISIBLE

					animateViewTranslation(binding.imageView, 0f)
					animateViewTranslation(binding.usernameContainer, 0f)
					animateViewTranslation(binding.passwordContainer, 0f)
				}
			}
			true
		}
	}

	private fun animateViewTranslation(view: View, targetTranslationY: Float) {
		val animator = ValueAnimator.ofFloat(view.translationY, targetTranslationY)
		animator.duration = 150
		animator.addUpdateListener { animation ->
			view.translationY = animation.animatedValue as Float
		}
		animator.start()
	}
}
