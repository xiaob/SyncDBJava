package net.kymjs.music.ui.fragment;

import java.io.Serializable;

import net.kymjs.music.AppLog;
import net.kymjs.music.Config;
import net.kymjs.music.R;
import net.kymjs.music.adapter.LrcListAdapter;
import net.kymjs.music.bean.Music;
import net.kymjs.music.service.DownMusicInfo;
import net.kymjs.music.service.DownMusicLrc;
import net.kymjs.music.ui.Main;
import net.kymjs.music.ui.widget.LrcView;
import net.kymjs.music.ui.widget.TabLayout;
import net.kymjs.music.ui.widget.TabLayout.OnViewChangeListener;
import net.kymjs.music.utils.LyricHelper;
import net.kymjs.music.utils.Player;
import net.kymjs.music.utils.PreferenceHelper;
import net.kymjs.music.utils.UIHelper;
import net.tsz.afinal.FinalDb;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * 歌词界面
 * 
 * @author kymjs
 */
public class LyricFragment extends BaseFragment {
    // 主体部分的控件
    private TabLayout mScrollLayout;
    private Button mBtnBack;
    private CheckBox mCboxWordImg;
    private SeekBar mSeekBarMusic;
    private SeekThread mSeekThread = new SeekThread();
    private SeekHandle mSeekHandle = new SeekHandle();
    private Player mPlayer = Player.getPlayer();
    private FinalDb db = FinalDb.create(getActivity(), Config.DB_NAME,
            Config.isDebug);
    private DownloadReceiver receiver = new DownloadReceiver();
    private DownMusicLrc mDownService;
    private DownloadService conn = new DownloadService();

    // 从activity中获取的变量
    private FrameLayout.LayoutParams contentParams;
    private View lyricView;
    private int screenHeight;

    // 底部栏控件
    private View bottomBar;
    private ImageView mImgPlay;
    private ImageView mImgPrevious;
    private ImageView mImgNext;
    private int loopMode;
    private ImageView mImgLoop;
    private ImageView mImgMenu;

    // 播放列表部分
    private ListView mPlayList;
    public LrcListAdapter adapter;

    // 歌词main部分
    private FrameLayout mLayoutMain;
    private TextView mMusicTitle;
    private TextView mMusicArtist;
    private Button mBtnCollect;
    private Button mBtnShared;
    private ImageView maskImg;
    // 歌词lyric部分
    private LrcView lrcView;
    private LyricHelper lyricHelper = new LyricHelper();

    private int[] loopModes = { R.drawable.bt_playing_mode_singlecycle,
            R.drawable.bt_playing_mode_order, R.drawable.bt_playing_mode_cycle,
            R.drawable.bt_playing_mode_shuffle };
    private String[] loopModeStr = { "单曲播放", "单曲循环", "列表播放", "随机播放" };

