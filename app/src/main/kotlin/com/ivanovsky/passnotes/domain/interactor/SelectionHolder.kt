package com.ivanovsky.passnotes.domain.interactor

import com.ivanovsky.passnotes.domain.entity.SelectionItem
import java.util.concurrent.atomic.AtomicReference

class SelectionHolder {

    private val selection = AtomicReference<SelectionItem>()
    private val action = AtomicReference<ActionType>()

    fun select(action: ActionType, selection: SelectionItem) {
        this.selection.set(selection)
        this.action.set(action)
    }

    fun hasSelection(): Boolean {
        return action.get() != null
    }

    fun getAction(): ActionType? {
        return action.get()
    }

    fun getSelection(): SelectionItem? {
        return selection.get()
    }

    fun clear() {
        selection.set(null)
        action.set(null)
    }

    enum class ActionType {
        CUT
    }
}