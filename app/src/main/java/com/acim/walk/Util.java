package com.acim.walk;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

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
     * @param strings some strings
     * @return true if both email and password are NOT empty, false otherwise
     */
    public static boolean validateForm(String ...strings) {
        // if one string is empty the form is invalid
        for(String s : strings) {
            if(s.isEmpty())
                return false;
        }
        // all strings are NOT empty, form is valid
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
        new AlertDialog.Builder(context, R.style.AlertDialogTheme).setTitle(errorTitle)
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

    /**
     *
     * prints a Toast
     *
     * @param context
     * @param message
     * @param isDurationShort TRUE -> toast disappears quickly, FALSE -> disappears NOT so quickly
     */
    public static void toast(Context context, String message, boolean isDurationShort) {
        int TOAST_DURATION = isDurationShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
        Toast.makeText(context, message, TOAST_DURATION).show();
    }


    // these are the messages that will be shown to display some errors to the users
    public static String ERROR_DIALOG_TITLE = "Errore Autenticazione";
    public static String ERROR_DIALOG_MESSAGE_VALIDATION = "Compilare tutti i campi";
    public static String ERROR_DIALOG_MESSAGE_FAILED_LOGIN = "Autenticazione fallita. Riprovare";
    public static String ERROR_DIALOG_MESSAGE_FAILED_SIGNUP = "Registrazione fallita. Riprovare";

    public static String PROGRESS_DIALOG_TITLE = "Caricamento";
    public static String PROGRESS_DIALOG_MESSAGE = "Si prega di attendere...";

    public static  String ALERT_EMPTY_EMAIL_TITLE = "Campo email vuoto";
    public static  String ALERT_EMPTY_EMAIL_MESSAGE = "Inserisci prima la tua email";
    public static  String ERROR_SEND_MAIL = "Errore Invio email";
    public static  String ERROR_SEND_MAIL_MESSAGE = "email non inviata correttamente";

    //fragment settings
    public static  String ALERT_EMPTY_PASSWORD_TITLE = "Campo password vuoto";
    public static  String ALERT_EMPTY_PASSWORD_MESSAGE = "Inserisci una nuova password";
    public static  String ERROR_UPDATE_PASSWORD = "Errore aggiornamento password";
    public static  String ERROR_UPDATE_PASSWORD_MESSAGE = "Aggiornamento non effettuato correttamente";
    public static  String ERROR_GET_USER = "Errore recupero utente";
    public static  String ERROR_GET_USER_MESSAGE = "non è stato recuperato correttamente l'utente";
    public static  String ERROR_DELETE_USER = "Errore eliminazione account";
    public static  String ERROR_DELETE_USER_MESSAGE = "Eliminazione Account non avvenuta correttamente";
    public static  String ALERT_EMPTY_PASSWORD_ELIMINA_MESSAGE = "Inserisci prima la tua password";

}
