package com.ivanovsky.passnotes.presentation.unlock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ivanovsky.passnotes.R;

import java.util.ArrayList;
import java.util.List;

class FileSpinnerAdapter extends BaseAdapter {

	private final LayoutInflater inflater;
	private final List<Item> items;

	FileSpinnerAdapter(Context context) {
		this.inflater = LayoutInflater.from(context);
		this.items = new ArrayList<>();
	}

	void setItem(List<Item> newItems) {
		items.clear();

		if (newItems != null) {
			items.addAll(newItems);
		}
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Item getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		View view = convertView;

		if (convertView == null) {
			view = inflater.inflate(R.layout.unlock_spinner_item, parent, false);

			holder = new ViewHolder();
			holder.filenameTextView = view.findViewById(R.id.filename);
			holder.pathTextView = view.findViewById(R.id.path);

			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}

		Item item = items.get(position);

		holder.filenameTextView.setText(item.filename);
		holder.pathTextView.setText(item.path);

		return view;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		View view = convertView;

		if (convertView == null) {
			view = inflater.inflate(R.layout.unlock_spinner_dropdown_item, parent, false);

			holder = new ViewHolder();
			holder.filenameTextView = view.findViewById(R.id.filename);
			holder.pathTextView = view.findViewById(R.id.path);

			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}

		Item item = items.get(position);

		holder.filenameTextView.setText(item.filename);
		holder.pathTextView.setText(item.path);

		return view;
	}

	private static class ViewHolder {
		private TextView filenameTextView;
		private TextView pathTextView;
	}

	static class Item {

		private final String filename;
		private final String path;

		Item(String filename, String path) {
			this.filename = filename;
			this.path = path;
		}
	}
}
