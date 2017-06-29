package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.app.Activity;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteOpenHelper;


import com.badbudget.erikartymiuk.badbudget.inputforms.AddBBObjectTask;
import com.badbudget.erikartymiuk.badbudget.inputforms.BBObjectType;
import com.badbudget.erikartymiuk.badbudget.inputforms.EditBBObjectTask;
import com.erikartymiuk.badbudgetlogic.budget.BudgetItem;
import com.erikartymiuk.badbudgetlogic.budget.RemainAmountAction;
import com.erikartymiuk.badbudgetlogic.main.Account;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetInvalidValueException;
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.Source;

import java.util.Calendar;
import java.util.Date;

/**
 * Bad budget database open helper. Takes care of correctly creating the database if it does not
 * exist, opening it if it does, and upgrading it if necessary. If the database does not exist
 * then on creation all the necessary tables are created. If the database does exist nothing needs
 * to be done. This class should only be accessed using the singleton instance via the get instance
 * method.
 *
 * Created by Erik Artymiuk on 6/10/2016.
 */
public class BBDatabaseOpenHelper extends SQLiteOpenHelper
{
    /* Singleton instance of the database open helper */
    private static BBDatabaseOpenHelper sInstance;

    /* Application context of this database open helper's singleton instance */
    private Application context;

    /* Current database version number */
    private static final int DATABASE_VERSION = 3;
    /* Bad budget database name */
    private static final String DATABASE_NAME = "BBDatabase.db";
    /* First Budget Name */
    private static final String FIRST_BUDGET_NAME = "First Budget";

    /* Possible states of EULA Agreement */
    public static final int EULA_EXTREME_EARLY_ADOPT = 0;
    public static final int EULA_NOT_AGREE = 1;
    public static final int EULA_AGREED = 2;

    /* Constants for use in various sql statements */
    private static final String COMMA_SEP = ",";
    private static final String CREATE_TABLE = "CREATE TABLE";
    private static final String LEFT_PAREN = "(";
    private static final String RIGHT_PAREN = ")";
    private static final String SPACE = " ";
    private static final String FOREIGN_KEY = "FOREIGN KEY";
    private static final String REFERENCES = "REFERENCES";
    private static final String ENABLE_FOREIGN_KEYS = "PRAGMA foreign_keys=ON";
    private static final String DISABLE_FOREIGN_KEYS = "PRAGMA foreign_keys=OFF";
    private static final String DEFAULT_TIMESTAMP_COLUMN_STATEMENT = "CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP";
    private static final String COPY_INSERT_INTO = "INSERT INTO";
    private static final String COPY_SELECT_FROM = "SELECT * FROM";
    private static final String COPY_DELETE_FROM = "DELETE FROM";
    private static final String DROP_TABLE = "DROP TABLE";
    private static final String ALTER_TABLE = "ALTER TABLE";
    private static final String ADD_COLUMN = "ADD COLUMN";


    /* Create table statement constructions */

    private static final String[] CASH_ACCOUNT_COLUMNS =
            {BBDatabaseContract.CashAccounts.COLUMN_NAME,
            BBDatabaseContract.CashAccounts.COLUMN_VALUE,
            BBDatabaseContract.CashAccounts.COLUMN_QUICK_LOOK,
            BBDatabaseContract.CashAccounts.COLUMN_SAVINGS,
            BBDatabaseContract.CashAccounts.COLUMN_GOAL,
            BBDatabaseContract.CashAccounts.COLUMN_CONTRIBUTION_AMOUNT,
            BBDatabaseContract.CashAccounts.COLUMN_CONTRIBUTION_FREQUENCY,
            BBDatabaseContract.CashAccounts.COLUMN_NEXT_CONTRIBUTION,
            BBDatabaseContract.CashAccounts.COLUMN_END_DATE,
            BBDatabaseContract.CashAccounts.COLUMN_GOAL_DATE,
            BBDatabaseContract.CashAccounts.COLUMN_SOURCE_NAME,
            BBDatabaseContract.CashAccounts.COLUMN_INTEREST_RATE};

    private static final String[] CASH_ACCOUNT_COLUMN_TYPES = {BBDatabaseContract.CashAccounts.NAME_TYPE,
            BBDatabaseContract.CashAccounts.VALUE_TYPE,
            BBDatabaseContract.CashAccounts.QUICK_LOOK_TYPE,
            BBDatabaseContract.CashAccounts.SAVINGS_TYPE,
            BBDatabaseContract.CashAccounts.GOAL_TYPE,
            BBDatabaseContract.CashAccounts.CONTRIBUTION_AMOUNT_TYPE,
            BBDatabaseContract.CashAccounts.CONTRIBUTION_FREQUENCY_TYPE,
            BBDatabaseContract.CashAccounts.NEXT_DATE_TYPE,
            BBDatabaseContract.CashAccounts.END_DATE_TYPE,
            BBDatabaseContract.CashAccounts.GOAL_DATE_TYPE,
            BBDatabaseContract.CashAccounts.SOURCE_NAME_TYPE,
            BBDatabaseContract.CashAccounts.INTEREST_RATE_TYPE};

    private static final String[] DEBT_COLUMNS = {BBDatabaseContract.Debts.COLUMN_NAME,
            BBDatabaseContract.Debts.COLUMN_AMOUNT,
            BBDatabaseContract.Debts.COLUMN_QUICK_LOOK,
            BBDatabaseContract.Debts.COLUMN_DEBT_TYPE,
            BBDatabaseContract.Debts.COLUMN_PAYMENT_AMOUNT,
            BBDatabaseContract.Debts.COLUMN_PAYMENT_SOURCE,
            BBDatabaseContract.Debts.COLUMN_PAYMENT_FREQUENCY,
            BBDatabaseContract.Debts.COLUMN_PAYMENT_NEXT_PAYMENT,
            BBDatabaseContract.Debts.COLUMN_PAYMENT_END_DATE,
            BBDatabaseContract.Debts.COLUMN_PAYMENT_GOAL_DATE,
            BBDatabaseContract.Debts.COLUMN_PAYMENT_PAYOFF,
            BBDatabaseContract.Debts.COLUMN_PAYMENT_ONGOING,
            BBDatabaseContract.Debts.COLUMN_INTEREST_RATE,
            BBDatabaseContract.Debts.COLUMN_SIMPLE_INTEREST,
            BBDatabaseContract.Debts.COLUMN_PRINCIPLE};

