package com.codewaves.stickyheadergrid;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

/**
 * Created by Sergej Kravcenko on 4/24/2017.
 * Copyright (c) 2017 Sergej Kravcenko
 */

@SuppressWarnings({"unused", "WeakerAccess"})
public class StickyHeaderGridLayoutManager extends RecyclerView.LayoutManager {
   public static final String TAG = "StickyLayoutManager";

   private static final int DEFAULT_ROW_COUNT = 16;

   private int mSpanCount;
   private SpanSizeLookup mSpanSizeLookup = new DefaultSpanSizeLookup();

   private StickyHeaderGridAdapter mAdapter;

   private int mHeadersStartPosition;

   private View mFloatingHeaderView;
   private int mFloatingHeaderPosition;
   private int mStickOffset;

   private View mFillViewSet[];

   private SavedState mPendingSavedState;
   private int mPendingScrollPosition = NO_POSITION;
   private int mPendingScrollPositionOffset;
   private int mFirstViewPosition;
   private int mFirstViewOffset;

   private final FillResult mFillResult = new FillResult();
   private ArrayList<LayoutRow> mLayoutRows = new ArrayList<>(DEFAULT_ROW_COUNT);


   /**
    * Creates a vertical StickyHeaderGridLayoutManager
    *
    * @param spanCount The number of columns in the grid
    */
   public StickyHeaderGridLayoutManager(int spanCount) {
      mSpanCount = spanCount;
      mFillViewSet = new View[spanCount];
      if (spanCount < 1) {
         throw new IllegalArgumentException("Span count should be at least 1. Provided " + spanCount);
      }
   }

   /**
    * Sets the source to get the number of spans occupied by each item in the adapter.
    *
    * @param spanSizeLookup {@link StickyHeaderGridLayoutManager.SpanSizeLookup} instance to be used to query number of spans
    *                       occupied by each item
    */
   public void setSpanSizeLookup(SpanSizeLookup spanSizeLookup) {
      mSpanSizeLookup = spanSizeLookup;
      if (mSpanSizeLookup == null) {
         mSpanSizeLookup = new DefaultSpanSizeLookup();
      }
   }

   /**
    * Returns the current {@link StickyHeaderGridLayoutManager.SpanSizeLookup} used by the StickyHeaderGridLayoutManager.
    *
    * @return The current {@link StickyHeaderGridLayoutManager.SpanSizeLookup} used by the StickyHeaderGridLayoutManager.
    */
   public SpanSizeLookup getSpanSizeLookup() {
      return mSpanSizeLookup;
   }

   @Override
   public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
      super.onAdapterChanged(oldAdapter, newAdapter);

