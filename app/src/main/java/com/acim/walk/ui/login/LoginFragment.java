package com.acim.walk.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.acim.walk.MainActivity;
import com.acim.walk.R;
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

    /**
     *
     * Logs user
     *
     * @param email
     * @param password
     */
    private void loginAccount(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();


                            String name = user.getDisplayName();
                            String email = user.getEmail();
                            if(name == null) name="NULLA";
                            if(email == null) email = "NULLA";

                            Toast.makeText(getContext(), "You're logged in.",Toast.LENGTH_SHORT).show();

                            // taking logged user to MainActivity
                            Intent myIntent = new Intent(getActivity(), MainActivity.class);
                            myIntent.putExtra("userID",user.getUid());
                            getActivity().startActivity(myIntent);

                        } else {
                            // If sign in fails, display a message to the user.
                            //Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(getContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();

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

        // referencing objects
        final EditText email = getView().findViewById(R.id.email_editext);
        final EditText password = getView().findViewById(R.id.password_editext);

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

                // authenticating user
                loginAccount(emailValue, passwordValue);
            }
        });

        /*
         * label below the login button onClick handler
         * it takes user to sign up fragment
         */
        view.findViewById(R.id.signup_label).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(LoginFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });
    }
}