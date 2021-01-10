package com.acim.walk;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import java.util.Calendar;

public class Util {

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
    public static final String ERROR_DIALOG_TITLE = "Errore Autenticazione";
    public static final String ERROR_DIALOG_MESSAGE_VALIDATION = "Compilare tutti i campi";
    public static final String ERROR_DIALOG_MESSAGE_FAILED_LOGIN = "Autenticazione fallita. Riprovare";
    public static final String ERROR_DIALOG_MESSAGE_FAILED_SIGNUP = "Registrazione fallita. Riprovare";

    public static final String PROGRESS_DIALOG_TITLE = "Caricamento";
    public static final String PROGRESS_DIALOG_MESSAGE = "Si prega di attendere...";

    //gestione mail
    public static final String ALERT_EMPTY_EMAIL_TITLE = "Campo email vuoto";
    public static final String ALERT_EMPTY_EMAIL_MESSAGE = "Inserisci prima la tua email";
    public static final String ERROR_SEND_MAIL = "Errore invio email";
    public static final String ERROR_SEND_MAIL_MESSAGE = "Email non inviata correttamente";

    //fragment settings
    public static final String ALERT_EMPTY_PASSWORD_TITLE = "Campo password vuoto";
    public static final String ALERT_EMPTY_PASSWORD_MESSAGE = "Inserisci una nuova password";
    public static final String ERROR_UPDATE_PASSWORD = "Errore aggiornamento password";
    public static final String ERROR_UPDATE_PASSWORD_MESSAGE = "Aggiornamento non effettuato correttamente";
    public static final String ERROR_GET_USER = "Errore recupero utente";
    public static final String ERROR_GET_USER_MESSAGE = "Non è stato recuperato correttamente l'utente";
    public static final String ERROR_DELETE_USER = "Errore eliminazione account";
    public static final String ERROR_DELETE_USER_MESSAGE = "Eliminazione account non avvenuta correttamente";
    public static final String ALERT_EMPTY_PASSWORD_ELIMINA_MESSAGE = "Inserisci prima la tua password";

    // ANSWER of fragment FAQ
    public static final String FIRST_ANSWER = "È possibile o ricercare una partita nelle" +
            " vicinanze premendo il bottone \"CERCA PARTITA\" o crearne una premendo il bottone \"CREA PARTITA\". " +
            "  Basta inserire la durata in minuti e rimanere in attesa che altri giocatori" +
            " si uniscano al gioco.";
    public static final String SECOND_ANSWER = "Sulla schermata di login, sotto all\' inserimento della password tocca \"Ho dimenticato" +
            " la Password\", verrà così inviata una mail sulla propria casella di posta, all'indirizzo specificato in fase di registrazione" +
            " contenente un link per il reset della password.";
    public static final String THIRD_ANSWER = "È possibile aggiornare la propria password nella schermata IMPOSTAZIONI, cliccando su \"AGGIORNA\", dopo aver inserito la vecchia e la nuova password.";
    public static final String FOURTH_ANSWER = "Nella schermata IMPOSTAZIONI in basso troverai il bottone per l'eliminazione dell'account, sarà prima necessario inserire la password corrente" +
            " per confermare l'azione che sarà irreversibile.";
    public static final String FIFTH_ANSWER = "Per la ricerca di giocatori nelle vicinanze l'applicazione utilizza \"Nearby\". Assicurati quindi" +
            " di avere attivato il Wi-Fi o la connessione dati mobili, il Bluetooth e la geolocalizzazione. Se pensi sia già tutto a posto, controlla nelle impostazioni del" +
            " dispositivo se sono state concesse tutte le autorizzazioni necessarie per l'applicazione \"Walk!\". Se ancora non riesci a creare o trovare una partita," +
            " sempre nelle impostazioni del dispositivo, controlla che l'applicazione sia nella lista delle eccezioni per quanto riguarda l'ottimizzazione della batteria" +
            " e il risparmio energetico.";

}
