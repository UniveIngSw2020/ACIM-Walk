package com.acim.walk.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.acim.walk.AuthActivity;
import com.acim.walk.MainActivity;
import com.acim.walk.R;
import com.acim.walk.SensorListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsFragment extends Fragment {

    private SettingsViewModel settingsViewModel;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private Button logoutBtn;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get userId from MainActivity
        MainActivity activity = (MainActivity) getActivity();
        String userId = activity.getUserID();

        /*
        *
        * Handle when user press go back button inside settings.
        * If user is playing a match, he will return to match recap page, otherwise he will return
        * to home page
        *
        * */
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                db.collection("users")
                        .document(userId)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if(task.isSuccessful()) {
                                    @Nullable
                                    String matchId = (String) task.getResult().getData().get("matchId");

                                    NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                                    NavController navController = navHostFragment.getNavController();

                                    // Go to home page
                                    if (matchId == null) {
                                        navController.navigate(R.id.nav_home);
                                    }
                                    // Otherwise, go to match recap
                                    else {
                                        navController.navigate(R.id.nav_matchrecap);
                                    }

                                }
                            }
                        });
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        final TextView textView = root.findViewById(R.id.text_slideshow);
        settingsViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }

        });

        mAuth = FirebaseAuth.getInstance();

        logoutBtn = root.findViewById(R.id.logoutBtn);
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();

                user = mAuth.getCurrentUser();

                // check is user is already logged in, if yes switch to home page
                if(user == null){

                    int nextMatchStartsAt = getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE).getInt("savedSteps", 0);
                    int offset = getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE).getInt("matchStartedAtSteps", 0);

                    getActivity().stopService(new Intent(getActivity(), SensorListener.class));

                    getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                            .putInt("matchStartedAtSteps", nextMatchStartsAt + offset).apply();
                    getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit().putBoolean("matchFinished", true).apply();

                    // taking logged user to MainActivity
                    Intent loginIntent = new Intent(getActivity(), AuthActivity.class);

                    // disabling animation for a better experience and delete history of fragments
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    // starting AuthActivity
                    getActivity().startActivity(loginIntent);
                }
            }
        });

        return root;
    }
}