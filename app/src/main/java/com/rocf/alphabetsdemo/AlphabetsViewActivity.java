package com.rocf.alphabetsdemo;

import android.app.ListActivity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;

import com.rocf.library.AlphabetsView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AlphabetsViewActivity extends ListActivity {

    AlphabetsView mAlphabetWavesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alphabetsview);

        // Use an existing ListAdapter that will map an array
        // of strings to TextViews
        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mStrings));
        getListView().setTextFilterEnabled(true);
        mAlphabetWavesView = (AlphabetsView) findViewById(R.id.listSideIndex);
//        mAlphabetWavesView.setIsAlphabetAnima(false);
        mAlphabetWavesView.setOnAlphabetListener(mAlphabetListener);
        Drawable alphabetDrawable = getResources().getDrawable(R.drawable.first_alphabet);
        HashMap<String, Drawable> alphabetDrawableSet = new HashMap<>();
        alphabetDrawableSet.put("!", alphabetDrawable);
        Drawable popupDrawable = getResources().getDrawable(R.drawable.first_pop_alphabet);
        HashMap<String, Drawable> popupDrawableSet = new HashMap<>();
        popupDrawableSet.put("!", popupDrawable);
        mAlphabetWavesView.setAlphabetList(Arrays.asList(mStrings), alphabetDrawableSet, popupDrawableSet);
        MyScrollListener my = new MyScrollListener();
        getListView().setOnScrollListener(my);
        mAlphabetWavesView.setIsAlphabetAnima(true);
    }


    private AlphabetsView.OnAlphabetListener mAlphabetListener = new AlphabetsView.OnAlphabetListener() {
        @Override
        public void onAlphabetChanged(int alphabetPosition, String firstAlphabet) {

            //need to setSelection
            // getListView().setSelection();
        }
    };
    public String[] mStrings = Names.mNameStrings;

    class MyScrollListener implements AbsListView.OnScrollListener {
        public CharSequence old = "";
        public int a = 0;

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {

            //just for demo ,logic is error
            List<String> alphabets = mAlphabetWavesView.getAlphabetList();
            old = mStrings[firstVisibleItem].substring(0, 1);
            int selectIndex = alphabets.indexOf(old);
            if (mAlphabetWavesView.getCurrentSelectedPos() > selectIndex) {
                mAlphabetWavesView.setSelection(selectIndex - 5, selectIndex, selectIndex);
            } else {
                mAlphabetWavesView.setSelection(selectIndex, selectIndex + 5, selectIndex);
            }
        }

    }
}
