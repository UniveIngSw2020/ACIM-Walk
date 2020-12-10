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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;



/*
 * this fragment belongs to AuthActivity
 * this fragment handles user SIGNUP feature (creates Firebase user w/ email and password)
 */


public class SignInFragment extends Fragment {

    private final String TAG = "SignInFragment";

    // Firebase Auth instance
    private FirebaseAuth mAuth;
    // Firebase Firestore instance
    FirebaseFirestore db = FirebaseFirestore.getInstance();


    /**
     *
     * creates a new Firebase Auth user
     *
     * @param email email
     * @param password password
     * @param username username (DisplayName)
     */
    private void createAccount(String email, String password, String username) {

        // displays progress bar while user waits for the device to comunicate w/ Firebase
        ProgressDialog progress = Util.createProgressBar(getContext(), Util.PROGRESS_DIALOG_TITLE, Util.PROGRESS_DIALOG_MESSAGE);
        progress.show();


        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
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


                            // Get a new write batch
                            WriteBatch batch = db.batch();

                            // Set up the object values for the user
                            DocumentReference userRef = db.collection("users").document(user.getUid());
                            batch.set(userRef, new User(email, user.getUid(), username));

                            // Commit the batch
                            batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Log.d(TAG, "User added to 'users' collection!");
                                }
                            });


                            Toast.makeText(getContext(), "New user created.",Toast.LENGTH_SHORT).show();

                            // taking logged user to MainActivity
                            Intent myIntent = new Intent(getActivity(), MainActivity.class);
                            // passing the Auth ID to MainActivity
                            myIntent.putExtra("userID",user.getUid());
                            myIntent.putExtra("userEmail",user.getEmail());
                            // passing the user's username to the MainActivity
                            myIntent.putExtra("username", user.getDisplayName());
                            // disabling animation for a better experience
                            myIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            // starting MainActivity
                            getActivity().startActivity(myIntent);

                        } else {
                            // If sign in fails, display a message to the user.
                            // hiding progress bar
                            progress.dismiss();
                            // showing error
                            Util.showErrorAlert(getContext(), Util.ERROR_DIALOG_TITLE, Util.ERROR_DIALOG_MESSAGE_FAILED_SIGNUP);
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
        return inflater.inflate(R.layout.fragment_signin, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // setting up Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // getting references
        final EditText email = getView().findViewById(R.id.email_signup_editext);
        final EditText password = getView().findViewById(R.id.password_signup_editext);
        final EditText username = getView().findViewById(R.id.username_signup_editext);

        /*
         * signup button onClick handler
         * tries to create new firebase user
         */
        view.findViewById(R.id.signup_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // getting email and password values
                String emailValue = email.getText().toString().trim();
                String passwordValue = password.getText().toString().trim();
                String usernameValue = username.getText().toString().trim();

                // validating form
                if(Util.validateForm(emailValue, passwordValue)) {
                    // if the form is ready, we create a new Firebase user
                    createAccount(emailValue, passwordValue, usernameValue);
                } else {
                    // form is NOT valid, we show an alert
                    Util.showErrorAlert(getContext(), Util.ERROR_DIALOG_TITLE, Util.ERROR_DIALOG_MESSAGE_VALIDATION);
                }
            }
        });




        /*
         * label below the signup button onClick handler
         * it takes user to login fragment
         */
        view.findViewById(R.id.login_label).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(SignInFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            }
        });



    }
}