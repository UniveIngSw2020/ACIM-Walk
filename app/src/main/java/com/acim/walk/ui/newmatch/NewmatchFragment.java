package com.acim.walk.ui.newmatch;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.acim.walk.R;

public class NewmatchFragment extends Fragment {

    private NewMatchViewModel newMatchViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        newMatchViewModel =
                new ViewModelProvider(this).get(NewMatchViewModel.class);
        View root = inflater.inflate(R.layout.fragment_newmatch, container, false);
        final TextView newMatchTxt = root.findViewById(R.id.text_newmatch);
        newMatchViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                newMatchTxt.setText(s);
            }
        });
        return root;
    }
}