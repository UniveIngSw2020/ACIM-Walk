package com.acim.walk.ui.matchrecap;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.acim.walk.MainActivity;
import com.acim.walk.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.reflect.Array;
import java.util.Date;
import java.util.HashMap;

public class ShowRankingFragment extends Fragment {

    private String userId;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ShowRankingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_show_ranking, container, false);

        MainActivity main = (MainActivity) getActivity();
        userId = main.getUserID();

        Task<QuerySnapshot> userRes = db.collection("users")
                .whereEqualTo("userId", userId)
                .get();

        userRes.addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {

                        // getting matchId property from the user
                        String matchId = (String) document.getData().get("matchId");

                        // User is not playing any game
                        if (matchId == null) return;

                        //TODO: implementare la parte di creazione classifica


                    }
                } else {
                    // TODO: do something here to handle error
                }
            }
        });

        return root;
    }
}