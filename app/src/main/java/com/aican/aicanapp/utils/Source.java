package com.aican.aicanapp.utils;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.aican.aicanapp.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class Source {

    public static boolean cfr_mode = false;

    public static String WEBSOCKET_URL = "ws://192.168.4.1:81";

    public static boolean EXPORT_CSV = false;
    public static boolean EXPORT_GRAPH = false;
    public static boolean EXPORT_PDF = true;

    public static boolean SOCKET_CONNECTED = false;
    public static int calibMode = 0;
    public static int activeFragment = 0;

    public static String userId, userPasscode, userRole, userName, userTrack, deviceID, expiryDate, dateCreated;
    public static Boolean status_export = false;
    public static Boolean status_phMvTable = false;
    public static Boolean status_setExtrapolate = false;
    public static Boolean toggle_is_checked = false;
    public static Boolean calibratingNow = false;
    public static String extrapolateValue;
    public static String extrapolateValueDeviceID;
    public static ArrayList<String> id_fetched, passcode_fetched, role_fetched, name_fetched, expiryDate_fetched, dateCreated_fetched;
    public static String subscription;
    public static String scannerData;
    public static String logUserName;
    public static String loginUserRole;

    public static int offlineStatus = 0;

    public static int auto_log = 0;
    public static String calib_completed_by;

    public static Dialog loadingDialog;

    public static String getCurrentTime() {
        Date date = Calendar.getInstance().getTime();
        DateFormat timeFormat = new SimpleDateFormat("HH:mm");
        return timeFormat.format(date);
    }

    public static String getPresentDate() {
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(date);
    }

    public static void showLoading(Activity context, boolean cancelable, boolean cancelOnTouchOutside, String message, boolean cancelBtn) {
        if (!context.isFinishing()) {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                cancelLoading();
            }
            loadingDialog = new Dialog(context);
            loadingDialog.setContentView(R.layout.loading_dialog);

            ImageView closeDialog = loadingDialog.findViewById(R.id.closeDialog);

            if (cancelBtn){

                closeDialog.setVisibility(View.VISIBLE);
            }else {
                closeDialog.setVisibility(View.GONE);
            }

            closeDialog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Source.cancelLoading();
                }
            });

            TextView loadingMessageTextView = loadingDialog.findViewById(R.id.textView);
            loadingMessageTextView.setText(message);

            try {
                loadingDialog.getWindow().setDimAmount(0);
                loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            } catch (Exception e) {
                Log.d("TAG", "showLoading: " + e.getMessage());
            }
            loadingDialog.setCanceledOnTouchOutside(cancelOnTouchOutside);
            loadingDialog.setCancelable(cancelable);
            loadingDialog.show();
        }
    }

    public static void cancelLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            try {
                loadingDialog.cancel();
                loadingDialog = null;
            } catch (Exception e) {
                Log.d("TAG", "cancelLoading: " + e.getMessage());
            }
        }
    }


}

