package com.acim.walk.ui.searchmatch;

import android.util.Log;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.acim.walk.Model.Match;
import com.acim.walk.Model.User;
import com.acim.walk.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.type.DateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;

public class SearchMatchViewModel extends ViewModel {

    private final String TAG = "SearchMatchViewModel";

    private FirebaseFirestore db;

    private Boolean hasMatch = false;

    public SearchMatchViewModel() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Check if matchId field in user document in the db has a value. If yes that means user has
     * joined a match, otherwise host hasn't started the match yet
     * @param userId
     * @return return true if user participates a match
     */
    public Boolean checkForMatchParticipation(String userId){
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Object userMatchIdObj = document.getData().get("matchId");
                        if(userMatchIdObj != null && userMatchIdObj.toString() != null && userMatchIdObj.toString() != "")
                        {
                            Log.d(TAG, "ENTRA " + userMatchIdObj.toString());
                            hasMatch = true;
                        }
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
        return hasMatch;
    }

}
