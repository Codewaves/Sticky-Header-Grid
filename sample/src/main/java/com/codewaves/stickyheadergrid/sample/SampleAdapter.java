package com.codewaves.stickyheadergrid.sample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.codewaves.sample.R;
import com.codewaves.stickyheadergrid.StickyHeaderGridAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sergej Kravcenko on 4/24/2017.
 * Copyright (c) 2017 Sergej Kravcenko
 */

public class SampleAdapter extends StickyHeaderGridAdapter {
   private List<String> labels;

   SampleAdapter(int count) {
      labels = new ArrayList<>(count);
      for (int i = 0; i < count; ++i) {
         labels.add(String.valueOf(i));
      }
   }

   @Override
   public int getSectionCount() {
      return 20;
   }

   @Override
   public int getSectionItemCount(int section) {
      return labels.size();
   }

   @Override
   public HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent, int headerType) {
      final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_header, parent, false);
      return new MyHeaderViewHolder(view);
   }

   @Override
   public ItemViewHolder onCreateItemViewHolder(ViewGroup parent, int itemType) {
      final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_item, parent, false);
      return new MyItemViewHolder(view);
   }

   @Override
   public void onBindHeaderViewHolder(HeaderViewHolder viewHolder, int section, int headerType) {
      final MyHeaderViewHolder holder = (MyHeaderViewHolder)viewHolder;
      final String label = "Header " + section;
      holder.labelView.setText(label);
   }

   @Override
   public void onBindItemViewHolder(ItemViewHolder viewHolder, int section, int position, int itemType) {
      final MyItemViewHolder holder = (MyItemViewHolder)viewHolder;
      final String label = labels.get(position);
      holder.labelView.setText(label);
      holder.labelView.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            Toast.makeText(holder.labelView.getContext(), label, Toast.LENGTH_SHORT).show();
         }
      });
   }

   public static class MyHeaderViewHolder extends HeaderViewHolder {
      TextView labelView;

      MyHeaderViewHolder(View itemView) {
         super(itemView);
         labelView = (TextView) itemView.findViewById(R.id.label);
      }
   }

   public static class MyItemViewHolder extends ItemViewHolder {
      TextView labelView;

      MyItemViewHolder(View itemView) {
         super(itemView);
         labelView = (TextView) itemView.findViewById(R.id.label);
      }
   }
}
