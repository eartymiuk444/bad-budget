package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.erikartymiuk.badbudgetlogic.budget.Budget;
import com.erikartymiuk.badbudgetlogic.budget.BudgetItem;
import com.erikartymiuk.badbudgetlogic.budget.RemainAmountAction;
import com.erikartymiuk.badbudgetlogic.main.Account;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetInvalidValueException;
import com.erikartymiuk.badbudgetlogic.main.Contribution;
import com.erikartymiuk.badbudgetlogic.main.CreditCard;
import com.erikartymiuk.badbudgetlogic.main.DebtType;
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.Loan;
import com.erikartymiuk.badbudgetlogic.main.MoneyGain;
import com.erikartymiuk.badbudgetlogic.main.MoneyLoss;
import com.erikartymiuk.badbudgetlogic.main.MoneyOwed;
import com.erikartymiuk.badbudgetlogic.main.Payment;
import com.erikartymiuk.badbudgetlogic.main.Prediction;
import com.erikartymiuk.badbudgetlogic.main.SavingsAccount;
import com.erikartymiuk.badbudgetlogic.main.Source;
import com.erikartymiuk.badbudgetlogic.predictdataclasses.PredictDataBudgetItem;
import com.erikartymiuk.badbudgetlogic.predictdataclasses.TransactionHistoryItem;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class defines the Bad Budget Database's table schemas.
 *
 * Created by Erik Artymiuk on 6/10/2016.
 */
public class BBDatabaseContract
{
    /**
     * Empty constructor of the BBDatabaseContract class. This isn't a class that should
     * be instantiated as an object. Its use is defining the table schemas for the various
     * tables needed by the bad budget application.
     */
    public BBDatabaseContract() {};

    /**
     * This method extracts all the relevant user data and preferences from the BB database tables
     * using the passed budget id in the database as our specification of the budget to extract for.
     * Sets the application wide data values of the bad budget data, tracker history, general history, and the
     * selected budget.
     * Additionally this checks if an update is necessary and runs one on the specified budget if so.
     * User input should be blocked when this runs via a progress dialog
     * @param application - the application whose values need to be set
     * @param writeableDB - the database to extract from and update if necessary
     * @param budgetId - the budget id of the budget we wish to load our data from
     */
    public static void populateUserData(BadBudgetApplication application, SQLiteDatabase writeableDB, int budgetId)
    {
        //Read in all the user data and create the bad budget data object
        BadBudgetData bbd = new BadBudgetData();
        List<TrackerHistoryItem> trackerHistoryItemList = new ArrayList<TrackerHistoryItem>();
        List<TransactionHistoryItem> transactionHistoryItems = new ArrayList<>();

        //First we will extract all the accounts (regular and savings) from our accounts table.
        extractAccountsFromDataBase(writeableDB, bbd, budgetId);
        //Next extract all the debts
        extractDebtsFromDataBase(writeableDB, bbd, budgetId);
        //Get all of the user gains
        extractGainsFromDataBase(writeableDB, bbd, budgetId);
        //Next all the user losses
        extractLossesFromDataBase(writeableDB, bbd, budgetId);

        //Need to extract auto update and remain amount action separate from the other budget prefs
        Object[] objArr = getAutoUpdateAndRemainAction(writeableDB, budgetId);

        //Finally extract budget prefs and budget items
        extractBudget(writeableDB, bbd, budgetId, (RemainAmountAction)objArr[1]);

        //Get our list of tracker history items
        extractTrackerHistoryItems(writeableDB, trackerHistoryItemList, budgetId);

        //Get our list of general history items
        extractGeneralHistoryItems(writeableDB, transactionHistoryItems, budgetId);

        //Check when our last update was and update both in memory and our db if necessary
        checkLastUpdate(writeableDB, bbd, application.getToday(), trackerHistoryItemList, transactionHistoryItems, budgetId, (Boolean)objArr[0]);

        application.setAutoUpdateSelectedBudget((Boolean)objArr[0]);
        application.setRemainAmountActionSelectedBudget((RemainAmountAction)objArr[1]);

        application.setBadBudgetUserData(bbd);
        application.setTrackerHistoryItems(trackerHistoryItemList);
        application.setGeneralHistoryItems(transactionHistoryItems);
        application.setSelectedBudgetId(budgetId);
    }

    /**
     * Private helper method that extracts the auto update and remain amount action from our database
     * for our remaining budget preferences. These are not extracted with the others as auto update
     * isn't set as part of the budget object and the remain amount action is set on a budget item basis
     * in the logic code rather than as a budget basis as it is in the application code.
     * @param writeableDB - the database to extract from
     * @param budgetId - the budget id of the budget to extract from
     * @return return a 2 object array. The first object is a boolean indicating the auto update flag
     *                  The second object is a RemainAmountAction object indicating the remainAmountAction
     *
     */
    private static Object[] getAutoUpdateAndRemainAction(SQLiteDatabase writeableDB, int budgetId)
    {
        String[] projection = {
                BudgetPreferences.COLUMN_AUTO_UPDATE,
                BudgetPreferences.COLUMN_REMAIN_AMOUNT_ACTION
        };

        Cursor cursor = writeableDB.query(
                BudgetPreferences.TABLE_NAME + "_" + budgetId,
                projection,
                null,
                null,
                null,
                null,
                null
        );

        int autoUpdateIndex = cursor.getColumnIndexOrThrow(BudgetPreferences.COLUMN_AUTO_UPDATE);
        int remainAmountActionIndex = cursor.getColumnIndexOrThrow(BudgetPreferences.COLUMN_REMAIN_AMOUNT_ACTION);

        boolean autoUpdate = false;
        RemainAmountAction remainAmountAction = RemainAmountAction.accumulates;

        if (cursor.moveToNext()) {
            autoUpdate = dbIntegerToBoolean(cursor.getInt(autoUpdateIndex));
            remainAmountAction = dbIntegerToRemainAmountAction(cursor.getInt(remainAmountActionIndex));

        }

        Object[] objArr = new Object[2];
        objArr[0] = new Boolean(autoUpdate);
        objArr[1] = remainAmountAction;
        return objArr;
    }

    /**
     * Gets the current default budget Id set in our global meta data table.
     * @param writeableDB - the database to get our default budget id from
     * @return - the default budget id in our global meta data table
     */
    public static int getDefaultBudgetId(SQLiteDatabase writeableDB)
    {
        String[] projection = {
                GlobalMetaData.COLUMN_DEFAULT_BUDGET_ID
        };

        Cursor cursor = writeableDB.query(
                GlobalMetaData.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        );

        int defaultBudgetIdIndex = cursor.getColumnIndexOrThrow(GlobalMetaData.COLUMN_DEFAULT_BUDGET_ID);
        int defaultBudgetId = 0;
        if (cursor.moveToNext()) {
            defaultBudgetId = cursor.getInt(defaultBudgetIdIndex);
        }
        return defaultBudgetId;
    }

    /**
     * Sets the default budget id in our global meta data table
     * @param writableDB - the database to update
     * @param defaultBudgetId - the new budget id to use as the default
     */
    public static void setDefaultBudgetId(SQLiteDatabase writableDB, int defaultBudgetId)
    {
        ContentValues globalMetaDataValues = new ContentValues();
        globalMetaDataValues.put(GlobalMetaData.COLUMN_DEFAULT_BUDGET_ID, defaultBudgetId);
        writableDB.update(GlobalMetaData.TABLE_NAME, globalMetaDataValues, null, null);
    }

    /**
     * Gets the current budgets from our database in a map from the budget name to the budget id.
     * @return - a map of our budgets from the budget name to the budget id
     */
    public static Map<String, Integer> getBudgetMapNameToID(SQLiteDatabase writeableDB)
    {
        HashMap<String, Integer> map = new HashMap<String, Integer>();

        String[] projection = {
                Budgets.COLUMN_NAME,
                Budgets.COLUMN_ID,
        };

        Cursor cursor = writeableDB.query(
                Budgets.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        );

        int budgetNameIndex = cursor.getColumnIndexOrThrow(Budgets.COLUMN_NAME);
        int budgetIdIndex = cursor.getColumnIndexOrThrow(Budgets.COLUMN_ID);

        while (cursor.moveToNext()) {
            map.put(cursor.getString(budgetNameIndex), cursor.getInt(budgetIdIndex));
        }

        return map;
    }

    /**
     * Gets the current budgets from our database in a map from the budget id to the budget name
     * @return - a map of our budgets from the budget id to the budget name
     */
    public static Map<Integer, String> getBudgetMapIDToName(SQLiteDatabase writeableDB)
    {
        HashMap<Integer, String> map = new HashMap<Integer, String>();

        String[] projection = {
                Budgets.COLUMN_NAME,
                Budgets.COLUMN_ID,
        };

        Cursor cursor = writeableDB.query(
                Budgets.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        );

        int budgetNameIndex = cursor.getColumnIndexOrThrow(Budgets.COLUMN_NAME);
        int budgetIdIndex = cursor.getColumnIndexOrThrow(Budgets.COLUMN_ID);

        while (cursor.moveToNext()) {
            map.put(cursor.getInt(budgetIdIndex), cursor.getString(budgetNameIndex));
        }

        return map;
    }

