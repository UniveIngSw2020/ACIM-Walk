package com.acim.walk.ui.searchmatch;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.acim.walk.R;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.tasks.Task;

import java.util.Random;

public class SearchMatchFragment extends Fragment {

    private SearchMatchViewModel searchMatchViewModel;
    private MessageListener myMessageListener;
    private Message myMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        searchMatchViewModel =
                new ViewModelProvider(this).get(SearchMatchViewModel.class);
        // Inflate the layout for this fragment

        // Create the object with the correct methods
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

        // Message contains a random value
        int randomValue = new Random().nextInt(10);
        myMessage = new Message(Integer.toString(randomValue).getBytes());

        return inflater.inflate(R.layout.fragment_searchmatch, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        Nearby.getMessagesClient(super.requireActivity()).publish(myMessage);
        Task<Void> value = Nearby.getMessagesClient(super.requireActivity()).subscribe(myMessageListener);
        // final TextView opponentsID = root.findViewById(R.id.opponent_id);
        // opponentsID.setText(value.toString());
    }

    @Override
    public void onStop() {

        Nearby.getMessagesClient(super.requireActivity()).unpublish(myMessage);
        Nearby.getMessagesClient(super.requireActivity()).unsubscribe(myMessageListener);

        super.onStop();
    }
}