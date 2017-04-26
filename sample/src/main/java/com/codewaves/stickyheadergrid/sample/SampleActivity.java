package com.codewaves.stickyheadergrid.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;

import com.codewaves.sample.R;
import com.codewaves.stickyheadergrid.StickyHeaderGridLayoutManager;

import static android.support.v7.widget.DividerItemDecoration.VERTICAL;

public class SampleActivity extends AppCompatActivity {
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_sample);

      // Setup recycler
      final RecyclerView recycler = (RecyclerView)findViewById(R.id.recycler);
      final StickyHeaderGridLayoutManager layoutManager = new StickyHeaderGridLayoutManager(3);
      /*layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
         @Override
         public int getSpanSize(int position) {
            return (3 - position % 3);
         }
      });*/
      recycler.addItemDecoration(new DividerItemDecoration(this, VERTICAL));
      recycler.setLayoutManager(layoutManager);
      recycler.setAdapter(new SampleAdapter(5));
   }
}
