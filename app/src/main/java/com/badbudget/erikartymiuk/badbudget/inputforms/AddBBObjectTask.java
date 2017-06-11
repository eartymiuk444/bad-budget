package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseContract;
import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseOpenHelper;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.TrackerHistoryItem;
import com.erikartymiuk.badbudgetlogic.budget.BudgetItem;
import com.erikartymiuk.badbudgetlogic.main.Account;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.MoneyGain;
import com.erikartymiuk.badbudgetlogic.main.MoneyLoss;
import com.erikartymiuk.badbudgetlogic.main.MoneyOwed;
import com.erikartymiuk.badbudgetlogic.main.Prediction;
import com.erikartymiuk.badbudgetlogic.main.SavingsAccount;
import com.erikartymiuk.badbudgetlogic.predictdataclasses.TransactionHistoryItem;

import java.util.Date;
import java.util.List;

/**
 * The Add BB Object Task handles and encapsulates the background thread that adds a bb object to the
 * user's database and to the bb data memory off of the UI thread. Before execution it displays a progress
 * dialog. After execution it dismisses the progress dialog and calls the callback
 * addBBOjectFinished method
 *
 * Created by eartymiuk on 6/25/2016.
 */
public class AddBBObjectTask extends AsyncTask<Void, Void, Void>
{
    private Activity contextActivity;       //The Activity to use for getting the application and database,
                                                //and for creation of the progress dialog
    private String progressDialogMessage;  //The message to display in the progress dialog as the task executes
    private Object bbObject;                //The bb object to add to memory
    private ContentValues values;           //The bb object to add to the database (should match bbObject)
    private BBOperationTaskCaller callback; //The object to call when the add operation completes
    private BBObjectType bbObjectType;      //Indicates what object we are adding.

    private ProgressDialog progressDialog;  //The progress dialog that is displayed and dismessed on
                                            //pre and post execution of this task

