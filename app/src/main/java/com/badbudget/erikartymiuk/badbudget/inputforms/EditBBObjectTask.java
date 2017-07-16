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
import com.erikartymiuk.badbudgetlogic.budget.Budget;
import com.erikartymiuk.badbudgetlogic.budget.BudgetItem;
import com.erikartymiuk.badbudgetlogic.budget.RemainAmountAction;
import com.erikartymiuk.badbudgetlogic.main.Account;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetInvalidValueException;
import com.erikartymiuk.badbudgetlogic.main.Loan;
import com.erikartymiuk.badbudgetlogic.main.MoneyGain;
import com.erikartymiuk.badbudgetlogic.main.MoneyLoss;
import com.erikartymiuk.badbudgetlogic.main.MoneyOwed;
import com.erikartymiuk.badbudgetlogic.main.MoneyTransfer;
import com.erikartymiuk.badbudgetlogic.main.Payment;
import com.erikartymiuk.badbudgetlogic.main.Prediction;
import com.erikartymiuk.badbudgetlogic.main.SavingsAccount;
import com.erikartymiuk.badbudgetlogic.predictdataclasses.TransactionHistoryItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * The Edit BB Object Task handles and encapsulates the background thread that edits
 * a bb object in the user's database and in the bb data memory off of the UI thread. Before execution
 * it displays a progress dialog. After execution it dismisses the progress dialog and calls the callback
 * editBBOjectFinished method
 *
 * Created by eartymiuk on 6/25/2016.
 */
public class EditBBObjectTask extends AsyncTask<Void, Void, Void>
{
    private Activity contextActivity;       //The Activity to use for getting the application and database,
                                            //and for creation of the progress dialog
    private String progressDialogMessage;  //The message to display in the progress dialog as the task executes
    private Object bbObject;                //The bb object with the changed values of the existing
                                                //object, used to encapsulate the new values (a wrapper for the new values)
                                                //the object itself isn't linked to the bad budget data

    private boolean autoUpdate;
    private RemainAmountAction remainAmountAction;

    private ContentValues values;           //The map with the changed values of the existing object
    private BBOperationTaskCaller callback; //The object to call when the edit operation completes
    private BBObjectType bbObjectType;      //Indicates what object type we are editing.

    private String identifier;              //Unique string identifier for this object
    private ProgressDialog progressDialog;  //The progress dialog that is displayed and dismissed on

    //pre and post execution of this task

    /**
     * Constructor
     *
     * @param contextActivity - The Activity to use for getting the application and database,
     *                              and for creation of the progress dialog
     * @param progressDialogMessage - Message to display in an uncancelable dialog shown during execution
     * @param bbObject - A wrapper to hold the new values for the object we are editing, should match the bbObjectType, invalid to set this to BUDGET_PREFS
     *                  use the dedicated constructor so that auto update and remainAmountAction are also edited correctly
     * @param values - Content Values with the changed values to be updated in the users database. Should represent the bbObject
     * @param callback - Who to callback when the execution is complete
     * @param bbObjectType - The type of object we are editing. Should match the bbObject and the values
     *
     */
    public EditBBObjectTask(Activity contextActivity, String progressDialogMessage, Object bbObject,
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

        BadBudgetData bbd = ((BadBudgetApplication) this.contextActivity.getApplication()).getBadBudgetUserData();

        switch (bbObjectType)
        {
            case ACCOUNT:
                this.identifier = ((Account)this.bbObject).name();
                break;
            case ACCOUNT_SAVINGSACCOUNT:
                this.identifier = ((Account)this.bbObject).name();
                break;
            case DEBT:
                this.identifier = ((MoneyOwed)this.bbObject).name();
                break;
            case DEBT_CREDITCARD:
                this.identifier = ((MoneyOwed)this.bbObject).name();
                break;
            case DEBT_LOAN:
                this.identifier = ((MoneyOwed)this.bbObject).name();
                break;
            case DEBT_MISC:
                this.identifier = ((MoneyOwed)this.bbObject).name();
                break;
            case GAIN:
                this.identifier = ((MoneyGain)this.bbObject).sourceDescription();
                break;
            case LOSS:
                this.identifier = ((MoneyLoss)this.bbObject).expenseDescription();
                break;
            case TRANSFER:
                this.identifier = ((MoneyTransfer)this.bbObject).getTransferDescription();
                break;
            case BUDGETITEM:
                this.identifier = ((BudgetItem)this.bbObject).expenseDescription();
                break;
        }

    }

