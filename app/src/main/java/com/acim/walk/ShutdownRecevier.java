package com.acim.walk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.firebase.firestore.FirebaseFirestore;

public class ShutdownRecevier extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {

        //start the service to count and update steps
        context.startService(new Intent(context, SensorListener.class));

        //restores the correct shutdown status on shared preferences. If correctShutdown is false
        //the application has been force stopped and it didn't get the DEVICE_SHUTDOWN broadcast signal
        context.getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                .putBoolean("correctShutdown", true).apply();

    }

}
