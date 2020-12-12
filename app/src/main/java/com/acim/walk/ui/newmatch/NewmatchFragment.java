package com.acim.walk.ui.newmatch;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.acim.walk.MainActivity;
import com.acim.walk.Model.User;
import com.acim.walk.R;
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
import java.util.HashSet;

public class NewmatchFragment extends Fragment {

    private final String TAG = "NewMatchFragment";

    private final String JSON_USER_ID = "userId";
    private final String JSON_EMAIL = "userEmail";
    private final String JSON_USERNAME ="username";
    private final String JSON_MATCH_ID = "matchId";

    private NewMatchViewModel newMatchViewModel;

    private TextView userIdTxt;
    private Button startMatchBtn;

    private MessageListener idsListener;
    private Message userIdMessage;

    // listview reference
    private ListView opponentsList;
    ArrayList<String> userList = new ArrayList<>();
    // list view data source
    private ArrayAdapter<String> adapter;

    private HashSet<User> participants = new HashSet<>();;

    private String userId = null;
    private String username = null;
    private String userEmail = null;
    private Boolean canHost = true;

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
                 * {"userId":"TuUU5TvJC6MgcBuagjYkNFIAOk82","userEmail":"mr@mail.com"}
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
                    User receivedUser = new User(receivedEmail, receivedId, receivedUsername);

                    Boolean toAdd = true;
                    for (User user : participants) {
                        if (user.getUserId().equals(receivedId)) {
                            toAdd = false;
                        }
                    }
                    if (toAdd) {
                        participants.add(receivedUser);
                        userList.add(receivedUsername);
                        adapter.notifyDataSetChanged();
                    }

                } catch (JSONException e) {
                    // the received message contains the new match id
                    canHost = false;
                }
            }

            @Override
            public void onLost(Message message) {
                System.out.println("ERROR: " + new String(message.getContent()));
                //opponentsIds.add(new String(message.getContent()));
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

        // Get userID and userEmail from MainActivity function, getUserID(), getUserEmail()
        MainActivity activity = (MainActivity)getActivity();
        userId = activity != null ? activity.getUserID() : "NaN";
        userEmail = activity != null ? activity.getUserEmail() : "NaN";
        User currentUser = new User(userEmail, userId, username, 0);

        userIdTxt = root.findViewById(R.id.userID_text);
        startMatchBtn = root.findViewById(R.id.startNewMatch_btn);

        startMatchBtn.setEnabled(false);

        participants.add(currentUser);

        // setting up arguments to pass to listview
        userList.clear();
        for(User user : participants){
            userList.add(user.getUsername());
        }
        // setting up listview
        opponentsList = (ListView) root.findViewById(R.id.opponents_list);
        userList.remove(null);
        // setting up listview adapter
        adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_list_item_1, android.R.id.text1, userList);
        // appending data source to listview
        opponentsList.setAdapter(adapter);

        // this JSON object will store all user's info
        JSONObject userInfo = new JSONObject();
        try {
            userInfo.put(JSON_USER_ID, userId);
            userInfo.put(JSON_EMAIL, userEmail);
            userInfo.put(JSON_USERNAME, username);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // sending JSON object to nearby devices
        publish(userInfo.toString());
        // Show on screen own user ID
        userIdTxt.setText(userId);

        startMatchBtn.setOnClickListener(x -> {
            if(canHost) {
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
                publish(matchInfo.toString());

                // go to match recap page
                NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                NavController navController = navHostFragment.getNavController();
                navController.navigate(R.id.nav_matchrecap);
            }
            else{
                Util.toast(getActivity(), "Qualcuno sta cercando di creare una gara con gli stessi partecipanti!", false);
            }
        });

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Method used to check if user has insert a duration for his game
        EditText gameTime = getActivity().findViewById(R.id.gameDuration_time);
        gameTime.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.toString().trim().length() == 0) {
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
        //unpublish();
        unsubscribe();
        super.onStop();
    }


    public void publish(String message) {
        Log.i(TAG, "Publishing: " + message);

        userIdMessage = new Message(message.getBytes());
        Nearby.getMessagesClient(getActivity()).publish(userIdMessage);

    }

    // Only use if someone wants to unpublish a message from the chatroom
    public void unpublish() {
        Log.i(TAG, "Unpublishing...");
        if (userIdMessage != null) {
            Nearby.getMessagesClient(getActivity()).unpublish(userIdMessage);
            userIdMessage = null;
        }
    }

    private void subscribe(){
        Log.i(TAG, "Subscribing...");

        Nearby.getMessagesClient(getActivity()).subscribe(idsListener, options)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getActivity(), "Successfully subcribed!", Toast.LENGTH_LONG).show();
                    }
                });

    }
    private void unsubscribe() {
        Log.i(TAG, "Unsubscribing...");
        Nearby.getMessagesClient(getActivity()).unsubscribe(idsListener);
    }
}