    /**
     * Extracts all tracker history items from our database and inserts them into the application trackerHistoryItemList
     * in order of date and time.
     * @param db - the database to extract the tracker history items from
     * @param trackerHistoryItemList - the applications list of tracker history items to be populated.
     */
    private static void extractTrackerHistoryItems(SQLiteDatabase db, List<TrackerHistoryItem> trackerHistoryItemList, int budgetId)
    {
        String[] projection = {
                TrackerHistoryItems.COLUMN_BUDGET_ITEM_DESCRIPTION,
                TrackerHistoryItems.COLUMN_USER_TRANSACTION_DESCRIPTION,
                TrackerHistoryItems.COLUMN_DATE,
                TrackerHistoryItems.COLUMN_DAY,
                TrackerHistoryItems.COLUMN_TIME,
                TrackerHistoryItems.COLUMN_ACTION,
                TrackerHistoryItems.COLUMN_ACTION_AMOUNT,
                TrackerHistoryItems.COLUMN_ORIGINAL_BUDGET_AMOUNT,
                TrackerHistoryItems.COLUMN_UPDATED_BUDGET_AMOUNT
        };

        /**
         * Sorting tracker history items works by first sorting by the date string. Then to sort within a date
         * we use the created_at field. Auto updates will always be handled prior to any user generated history
         * items and thus the created at fields show up as least recent for a given date.
         */
        String sortOrder = TrackerHistoryItems.COLUMN_DATE + " " + TrackerHistoryItems.SORT_ORDER_DESC +
                            "," + TrackerHistoryItems.COLUMN_CREATED_AT + " " + TrackerHistoryItems.SORT_ORDER_DESC;

        //Going to select all the tracker history items with our query, sorted
        //by date and time. The where clause (including its args), group by, and having
        //statements are null.
        Cursor cursor = db.query(
                TrackerHistoryItems.TABLE_NAME + "_" + budgetId,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );

        int budgetItemDescriptionIndex = cursor.getColumnIndexOrThrow(TrackerHistoryItems.COLUMN_BUDGET_ITEM_DESCRIPTION);
        int userTransactionIndex = cursor.getColumnIndexOrThrow(TrackerHistoryItems.COLUMN_USER_TRANSACTION_DESCRIPTION);
        int dateIndex = cursor.getColumnIndexOrThrow(TrackerHistoryItems.COLUMN_DATE);
        int dayIndex = cursor.getColumnIndexOrThrow(TrackerHistoryItems.COLUMN_DAY);
        int timeIndex = cursor.getColumnIndexOrThrow(TrackerHistoryItems.COLUMN_TIME);
        int actionIndex = cursor.getColumnIndexOrThrow(TrackerHistoryItems.COLUMN_ACTION);
        int addSubtractAmountIndex = cursor.getColumnIndexOrThrow(TrackerHistoryItems.COLUMN_ACTION_AMOUNT);
        int originalBudgetAmountIndex = cursor.getColumnIndexOrThrow(TrackerHistoryItems.COLUMN_ORIGINAL_BUDGET_AMOUNT);
        int updatedBudgetAmountIndex = cursor.getColumnIndexOrThrow(TrackerHistoryItems.COLUMN_UPDATED_BUDGET_AMOUNT);

        while (cursor.moveToNext())
        {
            String budgetItemDescription = cursor.getString(budgetItemDescriptionIndex);
            String userTransactionDescription = cursor.getString(userTransactionIndex);
            String date = cursor.getString(dateIndex);
            String day = cursor.getString(dayIndex);
            String time = cursor.getString(timeIndex);
            TrackerHistoryItem.TrackerAction action = dbIntegerToAction(cursor.getInt(actionIndex));
            double addSubtractAmount = cursor.getDouble(addSubtractAmountIndex);
            double originalBudgetAmount = cursor.getDouble(originalBudgetAmountIndex);
            double updatedBudgetAmount = cursor.getDouble(updatedBudgetAmountIndex);

            TrackerHistoryItem trackerHistoryItem = new TrackerHistoryItem(budgetItemDescription, userTransactionDescription, date, day, time, action, addSubtractAmount, originalBudgetAmount, updatedBudgetAmount);
            trackerHistoryItemList.add(trackerHistoryItem);
        }
    }

    /**
     * Extracts all general history items from our database and inserts them into the passed list
     * to be set as the application's list of transaction history items
     * @param db - the database to extract the general history items from
     * @param transactionHistoryItemList - the applications list (or soon to be list) of general history items to be populated.
     *
     */
    private static void extractGeneralHistoryItems(SQLiteDatabase db, List<TransactionHistoryItem> transactionHistoryItemList, int budgetId)
    {
        String[] projection = {
                GeneralHistoryItems.COLUMN_DESTINATION_ACTION,
                GeneralHistoryItems.COLUMN_DESTINATION_ORIGINAL,
                GeneralHistoryItems.COLUMN_DESTINATION_UPDATED,
                GeneralHistoryItems.COLUMN_SOURCE_ACTION,
                GeneralHistoryItems.COLUMN_SOURCE_ORIGINAL,
                GeneralHistoryItems.COLUMN_SOURCE_UPDATED,
                GeneralHistoryItems.COLUMN_TRANSACTION_AMOUNT,
                GeneralHistoryItems.COLUMN_TRANSACTION_DATE,
                GeneralHistoryItems.COLUMN_TRANSACTION_DESTINATION,
                GeneralHistoryItems.COLUMN_TRANSACTION_SOURCE,
                GeneralHistoryItems.COLUMN_DESTINATION_CAN_SHOW_CHANGE,
                GeneralHistoryItems.COLUMN_SOURCE_CAN_SHOW_CHANGE,
        };


        //Going to select all the general history items with our query, sorted
        //by date, the where clause (including its args), group by, and having
        //statements are null.
        Cursor cursor = db.query(
                GeneralHistoryItems.TABLE_NAME + "_" + budgetId,
                projection,
                null,
                null,
                null,
                null,
                null
        );

        int destinationActionIndex = cursor.getColumnIndexOrThrow(GeneralHistoryItems.COLUMN_DESTINATION_ACTION);
        int destinationOriginalIndex = cursor.getColumnIndexOrThrow(GeneralHistoryItems.COLUMN_DESTINATION_ORIGINAL);
        int destinationUpdatedIndex = cursor.getColumnIndexOrThrow(GeneralHistoryItems.COLUMN_DESTINATION_UPDATED);
        int sourceActionIndex = cursor.getColumnIndexOrThrow(GeneralHistoryItems.COLUMN_SOURCE_ACTION);
        int sourceOriginalIndex = cursor.getColumnIndexOrThrow(GeneralHistoryItems.COLUMN_SOURCE_ORIGINAL);
        int sourceUpdatedIndex = cursor.getColumnIndexOrThrow(GeneralHistoryItems.COLUMN_SOURCE_UPDATED);
        int transactionAmountIndex = cursor.getColumnIndexOrThrow(GeneralHistoryItems.COLUMN_TRANSACTION_AMOUNT);
        int transactionDateIndex = cursor.getColumnIndexOrThrow(GeneralHistoryItems.COLUMN_TRANSACTION_DATE);
        int transactionDestinationIndex = cursor.getColumnIndexOrThrow(GeneralHistoryItems.COLUMN_TRANSACTION_DESTINATION);
        int transactionSourceIndex = cursor.getColumnIndexOrThrow(GeneralHistoryItems.COLUMN_TRANSACTION_SOURCE);
        int destinationCanShowChangeIndex = cursor.getColumnIndexOrThrow(GeneralHistoryItems.COLUMN_DESTINATION_CAN_SHOW_CHANGE);
        int sourceCanShowChangeIndex = cursor.getColumnIndexOrThrow(GeneralHistoryItems.COLUMN_SOURCE_CAN_SHOW_CHANGE);

        while (cursor.moveToNext())
        {
            String destinationActionString = cursor.getString(destinationActionIndex);
            double destinationOriginal = cursor.getDouble(destinationOriginalIndex);
            double destinationUpdated = cursor.getDouble(destinationUpdatedIndex);
            String sourceActionString = cursor.getString(sourceActionIndex);
            double sourceOriginal = cursor.getDouble(sourceOriginalIndex);
            double sourceUpdated = cursor.getDouble(sourceUpdatedIndex);
            double transactionAmount = cursor.getDouble(transactionAmountIndex);
            Date transactionDate = dbStringToDate(cursor.getString(transactionDateIndex));
            String transactionDestination = cursor.getString(transactionDestinationIndex);
            String transactionSource = cursor.getString(transactionSourceIndex);
            boolean destinationCanShowChange = dbIntegerToBoolean(cursor.getInt(destinationCanShowChangeIndex));
            boolean sourceCanShowChange = dbIntegerToBoolean(cursor.getInt(sourceCanShowChangeIndex));

            TransactionHistoryItem transactionHistoryItem = new TransactionHistoryItem(transactionDate, transactionAmount, sourceActionString,
                    transactionSource, sourceOriginal, sourceUpdated, destinationActionString, transactionDestination, destinationOriginal,
                    destinationUpdated, sourceCanShowChange, destinationCanShowChange);
            transactionHistoryItemList.add(transactionHistoryItem);
        }

        Comparator<TransactionHistoryItem> comparator = new Comparator<TransactionHistoryItem>() {
            @Override
            public int compare(TransactionHistoryItem lhs, TransactionHistoryItem rhs) {
                Date lhsDate = lhs.getTransactionDate();
                Date rhsDate = rhs.getTransactionDate();

                return Prediction.numDaysBetween(lhsDate, rhsDate);
            }
        };
        Collections.sort(transactionHistoryItemList, comparator);
    }

    /**
     * This method checks the passed today date against the last update date in the database. If
     * the last update was in the past then it runs an update up to and including today (for in memory
     * and in the database). If it is is the future it sets the last update date back to today. If
     * the db does not have an entry for the last update it sets it to today as it assumes it is
     * being initialized for the first time. Updates also include appending to the tracker history
     * any auto resets that may have occurred.
     *
     * @param db - a writable db connection to our bad budget db
     * @param bbd - the bad budget data containing all our bad budget objects
     * @param today - today's date and the date that this method is checking to see if the bbd db data is up to date with
     *
     */
    private static void checkLastUpdate(SQLiteDatabase db, BadBudgetData bbd, Date today,
                                        List<TrackerHistoryItem> trackerHistoryItems, List<TransactionHistoryItem> transactionHistoryItems,
                                        int budgetId, boolean autoUpdate)
    {
        String[] projection = {
                BBMetaData.COLUMN_LAST_UPDATE
        };

        String sortOrder =
                BBMetaData.COLUMN_LAST_UPDATE;

        Cursor cursor = db.query(
                BBMetaData.TABLE_NAME + "_" + budgetId,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );
        int lastUpdateIndex = cursor.getColumnIndexOrThrow(BBMetaData.COLUMN_LAST_UPDATE);

        //Last Update always ends up as today's date
        ContentValues metaValues = new ContentValues();
        metaValues.put(BBDatabaseContract.BBMetaData.COLUMN_LAST_UPDATE, BBDatabaseContract.dbDateToString(today));

        if (cursor.moveToNext())
        {
            Date lastUpdate = dbStringToDate(cursor.getString(lastUpdateIndex));

            if (lastUpdate != null)
            {
                //If the last update occurred in the past we need to update to today
                if (Prediction.numDaysBetween(lastUpdate, today) > 0)
                {
                    //Update in memory
                    if (autoUpdate)
                    {
                        Prediction.update(bbd, lastUpdate, today);
                    }
                    else
                    {
                        Prediction.updateNextDatesOnly(bbd, lastUpdate, today);
                    }

                    List<TrackerHistoryItem> newTrackerHistoryItems = updateTrackerHistoryItemsMemory(bbd, trackerHistoryItems, lastUpdate, today);
                    List<TransactionHistoryItem> newTransactionHistoryItems = updateGeneralHistoryItemsMemory(bbd, transactionHistoryItems, lastUpdate, today);

                    //Need to update DB too, both the updated bad budget objects, and any new history items
                    updateFullDatabase(db, bbd, newTrackerHistoryItems, newTransactionHistoryItems, budgetId);
                }
                //Update to today unconditionally
                db.update(BBMetaData.TABLE_NAME + "_" + budgetId, metaValues, null, null);
            }
        }
        else
        {
            //Last update not set yet. (i.e. first time setup) Initialize it to today.
            // TODO may want to also move budget prefs defaults here 10/6
            db.insert(BBMetaData.TABLE_NAME + "_" + budgetId, null, metaValues);
        }
    }

