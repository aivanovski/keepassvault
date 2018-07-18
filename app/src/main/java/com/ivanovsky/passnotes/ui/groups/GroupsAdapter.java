package com.ivanovsky.passnotes.ui.groups;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.safedb.model.Group;
import com.ivanovsky.passnotes.databinding.ListItemGroupBinding;

import java.util.ArrayList;
import java.util.List;

public class GroupsAdapter extends RecyclerView.Adapter<GroupsAdapter.ViewHolder> {

	private final LayoutInflater inflater;
	private final List<ListItem> items;
	private OnItemClickListener listener;

	public interface OnItemClickListener {
		void onItemClicked(int position);
	}

	public GroupsAdapter(Context context) {
		this.inflater = LayoutInflater.from(context);
		this.items = new ArrayList<>();
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		this.listener = listener;
	}

	public void setItems(List<ListItem> newItems) {
		items.clear();

		if (newItems != null) {
			items.addAll(newItems);
		}

		notifyDataSetChanged();
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		ListItemGroupBinding binding = DataBindingUtil.inflate(inflater,
				R.layout.list_item_group, parent, false);

		return new ViewHolder(binding);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		String title = items.get(position).title;

		holder.binding.title.setText(title);
		holder.binding.rootLayout.setOnClickListener(view -> onItemClicked(position));
	}

	private void onItemClicked(int position) {
		if (listener != null) {
			listener.onItemClicked(position);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	static class ViewHolder extends RecyclerView.ViewHolder {

		final ListItemGroupBinding binding;

		ViewHolder(ListItemGroupBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}

	static class ListItem {

		final String title;

		ListItem(String title) {
			this.title = title;
		}
	}
}
