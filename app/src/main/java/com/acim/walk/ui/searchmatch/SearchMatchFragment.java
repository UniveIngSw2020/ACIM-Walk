package com.acim.walk.ui.searchmatch;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.telephony.SmsMessage;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.acim.walk.MainActivity;
import com.acim.walk.R;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;

import com.google.android.gms.nearby.messages.MessagesClient;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.StatusCallback;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SearchMatchFragment extends Fragment {

    private TextView userIdTxt;
    private Button startMatchBtn;

    private MessageListener myMessageListener;
    private Message myMessage;
    private View root;

    private String opponentID = null;
    private String userID = null;

    private final SubscribeOptions options = new SubscribeOptions.Builder()
            .setStrategy(Strategy.DEFAULT)
            .build();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Create a message listener, used with Nearby subscribe/unsubscribe
        myMessageListener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                System.out.println("ID RICEVUTO: " + new String(message.getContent()));
                opponentID = new String(message.getContent());
            }

            @Override
            public void onLost(Message message) {
                System.out.println("ERROR: " + new String(message.getContent()));
            }
        };
        subscribe();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_searchmatch, container, false);

        userIdTxt = root.findViewById(R.id.userID_text);
        startMatchBtn = root.findViewById(R.id.startNewMatch_btn);

        // Get userID from MainActivity function, getUserID()
        MainActivity activity = (MainActivity)getActivity();
        userID = activity != null ? activity.getUserID() : "NaN";
        publish(userID);
        // Show on screen own user ID
        userIdTxt.setText(userID);

        // Create a message that contains user ID, used with Nearby publish/unpublish
        myMessage = new Message(userID.getBytes());

        startMatchBtn.setOnClickListener(x -> {
            myMessage = new Message(userID.getBytes());
            Nearby.getMessagesClient(getActivity()).publish(myMessage);


            FirebaseFirestore dbFirestore = FirebaseFirestore.getInstance();
            DocumentReference currentUserDocRef = dbFirestore.collection("match").document();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault());
            String currentDateandTime = sdf.format(new Date());

            Map<String, String> attributes = new HashMap<>();
            attributes.put("id1", userID);
            attributes.put("id2", opponentID);
            attributes.put("isOver", "N");
            attributes.put("time_start", currentDateandTime);

            currentUserDocRef
                    .set(attributes)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("TEST SCRITTURA", "DocumentSnapshot successfully written!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("TEST SCRITTURA", "Error writing document", e);
                        }
                    });

        });

        /*
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(Strategy.BLE_ONLY)
                .build();
        */
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        Nearby.getMessagesClient(getActivity()).publish(myMessage);
        Nearby.getMessagesClient(getActivity()).subscribe(myMessageListener);
    }

    @Override
    public void onStop() {
        // Close the methods publish and subscribe, so close Nearby function
        Nearby.getMessagesClient(getActivity()).unpublish(myMessage);
        Nearby.getMessagesClient(getActivity()).unsubscribe(myMessageListener);

        super.onStop();
    }


    public void publish(String message) {
        Log.i("SC: ", "Publishing: " + message);

        myMessage = new Message(message.getBytes());
        Nearby.getMessagesClient(getActivity()).publish(myMessage);

    }

    // Only use if someone wants to unpublish a message from the chatroom
    public void unpublish() {
        Log.i("SC: ", "Unpublishing...");
        if (myMessage != null) {
            Nearby.getMessagesClient(getActivity()).unpublish(myMessage);
            myMessage = null;
        }
    }

    private void subscribe(){
        Log.i("SC:", "Subscribing...");

        Nearby.getMessagesClient(getActivity()).subscribe(myMessageListener, options)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getActivity(), "Successfully subcribed!", Toast.LENGTH_LONG).show();
                    }
                });

    }
    private void unsubscribe() {
        Log.i("SC:", "Unsubscribing...");
        Nearby.getMessagesClient(getActivity()).unsubscribe(myMessageListener);
    }

}