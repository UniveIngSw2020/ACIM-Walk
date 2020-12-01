package com.acim.walk;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;


/*
 *
 * FirstFragment, SecondFramgment
 * activity_auth2, content_auth, fragment_first, fragment_second
 */


public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth2);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Intent myIntent = new Intent(AuthActivity.this, MainActivity.class);
        //AuthActivity.this.startActivity(myIntent);

    }
}