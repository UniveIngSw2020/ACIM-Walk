package com.acim.walk;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.util.Calendar;

public class Util {
    /**
     * @return milliseconds since 1.1.1970 for today 0:00:00 local timezone
     */
    public static long getToday() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /**
     * @return milliseconds since 1.1.1970 for tomorrow 0:00:01 local timezone
     */
    public static long getTomorrow() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 1);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DATE, 1);
        return c.getTimeInMillis();
    }




    /**
     *
     * Validates form for AuthActivity's LoginFragment and SignInFragment
     *
     * @param email
     * @param password
     * @return true if both email and password are NOT empty, false otherwise
     */
    public static boolean validateForm(String email, String password) {

        // if either one of the strings is empty
        if(email.isEmpty() || password.isEmpty())
            return false;

        // both strings are NOT empty
        return true;
    }


    /**
     *
     * Shows error dialog
     *
     * @param errorTitle
     * @param errorMessage
     */
    public static void showErrorAlert(Context context, String errorTitle, String errorMessage) {
        new AlertDialog.Builder(context).setTitle(errorTitle)
                .setMessage(errorMessage)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(final DialogInterface dialogInterface) {
                        // we do nothing here
                    }
                }).setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        }).create().show();
    }


    /**
     *
     * Shows progress bar
     *
     * @param context
     * @param title
     * @param message
     * @return
     */
    public static ProgressDialog createProgressBar(Context context, String title, String message) {
        ProgressDialog progress = new ProgressDialog(context);
        progress.setTitle(title);
        progress.setMessage(message);
        // disable dismiss by tapping outside of the dialog
        progress.setCancelable(false);

        return progress;
    }




    // these are the messages that will be shown to display some errors to the users
    public static String ERROR_DIALOG_TITLE = "Errore Autenticazione";
    public static String ERROR_DIALOG_MESSAGE_VALIDATION = "Compilare entrambi i campi";
    public static String ERROR_DIALOG_MESSAGE_FAILED_LOGIN = "Autenticazione fallita. Riprovare";

    public static String PROGRESS_DIALOG_TITLE = "Caricamento";
    public static String PROGRESS_DIALOG_MESSAGE = "Si prega di attendere...";
}
