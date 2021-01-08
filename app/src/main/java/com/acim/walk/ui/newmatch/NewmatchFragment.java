package com.acim.walk.ui.newmatch;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.acim.walk.MainActivity;
import com.acim.walk.Model.User;
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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;


public class NewmatchFragment extends Fragment {

    private final String TAG = "NewMatchFragment";

    //constants with JSON object fields names
    private final String JSON_USER_ID = "userId";
    private final String JSON_EMAIL = "userEmail";
    private final String JSON_USERNAME = "username";
    private final String JSON_MATCH_ID = "matchId";
    private final String JSON_IS_HOST = "isHost";

    private NewMatchViewModel newMatchViewModel;

    //reference to the start button (Walk!)
    private Button startMatchBtn;

    private MessageListener idsListener; //Message listener object for Nearby Messages
    private Message matchMessage; //Nearby message object

    // listview reference
    private ListView opponentsList;
    //list containing usernames of found users
    ArrayList<String> userList = new ArrayList<>();
    // list view data source
    private ArrayAdapter<String> adapter;

    private HashSet<User> participants = new HashSet<>(); //set containing User object of nearby users
    private HashMap<String, Boolean> participantsConfirmation = new HashMap<>();

    //current user information fields
    private String userId = null;
    private String username = null;
    private String userEmail = null;
    //to avoid conflicts between users creating the same match simultaneously
    private Boolean canHost = true;

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
                    String receivedId = receivedObject.getString(JSON_USER_ID);
                    String receivedEmail = receivedObject.getString(JSON_EMAIL);
                    String receivedUsername = receivedObject.getString(JSON_USERNAME);
                    Boolean isHost = receivedObject.getBoolean(JSON_IS_HOST);
                    User receivedUser = new User(receivedEmail, receivedId, receivedUsername);

                    //check if current user has to be added in the game
                    //if he is host then do not add him. Mess otherwise
                    if(!isHost) {
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
                    }

                } catch (JSONException e) {
                    try {
                        receivedObject = new JSONObject(new String(message.getContent()));
                        String receivedId = receivedObject.getString(JSON_USER_ID);
                        participantsConfirmation.put(receivedId, true);
                    } catch (JSONException ex) {
                        //the received message contains the new match id
                        canHost = false;
                    }
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
        newMatchViewModel =
                new ViewModelProvider(this).get(NewMatchViewModel.class);

        View root = inflater.inflate(R.layout.fragment_newmatch, container, false);

        // Get user infos from MainActivity function and create a new User object
        MainActivity activity = (MainActivity) getActivity();
        userId = activity != null ? activity.getUserID() : "NaN";
        userEmail = activity != null ? activity.getUserEmail() : "NaN";
        username = activity != null ? activity.getUsername() : "NaN";
        User currentUser = new User(userEmail, userId, username, 0);


        // Disable start match button. It can be enabled only when user choose the length of the game
        startMatchBtn = root.findViewById(R.id.newmatch_start_button);
        startMatchBtn.setEnabled(false);

        // Add itself to the list of participants
        participants.add(currentUser);

        // setting up arguments to pass to listview
        userList.clear();
        for (User user : participants) {
            userList.add(user.getUsername() + " (tu)");
        }
        // setting up listview
        opponentsList = (ListView) root.findViewById(R.id.opponents_list);
        userList.remove(null);
        // setting up listview adapter
        adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_list_item_1, android.R.id.text1, userList);
        // appending data source to listview
        opponentsList.setAdapter(adapter);

        // this JSON object will store all user's info and it will be sent to other players using nearby
        JSONObject userInfo = new JSONObject();
        try {
            userInfo.put(JSON_USER_ID, userId);
            userInfo.put(JSON_EMAIL, userEmail);
            userInfo.put(JSON_USERNAME, username);
            userInfo.put(JSON_IS_HOST, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // sending JSON object to nearby devices
        publish(userInfo.toString());

        startMatchBtn.setOnClickListener(x -> {
            if (canHost) {

                // Retrieve game duration and the timestamp when the game is ended
                EditText gameTime = root.findViewById(R.id.gameDuration_time);
                Date endDate = newMatchViewModel.getEndDate(gameTime);

                String matchId = newMatchViewModel.createMatch(participants, endDate);

                // notify all the opponents the matchId so that the match has started
                JSONObject matchInfo = new JSONObject();
                try {
                    matchInfo.put(JSON_MATCH_ID, matchId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                unpublish(); //stop publishing user info
                publish(matchInfo.toString()); //publish match id instead

                //match has started so set up counters
                getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit().putBoolean("matchFinished", false).apply();

                getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                        .putInt("savedSteps", 0).apply();

                // start the service to count steps
                getActivity().startForegroundService(new Intent(getActivity(), SensorListener.class));

                // go to match recap page
                NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                NavController navController = navHostFragment.getNavController();
                navController.navigate(R.id.nav_matchrecap);

            } else {
                Util.toast(getActivity(), "Qualcuno sta cercando di creare una gara con gli stessi partecipanti!", false);
            }
        });

        return root;
    }

    /**
     * Method used to check if user has insert a duration for his game. If he has insert a duration,
     * enable the button to start match
     */
    @Override
    public void onStart() {
        super.onStart();

        EditText gameTime = getActivity().findViewById(R.id.gameDuration_time);
        gameTime.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() == 0) {
                    startMatchBtn.setEnabled(false);
                } else {
                    startMatchBtn.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    @Override
    public void onStop() {
        // Close the methods publish and subscribe, so close Nearby function
        unpublish();
        unsubscribe();
        super.onStop();
    }

    /**
     * Used to publish a Nearby message
     * @param message Nearby message (containing the true message)
     */
    public void publish(String message) {
        Log.i(TAG, "Publishing: " + message);

        matchMessage = new Message(message.getBytes());
        Nearby.getMessagesClient(getActivity()).publish(matchMessage);

    }

    /**
     * Used to stop sending Nearby messages
     */
    public void unpublish() {
        Log.i(TAG, "Unpublishing...");
        if (matchMessage != null) {
            Nearby.getMessagesClient(getActivity()).unpublish(matchMessage);
            matchMessage = null;
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
