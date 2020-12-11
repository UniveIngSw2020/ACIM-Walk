package com.acim.walk.ui.matchrecap;

import android.os.CountDownTimer;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.acim.walk.Model.User;
import com.acim.walk.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.sql.Time;
import java.util.Date;

public class MatchRecapViewModel extends ViewModel{

    private FirebaseFirestore db;

    public MatchRecapViewModel() {
        db = FirebaseFirestore.getInstance();
    }
}

