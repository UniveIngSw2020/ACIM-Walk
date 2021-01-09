package com.acim.walk.ui.faq;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.acim.walk.MainActivity;
import com.acim.walk.R;
import com.acim.walk.Util;
import com.google.android.material.navigation.NavigationView;

public class FaqFragment extends Fragment {

    private FaqViewModel faqViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        faqViewModel = new ViewModelProvider(this).get(FaqViewModel.class);

        View root = inflater.inflate(R.layout.fragment_faq, container, false);

        //open AlertDialog on click to see the answer
        root.findViewById(R.id.first_question).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Util.showErrorAlert(getContext(), "Giocare", Util.FIRST_ANSWER);
            }
        });
        root.findViewById(R.id.second_question).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Util.showErrorAlert(getContext(), "Recupero Password", Util.SECOND_ANSWER);
            }
        });
        root.findViewById(R.id.third_question).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Util.showErrorAlert(getContext(), "Aggiornamento Password", Util.THIRD_ANSWER);
            }
        });
        root.findViewById(R.id.fouth_question).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Util.showErrorAlert(getContext(), "Eliminazione Account", Util.FOURTH_ANSWER);
            }
        });

        //set visible
        MainActivity activity = (MainActivity)getActivity();
        NavigationView nav = activity.getNavigation();
        nav.getMenu().findItem(R.id.nav_home).setVisible(true);
        nav.getMenu().findItem(R.id.nav_settings).setVisible(true);

        return root;
    }
}
