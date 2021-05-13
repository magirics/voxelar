package com.voxelar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class BottomSheet extends BottomSheetDialogFragment {

    protected BottomSheetListener mlistener;
    View main_layout;

    BottomSheet() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        main_layout = inflater.inflate(R.layout.fragment_bottom_sheet, container, false);

        return main_layout;


    }

    @Override
    public void onStart() {
        super.onStart();

        FrameLayout bottomSheet = getDialog().findViewById(R.id.design_bottom_sheet);
        final BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);

        Button btn_delete = main_layout.findViewById(R.id.delete);
        Button btn_import = main_layout.findViewById(R.id.import_project);
        Button btn_changeName = main_layout.findViewById(R.id.change_name);

        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mlistener.onClickDelete(v);
                behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });
        btn_import.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mlistener.onClickImport(v);
                behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });
        btn_changeName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mlistener.onClickChangeName(v);
                behavior.setState(BottomSheetBehavior.STATE_HIDDEN);

            }
        });
    }

    void setListener(BottomSheetListener listener) {
        mlistener = listener;
    }
}

interface BottomSheetListener {
    void onClickDelete(View v);
    void onClickImport(View v);
    void onClickChangeName(View v);
}
