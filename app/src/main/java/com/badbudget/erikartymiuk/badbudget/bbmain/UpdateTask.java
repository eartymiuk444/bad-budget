package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.Prediction;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Async Task that checks the current application set date against the actual date. If the set date
 * is in the past then this task runs an update on the application bad budget data up to today in memory
 * and persisted in the database.
 * If the set date was in the past or in the future then the application's set date is set to the
 * actual date and the last update date in the database is set to the actual date. Prior to execution
 * this task checks if the background task should be run and if it doesn't need to be run then
 * immediately calls the callback method and cancels the task. If it does need to run it presents
 * the passed progress dialog on the context and the task starts. Upon task completion it then
 * calls back the caller and after the callback finishes dismisses the progress dialog that was shown.
 *
 * An update task will always update the applications today date even if an update isn't run.
 *
 */
public class UpdateTask extends AsyncTask<Void, Void, Void> {

    /* Message shown if it is determined an update needs to be run */
    private static final String progressDialogMessage = "Date changed, updating...";

    private Activity contextActivity;
    private UpdateTaskCaller caller;
    private ProgressDialog progressDialog;

    private Date actualToday;
    private boolean setDateInPast;
    private boolean setDateInFuture;

    /**
     * Constructor for an UpdateTask. Requires a context where the progress dialog is being presented
     * and where the database helper can get its instance. Also requires an UpdateTaskCaller that will
     * be sent a method upon completion of the task and the progress dialog that is to be presented as
     * the task runs.
     * @param contextActivity - the context from which this task is run
     * @param caller - the UpdateTaskCaller to callback after completion of the task
     * @param progressDialog - the progress dialog to display during task execution
     */
    public UpdateTask(Activity contextActivity, UpdateTaskCaller caller, ProgressDialog progressDialog)
    {
        super();
        this.contextActivity = contextActivity;
        this.caller = caller;
        this.progressDialog = progressDialog;
        this.progressDialog.setMessage(progressDialogMessage);
    }

    /**
     * Background task that runs the update in memory and updates the database, if necessary.
     * Sets the application today date as the actual date and also updates the last update date
     * in the database to today's date. Updates the progress dialog message
     * if an update does need to be run indicating that this is what is happening.
     * @param params - unused
     * @return - unused (null)
     */
    protected Void doInBackground(Void... params)
    {
        BBDatabaseOpenHelper dbHelper = BBDatabaseOpenHelper.getInstance(contextActivity);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Date setToday = ((BadBudgetApplication)contextActivity.getApplication()).getToday();
        BadBudgetApplication application = (BadBudgetApplication)contextActivity.getApplication();
        BadBudgetData bbd = application.getBadBudgetUserData();

        //If apps today date is in past we update it to today
        if (setDateInPast)
        {
            //Update in memory
            if (application.getAutoUpdateSelectedBudget())
            {
                Prediction.update(bbd, setToday, actualToday);
            }
            else
            {
                Prediction.updateNextDatesOnly(bbd, setToday, actualToday);
            }

            List<TrackerHistoryItem> newTrackerHistoryItems =
                    BBDatabaseContract.updateTrackerHistoryItemsMemory(bbd,
                            application.getTrackerHistoryItems(), setToday, actualToday);
            BBDatabaseContract.updateFullDatabase(db, bbd, newTrackerHistoryItems, application.getSelectedBudgetId());
        }

        //If apps today date is in future or past we update it to today
        if (setDateInPast || setDateInFuture) {

            //Update the last update date in memory and in the database
            ((BadBudgetApplication) contextActivity.getApplication()).setToday(actualToday);

            ContentValues metaValues = new ContentValues();
            metaValues.put(BBDatabaseContract.BBMetaData.COLUMN_LAST_UPDATE, BBDatabaseContract.dbDateToString(actualToday));
            db.update(BBDatabaseContract.BBMetaData.TABLE_NAME + "_" +
                    ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), metaValues, null, null);
        }

        application.setPredictBBDUpdated(true);
        return null;
    }

    /**
     * Overridden AsyncTask method. Checks if an update is needed and shows the progress dialog
     * passed in on object creation on the UI Thread if it is. If an update isn't needed calls
     * the callers callback method immediately, updateTaskCompleted, and cancels the task.
     */
    protected void onPreExecute()
    {
        actualToday = Calendar.getInstance().getTime();

        //TODO Remove Testing 12/10
        //actualToday = ((BadBudgetApplication)contextActivity.getApplication()).today;
        //actualToday = Prediction.addDays(actualToday, 1);
        //TODO

        Date setToday = ((BadBudgetApplication)contextActivity.getApplication()).getToday();
        setDateInPast = Prediction.numDaysBetween(setToday, actualToday) > 0;
        setDateInFuture = Prediction.numDaysBetween(setToday, actualToday) < 0;

        if (setDateInPast || setDateInFuture) {
            this.progressDialog.show();
        }
        else
        {
            //An update should always set the apps time to the actual time.
            ((BadBudgetApplication) contextActivity.getApplication()).setToday(actualToday);
            caller.updateTaskCompleted(false);
            this.cancel(false);
        }
    }

    /**
     * Method run after completion of the background task. Calls the specified caller with the
     * updateTaskCompleted method. After this method completes the progress dialog is dismissed.
     * @param result - unused.
     */
    protected void onPostExecute(Void result)
    {
        caller.updateTaskCompleted(true);
        this.progressDialog.dismiss();
    }
}
