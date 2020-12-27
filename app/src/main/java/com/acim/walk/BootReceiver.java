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

                            //sensor counter is set to 0 after every reboot
                            prefs.edit().putInt("matchStartedAtSteps", 0).apply();
                            int savedSteps = prefs.getInt("savedSteps", 0);
                            Log.i(TAG, "Saved steps: " + savedSteps + " DB Steps: " + steps);
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
        context.startForegroundService(new Intent(context, SensorListener.class));
    }
}