    /**
     * Constructor
     *
     * @param contextActivity - The Activity to use for getting the application and database,
     *                              and for creation of the progress dialog
     * @param progressDialogMessage - Message to display in an uncancelable dialog shown during execution
     * @param bbObject - The object we are adding, should match the bbObjectType
     * @param values - Content Values added to the users database. Should represent the bbObject
     * @param callback - Who to callback when the execution is complete
     * @param bbObjectType - The type of object to add. Should match the bbObject and the values
     *
     */
    public AddBBObjectTask(Activity contextActivity, String progressDialogMessage, Object bbObject,
                          ContentValues values, BBOperationTaskCaller callback, BBObjectType bbObjectType)
    {
        super();
        this.contextActivity = contextActivity;
        this.progressDialogMessage = progressDialogMessage;
        this.bbObject = bbObject;
        this.values = values;
        this.callback = callback;
        this.bbObjectType = bbObjectType;

        progressDialog = new ProgressDialog(this.contextActivity);
        progressDialog.setMessage(progressDialogMessage);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);

    }

    /**
     * In a background thread off of the UI thread this method adds the passed in bbObject to the
     * bad budget in memory data and the bad budget sql database. The object is assumed to not be a
     * duplicate.
     * @param params - unused
     * @return Void
     */
    protected Void doInBackground(Void... params)
    {
        BBDatabaseOpenHelper dbHelper = BBDatabaseOpenHelper.getInstance(this.contextActivity);
        SQLiteDatabase writableDB = dbHelper.getWritableDatabase();

        BadBudgetApplication application = ((BadBudgetApplication) this.contextActivity.getApplication());
        BadBudgetData bbd = application.getBadBudgetUserData();

        switch (this.bbObjectType)
        {
            case ACCOUNT: {
                writableDB.insert(BBDatabaseContract.CashAccounts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), null, this.values);
                bbd.addAccount((Account)bbObject);
                break;
            }
            case ACCOUNT_SAVINGSACCOUNT:
            {
                writableDB.insert(BBDatabaseContract.CashAccounts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), null, this.values);
                bbd.addAccount((SavingsAccount)bbObject);
                todayUpdate(((SavingsAccount)bbObject).nextContribution());

                break;
            }
            case DEBT:
            {
                writableDB.insert(BBDatabaseContract.Debts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), null, this.values);
                bbd.addDebt((MoneyOwed)bbObject);
                if (((MoneyOwed)bbObject).payment() != null)
                {
                    todayUpdate(((MoneyOwed)bbObject).payment().nextPaymentDate());
                }

                break;
            }
            case DEBT_CREDITCARD:
            {
                writableDB.insert(BBDatabaseContract.Debts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), null, this.values);
                bbd.addDebt((MoneyOwed)bbObject);
                if (((MoneyOwed)bbObject).payment() != null)
                {
                    todayUpdate(((MoneyOwed)bbObject).payment().nextPaymentDate());
                }

                break;
            }
            case DEBT_LOAN:
            {
                writableDB.insert(BBDatabaseContract.Debts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), null, this.values);
                bbd.addDebt((MoneyOwed)bbObject);
                if (((MoneyOwed)bbObject).payment() != null)
                {
                    todayUpdate(((MoneyOwed)bbObject).payment().nextPaymentDate());
                }

                break;
            }
            case DEBT_MISC:
            {
                writableDB.insert(BBDatabaseContract.Debts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), null, this.values);
                bbd.addDebt((MoneyOwed)bbObject);
                if (((MoneyOwed)bbObject).payment() != null)
                {
                    todayUpdate(((MoneyOwed)bbObject).payment().nextPaymentDate());
                }

                break;
            }
            case GAIN:
            {
                writableDB.insert(BBDatabaseContract.Gains.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), null, this.values);
                bbd.addGain((MoneyGain)bbObject);
                todayUpdate(((MoneyGain)bbObject).nextDeposit());

                break;
            }
            case LOSS:
            {
                writableDB.insert(BBDatabaseContract.Losses.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), null, this.values);
                bbd.addLoss((MoneyLoss)bbObject);
                todayUpdate(((MoneyLoss)bbObject).nextLoss());

                break;
            }
            case BUDGETITEM:
            {
                writableDB.insert(BBDatabaseContract.BudgetItems.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), null, this.values);
                bbd.getBudget().addBudgetItem((BudgetItem)bbObject);
                todayUpdate(((BudgetItem)bbObject).nextLoss());

                break;
            }
        }

        application.setPredictBBDUpdated(true);
        return null;
    }

    /**
     * Overridden AsyncTask method. Simply shows the progress dialog with the passed in message
     */
    protected void onPreExecute()
    {
        this.progressDialog.show();
    }

    /**
     * Overridden AsyncTask method. After execution of the background task this executes on the UI
     * thread. It dismisses the progress dialog and calls the callback's addBBObjectFinished method.
     * @param result - unused.
     */
    protected void onPostExecute(Void result)
    {
        this.progressDialog.dismiss();
        callback.addBBObjectFinished();
    }

    /**
     * Using the passed next date this method checks if it equals the applications current today date
     * and if it does runs an update for that single day for both in memory and the database.
     * @param nextDate - a next date that we are checking to see if it equals the today date of the
     *                      bad budget application which indicates an update must occur
     */
    private void todayUpdate(Date nextDate)
    {
        BadBudgetApplication application = (BadBudgetApplication)contextActivity.getApplication();
        BBDatabaseOpenHelper dbHelper = BBDatabaseOpenHelper.getInstance(this.contextActivity);
        SQLiteDatabase writableDB = dbHelper.getWritableDatabase();

        if (Prediction.datesEqualUpToDay(nextDate, application.getToday()))
        {
            //Run update for today only
            if (application.getAutoUpdateSelectedBudget())
            {
                Prediction.update(application.getBadBudgetUserData(), application.getToday(), application.getToday());
            }
            else
            {
                Prediction.updateNextDatesOnly(application.getBadBudgetUserData(), application.getToday(), application.getToday());
            }

            List<TrackerHistoryItem> newTrackerHistoryItems =
                    BBDatabaseContract.updateTrackerHistoryItemsMemory(application.getBadBudgetUserData(),
                            application.getTrackerHistoryItems(), application.getToday(), application.getToday());
            List<TransactionHistoryItem> newTransactionHistoryItems = BBDatabaseContract.updateGeneralHistoryItemsMemory(application.getBadBudgetUserData(),
                    application.getGeneralHistoryItems(), application.getToday(), application.getToday());

            BBDatabaseContract.updateFullDatabase(writableDB, application.getBadBudgetUserData(), newTrackerHistoryItems, newTransactionHistoryItems, application.getSelectedBudgetId());
        }
    }

}
