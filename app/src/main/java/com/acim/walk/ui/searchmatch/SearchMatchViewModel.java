package com.acim.walk.ui.searchmatch;

import android.icu.text.SymbolTable;
import android.util.Log;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.acim.walk.DTO.UserDTO;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SearchMatchViewModel extends ViewModel {

    private FirebaseFirestore db;

    private MutableLiveData<String> mText;


    public SearchMatchViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Ricerca avversario :)");

        db = FirebaseFirestore.getInstance();
    }

    public LiveData<String> getText() {
        return mText;
    }

    public void createMatch(Collection<UserDTO> participants, String gameTime) {

        final DocumentReference matchesDocRef = db.collection("match").document();


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault());
        String currentDateandTime = sdf.format(new Date());
        ArrayList<UserDTO>participantsList = new ArrayList<>(participants);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("participants", participantsList);
        attributes.put("isOver", false);
        attributes.put("time_start", currentDateandTime);

        //TODO: transazione con inserimento dell'id partita su ciascun utente del db
        //transaction.set(matchesDocRef,attributes);

        matchesDocRef
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

    // Function that set the correct string that contains the timer
    public int gameTimeInMilliseconds(EditText gameTime) {
        String time = String.valueOf(gameTime.getText());
        int timeInMillis = 0;

        String[] times = time.split(":");

        timeInMillis = (Integer.parseInt(times[0]) * 60);

        if (times.length != 1) {
            timeInMillis += Integer.parseInt(times[1]);
        }

        return timeInMillis * 1000;
    }
}
