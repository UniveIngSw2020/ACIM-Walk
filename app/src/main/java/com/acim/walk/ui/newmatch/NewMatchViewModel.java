package com.acim.walk.ui.newmatch;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class NewMatchViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public NewMatchViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Fragment Nuova partita :)");
    }

    public LiveData<String> getText() {
        return mText;
    }
}