    private static final String[] DEBT_COLUMN_TYPES = {BBDatabaseContract.Debts.NAME_TYPE,
            BBDatabaseContract.Debts.AMOUNT_TYPE,
            BBDatabaseContract.Debts.QUICKLOOK_TYPE,
            BBDatabaseContract.Debts.DEBT_TYPE,
            BBDatabaseContract.Debts.PAYMENT_AMOUNT_TYPE,
            BBDatabaseContract.Debts.PAYMENT_SOURCE_TYPE,
            BBDatabaseContract.Debts.PAYMENT_FREQUENCY_TYPE,
            BBDatabaseContract.Debts.PAYMENT_NEXT_DATE_TYPE,
            BBDatabaseContract.Debts.PAYMENT_END_DATE_TYPE,
            BBDatabaseContract.Debts.PAYMENT_GOAL_DATE_TYPE,
            BBDatabaseContract.Debts.PAYMENT_PAYOFF_TYPE,
            BBDatabaseContract.Debts.PAYMENT_ONGOING_TYPE,
            BBDatabaseContract.Debts.INTEREST_RATE_TYPE,
            BBDatabaseContract.Debts.SIMPLE_INTEREST_TYPE,
            BBDatabaseContract.Debts.PRINCIPLE_TYPE};

    private static final String[] GAIN_COLUMNS = {BBDatabaseContract.Gains.COLUMN_DESCRIPTION,
            BBDatabaseContract.Gains.COLUMN_AMOUNT,
            BBDatabaseContract.Gains.COLUMN_FREQUENCY,
            BBDatabaseContract.Gains.COLUMN_NEXT_GAIN,
            BBDatabaseContract.Gains.COLUMN_END_DATE,
            BBDatabaseContract.Gains.COLUMN_DESTINATION};

    private static final String[] GAIN_COLUMN_TYPES = {BBDatabaseContract.Gains.DESCRIPTION_TYPE,
            BBDatabaseContract.Gains.AMOUNT_TYPE,
            BBDatabaseContract.Gains.FREQUENCY_TYPE,
            BBDatabaseContract.Gains.NEXT_DATE_TYPE,
            BBDatabaseContract.Gains.END_DATE_TYPE,
            BBDatabaseContract.Gains.DESTINATION_TYPE};


    private static final String[] LOSS_COLUMNS = {BBDatabaseContract.Losses.COLUMN_EXPENSE,
            BBDatabaseContract.Losses.COLUMN_AMOUNT,
            BBDatabaseContract.Losses.COLUMN_FREQUENCY,
            BBDatabaseContract.Losses.COLUMN_NEXT_LOSS,
            BBDatabaseContract.Losses.COLUMN_END_DATE,
            BBDatabaseContract.Losses.COLUMN_SOURCE};

    private static final String[] LOSS_COLUMN_TYPES = {BBDatabaseContract.Losses.EXPENSE_TYPE,
            BBDatabaseContract.Losses.AMOUNT_TYPE,
            BBDatabaseContract.Losses.FREQUENCY_TYPE,
            BBDatabaseContract.Losses.NEXT_DATE_TYPE,
            BBDatabaseContract.Losses.END_DATE_TYPE,
            BBDatabaseContract.Losses.SOURCE_TYPE};


    private static final String[] BUDGET_PREFERENCE_COLUMNS = {BBDatabaseContract.BudgetPreferences.COLUMN_BUDGET_SOURCE,
            BBDatabaseContract.BudgetPreferences.COLUMN_AUTO_RESET,
            BBDatabaseContract.BudgetPreferences.COLUMN_AUTO_UPDATE,
            BBDatabaseContract.BudgetPreferences.COLUMN_REMAIN_AMOUNT_ACTION,
            BBDatabaseContract.BudgetPreferences.COLUMN_WEEKLY_RESET,
            BBDatabaseContract.BudgetPreferences.COLUMN_MONTHLY_RESET};

    private static final String[] BUDGET_PREFERENCE_COLUMN_TYPES = {BBDatabaseContract.BudgetPreferences.BUDGET_SOURCE_TYPE,
            BBDatabaseContract.BudgetPreferences.AUTO_RESET_TYPE,
            BBDatabaseContract.BudgetPreferences.AUTO_UPDATE_TYPE,
            BBDatabaseContract.BudgetPreferences.REMAIN_AMOUNT_ACTION_TYPE,
            BBDatabaseContract.BudgetPreferences.WEEKLY_RESET_TYPE,
            BBDatabaseContract.BudgetPreferences.MONTHLY_RESET_TYPE};


    private static final String[] BUDGET_ITEM_COLUMNS = {BBDatabaseContract.BudgetItems.COLUMN_DESCRIPTION,
            BBDatabaseContract.BudgetItems.COLUMN_AMOUNT,
            BBDatabaseContract.BudgetItems.COLUMN_FREQUENCY,
            BBDatabaseContract.BudgetItems.COLUMN_NEXT_LOSS,
            BBDatabaseContract.BudgetItems.COLUMN_END_DATE,
            BBDatabaseContract.BudgetItems.COLUMN_PRORATED_START,
            BBDatabaseContract.BudgetItems.COLUMN_PLUS_AMOUNT,
            BBDatabaseContract.BudgetItems.COLUMN_MINUS_AMOUNT,
            BBDatabaseContract.BudgetItems.COLUMN_REMAINING_AMOUNT,
            };

    private static final String[] BUDGET_ITEM_COLUMN_TYPES = {BBDatabaseContract.BudgetItems.DESCRIPTION_TYPE,
            BBDatabaseContract.BudgetItems.AMOUNT_TYPE,
            BBDatabaseContract.BudgetItems.FREQUENCY_TYPE,
            BBDatabaseContract.BudgetItems.NEXT_DATE_TYPE,
            BBDatabaseContract.BudgetItems.END_DATE_TYPE,
            BBDatabaseContract.BudgetItems.PRORATED_START_TYPE,
            BBDatabaseContract.BudgetItems.PLUS_AMOUNT_TYPE,
            BBDatabaseContract.BudgetItems.MINUS_AMOUNT_TYPE,
            BBDatabaseContract.BudgetItems.REMAINING_AMOUNT_TYPE,
            };


