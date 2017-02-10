package com.kilogramm.mattermost.view.authorization;

import android.app.Service;
import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

/**
 * Created by Evgeny on 09.02.2017.
 */

public class TeamListCursorAdapter extends RecyclerView.Adapter<TeamViewHolder> {

    private LayoutInflater mInflater;
    private Context mContext;
    private Cursor mCursor;

    public TeamListCursorAdapter(Context context, Cursor cursor) {
        this.mContext = context;
        this.mInflater = (LayoutInflater) this.mContext.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        this.mCursor = cursor;
    }

    @Override
    public TeamViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return TeamViewHolder.create(mInflater,parent);
    }

    @Override
    public void onBindViewHolder(TeamViewHolder holder, int position) {
        holder.bindTo(mCursor);
    }

    @Override
    public int getItemCount() {
        if(mCursor!=null){
            return mCursor.getCount();
        } else {
            return 0;
        }
    }

    public void swapCursor(Cursor newCursor){
        if(this.mCursor!=null && !this.mCursor.isClosed()){
            this.mCursor.close();
        }
        this.mCursor = newCursor;
        notifyDataSetChanged();
    }
}
