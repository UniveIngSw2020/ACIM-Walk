package com.acim.walk.ui.matchrecap;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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


                        /*
                         * at this point we have the matchId.
                         * time to get all the participants of this match
                         */
                        Task<QuerySnapshot> matchRes = db.collection("matches")
                                .whereEqualTo("matchId", matchId)
                                .get();
                        matchRes.addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if(task.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {

                                        // deserializing [{}, .., {}] the collection of objects (users) from Firebase
                                        List<Map<String, Object>> tempUsers = (List<Map<String, Object>>) document.get("participants");

                                        // adding each user to the arraylist
                                        for (Map<String, Object> user : tempUsers) {
                                            int steps = Math.toIntExact((long) user.get("steps"));
                                            String email = user.get("email").toString();
                                            String userId = user.get("userId").toString();
                                            String username = (user.get("username") == null) ? "Anonymous" : user.get("username").toString();

                                            User temp = new User(email, userId, username, steps);
                                            users.add(temp);
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

                                                if(mySteps > opponentSteps) return -1;
                                                else if(mySteps < opponentSteps) return 1;
                                                return 0;
                                            }
                                        });

                                        /*
                                         * we have all the participants of this match sorted by the number of steps
                                         * we can now update the recyclerview
                                         */
                                        adapter.notifyDataSetChanged();

                                    }
                                } else {
                                    // TODO: do something here to handle error
                                }
                            }
                        });


                    }
                } else {
                    // TODO: do something here to handle error
                }
            }
        });

        return root;
    }
}