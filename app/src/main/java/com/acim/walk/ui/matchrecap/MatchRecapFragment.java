package com.acim.walk.ui.matchrecap;

import androidx.activity.OnBackPressedCallback;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.acim.walk.MainActivity;
import com.acim.walk.R;
import com.acim.walk.SensorListener;
import com.acim.walk.ui.CloseAppDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MatchRecapFragment extends Fragment {

    private final String TAG = "MatchRecapFragment";

    private Button showRanking;
    private Button leaveMatch;
    private TextView timer_txt;
    private TextView steps_txt;

    private String userId;
    private long timeInMillis;

    // An ExecutorService that can schedule commands to run after a given delay, or to execute periodically
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /*
     * when the match is over, this will become TRUE
     * it will be used for the 'leaveMatch' button
     * if 'isOver' is TRUE, the user will be redirected to HOME because the match is ended/over
     * if 'isOver' is FALSE, user will abandon the match, because it's not over yet
     */
    private boolean matchIsOver = false;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private CountDownTimer countDownTimer;

    public static MatchRecapFragment newInstance() {
        return new MatchRecapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        /*
         * Callback used when user press go back button. In this case user can go back to previous page
         * but he can only close application. So when user press go back button a dialog will be opened
         * and ask to user if he wants to close application
         *
         */
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                // Handle the back button even
                Log.d("BACKBUTTON", "Back button clicks");

                CloseAppDialog closeAppDialog = new CloseAppDialog();
                closeAppDialog.show(getActivity().getSupportFragmentManager(), "");
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.match_recap_fragment, container, false);

        MainActivity activity = (MainActivity)getActivity();
        userId = activity.getUserID();

        // getting access to the menu
        NavigationView nav = activity.getNavigation();
        // hiding some options on this fragment (Home and NewMatch)
        nav.getMenu().findItem(R.id.nav_home).setVisible(false);
        nav.getMenu().findItem(R.id.nav_newmatch).setVisible(false);

        // Set global variables
        steps_txt = root.findViewById(R.id.matchrecap_steps_text);
        timer_txt = root.findViewById(R.id.matchrecap_timer_text);
        showRanking = root.findViewById(R.id.matchrecap_ranking_button);
        leaveMatch = root.findViewById(R.id.matchrecap_quit_button);

        // Retrieve from db the remaining time in ms, then show on timer_txt
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful() && task.getResult().exists()){
                    // getting matchId property from the user
                    String matchId = (String) task.getResult().getData().get("matchId");

                    // User is not playing any game
                    if (matchId == null) return;

                    // Read data from matches table now
                    DocumentReference matchRef = db.collection("matches").document(matchId);
                    matchRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if(task.isSuccessful() && task.getResult().exists()){

                                // Retrieve start and end dates used to calculate game duration
                                Timestamp end = (Timestamp) task.getResult().getData().get("endDate");
                                Timestamp now = new Timestamp(new Date());

                                long remainingTime = end.getSeconds() - now.getSeconds();

                                // Using these two var, calculate timer
                                timeInMillis = remainingTime * 1000;

                                startCountDown(getActivity());
                            }
                        }
                    });
                }
            }
        });

        // Set scheduler that every x seconds update text view that contains the number of steps made by the user.
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {

                // Go to firebase match and retrieve user steps, then update the textview
                db.collection("users")
                        .document(userId)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {

                                    // Retrieve matchId
                                    String matchId = (String) task.getResult().getData().get("matchId");

                                    if (matchId == null) return;

                                    // Retrieve steps from matches collection and update textview
                                    db.collection("matches")
                                            .document(matchId) // Retrieve the current match infos
                                            .collection("participants") // Retrieve the participants document
                                            .document(userId) // Retrieve the user's infos
                                            .get()
                                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    if(task.isSuccessful())  {
                                                        DocumentSnapshot document = task.getResult();
                                                        // the query returned SOMETHING
                                                        if(document.exists()) {
                                                            Log.d("MRECAP", task.getResult().getData().toString());

                                                            String steps = task.getResult().getData().get("steps").toString();
                                                            steps_txt.setText(steps);
                                                        }
                                                        // result is NULL, the query returned nothing
                                                        else {
                                                            Log.d("MRECAP", "NON VA");
                                                        }
                                                    }
                                                }
                                            });
                                }
                            }
                        });
            }
        }, 0, 1, TimeUnit.SECONDS);


        showRanking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Go to ranking page
                NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                NavController navController = navHostFragment.getNavController();
                navController.navigate(R.id.nav_show_ranking);
            }
        });


        leaveMatch.setOnClickListener(new View.OnClickListener() {
            MainActivity activity = (MainActivity)getActivity();
            @Override
            public void onClick(View view) {
                // if match is over, takes user back to home
                if(matchIsOver) {

                    //Clear matchId reference on user document
                    WriteBatch batch = db.batch();
                    DocumentReference userDoc = db.collection("users").document(userId);
                    batch.update(userDoc, "matchId", null);
                    batch.update(userDoc, "steps", 0);

                    // Commit the batch
                    batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(TAG, "User is removed from match, he completed it!");
                        }
                    });

                    // Stop pedometer service
                    getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                            .putInt("savedSteps", 0).apply();
                    getActivity().stopService(new Intent(getActivity(), SensorListener.class));

                    // Redirect user to home page
                    NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                    NavController navController = navHostFragment.getNavController();
                    navController.navigate(R.id.nav_home);
                }
                // match is NOT over yet, user will leave the match
                else {
                    LeaveMatchDialog leaveMatchDialog = new LeaveMatchDialog();
                    leaveMatchDialog.show(activity.getSupportFragmentManager(), "");
                }
            }
        });

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    // Function used to handle timer events as start or finish and timer ticking
    private void startCountDown(Context context) {
        countDownTimer = new CountDownTimer(timeInMillis, 1000) {
            @Override
            public void onTick(long l) {
                timeInMillis = l;
                updateCountDown();
            }

            @Override
            public void onFinish() {

                /*
                 * timer finished: match is over.
                 * the 'leave match' button will display now 'home', to redirect user to HOME
                 */
                matchIsOver = true;

                leaveMatch.setText("Home");
                timer_txt.setText("Time's over!");

                // stopping steps service
                int nextMatchStartsAt = context.getSharedPreferences("pedometer", Context.MODE_PRIVATE).getInt("savedSteps", 0);
                nextMatchStartsAt += context.getSharedPreferences("pedometer", Context.MODE_PRIVATE).getInt("matchStartedAtSteps", 0);
                context.getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit().putBoolean("matchFinished", true).apply();
                Log.i(TAG, "nextMatchStartsAt -> " + nextMatchStartsAt);
                context.stopService(new Intent(context, SensorListener.class));
                context.getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                        .putInt("matchStartedAtSteps", nextMatchStartsAt).apply();
                context.getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                        .putInt("savedSteps", 0).apply();

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