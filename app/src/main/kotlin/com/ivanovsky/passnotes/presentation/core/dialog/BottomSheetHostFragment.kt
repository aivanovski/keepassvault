package com.ivanovsky.passnotes.presentation.core.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.FragmentBottomSheetDialogBinding
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.storagelist.StorageListArgs
import com.ivanovsky.passnotes.presentation.storagelist.StorageListFragment

class BottomSheetHostFragment : BottomSheetDialogFragment() {

    private val args: StorageListArgs by lazy {
        getMandatoryArgument(ARGUMENTS)
    }

    override fun getTheme(): Int = R.style.BottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentBottomSheetDialogBinding
            .inflate(inflater, container, false)

        if (childFragmentManager.findFragmentByTag(CONTENT_TAG) == null) {
            childFragmentManager
                .beginTransaction()
                .replace(
                    R.id.innerFragmentContainer,
                    StorageListFragment.newInstance(args),
                    CONTENT_TAG
                )
                .commitNow()
        }

        binding.innerFragmentContainer.invalidate()

        return binding.root
    }

    companion object {

        val TAG: String = BottomSheetHostFragment::class.java.simpleName

        private const val ARGUMENTS = "arguments"
        private const val CONTENT_TAG = "content"

        fun newInstance(args: StorageListArgs): BottomSheetHostFragment {
            return BottomSheetHostFragment()
                .withArguments {
                    putParcelable(ARGUMENTS, args)
                }
        }
    }
}
