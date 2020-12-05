package com.acim.walk.ui.searchmatch;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.app.PendingIntent;
import android.content.Intent;
import androidx.lifecycle.ViewModelProvider;
import android.telephony.SmsMessage;

import com.acim.walk.ui.home.HomeViewModel;
import com.google.android.gms.common.api.GoogleApiClient;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SearchMatchFragment extends Fragment {

    private SearchMatchViewModel searchMatchViewModel;

    private TextView userIdTxt;
    private Button startMatchBtn;

    private MessageListener idsListener;
    private Message userIdMessage;
    private View root;

    private ArrayList<String> opponentsIds = new ArrayList<>();
    private String userId = null;

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
                opponentsIds.add(new String(message.getContent()));
            }

            @Override
            public void onLost(Message message) {
                System.out.println("ERROR: " + new String(message.getContent()));
                opponentsIds.add(new String(message.getContent()));
            }
        };
        subscribe();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        searchMatchViewModel =
                new ViewModelProvider(this).get(SearchMatchViewModel.class);

        View root = inflater.inflate(R.layout.fragment_home, container, false);
        root = inflater.inflate(R.layout.fragment_searchmatch, container, false);

        userIdTxt = root.findViewById(R.id.userID_text);
        startMatchBtn = root.findViewById(R.id.startNewMatch_btn);

        // Get userID from MainActivity function, getUserID()
        MainActivity activity = (MainActivity)getActivity();
        userId = activity != null ? activity.getUserID() : "NaN";
        publish(userId);
        // Show on screen own user ID
        userIdTxt.setText(userId);


        startMatchBtn.setOnClickListener(x -> {
            searchMatchViewModel.createMatch(userId, opponentsIds);
        });

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        Nearby.getMessagesClient(getActivity()).publish(userIdMessage);
        Nearby.getMessagesClient(getActivity()).subscribe(idsListener);
    }

    @Override
    public void onStop() {
        // Close the methods publish and subscribe, so close Nearby function
        Nearby.getMessagesClient(getActivity()).unpublish(userIdMessage);
        Nearby.getMessagesClient(getActivity()).unsubscribe(idsListener);

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