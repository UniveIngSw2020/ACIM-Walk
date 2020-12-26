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

//import com.acim.walk.Database;
import com.acim.walk.MainActivity;
import com.acim.walk.R;
import com.acim.walk.SensorListener;
import com.acim.walk.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.Locale;

public class HomeFragment extends Fragment /*implements SensorEventListener2*/ {

    private final String TAG = "HomeFragment";

    private HomeViewModel homeViewModel;
    private TextView currentStepsTxt, userInfoTxt;
    private Button newMatchBtn, searchMatchBtn;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private int todayOffset, total_start, since_boot, total_days;
    public final static NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
    private boolean showSteps = true;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        currentStepsTxt = root.findViewById(R.id.text_home);
        userInfoTxt = root.findViewById(R.id.userInfoTxt);
        newMatchBtn = root.findViewById(R.id.newMatchBtn);
        searchMatchBtn = root.findViewById(R.id.searchMatchBtn);

        // displaying user info
        FirebaseUser user = mAuth.getCurrentUser();
        // getting username from MainActivity method
        String username = ((MainActivity)getActivity()).getUsername();
        String text = user.getEmail() + "-" + username;
        if(username == null)
            text = user.getEmail();
        userInfoTxt.setText(text);


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
}