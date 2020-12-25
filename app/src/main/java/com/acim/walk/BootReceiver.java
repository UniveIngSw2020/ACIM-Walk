package com.acim.walk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {

        SharedPreferences prefs = context.getSharedPreferences("pedometer", Context.MODE_PRIVATE);

        FirebaseFirestore dbf = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            if (!prefs.getBoolean("correctShutdown", false)) {
                // can we at least recover some steps?
                DocumentReference userRef = dbf.collection("users").document(mAuth.getUid());
                userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            int steps = Math.toIntExact(task.getResult().getLong("steps"));
                            userRef.update("steps", 0);
                            prefs.edit().remove("correctShutdown").apply();
                            int savedSteps = prefs.getInt("savedSteps", 0);
                            if(steps < savedSteps){
                                userRef.update("steps", savedSteps);
                            }
                        }
                    }
                });
            }
        }

        context.startForegroundService(new Intent(context, SensorListener.class));

    }
}
