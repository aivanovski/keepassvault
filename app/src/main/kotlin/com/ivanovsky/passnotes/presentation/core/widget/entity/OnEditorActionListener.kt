package com.ivanovsky.passnotes.presentation.core.widget.entity

interface OnEditorActionListener {

    /**
     * Called when action is performed on [android.widget.EditText]
     * @param actionId Identifier of the action, for example
     * [android.view.inputmethod.EditorInfo.IME_ACTION_DONE]
     */
    fun onEditorAction(actionId: Int)
}