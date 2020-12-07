package com.acim.walk.ui.newmatch;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.acim.walk.MainActivity;
import com.acim.walk.R;
import com.acim.walk.ui.searchmatch.SearchMatchFragment;

public class NewmatchFragment extends Fragment {

    private NewMatchViewModel newMatchViewModel;

    private TextView countDownText;
    private TextView stepsText;

    private CountDownTimer countDownTimer;
    private long timeInMillis; // 10 min in millis

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_newmatch, container, false);

        MainActivity main = (MainActivity) getActivity();
        timeInMillis = main.getTimesInMillis(); // Retrieve timer from the previous fragment

        newMatchViewModel =
                new ViewModelProvider(this).get(NewMatchViewModel.class);

        countDownText = root.findViewById(R.id.text_countdown);
        stepsText = root.findViewById(R.id.stepsCounter_text);

        // Set text used to set steps counter
        newMatchViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                stepsText.setText(s);
            }
        });

        startCountDown();

        return root;
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
                countDownText.setText("Time's over!");
            }
        }.start();
    }

    private void updateCountDown() {
        int minutes = (int) (timeInMillis / 60000);
        int seconds = (int) (timeInMillis % 60000 / 1000);

        String timeLeftText;
        timeLeftText= "" + minutes + ":";
        timeLeftText += (seconds < 10) ? "0" + seconds : seconds;

        countDownText.setText(timeLeftText);
    }
}
