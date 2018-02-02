package com.ivanovsky.passnotes.ui.core.adapter;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.databinding.ListItemTwoLineTwoTextBinding;

import java.util.ArrayList;
import java.util.List;

public class TwoLineTwoTextAdapter extends RecyclerView.Adapter<TwoLineTwoTextAdapter.ViewHolder> {

	private OnListItemClickListener clickListener;
	private final LayoutInflater inflater;
	private final List<ListItem> items;

	public interface OnListItemClickListener {
		void onListItemClicked(int position);
	}

	public TwoLineTwoTextAdapter(Context context) {
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

	public void setOnListItemClickListener(OnListItemClickListener clickListener) {
		this.clickListener = clickListener;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		ListItemTwoLineTwoTextBinding binding = DataBindingUtil.inflate(inflater, R.layout.list_item_two_line_two_text, parent, false);
		return new ViewHolder(binding);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		ListItem item = items.get(position);

		holder.title.setText(item.title);
		holder.description.setText(item.description);

		holder.layout.setOnClickListener(view -> onListItemClicked(position));
	}

	private void onListItemClicked(int position) {
		if (clickListener != null) {
			clickListener.onListItemClicked(position);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	static class ViewHolder extends RecyclerView.ViewHolder {

		View layout;
		TextView title;
		TextView description;


		ViewHolder(ListItemTwoLineTwoTextBinding binding) {
			super(binding.getRoot());
			layout = binding.layout;
			title = binding.primaryText;
			description = binding.secondaryText;
		}
	}

	public static class ListItem {

		final String title;
		final String description;

		public ListItem(String title, String description) {
			this.title = title;
			this.description = description;
		}
	}
}
