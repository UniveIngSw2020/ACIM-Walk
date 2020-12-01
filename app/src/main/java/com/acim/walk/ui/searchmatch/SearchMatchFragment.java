package com.acim.walk.ui.searchmatch;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.acim.walk.MainActivity;
import com.acim.walk.R;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.tasks.Task;

import org.w3c.dom.Text;

import java.util.Random;

public class SearchMatchFragment extends Fragment {

    private SearchMatchViewModel searchMatchViewModel;
    private MessageListener myMessageListener;
    private Message myMessage;

    private String userID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_searchmatch, container, false);

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

    }

    @Override
    public void onStop() {

        // Close the methods publish and subscripe, so close Nearby function
        Nearby.getMessagesClient(super.requireActivity()).unpublish(myMessage);
        Nearby.getMessagesClient(super.requireActivity()).unsubscribe(myMessageListener);

        super.onStop();
    }
}