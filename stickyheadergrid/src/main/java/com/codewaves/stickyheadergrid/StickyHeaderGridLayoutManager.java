package com.codewaves.stickyheadergrid;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Sergej Kravcenko on 4/24/2017.
 * Copyright (c) 2017 Sergej Kravcenko
 */

@SuppressWarnings({"unused", "WeakerAccess"})
public class StickyHeaderGridLayoutManager extends RecyclerView.LayoutManager {
   public static final String TAG = "StickyLayoutManager";

   private int mSpanCount;

   private StickyHeaderGridAdapter mAdapter;

   private int mHeadersStartPosition;

   private View mTopView;
   private View mBottomView;

   private View mFloatingHeaderView;
   private int mFloatingHeaderPosition;
   private int mStickOffset;

   public StickyHeaderGridLayoutManager(int spanCount) {
      mSpanCount = spanCount;
      if (spanCount < 1) {
         throw new IllegalArgumentException("Span count should be at least 1. Provided " + spanCount);
      }
   }

   @Override
   public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
      super.onAdapterChanged(oldAdapter, newAdapter);

      try {
         mAdapter = (StickyHeaderGridAdapter)newAdapter;
      } catch (ClassCastException e) {
         throw new ClassCastException("Adapter used with StickyHeaderGridLayoutManager must be kind of StickyHeaderGridAdapter");
      }