    /**
     * Given our the current bad budget data and the last update and current today date, this method updates the in memory
     * trackerHistoryItems (passed) with any auto resets of budgets that may have occurred during the update period. Returns
     * the newly added items in a list ordered by the most recent date first. Checks if auto reset is set first and if not
     * returns an empty list and adds no items to the apps list of tracker hisotry items
     * @param bbd - the recently updated bad budget data object to check for any auto resets of budgets
     * @param appTrackerHistoryItems - the application wide trackerHistoryItems list that should be added to
     * @param lastUpdate - the beginning of the update period that occurred before calling this method
     * @param today - the end of the update period that occurred before calling this method
     * @return - a list of the newly created tracker history items in order from most recent to furthest in the past.
     */
    public static List<TrackerHistoryItem> updateTrackerHistoryItemsMemory(BadBudgetData bbd, List<TrackerHistoryItem> appTrackerHistoryItems, Date lastUpdate, Date today)
    {
        //Get a hold of any new budget transaction history items
        List<TrackerHistoryItem> newTrackerHistoryItems = new ArrayList<TrackerHistoryItem>();

        boolean autoReset = bbd.getBudget().isAutoReset();

        if (autoReset)
        {
            int dayIndexMax = Prediction.numDaysBetween(lastUpdate, today);
            for (int dayIndex = 0; dayIndex <= dayIndexMax; dayIndex++)
            {
                for (BudgetItem item : bbd.getBudget().getAllBudgetItems().values())
                {
                    PredictDataBudgetItem pdbi = item.getPredictData(dayIndex);
                    if (pdbi.getLossAmountToday() != -1)
                    {
                        SimpleDateFormat dayOfWeekFormat = new SimpleDateFormat(BadBudgetApplication.TRACKER_DAY_FORMAT);
                        Date dateOfAutoReset = Prediction.addDays(lastUpdate, dayIndex);
                        TrackerHistoryItem historyItem = new TrackerHistoryItem(item.getDescription(), "",
                                BadBudgetApplication.dateString(dateOfAutoReset),
                                dayOfWeekFormat.format(dateOfAutoReset),
                                BadBudgetApplication.AUTO_RESET_TRACKER_HISTORY_TIME,
                                TrackerHistoryItem.TrackerAction.autoReset,
                                pdbi.getLossAmountToday(), pdbi.getOriginalAmount(), pdbi.getUpdatedAmount());
                        newTrackerHistoryItems.add(historyItem);
                        appTrackerHistoryItems.add(0, historyItem);
                    }
                }
            }
        }

        return newTrackerHistoryItems;
    }

    /**
     * Method that takes the bbd object, the current in memory general history items, the last update, and today's date; and
     * adds to the general history items in memory (the most recent are added to the beginning of the list)
     * any newly created transactions that occurred during the prediction that
     * was called prior to calling this method that was used to update from lastUpdate to today. Returns a list of the newly
     * added items.
     * @param bbd the bad budget data
     * @param appGeneralHistoryItems the current in memory general history items
     * @param lastUpdate - the last update date that we are running an update from
     * @param today - today's date that we are running an update to
     * @return a list of the newly added history items in order from least recent to most recent
     */
    public static List<TransactionHistoryItem> updateGeneralHistoryItemsMemory(BadBudgetData bbd, List<TransactionHistoryItem> appGeneralHistoryItems, Date lastUpdate, Date today)
    {
        int numDays = Prediction.numDaysBetween(lastUpdate, today);
        List<TransactionHistoryItem> newTransactionHistoryItems = new ArrayList<>();

        for (int i = 0; i <= numDays; i++) {
            for (MoneyOwed currDebt : bbd.getDebts()) {
                List<TransactionHistoryItem> transactionHistoryItems = currDebt.getPredictData(i).transactionHistory();
                if (transactionHistoryItems != null) {
                    for (TransactionHistoryItem transactionHistoryItem : transactionHistoryItems) {
                        if (!newTransactionHistoryItems.contains(transactionHistoryItem)) {
                            newTransactionHistoryItems.add(transactionHistoryItem);
                            appGeneralHistoryItems.add(0, transactionHistoryItem);
                        }
                    }
                }
            }
            for (Account currAccount : bbd.getAccounts()) {
                List<TransactionHistoryItem> transactionHistoryItems = currAccount.getPredictData(i).transactionHistory();
                if (transactionHistoryItems != null) {
                    for (TransactionHistoryItem transactionHistoryItem : transactionHistoryItems) {
                        if (!newTransactionHistoryItems.contains(transactionHistoryItem)) {
                            newTransactionHistoryItems.add(transactionHistoryItem);
                            appGeneralHistoryItems.add(0, transactionHistoryItem);
                        }
                    }
                }
            }
        }

        return newTransactionHistoryItems;
    }

