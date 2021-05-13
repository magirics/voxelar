package com.voxelar;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class ImportDialog extends DialogFragment {

    ExportDialogListener listener;
    View view;
    String title;

    ImportDialog(String title) {
        this.title = title;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.dialog_import, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Export " + title)
                .setView(view)
                .setPositiveButton("Export", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onExport();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onCancel();
                    }
                });

        return builder.create();
    }

    public float getScale() {
        return Float.parseFloat(((EditText)view.findViewById(R.id.stl_scale)).getText().toString());
    }

    public void setListener(ExportDialogListener listener) {
        this.listener = listener;
    }

    interface ExportDialogListener {
        void onExport();
        void onCancel();
    }
}
