package com.acim.walk.ui.searchmatch;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SearchMatchFragment extends Fragment {

    private TextView userIdTxt;
    private Button startMatchBtn;

    private MessageListener idsListener;
    private Message userIdMessage;
    private View root;

    private String opponentId = null;
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
                opponentId = new String(message.getContent());
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
        userId = activity != null ? activity.getUserID() : "NaN";
        publish(userId);
        // Show on screen own user ID
        userIdTxt.setText(userId);

        // Create a message that contains user ID, used with Nearby publish/unpublish
        userIdMessage = new Message(userId.getBytes());

        startMatchBtn.setOnClickListener(x -> {
            userIdMessage = new Message(userId.getBytes());
            Nearby.getMessagesClient(getActivity()).publish(userIdMessage);


            FirebaseFirestore dbFirestore = FirebaseFirestore.getInstance();
            DocumentReference currentUserDocRef = dbFirestore.collection("match").document();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault());
            String currentDateandTime = sdf.format(new Date());

            Map<String, String> attributes = new HashMap<>();
            attributes.put("id1", userId);
            attributes.put("id2", opponentId);
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