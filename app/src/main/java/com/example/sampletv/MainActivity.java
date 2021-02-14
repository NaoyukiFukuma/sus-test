package com.example.sampletv;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.tv.TvContract;
import android.media.tv.TvView;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "SampleTV";
    private static final String TERRESTRIAL_INPUT_ID =
            "com.sony.dtv.tvinput.tuner/.JpTerrTunerInputService/HW0";
    private TvView mTvView;
    private List<Uri> mChannelIdList = new ArrayList<>();
    private int mCurrentChannelIndex = 0;
    private AudioManager mAudioManager;
    private AudioFocusRequest mAudioFocusRequest;
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.i(TAG, "onAudioFocusChange: " + focusChange);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        mTvView = findViewById(R.id.tv_view);
        mChannelIdList = getChannelIdList();
        if (mChannelIdList == null) {
            Toast toast = Toast.makeText(getApplicationContext(),
                    R.string.error_tune_message, Toast.LENGTH_LONG);
            toast.show();
            finish();
            return;
        } else {
            tune(mCurrentChannelIndex);
        }
        mTvView.setCallback(new TvView.TvInputCallback() {
            @Override
            public void onVideoAvailable(String inputId) {
                super.onVideoAvailable(inputId);
                Log.i(TAG, "onVideoAvailable: " + inputId);
            }

            @Override
            public void onVideoUnavailable(String inputId, int reason) {
                super.onVideoUnavailable(inputId, reason);
                Log.i(TAG, "onVideoUnavailable: " + inputId);
            }

            @Override
            public void onContentAllowed(String inputId) {
                super.onContentAllowed(inputId);
                Log.i(TAG, "onContentAllowed: " + inputId);
            }
        });

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build();
        mAudioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(mOnAudioFocusChangeListener)
                .build();
        mAudioManager.requestAudioFocus(mAudioFocusRequest);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
      Log.d(TAG, "onKeyDown: " + keyCode + ", " + event);
      if (keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
          tune(mCurrentChannelIndex + 1);
          return true;
      } else if (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
          tune(mCurrentChannelIndex - 1);
          return true;
      }
      return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        mTvView.reset();
        mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    private List<Uri> getChannelIdList() {
        List<Uri> channelIdList = new ArrayList<>();
        try (Cursor cursor = getContentResolver().query(TvContract.Channels.CONTENT_URI,
                new String[]{TvContract.Channels.COLUMN_INPUT_ID, TvContract.Channels._ID},
                null, null, null)){
            if (cursor == null || !cursor.moveToFirst()) {
                Log.e(TAG, "cursor is null");
                return null;
            }
            while (cursor.moveToNext()) {
                String inputId = cursor.getString(0);
                int channelId = cursor.getInt(1);
                Log.v(TAG, "inputId: " + inputId +  ", channelId: " + channelId);
                if (TERRESTRIAL_INPUT_ID.equals(inputId)) {
                    channelIdList.add(TvContract.buildChannelUri(channelId));
                }
            }
        }
        return channelIdList;
    }

    private void tune(int index) {
        if (index > mChannelIdList.size() - 1) {
            index = 0;
        } else if (index < 0) {
            index = mChannelIdList.size() - 1;
        }
        Log.i(TAG, "tune: " + mChannelIdList.get(index));
        mTvView.tune(TERRESTRIAL_INPUT_ID, mChannelIdList.get(index));
        mCurrentChannelIndex = index;
    }
}