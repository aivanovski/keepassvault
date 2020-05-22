package com.ivanovsky.passnotes.presentation.core

interface BaseView<T : BasePresenter> : GenericScreen {

    var presenter: T
}