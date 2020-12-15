package com.acim.walk.ui.matchrecap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.acim.walk.MainActivity;
import com.acim.walk.Model.User;
import com.acim.walk.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LeaveMatchDialog extends AppCompatDialogFragment {

    private final String TAG = "LeaveMatchDialog";

    private FirebaseFirestore db;
    private String userId;
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        db = FirebaseFirestore.getInstance();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        return builder.setTitle("Attenzione!")
                .setMessage("Confermando, abbandonerai la partita e non potrai più partecipare. Sei sicuro?")
                .setPositiveButton("Sì", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        //Remove current user from match
                        MainActivity main = (MainActivity) getActivity();
                        userId = main.getUserID();

                        // Get matchId from users table, searching the current user
                        db.collection("users")
                                .document(userId)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if(task.isSuccessful()) {

                                            // Get matchId
                                            String matchId = (String) task.getResult().get("matchId");

                                            // Retrieve match datas, delete user from participants and update match participants
                                            db.collection("matches")
                                                    .document(matchId)
                                                    .get()
                                                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

                                                        @Override
                                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                            if(task.isSuccessful()) {

                                                                // deserializing [{}, .., {}] the collection of objects (users) from Firebase
                                                                List<Map<String, Object>> tempUsers = (List<Map<String, Object>>) task.getResult().get("participants");

                                                                // Create a new List that contains all the participants, except current user
                                                                List<User> participants = new ArrayList<>();

                                                                for (Map<String, Object> user : tempUsers) {
                                                                    // Create an user and add to participants
                                                                    if (!userId.equals(user.get("userId").toString())) {
                                                                        String email = user.get("email").toString();
                                                                        String id = user.get("userId").toString();
                                                                        String username = user.get("username").toString();
                                                                        int steps = Math.toIntExact((Long) user.get("steps"));
                                                                        participants.add(new User(email,id,username,steps));
                                                                    }
                                                                }

                                                                // Update new List in firebase, without current user
                                                                WriteBatch batch = db.batch();
                                                                DocumentReference doc = db.collection("matches").document(matchId);
                                                                batch.update(doc, "participants", participants);

                                                                // Commit the batch
                                                                batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        Log.d(TAG, "User added to 'users' collection!");
                                                                    }
                                                                });
                                                            }
                                                        }
                                                    });
                                        }
                                    }
                                });

                        // Return to home
                        NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                        NavController navController = navHostFragment.getNavController();
                        navController.navigate(R.id.nav_home);

                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).create();
    }
}