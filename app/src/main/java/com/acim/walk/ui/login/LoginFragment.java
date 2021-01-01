package com.acim.walk.ui.login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.acim.walk.MainActivity;
import com.acim.walk.R;
import com.acim.walk.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;


/*
 * this fragment belongs to AuthActivity
 * this fragment handles user LOGIN feature
 */


public class LoginFragment extends Fragment {
    // Firebase Auth instance
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    /**
     *
     * Logs user
     *
     * @param email
     * @param password
     */
    private void loginAccount(String email, String password) {

        // displays progress bar while user waits for the device to comunicate w/ Firebase
        ProgressDialog progress = Util.createProgressBar(getContext(), Util.PROGRESS_DIALOG_TITLE, Util.PROGRESS_DIALOG_MESSAGE);
        progress.show();


        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();

                            // taking logged user to MainActivity
                            Intent myIntent = new Intent(getActivity(), MainActivity.class);
                            // passing the Auth ID to MainActivity
                            myIntent.putExtra("userID",user.getUid());
                            myIntent.putExtra("userEmail",user.getEmail());
                            // passing the user's username to the MainActivity
                            myIntent.putExtra("username", user.getDisplayName());
                            // disabling animation for a better experience
                            // disabling animation for a better experience
                            myIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            // starting MainActivity
                            getActivity().startActivity(myIntent);
                        } else {
                            // If sign in fails, display a message to the user.
                            // hiding progress bar
                            progress.dismiss();
                            // showing error
                            Util.showErrorAlert(getContext(), Util.ERROR_DIALOG_TITLE, Util.ERROR_DIALOG_MESSAGE_FAILED_LOGIN);
                        }

                    }
                });
    }



    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // setting up Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        // check is user is already logged in, if yes switch to home page
        if(user != null){
            // taking logged user to MainActivity
            Intent myIntent = new Intent(getActivity(), MainActivity.class);
            // passing the Auth ID to MainActivity
            myIntent.putExtra("userID",user.getUid());
            // passing the user's email to MainActivity
            myIntent.putExtra("userEmail",user.getEmail());
            // passing the user's username to the MainActivity
            myIntent.putExtra("username", user.getDisplayName());
            // disabling animation for a better experience
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            // starting MainActivity
            getActivity().startActivity(myIntent);
        }

        // referencing objects
        final EditText email = getView().findViewById(R.id.login_email);
        final EditText password = getView().findViewById(R.id.login_password);

        /*
         * login button onClick handler
         * tries to authenticate user
         */
        view.findViewById(R.id.login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // getting email and password values
                String emailValue = email.getText().toString().trim();
                String passwordValue = password.getText().toString().trim();

                // validating form
                if(Util.validateForm(emailValue, passwordValue)) {
                    // if the form is ready, we try to login the user
                    loginAccount(emailValue, passwordValue);
                } else {
                    // form is NOT valid, we show an alert
                    Util.showErrorAlert(getContext(), Util.ERROR_DIALOG_TITLE, Util.ERROR_DIALOG_MESSAGE_VALIDATION);
                }
            }
        });

        /*
         * label below the login button onClick handler
         * it takes user to sign up fragment
         */
        view.findViewById(R.id.login_signup_label).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(LoginFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });

        /*
         * label under the password editText
         * reset password
         */
        view.findViewById(R.id.reset_password_label).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String emailValue = email.getText().toString().trim();
                if(emailValue != ""){
                    mAuth.sendPasswordResetEmail(emailValue)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Util.toast(getActivity(), "Email inviata!", true);
                                    }else{
                                        Util.showErrorAlert(getContext(), Util.ERROR_SEND_MAIL, Util.ERROR_SEND_MAIL_MESSAGE);
                                    }
                                }
                            });
                } else {
                    // email not setted, we show an alert
                    Util.showErrorAlert(getContext(), Util.ALERT_EMPTY_EMAIL_TITLE, Util.ALERT_EMPTY_EMAIL_MESSAGE);
                }
            }
        });

        view.findViewById(R.id.twitter_logo).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://twitter.com/Walk02318614");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        view.findViewById(R.id.facebook_logo).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://www.facebook.com/walkk.acim/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        view.findViewById(R.id.instagram_logo).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://www.instagram.com/walk_acim/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }
}