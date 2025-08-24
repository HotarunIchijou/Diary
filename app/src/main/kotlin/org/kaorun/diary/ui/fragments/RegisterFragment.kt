package org.kaorun.diary.ui.fragments

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.android.material.color.MaterialColors
import com.google.firebase.auth.FirebaseAuth
import org.kaorun.diary.R
import org.kaorun.diary.databinding.FragmentRegisterBinding
import org.kaorun.diary.ui.activities.MainActivity
import org.kaorun.diary.utils.InsetsHandler

class RegisterFragment : BaseFragment() {

	private lateinit var auth: FirebaseAuth
	private lateinit var username: String
	private lateinit var password: String
	private var _binding: FragmentRegisterBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentRegisterBinding.inflate(inflater, container, false)
		InsetsHandler.applyViewInsets(binding.root, isTopPadding = true)

		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		auth = FirebaseAuth.getInstance()

		liftOnKeyboardOpen()

		val textWatcher = object : TextWatcher {
			override fun afterTextChanged(s: Editable?) {
				username = binding.username.text.toString()
				password = binding.password.text.toString()
				binding.register.isEnabled = username.isNotEmpty() && password.isNotEmpty()
			}

			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
		}

		binding.username.addTextChangedListener(textWatcher)
		binding.password.addTextChangedListener(textWatcher)

		binding.toolbar.setNavigationOnClickListener {
			val supportFragmentManager = requireActivity().supportFragmentManager
			supportFragmentManager.popBackStack()
		}

		binding.register.setOnClickListener {
			val currentUser = auth.currentUser
			binding.username.clearFocus()
			binding.password.clearFocus()
			val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
					as InputMethodManager
			imm.hideSoftInputFromWindow(binding.username.windowToken, 0)
			imm.hideSoftInputFromWindow(binding.password.windowToken, 0)

			if (currentUser != null) {
				// Check if the email is verified
				currentUser.reload().addOnCompleteListener { reloadTask ->
					if (reloadTask.isSuccessful) {
						if (currentUser.isEmailVerified) {
							val intent = Intent(requireContext(), MainActivity::class.java)
							startActivity(intent)
							requireActivity().finish()
						} else {
							with(binding.hintText) {
								setTextColor(MaterialColors.getColor(view, android.R.attr.colorError))
								text = getString(R.string.verify_email)
							}
						}
					} else {
						with(binding.hintText) {
							setTextColor(MaterialColors.getColor(view, android.R.attr.colorError))
							text = getString(R.string.verification_status_failed)
						}
					}
				}
			}
			else {
				// Register the user
				username = binding.username.text.toString()
				password = binding.password.text.toString()

				auth.createUserWithEmailAndPassword(username, password).addOnCompleteListener { task ->
					if (task.isSuccessful) {
						val user = auth.currentUser
						user?.sendEmailVerification()
							?.addOnCompleteListener { verificationTask ->
								if (verificationTask.isSuccessful) {
									with (binding.hintText) {
										setTextColor(MaterialColors.getColor(view, android.R.attr.colorPrimary))
										text = getString(R.string.verification_sent)
									}
								} else {
									with(binding.hintText) {
										setTextColor(MaterialColors.getColor(view, android.R.attr.colorError))
										text = getString(R.string.verification_failed)
									}
								}
							}
							?.addOnFailureListener { exception ->
								Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_SHORT).show()
							}
					}
					else {
						task.exception?.let {
							when (it.message!!.contains("PASSWORD_DOES_NOT_MEET_REQUIREMENTS")) {
								true -> with(binding.hintText) {
									setTextColor(MaterialColors.getColor(view, android.R.attr.colorError))
									text = getString(R.string.weak_password)
								}

								false -> with(binding.hintText) {
									setTextColor(MaterialColors.getColor(view, android.R.attr.colorError))
									text = getString(R.string.registration_failed)
								}
							}
						}
					}
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
					binding.infoContainer.visibility = View.INVISIBLE

					animateViewTranslation(binding.imageView, -100f)
					animateViewTranslation(binding.usernameContainer, -100f)
					animateViewTranslation(binding.passwordContainer, -100f)
				}
			} else {
				if (binding.titleBar.visibility != View.VISIBLE) {
					animateViewTranslation(binding.titleBar, 0f)
					binding.titleBar.visibility = View.VISIBLE
					binding.infoContainer.visibility = View.VISIBLE

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
