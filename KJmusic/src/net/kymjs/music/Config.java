package net.kymjs.music;

public class Config {
    /** 当前版本 */
    public static final double VEWSION = 1.0;

    /** Default display */
    public static final String TITLE = "KJ音乐";
    public static final String ARTIST = "kymJS";

    /** 数据库名字 */
    public static final String DB_NAME = "kymjsmusicdb";
    /** 是否启动调试模式 */
    public static final boolean isDebug = true;

    /** 播放列表循环模式 */
    public static final int MODE_REPEAT_SINGLE = 0;
    public static final int MODE_REPEAT_ALL = 1;
    public static final int MODE_SEQUENCE = 2;
    public static final int MODE_RANDOM = 3;

    /** 播放器状态 */
    public static final int PLAYING_STOP = 0;
    public static final int PLAYING_PAUSE = 1;
    public static final int PLAYING_PLAY = 2;

    /** 音乐改变的广播 */
    public static final String RECEIVER_MUSIC_CHANGE = "com.kymjs.music.music_change";

    /** 歌曲扫描完成广播 */
    public static final String RECEIVER_MUSIC_SCAN_SUCCESS = "com.kymjs.music.music_scan_success";
    public static final String RECEIVER_MUSIC_SCAN_FAIL = "com.kymjs.music.music_scan_fail";
    /** 音乐列表信息被改变 */
    public static boolean changeMusicInfo = false;
    public static boolean changeCollectInfo = false;
}