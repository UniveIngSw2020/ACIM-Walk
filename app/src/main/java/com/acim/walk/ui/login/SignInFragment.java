package com.acim.walk.ui.login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.acim.walk.MainActivity;
import com.acim.walk.Model.User;
import com.acim.walk.R;
import com.acim.walk.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;



/*
 * This fragment belongs to AuthActivity and handles user SIGNUP feature
 * (creates Firebase user with email and password)
 */


public class SignInFragment extends Fragment {

    /*
    * SignInFragment field.
    * mAuth: Firebase Auth instance
    * db: Firebase Firestore instance
    *
    * */
    private final String TAG = "SignInFragment";
    private FirebaseAuth mAuth;
    FirebaseFirestore db = FirebaseFirestore.getInstance();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        /*
        * Inflate the layout for this fragment
        * */
        return inflater.inflate(R.layout.fragment_signin, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /*
        * Setting up Firebase Auth and getting references
        * */
        mAuth = FirebaseAuth.getInstance();
        final EditText email = getView().findViewById(R.id.signup_email);
        final EditText password = getView().findViewById(R.id.signup_password);
        final EditText username = getView().findViewById(R.id.signup_username);

        /*
         * Signup button onClick handler, it tries to create a new firebase user using
         * createAccount function.
         * */
        view.findViewById(R.id.signup_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                /*
                * Getting email, password and username values
                * */
                String emailValue = email.getText().toString().trim();
                String passwordValue = password.getText().toString().trim();
                String usernameValue = username.getText().toString().trim();

                /*
                 * Validating form:
                 * If the form is ready, try to sign in user, using createAccount function.
                 * Otherwise, form is NOT valid, so show an alert
                 * */
                if(Util.validateForm(emailValue, passwordValue)) {
                    createAccount(emailValue, passwordValue, usernameValue);
                } else {
                    Util.showErrorAlert(getContext(), Util.ERROR_DIALOG_TITLE, Util.ERROR_DIALOG_MESSAGE_VALIDATION);
                }
            }
        });

        /*
         * Label below the signup button onClick handler, it takes user to LoginFragment.
         */
        view.findViewById(R.id.login_label).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(SignInFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            }
        });
    }

    /**
     * create a new Firebase Auth user
     * @param email email
     * @param password password
     * @param username username (DisplayName)
     */
    private void createAccount(String email, String password, String username) {

        /*
        * displays progress bar while user waits for the device to comunicate with Firebase
        * */
        ProgressDialog progress = Util.createProgressBar(getContext(), Util.PROGRESS_DIALOG_TITLE, Util.PROGRESS_DIALOG_MESSAGE);
        progress.show();

        /*
        * Try to create a new Firebase user.
        * If sign in success, save it on Firebase and then go to MainActivity.
        * Otherwise, show an error alert.
        * */
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            /*
                            * Sign in success, update UI with the signed-in user's information
                            * */
                            FirebaseUser user = mAuth.getCurrentUser();
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(TAG, "User profile updated.");
                                            }
                                        }
                                    });


                            /*
                            * Get a new write batch.
                            * Set up the object values for the user and insert inside "users" table.
                            * Then, commit the batch
                            * */
                            WriteBatch batch = db.batch();

                            DocumentReference userRef = db.collection("users").document(user.getUid());
                            batch.set(userRef, new User(email, user.getUid(), username));

                            batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Log.d(TAG, "User added to 'users' collection!");
                                }
                            });

                            Toast.makeText(getContext(), "New user created.",Toast.LENGTH_SHORT).show();

                            /*
                             * taking logged user to MainActivity
                             * Passing the Auth ID, user's username and user's email to MainActivity.
                             * Then, start MainActivity.
                             * */

                            Intent myIntent = new Intent(getActivity(), MainActivity.class);

                            myIntent.putExtra("userID",user.getUid());
                            myIntent.putExtra("userEmail",user.getEmail());
                            myIntent.putExtra("username", user.getDisplayName());

                            myIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                            getActivity().startActivity(myIntent);

                        } else {
                            /*
                             * If sign in fails, display a message to the user and hiding progress bar.
                             * Then, showing error.
                             * */
                            progress.dismiss();
                            Util.showErrorAlert(getContext(), Util.ERROR_DIALOG_TITLE, Util.ERROR_DIALOG_MESSAGE_FAILED_SIGNUP);
                        }

                    }
                });
    }
}