package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseContract;
import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseOpenHelper;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;

/**
 * The Delete BB Object Task handles and encapsulates the background thread that deletes a bb object from ths
 * user's database and from bb data memory off of the UI thread. Before execution it displays a progress
 * dialog. After execution it dismisses the progress dialog and calls the callback
 * deleteBBOjectFinished method
 *
 * Created by eartymiuk on 6/25/2016.
 */
public class DeleteBBObjectTask extends AsyncTask<Void, Void, Void>
{
    private Activity contextActivity;       //The Activity to use for getting the application and database,
                                            //and for creation of the progress dialog
    private String progressDialogMessage;  //The message to display in the progress dialog as the task executes
    private String identifier;              //An identifying string for the object that is being deleted
    private BBOperationTaskCaller callback; //The object to call when the delete operation completes
    private BBObjectType bbObjectType;      //Indicates what type of object we are deleting.

    private ProgressDialog progressDialog;  //The progress dialog that is displayed and dismessed on
    //pre and post execution of this task

    /**
     * Constructor
     *
     * @param contextActivity - The Activity to use for getting the application and database,
     *                              and for creation of the progress dialog
     * @param progressDialogMessage - Message to display in an uncancelable dialog shown during execution
     * @param identifier - Unique string identifier for the object we are deleting
     * @param callback - Who to callback when the execution is complete
     * @param bbObjectType - The type of object to add. Should match the bbObject and the values
     *
     */
    public DeleteBBObjectTask(Activity contextActivity, String progressDialogMessage, String identifier,
                           BBOperationTaskCaller callback, BBObjectType bbObjectType)
    {
        super();
        this.contextActivity = contextActivity;
        this.progressDialogMessage = progressDialogMessage;
        this.identifier = identifier;
        this.callback = callback;
        this.bbObjectType = bbObjectType;

        progressDialog = new ProgressDialog(this.contextActivity);
        progressDialog.setMessage(progressDialogMessage);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
    }

    /**
     * In a background thread off of the UI thread this method deletes the object with the passed in identifier from the
     * bad budget in memory data and the bad budget sql database. The object is assumed to be present in
     * both places
     * @param params - unused
     * @return Void
     */
    protected Void doInBackground(Void... params)
    {
        BBDatabaseOpenHelper dbHelper = BBDatabaseOpenHelper.getInstance(this.contextActivity);
        SQLiteDatabase writableDB = dbHelper.getWritableDatabase();
        String[] whereArgs = {identifier};

        BadBudgetApplication application = ((BadBudgetApplication) this.contextActivity.getApplication());
        BadBudgetData bbd = application.getBadBudgetUserData();

        switch (this.bbObjectType)
        {
            case ACCOUNT: {
                writableDB.delete(BBDatabaseContract.CashAccounts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(),
                        BBDatabaseContract.CashAccounts.COLUMN_NAME + "=?",
                        whereArgs);
                bbd.deleteAccountWithName(identifier);
                break;
            }
            case ACCOUNT_SAVINGSACCOUNT:
            {
                writableDB.delete(BBDatabaseContract.CashAccounts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(),
                        BBDatabaseContract.CashAccounts.COLUMN_NAME + "=?",
                        whereArgs);
                bbd.deleteAccountWithName(identifier);
                break;
            }
            case DEBT:
            {
                writableDB.delete(BBDatabaseContract.Debts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(),
                        BBDatabaseContract.Debts.COLUMN_NAME + "=?",
                        whereArgs);
                bbd.deleteDebtWithName(identifier);
                break;
            }
            case DEBT_CREDITCARD:
            {
                writableDB.delete(BBDatabaseContract.Debts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(),
                        BBDatabaseContract.Debts.COLUMN_NAME + "=?",
                        whereArgs);
                bbd.deleteDebtWithName(identifier);
                break;
            }
            case DEBT_LOAN:
            {
                writableDB.delete(BBDatabaseContract.Debts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(),
                        BBDatabaseContract.Debts.COLUMN_NAME + "=?",
                        whereArgs);
                bbd.deleteDebtWithName(identifier);
                break;
            }
            case DEBT_MISC:
            {
                writableDB.delete(BBDatabaseContract.Debts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(),
                        BBDatabaseContract.Debts.COLUMN_NAME + "=?",
                        whereArgs);
                bbd.deleteDebtWithName(identifier);
                break;
            }
            case GAIN:
            {
                writableDB.delete(BBDatabaseContract.Gains.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(),
                        BBDatabaseContract.Gains.COLUMN_DESCRIPTION + "=?",
                        whereArgs);
                bbd.deleteGainWithDescription(identifier);
                break;
            }
            case LOSS:
            {
                writableDB.delete(BBDatabaseContract.Losses.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(),
                        BBDatabaseContract.Losses.COLUMN_EXPENSE + "=?",
                        whereArgs);
                bbd.deleteLossWithDescription(identifier);
                break;
            }
            case BUDGETITEM:
            {
                writableDB.delete(BBDatabaseContract.BudgetItems.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(),
                        BBDatabaseContract.BudgetItems.COLUMN_DESCRIPTION + "=?",
                        whereArgs);
                //TODO - Check to see if this works, or add method, or handle another way
                bbd.getBudget().getAllBudgetItems().remove(identifier);
                //bbd.getBudget().removeBudgetItemWithDescription(identifier);
                break;
            }
        }

        application.setPredictBBDUpdated(true);
        return null;
    }

    /**
     * Overridden AsyncTask method. Shows the progress dialog with the passed in message
     */
    protected void onPreExecute()
    {
        this.progressDialog.show();
    }

    /**
     * Overridden AsyncTask method. After execution of the background task this executes on the UI
     * thread. It dismisses the progress dialog and calls the callback's deleteBBObjectFinished method.
     * @param result - unused.
     */
    protected void onPostExecute(Void result)
    {
        this.progressDialog.dismiss();
        callback.deleteBBObjectFinished();
    }

}