      removeAllViews();
      clearState();
   }

   @Override
   public void onAttachedToWindow(RecyclerView view) {
      super.onAttachedToWindow(view);

      try {
         mAdapter = (StickyHeaderGridAdapter)view.getAdapter();
      } catch (ClassCastException e) {
         throw new ClassCastException("Adapter used with StickyHeaderGridLayoutManager must be kind of StickyHeaderGridAdapter");
      }
   }

   @Override
   public RecyclerView.LayoutParams generateDefaultLayoutParams() {
      return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
   }

   @Override
   public boolean canScrollVertically() {
      return true;
   }

   @Override
   public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
      if (state.getItemCount() == 0) {
         removeAndRecycleAllViews(recycler);
         return;
      }
      // TODO: check this detach logic
      detachAndScrapAttachedViews(recycler);
      clearState();

      int firstAdapterPosition = 0;
      if (firstAdapterPosition > state.getItemCount()) {
         firstAdapterPosition = 0;
      }

      int height;
      int top = getPaddingTop();
      int left = getPaddingLeft();
      int right = getWidth() - getPaddingRight();
      final int bottom = getHeight() - getPaddingBottom();
      int totalHeight = 0;

      for (int adapterPosition = firstAdapterPosition; adapterPosition < state.getItemCount(); adapterPosition++) {
         final View v = recycler.getViewForPosition(adapterPosition);

         final int viewType = getViewType(v);
         if (viewType == StickyHeaderGridAdapter.TYPE_HEADER) {
            addView(v, mHeadersStartPosition);
            measureChildWithMargins(v, 0, 0);
            height = getDecoratedMeasuredHeight(v);
            layoutDecorated(v, left, top, right, top + height);

            if (mTopView == null) {
               mTopView = v;
            }
            mBottomView = v;
         }
         else {
            addView(v, mHeadersStartPosition);
            measureChildWithMargins(v, 0, 0);
            height = getDecoratedMeasuredHeight(v);
            layoutDecorated(v, left, top, right, top + height);

            if (mTopView == null) {
               mTopView = v;
            }
            mBottomView = v;
            mHeadersStartPosition++;
         }

         top += height;
         totalHeight += height;

         if (getDecoratedBottom(v) >= bottom) {
            break;
         }
      }

      int recyclerHeight = getHeight() - (getPaddingTop() + getPaddingBottom());
      if (totalHeight < recyclerHeight) {
         scrollVerticallyBy(totalHeight - recyclerHeight, recycler, state);
      }
      else {
         stickTopHeader(recycler);
      }
   }

   @Override
   public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
      if (getChildCount() == 0) {
         return 0;
      }

      int scrolled = 0;
      int left = getPaddingLeft();
      int right = getWidth() - getPaddingRight();
      final int recyclerTop = getPaddingTop();
      final int recyclerBottom = getHeight() - getPaddingBottom();

      // If we have simple header stick, offset it back
      final int topViewType = getViewType(mTopView);
      if (topViewType == StickyHeaderGridAdapter.TYPE_HEADER) {
         mTopView.offsetTopAndBottom(-mStickOffset);
      }

      if (dy >= 0) {
         // Up
         while (scrolled < dy) {
            final int scrollChunk = -Math.min(Math.max(getDecoratedBottom(mBottomView) - recyclerBottom, 0), dy - scrolled);

            offsetChildrenVertical(scrollChunk);
            scrolled -= scrollChunk;

            final int top = getDecoratedBottom(mBottomView);
            int adapterPosition = getPosition(mBottomView) + 1;
            if (scrolled >= dy || adapterPosition >= state.getItemCount()) {
               break;
            }

            final View v = recycler.getViewForPosition(adapterPosition);
            mBottomView = v;

            final int viewType = getViewType(v);
            if (viewType == StickyHeaderGridAdapter.TYPE_HEADER) {
               addView(v, mHeadersStartPosition);
               measureChildWithMargins(v, 0, 0);
               final int height = getDecoratedMeasuredHeight(v);
               layoutDecorated(v, left, top, right, top + height);
            }
            else {
               addView(v, mHeadersStartPosition);
               measureChildWithMargins(v, 0, 0);
               final int height = getDecoratedMeasuredHeight(v);
               layoutDecorated(v, left, top, right, top + height);
               mHeadersStartPosition++;
            }
         }
      }
      else {
         // Down
         while (scrolled > dy) {
            final int scrollChunk = Math.min(Math.max(-getDecoratedTop(mTopView) + recyclerTop, 0), scrolled - dy);

            offsetChildrenVertical(scrollChunk);
            scrolled -= scrollChunk;

            final int top = getDecoratedTop(mTopView);
            int adapterPosition = getPosition(mTopView) - 1;
            if (scrolled <= dy || adapterPosition >= state.getItemCount() || adapterPosition < 0) {
               break;
            }

            if (mFloatingHeaderView != null && adapterPosition == mFloatingHeaderPosition) {
               removeAndRecycleView(mFloatingHeaderView, recycler);
               mFloatingHeaderView = null;
               mFloatingHeaderPosition = -1;
            }

            final View v = recycler.getViewForPosition(adapterPosition);
            mTopView = v;

            final int viewType = getViewType(v);
            if (viewType == StickyHeaderGridAdapter.TYPE_HEADER) {
               addView(v);
               measureChildWithMargins(v, 0, 0);
               final int height = getDecoratedMeasuredHeight(v);
               layoutDecorated(v, left, top - height, right, top);
            }
            else {
               addView(v, 0);
               measureChildWithMargins(v, 0, 0);
               final int height = getDecoratedMeasuredHeight(v);
               layoutDecorated(v, left, top - height, right, top);
               mHeadersStartPosition++;
            }
         }
      }

      // Remove hidden item views
      for (int i = 0; i < mHeadersStartPosition; ++i) {
         final View v = getChildAt(i);

         if (getDecoratedBottom(v) < recyclerTop || getDecoratedTop(v) > recyclerBottom) {
            removeAndRecycleView(v, recycler);
            mHeadersStartPosition--;
            i--;
         }
      }

      // Remove hidden header views
      for (int i = mHeadersStartPosition; i < getChildCount(); ++i) {
         final View v = getChildAt(i);

         if (v != mFloatingHeaderView && (getDecoratedBottom(v) < recyclerTop || getDecoratedTop(v) > recyclerBottom)) {
            removeAndRecycleView(v, recycler);
            i--;
         }
      }

      // Update top/bottom views
      if (getChildCount() > 0) {
         mTopView = getTopmostView();
         mBottomView = getBottommostView();

         stickTopHeader(recycler);
      }
      else {
         mTopView = mBottomView = null;
      }

      return  scrolled;
   }

   private View getTopmostView() {
      View top = null;
      if (getChildCount() > 0 && mHeadersStartPosition > 0) {
         top = getChildAt(0);
      }

      for (int i = getChildCount() - 1; i >= mHeadersStartPosition ; --i) {
         final View topHeader = getChildAt(i);
         if (topHeader == mFloatingHeaderView) {
            continue;
         }

         if (top == null || getDecoratedTop(topHeader) < getDecoratedTop(top)) {
            top = topHeader;
            break;
         }
      }

      return top;
   }

   private View getBottommostView() {
      View bottom = null;
      if (getChildCount() > 0 && mHeadersStartPosition > 0) {
         bottom = getChildAt(mHeadersStartPosition - 1);
      }

      if (mHeadersStartPosition < getChildCount()) {
         final View bottomHeader = getChildAt(mHeadersStartPosition);
         if (bottom == null || getDecoratedBottom(bottomHeader) > getDecoratedBottom(bottom)) {
            bottom = bottomHeader;
         }
      }

      return bottom;
   }

   private View getNextHeader(View fromHeader) {
      boolean found = false;
      for (int i = getChildCount() - 1; i >= mHeadersStartPosition ; --i) {
         final View header = getChildAt(i);
         if (header == fromHeader) {
            found = true;
            continue;
         }

         if (found) {
            return header;
         }
      }

      return null;
   }

   private void stickTopHeader(RecyclerView.Recycler recycler) {
      final int topViewType = getViewType(mTopView);

      final int top = getPaddingTop();
      final int left = getPaddingLeft();
      final int right = getWidth() - getPaddingRight();

      if (topViewType == StickyHeaderGridAdapter.TYPE_HEADER) {
         final int height = getDecoratedMeasuredHeight(mTopView);
         final View nextHeader = getNextHeader(mTopView);
         int offset = 0;
         if (nextHeader != null) {
            offset = Math.max(top - getDecoratedTop(nextHeader), -height) + height;
         }

         if (offset <= 0) {
            mStickOffset = top - getDecoratedTop(mTopView);
            mTopView.offsetTopAndBottom(mStickOffset);
         }
         else {
            mStickOffset = 0;
         }
      }
      else {
         // Find section number and create header if needed
         final int adapterPosition = getPosition(mTopView);
         final int section = mAdapter.getPositionSection(adapterPosition);

         if (section != -1) {
            final int headerPosition = mAdapter.getSectionHeaderPosition(section);

            if (mFloatingHeaderView == null || mFloatingHeaderPosition != headerPosition) {
               if (mFloatingHeaderView != null) {
                  removeAndRecycleView(mFloatingHeaderView, recycler);
               }

               // Create floating header
               final View v = recycler.getViewForPosition(headerPosition);
               addView(v);
               measureChildWithMargins(v, 0, 0);
               mFloatingHeaderView = v;
               mFloatingHeaderPosition = headerPosition;
            }

            // Push floating header up, if needed
            final int height = getDecoratedMeasuredHeight(mFloatingHeaderView);
            int offset = 0;
            if (getChildCount() - mHeadersStartPosition > 1) {
               final View nextHeader = getChildAt(getChildCount() - 2);
               offset = Math.max(top - getDecoratedTop(nextHeader), -height) + height;
            }

            layoutDecorated(mFloatingHeaderView, left, top - offset, right, top + height - offset);
         }
      }
   }

   private int getViewType(View view) {
      return getItemViewType(view) & 0xFF;
   }

   private void clearState() {
      mTopView = mBottomView = null;
      mHeadersStartPosition = 0;
      mStickOffset = 0;
      mFloatingHeaderView = null;
   }

   public static class LayoutParams extends RecyclerView.LayoutParams {
      public static final int INVALID_SPAN_ID = -1;

      int mSpanIndex = INVALID_SPAN_ID;
      int mSpanSize = 0;

      public LayoutParams(Context c, AttributeSet attrs) {
         super(c, attrs);
      }

      public LayoutParams(int width, int height) {
         super(width, height);
      }

      public LayoutParams(ViewGroup.MarginLayoutParams source) {
         super(source);
      }

      public LayoutParams(ViewGroup.LayoutParams source) {
         super(source);
      }

      public LayoutParams(RecyclerView.LayoutParams source) {
         super(source);
      }

      public int getSpanIndex() {
         return mSpanIndex;
      }

      public int getSpanSize() {
         return mSpanSize;
      }
   }
}
