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

public class MatchRecapFragment extends Fragment {

    private MatchRecapViewModel mViewModel;

    private Button showRanking;
    private Button leaveMatch;
    private TextView timer_txt;

    private String userId;
    private long timeInMillis;

    private CountDownTimer countDownTimer;

    public static MatchRecapFragment newInstance() {
        return new MatchRecapFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.match_recap_fragment, container, false);

        // Set global variables
        timer_txt = root.findViewById(R.id.timer_txt);
        showRanking = root.findViewById(R.id.ranking_btn);
        leaveMatch = root.findViewById(R.id.abandonMatch_btn);

        MainActivity main = new MainActivity();
        userId = main.getUserID();

        System.out.println("UID UTENTE: " + userId);

        // Retrieve from db the remaining time in ms, then show on timer_txt
        timeInMillis = mViewModel.getTimeFromDb(userId);

        startCountDown();
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(MatchRecapViewModel.class);
        // TODO: Use the ViewModel
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