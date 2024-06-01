package com.ivanovsky.passnotes.presentation.serverLogin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.CoreComposeFragmentBinding
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.getComposeTheme
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.hideKeyboard
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.updateMenuItemVisibility
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.serverLogin.ServerLoginViewModel.ServerLoginMenuItem
import com.ivanovsky.passnotes.presentation.serverLogin.model.LoginType
import com.ivanovsky.passnotes.presentation.serverLogin.model.ServerLoginIntent.NavigateBack
import com.ivanovsky.passnotes.presentation.serverLogin.model.ServerLoginIntent.OnDoneButtonClicked
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ServerLoginFragment : Fragment() {

    private val args by lazy { getMandatoryArgument<ServerLoginArgs>(ARGUMENTS) }

    private val viewModel: ServerLoginViewModel by viewModel {
        parametersOf(args)
    }

    private var menu: Menu? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        setupActionBar {
            title = when (args.loginType) {
                LoginType.USERNAME_PASSWORD -> getString(R.string.login_to_server)
                LoginType.GIT -> getString(R.string.login_to_git)
            }
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.menu = menu

        inflater.inflate(R.menu.base_done, menu)

        viewModel.visibleMenuItems.value?.let { visibleItems ->
            updateMenuItemVisibility(
                menu = menu,
                visibleItems = visibleItems,
                allScreenItems = ServerLoginMenuItem.entries
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = CoreComposeFragmentBinding.inflate(inflater, container, false)

        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val theme by viewModel.theme.collectAsState(initial = context.getComposeTheme())

                AppTheme(theme = theme) {
                    ServerLoginScreen(viewModel = viewModel)
                }
            }
        }

        return binding.root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val action = MENU_ACTIONS[item.itemId] ?: throw IllegalArgumentException()
        action.invoke(viewModel)
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToLiveData()
        subscribeToLiveEvents()

        viewModel.start()
    }

    private fun subscribeToLiveData() {
        viewModel.visibleMenuItems.observe(viewLifecycleOwner) { visibleItems ->
            setVisibleMenuItems(visibleItems)
        }
    }

    private fun subscribeToLiveEvents() {
        viewModel.hideKeyboardEvent.observe(viewLifecycleOwner) {
            hideKeyboard()
        }
    }

    private fun setVisibleMenuItems(
        visibleItems: List<ServerLoginMenuItem>
    ) {
        val menu = menu ?: return

        updateMenuItemVisibility(
            menu = menu,
            visibleItems = visibleItems,
            allScreenItems = ServerLoginMenuItem.entries
        )
    }

    companion object {

        private const val ARGUMENTS = "arguments"

        private val MENU_ACTIONS = mapOf<Int, (vm: ServerLoginViewModel) -> Unit>(
            android.R.id.home to { vm -> vm.sendIntent(NavigateBack) },
            R.id.menu_done to { vm -> vm.sendIntent(OnDoneButtonClicked) }
        )

        fun newInstance(args: ServerLoginArgs) = ServerLoginFragment().withArguments {
            putParcelable(ARGUMENTS, args)
        }
    }
}