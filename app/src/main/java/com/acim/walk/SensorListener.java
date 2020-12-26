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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

public class SensorListener extends Service implements SensorEventListener2 {

    private final String TAG = "SensorListenerClass";

    private final static long MICROSECONDS_IN_ONE_MINUTE = 60000000;
    private final static int SAVE_OFFSET_STEPS = 1;
    private final static long SAVE_OFFSET_TIME = 30000;
    public final static String NOTIFICATION_CHANNEL_ID = "Notification";
    public final static int NOTIFICATION_ID = 1;
    private static int sensorSteps;
    private static long lastSaveTime;

    private final BroadcastReceiver shutdownReceiver = new ShutdownRecevier();
    private static FirebaseFirestore dbContext = FirebaseFirestore.getInstance();
    private static FirebaseAuth mAuth = FirebaseAuth.getInstance();


    //Prove
    private static int currentMatchSteps;
    private static boolean startedOnce = false;
    private static boolean userInGame = false; // Boolean variable to control the repeating timer.

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
        Log.i(TAG, "onSensorChanged");
        if (event.values[0] > Integer.MAX_VALUE) {
            if (BuildConfig.DEBUG)
                Log.i(TAG + " - onSensorChanged", "probably not a real value: " + event.values[0]);
            return;
        } else {
            sensorSteps = (int) event.values[0];

            SharedPreferences prefs = getApplicationContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
            int startMatchSteps = prefs.getInt("matchStartedAtSteps", 0);
            // -The efficient way of starting a new step counting sequence.-
            /*if (stepCounter == 0) { // If the stepCounter is in its initial value, then...
                stepCounter = (int) event.values[0]; // Assign the StepCounter Sensor event value to it.
                Log.d(TAG, String.format("Entra IF"));
            }*/
            currentMatchSteps = sensorSteps - startMatchSteps; // By subtracting the stepCounter variable from the Sensor event value - We start a new counting sequence from 0. Where the Sensor event value will increase, and stepCounter value will be only initialised once.
            Log.i(TAG, String.format("current steps: %d", currentMatchSteps));
            Log.i(TAG, String.format("start match steps: %d", startMatchSteps));
            prefs.edit().putInt("savedSteps", currentMatchSteps);
            updateIfNecessary();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private boolean updateIfNecessary() {
        Log.i(TAG, String.format("updateIfNecessary -> sensorSteps: %d", sensorSteps));
        Log.i(TAG, String.format("updateIfNecessary -> currentMatchSteps: %d", currentMatchSteps));
        if (currentMatchSteps > 0 && System.currentTimeMillis() > lastSaveTime + SAVE_OFFSET_TIME) {

            DocumentReference userRef = dbContext.collection("users").document(mAuth.getUid());

            dbContext.runTransaction(new Transaction.Function<Void>() {
                @Nullable
                @Override
                public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                    DocumentSnapshot userDoc = transaction.get(userRef);
                    if (userDoc.exists()) {
                        String matchId = userDoc.get("matchId").toString();
                        transaction.update(userRef, "steps", currentMatchSteps);

                        DocumentReference matchRef = dbContext.collection("matches").document(matchId).collection("participants").document(mAuth.getUid());
                        transaction.update(matchRef, "steps", currentMatchSteps);
                    }

                    return null;
                }
            }).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.i(TAG, "Transaction completed successfully!");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.i(TAG, "TRANSACTION FAILED!");
                    Log.i(TAG, e.getMessage());
                }
            });

            lastSaveTime = System.currentTimeMillis();

            showNotification(); // update notification
            return true;
        }
        return false;
    }

    private void showNotification() {
        Log.i(TAG, "showNotification");
        startForeground(NOTIFICATION_ID, getNotification(this));
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        Log.i(TAG, "onStartCommand SERVICE");
        reRegisterSensor();
        registerBroadcastReceiver();
        updateIfNecessary();

        // if user is participating a match restart service every hour to save the current step count
        DocumentReference userRef = dbContext.document("users/" + mAuth.getUid());
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult().exists()) {
                    if (task.getResult().getString("matchId") != null && !task.getResult().getString("matchId").equals(""))
                        userInGame = true;


                    SharedPreferences prefs = getApplicationContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
                    currentMatchSteps = prefs.getInt("savedSteps", 0);

                    if (!startedOnce) {
                        startedOnce = true;
                        getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                                .putInt("matchStartedAtSteps", sensorSteps).apply();
                    }

                    long nextUpdate = Math.min(Util.getTomorrow(),
                            System.currentTimeMillis() + AlarmManager.INTERVAL_HALF_HOUR);
                    //if (BuildConfig.DEBUG) Logger.log("next update: " + new Date(nextUpdate).toLocaleString());
                    AlarmManager am =
                            (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                    PendingIntent pi = PendingIntent
                            .getService(getApplicationContext(), 2, new Intent(getApplicationContext(), SensorListener.class),
                                    PendingIntent.FLAG_UPDATE_CURRENT);
                    am.set(AlarmManager.RTC, nextUpdate, pi);
                    am.set(AlarmManager.RTC, AlarmManager.INTERVAL_HOUR, pi);
                    //return START_STICKY;
                }
            }
        });
        if (userInGame) {

/*            SharedPreferences prefs = getApplicationContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
            currentMatchSteps = prefs.getInt("savedSteps", 0);

            int offset = getSharedPreferences("pedometer", Context.MODE_PRIVATE).getInt("matchStartedAtSteps", 0);
            Log.i(TAG, "onStartCommand -> offset: " + offset);
            if(sensorSteps == 0 || offset != 0){
                getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                        .putInt("matchStartedAtSteps", sensorSteps).apply();
            }

            long nextUpdate = Math.min(Util.getTomorrow(),
                    System.currentTimeMillis() + AlarmManager.INTERVAL_HALF_HOUR);
            //if (BuildConfig.DEBUG) Logger.log("next update: " + new Date(nextUpdate).toLocaleString());
            AlarmManager am =
                    (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            PendingIntent pi = PendingIntent
                    .getService(getApplicationContext(), 2, new Intent(this, SensorListener.class),
                            PendingIntent.FLAG_UPDATE_CURRENT);
            am.set(AlarmManager.RTC, nextUpdate, pi);
            am.set(AlarmManager.RTC, AlarmManager.INTERVAL_HOUR, pi);*/
            return START_STICKY;
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(final Intent rootIntent) {
        Log.i(TAG, "OnTaskRemoved");
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
            Log.i(TAG, "onDestroy called");
            SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.cancel(NOTIFICATION_ID);
            startedOnce = false;
            userInGame = false;
            stopService(new Intent(this, SensorListener.class));
            getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                    .edit().putInt("savedSteps", currentMatchSteps);
            sm.unregisterListener(this);
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                e.printStackTrace();
        }
    }

    private void registerBroadcastReceiver() {
        Log.i(TAG, "registerBroadcastReceiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SHUTDOWN);
        registerReceiver(shutdownReceiver, filter);
    }

    public static Notification getNotification(final Context context) {
        Log.i("STATIC", "getNotification -> currentMatchSteps: " + currentMatchSteps);
        Notification.Builder notificationBuilder = getNotificationBuilder(context);
        notificationBuilder.setContentText(String.format("%d", (currentMatchSteps))).setContentTitle("Passi");

        notificationBuilder.setPriority(Notification.PRIORITY_MIN).setShowWhen(false)
                .setContentIntent(PendingIntent
                        .getActivity(context, 0, new Intent(context, MainActivity.class),
                                PendingIntent.FLAG_UPDATE_CURRENT))
                .setSmallIcon(R.drawable.common_full_open_on_phone).setOngoing(true);
        return notificationBuilder.build();
    }

    public static Notification.Builder getNotificationBuilder(final Context context) {
        Log.i("STATIC", "getNotificationBuilder");
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel =
                new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID,
                        NotificationManager.IMPORTANCE_NONE);
        channel.setImportance(NotificationManager.IMPORTANCE_MIN);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setBypassDnd(false);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
        Notification.Builder builder = new Notification.Builder(context, NOTIFICATION_CHANNEL_ID);
        return builder;
    }

    private void reRegisterSensor() {
        Log.i(TAG, "reRegisterSensor");
        //if (BuildConfig.DEBUG) Logger.log("re-register sensor listener");
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        try {
            sm.unregisterListener(this);
        } catch (Exception e) {
            //if (BuildConfig.DEBUG) Logger.log(e);
            e.printStackTrace();
        }

/*        if (BuildConfig.DEBUG) {
            //Logger.log("step sensors: " + sm.getSensorList(Sensor.TYPE_STEP_COUNTER).size());
            if (sm.getSensorList(Sensor.TYPE_STEP_COUNTER).size() < 1) return; // emulator
            //Logger.log("default: " + sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER).getName());
        }*/

        // enable batching with delay of max 5 min
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
                SensorManager.SENSOR_DELAY_NORMAL, (int) (6000000));
    }


}