    private static final String[] TRACKER_HISTORY_ITEM_COLUMNS = {BBDatabaseContract.TrackerHistoryItems.COLUMN_BUDGET_ITEM_DESCRIPTION,
            BBDatabaseContract.TrackerHistoryItems.COLUMN_USER_TRANSACTION_DESCRIPTION,
            BBDatabaseContract.TrackerHistoryItems.COLUMN_DATE,
            BBDatabaseContract.TrackerHistoryItems.COLUMN_DAY,
            BBDatabaseContract.TrackerHistoryItems.COLUMN_TIME,
            BBDatabaseContract.TrackerHistoryItems.COLUMN_ACTION,
            BBDatabaseContract.TrackerHistoryItems.COLUMN_ACTION_AMOUNT,
            BBDatabaseContract.TrackerHistoryItems.COLUMN_ORIGINAL_BUDGET_AMOUNT,
            BBDatabaseContract.TrackerHistoryItems.COLUMN_UPDATED_BUDGET_AMOUNT};

    private static final String[] TRACKER_HISTORY_ITEM_COLUMN_TYPES = {BBDatabaseContract.TrackerHistoryItems.BUDGET_ITEM_DESCRIPTION_TYPE,
            BBDatabaseContract.TrackerHistoryItems.USER_TRANSACTION_DESCRIPTION_TYPE,
            BBDatabaseContract.TrackerHistoryItems.DATE_TYPE,
            BBDatabaseContract.TrackerHistoryItems.DAY_TYPE,
            BBDatabaseContract.TrackerHistoryItems.TIME_TYPE,
            BBDatabaseContract.TrackerHistoryItems.ACTION_TYPE,
            BBDatabaseContract.TrackerHistoryItems.ACTION_AMOUNT_TYPE,
            BBDatabaseContract.TrackerHistoryItems.ORIGINAL_BUDGET_AMOUNT_TYPE,
            BBDatabaseContract.TrackerHistoryItems.UPDATED_BUDGET_AMOUNT_TYPE};

    private static final String[] GENERAL_HISTORY_ITEM_COLUMNS = {BBDatabaseContract.GeneralHistoryItems.COLUMN_DESTINATION_ACTION,
            BBDatabaseContract.GeneralHistoryItems.COLUMN_DESTINATION_ORIGINAL,
            BBDatabaseContract.GeneralHistoryItems.COLUMN_DESTINATION_UPDATED,
            BBDatabaseContract.GeneralHistoryItems.COLUMN_SOURCE_ACTION,
            BBDatabaseContract.GeneralHistoryItems.COLUMN_SOURCE_ORIGINAL,
            BBDatabaseContract.GeneralHistoryItems.COLUMN_SOURCE_UPDATED,
            BBDatabaseContract.GeneralHistoryItems.COLUMN_TRANSACTION_AMOUNT,
            BBDatabaseContract.GeneralHistoryItems.COLUMN_TRANSACTION_DATE,
            BBDatabaseContract.GeneralHistoryItems.COLUMN_TRANSACTION_DESTINATION,
            BBDatabaseContract.GeneralHistoryItems.COLUMN_TRANSACTION_SOURCE,
            BBDatabaseContract.GeneralHistoryItems.COLUMN_DESTINATION_CAN_SHOW_CHANGE,
            BBDatabaseContract.GeneralHistoryItems.COLUMN_SOURCE_CAN_SHOW_CHANGE};

    private static final String[] GENERAL_HISTORY_ITEM_COLUMN_TYPES = {BBDatabaseContract.GeneralHistoryItems.DESTINATION_ACTION_TYPE,
            BBDatabaseContract.GeneralHistoryItems.DESTINATION_ORIGINAL_TYPE,
            BBDatabaseContract.GeneralHistoryItems.DESTINATION_UPDATED_TYPE,
            BBDatabaseContract.GeneralHistoryItems.SOURCE_ACTION_TYPE,
            BBDatabaseContract.GeneralHistoryItems.SOURCE_ORIGINAL_TYPE,
            BBDatabaseContract.GeneralHistoryItems.SOURCE_UPDATED_TYPE,
            BBDatabaseContract.GeneralHistoryItems.TRANSACTION_AMOUNT_TYPE,
            BBDatabaseContract.GeneralHistoryItems.TRANSACTION_DATE_TYPE,
            BBDatabaseContract.GeneralHistoryItems.TRANSACTION_DESTINATION_TYPE,
            BBDatabaseContract.GeneralHistoryItems.TRANSACTION_SOURCE_TYPE,
            BBDatabaseContract.GeneralHistoryItems.DESTINATION_CAN_SHOW_CHANGE_TYPE,
            BBDatabaseContract.GeneralHistoryItems.SOURCE_CAN_SHOW_CHANGE_TYPE};

    private static final String[] BUDGET_META_DATA_COLUMNS = {BBDatabaseContract.BBMetaData.COLUMN_LAST_UPDATE};
    private static final String[] BUDGET_META_DATA_COLUMN_TYPES = {BBDatabaseContract.BBMetaData.LAST_UPDATE_TYPE};

    private static final String GLOBAL_META_DATA_CREATE_TABLE =
            CREATE_TABLE + SPACE +
                    BBDatabaseContract.GlobalMetaData.TABLE_NAME + LEFT_PAREN +
                    BBDatabaseContract.GlobalMetaData.COLUMN_LAST_ID + SPACE + BBDatabaseContract.GlobalMetaData.LAST_ID_TYPE + COMMA_SEP +
                    BBDatabaseContract.GlobalMetaData.COLUMN_DEFAULT_BUDGET_ID + SPACE + BBDatabaseContract.GlobalMetaData.DEFAULT_BUDGET_ID_TYPE + COMMA_SEP +
                    BBDatabaseContract.GlobalMetaData.COLUMN_AGREED_EULA + SPACE + BBDatabaseContract.GlobalMetaData.AGREED_EULA_TYPE +
                    RIGHT_PAREN;

    private static final String BUDGETS_CREATE_TABLE =
            CREATE_TABLE + SPACE +
                    BBDatabaseContract.Budgets.TABLE_NAME + LEFT_PAREN +
                    BBDatabaseContract.Budgets.COLUMN_ID + SPACE + BBDatabaseContract.Budgets.ID_TYPE + COMMA_SEP +
                    BBDatabaseContract.Budgets.COLUMN_NAME + SPACE + BBDatabaseContract.Budgets.NAME_TYPE +
                    RIGHT_PAREN;

    private static final String ADD_EULA_COLUMN_STATEMENT = ALTER_TABLE + SPACE +
           BBDatabaseContract.GlobalMetaData.TABLE_NAME + SPACE + ADD_COLUMN + SPACE +
            BBDatabaseContract.GlobalMetaData.COLUMN_AGREED_EULA + SPACE +
            BBDatabaseContract.GlobalMetaData.AGREED_EULA_TYPE;

