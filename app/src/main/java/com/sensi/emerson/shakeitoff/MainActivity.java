package com.sensi.emerson.shakeitoff;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.common.collect.EvictingQueue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class MainActivity extends AppCompatActivity {

    private AudioPalyer audioPlayer;
    SensorManager sensorManager;

    BehaviorSubject<Float> xValue = BehaviorSubject.create();
    BehaviorSubject<Float> yValue = BehaviorSubject.create();
    BehaviorSubject<Float> zValue = BehaviorSubject.create();
    Disposable disposable;
    public static final int EVENT_COUNT = 10;
    public static final int MUSIC_THRESHOLD = 15 * 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioPlayer = new AudioPalyer();
        audioPlayer.init(this,R.raw.gameof);


        findViewById(R.id.play_button).setOnClickListener(v -> audioPlayer.play());

        findViewById(R.id.pause_button).setOnClickListener(v -> audioPlayer.pause());

        SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                xValue.onNext(event.values[0]);
                yValue.onNext(event.values[1]);
                zValue.onNext(event.values[2]);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(sensorEventListener,accelerometer,SensorManager.SENSOR_DELAY_NORMAL);


    }

    @Override
    protected void onStart() {
        super.onStart();



        //Combine the latest x, y, and z sensor values and emit the combination as a list of Floats.
        disposable = Observable.combineLatest(xValue, yValue, zValue, (x, y, z) -> Arrays.asList(x, y, z))
                //Sample the latest event every 20 seconds to limit backpressure
                .sample(20, TimeUnit.MILLISECONDS)
                //From here on go to the computation thread
                .subscribeOn(Schedulers.computation())
                //Turn the list of Floats into a Magnitude
                .map(floats -> {
                    float magnitude = 0;
                    for(Float aFloat : floats){
                        magnitude += aFloat * aFloat;
                    }
                    return magnitude;
                })
                //Accumulate the last 10 magnitude events in an Evicting Queue (you can write one if you are against Guava)
                .scan(EvictingQueue.create(EVENT_COUNT), (BiFunction<EvictingQueue<Float>, Float, EvictingQueue<Float>>) (objects, aFloat) -> {
                    objects.add(aFloat);
                    return objects;
                })
                //Take the last accumulated events and calculate an average
                .map(floats -> {
                    float sum = 0;
                    for(Float foo : floats){
                        sum += foo;
                    }
                    return sum/ EVENT_COUNT;
                })
                //Move onto a new thread to interact with the music player
                .observeOn(Schedulers.newThread())
                .subscribe(weightedMagnitude -> {
                    Log.e("Music", "weighted magnitude was " + weightedMagnitude);
                    if (weightedMagnitude != null && weightedMagnitude > MUSIC_THRESHOLD) {
                        audioPlayer.play();
                    } else {
                        audioPlayer.pause();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioPlayer.destroy();

        if(disposable!=null && !disposable.isDisposed()) {
           disposable.dispose();
        }

    }
}
