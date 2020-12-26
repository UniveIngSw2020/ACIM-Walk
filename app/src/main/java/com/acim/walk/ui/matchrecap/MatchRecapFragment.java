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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.acim.walk.MainActivity;
import com.acim.walk.R;
import com.acim.walk.SensorListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.Date;

public class MatchRecapFragment extends Fragment {

    private final String TAG = "MatchRecapFragment";

    private MatchRecapViewModel mViewModel;

    private Button showRanking;
    private Button leaveMatch;
    private TextView timer_txt;
    private TextView steps_txt;

    private String userId;
    private long timeInMillis;

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

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                // Handle the back button even
                Log.d("BACKBUTTON", "Back button clicks");
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);

        // getting access to the menu
        NavigationView nav = ((MainActivity)getActivity()).getNavigation();
        // hiding Home option on this fragment
        nav.getMenu().findItem(R.id.nav_home).setVisible(false);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.match_recap_fragment, container, false);

        mViewModel = new ViewModelProvider(this).get(MatchRecapViewModel.class);

        // Set global variables
        steps_txt = root.findViewById(R.id.steps_txt);
        timer_txt = root.findViewById(R.id.timer_txt);
        showRanking = root.findViewById(R.id.ranking_btn);
        leaveMatch = root.findViewById(R.id.abandonMatch_btn);

        MainActivity activity = (MainActivity)getActivity();
        userId = activity.getUserID();

        System.out.println("UID UTENTE: " + userId);

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

                                System.out.println("TIME IN MILLIS: " + timeInMillis);
                                startCountDown(getActivity());
                            }
                        }
                    });
                }
            }
        });


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
                    //TODO (SC): reset counter in user document (in users collection). Stop service and reset local step counter.
                    DocumentReference userRef = db.collection("matches").document(userId);
                    userRef.update("steps", 0);

                    getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                            .putInt("savedSteps", 0).apply();
                    getActivity().stopService(new Intent(getActivity(), SensorListener.class));

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

                // Remove user matchId, because he finish this match
                db.collection("users")
                        .document(userId)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if(task.isSuccessful()) {

                                    WriteBatch batch = db.batch();
                                    //Clear matchId reference on user document
                                    DocumentReference userDoc = db.collection("users").document(userId);
                                    batch.update(userDoc, "matchId", null);
                                    batch.update(userDoc, "steps", 0);

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

                // stopping steps service

                context.getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                        .putInt("savedSteps", 0).apply();

                context.stopService(new Intent(context, SensorListener.class));
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