    /**
     * Constructor for the BBDatabaseOpenHelper class. This is set to private as these objects
     * should only be opened via the getInstance singleton method.
     * @param context - the application's context
     */
    private BBDatabaseOpenHelper(Application context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    /**
     * Use this static method to get the singleton instance of the database helper object. Creates
     * the instance if it does not yet exist.
     * @param context - the context from which this method is called
     * @return the singleton instance of the BBDatabaseOpenHelper class
     */
    public static synchronized BBDatabaseOpenHelper getInstance(Activity context)
    {
        if (sInstance == null) {
            sInstance = new BBDatabaseOpenHelper(context.getApplication());
        }
        return sInstance;
    }

    /**
     *
     * Called when the bad budget database does not yet exist. Would be the case for a new user.
     * Creates the two global tables, global meta data and the budgets map table. Then creates
     * a single initial budget and sets it as the default.
     * @param writeableDB - the sql database
     */
    public void onCreate(SQLiteDatabase writeableDB)
    {
        System.out.println(GLOBAL_META_DATA_CREATE_TABLE);
        writeableDB.execSQL(GLOBAL_META_DATA_CREATE_TABLE);
        writeableDB.execSQL(BUDGETS_CREATE_TABLE);

        ContentValues globalMetaDataValues = new ContentValues();
        globalMetaDataValues.put(BBDatabaseContract.GlobalMetaData.COLUMN_LAST_ID, 0);
        globalMetaDataValues.put(BBDatabaseContract.GlobalMetaData.COLUMN_AGREED_EULA, EULA_NOT_AGREE);

        writeableDB.insert(BBDatabaseContract.GlobalMetaData.TABLE_NAME, null, globalMetaDataValues);

        BBDatabaseContract.setDefaultBudgetId(writeableDB, createNewBudget(writeableDB, FIRST_BUDGET_NAME));

        addSampleBudgetData(writeableDB);
    }

    //TODO look into this method more, should check read only?
    //TODO move foreing key enabling to onConfigure
    public void onOpen(SQLiteDatabase db)
    {
        db.execSQL(ENABLE_FOREIGN_KEYS);
    }

    /**
     * In general this method handles any changes between different db versions.
     *
     * From version 1 to version 2 of our database we need to add the general history table to
     * each of the existing budgets in our db.
     *
     * Version 3 of the DB adds the EULA agreement field for checking.
     * @param db - The database
     * @param oldVersion - the old version number
     * @param newVersion - the new version number
     */
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        //Version 2 of the DB adds the General History Table (to all existing budgets)
        if (oldVersion < 2) {
            String[] projection = {
                    BBDatabaseContract.Budgets.COLUMN_ID
            };

            String sortOrder =
                    BBDatabaseContract.Budgets.COLUMN_ID;

            Cursor cursor = db.query(
                    BBDatabaseContract.Budgets.TABLE_NAME,
                    projection,
                    null,
                    null,
                    null,
                    null,
                    sortOrder
            );
            int idIndex = cursor.getColumnIndexOrThrow(BBDatabaseContract.Budgets.COLUMN_ID);

            while (cursor.moveToNext()) {
                int currBudgetId = cursor.getInt(idIndex);
                String createGeneralHistoryItemTableStatement = createTableStatement(BBDatabaseContract.GeneralHistoryItems.TABLE_NAME, currBudgetId, GENERAL_HISTORY_ITEM_COLUMNS, GENERAL_HISTORY_ITEM_COLUMN_TYPES,
                        null, null, null, false);
                db.execSQL(createGeneralHistoryItemTableStatement);
            }
        }

        if (oldVersion < 3)
        {
            //Version 3 of the DB adds the field for checking if the user agreed to the EULA.
            db.execSQL(ADD_EULA_COLUMN_STATEMENT);

            ContentValues globalMetaDataValues = new ContentValues();
            globalMetaDataValues.put(BBDatabaseContract.GlobalMetaData.COLUMN_AGREED_EULA, EULA_EXTREME_EARLY_ADOPT);

            db.update(BBDatabaseContract.GlobalMetaData.TABLE_NAME, globalMetaDataValues, null, null);
        }
    }

