package com.ivanovsky.passnotes.presentation.server_login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.databinding.ServerLoginFragmentBinding
import com.ivanovsky.passnotes.presentation.core.FragmentWithDoneButton
import com.ivanovsky.passnotes.presentation.core.extensions.getMandarotyArgument
import com.ivanovsky.passnotes.presentation.core.extensions.hideKeyboard
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ServerLoginFragment : FragmentWithDoneButton() {

    private val args by lazy { getMandarotyArgument<ServerLoginArgs>(ARGUMENTS) }

    private val viewModel: ServerLoginViewModel by viewModel {
        parametersOf(args)
    }

    private lateinit var binding: ServerLoginFragmentBinding

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = getString(R.string.add_new_server)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ServerLoginFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.doneButtonVisibility.observe(viewLifecycleOwner) { isVisible ->
            setDoneButtonVisibility(isVisible)
        }
        viewModel.hideKeyboardEvent.observe(viewLifecycleOwner) {
            hideKeyboard()
        }
        viewModel.finishScreenEvent.observe(viewLifecycleOwner) {
            setResultAndFinish(it)
        }
    }

    override fun onDoneMenuClicked() {
        viewModel.authenticate()
    }

    private fun setResultAndFinish(fsAuthority: FSAuthority) {
        val data = Intent().apply {
            putExtra(ServerLoginActivity.EXTRA_RESULT, fsAuthority)
        }

        requireActivity().apply {
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    companion object {

        private const val ARGUMENTS = "arguments"

        fun newInstance(args: ServerLoginArgs) = ServerLoginFragment().withArguments {
            putParcelable(ARGUMENTS, args)
        }
    }
}