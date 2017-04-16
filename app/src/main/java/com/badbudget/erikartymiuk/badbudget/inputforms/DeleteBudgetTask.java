package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.app.ProgressDialog;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseContract;
import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseOpenHelper;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetBaseActivity;

/**
 * Async task that deletes the selected budget from our database and from memory (the budgets map)
 *
 * Created by Erik Artymiuk on 7/7/2016.
 */
public class DeleteBudgetTask extends AsyncTask<Void, Void, Void> {

    private static final String PROGRESS_DIALOG_MESSAGE = "Deleting Budget...";

    private BadBudgetBaseActivity contextActivity;
    private ProgressDialog progressDialog;
    private String deleteBudgetName;

    /**
     * Constructor for the delete budget task. Takes the context where the progress dialog will
     * be displayed and where we will get our database and application instance from,
     * and the name of the budget that is to be deleted.
     * @param context - the context where the progress dialog will be displayed and where we get our db and app reference from
     * @param deleteBudgetName - the name of the budget to be deleted
     */
    public DeleteBudgetTask(BadBudgetBaseActivity context, String deleteBudgetName)
    {
        super();
        this.contextActivity = context;
        this.deleteBudgetName = deleteBudgetName;
    }

    /**
     * Background task that deletes the budget with the given name from our db and from memory (the
     * budget map).
     * @param params - unused
     * @return - Void
     */
    protected Void doInBackground(Void... params)
    {
        SQLiteDatabase writeableDB = BBDatabaseOpenHelper.getInstance(contextActivity).getWritableDatabase();
        BadBudgetApplication application = (BadBudgetApplication)contextActivity.getApplication();

        int deleteBudgetId = BBDatabaseContract.getBudgetMapNameToID(writeableDB).get(deleteBudgetName);
        BBDatabaseOpenHelper.deleteBudget(writeableDB, deleteBudgetId);
        application.getBudgetMapIDToName().remove(deleteBudgetId);

        return null;
    }

    /**
     * Executed on the ui thread prior to the background task. Creates and shows a progress dialog
     * on the context.
     */
    protected void onPreExecute()
    {
        progressDialog = new ProgressDialog(contextActivity);
        progressDialog.setMessage(PROGRESS_DIALOG_MESSAGE);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    /**
     * Executes after the background task completes. Dismisses the progress dialog.
     * @param result - unused.
     */
    protected void onPostExecute(Void result)
    {
        progressDialog.dismiss();
    }
}
