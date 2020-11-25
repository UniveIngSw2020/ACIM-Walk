package com.acim.walk.ui.home;

import android.hardware.SensorManager;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<String> mText;


    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Fragment Home :D");
    }

    public LiveData<String> getText() {
        return mText;
    }
}