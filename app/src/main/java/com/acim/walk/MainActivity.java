package com.acim.walk;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.TextView;

import com.acim.walk.ui.home.HomeFragment;
import com.acim.walk.ui.login.LoginFragment;
import com.acim.walk.ui.login.SignInFragment;
import com.acim.walk.ui.newmatch.NewmatchFragment;
import com.acim.walk.ui.searchmatch.SearchMatchFragment;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity /*implements SensorEventListener2*/ {

    private final String TAG = "MainActivity";

    private AppBarConfiguration mAppBarConfiguration;
    private NavigationView navigationView;
    private String userID = "";
    private String userEmail = "";
    private String username = "";

    // Firebase Firestore instance
    // we need it to find out if the user is participating a game
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private long timesInMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // startService(new Intent(this, SensorListener.class));

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            userID = extras.getString("userID");
            userEmail = extras.getString("userEmail");
            username = extras.getString("username");
        }
        else{
            // Activity created from tap on notification
            userID = mAuth.getUid();
            userEmail = mAuth.getCurrentUser().getEmail();
            username = mAuth.getCurrentUser().getDisplayName();
            username = mAuth.getCurrentUser().getDisplayName();
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_matchrecap, R.id.nav_settings)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);


        /*
         * checking if user has an actual match going on.
         * first we get the user's data from Firebase.
         * the attribute 'matchId' will tell us his status:
         *      'matchId' is NULL     -> user HAS NOT an actual match going on
         *      'matchId' is NOT NULL -> user HAS an actual match going on
         * if there's a match going on, show MatchRecapFragment that displays info about the current match
         * if there's NO match going on, show regular HomeFragment
         */

        // displays progress bar while user waits for the device to comunicate w/ Firebase
        ProgressDialog progress = Util.createProgressBar(this, Util.PROGRESS_DIALOG_TITLE, Util.PROGRESS_DIALOG_MESSAGE);
        progress.show();

        Log.d(TAG, "USER: " + userID);

        DocumentReference userDocRef = db.collection("users").document(userID);
        if(userDocRef == null){
            progress.dismiss();
            // user account not found
            Intent myIntent = new Intent(MainActivity.this, AuthActivity.class);
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            // starting AuthActivity
            MainActivity.this.startActivity(myIntent);
        }

        // checking if the user has some games going on
        db.collection("users")
                .whereEqualTo("userId", userID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // stop loading
                                progress.dismiss();
                                // getting matchId property from the user
                                Object matchId = document.getData().get("matchId");
                                // if user has a match going on, we send him to the recap fragment
                                if(matchId != null)
                                    navController.navigate(R.id.nav_matchrecap);
                            }
                        } else {

                            progress.dismiss();
                            // user account not found
                            Intent myIntent = new Intent(MainActivity.this, AuthActivity.class);
                            myIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            // starting AuthActivity
                            MainActivity.this.startActivity(myIntent);
                        }
                    }
                });

    }

    public void passTimeInMillis(long time) {
        timesInMillis = time;
    }

    public long getTimesInMillis() {
        return timesInMillis;
    }

    /*
    * these 2 methods will be used later in different fragments to get user's data
    * */
    public String getUserID() { return userID; }
    public String getUserEmail() { return userEmail; }
    public String getUsername() { return  username; }

    // needed to hide some menu's options in some fragments
    public NavigationView getNavigation() { return navigationView; }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

}