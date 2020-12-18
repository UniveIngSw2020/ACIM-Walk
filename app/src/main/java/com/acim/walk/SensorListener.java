package com.acim.walk;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

public class SensorListener extends Service implements SensorEventListener2 {

    private final String TAG = "SensorListenerClass";
    private final static long MICROSECONDS_IN_ONE_MINUTE = 60000000;
    private final static int SAVE_OFFSET_STEPS = 1;
    private final static long SAVE_OFFSET_TIME = 30000;
    public final static String NOTIFICATION_CHANNEL_ID = "Notification";
    public final static int NOTIFICATION_ID = 1;
    private static int steps;
    private static int lastSaveSteps;
    private static long lastSaveTime;

    private final BroadcastReceiver shutdownReceiver = new ShutdownRecevier();
    private FirebaseFirestore dbContext = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "SensorListener onCreate");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.values[0] > Integer.MAX_VALUE) {
            if (BuildConfig.DEBUG) Log.i(TAG + " - onSensorChanged","probably not a real value: " + event.values[0]);
            return;
        } else {
            steps = (int) event.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private boolean updateIfNecessary() {
        if (steps > lastSaveSteps + SAVE_OFFSET_STEPS ||
                (steps > 0 && System.currentTimeMillis() > lastSaveTime + SAVE_OFFSET_TIME)) {

            /*
            Database db = Database.getInstance(this);
            if (db.getSteps(Util.getToday()) == Integer.MIN_VALUE) {
                int pauseDifference = steps -
                        getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                                .getInt("pauseCount", steps);
                db.insertNewDay(Util.getToday(), steps - pauseDifference);
                if (pauseDifference > 0) {
                    // update pauseCount for the new day
                    getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                            .putInt("pauseCount", steps).apply();
                }
            }
            db.saveCurrentSteps(steps);
            db.close();
            lastSaveSteps = steps;
            lastSaveTime = System.currentTimeMillis();
            */

            DocumentReference userRef = dbContext.collection("users").document(mAuth.getUid());
            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        if(task.getResult() != null){
                            if(task.getResult().getLong("steps") != null){
                                if(task.getResult().getLong("steps").intValue() == Integer.MIN_VALUE){
                                    int pauseDifference = steps -
                                            getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                                                    .getInt("pauseCount", steps);
                                    if (pauseDifference > 0) {
                                        // update pauseCount for the new day
                                        getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                                                .putInt("pauseCount", steps).apply();
                                    }
                                }
                            }
                        }
                    }
                }
            });
            userRef.update("steps", steps);
            lastSaveSteps = steps;
            lastSaveTime = System.currentTimeMillis();

            showNotification(); // update notification
            return true;
        } else {
            return false;
        }
    }

    private void showNotification() {
            startForeground(NOTIFICATION_ID, getNotification(this));
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand SERVICE");
        reRegisterSensor();
        registerBroadcastReceiver();
        if (!updateIfNecessary()) {
            showNotification();
        }

        // restart service every hour to save the current step count
        long nextUpdate = Math.min(Util.getTomorrow(),
                System.currentTimeMillis() + AlarmManager.INTERVAL_HALF_HOUR);
        //if (BuildConfig.DEBUG) Logger.log("next update: " + new Date(nextUpdate).toLocaleString());
        AlarmManager am =
                (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent
                .getService(getApplicationContext(), 2, new Intent(this, SensorListener.class),
                        PendingIntent.FLAG_UPDATE_CURRENT);
        am.set(AlarmManager.RTC, nextUpdate, pi);
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(final Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Restart service in 500 ms
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                .set(AlarmManager.RTC, System.currentTimeMillis() + 500, PendingIntent
                        .getService(this, 3, new Intent(this, SensorListener.class), 0));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
            sm.unregisterListener(this);
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
            e.printStackTrace();
        }
    }

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SHUTDOWN);
        registerReceiver(shutdownReceiver, filter);
    }

    public static Notification getNotification(final Context context) {
        SharedPreferences prefs = context.getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        Database db = Database.getInstance(context);
        int today_offset = db.getSteps(Util.getToday());
        if (steps == 0)
            steps = db.getCurrentSteps(); // use saved value if we haven't anything better
        db.close();
        Notification.Builder notificationBuilder = getNotificationBuilder(context);
        if (steps > 0) {
            notificationBuilder.setContentText(String.format("%d", (today_offset + steps)) )
                    .setContentTitle("Passi");
        }
        notificationBuilder.setPriority(Notification.PRIORITY_MIN).setShowWhen(false)
                .setContentIntent(PendingIntent
                        .getActivity(context, 0, new Intent(context, MainActivity.class),
                                PendingIntent.FLAG_UPDATE_CURRENT))
                .setSmallIcon(R.drawable.common_full_open_on_phone).setOngoing(true);
        return notificationBuilder.build();
    }

    public static Notification.Builder getNotificationBuilder(final Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel =
                new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID,
                        NotificationManager.IMPORTANCE_NONE);
        channel.setImportance(NotificationManager.IMPORTANCE_MIN); // ignored by Android O ...
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setBypassDnd(false);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
        Notification.Builder builder = new Notification.Builder(context, NOTIFICATION_CHANNEL_ID);
        return builder;
    }

    private void reRegisterSensor() {
        //if (BuildConfig.DEBUG) Logger.log("re-register sensor listener");
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        try {
            sm.unregisterListener(this);
        } catch (Exception e) {
            //if (BuildConfig.DEBUG) Logger.log(e);
            e.printStackTrace();
        }

        if (BuildConfig.DEBUG) {
            //Logger.log("step sensors: " + sm.getSensorList(Sensor.TYPE_STEP_COUNTER).size());
            if (sm.getSensorList(Sensor.TYPE_STEP_COUNTER).size() < 1) return; // emulator
            //Logger.log("default: " + sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER).getName());
        }

        // enable batching with delay of max 5 min
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
                SensorManager.SENSOR_DELAY_NORMAL, (int) (5 * MICROSECONDS_IN_ONE_MINUTE));
    }
}