    /**
     * Private helper to add sample budget data to a newly created budget. This only adds objects to the database and thus
     * a call populating the data from the db to memory would be needed following a call to this method. Populates
     * a single gain and account. The account is set to the budget source. Then sets a variable number of losses, budget items,
     * and savings objects. All object are defined in BadBudgetApplication as arrays or constants.
     * @param writeableDB - the database to put the sample data into.
     */
    private void addSampleBudgetData(SQLiteDatabase writeableDB)
    {
        ContentValues accountValues = new ContentValues();
        ContentValues gainValues = new ContentValues();
        ContentValues budgetPrefsValues = new ContentValues();

        BadBudgetApplication app = (BadBudgetApplication)context;

        //Insert sample objects into the database, they will be extracted and added to in memory
        //when populate user data is called

        //Sample Account
        accountValues.put(BBDatabaseContract.CashAccounts.COLUMN_NAME, BadBudgetApplication.SAMPLE_BUDGET_ACCOUNT_DESCR);
        accountValues.put(BBDatabaseContract.CashAccounts.COLUMN_VALUE, BadBudgetApplication.SAMPLE_BUDGET_ACCOUNT);
        accountValues.put(BBDatabaseContract.CashAccounts.COLUMN_QUICK_LOOK, BadBudgetApplication.SAMPLE_BUDGET_ACCOUNT_QUICKLOOK);
        writeableDB.insert(BBDatabaseContract.CashAccounts.TABLE_NAME + "_" + BBDatabaseContract.getDefaultBudgetId(writeableDB), null, accountValues);


        //Sample Gain
        gainValues.put(BBDatabaseContract.Gains.COLUMN_DESCRIPTION, BadBudgetApplication.SAMPLE_BUDGET_GAIN_DESCR);
        gainValues.put(BBDatabaseContract.Gains.COLUMN_AMOUNT, BadBudgetApplication.SAMPLE_BUDGET_GAIN);
        gainValues.put(BBDatabaseContract.Gains.COLUMN_FREQUENCY, BBDatabaseContract.dbFrequencyToInteger(BadBudgetApplication.SAMPLE_BUDGET_GAIN_FREQ));
        gainValues.put(BBDatabaseContract.Gains.COLUMN_NEXT_GAIN, BBDatabaseContract.dbDateToString(getSampleNextDate(app.getToday(), BadBudgetApplication.SAMPLE_BUDGET_GAIN_FREQ)));
        gainValues.put(BBDatabaseContract.Gains.COLUMN_END_DATE, (String)null);
        gainValues.put(BBDatabaseContract.Gains.COLUMN_DESTINATION, BadBudgetApplication.SAMPLE_BUDGET_ACCOUNT_DESCR);
        writeableDB.insert(BBDatabaseContract.Gains.TABLE_NAME + "_" + BBDatabaseContract.getDefaultBudgetId(writeableDB), null, gainValues);

         /* Set the source for our budget. The budget prefs should already be initialized without a budget source set */
        budgetPrefsValues.put(BBDatabaseContract.BudgetPreferences.COLUMN_BUDGET_SOURCE, BadBudgetApplication.SAMPLE_BUDGET_ACCOUNT_DESCR);
        writeableDB.update(BBDatabaseContract.BudgetPreferences.TABLE_NAME + "_" +  BBDatabaseContract.getDefaultBudgetId(writeableDB), budgetPrefsValues, null, null);

        //Sample Savings Accounts
        for (int i = 0; i < BadBudgetApplication.SAMPLE_BUDGET_SAVINGS_DESCR.length; i++) {

            ContentValues values = new ContentValues();
            values.put(BBDatabaseContract.CashAccounts.COLUMN_NAME, BadBudgetApplication.SAMPLE_BUDGET_SAVINGS_DESCR[i]);
            values.put(BBDatabaseContract.CashAccounts.COLUMN_VALUE, BadBudgetApplication.SAMPLE_BUDGET_SAVINGS_VALUES[i]);
            values.put(BBDatabaseContract.CashAccounts.COLUMN_INTEREST_RATE, BadBudgetApplication.SAMPLE_BUDGET_SAVINGS_INTEREST[i]);

            if (BadBudgetApplication.SAMPLE_BUDGET_SAVINGS_QUICKLOOK[i]) {
                values.put(BBDatabaseContract.CashAccounts.COLUMN_QUICK_LOOK, BBDatabaseContract.TRUE_VALUE);
            } else {
                values.put(BBDatabaseContract.CashAccounts.COLUMN_QUICK_LOOK, BBDatabaseContract.FALSE_VALUE);
            }

            values.put(BBDatabaseContract.CashAccounts.COLUMN_SAVINGS, BBDatabaseContract.TRUE_VALUE);
            values.put(BBDatabaseContract.CashAccounts.COLUMN_GOAL, BadBudgetApplication.SAMPLE_BUDGET_SAVINGS_GOAL[i]);
            values.put(BBDatabaseContract.CashAccounts.COLUMN_CONTRIBUTION_AMOUNT, BadBudgetApplication.SAMPLE_BUDGET_SAVINGS_CONTRI[i]);
            values.put(BBDatabaseContract.CashAccounts.COLUMN_CONTRIBUTION_FREQUENCY, BBDatabaseContract.dbFrequencyToInteger(BadBudgetApplication.SAMPLE_BUDGET_SAVINGS_FREQ[i]));
            values.put(BBDatabaseContract.CashAccounts.COLUMN_NEXT_CONTRIBUTION, BBDatabaseContract.dbDateToString(getSampleNextDate(app.getToday(), BadBudgetApplication.SAMPLE_BUDGET_SAVINGS_FREQ[i])));
            values.put(BBDatabaseContract.CashAccounts.COLUMN_GOAL_DATE, (String)null);
            values.put(BBDatabaseContract.CashAccounts.COLUMN_END_DATE, (String)null);
            values.put(BBDatabaseContract.CashAccounts.COLUMN_SOURCE_NAME, BadBudgetApplication.SAMPLE_BUDGET_ACCOUNT_DESCR);
            writeableDB.insert(BBDatabaseContract.CashAccounts.TABLE_NAME + "_" + BBDatabaseContract.getDefaultBudgetId(writeableDB), null, values);
        }

        //Sample Losses
        for (int i = 0; i < BadBudgetApplication.SAMPLE_BUDGET_LOSSES_DESCR.length; i++)
        {
            ContentValues values = new ContentValues();
            BBDatabaseOpenHelper.sampleLossPopulate(values, BadBudgetApplication.SAMPLE_BUDGET_LOSSES_DESCR[i],
                    BadBudgetApplication.SAMPLE_BUDGET_LOSSES_FREQUENCIES[i], BadBudgetApplication.SAMPLE_BUDGET_LOSSES_VALUES[i],
                    getSampleNextDate(app.getToday(), BadBudgetApplication.SAMPLE_BUDGET_LOSSES_FREQUENCIES[i]), null,
                    BadBudgetApplication.SAMPLE_BUDGET_ACCOUNT_DESCR);
            writeableDB.insert(BBDatabaseContract.Losses.TABLE_NAME + "_" + BBDatabaseContract.getDefaultBudgetId(writeableDB), null, values);
        }

        //Sample Budget Items
        for (int i = 0; i < BadBudgetApplication.SAMPLE_BUDGET_ITEMS_DESCR.length; i++)
        {
            ContentValues values = new ContentValues();
            BBDatabaseOpenHelper.sampleBudgetItemPopulate(values, BadBudgetApplication.SAMPLE_BUDGET_ITEMS_DESCR[i],
                    BadBudgetApplication.SAMPLE_BUDGET_ITEMS_FREQUENCIES[i], BadBudgetApplication.SAMPLE_BUDGET_ITEMS_VALUES[i],
                    getSampleNextDate(app.getToday(), BadBudgetApplication.SAMPLE_BUDGET_ITEMS_FREQUENCIES[i]),
                    null, false, BadBudgetApplication.SAMPLE_BUDGET_ITEMS_PLUS_MINUS[i]);
            writeableDB.insert(BBDatabaseContract.BudgetItems.TABLE_NAME + "_" + BBDatabaseContract.getDefaultBudgetId(writeableDB), null, values);
        }

        ContentValues metaValues = new ContentValues();
        metaValues.put(BBDatabaseContract.BBMetaData.COLUMN_LAST_UPDATE, BBDatabaseContract.dbDateToString(app.getToday()));
        writeableDB.insert(BBDatabaseContract.BBMetaData.TABLE_NAME + "_" +
                BBDatabaseContract.getDefaultBudgetId(writeableDB), null, metaValues);

    }

