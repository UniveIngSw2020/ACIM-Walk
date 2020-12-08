package com.acim.walk.ui.searchmatch;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.app.PendingIntent;
import android.content.Intent;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.telephony.SmsMessage;

import com.acim.walk.DTO.UserDTO;
import com.acim.walk.ui.home.HomeViewModel;
import com.acim.walk.ui.newmatch.NewmatchFragment;
import com.google.android.gms.common.api.GoogleApiClient;

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
import com.google.android.gms.nearby.messages.MessagesClient;
import com.google.android.gms.nearby.messages.MessagesOptions;
import com.google.android.gms.nearby.messages.NearbyPermissions;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.StatusCallback;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.tasks.Task;
import com.acim.walk.MainActivity;
import com.acim.walk.R;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;

import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.auth.User;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SearchMatchFragment extends Fragment {

    private SearchMatchViewModel searchMatchViewModel;

    private TextView userIdTxt;
    private Button startMatchBtn;

    private MessageListener idsListener;
    private Message userIdMessage;

    // listview reference
    private ListView opponentsList;
    // list view data source
    private ArrayAdapter<String> adapter;

    private HashSet<UserDTO> opponents = new HashSet<>();;

    //private ArrayList<String> opponentsIds = new ArrayList<>();
    //private ArrayList<String> opponentsEmails = new ArrayList<>();
    private String userId = null;
    // user's email
    private String userEmail = null;

    private long timeInMillis = 10000;

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
                System.out.println("ID RICEVUTO: " + new String(message.getContent()));


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
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                // now we have a JSON object, we just need to access the values we need
                String receivedId = "";
                String receivedEmail = "";
                String receivedUsername = "";
                Integer receivedSteps = 0;
                UserDTO receivedUser = null;
                try {
                    // getting access to the values of the JSON object
                    receivedId = receivedObject.getString("userId");
                    receivedEmail = receivedObject.getString("userEmail");
                    receivedUsername = receivedObject.getString("username");
                    receivedSteps = receivedObject.getInt("userSteps");
                    receivedUser = new UserDTO(receivedId, receivedEmail, receivedUsername, receivedSteps);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                opponents.add(receivedUser);
                /*
                // checking if we have already received this message
                if(!opponents.contains(receivedUser)) {
                    // new opponent found
                    opponents.add(receivedUser);
                    // new data is available for the listview to display
                    adapter.notifyDataSetChanged();
                }*/
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
        searchMatchViewModel =
                new ViewModelProvider(this).get(SearchMatchViewModel.class);

        View root = inflater.inflate(R.layout.fragment_searchmatch, container, false);

        // Get userID and userEmail from MainActivity function, getUserID(), getUserEmail()
        MainActivity activity = (MainActivity)getActivity();
        userId = activity != null ? activity.getUserID() : "NaN";
        userEmail = activity != null ? activity.getUserEmail() : "NaN";

        userIdTxt = root.findViewById(R.id.userID_text);
        startMatchBtn = root.findViewById(R.id.startNewMatch_btn);

        startMatchBtn.setEnabled(false);

        opponents.add(new UserDTO(userEmail, userId, "", 0));

        // setting up arguments to pass to listview
        ArrayList<String> userList = new ArrayList<>();
        for(UserDTO user : opponents){
            userList.add(user.email);
        }
        // setting up listview
        opponentsList = (ListView) root.findViewById(R.id.opponents_list);
        // setting up listview adapter
        adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_list_item_1, android.R.id.text1, userList);
        // appending data source to listview
        opponentsList.setAdapter(adapter);

        // this JSON object will store all user's info
        JSONObject userInfo = new JSONObject();
        try {
            userInfo.put("userId", userId);
            userInfo.put("userEmail", userEmail);
            //TODO: add username
            userInfo.put("username", "");
            userInfo.put("stepsNumber", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // sending JSON object to nearby devices
        publish(userInfo.toString());
        // Show on screen own user ID
        userIdTxt.setText(userId);

        startMatchBtn.setOnClickListener(x -> {

            EditText gameTime = root.findViewById(R.id.gameDuration_time);
            timeInMillis = searchMatchViewModel.gameTimeInMilliseconds(gameTime);

            searchMatchViewModel.createMatch(opponents, String.valueOf(gameTime.getText()));

            // Send timer to NewMatchFragment
            MainActivity main = (MainActivity) getActivity();
            main.passTimeInMillis(timeInMillis);

            // Go to match page
            NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            NavController navController = navHostFragment.getNavController();
            navController.navigate(R.id.nav_newmatch);
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
        unpublish();
        unsubscribe();

        super.onStop();
    }


    public void publish(String message) {
        Log.i("SC: ", "Publishing: " + message);

        userIdMessage = new Message(message.getBytes());
        Nearby.getMessagesClient(getActivity()).publish(userIdMessage);

    }

    // Only use if someone wants to unpublish a message from the chatroom
    public void unpublish() {
        Log.i("SC: ", "Unpublishing...");
        if (userIdMessage != null) {
            Nearby.getMessagesClient(getActivity()).unpublish(userIdMessage);
            userIdMessage = null;
        }
    }

    private void subscribe(){
        Log.i("SC:", "Subscribing...");

        Nearby.getMessagesClient(getActivity()).subscribe(idsListener, options)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getActivity(), "Successfully subcribed!", Toast.LENGTH_LONG).show();
                    }
                });

    }
    private void unsubscribe() {
        Log.i("SC:", "Unsubscribing...");
        Nearby.getMessagesClient(getActivity()).unsubscribe(idsListener);
    }
}