    /**
     * Constructor for editing budget prefs
     *
     * @param contextActivity - The Activity to use for getting the application and database,
     *                              and for creation of the progress dialog
     * @param progressDialogMessage - Message to display in an uncancelable dialog shown during execution
     * @param wrapperBudget - A wrapper to hold the new values for the budget prefs
     * @param autoUpdate - Since auto update cannot be wrapped in the budget object it is passed separately
     * @param remainAmountAction - Since remainAmountAction cannot be wrapped in the budget object it is passed separately
     *                              and applied to all budget items
     * @param values - Content Values with the changed values to be updated in the users database. Should represent the bbObject
     * @param callback - Who to callback when the execution is complete
     */
    public EditBBObjectTask(Activity contextActivity, String progressDialogMessage, Budget wrapperBudget,
                            boolean autoUpdate, RemainAmountAction remainAmountAction, ContentValues values,
                            BBOperationTaskCaller callback)
    {
        super();
        this.contextActivity = contextActivity;
        this.progressDialogMessage = progressDialogMessage;
        this.bbObject = wrapperBudget;
        this.values = values;
        this.callback = callback;
        this.bbObjectType = null;

        this.autoUpdate = autoUpdate;
        this.remainAmountAction = remainAmountAction;

        progressDialog = new ProgressDialog(this.contextActivity);
        progressDialog.setMessage(progressDialogMessage);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);