    /**
     * Private helper method that given today's date and a freq gets the default next date for
     * our sample budget objects. Daily->tomorrow, Weekly->Next Sunday, BiWeekly->Next Next Sunday,
     * Monthly->First of the next month, yearly->Jan 1 of the next year
     * @param today - today's date
     * @param freq - the freq of the budget object
     * @return the default next date for a sample bad budget object
     */
    private static Date getSampleNextDate(Date today, Frequency freq)
    {
        Calendar tmrwCal = Calendar.getInstance();
        tmrwCal.setTime(today);
        tmrwCal.add(Calendar.DAY_OF_YEAR, 1);

        Calendar nextMonthCal = Calendar.getInstance();
        nextMonthCal.setTime(today);
        nextMonthCal.add(Calendar.MONTH, 1);
        nextMonthCal.set(nextMonthCal.get(Calendar.YEAR), nextMonthCal.get(Calendar.MONTH), 1);

        Calendar nextWeekCal = Calendar.getInstance();
        nextWeekCal.setTime(today);
        int dayDiff = nextWeekCal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        nextWeekCal.add(Calendar.DAY_OF_WEEK, 7-dayDiff);

        Calendar biWeekCal = Calendar.getInstance();
        biWeekCal.setTime(nextWeekCal.getTime());
        biWeekCal.add(Calendar.WEEK_OF_YEAR, 1);

        Calendar nextYearCal = Calendar.getInstance();
        nextYearCal.setTime(today);
        nextYearCal.add(Calendar.YEAR, 1);
        nextYearCal.set(nextYearCal.get(Calendar.YEAR), Calendar.JANUARY, 1);

        switch (freq)
        {
            case daily:
            {
                return tmrwCal.getTime();
            }
            case weekly:
            {
                return nextWeekCal.getTime();
            }
            case biWeekly:
            {
                return biWeekCal.getTime();
            }
            case monthly:
            {
                return nextMonthCal.getTime();
            }
            case yearly:
            {
                return nextYearCal.getTime();
            }
            default:
            {
                return null;
            }
        }
    }

    /**
     * Private helper method used to correctly populate the contentvalues of a loss for insertion into
     * the bad budget database.
     * @param values - An empty ContentValues object that should be populated with the passed fields
     * @param expense - the expense description of the loss
     * @param freq - the frequency of the loss
     * @param amount - the amount of the loss
     * @param nextLoss - when the next loss will occur
     * @param end - when this loss will end
     * @param source - the source of this loss
     */
    private static void sampleLossPopulate(ContentValues values, String expense, Frequency freq, double amount,
                                    Date nextLoss, Date end, String source)
    {
        values.put(BBDatabaseContract.Losses.COLUMN_EXPENSE, expense);
        values.put(BBDatabaseContract.Losses.COLUMN_FREQUENCY, BBDatabaseContract.dbFrequencyToInteger(freq));
        values.put(BBDatabaseContract.Losses.COLUMN_AMOUNT, amount);
        values.put(BBDatabaseContract.Losses.COLUMN_NEXT_LOSS, BBDatabaseContract.dbDateToString(nextLoss));
        values.put(BBDatabaseContract.Losses.COLUMN_END_DATE, BBDatabaseContract.dbDateToString(end));
        values.put(BBDatabaseContract.Losses.COLUMN_SOURCE, source);
    }

    /**
     * Private helper method that populates an empty ContentValues object with the passed fields with
     * the intention of inserting that values object into our bad budget database as a budget item.
     * @param values - the values object to populate with the intention of inserting it into the database
     * @param expense - the budget item's expense description
     * @param freq -  the frequency of the budget item
     * @param amount - the amount budgeted to the budget item
     * @param nextLoss - when the next loss ocurrs for this budget item
     * @param end - the end date of this budget item
     * @param prorated - whether the budget item is prorated
     * @param plusMinusAmount - the plus minus value for this budget item in the tracker
     */
    private static void sampleBudgetItemPopulate(ContentValues values, String expense, Frequency freq, double amount,
                                                 Date nextLoss, Date end, boolean prorated, double plusMinusAmount)
    {
        values.put(BBDatabaseContract.BudgetItems.COLUMN_DESCRIPTION, expense);
        values.put(BBDatabaseContract.BudgetItems.COLUMN_AMOUNT, amount);
        values.put(BBDatabaseContract.BudgetItems.COLUMN_FREQUENCY, BBDatabaseContract.dbFrequencyToInteger(freq));
        values.put(BBDatabaseContract.BudgetItems.COLUMN_NEXT_LOSS, BBDatabaseContract.dbDateToString(nextLoss));
        values.put(BBDatabaseContract.BudgetItems.COLUMN_END_DATE, BBDatabaseContract.dbDateToString(end));
        values.put(BBDatabaseContract.BudgetItems.COLUMN_PRORATED_START, BBDatabaseContract.dbBooleanToInteger(prorated));

        values.put(BBDatabaseContract.BudgetItems.COLUMN_MINUS_AMOUNT, plusMinusAmount);
        values.put(BBDatabaseContract.BudgetItems.COLUMN_PLUS_AMOUNT, plusMinusAmount);
        values.put(BBDatabaseContract.BudgetItems.COLUMN_REMAINING_AMOUNT, amount);
    }

    /**
     * Private helper method to form the create statement for a generic table with an id able to be
     * specified to create multiple distinct tables, foreign key optional specification, and timestamp optional
     * specification.
     * @param tableName - the table name minus the unique identifier which is appended on here
     * @param budgetId - the unique identifier so that multiple tables of the same contract can be distinctly created
     * @param columns - all of the columns in this table as strings
     * @param columnTypes - all of the column types (should be a 1 to 1 match with the columns array) as strings
     * @param foreignKeyColumn - optional foreign key column in this table, pass null if none
     * @param referencesTable - optional table that the foreign key is referencing, should be null if no foreign key
     * @param referencesColumn - optional references column that the foreign key is referencing in the references table, should be null if no foreign key
     * @param timestampEnabled - enables a timestamp column for each row in the table
     * @return a string that can be executed as sql to create the table using the given passed arguments.
     */
    private static String createTableStatement(String tableName, int budgetId, String[] columns,
                                               String[] columnTypes, String foreignKeyColumn, String referencesTable, String referencesColumn,
                                               boolean timestampEnabled)
    {
        String statement = CREATE_TABLE + SPACE + tableName + "_" + budgetId + SPACE + LEFT_PAREN;

        for (int i = 0; i < columns.length - 1; i++)
        {
            statement += (columns[i] + SPACE + columnTypes[i] + COMMA_SEP);
        }
        statement += (columns[columns.length - 1] + SPACE + columnTypes[columns.length - 1]);

        if (foreignKeyColumn != null)
        {
            statement += (COMMA_SEP);
            statement += (FOREIGN_KEY + LEFT_PAREN + foreignKeyColumn + RIGHT_PAREN + SPACE + REFERENCES + SPACE +
                    referencesTable + "_" + budgetId + LEFT_PAREN + referencesColumn + RIGHT_PAREN);
        }

        if (timestampEnabled)
        {
            statement += (COMMA_SEP);
            statement += (DEFAULT_TIMESTAMP_COLUMN_STATEMENT);
        }

        statement += RIGHT_PAREN;
        return statement;
    }

