package com.acim.walk;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

public class AuthActivity extends AppCompatActivity {

    private final String TAG = "AuthActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth2);
        Log.d(TAG, "AuthActivity --- onCreate");
    }

    protected void onResume(){
        super.onResume();
        Log.d(TAG, "AuthActivity --- onResume");
    }
}