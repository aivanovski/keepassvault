package com.ivanovsky.passnotes.ui.notes

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.NotesFragmentBinding
import com.ivanovsky.passnotes.ui.core.BaseFragment

class NotesFragment: BaseFragment(), NotesContract.View {

	private lateinit var binding: NotesFragmentBinding
	private lateinit var presenter: NotesContract.Presenter

	override fun onCreateContentView(inflater: LayoutInflater?, container: ViewGroup?,
	                                 savedInstanceState: Bundle?): View {
		binding = DataBindingUtil.inflate(inflater, R.layout.notes_fragment, container, false)

		val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
		val dividerDecorator = DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL)

		binding.recyclerView.layoutManager = linearLayoutManager
		binding.recyclerView.addItemDecoration(dividerDecorator)

		return binding.root
	}

	override fun setPresenter(presenter: NotesContract.Presenter?) {
		this.presenter = presenter!!
	}

}