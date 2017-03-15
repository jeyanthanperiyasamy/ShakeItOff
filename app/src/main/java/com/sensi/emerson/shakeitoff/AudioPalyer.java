package com.sensi.emerson.shakeitoff;

import android.content.Context;
import android.media.MediaPlayer;
import android.support.annotation.RawRes;

/**
 * Created by jeyperiyasamy on 3/14/17.
 */

public class AudioPalyer {

    private MediaPlayer mediaPlayer;

    public void init(Context context, @RawRes int resid) {
         mediaPlayer = MediaPlayer.create(context,resid);

    }

    public void play() {
       if(!mediaPlayer.isPlaying()) {
           mediaPlayer.start();
       }
    }

    public void pause() {
        if(mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void destroy() {
        if(mediaPlayer != null) {
            if(mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
