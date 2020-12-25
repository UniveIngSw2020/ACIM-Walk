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
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

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
    private static FirebaseFirestore dbContext = FirebaseFirestore.getInstance();
    private static FirebaseAuth mAuth = FirebaseAuth.getInstance();


    //Prove
    private final Handler handler = new Handler();
    int stepCounter;
    static int newStepCounter;
    boolean userInGame; // Boolean variable to control the repeating timer.

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
        stepCounter = 0;
        newStepCounter = 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.values[0] > Integer.MAX_VALUE) {
            if (BuildConfig.DEBUG)
                Log.i(TAG + " - onSensorChanged", "probably not a real value: " + event.values[0]);
            return;
        } else {
            Log.d(TAG, String.format("steps - sensor: %d", steps));
            steps = (int) event.values[0];


            int countSteps = (int) event.values[0];

            SharedPreferences prefs = getApplicationContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
            int savedSteps = prefs.getInt("savedSteps", 0);
            // -The efficient way of starting a new step counting sequence.-
            /*if (stepCounter == 0) { // If the stepCounter is in its initial value, then...
                stepCounter = (int) event.values[0]; // Assign the StepCounter Sensor event value to it.
                Log.d(TAG, String.format("Entra IF"));
            }*/
            newStepCounter = countSteps - savedSteps; // By subtracting the stepCounter variable from the Sensor event value - We start a new counting sequence from 0. Where the Sensor event value will increase, and stepCounter value will be only initialised once.
            Log.d(TAG, String.format("NewStepCounter: %d", newStepCounter));
            prefs.edit().putInt("savedSteps", newStepCounter);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private boolean updateIfNecessary() {
        Log.d(TAG, String.format("steps: %d", steps));
        Log.d(TAG, String.format("lastSaveSteps: %d", lastSaveSteps));
        if (steps > lastSaveSteps + SAVE_OFFSET_STEPS ||
                (steps > 0 && System.currentTimeMillis() > lastSaveTime + SAVE_OFFSET_TIME)) {

            DocumentReference userRef = dbContext.collection("users").document(mAuth.getUid());

            dbContext.runTransaction(new Transaction.Function<Void>() {
                @Nullable
                @Override
                public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                    DocumentSnapshot userDoc = transaction.get(userRef);
                    if (userDoc.exists()) {
                        String matchId = userDoc.get("matchId").toString();
                        transaction.update(userRef, "steps", newStepCounter);

                        DocumentReference matchRef = dbContext.collection("matches").document(matchId).collection("participants").document(mAuth.getUid());
                        transaction.update(matchRef, "steps", newStepCounter);
                    }

                    int pauseDifference = steps -
                            getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                                    .getInt("pauseCount", newStepCounter);
                    if (pauseDifference > 0) {
                        // update pauseCount for the new day
                        getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                                .putInt("pauseCount", newStepCounter).apply();
                    }

                    return null;
                }
            }).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "Transaction completed successfully!");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "TRANSACTION FAILED!");
                    Log.d(TAG, e.getMessage());
                }
            });

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


        // if user is participating a match restart service every hour to save the current step count

        DocumentReference userRef = dbContext.document("users/" + mAuth.getUid());
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult().exists()) {
                    userInGame = true;
                }
            }
        });
        if (userInGame) {
            long nextUpdate = Math.min(Util.getTomorrow(),
                    System.currentTimeMillis() + AlarmManager.INTERVAL_HALF_HOUR);
            //if (BuildConfig.DEBUG) Logger.log("next update: " + new Date(nextUpdate).toLocaleString());
            AlarmManager am =
                    (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            PendingIntent pi = PendingIntent
                    .getService(getApplicationContext(), 2, new Intent(this, SensorListener.class),
                            PendingIntent.FLAG_UPDATE_CURRENT);
            am.set(AlarmManager.RTC, nextUpdate, pi);
            am.set(AlarmManager.RTC, AlarmManager.INTERVAL_HOUR, pi);
            return START_STICKY;
        }
        return START_NOT_STICKY;
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
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.cancel(0);

            Log.d(TAG, "onDestroy called");
            getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                    .getInt("pauseCount", 0);
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


/*        Database db = Database.getInstance(context);
        int today_offset = db.getSteps(Util.getToday());
        if (steps == 0)
            steps = db.getCurrentSteps(); // use saved value if we haven't anything better
        db.close();*/

        //TODO: check if the value of the step counter is correct
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        DocumentReference userRef = db.collection("users").document(mAuth.getUid());
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult() != null) {
                        if (task.getResult().getLong("steps") != null) {
                            steps += task.getResult().getLong("steps").intValue();
                        }
                    }
                }
            }
        });


        Notification.Builder notificationBuilder = getNotificationBuilder(context);
        if (steps > 0) {
            notificationBuilder.setContentText(String.format("%d", (newStepCounter)))
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
                SensorManager.SENSOR_DELAY_NORMAL, (int) (MICROSECONDS_IN_ONE_MINUTE));
    }


}
