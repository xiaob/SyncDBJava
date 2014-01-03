package net.kymjs.music.ui.fragment;

import net.kymjs.music.R;
import net.kymjs.music.adapter.LrcListAdapter;
import net.kymjs.music.ui.Main;
import net.kymjs.music.ui.widget.TabLayout;
import net.kymjs.music.ui.widget.TabLayout.OnViewChangeListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class LyricFragment extends BaseFragment {

    private TabLayout mScrollLayout;
    private ListView mPlayList;

    @Override
    public View setView(LayoutInflater inflater, ViewGroup container,
            Bundle bundle) {
        View view = inflater.inflate(R.layout.frag_lyric, container, false);
        return view;
    }

    @Override
    public void initWidget(View parentView) {
        initScrollLayout(parentView);
        ((Main) getActivity()).getResideMenu().addIgnoredView(mScrollLayout);
        mPlayList = (ListView) parentView.findViewById(R.id.lrc_pager_list);
        mPlayList.setAdapter(new LrcListAdapter(getActivity()));
    }

    /**
     * 初始化主体界面
     */
    private void initScrollLayout(View parentView) {
        mScrollLayout = (TabLayout) parentView.findViewById(R.id.lrc_tablayout);
        mScrollLayout.scrollToScreen(2);
        int mScrollChildCount = mScrollLayout.getChildCount();

        final RadioGroup circles = (RadioGroup) parentView
                .findViewById(R.id.lrc_circle_layout);
        for (int i = 0; i < mScrollChildCount; i++) {
            circles.addView(getCircles());
            ((RadioButton) circles.getChildAt(0)).setChecked(true);
        }
        mScrollLayout.SetOnViewChangeListener(new OnViewChangeListener() {
            @Override
            public void OnViewChange(int view) {
                RadioButton circle = (RadioButton) circles.getChildAt(view);
                circle.setChecked(true);
            }
        });
    }

    /**
     * 获取一个"小点"
     */
    private RadioButton getCircles() {
        RadioButton circle = new RadioButton(getActivity());
        int dimen5 = (int) getResources().getDimension(R.dimen.space_5);
        int dimen3 = (int) getResources().getDimension(R.dimen.space_3);
        RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(dimen5,
                dimen5);
        params.setMargins(dimen3, 0, dimen3, 0);
        circle.setLayoutParams(params);
        circle.setBackgroundResource(R.drawable.selector_rbtn_circle);
        return circle;
    }

    @Override
    public void widgetClick(View parentView) {

    }
}
