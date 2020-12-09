package com.acim.walk.ui.searchmatch;

import android.util.Log;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.acim.walk.Model.Match;
import com.acim.walk.Model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SearchMatchViewModel extends ViewModel {

    private final String TAG = "SearchMatchViewModel";

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

    public void createMatch(Collection<User> participants, Date endDate) {

        // Retrieves starting moment of the match
        Date startDate = Calendar.getInstance().getTime();
        // Convert input collection to List (collection is not serializable)
        List<User> usersList = new ArrayList<>(participants);

        // Get a new write batch
        WriteBatch batch = db.batch();

        // Creates a new match document assigning to it a random id
        DocumentReference newMatchRef = db.collection("matches").document();
        batch.set(newMatchRef, new Match(newMatchRef.getId(), startDate, endDate, usersList ));

        // In the same "transaction" updates the current match reference to users
        for(User user : participants){
            DocumentReference userRef = db.collection("users").document(user.getUserId());
            batch.update(userRef, "matchId", newMatchRef.getId());
        }

        // Commit the batch
        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG, "Match created and users updated!");
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
