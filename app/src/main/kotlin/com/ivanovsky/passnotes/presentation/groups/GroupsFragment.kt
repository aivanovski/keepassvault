package com.ivanovsky.passnotes.presentation.groups

import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.presentation.Screen
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.FragmentState
import com.ivanovsky.passnotes.presentation.group.GroupActivity
import com.ivanovsky.passnotes.presentation.notes.NotesActivity
import com.ivanovsky.passnotes.presentation.unlock.UnlockActivity

class GroupsFragment : BaseFragment(), GroupsContract.View {

	private lateinit var presenter: GroupsContract.Presenter
	private lateinit var adapter: GroupsAdapter
	private lateinit var recyclerView: RecyclerView
	private lateinit var fab: FloatingActionButton

	companion object {

		fun newInstance(): GroupsFragment {
			return GroupsFragment()
		}
	}

	override fun onResume() {
		super.onResume()
		presenter.start()
	}

	override fun onPause() {
		super.onPause()
		presenter.stop()
	}

	override fun onDestroy() {
		super.onDestroy()
		presenter.destroy()
	}

	override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
		val view = inflater.inflate(R.layout.groups_fragment, container, false)

		recyclerView =  view.findViewById(R.id.recycler_view)
		fab = view.findViewById(R.id.fab)

		val layoutManager = GridLayoutManager(context, 3)

		adapter = GroupsAdapter(context!!)

		recyclerView.layoutManager = layoutManager
		recyclerView.adapter = adapter

		fab.setOnClickListener { showNewGroupScreen() }

		presenter.globalSnackbarMessageAction.observe(this, Screen.GROUPS,
				Observer { message -> showSnackbar(message) })

		return view
	}

	override fun getContentContainerId(): Int {
		return R.id.recycler_view
	}

	override fun onStateChanged(oldState: FragmentState?, newState: FragmentState) {
		when (newState) {
			FragmentState.EMPTY,
			FragmentState.DISPLAYING_DATA_WITH_ERROR_PANEL,
			FragmentState.DISPLAYING_DATA -> fab.show()

			FragmentState.LOADING, FragmentState.ERROR -> fab.hide()
		}
	}

	override fun setPresenter(presenter: GroupsContract.Presenter) {
		this.presenter = presenter
	}

	override fun showGroups(groupsAndCounts: List<Pair<Group, Int>>) {
		adapter.setItems(createAdapterItems(groupsAndCounts))
		adapter.onGroupItemClickListener = { position -> onGroupClicked(groupsAndCounts[position].first)}
		adapter.onButtonItemClickListener = { onNewGroupClicked() }

		state = FragmentState.DISPLAYING_DATA
	}

	private fun createAdapterItems(groupsAndCounts: List<Pair<Group, Int>>): List<GroupsAdapter.ListItem> {
		val result = mutableListOf<GroupsAdapter.ListItem>()

		for (groupAndCount in groupsAndCounts) {
			val group = groupAndCount.first
			val noteCount = groupAndCount.second

			result.add(GroupsAdapter.GroupListItem(group.title, noteCount))
		}

		result.add(GroupsAdapter.ButtonListItem())

		return result
	}

	private fun onGroupClicked(group: Group) {
		presenter.onGroupClicked(group)
	}

	private fun onNewGroupClicked() {
		showNewGroupScreen()
	}

	override fun showNoItems() {
		setEmptyText(getString(R.string.no_items_to_show))
		state = FragmentState.EMPTY
	}

	override fun showNewGroupScreen() {
		startActivity(GroupActivity.createStartIntent(context!!))
	}

	override fun showUnlockScreenAndFinish() {
		startActivity(UnlockActivity.createStartIntent(context!!))

		activity!!.finish()
	}

	override fun showNotesScreen(group: Group) {
		startActivity(NotesActivity.createStartIntent(context!!, group))
	}

	override fun showError(message: String) {
		setErrorText(message)
		state = FragmentState.ERROR
	}
}