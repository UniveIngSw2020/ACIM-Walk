package com.acim.walk.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.acim.walk.MainActivity;
import com.acim.walk.R;
import com.acim.walk.ui.CloseAppDialog;
import com.google.android.material.navigation.NavigationView;


public class HomeFragment extends Fragment /*implements SensorEventListener2*/ {
    /*
    * HomeFragment Fields.
    *
    * */
    private final String TAG = "HomeFragment";
    private Button newMatchBtn, searchMatchBtn;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        /*
         * Callback used when user press go back button. In this case user can go back to previous page
         * but he can only close application. So when user press go back button a dialog will be opened
         * and ask to user if he wants to close application
         *
         */
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                // Handle the back button even
                Log.d("BACKBUTTON", "Back button clicks");

                CloseAppDialog closeAppDialog = new CloseAppDialog();
                closeAppDialog.show(getActivity().getSupportFragmentManager(), "");
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        /*
        * Make assignments.
        * newMatchBtn and searchMatchBtn are associated with the two buttons that are inside
        * fragment_home.
        * homeViewModel call HomeViewModel class, that contains util functions.
        * */

        View root = inflater.inflate(R.layout.fragment_home, container, false);
        newMatchBtn = root.findViewById(R.id.home_createMatch_button);
        searchMatchBtn = root.findViewById(R.id.home_searchMatch_button);

        /*
        * Hiding 'Home' inside menu option
        * */
        MainActivity activity = (MainActivity)getActivity();
        NavigationView nav = activity.getNavigation();
        nav.getMenu().findItem(R.id.nav_home).setVisible(false);
        nav.getMenu().findItem(R.id.nav_settings).setVisible(true);

        /*
        * Set username on HeaderView, inside menu
        * */
        View headerView = nav.getHeaderView(0);
        TextView username = (TextView) headerView.findViewById(R.id.navUsername);
        username.setText(activity.getUsername());

        /*
        * Set event when user click "Cerca Partita".
        * Take user to fragment_searchmatch, where user can search a match.
        * */
        searchMatchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                NavController navController = navHostFragment.getNavController();
                navController.navigate(R.id.search_opponent_layout);
            }
        });

        /*
         * Set event when user click "Cerca Partita".
         * Take user to fragment_newmatch, where user can start a match and choose game time.
         * */
        newMatchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                NavController navController = navHostFragment.getNavController();
                navController.navigate(R.id.nav_newmatch);
            }
        });

        return root;
    }
}