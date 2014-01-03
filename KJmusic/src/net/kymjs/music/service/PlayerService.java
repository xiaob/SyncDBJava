package net.kymjs.music.service;

import java.util.List;

import net.kymjs.music.AppLog;
import net.kymjs.music.Config;
import net.kymjs.music.bean.Music;
import net.kymjs.music.utils.ListData;
import net.kymjs.music.utils.Player;
import net.kymjs.music.utils.UIHelper;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.TelephonyManager;

/**
 * 音乐播放的后台服务
 * 
 * @author kymjs
 * 
 */
public class PlayerService extends Service {
    private Player mPlayer = Player.getPlayer();
    private LocalPlayer localPlayer = new LocalPlayer();
    private SystemReceiver mReceiver;

    public class LocalPlayer extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        IntentFilter intentFilter = new IntentFilter();
        mReceiver = new SystemReceiver();
        intentFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        registerReceiver(mReceiver, intentFilter);
        return localPlayer;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPlayer.destroy();
        unregisterReceiver(mReceiver);
    }

    /**
     * 播放
     * 
     * @param list
     * @param position
     * @return
     */
    public Music play(List<Music> list, int position) {
        Music music = null;
        if (list == null || list.isEmpty()) {
            play();
        } else {
            music = mPlayer.play(this, list, position);
        }
        return music;
    }

    /**
     * 默认播放
     * 
     * @return
     */
    public Music play() {
        Music music = null;
        if (ListData.getLocalList(this) == null
                || ListData.getLocalList(this).isEmpty()) {
            UIHelper.toast("亲，还没有歌呢，去扫描一下吧");
        } else {
            music = mPlayer.play(this, ListData.getLocalList(this), 0);
        }
        return music;
    }

    /**
     * 暂停
     */
    public void pause() {
        mPlayer.pause();
    }

    /**
     * 正在暂停，调用后开始继续播放
     */
    public void replay() {
        mPlayer.replay();
    }

    /**
     * 下一首
     * 
     * @return
     */
    public Music next() {
        Music music = null;
        if (mPlayer.getPlaying() == Config.PLAYING_STOP
                || mPlayer.getList().isEmpty()) {
            play();
        } else {
            music = mPlayer.next(this);
            AppLog.debug("正在播放：" + music.getTitle());
        }
        return music;
    }

    /**
     * 上一首
     * 
     * @return
     */
    public Music previous() {
        Music music = null;
        if (mPlayer.getPlaying() == Config.PLAYING_STOP
                || mPlayer.getList().isEmpty()) {
            play();
        } else {
            music = mPlayer.previous(this);
            AppLog.debug("正在播放：" + music.getTitle());
        }
        return music;
    }

    /**
     * 打电话时暂停播放
     * 
     * @author kymjs
     * 
     */
    public class SystemReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 如果是打电话
            if (Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) {
                pause();
            } else {
                // 如果是来电
                TelephonyManager tm = (TelephonyManager) context
                        .getSystemService(Service.TELEPHONY_SERVICE);
                switch (tm.getCallState()) {
                // 响铃
                case TelephonyManager.CALL_STATE_RINGING:
                    pause();
                    break;
                // 摘机
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    pause();
                    break;
                // 空闲
                case TelephonyManager.CALL_STATE_IDLE:
                    replay();
                    break;
                }
            }
        }
    }
}