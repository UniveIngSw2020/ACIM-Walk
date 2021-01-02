package com.acim.walk;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;


/*                            // taking logged user to MainActivity
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
 *
 * FirstFragment, SecondFramgment
 * activity_auth2, content_auth, fragment_first, fragment_second
 */


public class AuthActivity extends AppCompatActivity {

    private final String TAG = "AuthActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth2);
        Log.d(TAG, "AuthActivity --- onCreate");
        //Intent myIntent = new Intent(AuthActivity.this, MainActivity.class);
        //AuthActivity.this.startActivity(myIntent);

    }

    protected void onResume(){
        super.onResume();
        Log.d(TAG, "AuthActivity --- onResume");
    }
}