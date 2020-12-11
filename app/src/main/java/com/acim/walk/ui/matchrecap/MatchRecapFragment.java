package com.acim.walk.ui.matchrecap;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.acim.walk.MainActivity;
import com.acim.walk.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;

public class MatchRecapFragment extends Fragment {

    private MatchRecapViewModel mViewModel;

    private Button showRanking;
    private Button leaveMatch;
    private TextView timer_txt;

    private String userId;
    private long timeInMillis;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private CountDownTimer countDownTimer;

    public static MatchRecapFragment newInstance() {
        return new MatchRecapFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.match_recap_fragment, container, false);

        mViewModel = new ViewModelProvider(this).get(MatchRecapViewModel.class);

        // Set global variables
        timer_txt = root.findViewById(R.id.timer_txt);
        showRanking = root.findViewById(R.id.ranking_btn);
        leaveMatch = root.findViewById(R.id.abandonMatch_btn);

        MainActivity activity = (MainActivity)getActivity();
        userId = activity.getUserID();

        System.out.println("UID UTENTE: " + userId);

        // Retrieve from db the remaining time in ms, then show on timer_txt
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

                        // Read data from matches table now
                        Task<QuerySnapshot> matchRes = db.collection("matches")
                                .whereEqualTo("matchId",matchId)
                                .get();

                        matchRes.addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {

                                        // Retrieve start and end dates used to calculate game duration
                                        Timestamp end = (Timestamp) document.getData().get("endDate");
                                        Timestamp now = new Timestamp(new Date());

                                        System.out.println("END: " + end.getSeconds());
                                        System.out.println("NOW: " + now.getSeconds());

                                        long remainingTime = end.getSeconds() - now.getSeconds();

                                        System.out.println("REMAINING TIME: " + remainingTime);

                                        // Using these two var, calculate timer
                                        timeInMillis = remainingTime * 1000;

                                        System.out.println("TIME IN MILLIS: " + timeInMillis);
                                        startCountDown();
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

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void startCountDown() {
        countDownTimer = new CountDownTimer(timeInMillis, 1000) {
            @Override
            public void onTick(long l) {
                timeInMillis = l;
                updateCountDown();
            }

            @Override
            public void onFinish() {
                timer_txt.setText("Time's over!");
            }
        }.start();
    }

    private void updateCountDown() {
        int minutes = (int) (timeInMillis / 60000);
        int seconds = (int) (timeInMillis % 60000 / 1000);

        String timeLeftText;
        timeLeftText= "" + minutes + ":";
        timeLeftText += (seconds < 10) ? "0" + seconds : seconds;

        timer_txt.setText(timeLeftText);
    }
}