    @Override
    public View setView(LayoutInflater inflater, ViewGroup container,
            Bundle bundle) {
        View view = inflater.inflate(R.layout.frag_lyric, container, false);
        return view;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.RECEIVER_DOWNLOAD_XML);
        filter.addAction(Config.RECEIVER_DOWNLOAD_LYRIC);
        getActivity().registerReceiver(receiver, filter);
        Intent downService = new Intent(getActivity(), DownMusicLrc.class);
        getActivity().bindService(downService, conn, Context.BIND_AUTO_CREATE);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(receiver);
        getActivity().unbindService(conn);
    }

    @Override
    public void initWidget(View parentView) {
        // 初始化从activity中获取的变量
        contentParams = ((Main) getActivity()).contentParams;
        lyricView = ((Main) getActivity()).lyricView;
        screenHeight = ((Main) getActivity()).screenHeight;
        initScrollLayout(parentView);
        ((Main) getActivity()).getResideMenu().addIgnoredView(mScrollLayout);
        initSeekBar(parentView);
        initBottomBar(parentView);
        initPlayList(parentView);
        initLrcMainView(parentView);
        initLrcView(parentView);

        mBtnBack = (Button) parentView.findViewById(R.id.lrc_btn_back);
        mBtnBack.setOnClickListener(this);
        mCboxWordImg = (CheckBox) parentView
                .findViewById(R.id.lrc_cbox_wordimg);
        mCboxWordImg.setOnClickListener(this);
    }

    private void initLrcView(View parentView) {
        lrcView = (LrcView) parentView.findViewById(R.id.lyric_pager_lrcView);
        lrcView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // 如果没有歌词
                if (Config.LRC_TEXT.equals(lrcView.getLoadingTipText())) {
                    Music music = mPlayer.getList().get(
                            mPlayer.getListPosition());
                    Intent downIntent = new Intent(getActivity(),
                            DownMusicInfo.class);
                    if (music.getlrcId() != null
                            && music.getlrcId().length() > 2) {
                        // 如果歌曲信息中已经有路径，则直接下载
                        AppLog.kymjs("歌词fragment->165行--------"
                                + music.getLrcUrl());
                        mDownService.downLrc(music);
                    } else {
                        downIntent.putExtra("music", (Serializable) music);
                        getActivity().startService(downIntent);
                    }
                }
            }
        });
        Music music = mPlayer.getMusic();
        lrcView.setLrc(lyricHelper.resolve(music));
    }

    /**
     * 初始化歌词界面中心部分
     */
    private void initLrcMainView(View parentView) {
        mLayoutMain = (FrameLayout) parentView.findViewById(R.id.lrc_layout);
        mLayoutMain.setOnTouchListener(new LrcTouchlisener());
        mMusicTitle = (TextView) parentView.findViewById(R.id.lrc_main_title);
        mMusicTitle.setText(mPlayer.getMusic().getTitle());
        mMusicArtist = (TextView) parentView.findViewById(R.id.lrc_main_artist);
        mMusicArtist.setText(mPlayer.getMusic().getArtist());
        mBtnCollect = (Button) parentView.findViewById(R.id.lrc_main_collect);
        mBtnCollect.setBackgroundResource(getBtnCollectBg(mPlayer.getMusic()
                .getCollect() != 0));
        mBtnShared = (Button) parentView.findViewById(R.id.lrc_main_share);
        mBtnCollect.setOnClickListener(this);
        mBtnShared.setOnClickListener(this);
    }

    /**
     * 初始化歌词界面底部滑动条
     */
    private void initSeekBar(View parentView) {
        mSeekHandle.post(mSeekThread);
        mSeekBarMusic = (SeekBar) parentView
                .findViewById(R.id.lrc_seekbar_music);
        mSeekBarMusic.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mPlayer.seekTo(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mPlayer.seekTo(seekBar.getProgress());
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
            }
        });
    }

    /**
     * SeekBar的控制器，随歌曲播放改变
     */
    class SeekThread implements Runnable {
        @Override
        public void run() {
            Message msg = Message.obtain();
            msg.arg1 = mPlayer.getDuration(); // 最大值
            msg.arg2 = mPlayer.getCurrentPosition(); // 进度
            mSeekHandle.sendMessage(msg);
        }
    }

    @SuppressLint("HandlerLeak")
    class SeekHandle extends Handler {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mSeekBarMusic.setMax(msg.arg1);
            mSeekBarMusic.setProgress(msg.arg2);
            lrcView.seekLrcToTime(msg.arg2);
            // mTextProg.setText(StringUtils.timeFormat(msg.arg2));
            // mTextTotal.setText(StringUtils.timeFormat(msg.arg1));
            mSeekHandle.post(mSeekThread);
        }
    }

    /**
     * 初始化歌词界面底部栏
     */
    private void initBottomBar(View parentView) {
        bottomBar = parentView.findViewById(R.id.lrc_bottom);
        bottomBar.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mImgPlay = (ImageView) parentView.findViewById(R.id.lrc_btn_play);
        mImgPlay.setImageResource(getBtnPlayBg());
        mImgPrevious = (ImageView) parentView.findViewById(R.id.lrc_btn_prev);
        mImgNext = (ImageView) parentView.findViewById(R.id.lrc_btn_next);
        mImgPlay.setOnClickListener(this);
        mImgPrevious.setOnClickListener(this);
        mImgNext.setOnClickListener(this);
        mImgLoop = (ImageView) parentView.findViewById(R.id.lrc_btn_loop);
        mImgLoop.setImageResource(getImgLoopBg());
        mPlayer.setMode(loopMode);
        mImgLoop.setOnClickListener(this);
        mImgMenu = (ImageView) parentView.findViewById(R.id.lrc_btn_menu);
        mImgMenu.setOnClickListener(this);
    }

    /**
     * 初始化播放列表控件
     */
    private void initPlayList(View parentView) {
        mPlayList = (ListView) parentView.findViewById(R.id.lrc_pager_list);
        adapter = new LrcListAdapter(getActivity());
        mPlayList.setAdapter(adapter);
        mPlayList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                ((Main) getActivity()).mPlayersService.play(mPlayer.getList(),
                        position);
            }
        });
    }

    /**
     * 初始化主体界面
     */
    private void initScrollLayout(View parentView) {
        int defScreen = 1; // 默认显示第几屏
        mScrollLayout = (TabLayout) parentView.findViewById(R.id.lrc_tablayout);
        mScrollLayout.setToScreen(defScreen);
        int mScrollChildCount = mScrollLayout.getChildCount();
        final RadioGroup circles = (RadioGroup) parentView
                .findViewById(R.id.lrc_circle_layout);
        for (int i = 0; i < mScrollChildCount; i++) {
            circles.addView(getCircles());
        }
        ((RadioButton) circles.getChildAt(defScreen)).setChecked(true);
        mScrollLayout.SetOnViewChangeListener(new OnViewChangeListener() {
            @Override
            public void OnViewChange(int view) {
                RadioButton circle = (RadioButton) circles.getChildAt(view);
                circle.setChecked(true);
                if (view == 1) {
                    mCboxWordImg.setChecked(true);
                } else if (view == 2) {
                    mCboxWordImg.setChecked(false);
                }
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
        switch (parentView.getId()) {
        case R.id.lrc_main_share:
            UIHelper.toast("点击");
            break;
        case R.id.lrc_main_collect:
            Music music = mPlayer.getMusic();
            music.setCollect((music.getCollect() + 1) % 2);
            mBtnCollect.setBackgroundResource(getBtnCollectBg(music
                    .getCollect() != 0));
            db.update(music, "id = '" + music.getId() + "'");
            Config.changeCollectInfo = true;
            Config.changeMusicInfo = true;
            getActivity().sendBroadcast(
                    new Intent(Config.RECEIVER_MUSIC_SCAN_SUCCESS));
            break;
        case R.id.lrc_btn_back:
            ((Main) getActivity()).wantScroll((Main) getActivity());
            break;
        case R.id.lrc_cbox_wordimg:
            if (mCboxWordImg.isChecked()) {
                mScrollLayout.scrollToScreen(1);
            } else {
                mScrollLayout.scrollToScreen(2);
            }
            break;
        case R.id.lrc_btn_play:
            if (mPlayer.getPlaying() == Config.PLAYING_STOP) {
                ((Main) getActivity()).mPlayersService.play();
            } else if (mPlayer.getPlaying() == Config.PLAYING_PLAY) {
                ((Main) getActivity()).mPlayersService.pause();
            } else {
                ((Main) getActivity()).mPlayersService.replay();
            }
            mImgPlay.setImageResource(getBtnPlayBg());
            break;
        case R.id.lrc_btn_prev:
            ((Main) getActivity()).mPlayersService.previous();
            break;
        case R.id.lrc_btn_next:
            ((Main) getActivity()).mPlayersService.next();
            break;
        case R.id.lrc_btn_loop:
            PreferenceHelper.write(getActivity(), Config.LOOP_MODE_FILE,
                    Config.LOOP_MODE_KEY, loopMode = (loopMode + 1) % 4);
            mPlayer.setMode(loopMode);
            mImgLoop.setImageResource(getImgLoopBg());
            UIHelper.toast(loopModeStr[loopMode]);
            break;
        case R.id.lrc_btn_menu:
            UIHelper.toast("暂时不知道这里应该放什么");
            break;
        }
    }

    /**
     * 刷新歌词界面
     */
    public void refreshLrcView() {
        if (adapter != null) {
            adapter.refreshLrcAdapter();
        }
        mMusicTitle.setText(mPlayer.getMusic().getTitle());
        mMusicArtist.setText(mPlayer.getMusic().getArtist());
        mBtnCollect.setBackgroundResource(getBtnCollectBg(mPlayer.getMusic()
                .getCollect() != 0));
        mImgPlay.setImageResource(getBtnPlayBg());
        lrcView.setLrc(lyricHelper.resolve(mPlayer.getMusic()));
    }

    // 获取收藏按钮背景
    private int getBtnCollectBg(boolean isCollect) {
        return isCollect ? R.drawable.selector_lrc_collected
                : R.drawable.selector_lrc_collect;
    }

    // 获取播放按钮背景
    private int getBtnPlayBg() {
        int background = 0;
        if (mPlayer.getPlaying() == Config.PLAYING_PLAY) {
            background = R.drawable.selector_radio_pause;
        } else {
            background = R.drawable.selector_radio_play;
        }
        return background;
    }

    // 获取循环播放控件背景
    private int getImgLoopBg() {
        loopMode = PreferenceHelper.readInt(getActivity(),
                Config.LOOP_MODE_FILE, Config.LOOP_MODE_KEY,
                Config.MODE_REPEAT_ALL);
        return loopModes[loopMode];
    }

    class DownloadService implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mDownService = ((DownMusicLrc.DownLrcHolder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            UIHelper.toast("歌词下载异常，请重试");
        }
    }

    class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Config.RECEIVER_DOWNLOAD_XML.equals(intent.getAction())) {
                Music music = (Music) intent.getSerializableExtra("music");
                AppLog.kymjs("--------" + (music == null) + "-------"
                        + (mDownService == null));
                mDownService.downLrc(music);
            } else {
                // 下载完成，更新控件显示
                lrcView.setLrc(new LyricHelper().resolve(mPlayer.getMusic()));
            }
        }
    }

    //
    // /***********************************************************************
    // *
    // * 仿百度音乐歌词界面滑动后黑色过滤蒙版效果
    // *
    // ***********************************************************************/
    // private void sleep(long millis) {
    // try {
    // Thread.sleep(millis);
    // } catch (InterruptedException e) {
    // e.printStackTrace();
    // }
    // }
    //
    // class Mask extends AsyncTask<Integer, Integer, Void> {
    // private Integer startAlpha;
    // private Integer endAlpha;
    //
    // @Override
    // protected Void doInBackground(Integer... params) {
    // while (true) {
    // startAlpha = params[0];
    // endAlpha = params[0];
    // if (startAlpha > endAlpha) {
    // startAlpha -= 0x25;
    // if (startAlpha < endAlpha) {
    // break;
    // }
    // } else {
    // startAlpha += 0x25;
    // if (startAlpha > endAlpha) {
    // break;
    // }
    // }
    // publishProgress(startAlpha);
    // sleep(100);
    // }
    // return null;
    // }
    //
    // @Override
    // protected void onProgressUpdate(Integer... values) {
    // super.onProgressUpdate(values);
    // maskImg.setAlpha(values[0]);
    // }
    // }

    /***********************************************************************
     * 
     * 歌词界面主要部分的点击事件（用于手势下拉）
     * 
     ***********************************************************************/

    /** 用于计算手指滑动的速度。 */
    private VelocityTracker mVelocityTracker;

    /** 滚动显示和隐藏lrc时，手指滑动需要达到的速度。 */
    public static final int SNAP_VELOCITY = 200;

    /** 此时Y坐标 */
    float yDown = 0, yMove = 0, yUp = 0;

    class LrcTouchlisener implements OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mScrollLayout.getCurScreen() == 1) {
                createVelocityTracker(event);
                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 手指按下时记录此时Y坐标
                    yDown = event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    // 手指移动时，对比按下时的坐标，计算出移动的距离，来调整menu的leftMargin值，从而显示和隐藏menu
                    yMove = event.getRawY();
                    int distanceY = (int) (yMove - yDown);
                    contentParams.topMargin = distanceY;
                    lyricView.setLayoutParams(contentParams);
                    break;
                case MotionEvent.ACTION_UP:
                    yUp = event.getRawY();
                    changeMenuState();
                    recycleVelocityTracker();
                    break;
                }
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * 是否应该打开
     */
    private void changeMenuState() {
        float y = yUp - yDown;
        AppLog.kymjs("y=" + y + "-----yup=" + yUp + "-------yDown=" + yDown
                + "-----歌词界面状态" + ((Main) getActivity()).isOpen);
        if ((y > screenHeight / 2 && getScrollVelocity() > 0)
                || getScrollVelocity() > SNAP_VELOCITY) {
            // 关闭的（歌词界面正在呈现即将转入content界面）
            ((Main) getActivity()).isOpen = true;
        } else {
            // 打开的（内容界面正在呈现即将转入lyric界面）
            ((Main) getActivity()).isOpen = false;
        }
        ((Main) getActivity()).wantScroll((Main) getActivity());
    }

    /**
     * 获取手指在content界面滑动的速度。
     * 
     * @return 滑动速度，以每秒钟移动了多少像素值为单位。
     */
    private int getScrollVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000);
        int velocity = (int) mVelocityTracker.getYVelocity();
        // return Math.abs(velocity);
        return velocity;
    }

    /**
     * 创建VelocityTracker对象，并将触摸content界面的滑动事件加入到VelocityTracker当中。
     */
    private void createVelocityTracker(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    /**
     * 回收VelocityTracker对象。
     */
    private void recycleVelocityTracker() {
        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }
}