        this.identifier = null;
    }

    /**
     * In a background thread off of the UI thread this method deletes and adds the changed bbObject from the
     * bad budget in memory data and the bad budget sql database. The object is assumed to exist in those locations
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

        if (this.bbObjectType != null)
        {
            switch (this.bbObjectType)
            {
                case ACCOUNT: {

                    Account wrapperAccount = (Account)bbObject;
                    Account editAccount = bbd.getAccountWithName(wrapperAccount.name());
                    editAccount.setValue(wrapperAccount.value());
                    editAccount.setQuickLook(wrapperAccount.quickLook());

                    String strFilter = BBDatabaseContract.CashAccounts.COLUMN_NAME + "=?";
                    writableDB.update(BBDatabaseContract.CashAccounts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(),
                            values, strFilter, new String[] {editAccount.name()});

                    break;
                }
                case ACCOUNT_SAVINGSACCOUNT:
                {
                    SavingsAccount wrapperSavingsAccount = (SavingsAccount)bbObject;
                    SavingsAccount editAccount = (SavingsAccount)bbd.getAccountWithName(wrapperSavingsAccount.name());
                    editAccount.setValue(wrapperSavingsAccount.value());
                    editAccount.setQuickLook(wrapperSavingsAccount.quickLook());
                    editAccount.updateSourceAccount(wrapperSavingsAccount.sourceAccount());
                    editAccount.updateGoalAndContribution(wrapperSavingsAccount.goal(), wrapperSavingsAccount.goalDate(), wrapperSavingsAccount.contribution());
                    editAccount.changeNextContribution(wrapperSavingsAccount.nextContribution());
                    editAccount.changeEndDate(wrapperSavingsAccount.endDate());
                    editAccount.setInterestRate(wrapperSavingsAccount.getInterestRate());

                    String strFilter = BBDatabaseContract.CashAccounts.COLUMN_NAME + "=?";
                    writableDB.update(BBDatabaseContract.CashAccounts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), values, strFilter, new String[] {editAccount.name()});

                    todayUpdate(((SavingsAccount)bbObject).nextContribution());

                    break;
                }
                case DEBT:
                {
                    MoneyOwed wrapperDebt = (MoneyOwed)bbObject;
                    MoneyOwed editDebt = bbd.getDebtWithName(wrapperDebt.name());

                    try
                    {
                        editDebt.changeAmount(wrapperDebt.amount());
                        editDebt.setInterestRate(wrapperDebt.interestRate());
                        editDebt.setQuicklook(wrapperDebt.quicklook());
                        //TODO - Better way to handle this. Update payment directly if it exists or create entirely new? 10/1
                        //editDebt.setupPayment(wrapperDebt.payment());
                        Payment wrapperPayment = wrapperDebt.payment();
                        if (wrapperPayment != null) {
                            Payment payment = new Payment(wrapperPayment.amount(), wrapperPayment.amount() == -1, wrapperPayment.frequency(), wrapperPayment.sourceAccount(),
                                    wrapperPayment.nextPaymentDate(), wrapperPayment.ongoing(), wrapperPayment.endDate(), editDebt, wrapperPayment.goalDate());
                            editDebt.setupPayment(payment);
                        }

                        String strFilter = BBDatabaseContract.Debts.COLUMN_NAME + "=?";
                        writableDB.update(BBDatabaseContract.Debts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), values, strFilter, new String[] {editDebt.name()});

                        if (((MoneyOwed)bbObject).payment() != null)
                        {
                            todayUpdate(((MoneyOwed)bbObject).payment().nextPaymentDate());
                        }
                    }
                    catch (BadBudgetInvalidValueException e)
                    {
                        //TODO
                        System.err.println("Invalid update to payment");
                        e.printStackTrace();
                    }

                    break;
                }
                case DEBT_CREDITCARD:
                {
                    MoneyOwed wrapperDebt = (MoneyOwed)bbObject;
                    MoneyOwed editDebt = bbd.getDebtWithName(wrapperDebt.name());

                    try
                    {
                        editDebt.changeAmount(wrapperDebt.amount());
                        editDebt.setInterestRate(wrapperDebt.interestRate());
                        editDebt.setQuicklook(wrapperDebt.quicklook());

                        //TODO - Better way to handle this. Update payment directly if it exists or create entirely new? 10/1
                        //editDebt.setupPayment(wrapperDebt.payment());
                        Payment wrapperPayment = wrapperDebt.payment();
                        if (wrapperPayment != null)
                        {
                            Payment payment = new Payment(wrapperPayment.amount(), wrapperPayment.amount() == -1, wrapperPayment.frequency(), wrapperPayment.sourceAccount(),
                                    wrapperPayment.nextPaymentDate(), wrapperPayment.ongoing(), wrapperPayment.endDate(), editDebt, wrapperPayment.goalDate());
                            editDebt.setupPayment(payment);
                        }

                        String strFilter = BBDatabaseContract.Debts.COLUMN_NAME + "=?";
                        writableDB.update(BBDatabaseContract.Debts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), values, strFilter, new String[] {editDebt.name()});


                        if (((MoneyOwed)bbObject).payment() != null)
                        {
                            todayUpdate(((MoneyOwed)bbObject).payment().nextPaymentDate());
                        }
                    }
                    catch (BadBudgetInvalidValueException e)
                    {
                        //TODO
                        System.err.println("Invalid update to payment");
                        e.printStackTrace();
                    }

                    break;
                }
                case DEBT_LOAN:
                {
                    Loan wrapperLoan = (Loan)bbObject;
                    Loan editLoan = (Loan)bbd.getDebtWithName(wrapperLoan.name());

                    try
                    {
                        editLoan.changeAmount(wrapperLoan.amount());
                        editLoan.setInterestRate(wrapperLoan.interestRate());
                        editLoan.setSimpleInterest(wrapperLoan.isSimpleInterest());
                        editLoan.setPrincipalBalance(wrapperLoan.getPrincipalBalance());
                        editLoan.setInterestAmount(wrapperLoan.getInterestAmount());

                        editLoan.setQuicklook(wrapperLoan.quicklook());
                        //TODO - Better way to handle this. Update payment directly if it exists or create entirely new? 10/1
                        //editDebt.setupPayment(wrapperDebt.payment());
                        Payment wrapperPayment = wrapperLoan.payment();
                        if (wrapperPayment != null) {
                            Payment payment = new Payment(wrapperPayment.amount(), wrapperPayment.amount() == -1, wrapperPayment.frequency(), wrapperPayment.sourceAccount(),
                                    wrapperPayment.nextPaymentDate(), wrapperPayment.ongoing(), wrapperPayment.endDate(), editLoan, wrapperPayment.goalDate());
                            editLoan.setupPayment(payment);
                        }

                        String strFilter = BBDatabaseContract.Debts.COLUMN_NAME + "=?";
                        writableDB.update(BBDatabaseContract.Debts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), values, strFilter, new String[] {editLoan.name()});

                        if (((MoneyOwed)bbObject).payment() != null)
                        {
                            todayUpdate(((MoneyOwed)bbObject).payment().nextPaymentDate());
                        }
                    }
                    catch (BadBudgetInvalidValueException e)
                    {
                        //TODO
                        System.err.println("Invalid update to payment");
                        e.printStackTrace();
                    }

                    break;
                }
                case DEBT_MISC:
                {
                    MoneyOwed wrapperDebt = (MoneyOwed)bbObject;
                    MoneyOwed editDebt = bbd.getDebtWithName(wrapperDebt.name());

                    try
                    {
                        editDebt.changeAmount(wrapperDebt.amount());
                        editDebt.setInterestRate(wrapperDebt.interestRate());
                        editDebt.setQuicklook(wrapperDebt.quicklook());
                        //TODO - Better way to handle this. Update payment directly if it exists or create entirely new? 10/1
                        //editDebt.setupPayment(wrapperDebt.payment());
                        Payment wrapperPayment = wrapperDebt.payment();

                        if (wrapperPayment != null) {
                            Payment payment = new Payment(wrapperPayment.amount(), wrapperPayment.amount() == -1, wrapperPayment.frequency(), wrapperPayment.sourceAccount(),
                                    wrapperPayment.nextPaymentDate(), wrapperPayment.ongoing(), wrapperPayment.endDate(), editDebt, wrapperPayment.goalDate());
                            editDebt.setupPayment(payment);
                        }

                        String strFilter = BBDatabaseContract.Debts.COLUMN_NAME + "=?";
                        writableDB.update(BBDatabaseContract.Debts.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), values, strFilter, new String[] {editDebt.name()});

                        if (((MoneyOwed)bbObject).payment() != null)
                        {
                            todayUpdate(((MoneyOwed)bbObject).payment().nextPaymentDate());
                        }
                    }
                    catch (BadBudgetInvalidValueException e)
                    {
                        //TODO
                        System.err.println("Invalid update to payment");
                        e.printStackTrace();
                    }

                    break;
                }
                case GAIN:
                {
                    MoneyGain wrapperGain = (MoneyGain)bbObject;
                    MoneyGain editGain = bbd.getGainWithDescription(wrapperGain.sourceDescription());

                    editGain.setGainFrequency(wrapperGain.gainFrequency());
                    editGain.setGainAmount(wrapperGain.gainAmount());
                    editGain.setNextDeposit(wrapperGain.nextDeposit());
                    editGain.setEndDate(wrapperGain.endDate());
                    editGain.setDestination(wrapperGain.destinationAccount());

                    String strFilter = BBDatabaseContract.Gains.COLUMN_DESCRIPTION + "=?";
                    writableDB.update(BBDatabaseContract.Gains.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), values, strFilter, new String[] {editGain.sourceDescription()});

                    todayUpdate(((MoneyGain)bbObject).nextDeposit());

                    break;
                }
                case LOSS:
                {
                    MoneyLoss wrapperLoss = (MoneyLoss)bbObject;
                    MoneyLoss editLoss = bbd.getLossWithDescription(wrapperLoss.expenseDescription());

                    editLoss.setLossFrequency(wrapperLoss.lossFrequency());
                    editLoss.setLossAmount(wrapperLoss.lossAmount());
                    editLoss.setNextLoss(wrapperLoss.nextLoss());
                    editLoss.setEndDate(wrapperLoss.endDate());
                    editLoss.setSource(wrapperLoss.source());

                    String strFilter = BBDatabaseContract.Losses.COLUMN_EXPENSE + "=?";
                    writableDB.update(BBDatabaseContract.Losses.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), values, strFilter, new String[] {editLoss.expenseDescription()});

                    todayUpdate(((MoneyLoss)bbObject).nextLoss());

                    break;
                }
                case TRANSFER:
                {
                    MoneyTransfer wrapperTransfer = (MoneyTransfer)bbObject;
                    MoneyTransfer editTransfer = bbd.getTransferWithDescription(wrapperTransfer.getTransferDescription());

                    editTransfer.setSource(wrapperTransfer.getSource());
                    editTransfer.setDestination(wrapperTransfer.getDestination());
                    editTransfer.setAmount(wrapperTransfer.getAmount());
                    editTransfer.setFrequency(wrapperTransfer.getFrequency());
                    editTransfer.setNextTransfer(wrapperTransfer.getNextTransfer());
                    editTransfer.setEndDate(wrapperTransfer.getEndDate());

                    String strFilter = BBDatabaseContract.Transfers.COLUMN_DESCRIPTION + "=?";
                    writableDB.update(BBDatabaseContract.Transfers.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), values, strFilter, new String[] {editTransfer.getTransferDescription()});

                    todayUpdate(((MoneyTransfer)bbObject).getNextTransfer());

                    break;
                }
                case BUDGETITEM:
                {
                    BudgetItem wrapperItem = (BudgetItem)bbObject;
                    BudgetItem editItem = bbd.getBudget().retrieveBudgetItem(wrapperItem.expenseDescription());

                    editItem.setLossFrequency(wrapperItem.lossFrequency());
                    editItem.setLossAmount(wrapperItem.lossAmount());
                    editItem.setNextLoss(wrapperItem.nextLoss());
                    editItem.setProratedStart(wrapperItem.isProratedStart());
                    editItem.setEndDate(wrapperItem.endDate());

                    //Tracker fields
                    editItem.setCurrAmount(wrapperItem.getCurrAmount());
                    editItem.setPlusAmount(wrapperItem.getPlusAmount());
                    editItem.setMinusAmount(wrapperItem.getMinusAmount());

                    String strFilter = BBDatabaseContract.BudgetItems.COLUMN_DESCRIPTION + "=?";
                    writableDB.update(BBDatabaseContract.BudgetItems.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), values, strFilter, new String[] {editItem.expenseDescription()});

                    todayUpdate(((BudgetItem)bbObject).nextLoss());

                    break;
                }
            }
        }
        //Budget Prefs can't be encapsulated with only a budget because of auto update
        //and remain amount action fields
        else
        {
            Budget wrapperBudget = (Budget)bbObject;
            Budget editBudget = bbd.getBudget();

            editBudget.setBudgetSource(wrapperBudget.getBudgetSource());
            editBudget.setAutoReset(wrapperBudget.isAutoReset());
            editBudget.setWeeklyReset(wrapperBudget.getWeeklyReset());
            editBudget.setMonthlyReset(wrapperBudget.getMonthlyReset());

            application.setAutoUpdateSelectedBudget(this.autoUpdate);
            application.setRemainAmountActionSelectedBudget(this.remainAmountAction);

            //Need to update the remain action in memory for all the budget items
            //and in the database which applies to all budget items.
            for (BudgetItem currItem : editBudget.getAllBudgetItems().values())
            {
                currItem.setRemainAmountAction(remainAmountAction);
            }

            writableDB.update(BBDatabaseContract.BudgetPreferences.TABLE_NAME + "_" + ((BadBudgetApplication)this.contextActivity.getApplication()).getSelectedBudgetId(), values, null, null);
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
     * thread. It dismisses the progress dialog and calls the callback's editBBObjectFinished method.
     * @param result - unused.
     */
    protected void onPostExecute(Void result)
    {
        this.progressDialog.dismiss();
        callback.editBBObjectFinished();
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
