package com.voxelar;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class RenameDialog extends DialogFragment {

    String name;
    RenameListener listener;
    EditText et;

    RenameDialog(String name) {
        this.name = name;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_change_name, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Rename")
                .setView(view)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onSave();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onCancel();
                    }
                });

        et = view.findViewById(R.id.project_name);
        et.setText(name);

        return builder.create();
    }

    public void setListener(RenameListener listener) {
        this.listener = listener;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return et.getText().toString();
    }

    interface RenameListener {
        void onSave();
        void onCancel();
    }
}
