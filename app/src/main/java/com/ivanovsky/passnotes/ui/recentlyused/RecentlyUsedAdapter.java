package com.ivanovsky.passnotes.ui.recentlyused;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class RecentlyUsedAdapter extends RecyclerView.Adapter<RecentlyUsedAdapter.ViewHolder> {

	private static final int VIEW_TYPE_HEADER = 0;
	private static final int VIEW_TYPE_LIST_ITEM = 1;

	private final LayoutInflater inflater;

	public RecentlyUsedAdapter(Context context) {
		this.inflater = LayoutInflater.from(context);
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		return null;
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {

	}

	@Override
	public int getItemCount() {
		return 0;
	}

	abstract static class ViewHolder extends RecyclerView.ViewHolder {

		ViewHolder(View itemView) {
			super(itemView);
		}
	}

	static class HeaderViewHolder extends ViewHolder {

		HeaderViewHolder(View itemView) {
			super(itemView);
		}
	}

	static class ListItemViewHolder extends ViewHolder {

		ListItemViewHolder(View itemView) {
			super(itemView);
		}
	}
}
