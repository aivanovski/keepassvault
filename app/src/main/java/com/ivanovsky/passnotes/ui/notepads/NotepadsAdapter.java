package com.ivanovsky.passnotes.ui.notepads;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.databinding.ListItemNotepadBinding;

import java.util.ArrayList;
import java.util.List;

public class NotepadsAdapter extends RecyclerView.Adapter<NotepadsAdapter.ViewHolder> {

	private final LayoutInflater inflater;
	private final List<ListItem> items;

	public NotepadsAdapter(Context context) {
		this.inflater = LayoutInflater.from(context);
		this.items = new ArrayList<>();
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
		ListItemNotepadBinding binding = DataBindingUtil.inflate(inflater,
				R.layout.list_item_notepad, parent, false);

		return new ViewHolder(binding);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		String title = items.get(position).title;
		holder.binding.title.setText(title);
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	static class ViewHolder extends RecyclerView.ViewHolder {

		final ListItemNotepadBinding binding;

		ViewHolder(ListItemNotepadBinding binding) {
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
