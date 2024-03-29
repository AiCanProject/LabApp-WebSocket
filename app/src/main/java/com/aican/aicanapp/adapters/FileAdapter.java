package com.aican.aicanapp.adapters;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aican.aicanapp.PDFViewer;
import com.aican.aicanapp.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private Context context;
    private List<File> fileList;
    private String activity;

    public FileAdapter(Context context, File[] files, String activity) {
        this.context = context;
        this.fileList = new ArrayList<>(Arrays.asList(files));
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.file_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File selectedFile = fileList.get(position);
        holder.textView.setText(selectedFile.getName());
        holder.imageView.setImageResource(R.drawable.ic_baseline_insert_drive_file_24);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = null;
                String path = null;
                if (activity.equals("PhExport")) {
                    path = new ContextWrapper(context).getExternalMediaDirs()[0] + File.separator + "/LabApp/Sensordata/" + selectedFile.getName();
                    file = new File(path);
                }

                if (activity.equals("EcExport")) {
                    path = new ContextWrapper(context).getExternalMediaDirs()[0] + File.separator + "/LabApp/EcSensordata/" + selectedFile.getName();
                    file = new File(path);
                }


                try {
//                    Intent mIntent = new Intent(Intent.ACTION_VIEW);
//
//                    mIntent.setDataAndType(Uri.fromFile(file), "application/pdf");
//                    mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//                    mIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                    mIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
////                    mIntent.setClassName("com.google.android.apps.docs.editors.sheets", "com.google.android.apps.docs.editors.sheets.QuickSheetDocumentOpenerActivityAlias");
//
//                    Intent cIntent = Intent.createChooser(mIntent, "Open PDF");
//                    cIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    context.getApplicationContext().startActivity(cIntent);

                    if (file.length() != 0) {
                        Intent intent = new Intent(context.getApplicationContext(), PDFViewer.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("Path", path);
                        context.getApplicationContext().startActivity(intent);
                    } else {
                        Toast.makeText(context, "File is empty", Toast.LENGTH_SHORT).show();
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                PopupMenu popupMenu = new PopupMenu(context, v);
                popupMenu.getMenu().add("DELETE");
                popupMenu.getMenu().add("SHARE");

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getTitle().equals("DELETE")) {
                            boolean deleted = selectedFile.delete();
                            if (deleted) {
                                Toast.makeText(context.getApplicationContext(), "DELETED ", Toast.LENGTH_SHORT).show();
                                fileList.remove(selectedFile);
                                notifyDataSetChanged();
                            }
                        } else if (item.getTitle().equals("SHARE")) {
                            shareFile(selectedFile);
                        }
                        return true;
                    }
                });

                popupMenu.show();
                return true;
            }
        });

        holder.shareBtn.setVisibility(View.VISIBLE);
        holder.shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareFile(selectedFile);
            }
        });
    }

    private void shareFile(File file) {
        String path = file.getAbsolutePath();
        if (file != null && file.exists()) {
            try {
                Intent intentShare = new Intent(Intent.ACTION_SEND);
                intentShare.setType("application/pdf"); // Change type to "text/csv" for CSV files
                intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intentShare.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intentShare.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intentShare.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));

                Intent chooserIntent = Intent.createChooser(intentShare, "Share file");
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(chooserIntent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Error sharing file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView imageView;
        ImageView shareBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.file_name_text_view);
            imageView = itemView.findViewById(R.id.icon_view);
            shareBtn = itemView.findViewById(R.id.shareBtn);
        }
    }
}
