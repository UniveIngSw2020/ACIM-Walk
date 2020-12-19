package com.acim.walk.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.acim.walk.Database;
import com.acim.walk.MainActivity;
import com.acim.walk.R;
import com.acim.walk.SensorListener;
import com.acim.walk.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.Locale;

public class HomeFragment extends Fragment implements SensorEventListener2 {

    private HomeViewModel homeViewModel;
    private TextView currentStepsTxt;
    private Button newMatchBtn, searchMatchBtn;
    FirebaseFirestore dbfirestore = FirebaseFirestore.getInstance();
    private int todayOffset, total_start, since_boot, total_days;
    public final static NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
    private boolean showSteps = true;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //First check if logged in user participates a match. If yes starts the service to enable sensor to count steps and to show notification
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        Task<DocumentSnapshot> useRef = db.collection("users").document(mAuth.getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){
                            if(task.getResult() != null && task.getResult().getString("matchId") != null && task.getResult().getString("matchId") != ""){
                                getActivity().startForegroundService(new Intent(getActivity(), SensorListener.class));
                                Log.i("LOG", "Service started");
                            }
                        }
                    }
                });
        Log.i("LOG", "Service started");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        currentStepsTxt = root.findViewById(R.id.text_home);
        newMatchBtn = root.findViewById(R.id.newMatchBtn);
        searchMatchBtn = root.findViewById(R.id.searchMatchBtn);

        searchMatchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                NavController navController = navHostFragment.getNavController();
                navController.navigate(R.id.search_opponent_layout);
            }
        });

        newMatchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                NavController navController = navHostFragment.getNavController();
                // navController.navigate(R.id.nav_matchrecap);
                navController.navigate(R.id.nav_newmatch);
            }
        });
        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                currentStepsTxt.setText(s);
            }
        });
        Log.i("LOG", "onCreateView");
        return root;
    }


    @Override
    public void onResume() {
        super.onResume();

        Database db = Database.getInstance(getActivity());

        FirebaseFirestore dbfirestore = FirebaseFirestore.getInstance();
        MainActivity activity = (MainActivity)getActivity();
        String userID = activity != null ? activity.getUserID() : "NaN";

        DocumentReference currentUserDocRef = dbfirestore.collection("users").document("prova");

        // read todays offset
        todayOffset = db.getSteps(Util.getToday());

        SharedPreferences prefs =
                getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);

        since_boot = db.getCurrentSteps();
        int pauseDifference = since_boot - prefs.getInt("pauseCount", since_boot);

        // register a sensorlistener to live update the UI if a step is taken
        SensorManager sm = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
/*
        if (sensor == null) {

            // L'APP mi crasha nell'emulatore

            new AlertDialog.Builder(getActivity()).setTitle("Sensori richiesti assenti")
                    .setMessage("Il tuo telefono non supporta l'applicazione")
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(final DialogInterface dialogInterface) {
                            getActivity().finish();
                        }
                    }).setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).create().show();



        } else {
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, 0);
        }

*/
        since_boot -= pauseDifference;

        total_start = db.getTotalWithoutToday();
        total_days = db.getDays();

        db.close();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            SensorManager sm =
                    (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            sm.unregisterListener(this);
        } catch (Exception e) {
        }
        Database db = Database.getInstance(getActivity());
        db.saveCurrentSteps(since_boot);
        db.close();
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (event.values[0] > Integer.MAX_VALUE || event.values[0] == 0) {
            return;
        }
        if (todayOffset == Integer.MIN_VALUE) {
            // no values for today
            // we dont know when the reboot was, so set todays steps to 0 by
            // initializing them with -STEPS_SINCE_BOOT
            todayOffset = -(int) event.values[0];
            Database db = Database.getInstance(getActivity());
            db.insertNewDay(Util.getToday(), (int) event.values[0]);
            db.close();
        }
        since_boot = (int) event.values[0];
        Log.i("LOG", "onSensorChanged");
        updateCounter();
    }

    private void updateCounter(){

        int steps_today = Math.max(todayOffset + since_boot, 0);
        currentStepsTxt.setText(String.format("%d",steps_today));
        Log.i("LOG", "steps updated!");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}