package com.acim.walk.ui.newmatch;

import android.util.Log;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.acim.walk.Model.Match;
import com.acim.walk.Model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewMatchViewModel extends ViewModel {

    private final String TAG = "NewMatchViewModel";

    private FirebaseFirestore db;

    public NewMatchViewModel() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Method used by the host of a match to effectively start a new match
     * @param participants Collection of User objects which are the participants of the game
     * @param endDate end moment of the match
     * @return String containing the created match id
     */
    public String createMatch(Collection<User> participants, Date endDate) {

        //Retrieves starting moment of the match
        Date startDate = Calendar.getInstance().getTime();
        //Converts input collection to List (collection is not serializable)
        List<User> usersList = new ArrayList<>(participants);

        //Gets a new write batch
        WriteBatch batch = db.batch();

        //Creates a new match document assigning to it a random id
        DocumentReference newMatchRef = db.collection("matches").document();

        //Map containing match participants
        HashMap<String, User> participantsMap = new HashMap<>();
        for(User user : participants){
            participantsMap.put(user.getUserId(), user);
        }

        //Map containing match details
        HashMap<String, Object> matchDetails = new HashMap<>();
        matchDetails.put("endDate", endDate);
        matchDetails.put("matchId", newMatchRef.getId());
        matchDetails.put("startDate", startDate);

        //generates the match document
        batch.set(newMatchRef, matchDetails);
        //adds to the previously created match document a collection of users
        CollectionReference participantsCollection = newMatchRef.collection("participants");

        for (Map.Entry<String, User> entry : participantsMap.entrySet()) {
            //Adds each participant to the collection of user of the created match
            DocumentReference newParticipantRef = participantsCollection.document(entry.getKey());
            batch.set(newParticipantRef, entry.getValue());
        }

        // In the same "transaction" updates the current match reference to users
        for(User user : participants){
            DocumentReference userRef = db.collection("users").document(user.getUserId());
            batch.update(userRef, "matchId", newMatchRef.getId());
        }

        // Commits the batch
        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG, "Match created and users updated!");
            }
        });

        return newMatchRef.getId();
    }

    /**
     * Function that retrieve the timestamp when the game will end
     * @param gameTime
     * @return
     */
    public Date getEndDate(EditText gameTime) {
        Date currentDate = Calendar.getInstance().getTime();
        long timer = currentDate.getTime();

        timer += gameTimeInMilliseconds(gameTime);

        currentDate.setTime(timer);

        return currentDate;
    }

    /**
     * Function that set the correct string that contains the timer in milliseconds
     * @param gameTime
     * @return
     */
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