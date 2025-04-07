@file:Suppress("DEPRECATION")

package org.kaorun.diary.ui.fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.kaorun.diary.R
import org.kaorun.diary.databinding.FragmentWelcomeBinding
import org.kaorun.diary.ui.activities.MainActivity
import org.kaorun.diary.ui.utils.InsetsHandler

class WelcomeFragment : BaseFragment() {

	private var _binding: FragmentWelcomeBinding? = null
	private val binding get() = _binding!!
	private val auth = FirebaseAuth.getInstance()

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentWelcomeBinding.inflate(inflater, container, false)
		InsetsHandler.applyViewInsets(binding.root)

		return binding.root
	}


	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

		binding.emailButton.setOnClickListener {
			val supportFragmentManager = requireActivity().supportFragmentManager
			supportFragmentManager.commit {
				add(R.id.fragmentContainerView, LoginFragment())
				addToBackStack(null)
			}
		}

		binding.registerButton.setOnClickListener {
			val supportFragmentManager = requireActivity().supportFragmentManager
			supportFragmentManager.commit {
				add(R.id.fragmentContainerView, RegisterFragment())
				addToBackStack(null)
			}
		}

		binding.googleButton.setOnClickListener {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) signInWithGoogleOld()
			else continueWithGoogle()
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	private fun continueWithGoogle() {
		val googleIdOption = GetGoogleIdOption.Builder()
			.setFilterByAuthorizedAccounts(false)
			.setServerClientId(getString(R.string.default_web_client_id))
			.build()

		val request = GetCredentialRequest.Builder()
			.addCredentialOption(googleIdOption)
			.build()

		val credentialManager = CredentialManager.create(requireContext())

		lifecycleScope.launch {
			try {
				val result = credentialManager.getCredential(
					request = request,
					context = requireContext()
				)

				val credential = result.credential
				if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
					val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
					val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
					val data = Firebase.auth.signInWithCredential(firebaseCredential).await()

					Log.i("Success", "${data.user?.displayName} ${data.user?.email} ${data.user?.uid}")

					val intent = Intent(context, MainActivity::class.java)
					startActivity(intent)

					requireActivity().finish()
				}
			}
			catch (e: GetCredentialCancellationException) {
				Log.e("Credential", "An error occurred: ${e.message}", e)
			}
			catch (e: GetCredentialProviderConfigurationException) {
				Log.e("Credential", "An error occurred: ${e.message}", e)
				Toast.makeText(requireContext(),e.message, Toast.LENGTH_SHORT).show()
			}
			catch (e: NoCredentialException) {
				Log.e("Credential", "An error occurred: ${e.message}", e)
				Toast.makeText(requireContext(), getString(R.string.no_accounts_found), Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun signInWithGoogleOld() {
		val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
			.requestIdToken(getString(R.string.default_web_client_id))
			.requestEmail()
			.build()

		val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
		val signInIntent = googleSignInClient.signInIntent
		startActivityForResult(signInIntent, 9001)
	}

	@Deprecated("Deprecated in Java",
		ReplaceWith("super.onActivityResult(requestCode, resultCode, data)"))
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)

		if (requestCode == 9001) {
			val task = GoogleSignIn.getSignedInAccountFromIntent(data)
			try {
				val account = task.getResult(ApiException::class.java)
				firebaseAuthWithGoogle(account.idToken!!)
			}
			catch (_: ApiException) {}
		}
	}

	private fun firebaseAuthWithGoogle(idToken: String) {
		val credential = GoogleAuthProvider.getCredential(idToken, null)

		auth.signInWithCredential(credential).addOnCompleteListener(requireActivity()) { task ->
			if (task.isSuccessful) {
				val intent = Intent(context, MainActivity::class.java)
				startActivity(intent)

				requireActivity().finish()
			}
		}
	}
}
