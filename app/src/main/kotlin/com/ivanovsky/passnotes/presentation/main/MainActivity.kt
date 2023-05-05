package com.ivanovsky.passnotes.presentation.main

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.terrakok.cicerone.NavigatorHolder
import com.github.terrakok.cicerone.androidx.AppNavigator
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.NoteCandidate
import com.ivanovsky.passnotes.databinding.CoreBaseActivityWithSideMenuBinding
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryExtra
import com.ivanovsky.passnotes.presentation.core.extensions.initActionBar
import com.ivanovsky.passnotes.presentation.main.navigation.NavigationMenuViewModel
import com.ivanovsky.passnotes.presentation.settings.SettingsRouter
import com.ivanovsky.passnotes.util.InputMethodUtils
import com.ivanovsky.passnotes.util.IntentUtils.immutablePendingIntentFlags

class MainActivity :
    AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private lateinit var binding: CoreBaseActivityWithSideMenuBinding

    private val navigatorHolder: NavigatorHolder by inject()
    private val settingsRouter: SettingsRouter by inject()
    private val navigator = AppNavigator(this, R.id.fragmentContainer)
    private val args by lazy {
        getMandatoryExtra<Bundle>(ARGUMENTS_BUNDLE).getParcelable(ARGUMENTS) as? MainScreenArgs
            ?: throw IllegalStateException()
    }

    private val navigationViewModel: NavigationMenuViewModel by lazy {
        ViewModelProvider(this, NavigationMenuViewModel.Factory())
            .get(NavigationMenuViewModel::class.java)
    }

    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this, MainViewModel.Factory(args))
            .get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = CoreBaseActivityWithSideMenuBinding.inflate(layoutInflater)
            .also {
                it.lifecycleOwner = this
                it.navigationViewModel = navigationViewModel
            }

        setContentView(binding.root)
        initActionBar(R.id.toolBar)

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerOpened(drawerView: View) {
            }

            override fun onDrawerClosed(drawerView: View) {
            }

            override fun onDrawerStateChanged(newState: Int) {
                if (newState == DrawerLayout.STATE_DRAGGING ||
                    newState == DrawerLayout.STATE_SETTLING
                ) {
                    navigationViewModel.onMenuDragging()
                }
            }
        })

        if (savedInstanceState == null) {
            viewModel.navigateToRootScreen()
        }

        subscribeToLiveData()
        subscribeToEvents()
    }

    override fun onBackPressed() {
        val handledByFragment = supportFragmentManager.fragments.any { fragment ->
            fragment is BaseFragment && fragment.onBackPressed()
        }
        if (!handledByFragment) {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        navigatorHolder.removeNavigator()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        navigatorHolder.setNavigator(navigator)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val handledByFragment = supportFragmentManager.fragments.any {
            it.onOptionsItemSelected(item)
        }
        if (handledByFragment) {
            return true
        }

        return when (item.itemId) {
            android.R.id.home -> {
                binding.drawerLayout.openDrawer(GravityCompat.START)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun subscribeToLiveData() {
        navigationViewModel.isNavigationMenuEnabled.observe(this) {
            setDrawerEnabled(it)
        }
    }

    private fun subscribeToEvents() {
        navigationViewModel.hideKeyboardEvent.observe(this) {
            InputMethodUtils.hideSoftInput(this)
        }
        navigationViewModel.hideMenuEvent.observe(this) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun setDrawerEnabled(isEnabled: Boolean) {
        if (isEnabled) {
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } else {
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat?,
        pref: Preference?
    ): Boolean {
        val settingsFragmentName = pref?.fragment ?: throw IllegalStateException()
        settingsRouter.navigateTo(settingsFragmentName)
        return true
    }

    companion object {

        private const val ARGUMENTS = "arguments"
        private const val ARGUMENTS_BUNDLE = "arguments_bundle"

        private const val AUTOFILL_AUTHENTICATION_REQUEST_CODE = 1001
        private const val AUTOFILL_SELECTION_REQUEST_CODE = 1002
        private const val AUTOFILL_SAVE_RESULT_REQUEST_CODE = 1003

        fun createStartIntent(
            context: Context,
            args: MainScreenArgs
        ): Intent {
            return Intent(context, MainActivity::class.java)
                .apply {
                    // Wrap arguments with Bundle in order to make it work
                    // when application is launched from autofill
                    val bundle = Bundle()
                        .apply {
                            putParcelable(
                                ARGUMENTS,
                                args
                            )
                        }

                    putExtra(ARGUMENTS_BUNDLE, bundle)
                }
        }

        fun createAutofillAuthenticationPendingIntent(
            context: Context,
            autofillStructure: AutofillStructure
        ): PendingIntent {
            val intent = createStartIntent(
                context,
                MainScreenArgs(
                    appMode = ApplicationLaunchMode.AUTOFILL_AUTHORIZATION,
                    autofillStructure = autofillStructure
                )
            )
            return PendingIntent.getActivity(
                context,
                AUTOFILL_AUTHENTICATION_REQUEST_CODE,
                intent,
                immutablePendingIntentFlags(PendingIntent.FLAG_CANCEL_CURRENT)
            )
        }

        fun createAutofillSelectionPendingIntent(
            context: Context,
            autofillStructure: AutofillStructure
        ): PendingIntent {
            val intent = createStartIntent(
                context,
                MainScreenArgs(
                    appMode = ApplicationLaunchMode.AUTOFILL_SELECTION,
                    autofillStructure = autofillStructure
                )
            )

            return PendingIntent.getActivity(
                context,
                AUTOFILL_SELECTION_REQUEST_CODE,
                intent,
                immutablePendingIntentFlags(PendingIntent.FLAG_CANCEL_CURRENT)
            )
        }

        fun createAutofillSaveResultIntent(
            context: Context,
            note: NoteCandidate
        ): Intent {
            return createStartIntent(
                context,
                MainScreenArgs(
                    appMode = ApplicationLaunchMode.NORMAL,
                    note = note
                )
            )
        }

        fun createAutofillSaveResultPendingIntent(
            context: Context,
            note: NoteCandidate
        ): PendingIntent {
            val intent = createStartIntent(
                context,
                MainScreenArgs(
                    appMode = ApplicationLaunchMode.NORMAL,
                    note = note
                )
            )

            return PendingIntent.getActivity(
                context,
                AUTOFILL_SAVE_RESULT_REQUEST_CODE,
                intent,
                immutablePendingIntentFlags(PendingIntent.FLAG_CANCEL_CURRENT)
            )
        }
    }
}