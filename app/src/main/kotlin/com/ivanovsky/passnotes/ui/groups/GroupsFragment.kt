package com.ivanovsky.passnotes.ui.groups

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.safedb.model.Group
import com.ivanovsky.passnotes.databinding.GroupsFragmentBinding
import com.ivanovsky.passnotes.ui.core.BaseFragment
import com.ivanovsky.passnotes.ui.core.FragmentState
import com.ivanovsky.passnotes.ui.newgroup.NewGroupActivity
import com.ivanovsky.passnotes.ui.notes.NotesActivity
import com.ivanovsky.passnotes.ui.unlock.UnlockActivity

class GroupsFragment : BaseFragment(), GroupsContract.View {

	private lateinit var presenter: GroupsContract.Presenter
	private lateinit var binding: GroupsFragmentBinding
	private lateinit var adapter: GroupsAdapter

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

	override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
		binding = DataBindingUtil.inflate(inflater, R.layout.groups_fragment, container, false)

		val layoutManager = GridLayoutManager(context, 3)

		adapter = GroupsAdapter(context)

		binding.recyclerView.layoutManager = layoutManager
		binding.recyclerView.adapter = adapter

		binding.fab.setOnClickListener { view -> showNewGroupScreen() }

		return binding.root
	}

	override fun getContentContainerId(): Int {
		return R.id.recycler_view
	}

	override fun onStateChanged(oldState: FragmentState?, newState: FragmentState) {
		when (newState) {
			FragmentState.EMPTY, FragmentState.DISPLAYING_DATA_WITH_ERROR_PANEL, FragmentState.DISPLAYING_DATA -> binding.fab.visibility = View.VISIBLE

			FragmentState.LOADING, FragmentState.ERROR -> binding.fab.visibility = View.GONE
		}
	}

	override fun setPresenter(presenter: GroupsContract.Presenter) {
		this.presenter = presenter
	}

	override fun showGroups(groups: List<Group>) {
		adapter.setItems(createAdapterItems(groups))
		adapter.onGroupItemClickListener = { position -> onGroupClicked(groups[position])}
		adapter.onButtonItemClickListener = { onNewGroupClicked() }

		state = FragmentState.DISPLAYING_DATA
	}

	private fun createAdapterItems(groups: List<Group>): List<GroupsAdapter.ListItem> {
		val result = mutableListOf<GroupsAdapter.ListItem>()

		groups.forEach { group -> result.add(GroupsAdapter.GroupListItem(group.title, group.noteCount)) }

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
		startActivity(NewGroupActivity.createStartIntent(context))
	}

	override fun showUnlockScreenAndFinish() {
		startActivity(UnlockActivity.createStartIntent(context))

		activity.finish()
	}

	override fun showNotesScreen(group: Group) {
		startActivity(NotesActivity.createStartIntent(context, group))
	}
}