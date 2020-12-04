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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SearchMatchFragment extends Fragment {

    private SearchMatchViewModel searchMatchViewModel;
    private MessageListener myMessageListener;
    private Message myMessage;
    private View root;

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
            }

            @Override
            public void onLost(Message message) {
                System.out.println("ERROR: " + new String(message.getContent()));
            }
        };

        // Create a message that contains user ID, used with Nearby publish/unpublish
        myMessage = new Message(userID.getBytes());

        // Inflate the layout for this fragment
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Methods to lets devices exchange small data. With publish, application sends a message and
        // with subscribe, application will hear if some message arrives
        Nearby.getMessagesClient(super.requireActivity()).publish(myMessage);
        Nearby.getMessagesClient(super.requireActivity()).subscribe(myMessageListener);

        Button startNewMatch = (Button) root.findViewById(R.id.startNewMatch_btn);
        startNewMatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                FirebaseFirestore dbFirestore = FirebaseFirestore.getInstance();
                DocumentReference currentUserDocRef = dbFirestore.collection("match").document();

                String otherOpponents = "OtherID";

                Map<String, String> attributes = new HashMap<>();
                attributes.put("id1", userID);
                attributes.put("id2", otherOpponents);

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