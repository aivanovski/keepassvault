package com.ivanovsky.passnotes.presentation.enterDbCredentials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.EnterDbCredentialsFragmentBinding
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments

class EnterDbCredentialsFragment : BaseFragment() {

    private lateinit var binding: EnterDbCredentialsFragmentBinding

    private val viewModel: EnterDbCredentialsViewModel by lazy {
        ViewModelProvider(
            this,
            EnterDbCredentialsViewModel.Factory(
                args = getMandatoryArgument(ARGUMENTS)
            )
        )[EnterDbCredentialsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = EnterDbCredentialsFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupActionBar {
            title = getString(R.string.type_a_password)
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    companion object {

        private const val ARGUMENTS = "arguments"

        fun newInstance(args: EnterDbCredentialsScreenArgs) =
            EnterDbCredentialsFragment()
                .withArguments {
                    putParcelable(ARGUMENTS, args)
                }
    }
}