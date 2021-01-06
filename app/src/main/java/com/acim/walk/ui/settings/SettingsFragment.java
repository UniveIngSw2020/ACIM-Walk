package com.acim.walk.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.acim.walk.AuthActivity;
import com.acim.walk.MainActivity;
import com.acim.walk.R;
import com.acim.walk.SensorListener;
import com.acim.walk.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
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
    private Button updatePassword;

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
/*
        final TextView textView = root.findViewById(R.id.text_slideshow);
        settingsViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }

        });*/

        MainActivity activity = (MainActivity)getActivity();

        /*
        * set user info - email and username
        */
        TextView email = (TextView) root.findViewById(R.id.email_user_info);
        email.setText(activity.getUserEmail());
        TextView username = (TextView) root.findViewById(R.id.username_user_info);
        username.setText(activity.getUsername());

        // getting access to the menu
        NavigationView nav = activity.getNavigation();
        // hiding some options on this fragment (Home and NewMatch)
        nav.getMenu().findItem(R.id.nav_settings).setVisible(false);
        nav.getMenu().findItem(R.id.nav_home).setVisible(true);


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


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // setting up Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        final EditText new_password = getView().findViewById(R.id.resetPassword);
        final EditText old_Password = getView().findViewById(R.id.oldPassword);

        /*
         * Button for Update Password
         */
        view.findViewById(R.id.update_button).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                user = mAuth.getCurrentUser();
                String oldPassword = old_Password.getText().toString().trim();
                String newPassword = new_password.getText().toString().trim();
                if (!newPassword.equals("")) {

                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);
                if(user != null) {

                        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    user.updatePassword(newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Util.toast(getActivity(), "Password Aggiornata!", true);

                                            } else {
                                                Util.showErrorAlert(getContext(), Util.ERROR_UPDATE_PASSWORD, Util.ERROR_UPDATE_PASSWORD_MESSAGE);
                                            }
                                        }
                                    });
                                } else {
                                    Util.showErrorAlert(getContext(), Util.ERROR_UPDATE_PASSWORD, Util.ERROR_UPDATE_PASSWORD_MESSAGE);
                                }
                            }
                        });
                    }
                }
                else {
                    Util.showErrorAlert(getContext(), Util.ALERT_EMPTY_PASSWORD_TITLE, Util.ALERT_EMPTY_PASSWORD_MESSAGE);
                }
            }
        });

        /*
         * label for reset the Password if the user forget it
         */
        view.findViewById(R.id.forget_old_password).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetPassword("Email inviata!", Util.ERROR_SEND_MAIL, Util.ERROR_SEND_MAIL_MESSAGE, Util.ERROR_GET_USER, Util.ERROR_GET_USER_MESSAGE );
            }
        });

        final EditText passwordElimina = getView().findViewById(R.id.password_elimina);

        /*
         * Button for Update Password
         */
        view.findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                user = mAuth.getCurrentUser();
                String password = passwordElimina.getText().toString().trim();
                if (!password.equals("")) {

                    AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
                    if(user != null) {

                        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Intent pageIniziale = new Intent(getActivity(), AuthActivity.class);
                                                pageIniziale.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                                getActivity().startActivity(pageIniziale);
                                                Util.toast(getActivity(), "Account Eliminato!", true);
                                            } else {
                                                Util.showErrorAlert(getContext(), Util.ERROR_DELETE_USER, Util.ERROR_DELETE_USER_MESSAGE);
                                            }
                                        }
                                    });
                                } else {
                                    Util.showErrorAlert(getContext(), Util.ERROR_DELETE_USER, Util.ERROR_DELETE_USER_MESSAGE);
                                }
                            }
                        });
                    }
                }
                else {
                    Util.showErrorAlert(getContext(), Util.ALERT_EMPTY_PASSWORD_TITLE, Util.ALERT_EMPTY_PASSWORD_ELIMINA_MESSAGE);
                }
            }
        });

        /*
         * label for reset the Password if the user forget it
         */
        view.findViewById(R.id.lbl_forget_password).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetPassword("Email inviata!", Util.ERROR_SEND_MAIL, Util.ERROR_SEND_MAIL_MESSAGE, Util.ERROR_GET_USER, Util.ERROR_GET_USER_MESSAGE );
            }
        });
    }

    /*
    *  Method for reset Password - usage: OnClick method of two label in the fragment
    */
    public void resetPassword(String message, String errorTitle, String errorMessage, String errorGetUser, String errorGetUserMesasge){
        user = mAuth.getCurrentUser();
        if(user != null) {
            String emailValue = user.getEmail();
            if (emailValue != "") {
                mAuth.sendPasswordResetEmail(emailValue)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Util.toast(getActivity(), message, true);
                                } else {
                                    Util.showErrorAlert(getContext(), errorTitle, errorMessage);
                                }
                            }
                        });
            } else {
                // email not setted, we show an alert
                Util.showErrorAlert(getContext(), errorGetUser, errorGetUserMesasge);
            }
        }
    }
}