    /**
     * Creates a budget with the given name and a unique id with the mapping from the name to the
     * id being added to the global budgets table
     * Creates all the necessary tables for a stand alone budget including tables for user
     * cash accounts (regular and savings), debts (regular, credit cards, and loans) , gains,
     * losses, budget items, budget preferences, tracker history, general history, and meta data.
     * Also initializes the budget prefs to default values, although source is not set
     * (as it cannot be in a new budget). Does not change the default budget id
     * @param writeableDB - database instance to add the new budget to
     * @param name - the name of the new budget being added
     * TODO - currently ids only go up and deleted ids are not reused, if user creates enough budgets could cause issue 1/18/2017
     */
    public static int createNewBudget(SQLiteDatabase writeableDB, String name)
    {
        int lastId = 0;

        String[] projection = {
                BBDatabaseContract.GlobalMetaData.COLUMN_LAST_ID
        };

        Cursor cursor = writeableDB.query(
                BBDatabaseContract.GlobalMetaData.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        );
        int lastIdIndex = cursor.getColumnIndexOrThrow(BBDatabaseContract.GlobalMetaData.COLUMN_LAST_ID);

        if (cursor.moveToNext())
        {
            lastId = cursor.getInt(lastIdIndex);
        }

        int nextId = lastId + 1;

        String createCashTableStatement = createTableStatement(BBDatabaseContract.CashAccounts.TABLE_NAME, nextId, CASH_ACCOUNT_COLUMNS, CASH_ACCOUNT_COLUMN_TYPES,
                                BBDatabaseContract.CashAccounts.FOREIGN_KEY_COLUMN, BBDatabaseContract.CashAccounts.REFERENCES_TABLE, BBDatabaseContract.CashAccounts.
                                REFERENCES_COLUMN, false);

        String createDebtTableStatement = createTableStatement(BBDatabaseContract.Debts.TABLE_NAME, nextId, DEBT_COLUMNS, DEBT_COLUMN_TYPES,
                BBDatabaseContract.Debts.FOREIGN_KEY_COLUMN, BBDatabaseContract.Debts.REFERENCES_TABLE, BBDatabaseContract.Debts.
                        REFERENCES_COLUMN, false);

        String createGainTableStatement = createTableStatement(BBDatabaseContract.Gains.TABLE_NAME, nextId, GAIN_COLUMNS, GAIN_COLUMN_TYPES,
                BBDatabaseContract.Gains.FOREIGN_KEY_COLUMN, BBDatabaseContract.Gains.REFERENCES_TABLE, BBDatabaseContract.Gains.
                        REFERENCES_COLUMN, false);

        String createLossTableStatement = createTableStatement(BBDatabaseContract.Losses.TABLE_NAME, nextId, LOSS_COLUMNS, LOSS_COLUMN_TYPES,
                null, null, null, false);

        String createBudgetPreferenceTableStatement = createTableStatement(BBDatabaseContract.BudgetPreferences.TABLE_NAME, nextId, BUDGET_PREFERENCE_COLUMNS, BUDGET_PREFERENCE_COLUMN_TYPES,
                null, null, null, false);

        String createBudgetItemTableStatement = createTableStatement(BBDatabaseContract.BudgetItems.TABLE_NAME, nextId, BUDGET_ITEM_COLUMNS, BUDGET_ITEM_COLUMN_TYPES,
                null, null, null, false);

        String createTrackerHistoryItemTableStatement = createTableStatement(BBDatabaseContract.TrackerHistoryItems.TABLE_NAME, nextId, TRACKER_HISTORY_ITEM_COLUMNS, TRACKER_HISTORY_ITEM_COLUMN_TYPES,
                null, null, null, true);

        String createGeneralHistoryItemTableStatement = createTableStatement(BBDatabaseContract.GeneralHistoryItems.TABLE_NAME, nextId, GENERAL_HISTORY_ITEM_COLUMNS, GENERAL_HISTORY_ITEM_COLUMN_TYPES,
                null, null, null, false);

        String createBudgetMetaDataTableStatement = createTableStatement(BBDatabaseContract.BBMetaData.TABLE_NAME, nextId, BUDGET_META_DATA_COLUMNS, BUDGET_META_DATA_COLUMN_TYPES,
                null, null, null, false);

        writeableDB.execSQL(createCashTableStatement);
        writeableDB.execSQL(createDebtTableStatement);
        writeableDB.execSQL(createGainTableStatement);
        writeableDB.execSQL(createLossTableStatement);
        writeableDB.execSQL(createBudgetPreferenceTableStatement);
        writeableDB.execSQL(createBudgetItemTableStatement);
        writeableDB.execSQL(createTrackerHistoryItemTableStatement);
        writeableDB.execSQL(createGeneralHistoryItemTableStatement);
        writeableDB.execSQL(createBudgetMetaDataTableStatement);

        /* Insert the default preferences into the budget preferences tables - no source is set */
        ContentValues values = new ContentValues();
        values.put(BBDatabaseContract.BudgetPreferences.COLUMN_AUTO_UPDATE, true);
        values.put(BBDatabaseContract.BudgetPreferences.COLUMN_AUTO_RESET, true);
        values.put(BBDatabaseContract.BudgetPreferences.COLUMN_REMAIN_AMOUNT_ACTION, BBDatabaseContract.dbRemainAmountActionToInteger(RemainAmountAction.accumulates));
        values.put(BBDatabaseContract.BudgetPreferences.COLUMN_WEEKLY_RESET, Calendar.SUNDAY);
        values.put(BBDatabaseContract.BudgetPreferences.COLUMN_MONTHLY_RESET, 1);
        writeableDB.insert(BBDatabaseContract.BudgetPreferences.TABLE_NAME + "_" + nextId, null, values);

        /* Update the global tables */
        ContentValues globalMetaDataValues = new ContentValues();
        globalMetaDataValues.put(BBDatabaseContract.GlobalMetaData.COLUMN_LAST_ID, nextId);
        writeableDB.update(BBDatabaseContract.GlobalMetaData.TABLE_NAME, globalMetaDataValues, null, null);

        ContentValues budgetsValues = new ContentValues();
        budgetsValues.put(BBDatabaseContract.Budgets.COLUMN_ID, nextId);
        budgetsValues.put(BBDatabaseContract.Budgets.COLUMN_NAME, name);
        writeableDB.insert(BBDatabaseContract.Budgets.TABLE_NAME, null, budgetsValues);

        return nextId;
    }

