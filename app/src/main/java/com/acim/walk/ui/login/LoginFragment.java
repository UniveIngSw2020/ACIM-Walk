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
* This fragment belongs to AuthActivity and handles user login features or redirect to social
* network pages.
* */
public class LoginFragment extends Fragment {
    /*
    * LoginFragment.
    * These fields are Firebase Auth instance
    * */
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        /*
        * Inflate the layout for this fragment
        * */
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /*
        * setting up Firebase Auth
        * */
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        /*
        * Check is user is already logged in, if yes switch to home page.
        * Before switching, pass Auth ID, user's username and user's email to MainActivity.
        * Then, pass to MainActivity.
        * */
        if(user != null){
            Intent myIntent = new Intent(getActivity(), MainActivity.class);
            myIntent.putExtra("userID",user.getUid());
            myIntent.putExtra("userEmail",user.getEmail());
            myIntent.putExtra("username", user.getDisplayName());
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            getActivity().startActivity(myIntent);
        }

        /*
        * Otherwise, user is not logged in, get email and password from the editText inside
        * fragment_login.
        * */
        final EditText email = getView().findViewById(R.id.login_email);
        final EditText password = getView().findViewById(R.id.login_password);

        /*
         * Login button onClick handler, tries to authenticate user and pass to MainActivity.
         * */
        view.findViewById(R.id.login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                * Get email and password values
                * */
                String emailValue = email.getText().toString().trim();
                String passwordValue = password.getText().toString().trim();

                /*
                * Validating form:
                * If the form is ready, try to login the user, using loginAccount function.
                * Otherwise, form is NOT valid, so show an alert
                * */
                if(Util.validateForm(emailValue, passwordValue)) {
                    loginAccount(emailValue, passwordValue);
                } else {
                    Util.showErrorAlert(getContext(), Util.ERROR_DIALOG_TITLE, Util.ERROR_DIALOG_MESSAGE_VALIDATION);
                }
            }
        });

        /*
         * Label below login button onClick handler. It takes user to SignInFragment
         * */
        view.findViewById(R.id.login_signup_label).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(LoginFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });

        /*
         * Label under password editText. It is used to reset user's password.
         * When pressed, send an email to user so he can reset his password
         * */
        view.findViewById(R.id.reset_password_label).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                * Take user's email.
                * If email is empty, application can not send an email because it can't, so show an alert.
                * Otherwise, send password reset email. If email is sent, show a notification that the process
                * is done, else show an error alert.
                * */
                String emailValue = email.getText().toString().trim();
                if(!emailValue.equals("")){
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
                    Util.showErrorAlert(getContext(), Util.ALERT_EMPTY_EMAIL_TITLE, Util.ALERT_EMPTY_EMAIL_MESSAGE);
                }
            }
        });

        /*
        * Connection to twitter page.
        * */
        view.findViewById(R.id.twitter_logo).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://twitter.com/Walk02318614");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        /*
         * Connection to facebook page.
         * */
        view.findViewById(R.id.facebook_logo).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://www.facebook.com/walkk.acim/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        /*
         * Connection to instagram page.
         * */
        view.findViewById(R.id.instagram_logo).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://www.instagram.com/walk_acim/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }

    /**
     * Function used to check if an user can login inside application
     *
     * @param email
     * @param password
     */
    private void loginAccount(String email, String password) {

        /*
         * displays progress bar while user waits for the device to communicate with Firebase
         * */
        ProgressDialog progress = Util.createProgressBar(getContext(), Util.PROGRESS_DIALOG_TITLE, Util.PROGRESS_DIALOG_MESSAGE);
        progress.show();

        /*
         * Login process, pass email and password as parameters.
         * If user exist, get his credentials and then pass to MainActivity.
         * Otherwise, show a error alert, saying that credentials are wrong
         * */
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            /*
                             * Sign in success, update UI with the signed-in user's information
                             * */
                            FirebaseUser user = mAuth.getCurrentUser();

                            /*
                             * taking logged user to MainActivity
                             * Passing the Auth ID, user's username and user's email to MainActivity.
                             * */
                            Intent myIntent = new Intent(getActivity(), MainActivity.class);
                            myIntent.putExtra("userID",user.getUid());
                            myIntent.putExtra("userEmail",user.getEmail());
                            myIntent.putExtra("username", user.getDisplayName());

                            /*
                             * disabling animation for a better experience
                             * */
                            myIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                            /*
                             * starting MainActivity
                             * */
                            getActivity().startActivity(myIntent);
                        } else {
                            /*
                             * If sign in fails, display a message to the user and hiding progress bar.
                             * Then, showing error.
                             * */
                            progress.dismiss();
                            Util.showErrorAlert(getContext(), Util.ERROR_DIALOG_TITLE, Util.ERROR_DIALOG_MESSAGE_FAILED_LOGIN);
                        }

                    }
                });
    }
}