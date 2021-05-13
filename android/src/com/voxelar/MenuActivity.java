package com.voxelar;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;

public class MenuActivity extends AppCompatActivity {

    LinearLayout layout;

    File directory;
    File[] thumbnails;
    String[] names;

    TextView global_tv;
    String globalProjectName;
    float globalScale = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        layout = findViewById(R.id.main_layout);

        directory = getExternalFilesDir(null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        layout.removeAllViews();

        thumbnails = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(".png")) return true;
                return false;
            }
        });
        names = new String[thumbnails.length];
        for (int i = 0; i < thumbnails.length; i++) {
            names[i] = thumbnails[i].getName().replace(".png", "");
        }

        for (final File thumbnail : thumbnails) {
            final View card_project = LayoutInflater.from(MenuActivity.this).inflate(R.layout.view_project, layout, false);
            final TextView tv_project = card_project.findViewById(R.id.name);
            ImageView img_project = card_project.findViewById(R.id.thumbnail);

            final String name = thumbnail.getName().replace(".png", "");
            tv_project.setText(name);
            img_project.setImageBitmap(BitmapFactory.decodeFile(thumbnail.getAbsolutePath()));

            img_project.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MenuActivity.this, CreateActivity.class);
                    intent.putExtra("name", tv_project.getText());
                    MenuActivity.this.startActivity(intent);
                }
            });

            img_project.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    globalProjectName = tv_project.getText().toString();

                    BottomSheet bottomSheet = new BottomSheet();
                    bottomSheet.show(MenuActivity.this.getSupportFragmentManager(), "ModalBottomSheet");

                    bottomSheet.setListener(new BottomSheetListener() {
                        @Override
                        public void onClickDelete(View v) {
                            deleteConfirmationMessage(tv_project.getText().toString(), card_project);
                        }

                        @Override
                        public void onClickImport(View v) {
                            importProject(tv_project.getText().toString());
                        }

                        @Override
                        public void onClickChangeName(View v) {
                            global_tv = tv_project;
                            rename();
                        }
                    });

                    return true;
                }
            });

            tv_project.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    globalProjectName = tv_project.getText().toString();
                    global_tv = tv_project;
                    rename();
                }
            });

            layout.addView(card_project);
        }
    }

    void rename() {
        final RenameDialog dialog = new RenameDialog(globalProjectName);
        dialog.show(getSupportFragmentManager(), "rename_dialog");

        dialog.setListener(new RenameDialog.RenameListener() {
            @Override
            public void onSave() {
                File bin = new File(directory + "/" + globalProjectName + ".bin");
                File json = new File(directory + "/" + globalProjectName + ".json");
                File png = new File(directory + "/" + globalProjectName + ".png");

                String name = dialog.getName();
                if (!name.equals(globalProjectName)) {
                    boolean bbin = bin.renameTo(new File(directory + "/" + dialog.getName() + ".bin"));
                    boolean bjson = json.renameTo(new File(directory + "/" + dialog.getName() + ".json"));
                    boolean bpng = png.renameTo(new File(directory + "/" + dialog.getName() + ".png"));

                    global_tv.setText(name);
                }
            }

            @Override
            public void onCancel() {
                //Do nothing
            }
        });
    }

    void importProject(final String project) {
        final ImportDialog dialog = new ImportDialog(globalProjectName);
        dialog.setListener(new ImportDialog.ExportDialogListener() {
            @Override
            public void onExport() {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/octet-stream");
                intent.putExtra(Intent.EXTRA_TITLE,project + ".stl");
                startActivityForResult(intent, 1);

                globalScale = dialog.getScale();
                globalProjectName = project;
            }

            @Override
            public void onCancel() {
                //Do nothing
            }
        });



        dialog.show(getSupportFragmentManager(), "import_dialog");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();

                try {
                    ParcelFileDescriptor pdf = getContentResolver().openFileDescriptor(uri, "w");
                    FileOutputStream writer = new FileOutputStream(pdf.getFileDescriptor());
                    File source = new File(directory + "/" + globalProjectName + ".bin");
                    Exporter.toSTL(source, writer, globalScale);
                    writer.close();
                } catch (Exception ignored) {}
            }
        }
    }

    void deleteConfirmationMessage(final String project, final View v) {
        new MaterialAlertDialogBuilder(MenuActivity.this)
                .setTitle("Delete " + project)
                .setMessage("This will delete all the files related to the project.")
                .setPositiveButton("delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File[] occurrences = directory.listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                if (name.startsWith(project)) return true;
                                return false;
                            }
                        });

                        for (File file : occurrences)
                            file.delete();

                        layout.removeView(v);
                    }
                }).setNegativeButton("cancel", null)
                .show();
    }

    String getNameAvailable(String[] names) {
        String project = "untitled";
        int max = 0;

        for (String n : names) {
            if (n.length() >= "untitled".length()) {
                String num_text = n.substring(project.length());
                int num = -1;
                try {
                    num = Integer.parseInt(num_text);
                } catch (Exception ignored) {
                }

                if (num > max) max = num;
            }
        }

        project += (max + 1);
        return project;
    }

    public void onClickNew(View v) {
        Intent intent = new Intent(MenuActivity.this, CreateActivity.class);
        String name = getNameAvailable(names);
        intent.putExtra("name", name);
        startActivity(intent);
    }
}
