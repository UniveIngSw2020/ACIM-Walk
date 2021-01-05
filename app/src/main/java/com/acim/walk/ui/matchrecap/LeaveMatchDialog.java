package com.acim.walk.ui.matchrecap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.acim.walk.MainActivity;
import com.acim.walk.R;
import com.acim.walk.SensorListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;


/*
* This class is used on MainActivity and will create a Dialog, asking to user if he wants to quit
* a match.
* If user confirms, he will quit a match and he can not continue this game.
* Otherwise, he will continue with current match
* */

public class LeaveMatchDialog extends AppCompatDialogFragment {

    /*
    * LeaveMatchDialog fields.
    * db: Firebase Firestore instance
    * userId: retrieve userId from MainActivity
    * */
    private final String TAG = "LeaveMatchDialog";
    private FirebaseFirestore db;
    private String userId;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        /*
        * Create db instances and create an alertDialog.
        * */
        db = FirebaseFirestore.getInstance();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);

        /*
        * Set alertDialog with title, message and Yes/No button. Then return it to previous fragment
        * so it can show this.
        * */
        return builder.setTitle("Attenzione!")
                .setMessage("Confermando, abbandonerai la partita e non potrai più partecipare. Sei sicuro?")
                .setPositiveButton("Sì", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        /*
                        * User decide to leave the current match.
                        * Retrieve his userId from MainActivity, then update datas inside Firestone database.
                        * */
                        MainActivity main = (MainActivity) getActivity();
                        userId = main.getUserID();

                        db.collection("users")
                                .document(userId)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if(task.isSuccessful()) {
                                            /*
                                            * First, get matchId because user has to be delete also on matches table.
                                            * Retrieve match's data, delete user from participants and update match participants.
                                            * */
                                            String matchId = (String) task.getResult().get("matchId");
                                            db.collection("matches")
                                                    .document(matchId)
                                                    .get()
                                                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

                                                        @Override
                                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                            if(task.isSuccessful()) {
                                                                /*
                                                                * Delete user from participants collection and clear matchId reference on user document.
                                                                * Then, commit the batch.
                                                                *  */
                                                                WriteBatch batch = db.batch();
                                                                DocumentReference doc = db.collection("matches").document(matchId).collection("participants").document(userId);
                                                                batch.delete(doc);
                                                                DocumentReference userDoc = db.collection("users").document(userId);
                                                                batch.update(userDoc, "matchId", null);
                                                                batch.update(userDoc, "steps", 0);

                                                                batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        Log.d(TAG, "Match left successfully. Collections and document updated!");
                                                                    }
                                                                });
                                                            }
                                                        }
                                                    });
                                        }
                                    }
                                });

                        /*
                        * Return to home, stop services and reset counters
                        * */
                        int nextMatchStartsAt = getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE).getInt("savedSteps", 0);
                        nextMatchStartsAt += getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE).getInt("matchStartedAtSteps", 0);

                        getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit().putBoolean("matchFinished", true).apply();
                        getActivity().stopService(new Intent(getActivity(), SensorListener.class));
                        getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                                .putInt("matchStartedAtSteps", nextMatchStartsAt).apply();
                        getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                                .putInt("savedSteps", 0).apply();

                        Log.i(TAG, "nextMatchStartsAt -> " + nextMatchStartsAt);

                        NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                        NavController navController = navHostFragment.getNavController();
                        navController.navigate(R.id.nav_home);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        /*
                        * Do nothing, close alertDialog.
                        * */
                    }
                }).create();

    }

}
