package com.acim.walk.ui.searchmatch;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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

    private SearchMatchViewModel searchMatchViewModel;
    private MessageListener myMessageListener;
    private Message myMessage;
    private View root;

    private String opponentID = null;
    private String userID = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_searchmatch, container, false);

        // Get userID from MainActivity function, getUserID()
        MainActivity activity = (MainActivity)getActivity();
        userID = activity != null ? activity.getUserID() : "NaN";

        // Show on screen own user ID
        TextView userIDTextView = (TextView) root.findViewById(R.id.userID_text);
        userIDTextView.setText(userID);

        searchMatchViewModel =
                new ViewModelProvider(this).get(SearchMatchViewModel.class);

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

        // Create a message that contains user ID, used with Nearby publish/unpublish
        myMessage = new Message(userID.getBytes());

        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(Strategy.BLE_ONLY)
                .build();

        // Methods to lets devices exchange small data. With publish, application sends a message and
        // with subscribe, application will hear if some message arrives
        Nearby.getMessagesClient(super.requireActivity()).publish(myMessage);
        Nearby.getMessagesClient(super.requireActivity()).subscribe(myMessageListener, options);

        // Inflate the layout for this fragment
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        Button startNewMatch = (Button) root.findViewById(R.id.startNewMatch_btn);
        startNewMatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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
            }
        });

    }

    @Override
    public void onStop() {

        // Close the methods publish and subscribe, so close Nearby function
        Nearby.getMessagesClient(super.requireActivity()).unpublish(myMessage);
        Nearby.getMessagesClient(super.requireActivity()).unsubscribe(myMessageListener);

        super.onStop();
    }
}