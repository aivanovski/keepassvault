package com.ivanovsky.passnotes.presentation.core.adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ivanovsky.passnotes.R;

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

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = inflater.inflate(R.layout.list_item_two_line_two_text, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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

		private View layout;
		private TextView title;
		private TextView description;

		ViewHolder(View view) {
			super(view);
			layout = view;
			title = view.findViewById(R.id.primaryText);
			description = view.findViewById(R.id.secondaryText);
		}
	}

	public static class ListItem {

		private final String title;
		private final String description;

		public ListItem(String title, String description) {
			this.title = title;
			this.description = description;
		}
	}
}
