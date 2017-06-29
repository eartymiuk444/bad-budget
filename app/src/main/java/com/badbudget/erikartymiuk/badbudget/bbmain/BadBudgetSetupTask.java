package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.PredictActivity;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.Prediction;

import java.util.Date;
import java.util.Map;

/**
 * Async task similar to the select budget task that sets up the base home activity by populating the
 * user data with the default budget and sets the applications budget id to name map.
 *
 */
public class BadBudgetSetupTask extends AsyncTask<Void, Void, Void> {

    private static final String PROGRESS_DIALOG_MESSAGE = "Loading Budget...";

    private SplashActivity contextActivity;
    private ProgressDialog progressDialog;

    /**
     * Constructor for SetupTask, require the SplashActivity context in which
     * this task will run, for getting a db connection, the application, and for presenting
     * a progress dialog as the budget data is loaded
     * @param context - the HomeActivity that will start this task
     */
    public BadBudgetSetupTask(SplashActivity context)
    {
        super();
        this.contextActivity = context;
    }

    /**
     * Background task done. Populates the initial user data using the default budget Id set in our
     * database. Also sets the application budget id to name map.
     * @param params - unused
     * @return - void
     */
    protected Void doInBackground(Void... params)
    {
        SQLiteDatabase writeableDB = BBDatabaseOpenHelper.getInstance(contextActivity).getWritableDatabase();
        BadBudgetApplication application = (BadBudgetApplication)contextActivity.getApplication();

        BBDatabaseContract.populateUserData(application, writeableDB, BBDatabaseContract.getDefaultBudgetId(writeableDB));
        Map<Integer, String> map = BBDatabaseContract.getBudgetMapIDToName(writeableDB);
        application.setBudgetMapIDToName(map);
        application.setPredictBBDUpdated(true);

        return null;
    }

    /**
     * Executed on the UI Thread prior to the background thread executing. Creates and shows a progress
     * dialog using the SplashActivity this task was created with.
     */
    protected void onPreExecute()
    {
        //Progress dialog to display to the user until the database user data is loaded into
        //memory
        progressDialog = new ProgressDialog(contextActivity);
        progressDialog.setMessage(PROGRESS_DIALOG_MESSAGE);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    /**
     * Calls back the SplashActivity indicating that a budget was selected and then dismisses the
     * progress dialog.
     * @param result - unused.
     */
    protected void onPostExecute(Void result)
    {
        progressDialog.dismiss();
        contextActivity.setupTaskComplete();
    }
}
