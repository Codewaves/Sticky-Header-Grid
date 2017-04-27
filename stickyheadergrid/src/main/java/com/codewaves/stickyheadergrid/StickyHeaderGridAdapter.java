package com.codewaves.stickyheadergrid;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.security.InvalidParameterException;
import java.util.ArrayList;

/**
 * Created by Sergej Kravcenko on 4/24/2017.
 * Copyright (c) 2017 Sergej Kravcenko
 */

@SuppressWarnings({"unused", "WeakerAccess"})
public class StickyHeaderGridAdapter extends RecyclerView.Adapter<StickyHeaderGridAdapter.ViewHolder> {
   public static final String TAG = "StickyHeaderGridAdapter";

   public static final int TYPE_HEADER = 0;
   public static final int TYPE_ITEM = 1;

   private ArrayList<Section> mSections;
   private int[] mSectionIndices;
   private int mTotalItemNumber;

   @SuppressWarnings("WeakerAccess")
   public static class ViewHolder extends RecyclerView.ViewHolder {
      public ViewHolder(View itemView) {
         super(itemView);
      }

      public boolean isHeader() {
         return false;
      }
   }

   public static class ItemViewHolder extends ViewHolder {
      public ItemViewHolder(View itemView) {
         super(itemView);
      }
   }

   public static class HeaderViewHolder extends ViewHolder {
      public HeaderViewHolder(View itemView) {
         super(itemView);
      }

      @Override
      public boolean isHeader() {
         return true;
      }
   }

   private static class Section {
      int position;
      int itemNumber;
      int length;
   }

   private void calculateSections() {
      mSections = new ArrayList<>();

      int total = 0;
      for (int s = 0, ns = getSectionCount(); s < ns; s++) {
         final Section section = new Section();
         section.position = total;
         section.itemNumber = getSectionItemCount(s);
         section.length = section.itemNumber + 1;
         mSections.add(section);

         total += section.length;
      }
      mTotalItemNumber = total;

      total = 0;
      mSectionIndices = new int[mTotalItemNumber];
      for (int s = 0, ns = getSectionCount(); s < ns; s++) {
         final Section section = mSections.get(s);
         for (int i = 0; i < section.length; i++) {
            mSectionIndices[total + i] = s;
         }
         total += section.length;
      }
   }

   int getItemViewInternalType(int position) {
      final int section = getPositionSection(position);
      final Section sectionObject = mSections.get(section);
      final int sectionPosition = position - sectionObject.position;

      return getItemViewInternalType(section, sectionPosition);
   }

   private int getItemViewInternalType(int section, int position) {
      return position == 0 ? TYPE_HEADER : TYPE_ITEM;
   }

   private int internalViewType(int type) {
      return type & 0xFF;
   }

   private int externalViewType(int type) {
      return type >> 8;
   }

   @Override
   public int getItemCount() {
      if (mSections == null) {
         calculateSections();
      }
      return mTotalItemNumber;
   }

   @Override
   public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      //Log.d(TAG, "onCreateViewHolder type: " + viewType);

      final int internalType = internalViewType(viewType);
      final int externalType = externalViewType(viewType);

