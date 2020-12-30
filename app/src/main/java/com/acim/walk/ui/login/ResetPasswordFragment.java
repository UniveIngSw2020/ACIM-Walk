package com.acim.walk.ui.login;

import android.widget.EditText;

import com.acim.walk.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.fragment.app.Fragment;

public class ResetPasswordFragment extends Fragment{
    // Firebase Auth instance
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private void updateAccount(String email, String password) {
        final EditText email_reset = getView().findViewById(R.id.signup_email);
        final EditText password_reset = getView().findViewById(R.id.signup_password);

        //implementa aggiornamento password
    }
}
