package com.codewaves.stickyheadergrid.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.codewaves.sample.R;
import com.codewaves.stickyheadergrid.StickyHeaderGridLayoutManager;

public class SampleActivity extends AppCompatActivity {
   private static final int SECTIONS = 30;
   private static final int SECTION_ITEMS = 8;

   private RecyclerView mRecycler;
   private StickyHeaderGridLayoutManager mLayoutManager;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_sample);

      // Setup recycler
      mRecycler = (RecyclerView)findViewById(R.id.recycler);
      mLayoutManager = new StickyHeaderGridLayoutManager(3);
      mLayoutManager.setSpanSizeLookup(new StickyHeaderGridLayoutManager.SpanSizeLookup() {
         @Override
         public int getSpanSize(int section, int position) {
            return (3 - position % 3);
         }
      });
      //recycler.addItemDecoration(new DividerItemDecoration(this, VERTICAL));
      mRecycler.setLayoutManager(mLayoutManager);
      mRecycler.setAdapter(new SampleAdapter(SECTIONS, SECTION_ITEMS));
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.main, menu);
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      int id = item.getItemId();
      switch (id) {
         case R.id.action_top:
            mRecycler.scrollToPosition(2);
            break;
         case R.id.action_center:
            mRecycler.scrollToPosition(SECTIONS * SECTION_ITEMS / 2);
            break;
         case R.id.action_bottom:
            mRecycler.scrollToPosition(SECTIONS * (SECTION_ITEMS + 1) - 1);
            break;
         case R.id.action_top_smooth:
            mRecycler.smoothScrollToPosition(3);
            break;
         case R.id.action_center_smooth:
            mRecycler.smoothScrollToPosition(SECTIONS * SECTION_ITEMS / 2);
            break;
         case R.id.action_bottom_smooth:
            mRecycler.smoothScrollToPosition(SECTIONS * (SECTION_ITEMS + 1) - 1);
            break;
      }
      return super.onOptionsItemSelected(item);

   }
}