      switch (internalType) {
         case TYPE_HEADER:
            return onCreateHeaderViewHolder(parent, externalType);
         case TYPE_ITEM:
            return onCreateItemViewHolder(parent, externalType);
      }
      throw new InvalidParameterException("invalid viewType: " + viewType);
   }

   @Override
   public void onBindViewHolder(ViewHolder holder, int position) {
      //Log.d(TAG, "onBindViewHolder position: " + position);

      if (mSections == null) {
         calculateSections();
      }

      final int section = mSectionIndices[position];
      final int internalType = internalViewType(holder.getItemViewType());
      final int externalType = externalViewType(holder.getItemViewType());

      switch (internalType) {
         case TYPE_HEADER:
            onBindHeaderViewHolder((HeaderViewHolder)holder, section, externalType);
            break;
         case TYPE_ITEM:
            final int sectionPosition = getItemSectionPosition(section, position);
            onBindItemViewHolder((ItemViewHolder)holder, section, sectionPosition, externalType);
            break;
         default:
            throw new InvalidParameterException("invalid viewType: " + internalType);
      }
   }

   @Override
   public int getItemViewType(int position) {
      final int section = getPositionSection(position);
      final Section sectionObject = mSections.get(section);
      final int sectionPosition = position - sectionObject.position;
      final int internalType = getItemViewInternalType(section, sectionPosition);
      int externalType = 0;

      switch (internalType) {
         case TYPE_HEADER:
            externalType = getSectionHeaderType(section);
            break;
         case TYPE_ITEM:
            externalType = getSectionItemType(section, sectionPosition - 1);
            break;
      }

      return ((externalType & 0xFF) << 8) | (internalType & 0xFF);
   }

   // Helpers
   public int getItemSectionPosition(int section, int position) {
      if (mSections == null) {
         calculateSections();
      }

      if (section < 0) {
         throw new IndexOutOfBoundsException("section " + section + " < 0");
      }

      if (section >= mSections.size()) {
         throw new IndexOutOfBoundsException("section " + section + " >=" + mSections.size());
      }

      final Section sectionObject = mSections.get(section);
      final int localPosition = position - sectionObject.position;
      if (localPosition >= sectionObject.length) {
         throw new IndexOutOfBoundsException("localPosition: " + localPosition + " >=" + sectionObject.length);
      }

      return localPosition - 1;
   }

   public int getPositionSection(int position) {
      if (mSections == null) {
         calculateSections();
      }

      if (getItemCount() == 0) {
         return -1;
      }

      if (position < 0) {
         throw new IndexOutOfBoundsException("position " + position + " < 0");
      }

      if (position >= getItemCount()) {
         throw new IndexOutOfBoundsException("position " + position + " >=" + getItemCount());
      }

      return mSectionIndices[position];
   }

   private int getAdapterPosition(int section, int offset) {
      if (mSections == null) {
         calculateSections();
      }

      if (section < 0) {
         throw new IndexOutOfBoundsException("section " + section + " < 0");
      }

      if (section >= mSections.size()) {
         throw new IndexOutOfBoundsException("section " + section + " >=" + mSections.size());
      }

      final Section sectionObject = mSections.get(section);
      return sectionObject.position + offset;
   }

   public int getSectionHeaderPosition(int section) {
      return getAdapterPosition(section, 0);
   }

   public int getSectionItemPosition(int section, int offset) {
      return getAdapterPosition(section, offset + 1);
   }

   // Overrides
   public int getSectionCount() {
      return 0;
   }

   public int getSectionItemCount(int section) {
      return 0;
   }

   public int getSectionHeaderType(int section) {
      return 0;
   }

   public int getSectionItemType(int section, int position) {
      return 0;
   }

   public boolean isSectionHeaderSticky(int section) {
      return true;
   }

   public HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent, int headerType) {
      return null;
   }

   public ItemViewHolder onCreateItemViewHolder(ViewGroup parent, int itemType) {
      return null;
   }

   public void onBindHeaderViewHolder(HeaderViewHolder viewHolder, int section, int headerType) {
   }

   public void onBindItemViewHolder(ItemViewHolder viewHolder, int section, int position, int itemType) {
   }

   // Notify
   public void notifyAllSectionsDataSetChanged() {
      calculateSections();
      notifyDataSetChanged();
   }

   public void notifySectionDataSetChanged(int section) {
      calculateSections();
      if (mSections == null) {
         notifyAllSectionsDataSetChanged();
      }
      else {
         final Section sectionObject = mSections.get(section);
         notifyItemRangeChanged(sectionObject.position, sectionObject.length);
      }
   }

   public void notifySectionItemRangeInserted(int section, int position, int count) {
      calculateSections();
      if (mSections == null) {
         notifyAllSectionsDataSetChanged();
      }
      else {
         final Section sectionObject = mSections.get(section);

         if (position < 0 || position >= sectionObject.itemNumber) {
            throw new IndexOutOfBoundsException("Invalid index " + position + ", size is " + sectionObject.itemNumber);
         }
         if (position + count > sectionObject.itemNumber) {
            throw new IndexOutOfBoundsException("Invalid index " + (position + count) + ", size is " + sectionObject.itemNumber);
         }

         notifyItemRangeInserted(sectionObject.position + position + 1, count);
      }
   }

   private void notifySectionItemRangeRemoved(int section, int position, int count) {
      if (mSections == null) {
         calculateSections();
         notifyAllSectionsDataSetChanged();
      }
      else {
         final Section sectionObject = mSections.get(section);

         if (position < 0 || position >= sectionObject.itemNumber) {
            throw new IndexOutOfBoundsException("Invalid index " + position + ", size is " + sectionObject.itemNumber);
         }
         if (position + count > sectionObject.itemNumber) {
            throw new IndexOutOfBoundsException("Invalid index " + (position + count) + ", size is " + sectionObject.itemNumber);
         }

         calculateSections();
         notifyItemRangeRemoved(sectionObject.position + position + 1, count);
      }
   }

   public void notifySectionItemChanged(int section, int position) {
      calculateSections();
      if (mSections == null) {
         notifyAllSectionsDataSetChanged();
      }
      else {
         final Section sectionObject = mSections.get(section);

         if (position >= sectionObject.itemNumber) {
            throw new IndexOutOfBoundsException("Invalid index " + position + ", size is " + sectionObject.itemNumber);
         }

         notifyItemChanged(sectionObject.position + position + 1);
      }
   }

   public void notifySectionItemInserted(int section, int position) {
      calculateSections();
      if (mSections == null) {
         notifyAllSectionsDataSetChanged();
      }
      else {
         final Section sectionObject = mSections.get(section);

         if (position < 0 || position >= sectionObject.itemNumber) {
            throw new IndexOutOfBoundsException("Invalid index " + position + ", size is " + sectionObject.itemNumber);
         }

         notifyItemInserted(sectionObject.position + position + 1);
      }
   }

   public void notifySectionItemRemoved(int section, int position) {
      if (mSections == null) {
         calculateSections();
         notifyAllSectionsDataSetChanged();
      }
      else {
         final Section sectionObject = mSections.get(section);

         if (position < 0 || position >= sectionObject.itemNumber) {
            throw new IndexOutOfBoundsException("Invalid index " + position + ", size is " + sectionObject.itemNumber);
         }

         calculateSections();
         notifyItemRemoved(sectionObject.position + position + 1);
      }
   }

   public void notifySectionInserted(int section) {
      calculateSections();
      if (mSections == null) {
         notifyAllSectionsDataSetChanged();
      }
      else {
         final Section sectionObject = mSections.get(section);
         notifyItemRangeInserted(sectionObject.position, sectionObject.length);
      }
   }

   public void notifySectionRemoved(int section) {
      if (mSections == null) {
         calculateSections();
         notifyAllSectionsDataSetChanged();
      }
      else {
         final Section sectionObject = mSections.get(section);
         calculateSections();
         notifyItemRangeRemoved(sectionObject.position, sectionObject.length);
      }
   }
}
