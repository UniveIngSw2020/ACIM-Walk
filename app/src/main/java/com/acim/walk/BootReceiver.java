package com.acim.walk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.firebase.firestore.FirebaseFirestore;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {

        SharedPreferences prefs = context.getSharedPreferences("pedometer", Context.MODE_PRIVATE);


        Database db = Database.getInstance(context);

        if (!prefs.getBoolean("correctShutdown", false)) {
            // can we at least recover some steps?
            int steps = Math.max(0, db.getCurrentSteps());
            db.addToLastEntry(steps);
        }
        // last entry might still have a negative step value, so remove that
        // row if that's the case
        db.removeNegativeEntries();
        db.saveCurrentSteps(0);
        db.close();
        prefs.edit().remove("correctShutdown").apply();

        FirebaseFirestore dbf = FirebaseFirestore.getInstance();




        context.startForegroundService(new Intent(context, SensorListener.class));

    }
}
