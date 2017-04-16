package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.design.widget.NavigationView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.PredictActivity;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.Prediction;

import java.util.Date;

/**
 * Async task that selects a new budget either from our list of existing budgets, or an entirely new
 * budget that was either newly created empty or one that was copied. Selecting a budget means that
 * the application's data structures are updated so that the new budgets objects are in memory and
 * can be viewed and manipulated by the user.
 *
 * Created by Erik Artymiuk on 7/7/2016.
 */
public class SelectBudgetTask extends AsyncTask<Void, Void, Void> {

    private static final String PROGRESS_DIALOG_MESSAGE = "Loading Budget...";

    private BadBudgetBaseActivity contextActivity;
    private ProgressDialog progressDialog;
    private String selectedBudgetName;
    private String newBudgetName;

    private boolean creating;
    private boolean copying;

    /**
     * Constructor for the select budget task. Require a context in which the task will run and where
     * a progress dialog will be shown, the name of a selected budget, a new name for a budget if
     * applicable, whether or not we are creating and copying our selected budget.
     * @param context - the context in which the task will run
     * @param selectedBudgetName - if selecting existing, i.e. not creating, than this should be the name
     *                           of an existing budget that is being selected
     * @param newBudgetName - if creating a new budget this should be the name for that new budget,
     *                      should be null otherwise
     * @param creating - true if we are creating a new budget, either empty or copied
     * @param copying - if creating a new budget, this should be true if we are creating that new budget by creating
     *                  a copy of an existing budget.
     */
    public SelectBudgetTask(BadBudgetBaseActivity context, String selectedBudgetName, String newBudgetName,
                            boolean creating, boolean copying)
    {
        super();
        this.contextActivity = context;
        this.selectedBudgetName = selectedBudgetName;
        this.newBudgetName = newBudgetName;
        this.creating = creating;
        this.copying = copying;
    }

    /**
     * Background task that selects an existing or new budget. If new then this task also creates the
     * new budget and adds it to our applications map of budget ids to names.
     * @param params - unused
     * @return - Void
     */
    protected Void doInBackground(Void... params)
    {
        SQLiteDatabase writeableDB = BBDatabaseOpenHelper.getInstance(contextActivity).getWritableDatabase();
        BadBudgetApplication application = (BadBudgetApplication)contextActivity.getApplication();

        int updatedSelectedBudgetId;

        if (creating)
        {
            if (!copying)
            {
                updatedSelectedBudgetId = BBDatabaseOpenHelper.createNewBudget(writeableDB, newBudgetName);
            }
            else
            {
                int copyBudgetId = BBDatabaseContract.getBudgetMapNameToID(writeableDB).get(selectedBudgetName);
                updatedSelectedBudgetId = BBDatabaseOpenHelper.copyExistingBudget(writeableDB, copyBudgetId, newBudgetName);
            }
            application.getBudgetMapIDToName().put(updatedSelectedBudgetId, newBudgetName);
        }
        else
        {
            updatedSelectedBudgetId = BBDatabaseContract.getBudgetMapNameToID(writeableDB).get(selectedBudgetName);
        }

        BBDatabaseContract.populateUserData(application, writeableDB, updatedSelectedBudgetId);
        BBDatabaseContract.setDefaultBudgetId(writeableDB, updatedSelectedBudgetId);
        application.setPredictBBDUpdated(true);

        return null;
    }

    /**
     * Executed on the ui thread prior to the background task. Creates and shows a progress dialog
     * on the context.
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
     * Executes after the background task completes. Calls the budgetSelected callback on the
     * invoking context and dismisses the progress dialog.
     * @param result - unused.
     */
    protected void onPostExecute(Void result)
    {
        contextActivity.budgetSelected();
        progressDialog.dismiss();
    }
}