      try {
         mAdapter = (StickyHeaderGridAdapter)newAdapter;
      }
      catch (ClassCastException e) {
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
      }
      catch (ClassCastException e) {
         throw new ClassCastException("Adapter used with StickyHeaderGridLayoutManager must be kind of StickyHeaderGridAdapter");
      }
   }

   @Override
   public RecyclerView.LayoutParams generateDefaultLayoutParams() {
      return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
   }

   @Override
   public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
      return new LayoutParams(c, attrs);
   }

   @Override
   public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
      if (lp instanceof ViewGroup.MarginLayoutParams) {
         return new LayoutParams((ViewGroup.MarginLayoutParams)lp);
      }
      else {
         return new LayoutParams(lp);
      }
   }

   @Override
   public Parcelable onSaveInstanceState() {
      if (mPendingSavedState != null) {
         return new SavedState(mPendingSavedState);
      }

      SavedState state = new SavedState();
      if (getChildCount() > 0) {
         state.mAnchorPosition = mFirstViewPosition;
         state.mAnchorOffset = mFirstViewOffset;
      }
      else {
         state.invalidateAnchor();
      }

      return state;
   }

   @Override
   public void onRestoreInstanceState(Parcelable state) {
      if (state instanceof SavedState) {
         mPendingSavedState = (SavedState) state;
         requestLayout();
      }
      else {
         Log.d(TAG, "invalid saved state class");
      }
   }

   @Override
   public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
      return lp instanceof LayoutParams;
   }

   @Override
   public boolean canScrollVertically() {
      return true;
   }

   /**
    * <p>Scroll the RecyclerView to make the position visible.</p>
    *
    * <p>RecyclerView will scroll the minimum amount that is necessary to make the
    * target position visible.
    *
    * <p>Note that scroll position change will not be reflected until the next layout call.</p>
    *
    * @param position Scroll to this adapter position
    */
   @Override
   public void scrollToPosition(int position) {
      if (position < 0 || position > getItemCount()) {
         throw new IndexOutOfBoundsException("adapter position out of range");
      }

      mPendingScrollPosition = position;
      mPendingScrollPositionOffset = 0;
      if (mPendingSavedState != null) {
         mPendingSavedState.invalidateAnchor();
      }
      requestLayout();
   }

   @Override
   public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
      if (mAdapter == null || state.getItemCount() == 0) {
         removeAndRecycleAllViews(recycler);
         clearState();
         return;
      }

      if (mPendingScrollPosition >= 0) {
         mFirstViewPosition = mPendingScrollPosition;
         mFirstViewOffset = mPendingScrollPositionOffset;
         mPendingScrollPosition = NO_POSITION;
      }
      else if (mPendingSavedState != null && mPendingSavedState.hasValidAnchor()) {
         mFirstViewPosition = mPendingSavedState.mAnchorPosition;
         mFirstViewOffset = mPendingSavedState.mAnchorOffset;
         mPendingSavedState = null;
      }

      if (mFirstViewPosition < 0 || mFirstViewPosition > state.getItemCount()) {
         mFirstViewPosition = 0;
         mFirstViewOffset = getPaddingTop();
      }

      if (mFirstViewOffset > 0) {
         mFirstViewOffset = 0;
      }

      detachAndScrapAttachedViews(recycler);
      clearState();

      // Make sure mFirstViewPosition is the start of the row
      mFirstViewPosition = findFirstRowItem(mFirstViewPosition);

      int left = getPaddingLeft();
      int right = getWidth() - getPaddingRight();
      final int recyclerBottom = getHeight() - getPaddingBottom();
      int totalHeight = 0;

      int adapterPosition = mFirstViewPosition;
      int top = getPaddingTop() + mFirstViewOffset;
      while (true) {
         if (adapterPosition >= state.getItemCount()) {
            break;
         }

         int bottom;
         final int viewType = mAdapter.getItemViewInternalType(adapterPosition);
         if (viewType == StickyHeaderGridAdapter.TYPE_HEADER) {
            final View v = recycler.getViewForPosition(adapterPosition);
            addView(v, mHeadersStartPosition);
            measureChildWithMargins(v, 0, 0);

            final int height = getDecoratedMeasuredHeight(v);
            bottom = top + height;
            layoutDecorated(v, left, top, right, bottom);
            mLayoutRows.add(new LayoutRow(v, adapterPosition, 1, top, bottom));
            adapterPosition++;
         }
         else {
            final FillResult result = fillBottomRow(recycler, state, adapterPosition, top);
            bottom = top + result.height;
            mLayoutRows.add(new LayoutRow(result.adapterPosition, result.length, top, bottom));
            adapterPosition += result.length;
         }
         top = bottom;

         if (bottom >= recyclerBottom) {
            break;
         }
      }

      if (getBottomRow().bottom < recyclerBottom) {
         scrollVerticallyBy(getBottomRow().bottom - recyclerBottom, recycler, state);
      }
      else {
         clearViewsAndStickHeaders(recycler);
      }
   }

   @Override
   public void onLayoutCompleted(RecyclerView.State state) {
      super.onLayoutCompleted(state);
      mPendingSavedState = null;
   }

   private int findFirstRowItem(int adapterPosition) {
      final int section = mAdapter.getPositionSection(adapterPosition);
      int sectionPosition = mAdapter.getItemSectionPosition(section, adapterPosition);
      while (sectionPosition > 0 && mSpanSizeLookup.getSpanIndex(section, sectionPosition, mSpanCount) != 0) {
         sectionPosition--;
         adapterPosition--;
      }

      return adapterPosition;
   }

   private int getSpanWidth(int recyclerWidth, int spanIndex, int spanSize) {
      final int spanWidth = recyclerWidth / mSpanCount;
      final int spanWidthReminder = recyclerWidth - spanWidth * mSpanCount;
      final int widthCorrection = Math.min(Math.max(0, spanWidthReminder - spanIndex), spanSize);

      return spanWidth * spanSize + widthCorrection;
   }

   private int getSpanLeft(int recyclerWidth, int spanIndex) {
      final int spanWidth = recyclerWidth / mSpanCount;
      final int spanWidthReminder = recyclerWidth - spanWidth * mSpanCount;
      final int widthCorrection = Math.min(spanWidthReminder, spanIndex);

      return spanWidth * spanIndex + widthCorrection;
   }

   private FillResult fillBottomRow(RecyclerView.Recycler recycler, RecyclerView.State state, int position, int top) {
      final int recyclerWidth = getWidth() - getPaddingLeft() - getPaddingRight();
      final int section = mAdapter.getPositionSection(position);
      int adapterPosition = position;
      int sectionPosition = mAdapter.getItemSectionPosition(section, adapterPosition);
      int spanSize = mSpanSizeLookup.getSpanSize(section, sectionPosition);
      int spanIndex = mSpanSizeLookup.getSpanIndex(section, sectionPosition, mSpanCount);
      int count = 0;
      int maxHeight = 0;

      // Create phase
      Arrays.fill(mFillViewSet, null);
      while (spanIndex < mSpanCount) {
         // Create view and fill layout params
         final int spanWidth = getSpanWidth(recyclerWidth, spanIndex, spanSize);
         final View v = recycler.getViewForPosition(adapterPosition);
         final LayoutParams params = (LayoutParams)v.getLayoutParams();
         params.mSpanIndex = spanIndex;
         params.mSpanSize = spanSize;

         addView(v, mHeadersStartPosition);
         mHeadersStartPosition++;
         measureChildWithMargins(v, recyclerWidth - spanWidth, 0);
         mFillViewSet[count] = v;
         count++;

         final int height = getDecoratedMeasuredHeight(v);
         if (maxHeight < height) {
            maxHeight = height;
         }

         // Check next
         adapterPosition++;
         sectionPosition++;
         if (sectionPosition >= mAdapter.getSectionItemCount(section)) {
            break;
         }

         spanIndex += spanSize;
         spanSize = mSpanSizeLookup.getSpanSize(section, sectionPosition);
      }

      // Layout phase
      int left = getPaddingLeft();
      for (int i = 0; i < count; ++i) {
         final View v = mFillViewSet[i];
         final int height = getDecoratedMeasuredHeight(v);
         final int width = getDecoratedMeasuredWidth(v);
         layoutDecorated(v, left, top, left + width, top + height);
         left += width;
      }

      mFillResult.edgeView = mFillViewSet[count - 1];
      mFillResult.adapterPosition = position;
      mFillResult.length = count;
      mFillResult.height = maxHeight;

      return mFillResult;
   }

   private FillResult fillTopRow(RecyclerView.Recycler recycler, RecyclerView.State state, int position, int top) {
      final int recyclerWidth = getWidth() - getPaddingLeft() - getPaddingRight();
      final int section = mAdapter.getPositionSection(position);
      int adapterPosition = position;
      int sectionPosition = mAdapter.getItemSectionPosition(section, adapterPosition);
      int spanSize = mSpanSizeLookup.getSpanSize(section, sectionPosition);
      int spanIndex = mSpanSizeLookup.getSpanIndex(section, sectionPosition, mSpanCount);
      int count = 0;
      int maxHeight = 0;

      Arrays.fill(mFillViewSet, null);
      while (spanIndex >= 0) {
         // Create view and fill layout params
         final int spanWidth = getSpanWidth(recyclerWidth, spanIndex, spanSize);
         final View v = recycler.getViewForPosition(adapterPosition);
         final LayoutParams params = (LayoutParams)v.getLayoutParams();
         params.mSpanIndex = spanIndex;
         params.mSpanSize = spanSize;

         addView(v, 0);
         mHeadersStartPosition++;
         measureChildWithMargins(v, recyclerWidth - spanWidth, 0);
         mFillViewSet[count] = v;
         count++;

         final int height = getDecoratedMeasuredHeight(v);
         if (maxHeight < height) {
            maxHeight = height;
         }

         // Check next
         adapterPosition--;
         sectionPosition--;
         if (sectionPosition < 0) {
            break;
         }

         spanSize = mSpanSizeLookup.getSpanSize(section, sectionPosition);
         spanIndex -= spanSize;
      }

      // Layout phase
      int left = getPaddingLeft();
      for (int i = count - 1; i >= 0; --i) {
         final View v = mFillViewSet[i];
         final int height = getDecoratedMeasuredHeight(v);
         final int width = getDecoratedMeasuredWidth(v);
         layoutDecorated(v, left, top - maxHeight, left + width, top - (maxHeight - height));
         left += width;
      }

      mFillResult.edgeView = mFillViewSet[count - 1];
      mFillResult.adapterPosition = adapterPosition + 1;
      mFillResult.length = count;
      mFillResult.height = maxHeight;

      return mFillResult;
   }

   private void clearHiddenRows(RecyclerView.Recycler recycler) {
      if (mLayoutRows.size() <= 0) {
         return;
      }

      final int recyclerTop = getPaddingTop();
      final int recyclerBottom = getHeight() - getPaddingBottom();

      // First pass, from top
      LayoutRow row = getTopRow();
      while (row.bottom < recyclerTop || row.top > recyclerBottom) {
         if (row.header) {
            removeAndRecycleViewAt(getChildCount() - (mFloatingHeaderView != null ? 2 : 1), recycler);
         }
         else {
            for (int i = 0; i < row.length; ++i) {
               removeAndRecycleViewAt(0, recycler);
               mHeadersStartPosition--;
            }
         }
         mLayoutRows.remove(0);
         row = getTopRow();
      }

      if (mLayoutRows.size() <= 0) {
         return;
      }

      // Second, from bottom
      row = getBottomRow();
      while (row.bottom < recyclerTop || row.top > recyclerBottom) {
         if (row.header) {
            removeAndRecycleViewAt(mHeadersStartPosition, recycler);
         }
         else {
            for (int i = 0; i < row.length; ++i) {
               removeAndRecycleViewAt(mHeadersStartPosition - 1, recycler);
               mHeadersStartPosition--;
            }
         }
         mLayoutRows.remove(mLayoutRows.size() - 1);
         row = getBottomRow();
      }
   }

   private void clearViewsAndStickHeaders(RecyclerView.Recycler recycler) {
      clearHiddenRows(recycler);

      // Update top/bottom views
      if (getChildCount() > 0) {
         stickTopHeader(recycler);
      }
      updateTopPosition();
   }

   private LayoutRow getBottomRow() {
      return mLayoutRows.get(mLayoutRows.size() - 1);
   }

   private LayoutRow getTopRow() {
      return mLayoutRows.get(0);
   }

   private void offsetRowsVertical(int offset) {
      for (LayoutRow row : mLayoutRows) {
         row.top += offset;
         row.bottom += offset;
      }
      offsetChildrenVertical(offset);
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
      final int firstHeader = getFirstVisibleSectionHeader();
      if (firstHeader != NO_POSITION) {
         mLayoutRows.get(firstHeader).headerView.offsetTopAndBottom(-mStickOffset);
      }

      if (dy >= 0) {
         // Up
         while (scrolled < dy) {
            final LayoutRow bottomRow = getBottomRow();
            final int scrollChunk = -Math.min(Math.max(bottomRow.bottom - recyclerBottom, 0), dy - scrolled);

            offsetRowsVertical(scrollChunk);
            scrolled -= scrollChunk;

            int adapterPosition = bottomRow.adapterPosition + bottomRow.length;
            if (scrolled >= dy || adapterPosition >= state.getItemCount()) {
               break;
            }

            final int top = bottomRow.bottom;
            final int viewType = mAdapter.getItemViewInternalType(adapterPosition);
            if (viewType == StickyHeaderGridAdapter.TYPE_HEADER) {
               final View v = recycler.getViewForPosition(adapterPosition);
               addView(v, mHeadersStartPosition);
               measureChildWithMargins(v, 0, 0);
               final int height = getDecoratedMeasuredHeight(v);
               layoutDecorated(v, left, top, right, top + height);
               mLayoutRows.add(new LayoutRow(v, adapterPosition, 1, top, top + height));
            }
            else {
               final FillResult result = fillBottomRow(recycler, state, adapterPosition, top);
               mLayoutRows.add(new LayoutRow(result.adapterPosition, result.length, top, top + result.height));
            }
         }
      }
      else {
         // Down
         while (scrolled > dy) {
            final LayoutRow topRow = getTopRow();
            final int scrollChunk = Math.min(Math.max(-topRow.top + recyclerTop, 0), scrolled - dy);

            offsetRowsVertical(scrollChunk);
            scrolled -= scrollChunk;

            final int top = topRow.top;
            int adapterPosition = topRow.adapterPosition - 1;
            if (scrolled <= dy || adapterPosition >= state.getItemCount() || adapterPosition < 0) {
               break;
            }

            // Reattach floating header if needed
            if (mFloatingHeaderView != null && adapterPosition == mFloatingHeaderPosition) {
               removeFloatingHeader(recycler);
            }

            final int viewType = mAdapter.getItemViewInternalType(adapterPosition);
            if (viewType == StickyHeaderGridAdapter.TYPE_HEADER) {
               final View v = recycler.getViewForPosition(adapterPosition);
               addView(v);
               measureChildWithMargins(v, 0, 0);
               final int height = getDecoratedMeasuredHeight(v);
               layoutDecorated(v, left, top - height, right, top);
               mLayoutRows.add(0, new LayoutRow(v, adapterPosition, 1, top - height, top));
            }
            else {
               final FillResult result = fillTopRow(recycler, state, adapterPosition, top);
               mLayoutRows.add(0, new LayoutRow(result.adapterPosition, result.length, top - result.height, top));
            }
         }
      }

      clearViewsAndStickHeaders(recycler);
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

   private LayoutRow getFirstVisibleRow() {
      final int recyclerTop = getPaddingTop();
      for (LayoutRow row : mLayoutRows) {
         if (row.bottom > recyclerTop) {
            return row;
         }
      }
      return null;
   }

   private int getFirstVisibleSectionHeader() {
      final int recyclerTop = getPaddingTop();

      int header = NO_POSITION;
      for (int i = 0, n = mLayoutRows.size(); i < n; ++i) {
         final LayoutRow row = mLayoutRows.get(i);
         if (row.header) {
            header = i;
         }
         if (row.bottom > recyclerTop) {
            return header;
         }
      }
      return NO_POSITION;
   }

   private LayoutRow getNextVisibleSectionHeader(int headerFrom) {
      for (int i = headerFrom + 1, n = mLayoutRows.size(); i < n; ++i) {
         final LayoutRow row = mLayoutRows.get(i);
         if (row.header) {
            return row;
         }
      }
      return null;
   }

   private void removeFloatingHeader(RecyclerView.Recycler recycler) {
      if (mFloatingHeaderView == null) {
         return;
      }

      final View view = mFloatingHeaderView;
      mFloatingHeaderView = null;
      mFloatingHeaderPosition = NO_POSITION;
      removeAndRecycleView(view, recycler);
   }

   private void stickTopHeader(RecyclerView.Recycler recycler) {
      final int firstHeader = getFirstVisibleSectionHeader();
      final int top = getPaddingTop();
      final int left = getPaddingLeft();
      final int right = getWidth() - getPaddingRight();

      if (firstHeader != NO_POSITION) {
         // Top row is header, floating header is not visible, remove
         removeFloatingHeader(recycler);

         final LayoutRow firstHeaderRow = mLayoutRows.get(firstHeader);
         final LayoutRow nextHeaderRow = getNextVisibleSectionHeader(firstHeader);
         int offset = 0;
         if (nextHeaderRow != null) {
            final int height = firstHeaderRow.getHeight();
            offset = Math.min(Math.max(top - nextHeaderRow.top, -height) + height, height);
         }

         mStickOffset = top - firstHeaderRow.top - offset;
         firstHeaderRow.headerView.offsetTopAndBottom(mStickOffset);
      }
      else {
         // We don't have first visible sector header in layout, create floating
         final LayoutRow firstVisibleRow = getFirstVisibleRow();
         final int section = mAdapter.getPositionSection(firstVisibleRow.adapterPosition);
         if (section != -1) {
            final int headerPosition = mAdapter.getSectionHeaderPosition(section);
            if (mFloatingHeaderView == null || mFloatingHeaderPosition != headerPosition) {
               removeFloatingHeader(recycler);

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

   private void updateTopPosition() {
      if (getChildCount() == 0) {
         mFirstViewPosition = 0;
         mFirstViewOffset = 0;
      }

      final LayoutRow firstVisibleRow = getFirstVisibleRow();
      if (firstVisibleRow != null) {
         mFirstViewPosition = firstVisibleRow.adapterPosition;
         mFirstViewOffset = Math.min(firstVisibleRow.top - getPaddingTop(), 0);
      }
   }

   private int getViewType(View view) {
      return getItemViewType(view) & 0xFF;
   }

   private int getViewType(int position) {
      return mAdapter.getItemViewType(position) & 0xFF;
   }

   private void clearState() {
      mHeadersStartPosition = 0;
      mStickOffset = 0;
      mFloatingHeaderView = null;
      mLayoutRows.clear();
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

   public static final class DefaultSpanSizeLookup extends SpanSizeLookup {
      @Override
      public int getSpanSize(int section, int position) {
         return 1;
      }

      @Override
      public int getSpanIndex(int section, int position, int spanCount) {
         return position % spanCount;
      }
   }

   /**
    * An interface to provide the number of spans each item occupies.
    * <p>
    * Default implementation sets each item to occupy exactly 1 span.
    *
    * @see StickyHeaderGridLayoutManager#setSpanSizeLookup(StickyHeaderGridLayoutManager.SpanSizeLookup)
    */
   public static abstract class SpanSizeLookup {
      /**
       * Returns the number of span occupied by the item in <code>section</code> at <code>position</code>.
       *
       * @param section The adapter section of the item
       * @param position The adapter position of the item in section
       * @return The number of spans occupied by the item at the provided section and position
       */
      abstract public int getSpanSize(int section, int position);

      /**
       * Returns the final span index of the provided position.
       *
       * <p>
       * If you override this method, you need to make sure it is consistent with
       * {@link #getSpanSize(int, int)}. StickyHeaderGridLayoutManager does not call this method for
       * each item. It is called only for the reference item and rest of the items
       * are assigned to spans based on the reference item. For example, you cannot assign a
       * position to span 2 while span 1 is empty.
       * <p>
       *
       * @param section The adapter section of the item
       * @param position  The adapter position of the item in section
       * @param spanCount The total number of spans in the grid
       * @return The final span position of the item. Should be between 0 (inclusive) and
       * <code>spanCount</code>(exclusive)
       */
      public int getSpanIndex(int section, int position, int spanCount) {
         // TODO: cache them?
         final int positionSpanSize = getSpanSize(section, position);
         if (positionSpanSize >= spanCount) {
            return 0;
         }

         int spanIndex = 0;
         for (int i = 0; i < position; ++i) {
            final int spanSize = getSpanSize(section, i);
            spanIndex += spanSize;

            if (spanIndex == spanCount) {
               spanIndex = 0;
            }
            else if (spanIndex > spanCount) {
               spanIndex = spanSize;
            }
         }

         if (spanIndex + positionSpanSize <= spanCount) {
            return spanIndex;
         }

         return 0;
      }
   }

   public static class SavedState implements Parcelable {
      int mAnchorPosition;
      int mAnchorOffset;

      public SavedState() {

      }

      SavedState(Parcel in) {
         mAnchorPosition = in.readInt();
         mAnchorOffset = in.readInt();
      }

      public SavedState(SavedState other) {
         mAnchorPosition = other.mAnchorPosition;
         mAnchorOffset = other.mAnchorOffset;
      }

      boolean hasValidAnchor() {
         return mAnchorPosition >= 0;
      }

      void invalidateAnchor() {
         mAnchorPosition = NO_POSITION;
      }

      @Override
      public int describeContents() {
         return 0;
      }

      @Override
      public void writeToParcel(Parcel dest, int flags) {
         dest.writeInt(mAnchorPosition);
         dest.writeInt(mAnchorOffset);
      }

      public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
         @Override
         public SavedState createFromParcel(Parcel in) {
            return new SavedState(in);
         }

         @Override
         public SavedState[] newArray(int size) {
            return new SavedState[size];
         }
      };
   }

   private static class LayoutRow {
      boolean header;
      View headerView;
      int adapterPosition;
      int length;
      int top;
      int bottom;

      public LayoutRow(int adapterPosition, int length, int top, int bottom) {
         this.header = false;
         this.headerView = null;
         this.adapterPosition = adapterPosition;
         this.length = length;
         this.top = top;
         this.bottom = bottom;
      }

      public LayoutRow(View headerView, int adapterPosition, int length, int top, int bottom) {
         this.header = true;
         this.headerView = headerView;
         this.adapterPosition = adapterPosition;
         this.length = length;
         this.top = top;
         this.bottom = bottom;
      }

      int getHeight() {
         return bottom - top;
      }
   }

   private static class FillResult {
      View edgeView;
      int adapterPosition;
      int length;
      int height;
   }
}
