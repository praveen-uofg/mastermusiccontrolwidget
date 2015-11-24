package com.example.praveen.mastermusiccontrolwidget;

/**
 * Created by praveen on 10/18/2015.
 */

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.praveen.mastermusiccontrolwidget.Utils.ImageUtils;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter <RecyclerViewAdapter.ViewHolder> {
    List <String> list;
    Context mContext;
    public RecyclerViewAdapter(Context context,List<String> list) {
        mContext = context;
        this.list = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v= LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.default_list_item,viewGroup,false);

        ViewHolder holder = new ViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        viewHolder.imageView.setImageBitmap(ImageUtils.getPackageIcon(mContext,list.get(i)));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public  static class ViewHolder extends android.support.v7.widget.RecyclerView.ViewHolder{

        ImageView imageView;
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView)itemView.findViewById(R.id.defaultImageIcon);
        }
    }
}
