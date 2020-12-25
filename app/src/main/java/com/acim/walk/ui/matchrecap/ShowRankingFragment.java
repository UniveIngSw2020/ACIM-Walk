package com.acim.walk.ui.matchrecap;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.acim.walk.MainActivity;
import com.acim.walk.Model.RankingRecyclerViewAdapter;
import com.acim.walk.Model.User;
import com.acim.walk.R;
import com.acim.walk.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShowRankingFragment extends Fragment {

    private final String TAG = "ShowRankingFragment";

    private String userId;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // recyclerview's adapter
    private RankingRecyclerViewAdapter adapter;
    /*
     * this arraylist will contain the data (users) queried from Firebase.
     * after being filled or updated, it will be binded to the recyclerview.
     * the recyclerview will use the data of each element of the arraylist to fill each row
     */
    ArrayList<User> users = new ArrayList<>();

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

        // setting up recyclerview
        RecyclerView recyclerView = root.findViewById(R.id.rankingRecyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RankingRecyclerViewAdapter(getContext(), users);
        recyclerView.setAdapter(adapter);


        /*
         *
         * querying Firebase:
         * first we get the 'matchId' attribute from the 'users' collection to see the current match of the user
         * then we use the 'matchId' to query all the participants of that match from the 'matches' collection
         *
         */

        DocumentReference userDoc = db.document(String.format("users/%s", userId));
        userDoc.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        String matchId = (String) task.getResult().get("matchId");

                        if (matchId == null) return;

                        CollectionReference participants = db.collection("matches").document(matchId).collection("participants");

                        participants.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                    for(QueryDocumentSnapshot snap : task.getResult()){
                                        String email = snap.getString("email");
                                        String userId = snap.getString("userId");
                                        String username = snap.getString("username");
                                        int steps = Math.toIntExact(snap.getLong("steps"));

                                        users.add(new User(email, userId, username, steps));
                                    }



                                    /*
                                     * at this point the arraylist has ALL the participants of this match
                                     * now we sort it (DESCENDING)
                                     * user with the MOST steps is the FIRST
                                     */
                                    Collections.sort(users, new Comparator<User>() {
                                        @Override
                                        public int compare(User user, User t1) {
                                            int mySteps = user.getSteps();
                                            int opponentSteps = t1.getSteps();

                                            if (mySteps > opponentSteps) return -1;
                                            else if (mySteps < opponentSteps) return 1;
                                            return 0;
                                        }
                                    });

                                    /*
                                     * we have all the participants of this match sorted by the number of steps
                                     * we can now update the recyclerview
                                     */
                                    adapter.notifyDataSetChanged();
                                }
                            }

                        });
                    }
                });

        return root;
    }
}