    /**
     * Using the passed bad budget data object this method fully updates every object in our database.
     * Also part of this update is adding of new trackerHistoryItems as trackers may reset on update and thus new
     * entries in the history must be made.
     * Note budget preferences are not updated as these should not be  influenced by processing updates and only
     * by user input.
     * @param db - the db connection to our bad budget database that we are updating
     * @param bbd - the bad budget data in memory that we wish to persist in our database
     * @param newTrackerHistoryItems - a list of new tracker history items that need to be added to our database
     */
    public static void updateFullDatabase(SQLiteDatabase db, BadBudgetData bbd, List<TrackerHistoryItem> newTrackerHistoryItems, List<TransactionHistoryItem> newTransactionHistoryItems, int budgetId)
    {
        for (Account currAccount : bbd.getAccounts()) {
            if (!(currAccount instanceof SavingsAccount)) {
                ContentValues values = new ContentValues();
                values.put(BBDatabaseContract.CashAccounts.COLUMN_NAME, currAccount.name());
                values.put(BBDatabaseContract.CashAccounts.COLUMN_VALUE, currAccount.value());

                if (currAccount.quickLook()) {
                    values.put(BBDatabaseContract.CashAccounts.COLUMN_QUICK_LOOK, BBDatabaseContract.TRUE_VALUE);
                } else {
                    values.put(BBDatabaseContract.CashAccounts.COLUMN_QUICK_LOOK, BBDatabaseContract.FALSE_VALUE);
                }

                String strFilter = BBDatabaseContract.CashAccounts.COLUMN_NAME + "=?";
                db.update(BBDatabaseContract.CashAccounts.TABLE_NAME + "_" + budgetId, values, strFilter, new String[]{currAccount.name()});
            }
            else {
                SavingsAccount currSavingsAccount = (SavingsAccount) currAccount;
                ContentValues values = new ContentValues();
                values.put(BBDatabaseContract.CashAccounts.COLUMN_NAME, currSavingsAccount.name());
                values.put(BBDatabaseContract.CashAccounts.COLUMN_VALUE, currSavingsAccount.value());

                if (currSavingsAccount.quickLook()) {
                    values.put(BBDatabaseContract.CashAccounts.COLUMN_QUICK_LOOK, BBDatabaseContract.TRUE_VALUE);
                } else {
                    values.put(BBDatabaseContract.CashAccounts.COLUMN_QUICK_LOOK, BBDatabaseContract.FALSE_VALUE);
                }

                values.put(BBDatabaseContract.CashAccounts.COLUMN_SAVINGS, BBDatabaseContract.TRUE_VALUE);
                values.put(BBDatabaseContract.CashAccounts.COLUMN_GOAL, currSavingsAccount.goal());
                values.put(BBDatabaseContract.CashAccounts.COLUMN_CONTRIBUTION_AMOUNT, currSavingsAccount.contribution().getContribution());
                values.put(BBDatabaseContract.CashAccounts.COLUMN_CONTRIBUTION_FREQUENCY, BBDatabaseContract.dbFrequencyToInteger(currSavingsAccount.contribution().getFrequency()));
                values.put(BBDatabaseContract.CashAccounts.COLUMN_NEXT_CONTRIBUTION, BBDatabaseContract.dbDateToString(currSavingsAccount.nextContribution()));
                values.put(BBDatabaseContract.CashAccounts.COLUMN_GOAL_DATE, BBDatabaseContract.dbDateToString(currSavingsAccount.goalDate()));
                values.put(BBDatabaseContract.CashAccounts.COLUMN_END_DATE, BBDatabaseContract.dbDateToString(currSavingsAccount.endDate()));
                values.put(BBDatabaseContract.CashAccounts.COLUMN_SOURCE_NAME, currSavingsAccount.sourceAccount().name());

                String strFilter = BBDatabaseContract.CashAccounts.COLUMN_NAME + "=?";
                db.update(BBDatabaseContract.CashAccounts.TABLE_NAME + "_" + budgetId, values, strFilter, new String[]{currSavingsAccount.name()});
            }
        }

        for (MoneyOwed currDebt : bbd.getDebts())
        {
            DebtType debtType = DebtType.Other;
            if (currDebt instanceof CreditCard)
            {
                debtType = DebtType.CreditCard;
            }
            else if (currDebt instanceof Loan)
            {
                debtType = DebtType.Loan;
            }

            ContentValues values = new ContentValues();
            values.put(BBDatabaseContract.Debts.COLUMN_NAME, currDebt.name());
            values.put(BBDatabaseContract.Debts.COLUMN_AMOUNT, currDebt.amount());
            values.put(BBDatabaseContract.Debts.COLUMN_QUICK_LOOK, BBDatabaseContract.dbBooleanToInteger(currDebt.quicklook()));
            values.put(BBDatabaseContract.Debts.COLUMN_DEBT_TYPE, BBDatabaseContract.dbDebtTypeToInteger(debtType));

            if (currDebt.payment() != null) {
                values.put(BBDatabaseContract.Debts.COLUMN_PAYMENT_AMOUNT, currDebt.payment().amount());
                values.put(BBDatabaseContract.Debts.COLUMN_PAYMENT_SOURCE, currDebt.payment().sourceAccount().name());
                values.put(BBDatabaseContract.Debts.COLUMN_PAYMENT_FREQUENCY, BBDatabaseContract.dbFrequencyToInteger(currDebt.payment().frequency()));
                values.put(BBDatabaseContract.Debts.COLUMN_PAYMENT_NEXT_PAYMENT, BBDatabaseContract.dbDateToString(currDebt.payment().nextPaymentDate()));
                values.put(BBDatabaseContract.Debts.COLUMN_PAYMENT_END_DATE, BBDatabaseContract.dbDateToString(currDebt.payment().endDate()));
                values.put(BBDatabaseContract.Debts.COLUMN_PAYMENT_GOAL_DATE, BBDatabaseContract.dbDateToString(currDebt.payment().goalDate()));
                values.put(BBDatabaseContract.Debts.COLUMN_PAYMENT_ONGOING, BBDatabaseContract.dbBooleanToInteger(currDebt.payment().ongoing()));
                values.put(BBDatabaseContract.Debts.COLUMN_PAYMENT_PAYOFF, BBDatabaseContract.dbBooleanToInteger(currDebt.payment().payOff()));
            }

            String strFilter = BBDatabaseContract.Debts.COLUMN_NAME + "=?";
            db.update(BBDatabaseContract.Debts.TABLE_NAME + "_" + budgetId, values, strFilter, new String[] {currDebt.name()});
        }

        for (MoneyGain currGain : bbd.getGains())
        {
            ContentValues values = new ContentValues();
            values.put(BBDatabaseContract.Gains.COLUMN_DESCRIPTION, currGain.sourceDescription());
            values.put(BBDatabaseContract.Gains.COLUMN_AMOUNT, currGain.gainAmount());
            values.put(BBDatabaseContract.Gains.COLUMN_FREQUENCY, BBDatabaseContract.dbFrequencyToInteger(currGain.gainFrequency()));
            values.put(BBDatabaseContract.Gains.COLUMN_NEXT_GAIN, BBDatabaseContract.dbDateToString(currGain.nextDeposit()));
            values.put(BBDatabaseContract.Gains.COLUMN_END_DATE, BBDatabaseContract.dbDateToString(currGain.endDate()));
            values.put(BBDatabaseContract.Gains.COLUMN_DESTINATION, currGain.destinationAccount().name());

            String strFilter = BBDatabaseContract.Gains.COLUMN_DESCRIPTION + "=?";
            db.update(BBDatabaseContract.Gains.TABLE_NAME + "_" + budgetId, values, strFilter, new String[] {currGain.sourceDescription()});
        }

        for (MoneyLoss currLoss : bbd.getLosses())
        {
            ContentValues values = new ContentValues();
            values.put(BBDatabaseContract.Losses.COLUMN_EXPENSE, currLoss.expenseDescription());
            values.put(BBDatabaseContract.Losses.COLUMN_AMOUNT, currLoss.lossAmount());
            values.put(BBDatabaseContract.Losses.COLUMN_FREQUENCY, BBDatabaseContract.dbFrequencyToInteger(currLoss.lossFrequency()));
            values.put(BBDatabaseContract.Losses.COLUMN_NEXT_LOSS, BBDatabaseContract.dbDateToString(currLoss.nextLoss()));
            values.put(BBDatabaseContract.Losses.COLUMN_END_DATE, BBDatabaseContract.dbDateToString(currLoss.endDate()));

            //TODO - 1/13/2017 - Add back name() method for sources
            Source source = currLoss.source();
            if (source instanceof Account)
            {
                Account sourceAccount = (Account)source;
                values.put(BBDatabaseContract.Losses.COLUMN_SOURCE, sourceAccount.name());
            }
            else
            {
                CreditCard sourceCard = (CreditCard)source;
                values.put(BBDatabaseContract.Losses.COLUMN_SOURCE, sourceCard.name());
            }

            String strFilter = BBDatabaseContract.Losses.COLUMN_EXPENSE + "=?";
            db.update(BBDatabaseContract.Losses.TABLE_NAME + "_" + budgetId, values, strFilter, new String[] {currLoss.expenseDescription()});
        }

        for (BudgetItem currItem : bbd.getBudget().getAllBudgetItems().values())
        {
            ContentValues values = new ContentValues();
            values.put(BBDatabaseContract.BudgetItems.COLUMN_DESCRIPTION, currItem.expenseDescription());
            values.put(BBDatabaseContract.BudgetItems.COLUMN_AMOUNT, currItem.lossAmount());
            values.put(BBDatabaseContract.BudgetItems.COLUMN_FREQUENCY, BBDatabaseContract.dbFrequencyToInteger(currItem.lossFrequency()));
            values.put(BBDatabaseContract.BudgetItems.COLUMN_NEXT_LOSS, BBDatabaseContract.dbDateToString(currItem.nextLoss()));
            values.put(BBDatabaseContract.BudgetItems.COLUMN_END_DATE, BBDatabaseContract.dbDateToString(currItem.endDate()));
            values.put(BBDatabaseContract.BudgetItems.COLUMN_PRORATED_START, currItem.isProratedStart());
            values.put(BudgetItems.COLUMN_MINUS_AMOUNT, currItem.getMinusAmount());
            values.put(BudgetItems.COLUMN_PLUS_AMOUNT, currItem.getPlusAmount());
            values.put(BudgetItems.COLUMN_REMAINING_AMOUNT, currItem.getCurrAmount());

            String strFilter = BBDatabaseContract.BudgetItems.COLUMN_DESCRIPTION + "=?";
            db.update(BBDatabaseContract.BudgetItems.TABLE_NAME + "_" + budgetId, values, strFilter, new String[] {currItem.expenseDescription()});
        }

        /* add any new tracker history items to persistent storage */
        for (TrackerHistoryItem trackerHistoryItem : newTrackerHistoryItems)
        {
            ContentValues trackerHistoryItemValues = new ContentValues();
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_BUDGET_ITEM_DESCRIPTION, trackerHistoryItem.getBudgetItemDescription());
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_USER_TRANSACTION_DESCRIPTION, trackerHistoryItem.getUserTransactionDescription());
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_DATE, trackerHistoryItem.getDateString());
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_DAY, trackerHistoryItem.getDayString());
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_TIME, trackerHistoryItem.getTimeString());
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_ACTION, dbActionToInteger(trackerHistoryItem.getAction()));
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_ACTION_AMOUNT, trackerHistoryItem.getActionAmount());
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_ORIGINAL_BUDGET_AMOUNT, trackerHistoryItem.getOriginalBudgetAmount());
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_UPDATED_BUDGET_AMOUNT, trackerHistoryItem.getUpdatedBudgetAmount());

            db.insert(BBDatabaseContract.TrackerHistoryItems.TABLE_NAME + "_" + budgetId, null, trackerHistoryItemValues);
        }

        /* add any new general history items to persistent storage */
        for (TransactionHistoryItem transactionHistoryItem : newTransactionHistoryItems)
        {
            ContentValues transactionHistoryItemValues = new ContentValues();
            transactionHistoryItemValues.put(GeneralHistoryItems.COLUMN_DESTINATION_ACTION, transactionHistoryItem.getDestinationActionString());
            transactionHistoryItemValues.put(GeneralHistoryItems.COLUMN_DESTINATION_ORIGINAL, transactionHistoryItem.getDestinationOriginal());
            transactionHistoryItemValues.put(GeneralHistoryItems.COLUMN_DESTINATION_UPDATED, transactionHistoryItem.getDestinationUpdated());
            transactionHistoryItemValues.put(GeneralHistoryItems.COLUMN_SOURCE_ACTION, transactionHistoryItem.getSourceActionString());
            transactionHistoryItemValues.put(GeneralHistoryItems.COLUMN_SOURCE_ORIGINAL, transactionHistoryItem.getSourceOriginal());
            transactionHistoryItemValues.put(GeneralHistoryItems.COLUMN_SOURCE_UPDATED, transactionHistoryItem.getSourceUpdated());
            transactionHistoryItemValues.put(GeneralHistoryItems.COLUMN_TRANSACTION_AMOUNT, transactionHistoryItem.getTransactionAmount());
            transactionHistoryItemValues.put(GeneralHistoryItems.COLUMN_TRANSACTION_DATE, dbDateToString(transactionHistoryItem.getTransactionDate()));
            transactionHistoryItemValues.put(GeneralHistoryItems.COLUMN_TRANSACTION_DESTINATION, transactionHistoryItem.getTransactionDestination());
            transactionHistoryItemValues.put(GeneralHistoryItems.COLUMN_TRANSACTION_SOURCE, transactionHistoryItem.getTransactionSource());
            transactionHistoryItemValues.put(GeneralHistoryItems.COLUMN_DESTINATION_CAN_SHOW_CHANGE, dbBooleanToInteger(transactionHistoryItem.isDestinationCanShowChange()));
            transactionHistoryItemValues.put(GeneralHistoryItems.COLUMN_SOURCE_CAN_SHOW_CHANGE, dbBooleanToInteger(transactionHistoryItem.isSourceCanShowChange()));

            db.insert(GeneralHistoryItems.TABLE_NAME + "_" + budgetId, null, transactionHistoryItemValues);
        }

    }

    /**
     * Private helper method to extract the budget data from the database. Includes extraction
     * of both the budget preferences and the budget items
     * @param db - the database connection to extract from
     * @param bbd - the bad budget data object to place the extracted prefs and items into
     * @param remainAmountAction - the budget's remain amount action that all budget items will use
     */
    private static void extractBudget(SQLiteDatabase db, BadBudgetData bbd, int budgetId, RemainAmountAction remainAmountAction)
    {
        extractBudgetPrefs(db, bbd, budgetId);
        extractBudgetItems(db, bbd, budgetId, remainAmountAction);
    }

    /**
     * Private helper to extract all budget items from the bad budget database and place them into the in memory
     * bad budget data object.
     * @param db - the database to extract the items from
     * @param bbd - the in memory bad budget data object to place the extracted items in
     * @param remainAmountAction - the budget's remain amount action that all budget items will use
     */
    private static void extractBudgetItems(SQLiteDatabase db, BadBudgetData bbd, int budgetId, RemainAmountAction remainAmountAction)
    {
        String[] projection = {
                BudgetItems.COLUMN_DESCRIPTION,
                BudgetItems.COLUMN_AMOUNT,
                BudgetItems.COLUMN_FREQUENCY,
                BudgetItems.COLUMN_NEXT_LOSS,
                BudgetItems.COLUMN_END_DATE,
                BudgetItems.COLUMN_PRORATED_START,
                BudgetItems.COLUMN_MINUS_AMOUNT,
                BudgetItems.COLUMN_PLUS_AMOUNT,
                BudgetItems.COLUMN_REMAINING_AMOUNT,
        };

        String sortOrder =
                BudgetItems.COLUMN_DESCRIPTION;

        //Going to select all the budget items with our query, sorted
        //alphabetically by description. The where clause (including its args), group by, and having
        //statements are null.
        Cursor cursor = db.query(
                BudgetItems.TABLE_NAME + "_" + budgetId,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );
        int descriptionIndex = cursor.getColumnIndexOrThrow(BudgetItems.COLUMN_DESCRIPTION);
        int amountIndex = cursor.getColumnIndexOrThrow(BudgetItems.COLUMN_AMOUNT);
        int frequencyIndex = cursor.getColumnIndexOrThrow(BudgetItems.COLUMN_FREQUENCY);
        int nextLossIndex = cursor.getColumnIndexOrThrow(BudgetItems.COLUMN_NEXT_LOSS);
        int endDateIndex = cursor.getColumnIndexOrThrow(BudgetItems.COLUMN_END_DATE);
        int proratedStartIndex = cursor.getColumnIndexOrThrow(BudgetItems.COLUMN_PRORATED_START);
        int minusAmountIndex = cursor.getColumnIndexOrThrow(BudgetItems.COLUMN_MINUS_AMOUNT);
        int plusAmountIndex = cursor.getColumnIndexOrThrow(BudgetItems.COLUMN_PLUS_AMOUNT);
        int remainingAmountIndex = cursor.getColumnIndexOrThrow(BudgetItems.COLUMN_REMAINING_AMOUNT);

        while (cursor.moveToNext())
        {
            String description = cursor.getString(descriptionIndex);
            double amount = cursor.getDouble(amountIndex);
            Frequency frequency = dbIntegerToFrequency(cursor.getInt(frequencyIndex));
            Date next = dbStringToDate(cursor.getString(nextLossIndex));
            Date end = dbStringToDate(cursor.getString(endDateIndex));
            boolean proratedStart = dbIntegerToBoolean(cursor.getInt(proratedStartIndex));

            double minusAmount = cursor.getDouble(minusAmountIndex);
            double plusAmount = cursor.getDouble(plusAmountIndex);
            double remainingAmount = cursor.getDouble(remainingAmountIndex);

            try {
                BudgetItem item = new BudgetItem(description, amount, frequency, next, end, proratedStart, bbd.getBudget().getBudgetSource());
                item.setRemainAmountAction(remainAmountAction);
                item.setMinusAmount(minusAmount);
                item.setPlusAmount(plusAmount);
                item.setCurrAmount(remainingAmount);
                bbd.getBudget().addBudgetItem(item);
            }
            catch(BadBudgetInvalidValueException e)
            {
                //TODO - how to handle?
                e.printStackTrace();
            }
        }
    }

    /**
     * Private helper to extract budget prefs from the bad budget database (minus auto update and
     * remain amount action) and place them into the in memory bad budget data object.
     * @param db - the database to extract the prefs from
     * @param bbd - the in memory bad budget data object to place the extracted prefs into
     */
    private static void extractBudgetPrefs(SQLiteDatabase db, BadBudgetData bbd, int budgetId)
    {
        String[] projection = {
                BudgetPreferences.COLUMN_BUDGET_SOURCE,
                BudgetPreferences.COLUMN_AUTO_RESET,
                BudgetPreferences.COLUMN_WEEKLY_RESET,
                BudgetPreferences.COLUMN_MONTHLY_RESET
        };

        // Going to select the budget prefs with our query, unsorted (Last arg null).
        // The where clause (including its args), group by, and having
        //statements are null.
        Cursor cursor = db.query(
                BudgetPreferences.TABLE_NAME + "_" + budgetId,
                projection,
                null,
                null,
                null,
                null,
                null
        );

        int budgetSourceIndex = cursor.getColumnIndexOrThrow(BudgetPreferences.COLUMN_BUDGET_SOURCE);
        int autoResetIndex = cursor.getColumnIndexOrThrow(BudgetPreferences.COLUMN_AUTO_RESET);
        int weeklyResetIndex = cursor.getColumnIndexOrThrow(BudgetPreferences.COLUMN_WEEKLY_RESET);
        int monthlyResetIndex = cursor.getColumnIndexOrThrow(BudgetPreferences.COLUMN_MONTHLY_RESET);

        boolean noPrefsSet = !cursor.moveToNext();

        if (noPrefsSet)
        {
            //TODO - this is an error
        }

        String budgetSourceName = cursor.getString(budgetSourceIndex);

        boolean autoReset = dbIntegerToBoolean(cursor.getInt(autoResetIndex));
        int weeklyReset = cursor.getInt(weeklyResetIndex);
        int monthlyReset = cursor.getInt(monthlyResetIndex);

        Source source = bbd.getSourceWithNameExcludeSavingAccounts(budgetSourceName);
        try
        {

            //TODO - add autoUpdate, change hack for source allowing null 9/30
            Budget budget = null;
            if (source == null)
            {
                Account tempSource = new Account("tempSource", 0, false);
                budget = new Budget(tempSource, autoReset, weeklyReset, monthlyReset);
                budget.setBudgetSource(null);
            }
            else
            {
                budget = new Budget(source, autoReset, weeklyReset, monthlyReset);
            }
            bbd.setBudget(budget);
        }
        catch (BadBudgetInvalidValueException e)
        {
            //TODO - handle?
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Private helper to extract all the gains from our bad budget database and add them to the
     * passed bbd object.
     * @param db - the database to extract the gains from
     * @param bbd - the memory bbd object to place the gains in
     */
    private static void extractGainsFromDataBase(SQLiteDatabase db, BadBudgetData bbd, int budgetId)
    {
        String[] projection = {
                Gains.COLUMN_DESCRIPTION,
                Gains.COLUMN_AMOUNT,
                Gains.COLUMN_FREQUENCY,
                Gains.COLUMN_NEXT_GAIN,
                Gains.COLUMN_END_DATE,
                Gains.COLUMN_DESTINATION
        };

        String sortOrder =
                Gains.COLUMN_DESCRIPTION;

        //Going to select all the gains with our query, sorted
        //alphabetically by description. The where clause (including its args), group by, and having
        //statements are null.
        Cursor cursor = db.query(
                Gains.TABLE_NAME + "_" + budgetId,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );

        int descriptionColumnIndex = cursor.getColumnIndexOrThrow(Gains.COLUMN_DESCRIPTION);
        int amountColumnIndex = cursor.getColumnIndexOrThrow(Gains.COLUMN_AMOUNT);
        int frequencyColumnIndex = cursor.getColumnIndexOrThrow(Gains.COLUMN_FREQUENCY);
        int nextGainColumnIndex = cursor.getColumnIndexOrThrow(Gains.COLUMN_NEXT_GAIN);
        int endDateColumnIndex = cursor.getColumnIndexOrThrow(Gains.COLUMN_END_DATE);
        int destinationColumnIndex = cursor.getColumnIndexOrThrow(Gains.COLUMN_DESTINATION);

        while (cursor.moveToNext())
        {
            String description = cursor.getString(descriptionColumnIndex);
            double amount = cursor.getDouble(amountColumnIndex);
            Frequency frequency = dbIntegerToFrequency(cursor.getInt(frequencyColumnIndex));
            Date next = dbStringToDate(cursor.getString(nextGainColumnIndex));
            Date end = dbStringToDate(cursor.getString(endDateColumnIndex));
            Account destination = bbd.getAccountWithName(cursor.getString(destinationColumnIndex));

            try
            {
                MoneyGain gain = new MoneyGain(description, amount, frequency, next, end, destination);
                bbd.addGain(gain);
            }
            catch (BadBudgetInvalidValueException e)
            {
                //TODO - handle?
                e.printStackTrace();
            }
        }
    }

    /**
     * Private helper to extract all losses from the bad budget database and place them into the in memory
     * bad budget data object.
     * @param db - the database to extract the losses from
     * @param bbd - the in memory bad budget data object to place the extracted losses in
     */
    private static void extractLossesFromDataBase(SQLiteDatabase db, BadBudgetData bbd, int budgetId)
    {
        String[] projection = {
                Losses.COLUMN_EXPENSE,
                Losses.COLUMN_AMOUNT,
                Losses.COLUMN_FREQUENCY,
                Losses.COLUMN_NEXT_LOSS,
                Losses.COLUMN_END_DATE,
                Losses.COLUMN_SOURCE
        };

        String sortOrder =
                Losses.COLUMN_SOURCE;

        //Going to select all the losses with our query, sorted
        //alphabetically by expense description. The where clause (including its args), group by, and having
        //statements are null.
        Cursor cursor = db.query(
                Losses.TABLE_NAME + "_" + budgetId,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );

        int expenseColumnIndex = cursor.getColumnIndexOrThrow(Losses.COLUMN_EXPENSE);
        int amountColumnIndex = cursor.getColumnIndexOrThrow(Losses.COLUMN_AMOUNT);
        int frequencyColumnIndex = cursor.getColumnIndexOrThrow(Losses.COLUMN_FREQUENCY);
        int nextLossColumnIndex = cursor.getColumnIndexOrThrow(Losses.COLUMN_NEXT_LOSS);
        int endDateColumnIndex = cursor.getColumnIndexOrThrow(Losses.COLUMN_END_DATE);
        int sourceColumnIndex = cursor.getColumnIndexOrThrow(Losses.COLUMN_SOURCE);

        while (cursor.moveToNext())
        {
            String expense = cursor.getString(expenseColumnIndex);
            double amount = cursor.getDouble(amountColumnIndex);
            Frequency frequency = dbIntegerToFrequency(cursor.getInt(frequencyColumnIndex));
            Date next = dbStringToDate(cursor.getString(nextLossColumnIndex));
            Date end = dbStringToDate(cursor.getString(endDateColumnIndex));
            Source source = bbd.getSourceWithNameExcludeSavingAccounts(cursor.getString(sourceColumnIndex));

            try {
                MoneyLoss loss = new MoneyLoss(expense, amount, frequency, next, end, source);
                bbd.addLoss(loss);
            }
            catch (BadBudgetInvalidValueException e)
            {
                //TODO - decide how/if to handle
                e.printStackTrace();
            }
        }
    }

    /**
     * Private helper method for use with populating the BadBudget User data into a BadBudgetData object
     * Extracts all the account objects from the passed db and creates BadBudgetLogic Account classes
     * and adds all of them with their full data in the correct form to the passed bbd object.
     *
     * @param db - the database to extract the accounts from
     * @param bbd - the BadBudgetData object to add the extracted accounts to
     */
    private static void extractAccountsFromDataBase(SQLiteDatabase db, BadBudgetData bbd, int budgetId)
    {
        String[] projection = {
                BBDatabaseContract.CashAccounts.COLUMN_NAME,
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
                CashAccounts.COLUMN_INTEREST_RATE
        };

        String sortOrder =
                BBDatabaseContract.CashAccounts.COLUMN_NAME;

        //Going to select all the accounts with our query, sorted
        //alphabetically by name. The where clause (including its args), group by, and having
        //statements are null.
        Cursor cursor = db.query(
                BBDatabaseContract.CashAccounts.TABLE_NAME + "_" + budgetId,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );

        int nameColumnIndex = cursor.getColumnIndexOrThrow(BBDatabaseContract.CashAccounts.COLUMN_NAME);
        int valueColumnIndex = cursor.getColumnIndexOrThrow(BBDatabaseContract.CashAccounts.COLUMN_VALUE);
        int quickLookColumnIndex = cursor.getColumnIndexOrThrow(BBDatabaseContract.CashAccounts.COLUMN_QUICK_LOOK);
        int savingsColumnIndex = cursor.getColumnIndexOrThrow(BBDatabaseContract.CashAccounts.COLUMN_SAVINGS);
        int goalColumnIndex = cursor.getColumnIndexOrThrow(BBDatabaseContract.CashAccounts.COLUMN_GOAL);
        int contributionAmountColumnIndex = cursor.getColumnIndexOrThrow(BBDatabaseContract.CashAccounts.COLUMN_CONTRIBUTION_AMOUNT);
        int contributionFrequencyColumnIndex = cursor.getColumnIndexOrThrow(BBDatabaseContract.CashAccounts.COLUMN_CONTRIBUTION_FREQUENCY);
        int nextContributionColumnIndex = cursor.getColumnIndexOrThrow(BBDatabaseContract.CashAccounts.COLUMN_NEXT_CONTRIBUTION);
        int endDateColumnIndex = cursor.getColumnIndexOrThrow(BBDatabaseContract.CashAccounts.COLUMN_END_DATE);
        int goalDateColumnIndex = cursor.getColumnIndexOrThrow(BBDatabaseContract.CashAccounts.COLUMN_GOAL_DATE);
        int sourceNameColumnIndex = cursor.getColumnIndexOrThrow(BBDatabaseContract.CashAccounts.COLUMN_SOURCE_NAME);
        int interestRateColumnIndex = cursor.getColumnIndexOrThrow(CashAccounts.COLUMN_INTEREST_RATE);

        //Pass 1: Create all the account and savings account objects without the source account set.
        while (cursor.moveToNext())
        {
            String name = cursor.getString(nameColumnIndex);
            double value = cursor.getDouble(valueColumnIndex);
            boolean quickLook = dbIntegerToBoolean(cursor.getInt(quickLookColumnIndex));
            boolean savings = dbIntegerToBoolean(cursor.getInt(savingsColumnIndex));

            if (savings)
            {
                double goal = cursor.getDouble(goalColumnIndex);

                double contributionAmount = cursor.getDouble(contributionAmountColumnIndex);
                Frequency contributionFrequency = dbIntegerToFrequency(cursor.getInt(contributionFrequencyColumnIndex));

                double interestRate = cursor.getDouble(interestRateColumnIndex);

                try
                {
                    Contribution contribution = new Contribution(contributionAmount, contributionFrequency);

                    Date nextDate = dbStringToDate(cursor.getString(nextContributionColumnIndex));
                    Date endDate = dbStringToDate(cursor.getString(endDateColumnIndex));
                    Date goalDate = dbStringToDate(cursor.getString(goalDateColumnIndex));

                    //TODO - Better way to handle this. Can't have null source in logic... 9/29
                    Account emptyTempSource = new Account("TempAccountSource", 0, false);
                    SavingsAccount savingsAccount = new SavingsAccount(name, value, quickLook, goalDate != null, goal, goalDate, contribution, emptyTempSource, nextDate, endDate, endDate == null, interestRate);
                    bbd.addAccount(savingsAccount);
                }
                catch (BadBudgetInvalidValueException e)
                {
                    //TODO - Decide how to handle
                    e.printStackTrace();
                }
            }
            else
            {
                try
                {
                    Account account = new Account(name, value, quickLook);
                    bbd.addAccount(account);
                }
                catch (BadBudgetInvalidValueException e)
                {
                    //TODO - Handle?
                    e.printStackTrace();
                }
            }
        }

        //Pass 2 - Go back over all the rows and for any savings account we need to set it's source
        cursor.moveToPosition(-1);
        while (cursor.moveToNext())
        {
            boolean savings = dbIntegerToBoolean(cursor.getInt(savingsColumnIndex));
            if (savings)
            {
                String name = cursor.getString(nameColumnIndex);
                SavingsAccount savingsAccount = (SavingsAccount) bbd.getAccountWithName(name);
                String sourceName = cursor.getString(sourceNameColumnIndex);
                Account sourceAccount = bbd.getAccountWithName(sourceName);
                savingsAccount.updateSourceAccount(sourceAccount);
            }
        }
    }

    /**
     * Private helper method to extract any money loss (debt) objects from the bad budget database,
     * and populate the badbudget data with any extracted debts (includes credit cards, loans, and
     * misc. debts.
     * @param db - the database to extract from
     * @param bbd - the bad budget data to add the debt objects to and also to read any accounts for debt
     *              payment sources
     */
    private static void extractDebtsFromDataBase(SQLiteDatabase db, BadBudgetData bbd, int budgetId)
    {
        String[] projection = {
                Debts.COLUMN_NAME,
                Debts.COLUMN_AMOUNT,
                Debts.COLUMN_QUICK_LOOK,
                Debts.COLUMN_DEBT_TYPE,
                Debts.COLUMN_PAYMENT_AMOUNT,
                Debts.COLUMN_PAYMENT_SOURCE,
                Debts.COLUMN_PAYMENT_FREQUENCY,
                Debts.COLUMN_PAYMENT_NEXT_PAYMENT,
                Debts.COLUMN_PAYMENT_END_DATE,
                Debts.COLUMN_PAYMENT_GOAL_DATE,
                Debts.COLUMN_PAYMENT_ONGOING,
                Debts.COLUMN_PAYMENT_PAYOFF,
                Debts.COLUMN_INTEREST_RATE,
                Debts.COLUMN_SIMPLE_INTEREST,
                Debts.COLUMN_PRINCIPLE
        };

        String sortOrder =
                Debts.COLUMN_NAME;

        //Going to select all the debts with our query, sorted
        //alphabetically by name. The where clause (including its args), group by, and having
        //statements are null.
        Cursor cursor = db.query(
                Debts.TABLE_NAME + "_" + budgetId,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );

        int nameColumnIndex = cursor.getColumnIndexOrThrow(Debts.COLUMN_NAME);
        int debtColumnIndex = cursor.getColumnIndexOrThrow(Debts.COLUMN_AMOUNT);
        int quickookColumnIndex = cursor.getColumnIndexOrThrow(Debts.COLUMN_QUICK_LOOK);
        int typeColumnIndex = cursor.getColumnIndexOrThrow(Debts.COLUMN_DEBT_TYPE);
        int pAmountColumnIndex = cursor.getColumnIndexOrThrow(Debts.COLUMN_PAYMENT_AMOUNT);
        int pSourceColumnIndex = cursor.getColumnIndexOrThrow(Debts.COLUMN_PAYMENT_SOURCE);
        int pFrequencyColumnIndex = cursor.getColumnIndexOrThrow(Debts.COLUMN_PAYMENT_FREQUENCY);
        int pNextColumnIndex = cursor.getColumnIndexOrThrow(Debts.COLUMN_PAYMENT_NEXT_PAYMENT);
        int pEndColumnIndex = cursor.getColumnIndexOrThrow(Debts.COLUMN_PAYMENT_END_DATE);
        int pGoalColumnIndex = cursor.getColumnIndexOrThrow(Debts.COLUMN_PAYMENT_GOAL_DATE);
        int pOngoingColumnIndex = cursor.getColumnIndexOrThrow(Debts.COLUMN_PAYMENT_ONGOING);
        int pPayoffColumnIndex = cursor.getColumnIndexOrThrow(Debts.COLUMN_PAYMENT_PAYOFF);

        int interestRateColumnIndex = cursor.getColumnIndexOrThrow(Debts.COLUMN_INTEREST_RATE);
        int simpleInterestColumnIndex = cursor.getColumnIndexOrThrow(Debts.COLUMN_SIMPLE_INTEREST);
        int principleColumnIndex = cursor.getColumnIndexOrThrow(Debts.COLUMN_PRINCIPLE);

        while (cursor.moveToNext())
        {
            String name = cursor.getString(nameColumnIndex);
            double value = cursor.getDouble(debtColumnIndex);
            boolean quicklook = dbIntegerToBoolean(cursor.getInt(quickookColumnIndex));
            DebtType debtType = dbIntegerToDebtType(cursor.getInt(typeColumnIndex));
            double interestRate = cursor.getDouble(interestRateColumnIndex);

            MoneyOwed debt = null;

            try
            {
                switch (debtType)
                {
                    case CreditCard:
                    {
                        debt = new CreditCard(name, value, quicklook, interestRate);
                        break;
                    }
                    case Loan:
                    {
                        boolean simpleInterest = dbIntegerToBoolean(cursor.getInt(simpleInterestColumnIndex));
                        double principle = cursor.getDouble(principleColumnIndex);
                        debt = new Loan(name, value, quicklook, interestRate, simpleInterest, principle);
                        break;
                    }
                    case Other:
                    {
                        debt = new MoneyOwed(name, value, quicklook, interestRate);
                        break;
                    }
                }

                //TODO - May want a better way of checking if a debt has a payment attached 9/29
                boolean hasPayment = cursor.getString(pSourceColumnIndex) != null;
                Payment payment = null;

                if (hasPayment)
                {
                    //have a payment - get the rest of its fields
                    double pAmount = cursor.getDouble(pAmountColumnIndex);
                    Account pSource = bbd.getAccountWithName(cursor.getString(pSourceColumnIndex));
                    Frequency pFreq = dbIntegerToFrequency(cursor.getInt(pFrequencyColumnIndex));
                    Date pNext = dbStringToDate(cursor.getString(pNextColumnIndex));
                    Date pEnd = dbStringToDate(cursor.getString(pEndColumnIndex));
                    Date pGoal = dbStringToDate(cursor.getString(pGoalColumnIndex));
                    boolean ongoing = dbIntegerToBoolean(cursor.getInt(pOngoingColumnIndex));
                    boolean payoff = dbIntegerToBoolean(cursor.getInt(pPayoffColumnIndex));
                    payment = new Payment(pAmount, payoff, pFreq, pSource, pNext, ongoing, pEnd, debt, pGoal);
                    debt.setupPayment(payment);
                }
                bbd.addDebt(debt);
            }
            catch(BadBudgetInvalidValueException e)
            {
                //TODO - Handle? Debt or Payment failed to be created
                e.printStackTrace();
            }
        }
    }

    /**
     * Private helper method. Takes an integer extracted from a bad budget database and returns
     * its boolean representation.
     *
     * @param dbInt - the integer representing a boolean value in our database
     * @return - true if the integer represents true in our database, false otherwise.
     *
     */
    private static boolean dbIntegerToBoolean(int dbInt)
    {
        if (dbInt == BBDatabaseContract.TRUE_VALUE)
        {
            return true;
        }
        return false;
    }

    /**
     * Passed a boolean value this method returns the integer representation of that boolean. The
     * representation that the BadBudget Database uses. Use dbIntegerToBoolean to convert back to
     * a boolean. //TODO - update add savings activity to use this. and make values private
     * @param bool - the bool to convert
     * @return - the integer representation of this boolean used in the bad budget datebase.
     */
    public static int dbBooleanToInteger(boolean bool)
    {
        if (bool)
        {
            return BBDatabaseContract.TRUE_VALUE;
        }
        return BBDatabaseContract.FALSE_VALUE;
    }

    /**
     * Private helper that converts an integer in the bad budget db into a RemainAmountAction.
     * @param dbInt - an integer representing a RemainAmountAction in our db
     * @return - the corresponding RemainAmountAction
     */
    private static RemainAmountAction dbIntegerToRemainAmountAction(int dbInt)
    {
        switch (dbInt)
        {
            case REMAIN_ACTION_ACCUMULATES:
                return RemainAmountAction.accumulates;
            case REMAIN_ACTION_ADD_BACK:
                return RemainAmountAction.addBack;
            case REMAIN_ACTION_DISAPPEAR:
                return RemainAmountAction.disappear;
            default:
                return null;
        }
    }

    /**
     * Takes a RemainAmountAction and converts it into an integer that can then be added into our
     * db and later extracted.
     * @param action - the action to convert to an integer
     * @return - the integer representation of the action passed.
     */
    public static int dbRemainAmountActionToInteger(RemainAmountAction action)
    {
        switch(action)
        {
            case accumulates:
                return REMAIN_ACTION_ACCUMULATES;
            case addBack:
                return REMAIN_ACTION_ADD_BACK;
            case disappear:
                return REMAIN_ACTION_DISAPPEAR;
            default:
                return -1;
        }
    }

    /**
     * Given an integer from a bb database representing a frequency, this helper returns the equivalent
     * frequency object/enum constant
     * @param dbInt - the integer from the db representing a frequency
     * @return - the Frequency that the passed integer represents or null if it doesn't represent a valid frequency
     */
    private static Frequency dbIntegerToFrequency(int dbInt)
    {
        switch (dbInt)
        {
            case FREQ_ONETIME:
                return Frequency.oneTime;
            case FREQ_DAILY:
                return Frequency.daily;
            case FREQ_WEEKLY:
                return Frequency.weekly;
            case FREQ_BIWEEKLY:
                return Frequency.biWeekly;
            case FREQ_MONTHLY:
                return Frequency.monthly;
            case FREQ_YEARLY:
                return Frequency.yearly;
            default:
                return null;
        }
    }

    /**
     * Given a frequency this method returns the integer representing that frequency. To be used when
     * preparing an object for insertion into the db that has a frequency associated with it.
     * @param freq - the frequency we want the integer representation of
     * @return the integer representation of the passed frequency.
     */
    public static int dbFrequencyToInteger(Frequency freq)
    {
        switch (freq)
        {
            case oneTime:
                return FREQ_ONETIME;
            case daily:
                return FREQ_DAILY;
            case weekly:
                return FREQ_WEEKLY;
            case biWeekly:
                return FREQ_BIWEEKLY;
            case monthly:
                return FREQ_MONTHLY;
            case yearly:
                return FREQ_YEARLY;
            default:
                return -1;
        }
    }

    /**
     * Given a string from the bb database representing a date, this method returns the date
     * representation of that string. If the string represents null then this method returns null.
     *
     * @param dateString - the date from the db in string format (should be mm-dd-yyyy)
     * @return - the Date object corresponding to the passed string, or null if the string is represents
     * null.
     */
    private static Date dbStringToDate(String dateString)
    {
        if (dateString != null)
        {
            try {
                DateFormat format = new SimpleDateFormat("MM-dd-yyyy", Locale.ENGLISH);
                Date date = format.parse(dateString);
                return date;
            }
            catch (ParseException e)
            {
                //TODO
                System.err.println("Parse Exception");
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    /**
     * Converts the given date into a string to be inserted into the database as a date.  If passed
     * a null date then this method returns null. This operation
     * can be undone with the dbStringToDate method.
     *
     * @param date - the date to represent as a string
     * @return - the string representation of the passed date.
     */
    public static String dbDateToString(Date date)
    {
        if (date != null)
        {
            DateFormat format = new SimpleDateFormat("MM-dd-yyyy", Locale.ENGLISH);
            return format.format(date);
        }
        else
        {
            return null;
        }
    }

    /**
     * Given a debt type this mehtod returns the database representation as an integer of that debt
     * type.
     * @param type - the debtType to convert to an integer
     * @return - the integer representation of the passed debt type
     */
    public static int dbDebtTypeToInteger(DebtType type)
    {
        switch (type)
        {
            case CreditCard:
                return DEBT_TYPE_CREDIT_CARD;
            case Loan:
                return DEBT_TYPE_LOAN;
            case Other:
                return DEBT_TYPE_OTHER;
            default:
                //Shouldn't enter the default
                return -1;
        }
    }

    /**
     * Given an integer returns the DebtType associated with that integer
     * @param debtTypeInt - the integer we would like the debt type for
     */
    public static DebtType dbIntegerToDebtType(int debtTypeInt)
    {
        switch (debtTypeInt)
        {
            case DEBT_TYPE_CREDIT_CARD:
                return DebtType.CreditCard;
            case DEBT_TYPE_LOAN:
                return DebtType.Loan;
            case DEBT_TYPE_OTHER:
                return DebtType.Other;
            default:
                return null;
        }

    }

    /**
     * Converts an integer from our database into a tracker history action
     * @param actionInt - the db int that was extracted
     * @return the tracker history action represented by actionInt
     */
    public static TrackerHistoryItem.TrackerAction dbIntegerToAction(int actionInt)
    {
        switch (actionInt)
        {
            case ACTION_SUBTRACT:
            {
                return TrackerHistoryItem.TrackerAction.subtract;
            }
            case ACTION_ADD:
            {
                return TrackerHistoryItem.TrackerAction.add;
            }
            case ACTION_SET_INCREASE:
            {
                return TrackerHistoryItem.TrackerAction.setIncrease;
            }
            case ACTION_SET_DECREASE:
            {
                return TrackerHistoryItem.TrackerAction.setDecrease;
            }
            case ACTION_USER_RESET:
            {
                return TrackerHistoryItem.TrackerAction.userReset;
            }
            case ACTION_AUTO_RESET:
            {
                return TrackerHistoryItem.TrackerAction.autoReset;
            }
            default:
            {
                return null;
            }
        }
    }

    /**
     * Converts a tracker history action into an integer to be stored in our database
     * @param action - the action to convert
     * @return - the integer representation of the action in our db.
     */
    public static int dbActionToInteger(TrackerHistoryItem.TrackerAction action)
    {
        switch (action)
        {
            case subtract:
            {
                return ACTION_SUBTRACT;
            }
            case add:
            {
                return ACTION_ADD;
            }
            case setIncrease:
            {
                return ACTION_SET_INCREASE;
            }
            case setDecrease:
            {
                return ACTION_SET_DECREASE;
            }
            case userReset:
            {
                return ACTION_USER_RESET;
            }
            case autoReset:
            {
                return ACTION_AUTO_RESET;
            }
            default:
            {
                return -1;
            }
        }
    }

    private static final int ACTION_SUBTRACT = 0;
    private static final int ACTION_ADD = 1;
    private static final int ACTION_SET_DECREASE = 2;
    private static final int ACTION_SET_INCREASE = 5;
    private static final int ACTION_USER_RESET = 3;
    private static final int ACTION_AUTO_RESET = 4;

    private static final String TEXT_TYPE = "TEXT";
    private static final String REAL_TYPE = "REAL";
    private static final String INTEGER_TYPE = "INTEGER";
    private static final String DATE_TYPE = "DATE";
    private static final String BOOLEAN_TYPE = "BOOLEAN";
    private static final String PRIMARY_KEY = "PRIMARY KEY";

    public static final int TRUE_VALUE = 1;
    public static final int FALSE_VALUE = 0;

    /* Constants used to go from DB frequency integer to the Frequency type from the BBLogic classes */
    private static final int FREQ_ONETIME = 0;
    private static final int FREQ_DAILY = 1;
    private static final int FREQ_WEEKLY = 2;
    private static final int FREQ_BIWEEKLY = 3;
    private static final int FREQ_MONTHLY = 4;
    private static final int FREQ_YEARLY = 5;

    /* Constants used to go from DB Debt Type integer to the DebtType of the BBLogic classes */
    private static final int DEBT_TYPE_CREDIT_CARD = 0;
    private static final int DEBT_TYPE_LOAN = 1;
    private static final int DEBT_TYPE_OTHER = 2;

    private static final int REMAIN_ACTION_ACCUMULATES = 0;
    private static final int REMAIN_ACTION_DISAPPEAR = 1;
    private static final int REMAIN_ACTION_ADD_BACK = 2;

    /**
     *  Schema for any cash account including both regular and savings accounts
     *  */
    public static class CashAccounts implements BaseColumns
    {
        public static final String TABLE_NAME = "cashAccounts";

        /* Relevant fields for all accounts */
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_VALUE = "value";
        public static final String COLUMN_QUICK_LOOK = "quickLook";
        public static final String COLUMN_SAVINGS = "savings";

        /* Fields only relevant if this account is considered a savings account */
        public static final String COLUMN_GOAL = "goal";
        public static final String COLUMN_CONTRIBUTION_AMOUNT = "contributionAmount";
        public static final String COLUMN_CONTRIBUTION_FREQUENCY = "contributionFrequency";
        public static final String COLUMN_NEXT_CONTRIBUTION = "nextContribution";
        public static final String COLUMN_END_DATE = "endDate";
        public static final String COLUMN_GOAL_DATE = "goalDate";
        public static final String COLUMN_SOURCE_NAME = "sourceName";
        public static final String COLUMN_INTEREST_RATE = "interestRate";

        /* Define the types of each field  */
        public static final String NAME_TYPE = TEXT_TYPE + " " + PRIMARY_KEY ;
        public static final String VALUE_TYPE = REAL_TYPE;
        public static final String QUICK_LOOK_TYPE = BOOLEAN_TYPE;
        public static final String SAVINGS_TYPE = BOOLEAN_TYPE;

        public static final String GOAL_TYPE = REAL_TYPE;
        public static final String CONTRIBUTION_AMOUNT_TYPE = REAL_TYPE;
        public static final String CONTRIBUTION_FREQUENCY_TYPE = INTEGER_TYPE;
        public static final String NEXT_DATE_TYPE = DATE_TYPE;
        public static final String END_DATE_TYPE = DATE_TYPE;
        public static final String GOAL_DATE_TYPE = DATE_TYPE;
        public static final String SOURCE_NAME_TYPE = TEXT_TYPE;
        public static final String INTEREST_RATE_TYPE = REAL_TYPE;

        /* Foreign key constraint. We require the source of a savings account to be another
        * account already in the users account list */
        public static final String FOREIGN_KEY_COLUMN = COLUMN_SOURCE_NAME;
        public static final String REFERENCES_TABLE = TABLE_NAME;
        public static final String REFERENCES_COLUMN = COLUMN_NAME;
    }

    /**
     * Schema for all debts (regular, credit cards, and loans)
     */
    public static class Debts implements BaseColumns
    {
        public static final String TABLE_NAME = "debts";

        /* Columns in the debt table */
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_AMOUNT = "amount";
        public static final String COLUMN_QUICK_LOOK = "quicklook";
        public static final String COLUMN_DEBT_TYPE = "debtType"; //Regular, credit, loan
        public static final String COLUMN_INTEREST_RATE = "interestRate"; //Only applicable for credit cards and loans
        public static final String COLUMN_SIMPLE_INTEREST = "simpleInterest"; //Only applicable for loans
        public static final String COLUMN_PRINCIPLE = "principle"; //Only applicable for simple loans

        /* Columns specific to the payment associated with this debt (if any) */
        public static final String COLUMN_PAYMENT_AMOUNT = "paymentAmount";
        public static final String COLUMN_PAYMENT_SOURCE = "paymentSource";
        public static final String COLUMN_PAYMENT_FREQUENCY = "paymentFrequency";
        public static final String COLUMN_PAYMENT_NEXT_PAYMENT = "nextPayment";
        public static final String COLUMN_PAYMENT_END_DATE = "paymentEndDate";
        public static final String COLUMN_PAYMENT_GOAL_DATE = "paymentGoalDate";
        public static final String COLUMN_PAYMENT_ONGOING = "paymentOngoing";
        public static final String COLUMN_PAYMENT_PAYOFF = "paymentPayoff";

        /* Column data types */
        public static final String NAME_TYPE = TEXT_TYPE + " " + PRIMARY_KEY;
        public static final String AMOUNT_TYPE = REAL_TYPE;
        public static final String QUICKLOOK_TYPE = BOOLEAN_TYPE;
        public static final String DEBT_TYPE = INTEGER_TYPE;
        public static final String PAYMENT_AMOUNT_TYPE = REAL_TYPE;
        public static final String PAYMENT_SOURCE_TYPE = TEXT_TYPE;
        public static final String PAYMENT_FREQUENCY_TYPE = INTEGER_TYPE;
        public static final String PAYMENT_NEXT_DATE_TYPE = DATE_TYPE;
        public static final String PAYMENT_END_DATE_TYPE = DATE_TYPE;
        public static final String PAYMENT_GOAL_DATE_TYPE = DATE_TYPE;
        public static final String PAYMENT_ONGOING_TYPE = BOOLEAN_TYPE;
        public static final String PAYMENT_PAYOFF_TYPE = BOOLEAN_TYPE;
        public static final String INTEREST_RATE_TYPE = REAL_TYPE;
        public static final String SIMPLE_INTEREST_TYPE = BOOLEAN_TYPE;
        public static final String PRINCIPLE_TYPE = REAL_TYPE;

        /* Foreign key constraint. The payment source must be an account from the user's account
        * list*/
        public static final String FOREIGN_KEY_COLUMN = COLUMN_PAYMENT_SOURCE;
        public static final String REFERENCES_TABLE = CashAccounts.TABLE_NAME;
        public static final String REFERENCES_COLUMN = CashAccounts.COLUMN_NAME;
    }

    /**
     * Schema for the user's gains data.
     */
    public static class Gains implements BaseColumns
    {
        public static final String TABLE_NAME = "gains";

        /* Columns */
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_AMOUNT = "amount";
        public static final String COLUMN_FREQUENCY = "frequency";
        public static final String COLUMN_NEXT_GAIN = "nextGain";
        public static final String COLUMN_END_DATE = "endDate";
        public static final String COLUMN_DESTINATION = "destination";

        /* Column types */
        public static final String DESCRIPTION_TYPE = TEXT_TYPE;
        public static final String AMOUNT_TYPE = REAL_TYPE;
        public static final String FREQUENCY_TYPE = INTEGER_TYPE;
        public static final String NEXT_DATE_TYPE = DATE_TYPE;
        public static final String END_DATE_TYPE = DATE_TYPE;
        public static final String DESTINATION_TYPE = TEXT_TYPE;

        /* Foreign key constraint. The destination account must be an account in the user's
        * account table */
        public static final String FOREIGN_KEY_COLUMN = COLUMN_DESTINATION;
        public static final String REFERENCES_TABLE = CashAccounts.TABLE_NAME;
        public static final String REFERENCES_COLUMN = CashAccounts.COLUMN_NAME;
    }

    /**
     * Schema for the user's losses data
     */
    public static class Losses implements BaseColumns
    {
        public static final String TABLE_NAME = "losses";

        /* Columns */
        public static final String COLUMN_EXPENSE = "expense";
        public static final String COLUMN_AMOUNT = "amount";
        public static final String COLUMN_FREQUENCY = "frequency";
        public static final String COLUMN_NEXT_LOSS = "nextLoss";
        public static final String COLUMN_END_DATE = "endDate";
        public static final String COLUMN_SOURCE = "source";

        /* Column types */
        public static final String EXPENSE_TYPE = TEXT_TYPE;
        public static final String AMOUNT_TYPE = REAL_TYPE;
        public static final String FREQUENCY_TYPE = INTEGER_TYPE;
        public static final String NEXT_DATE_TYPE = DATE_TYPE;
        public static final String END_DATE_TYPE = DATE_TYPE;
        public static final String SOURCE_TYPE = TEXT_TYPE;

        /* A constraint for the source column of this table is that it must exist in either
         * the accounts table or the debts table (only as a credit card) This will need
         * to be implemented in code when determining what to display to the user
         * in the dropdown for the source of a loss */
    }

    /**
     * Schema for the user's budget preferences
     */
    public static class BudgetPreferences implements BaseColumns
    {
        public static final String TABLE_NAME = "budgetPreferences";

        /* Columns */
        public static final String COLUMN_AUTO_UPDATE = "autoUpdate";
        public static final String COLUMN_AUTO_RESET = "autoReset";
        public static final String COLUMN_REMAIN_AMOUNT_ACTION = "remainAmountAction";
        public static final String COLUMN_BUDGET_SOURCE = "budgetSource";
        public static final String COLUMN_WEEKLY_RESET = "weeklyReset";
        public static final String COLUMN_MONTHLY_RESET = "monthlyReset";

        /* Column types */
        public static final String AUTO_UPDATE_TYPE = BOOLEAN_TYPE;
        public static final String AUTO_RESET_TYPE = BOOLEAN_TYPE;
        public static final String REMAIN_AMOUNT_ACTION_TYPE = INTEGER_TYPE;
        public static final String BUDGET_SOURCE_TYPE = TEXT_TYPE;
        public static final String WEEKLY_RESET_TYPE = INTEGER_TYPE;
        public static final String MONTHLY_RESET_TYPE = INTEGER_TYPE;

        /* The constraint for a budget (and its items) is the same as that for money losses */
    }

    /**
     * Schema for the user's budget items
     */
    public static class BudgetItems implements BaseColumns
    {
        public static final String TABLE_NAME = "budgetItems";

        /* Columns - note a budget item's source is defined in the user's preferences */
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_AMOUNT = "amount";
        public static final String COLUMN_FREQUENCY = "frequency";
        public static final String COLUMN_NEXT_LOSS = "nextLoss";
        public static final String COLUMN_END_DATE = "endDate";
        public static final String COLUMN_PRORATED_START = "proratedStart";

        //Tracker Columns
        public static final String COLUMN_PLUS_AMOUNT = "plusAmount";
        public static final String COLUMN_MINUS_AMOUNT = "minusAmount";
        public static final String COLUMN_REMAINING_AMOUNT = "remainingAmount";

        /* Column types */
        public static final String DESCRIPTION_TYPE  = TEXT_TYPE;
        public static final String AMOUNT_TYPE = REAL_TYPE;
        public static final String FREQUENCY_TYPE = INTEGER_TYPE;
        public static final String NEXT_DATE_TYPE = DATE_TYPE;
        public static final String END_DATE_TYPE = DATE_TYPE;
        public static final String PRORATED_START_TYPE = BOOLEAN_TYPE;

        //Tracker Column Types
        public static final String PLUS_AMOUNT_TYPE = REAL_TYPE;
        public static final String MINUS_AMOUNT_TYPE = REAL_TYPE;
        public static final String REMAINING_AMOUNT_TYPE = REAL_TYPE;

        /* The constraint for a budget (and its items) is the same as that for money losses */
    }

    /**
     * Schema for tracker history items
     */
    public static class TrackerHistoryItems implements BaseColumns
    {
        public static final String TABLE_NAME = "trackerHistoryItems";

        /* Columns - note a budget item's source is defined in the user's preferences */
        public static final String COLUMN_BUDGET_ITEM_DESCRIPTION = "budgetItemDescription";
        public static final String COLUMN_USER_TRANSACTION_DESCRIPTION = "userTransactionDescription";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_DAY = "day";
        public static final String COLUMN_TIME = "time";
        public static final String COLUMN_ACTION = "action";
        public static final String COLUMN_ACTION_AMOUNT = "addSubtractAmount";
        public static final String COLUMN_ORIGINAL_BUDGET_AMOUNT = "originalBudgetAmount";
        public static final String COLUMN_UPDATED_BUDGET_AMOUNT = "updatedBudgetAmount";

        /* Column types */
        public static final String BUDGET_ITEM_DESCRIPTION_TYPE  = TEXT_TYPE;
        public static final String USER_TRANSACTION_DESCRIPTION_TYPE  = TEXT_TYPE;
        public static final String DATE_TYPE = TEXT_TYPE;
        public static final String DAY_TYPE = TEXT_TYPE;
        public static final String TIME_TYPE = TEXT_TYPE;
        public static final String ACTION_TYPE  = INTEGER_TYPE;
        public static final String ACTION_AMOUNT_TYPE  = REAL_TYPE;
        public static final String ORIGINAL_BUDGET_AMOUNT_TYPE  = REAL_TYPE;
        public static final String UPDATED_BUDGET_AMOUNT_TYPE  = REAL_TYPE;

        /* Default timestamp column and how to sort tracker history items using that column*/
        public static final String COLUMN_CREATED_AT = "CREATED_AT";
        public static final String SORT_ORDER_DESC = "DESC";
    }

    /**
     * Schema for general history items
     */
    public static class GeneralHistoryItems implements BaseColumns
    {
        public static final String TABLE_NAME = "generalHistoryItems";

        /* Columns */
        public static final String COLUMN_DESTINATION_ACTION = "destinationAction";
        public static final String COLUMN_DESTINATION_ORIGINAL = "destinationOriginal";
        public static final String COLUMN_DESTINATION_UPDATED = "destinationUpdated";

        public static final String COLUMN_SOURCE_ACTION = "sourceAction";
        public static final String COLUMN_SOURCE_ORIGINAL = "sourceOriginal";
        public static final String COLUMN_SOURCE_UPDATED = "sourceUpdated";

        public static final String COLUMN_TRANSACTION_AMOUNT = "transactionAmount";
        public static final String COLUMN_TRANSACTION_DATE = "transactionDate";
        public static final String COLUMN_TRANSACTION_DESTINATION = "transactionDestination";
        public static final String COLUMN_TRANSACTION_SOURCE = "transactionSource";

        public static final String COLUMN_DESTINATION_CAN_SHOW_CHANGE = "destinationCanShowChange";
        public static final String COLUMN_SOURCE_CAN_SHOW_CHANGE = "sourceCanShowChange";

        /* Column types */
        public static final String DESTINATION_ACTION_TYPE = INTEGER_TYPE;
        public static final String DESTINATION_ORIGINAL_TYPE = REAL_TYPE;
        public static final String DESTINATION_UPDATED_TYPE = REAL_TYPE;

        public static final String SOURCE_ACTION_TYPE = INTEGER_TYPE;
        public static final String SOURCE_ORIGINAL_TYPE = REAL_TYPE;
        public static final String SOURCE_UPDATED_TYPE = REAL_TYPE;

        public static final String TRANSACTION_AMOUNT_TYPE = REAL_TYPE;
        public static final String TRANSACTION_DATE_TYPE = TEXT_TYPE;
        public static final String TRANSACTION_DESTINATION_TYPE = TEXT_TYPE;
        public static final String TRANSACTION_SOURCE_TYPE = TEXT_TYPE;

        public static final String DESTINATION_CAN_SHOW_CHANGE_TYPE = BOOLEAN_TYPE;
        public static final String SOURCE_CAN_SHOW_CHANGE_TYPE = BOOLEAN_TYPE;
    }

    /**
     * Schema for Budget Specific Meta-Data
     */
    public static class BBMetaData implements BaseColumns
    {
        public static final String TABLE_NAME = "bbMetaData";

        public static final String COLUMN_LAST_UPDATE = "lastUpdate";
        public static final String LAST_UPDATE_TYPE = DATE_TYPE;
    }

    /**
     * Schema for Bad Budget Budgets
     */
    public static class Budgets implements BaseColumns
    {
        public static final String TABLE_NAME = "budgets";

        public static final String COLUMN_ID = "id";
        public static final String ID_TYPE = INTEGER_TYPE;

        public static final String COLUMN_NAME = "name";
        public static final String NAME_TYPE = TEXT_TYPE;
    }

    /**
     * Schema for Global Meta Data
     */
    public static class GlobalMetaData implements BaseColumns
    {
        public static final String TABLE_NAME = "globalMetaData";

        public static final String COLUMN_LAST_ID = "lastBudgetId";
        public static final String LAST_ID_TYPE = INTEGER_TYPE;

        public static final String COLUMN_DEFAULT_BUDGET_ID = "defaultBudgetId";
        public static final String DEFAULT_BUDGET_ID_TYPE = INTEGER_TYPE;
    }
}
