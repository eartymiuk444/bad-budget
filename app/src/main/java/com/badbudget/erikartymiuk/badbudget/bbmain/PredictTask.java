package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.PredictActivity;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.Prediction;

import java.util.Date;

/**
 * Async Task for running the prediction algorithm. For use with the predict activity when it determines
 * that it needs prediction data for a date that we don't have data for.
 *
 * Created by Erik Artymiuk on 7/7/2016.
 */
public class PredictTask extends AsyncTask<Void, Void, Void> {

    private PredictTaskCaller caller;
    private Activity contextActivity;
    private ProgressDialog progressDialog;

    private Date originalStartDate;
    private Date lastTargetDate;
    private Date targetDate;

    /**
     * Constructor for the PredictTask class.
     *
     * @param caller - caller of the PredictTask
     * @param context - the PredictActivity this task is executed from
     * @param progressDialog - a progress dialog to be shown until this task is complete
     * @param originalStartDate - the original start date of our prediction. Typically the current date
     * @param lastTargetDate - if the prediction has already been run this should be the date up to which
     *                       we already have data for. If this date comes before our start date then we
     *                       assume no prediction has yet been run.
     * @param targetDate - the date we want predict data up to (inclusive)
     */
    public PredictTask(PredictTaskCaller caller, Activity context, ProgressDialog progressDialog, Date originalStartDate, Date lastTargetDate, Date targetDate)
    {
        super();
        this.caller = caller;
        this.contextActivity = context;
        this.progressDialog = progressDialog;

        this.originalStartDate = originalStartDate;
        this.lastTargetDate = lastTargetDate;
        this.targetDate = targetDate;
    }

    /**
     * The background task that is run when execute is called on an instantiated PredictTask object.
     * Uses the dates passed in the constructor to run the prediction algorithm, either vanilla predict or
     * predict continue depending on the dates, so that the applications bad budget data object have
     * prediction data for every day up to and including target date. The check for if we should
     * run predict continue is whether the original start date comes before the last target date.
     * @param params - unused
     * @return - unused (null)
     */
    protected Void doInBackground(Void... params)
    {
        BadBudgetApplication application = ((BadBudgetApplication) this.contextActivity.getApplication());
        BadBudgetData bbd = application.getBadBudgetUserData();

        //Check to see if we already ran the prediction algorithm at least once
        boolean ranPrediction = Prediction.numDaysBetween(originalStartDate, lastTargetDate) >= 0;
        if (!ranPrediction)
        {
            Prediction.predict(bbd, originalStartDate, targetDate);
        }
        else
        {
            Prediction.predictContinue(bbd, originalStartDate, lastTargetDate, targetDate);
        }

        application.setPredictBBDUpdated(false);
        return null;
    }

    /**
     * Overridden AsyncTask method. Simply shows the progress dialog passed in on object creation
     * on the UI Thread.
     */
    protected void onPreExecute()
    {
        this.progressDialog.show();
    }

    /**
     * Method called on the UI thread after execution of the background task. This method informs the
     * invoking PredictActivity that the prediction data is now set (via the predictionSet callback),
     * and dismisses the progress dialog that was being shown to the user as the prediction algorithm
     * ran.
     * @param result - unused.
     */
    protected void onPostExecute(Void result)
    {
        caller.predictionSet(targetDate);
        this.progressDialog.dismiss();
    }
}
