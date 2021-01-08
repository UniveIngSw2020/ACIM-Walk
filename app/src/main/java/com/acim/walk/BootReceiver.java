package com.acim.walk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class BootReceiver extends BroadcastReceiver {

    private final String TAG = "BootReceiver";

    /**
     * Method that gets Broadcast messages from system. Useful to restart the service in order to
     * not to lose user's progresses if the device reboots or if it has been shut down and booted
     * up again later.
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(final Context context, final Intent intent) {

        SharedPreferences prefs = context.getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        FirebaseFirestore dbf = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
                DocumentReference userRef = dbf.collection("users").document(mAuth.getUid());
                userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            int steps = Math.toIntExact(task.getResult().getLong("steps"));

                            prefs.edit().remove("correctShutdown").apply();

                            //sensor counter is set to 0 after every reboot, so current steps must
                            //be calculated starting from 0 now
                            prefs.edit().putInt("matchStartedAtSteps", 0).apply();
                            int savedSteps = prefs.getInt("savedSteps", 0);
                            Log.i(TAG, "Saved steps: " + savedSteps + " DB Steps: " + steps);
                            //restores the higher score saved on shared preferences or inside
                            //user object in the db
                            if (steps < savedSteps) {
                                userRef.update("steps", savedSteps);
                                SensorListener.setCurrentSteps(savedSteps);
                            } else {
                                prefs.edit().putInt("savedSteps", steps).apply();
                                SensorListener.setCurrentSteps(steps);
                            }

                        }
                    }
                });
        }
        //starts the service
        context.startForegroundService(new Intent(context, SensorListener.class));
    }
}