    /**
     * Given an existing budget's id, this method creates a new empty budget with the given name and
     * copies all data from the existing budget's tables into the corresponding newly created tables.
     * @param writeableDB - the database connection
     * @param existingBudgetId - the existing budget id that we are copying from
     * @return the id of the new budget tables that were copied into
     */
    public static int copyExistingBudget(SQLiteDatabase writeableDB, int existingBudgetId, String newBudgetName)
    {
        int newBudgetId = BBDatabaseOpenHelper.createNewBudget(writeableDB, newBudgetName);

        writeableDB.execSQL(constructCopyStatment(BBDatabaseContract.CashAccounts.TABLE_NAME, existingBudgetId, newBudgetId));
        writeableDB.execSQL(constructCopyStatment(BBDatabaseContract.Debts.TABLE_NAME, existingBudgetId, newBudgetId));
        writeableDB.execSQL(constructCopyStatment(BBDatabaseContract.Gains.TABLE_NAME, existingBudgetId, newBudgetId));
        writeableDB.execSQL(constructCopyStatment(BBDatabaseContract.Losses.TABLE_NAME, existingBudgetId, newBudgetId));

        String sqlDeleteDefaultBudgetPreferences = COPY_DELETE_FROM + " " + BBDatabaseContract.BudgetPreferences.TABLE_NAME + "_" + newBudgetId;
        writeableDB.execSQL(sqlDeleteDefaultBudgetPreferences);
        writeableDB.execSQL(constructCopyStatment(BBDatabaseContract.BudgetPreferences.TABLE_NAME, existingBudgetId, newBudgetId));

        writeableDB.execSQL(constructCopyStatment(BBDatabaseContract.BudgetItems.TABLE_NAME, existingBudgetId, newBudgetId));
        writeableDB.execSQL(constructCopyStatment(BBDatabaseContract.TrackerHistoryItems.TABLE_NAME, existingBudgetId, newBudgetId));
        writeableDB.execSQL(constructCopyStatment(BBDatabaseContract.GeneralHistoryItems.TABLE_NAME, existingBudgetId, newBudgetId));
        writeableDB.execSQL(constructCopyStatment(BBDatabaseContract.BBMetaData.TABLE_NAME, existingBudgetId, newBudgetId));

        return newBudgetId;
    }

    /**
     * Private helper method for constructing an executable sql statemtent that copies items from
     * the existing budget tables to the new empty budget tables (specified by existingBudgetId and
     * newBudgetId respectively)
     * @param tableName - the name of the table being copied
     * @param existingBudgetId - the id of the existing budget table to copy items from
     * @param newBudgetId - the id of a newly created set of budget tables to copy items into
     * @return an executable sql statement that copies entries from the existing budget table to the
     *          new budget tables.
     */
    private static String constructCopyStatment(String tableName, int existingBudgetId, int newBudgetId)
    {
        String copyStatement = COPY_INSERT_INTO + SPACE + tableName + "_" +
                newBudgetId + SPACE + COPY_SELECT_FROM + SPACE + tableName + "_" + existingBudgetId;
        return copyStatement;
    }

    /**
     * Static method that drops all tables with the passed budget id. Additionally deletes the reference
     * to this budget from the budgets table effectively deleting this budget from our database.
     * TODO - The budget id is not able to be reused 1/18/2017
     * @param writeableDB - the db to delete our budget form
     * @param budgetId - the budget id to delete
     */
    public static void deleteBudget(SQLiteDatabase writeableDB, int budgetId)
    {
        String deleteCashTable = DROP_TABLE + SPACE + BBDatabaseContract.CashAccounts.TABLE_NAME + "_" + budgetId;
        String deleteDebtTable = DROP_TABLE + SPACE + BBDatabaseContract.Debts.TABLE_NAME + "_" + budgetId;
        String deleteGainTable = DROP_TABLE + SPACE + BBDatabaseContract.Gains.TABLE_NAME + "_" + budgetId;
        String deleteLossTable = DROP_TABLE + SPACE + BBDatabaseContract.Losses.TABLE_NAME + "_" + budgetId;
        String deleteBudgetItemTable = DROP_TABLE + SPACE + BBDatabaseContract.BudgetItems.TABLE_NAME + "_" + budgetId;
        String deleteBudgetPrefsTable = DROP_TABLE + SPACE + BBDatabaseContract.BudgetPreferences.TABLE_NAME + "_" + budgetId;
        String deleteTrackerHistoryTable = DROP_TABLE + SPACE + BBDatabaseContract.TrackerHistoryItems.TABLE_NAME + "_" + budgetId;
        String deleteGeneralHistoryTable = DROP_TABLE + SPACE + BBDatabaseContract.GeneralHistoryItems.TABLE_NAME + "_" + budgetId;
        String deleteMetaDataTable = DROP_TABLE + SPACE + BBDatabaseContract.BBMetaData.TABLE_NAME + "_" + budgetId;

        //Need to disable foreign keys as we don't care about any conflicts as all will be resolved as all
        //items will be deleted
        writeableDB.execSQL(DISABLE_FOREIGN_KEYS);

        writeableDB.execSQL(deleteCashTable);
        writeableDB.execSQL(deleteDebtTable);
        writeableDB.execSQL(deleteGainTable);
        writeableDB.execSQL(deleteLossTable);
        writeableDB.execSQL(deleteBudgetItemTable);
        writeableDB.execSQL(deleteBudgetPrefsTable);
        writeableDB.execSQL(deleteTrackerHistoryTable);
        writeableDB.execSQL(deleteGeneralHistoryTable);
        writeableDB.execSQL(deleteMetaDataTable);

         /* Update the global tables */
        String[] whereArgs = {"" + budgetId};
        writeableDB.delete(BBDatabaseContract.Budgets.TABLE_NAME, BBDatabaseContract.Budgets.COLUMN_ID + "=?", whereArgs);

        //Re-enable the foreign key checks
        writeableDB.execSQL(ENABLE_FOREIGN_KEYS);
    }
}
