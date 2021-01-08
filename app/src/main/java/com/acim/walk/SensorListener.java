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

import static androidx.lifecycle.Lifecycle.State.STARTED;

public class SensorListener extends Service implements SensorEventListener2 {

    //useful to understand where log messages come from
    private final String TAG = "SensorListenerClass";

    private final static long MICROSECONDS_IN_ONE_MINUTE = 60000000;
    private final static int SAVE_OFFSET_STEPS = 1;


    private final static long SAVE_OFFSET_TIME = 30000;
    public final static String NOTIFICATION_CHANNEL_ID = "Notification";
    public final static int NOTIFICATION_ID = 1; //notification id
    private static int sensorSteps;
    private static long lastSaveTime;

    private final BroadcastReceiver shutdownReceiver = new ShutdownRecevier();

    //firebase and fireauth instances
    private static FirebaseFirestore dbContext = FirebaseFirestore.getInstance();
    private static FirebaseAuth mAuth = FirebaseAuth.getInstance();

    //fields to manage step counter
    private static int currentMatchSteps;
    private int startMatchSteps;


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

        //retrieves from shared preferences the number of steps the match started at
        startMatchSteps = getApplicationContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE).getInt("matchStartedAtSteps", 0);
        currentMatchSteps = 0; //initializes the counter

        //notification to know if the service is running
        startForeground(NOTIFICATION_ID, getNotification(getApplicationContext()));
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        Log.i(TAG, "onStartCommand SERVICE");

        //gets shared preferences object
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE);

        //check if a match is currently going. if yes starts the service
        if (!prefs.getBoolean("matchFinished", true)) {

            //gets from shared preferences the number of steps the match started at
            startMatchSteps = getApplicationContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE).getInt("matchStartedAtSteps", 0);

            //register sensors
            reRegisterSensor();

            //necessary to understand if device is going to be shut down
            registerBroadcastReceiver();

            //updates notification (if necessary) and db
            updateIfNecessary();

            return START_STICKY; //start the service right now
        }
        //if no match is going do not start the service
        return START_NOT_STICKY;
    }

    /**
     * step counter sensor event. event.values[0] contains the number of steps since boot or since
     * the service has started
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.values[0] > Integer.MAX_VALUE) {
            Log.i(TAG + " - onSensorChanged", "maybe something went wrong: " + event.values[0]);
        } else {
            //gets steps count from sensor event
            sensorSteps = (int) event.values[0];

            //the following lines calculate the current step for the match
            SharedPreferences prefs = getApplicationContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
            startMatchSteps = prefs.getInt("matchStartedAtSteps", 0);
            if (sensorSteps - startMatchSteps > currentMatchSteps) {
                Log.i(TAG, String.format("onSensorChanged -> startMatchSteps: %d", startMatchSteps));
                currentMatchSteps = sensorSteps - startMatchSteps;
            }
            //updates the current score in shared preferences
            prefs.edit().putInt("savedSteps", currentMatchSteps).apply();

            //updates notification (if necessary) and db
            updateIfNecessary();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private boolean updateIfNecessary() {
        //if current steps score for the match is 0 or data has been update recently skip this update
        if (currentMatchSteps > 0 && System.currentTimeMillis() > lastSaveTime + SAVE_OFFSET_TIME) {

            SharedPreferences prefs = getApplicationContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
            if (sensorSteps - startMatchSteps > currentMatchSteps) {
                //calculates the current score
                currentMatchSteps = sensorSteps - startMatchSteps;
            }
            //updates current score on shared preferences
            prefs.edit().putInt("savedSteps", currentMatchSteps).apply();

            //user reference on db
            DocumentReference userRef = dbContext.collection("users").document(mAuth.getUid());

            //data update will run on a transaction because both user document and match document
            //which the user is participating will be update at the same time. If the transaction
            //fails none of the data will be update. Otherwise steps on user document and steps on
            //document of match user is participating to could be different, causing inconsistency
            dbContext.runTransaction(new Transaction.Function<Void>() {
                @Nullable
                @Override
                public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                    DocumentSnapshot userDoc = transaction.get(userRef);
                    if (userDoc.exists()) {
                        String matchId = userDoc.get("matchId").toString();

                        //read operation must take place before writes
                        DocumentReference matchRef = dbContext.collection("matches").document(matchId).collection("participants").document(mAuth.getUid());

                        transaction.update(userRef, "steps", currentMatchSteps);
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

            return true;
        }
        showNotification(); // update notification
        return false;
    }

    /**
     * start the notification
     */
    private void showNotification() {
        startForeground(NOTIFICATION_ID, getNotification(this));
    }

    /**
     * If task is removed from recent (multitasking) the service will restart itself in 500ms
     * @param rootIntent
     */
    @Override
    public void onTaskRemoved(final Intent rootIntent) {
        Log.i(TAG, "OnTaskRemoved: task has been force removed");
        super.onTaskRemoved(rootIntent);
        // Restart service in 500 ms
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                .set(AlarmManager.RTC, System.currentTimeMillis() + 500, PendingIntent
                        .getService(this, 3, new Intent(this, SensorListener.class), 0));
    }

    /**
     * Method called when the service is destroyed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            Log.i(TAG, "onDestroy called");
            SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.cancel(NOTIFICATION_ID); //removes notification
            MainActivity.isServiceStopped = true;
            stopService(new Intent(this, SensorListener.class)); //stops service
            sm.unregisterListener(this); //unregister listener
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                e.printStackTrace();
        }
    }

    /**
     * Method to get the broadcast ACTION_SHUTDOWN from system
     */
    private void registerBroadcastReceiver() {
        Log.i(TAG, "registerBroadcastReceiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SHUTDOWN);
        registerReceiver(shutdownReceiver, filter);
    }

    /**
     * Create the notification object to show that the service is working
     * @param context
     * @return Notification object to be shown
     */
    public static Notification getNotification(final Context context) {

        Notification.Builder notificationBuilder = getNotificationBuilder(context);
        notificationBuilder.setContentText(String.format("%d", (currentMatchSteps))).setContentTitle("Passi");

        notificationBuilder.setPriority(Notification.PRIORITY_MIN).setShowWhen(false)
                .setContentIntent(PendingIntent
                        .getActivity(context, 0, new Intent(context, MainActivity.class),
                                PendingIntent.FLAG_UPDATE_CURRENT))
                .setSmallIcon(R.drawable.common_full_open_on_phone).setOngoing(true);
        return notificationBuilder.build();
    }

    /**
     * Method useful to set more option to the notification
     * @param context
     * @return Notification.builder object to tune the notification
     */
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

    /**
     * re register the listener to sensor events
     */
    private void reRegisterSensor() {
        Log.i(TAG, "reRegisterSensor");
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        try {
            sm.unregisterListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // for demo purposes sensors set to be very reactive
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
                SensorManager.SENSOR_DELAY_FASTEST, (int) (6000000));
    }

    /**
     * Current steps can be modified outside this class to restore steps count if something go
     * wrong, for instance a force close
     * @param steps value to apply
     */
    public static void setCurrentSteps(int steps){
        currentMatchSteps = steps;
    }
}
