package com.acim.walk.ui.searchmatch;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SearchMatchViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public SearchMatchViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Ricerca avversario :)");
    }

    public LiveData<String> getText() {
        return mText;
    }

}
