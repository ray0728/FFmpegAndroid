package com.frank.ffmpeg.activity;

import android.annotation.SuppressLint;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.frank.ffmpeg.FFmpegCmd;
import com.frank.ffmpeg.R;
import com.frank.ffmpeg.util.FFmpegUtil;
import com.frank.ffmpeg.util.FileUtil;

import java.io.File;

/**
 * 使用ffmpeg进行音视频合成与分离
 * Created by frank on 2018/1/23.
 */
public class MediaHandleActivity extends BaseActivity {

    private final static String TAG = MediaHandleActivity.class.getSimpleName();
    private static final String PATH = Environment.getExternalStorageDirectory().getPath();
    private String videoFile;
    private String temp = PATH + File.separator + "temp.mp4";
    private boolean isMux;
    private final static int MSG_MUX = 100;
    private final static int MSG_BEGIN = 101;
    private final static int MSG_FINISH = 102;
    private ProgressBar progressMedia;
    private int viewId;
    private LinearLayout layoutMediaHandle;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_MUX:
                    String audioFile = PATH + File.separator + "tiger.mp3";//tiger.mp3
                    String muxFile = PATH + File.separator + "media-mux.mp4";

                    try {
                        //使用MediaPlayer获取视频时长
                        MediaPlayer mediaPlayer = new MediaPlayer();
                        mediaPlayer.setDataSource(videoFile);
                        mediaPlayer.prepare();
                        //单位为ms
                        int videoDuration = mediaPlayer.getDuration()/1000;
                        Log.i(TAG, "videoDuration=" + videoDuration);
                        mediaPlayer.release();
                        //使用MediaMetadataRetriever获取音频时长
                        MediaMetadataRetriever mediaRetriever = new MediaMetadataRetriever();
                        mediaRetriever.setDataSource(audioFile);
                        //单位为ms
                        String duration = mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                        int audioDuration = (int)(Long.parseLong(duration)/1000);
                        Log.i(TAG, "audioDuration=" + audioDuration);
                        mediaRetriever.release();
                        //如果视频时长比音频长，采用音频时长，否则用视频时长
                        int mDuration = Math.min(audioDuration, videoDuration);
                        //使用纯视频与音频进行合成
                        String[] commandLine = FFmpegUtil.mediaMux(temp, audioFile, mDuration, muxFile);
                        executeFFmpegCmd(commandLine);
                        isMux = false;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case MSG_BEGIN:
                    progressMedia.setVisibility(View.VISIBLE);
                    layoutMediaHandle.setVisibility(View.GONE);
                    break;
                case MSG_FINISH:
                    progressMedia.setVisibility(View.GONE);
                    layoutMediaHandle.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    int getLayoutId() {
        return R.layout.activity_media_handle;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideActionBar();
        initView();
    }

    private void initView() {
        progressMedia = getView(R.id.progress_media);
        layoutMediaHandle = getView(R.id.layout_media_handle);
        initViewsWithClick(
                R.id.btn_mux,
                R.id.btn_extract_audio,
                R.id.btn_extract_video
        );
    }

    @Override
    public void onViewClick(View view) {
        viewId = view.getId();
        selectFile();
    }

    @Override
    void onSelectedFile(String filePath) {
        doHandleMedia(filePath);
    }

    /**
     * 调用ffmpeg处理音视频
     * @param srcFile srcFile
     */
    private void doHandleMedia(String srcFile) {
        String[] commandLine = null;
        if (!FileUtil.checkFileExist(srcFile)){
            return;
        }
        if (!FileUtil.isVideo(srcFile)) {
            showToast(getString(R.string.wrong_video_format));
            return;
        }

        switch (viewId){
            case R.id.btn_mux://音视频合成
                try {
                    //视频文件有音频,先把纯视频文件抽取出来
                    videoFile = srcFile;
                    commandLine = FFmpegUtil.extractVideo(srcFile, temp);
                    isMux = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_extract_audio://提取音频
                String extractAudio = PATH + File.separator + "extractAudio.aac";
                commandLine = FFmpegUtil.extractAudio(srcFile, extractAudio);
                break;
            case R.id.btn_extract_video://提取视频
                String extractVideo = PATH + File.separator + "extractVideo.mp4";
                commandLine = FFmpegUtil.extractVideo(srcFile, extractVideo);
                break;
            default:
                break;
        }
        executeFFmpegCmd(commandLine);
    }

    /**
     * 执行ffmpeg命令行
     * @param commandLine commandLine
     */
    private void executeFFmpegCmd(final String[] commandLine) {
        if(commandLine == null){
            return;
        }
        FFmpegCmd.execute(commandLine, new FFmpegCmd.OnHandleListener() {
            @Override
            public void onBegin() {
                Log.i(TAG, "handle media onBegin...");
                mHandler.obtainMessage(MSG_BEGIN).sendToTarget();
            }

            @Override
            public void onEnd(int result) {
                Log.i(TAG, "handle media onEnd...");
                if(isMux){
                    mHandler.obtainMessage(MSG_MUX).sendToTarget();
                }else {
                    mHandler.obtainMessage(MSG_FINISH).sendToTarget();
                }
            }
        });
    }

}
