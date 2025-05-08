package org.kaorun.diary.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis

abstract class BaseFragment : Fragment() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
		returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
		exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
		reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
	}
}
