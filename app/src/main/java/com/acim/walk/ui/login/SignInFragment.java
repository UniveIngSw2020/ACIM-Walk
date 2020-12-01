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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

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

    // Firebase Auth instance
    private FirebaseAuth mAuth;
    // Firebase Firestore instance
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static final String TAG = "EmailPassword";

    /**
     *
     * creates a new Firebase Auth user
     *
     * @param email email
     * @param password password
     */
    private void createAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();

                            Toast.makeText(getContext(), "New user created.",Toast.LENGTH_SHORT).show();


                            // this may be useful for some features
                            //String userId = user.getUid();
                            //storeUserId(userId);


                            // taking new user to MainActivity
                            Intent myIntent = new Intent(getActivity(), MainActivity.class);
                            getActivity().startActivity(myIntent);

                        } else {
                            // If sign in fails, display a message to the user.
                            //Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(getContext(), "Error while creating a new user.", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
    }


    /**
     *
     * stores the ID of the Firebase Auth user in the Firebase Firestore docs
     *
     * @param userId id generated from the Firebase Auth user
     */
    private void storeUserId(String userId) {
        Map<String, Object> user = new HashMap<>();
        user.put("authId", userId);

        // Add a new document with a generated ID
        db.collection("users")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        //Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                        Toast.makeText(getContext(), "AGGIUNTO", Toast.LENGTH_SHORT).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Log.w(TAG, "Error adding document", e);
                        Toast.makeText(getContext(), "NON AGGIUNTO", Toast.LENGTH_SHORT).show();
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
                //String usernameValue = username.getText().toString().trim();

                // creating new Firebase user
                createAccount(emailValue, passwordValue);
                //storeUsername(usernameValue);
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