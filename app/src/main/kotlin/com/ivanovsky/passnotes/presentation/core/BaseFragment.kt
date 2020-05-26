package com.ivanovsky.passnotes.presentation.core

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.widget.ErrorPanelView
import com.ivanovsky.passnotes.presentation.core.widget.FragmentStateView
import com.ivanovsky.passnotes.util.InputMethodUtils.hideSoftInput

abstract class BaseFragment : Fragment(), GenericScreen {

    private lateinit var contentContainer: ViewGroup
    private lateinit var stateView: FragmentStateView
    private lateinit var errorPanelView: ErrorPanelView
    private lateinit var rootLayout: ViewGroup

    private val screenStateData = MutableLiveData<ScreenState>()
    private val showToastMessageEvent = SingleLiveEvent<String>()
    private val showSnackbarMessageEvent = SingleLiveEvent<String>()
    private val hideKeyboardEvent = SingleLiveEvent<String>()
    private val finishScreenEvent = SingleLiveEvent<Unit>()

    override var screenState: ScreenState
        get() = screenStateData.value ?: ScreenState.notInitialized()
        set(value) {
            screenStateData.postValue(value)
        }

    protected abstract fun onCreateContentView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.core_base_fragment, container, false)

        contentContainer = view.findViewById(R.id.content_container)
        stateView = view.findViewById(R.id.state_view)
        errorPanelView = view.findViewById(R.id.error_panel_view)
        rootLayout = view.findViewById(R.id.root_layout)

        val contentView = onCreateContentView(inflater, contentContainer, savedInstanceState)
        contentContainer.addView(contentView)

        if (getContentContainerId() != -1) {
            contentContainer = contentView.findViewById(getContentContainerId())
        }

        screenStateData.observe(viewLifecycleOwner,
            Observer { screenState -> setScreenStateInternal(screenState) })
        showSnackbarMessageEvent.observe(viewLifecycleOwner,
            Observer { message -> showSnackbarMessageInternal(message) })
        showToastMessageEvent.observe(viewLifecycleOwner,
            Observer { message -> showToastMessageInternal(message) })
        hideKeyboardEvent.observe(viewLifecycleOwner,
            Observer { hideKeyboardInternal() })
        finishScreenEvent.observe(viewLifecycleOwner,
            Observer { finishScreenInternal() })

        return view
    }

    //determines view that will be shown/hidden if fragment state will be changed, should be overridden in derived class
    protected open fun getContentContainerId(): Int {
        return -1
    }

    private fun setScreenStateInternal(screenState: ScreenState) {
        when (screenState.displayingMode) {
            ScreenDisplayingMode.EMPTY -> stateView.setEmptyText(screenState.message)
            ScreenDisplayingMode.ERROR -> stateView.setErrorText(screenState.message)
            ScreenDisplayingMode.DISPLAYING_DATA_WITH_ERROR_PANEL -> errorPanelView!!.setText(screenState.message)
        }
        applyScreenStateToViews(screenState)
        onScreenStateChanged(screenState)
    }

    private fun applyScreenStateToViews(screenState: ScreenState) {
        when (screenState.displayingMode) {
            ScreenDisplayingMode.NOT_INITIALIZED -> {
            }
            ScreenDisplayingMode.LOADING -> {
                contentContainer.visibility = View.GONE
                stateView.setState(FragmentStateView.State.LOADING)
                stateView.visibility = View.VISIBLE
                errorPanelView.visibility = View.GONE
            }
            ScreenDisplayingMode.EMPTY -> {
                contentContainer.visibility = View.GONE
                stateView.setState(FragmentStateView.State.EMPTY)
                stateView.visibility = View.VISIBLE
                errorPanelView.visibility = View.GONE
            }
            ScreenDisplayingMode.DISPLAYING_DATA -> {
                contentContainer.visibility = View.VISIBLE
                stateView.visibility = View.GONE
                errorPanelView.visibility = View.GONE
            }
            ScreenDisplayingMode.ERROR -> {
                contentContainer.visibility = View.GONE
                stateView.setState(FragmentStateView.State.ERROR)
                stateView.visibility = View.VISIBLE
                errorPanelView.visibility = View.GONE
            }
            ScreenDisplayingMode.DISPLAYING_DATA_WITH_ERROR_PANEL -> {
                contentContainer.visibility = View.VISIBLE
                stateView.visibility = View.GONE
                errorPanelView.visibility = View.VISIBLE
                errorPanelView.setState(ErrorPanelView.State.MESSAGE)
            }
            ScreenDisplayingMode.DISPLAYING_DATA_WITH_RETRY_BUTTON -> {
                contentContainer.visibility = View.VISIBLE
                stateView.visibility = View.GONE
                errorPanelView.visibility = View.VISIBLE
                errorPanelView.setState(ErrorPanelView.State.MESSAGE_WITH_RETRY)
            }
        }
    }

    protected open fun onScreenStateChanged(screenState: ScreenState) {
        //empty, should be overridden in derived class
    }

    override fun showSnackbarMessage(message: String) {
        showSnackbarMessageEvent.call(message)
    }

    private fun showSnackbarMessageInternal(message: String) {
        Snackbar.make(rootLayout, message, Snackbar.LENGTH_SHORT)
                .show()
    }

    fun showSnackbar(message: SnackbarMessage) {
        val snackbar: Snackbar
        if (message.isDisplayOkButton) {
            snackbar = Snackbar.make(rootLayout, message.message, Snackbar.LENGTH_INDEFINITE)
            snackbar.setAction(R.string.ok) { view: View? -> snackbar.dismiss() }
        } else {
            snackbar = Snackbar.make(rootLayout, message.message, Snackbar.LENGTH_SHORT)
        }
        snackbar.show()
    }

    override fun showToastMessage(message: String) {
        showToastMessageEvent.call(message)
    }

    private fun showToastMessageInternal(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT)
                .show()
    }

    override fun hideKeyboard() {
        hideKeyboardEvent.call()
    }

    private fun hideKeyboardInternal() {
        val activity = this.activity ?: return

        hideSoftInput(activity)
    }

    override fun finishScreen() {
        finishScreenEvent.call()
    }

    private fun finishScreenInternal() {
        activity?.finish()
    }
}