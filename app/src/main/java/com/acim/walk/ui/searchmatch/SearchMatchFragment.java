package com.acim.walk.ui.searchmatch;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.acim.walk.Model.User;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.acim.walk.MainActivity;
import com.acim.walk.R;
import com.acim.walk.SensorListener;
import com.acim.walk.Util;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;

import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

public class SearchMatchFragment extends Fragment {

    private final String TAG = "SearchMatchFragment";

    //constants with JSON object fields names
    private final String JSON_ID = "userId";
    private final String JSON_EMAIL = "userEmail";
    private final String JSON_USERNAME = "username";
    private final String JSON_IS_HOST = "isHost";
    private final String JSON_IS_LEAVING_GAME = "isLeaving";

    private SearchMatchViewModel searchMatchViewModel;

    private MessageListener idsListener; //Message listener object for Nearby Messages
    private Message userIdMessage; //Nearby message object

    // An ExecutorService that can schedule commands to run after a given delay, or to execute periodically
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    //listview reference
    private ListView opponentsList;
    //list containing usernames of found users
    ArrayList<String> userList = new ArrayList<>();
    // list view data source
    private ArrayAdapter<String> adapter;

    private HashSet<User> participants = new HashSet<>(); //set containing User object of nearby users

    //current user information fields
    private String userId = null;
    private String username = null;
    private String userEmail = null;

    //option for Nearby Messages listener
    private final SubscribeOptions options = new SubscribeOptions.Builder()
            .setStrategy(Strategy.DEFAULT)
            .build();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a message listener, used with Nearby subscribe/unsubscribe
        idsListener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                Log.d(TAG, new String(message.getContent()));
                /*
                 *
                 * the JSON string will be something like:
                 * {"userId":"TuUU5TvJC6MgcBuagjYkNFIAOk82","userEmail":"mr@mail.com",...}
                 * so here we convert the string to a JSON object
                 *
                 * */
                JSONObject receivedObject = null;
                try {
                    // converting string to JSON object
                    receivedObject = new JSONObject(new String(message.getContent()));

                    // now we have a JSON object, we just need to access the values we need
                    // getting access to the values of the JSON object
                    String receivedId = receivedObject.getString(JSON_ID);
                    String receivedEmail = receivedObject.getString(JSON_EMAIL);
                    String receivedUsername = receivedObject.getString(JSON_USERNAME);
                    User receivedUser = new User(receivedEmail, receivedId, receivedUsername);

                    //check if the received user has already been received. If yes do not add him
                    Boolean toAdd = true;
                    for (User user : participants) {
                        if (user.getUserId().equals(receivedId)) {
                            toAdd = false;
                        }
                    }
                    if (toAdd) {
                        participants.add(receivedUser);
                        userList.add(receivedUsername);
                        adapter.notifyDataSetChanged(); //updates the listview
                    }

                } catch (JSONException e) {

                }
            }

            @Override
            public void onLost(Message message) {
               Log.d(TAG, "ERROR: " + new String(message.getContent()));
            }
        };
        subscribe();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        searchMatchViewModel =
                new ViewModelProvider(this).get(SearchMatchViewModel.class);

        View root = inflater.inflate(R.layout.fragment_searchmatch, container, false);

        // Get user info from MainActivity function and create a new User object
        MainActivity activity = (MainActivity) getActivity();
        userId = activity != null ? activity.getUserID() : "NaN";
        userEmail = activity != null ? activity.getUserEmail() : "NaN";
        username = activity != null ? activity.getUsername() : "NaN";
        User currentUser = new User(userEmail, userId, username, 0);

        participants.add(currentUser); //current user participates the match if it will be started

        //setting up arguments to pass to listview
        userList.clear();
        for (User user : participants) {
            userList.add(user.getUsername() + " (tu)");
        }
        // setting up listview
        opponentsList = (ListView) root.findViewById(R.id.opponents_list);
        userList.remove(null);
        //setting up listview adapter
        adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_list_item_1, android.R.id.text1, userList);
        //appending data source to listview
        opponentsList.setAdapter(adapter);

        //this JSON object will store all user's info and it will be sent to other players using nearby
        JSONObject userInfo = new JSONObject();
        try {
            userInfo.put(JSON_ID, userId);
            userInfo.put(JSON_EMAIL, userEmail);
            userInfo.put(JSON_USERNAME, username);
            userInfo.put(JSON_IS_HOST, false);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //sending JSON object to nearby devices
        publish(userInfo.toString());

        //task which periodically verifies if user who is searching for a match ha join to one. This
        //is because Nearby Messages are not very reliable
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if(searchMatchViewModel.checkForMatchParticipation(userId)) {

                    //match has started, so sets up counters, starts the service and navigates to
                    //the recap fragment
                    getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit().putBoolean("matchFinished", false).apply();

                    getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                            .putInt("savedSteps", 0).apply();

                    getActivity().startForegroundService(new Intent(getActivity(), SensorListener.class));

                    scheduler.shutdown();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                            NavController navController = navHostFragment.getNavController();
                            navController.navigate(R.id.nav_matchrecap);
                        }
                    });
                }
            }
        }, 2, 2, TimeUnit.SECONDS);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (searchMatchViewModel.checkForMatchParticipation(userId)) {
                    getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit().putBoolean("matchFinished", false).apply();
                    getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                            .putInt("savedSteps", 0).apply();
                    getActivity().startForegroundService(new Intent(getActivity(), SensorListener.class));
                    scheduler.shutdown();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                            NavController navController = navHostFragment.getNavController();
                            navController.navigate(R.id.nav_matchrecap);
                        }
                    });
                }
            }
        }, 2, 2, TimeUnit.SECONDS);

    }

    @Override
    public void onStop() {

        /*
         * user IS LEAVING this game invitation.
         * a different message format is needed to let the HOST know.
         * we just add a 'IS_LEAVING_GAME' flag that the HOST will check on each message
         */
        JSONObject userInfo = new JSONObject();
        try {
            userInfo.put(JSON_ID, userId);
            userInfo.put(JSON_EMAIL, userEmail);
            userInfo.put(JSON_USERNAME, username);
            userInfo.put(JSON_IS_HOST, false);
            userInfo.put(JSON_IS_LEAVING_GAME, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //sending JSON object to nearby devices
        publish(userInfo.toString());



        // Close the methods publish and subscribe, so close Nearby function
        unpublish();
        unsubscribe();
        scheduler.shutdown(); //stops checking if user has join a game
        super.onStop();
    }

    /**
     * Used to publish a Nearby message
     * @param message Nearby message (containing the true message)
     */
    public void publish(String message) {
        Log.i(TAG, "Publishing: " + message);

        userIdMessage = new Message(message.getBytes());
        Nearby.getMessagesClient(getActivity()).publish(userIdMessage);
    }

    /**
     * Used to stop sending Nearby messages
     */
    public void unpublish() {
        Log.i(TAG, "Unpublishing...");
        if (userIdMessage != null) {
            Nearby.getMessagesClient(getActivity()).unpublish(userIdMessage);
            userIdMessage = null;
        }
    }

    /**
     * By calling this method user is able to receive Nearby messages
     */
    private void subscribe() {
        Log.i(TAG, "Subscribing...");

        Nearby.getMessagesClient(getActivity()).subscribe(idsListener, options)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getActivity(), "Successfully subcribed!", Toast.LENGTH_LONG).show();
                    }
                });

    }

    /**
     * By calling this method user won't be able to get Nearby messages
     */
    private void unsubscribe() {
        Log.i(TAG, "Unsubscribing...");
        Nearby.getMessagesClient(getActivity()).unsubscribe(idsListener);
    }
}