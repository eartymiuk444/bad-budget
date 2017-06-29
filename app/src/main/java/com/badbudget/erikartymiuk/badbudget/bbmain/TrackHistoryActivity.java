package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;

import com.badbudget.erikartymiuk.badbudget.BuildConfig;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.inputforms.TrackCalcFrag;
import com.badbudget.erikartymiuk.bbmain.FlavorSpecific;
import com.erikartymiuk.badbudgetlogic.budget.Budget;
import com.erikartymiuk.badbudgetlogic.main.Account;
import com.erikartymiuk.badbudgetlogic.main.Prediction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Tracker History Activity that displays the full history currently persisted for the user. Also
 * has a button option for clearing the full history list.
 */
public class TrackHistoryActivity extends BadBudgetChildActivity implements UpdateTaskCaller, AdapterView.OnItemSelectedListener,
        UserDescriptionDialog.UserDescriptionDialogListener
{

    private static final String USER_DESCRIPT_DIALOG_TAG = "user_descript_dialog_tag";

    /* History item currently being worked on by the User Description Dialog*/
    private TrackerHistoryItem dialogHistoryItem;

    private ListView trackerHistoryListView;

    /*
    The two possible adapters backing our current history list. One must be null at all times
     */
    private TrackerHistoryListAdapter mAdapter;
    private TrackerHistoryGroupListAdapter mGroupAdapter;

    private Spinner groupingsSpinner;
    private CheckBox hideResets;

    /*
    Set in onCreate and used to set the list divider for the list view when individual items
    is chosen
     */
    private Drawable defaultListViewDivider;

    /**
     * Overridden on resume method for when the Tracker History activity is resumed (can occur when brought
     * back into focus after user navigates away, or just when user first loads this activity) This
     * method creates and runs the update task displaying a progress dialog until the task completes.
     */
    protected void onResume()
    {
        super.onResume();
    }

    /**
     * On create method for this activity. Sets the content view for this activity and
     * sets the adapter for the tracker history list view
     * to be our custom TrackerHistoryListAdapter that uses the applications tracker history items
     * as it data source initially. Also sets up the toolbar and navigation drawer
     * @param savedInstanceState - unused
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        BadBudgetApplication application = ((BadBudgetApplication)this.getApplication());
        if (application.getBadBudgetUserData() == null)
        {
            this.finish();
            return;
        }

        setContent(R.layout.content_tracker_history);

        groupingsSpinner = (Spinner) findViewById(R.id.trackerHistoryGroupingsSpinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.tracker_history_groupings, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupingsSpinner.setAdapter(adapter);
        groupingsSpinner.setOnItemSelectedListener(this);
        groupingsSpinner.setSelection(0);

        hideResets = (CheckBox)findViewById(R.id.hideResetsCheckbox);
        hideResets.setChecked(false);

        trackerHistoryListView = (ListView) findViewById(R.id.trackerHistoryListView);
        defaultListViewDivider = trackerHistoryListView.getDivider();

        updateAdapter(true, null, hideResets.isChecked());

        if (BuildConfig.FREE_VERSION)
        {
            FlavorSpecific.adRequest(this, R.layout.content_tracker_history);
        }
    }

    /**
     * Overridden onSaveInstanceState method that doesn't save any state as no state is to be
     * restored.
     * @param outState - unused
     */
    protected void onSaveInstanceState(Bundle outState)
    {

    }

    /**
     * Method called when the user clicks the clear all button. Immediately deletes all items in
     * our tracker history from the database and clears the tracker history application items list
     * Then notifies our listAdapter that the data set has changed if individual items with hide resets unchecked
     * are showing.
     * If another grouping was selected (or hide resets was checked)then clear all reverts us back to individual items being selected
     * without hide resets checked.
     * @param view - the button that was clicked on
     */
    public void clearAllButtonClick(View view)
    {
        BBDatabaseOpenHelper dbHelper = BBDatabaseOpenHelper.getInstance(this);
        SQLiteDatabase writableDB = dbHelper.getWritableDatabase();
        writableDB.delete(BBDatabaseContract.TrackerHistoryItems.TABLE_NAME + "_" + ((BadBudgetApplication)this.getApplication()).getSelectedBudgetId(), null, null);

        List<TrackerHistoryItem> trackerHistoryItems = ((BadBudgetApplication) this.getApplication()).getTrackerHistoryItems();
        trackerHistoryItems.clear();

        if (mAdapter != null && !hideResets.isChecked())
        {
            mAdapter.notifyDataSetChanged();
        }
        else
        {
            hideResets.setChecked(false);
            groupingsSpinner.setSelection(0);
            updateAdapter(true, null, hideResets.isChecked());
        }
    }

    /**
     * Method called upon completion of the update task. Checks if any update occurred and if
     * so it notifies our list adapter that it's data may have changed if individual items is currently
     * selected without hide resets checked. If another grouping was selected or hide resets was checked
     * this reverts us back to individual items being selected without hide resets checked.
     * @param updated - indicates if the Prediction.update method was run potentially changing
     *                our bb data objects.
     */
    public void updateTaskCompleted(boolean updated)
    {
        if (updated)
        {
            if (mAdapter != null && !hideResets.isChecked())
            {
                mAdapter.notifyDataSetChanged();
            }
            else
            {
                hideResets.setChecked(false);
                groupingsSpinner.setSelection(0);
                updateAdapter(true, null, hideResets.isChecked());
            }
        }
    }

    /**
     * Method called when the hide reset checkbox is clicked.
     * Updates our list adapter to the appropriate grouping with or without resets hidden
     * depending on the hideResets checkbox.
     * @param view - the checkbox clicked
     */
    public void hideResetsClick(View view)
    {
        String selectedGrouping = (String) groupingsSpinner.getSelectedItem();

        if (this.getResources().getString(R.string.tracker_history_individual).equals(selectedGrouping))
        {
            updateAdapter(true, null, hideResets.isChecked());
        }
        else if (this.getResources().getString(R.string.daily).equals(selectedGrouping))
        {
            updateAdapter(false, TrackerHistoryGroupItem.GroupingType.daily, hideResets.isChecked());
        }
        else if (this.getResources().getString(R.string.weekly).equals(selectedGrouping))
        {
            updateAdapter(false, TrackerHistoryGroupItem.GroupingType.weekly, hideResets.isChecked());
        }
        else if (this.getResources().getString(R.string.monthly).equals(selectedGrouping))
        {
            updateAdapter(false, TrackerHistoryGroupItem.GroupingType.monthly, hideResets.isChecked());
        }
        else if (this.getResources().getString(R.string.yearly).equals(selectedGrouping))
        {
            updateAdapter(false, TrackerHistoryGroupItem.GroupingType.yearly, hideResets.isChecked());
        }
        else if (this.getResources().getString(R.string.tracker_history_all_time).equals(selectedGrouping))
        {
            updateAdapter(false, TrackerHistoryGroupItem.GroupingType.all_time, hideResets.isChecked());
        }
    }

    /**
     * Method called on selection of a grouping in our groupings spinner. Creates and sets the appropriate adapter
     * for the selected grouping, either the grouping adapter or the individual items adapter. The other
     * adapter is set to null.
     * @param parent - unused
     * @param view - unused
     * @param pos - the position of the item that was selected
     * @param id - unused
     */
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {

        String selectedGrouping = (String) parent.getItemAtPosition(pos);

        if (this.getResources().getString(R.string.tracker_history_individual).equals(selectedGrouping))
        {
            updateAdapter(true, null, hideResets.isChecked());
        }
        else if (this.getResources().getString(R.string.daily).equals(selectedGrouping))
        {
            updateAdapter(false, TrackerHistoryGroupItem.GroupingType.daily, hideResets.isChecked());
        }
        else if (this.getResources().getString(R.string.weekly).equals(selectedGrouping))
        {
            updateAdapter(false, TrackerHistoryGroupItem.GroupingType.weekly, hideResets.isChecked());
        }
        else if (this.getResources().getString(R.string.monthly).equals(selectedGrouping))
        {
            updateAdapter(false, TrackerHistoryGroupItem.GroupingType.monthly, hideResets.isChecked());
        }
        else if (this.getResources().getString(R.string.yearly).equals(selectedGrouping))
        {
            updateAdapter(false, TrackerHistoryGroupItem.GroupingType.yearly, hideResets.isChecked());
        }
        else if (this.getResources().getString(R.string.tracker_history_all_time).equals(selectedGrouping))
        {
            updateAdapter(false, TrackerHistoryGroupItem.GroupingType.all_time, hideResets.isChecked());
        }
    }

    /**
     * Private helper method to be called whenever the adapter backing our list needs to be updated/changed.
     * Also sets the onItemClickListener for the trackerHistoryListView
     * @param individualItems - indicates if our list should show the individual items rather than a time period grouping
     * @param groupingType - the grouping type if applicable (ignored if individualItems is true)
     * @param hideResets - indicates if our list should exclude reset actions
     */
    private void updateAdapter(boolean individualItems, TrackerHistoryGroupItem.GroupingType groupingType,
                               boolean hideResets)
    {
        List<TrackerHistoryItem> trackerHistoryItems = ((BadBudgetApplication) this.getApplication()).getTrackerHistoryItems();

        if (individualItems)
        {
            trackerHistoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    dialogHistoryItem = (TrackerHistoryItem)parent.getItemAtPosition(position);

                    //Don't show dialog if action is a reset
                    TrackerHistoryItem.TrackerAction action = dialogHistoryItem.getAction();
                    boolean showDialog = action != TrackerHistoryItem.TrackerAction.autoReset && action != TrackerHistoryItem.TrackerAction.userReset;

                    if (showDialog)
                    {
                        Bundle args = new Bundle();

                        args.putString(BadBudgetApplication.BUDGET_DESCRIPTION_KEY,
                                dialogHistoryItem.getBudgetItemDescription());
                        args.putString(BadBudgetApplication.USER_DESCRIPTION_KEY,
                                dialogHistoryItem.getUserTransactionDescription());

                        UserDescriptionDialog dialog = new UserDescriptionDialog();
                        dialog.setArguments(args);
                        dialog.show(TrackHistoryActivity.this.getSupportFragmentManager(), USER_DESCRIPT_DIALOG_TAG);
                    }
                }
            });
            trackerHistoryListView.setDivider(defaultListViewDivider);
            if (hideResets)
            {
                List<TrackerHistoryItem> abbrvTrackerHistoryItems = new ArrayList<>();
                for (TrackerHistoryItem historyItem : trackerHistoryItems)
                {
                    if (historyItem.getAction() != TrackerHistoryItem.TrackerAction.userReset &&
                            historyItem.getAction() != TrackerHistoryItem.TrackerAction.autoReset)
                    {
                        abbrvTrackerHistoryItems.add(historyItem);
                    }
                }

                mAdapter = new TrackerHistoryListAdapter(this, abbrvTrackerHistoryItems);
            }
            else
            {
                mAdapter = new TrackerHistoryListAdapter(this, trackerHistoryItems);
            }
            mGroupAdapter = null;
            trackerHistoryListView.setAdapter(mAdapter);
        }
        else
        {
            trackerHistoryListView.setOnItemClickListener(null);
            trackerHistoryListView.setDivider(null);
            Budget budget = ((BadBudgetApplication)this.getApplication()).getBadBudgetUserData().getBudget();
            List<TrackerHistoryGroupItem> groupItems =
                    TrackHistoryActivity.groupIndividualHistoryItems(trackerHistoryItems,
                            groupingType,
                            budget.getWeeklyReset(), budget.getMonthlyReset(), hideResets);
            mGroupAdapter = new TrackerHistoryGroupListAdapter(this, groupItems, hideResets);
            mAdapter = null;
            trackerHistoryListView.setAdapter(mGroupAdapter);
        }
    }

    /**
     * Empty implementation of an interface callback.
     * @param parent - unused
     */
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    /**
     * Given a date of a transaction and a grouping type, this method returns the start date of the
     * time period that the transaction would have taken place in.
     * @param transactionDate - the date the transaction occurred
     * @param groupingType - the grouping type to consider for determining the time period
     * @param weeklyBoundary - the day of the week to consider the boundary (start day of new grouping) of weekly transactions
     * @param monthlyBoundary - the day of the month to consider the boundary (start day of new grouping) of monthly transactions
     *                          TODO 5/3/2017 monthly boundaries greater than 28 are treated as 28
     * @return the start date of the time period this transaction would have taken place in or null if grouping type is all_time
     */
    private static Date findStartDate(Date transactionDate, TrackerHistoryGroupItem.GroupingType groupingType,
                                      int weeklyBoundary, int monthlyBoundary)
    {
        Calendar transactionCal = Calendar.getInstance();
        transactionCal.setTime(transactionDate);

        Calendar startDateCal = Calendar.getInstance();
        startDateCal.setTime(transactionDate);

        switch (groupingType)
        {
            case daily:
            {
                break;
            }
            case weekly:
            {
                int transactionWeekDay = transactionCal.get(Calendar.DAY_OF_WEEK);
                int dayDiff = transactionWeekDay - weeklyBoundary;
                if (dayDiff > 0)
                {
                    startDateCal.add(Calendar.DAY_OF_WEEK, -dayDiff);
                }
                else if (dayDiff < 0)
                {
                    startDateCal.add(Calendar.DAY_OF_WEEK, -(7+dayDiff));
                }
                break;
            }
            case monthly:
            {
                int transactionYear = transactionCal.get(Calendar.YEAR);
                int transactionMonth = transactionCal.get(Calendar.MONTH);
                int transactionDay = transactionCal.get(Calendar.DAY_OF_MONTH);

                startDateCal.set(Calendar.YEAR, transactionYear);
                startDateCal.set(Calendar.MONTH, transactionMonth);
                if (monthlyBoundary >= 28)
                {
                    startDateCal.set(Calendar.DAY_OF_MONTH, 28);
                }
                else
                {
                    startDateCal.set(Calendar.DAY_OF_MONTH, monthlyBoundary);
                }

                if (transactionDay < monthlyBoundary)
                {
                    startDateCal.add(Calendar.MONTH, -1);
                }
                break;
            }
            case yearly:
            {
                int transactionYear = transactionCal.get(Calendar.YEAR);

                startDateCal.set(Calendar.YEAR, transactionYear);
                startDateCal.set(Calendar.MONTH, Calendar.JANUARY);
                startDateCal.set(Calendar.DAY_OF_MONTH, 1);

                break;
            }
            case all_time:
            {
                return null;
            }
        }
        return startDateCal.getTime();
    }

    /**
     * Given a start date and a grouping type this method finds the end date for the time period,
     * defined by these two parameters.
     * @param startDate - the start date of a time period of a grouping
     * @param groupingType - the type of grouping to consider
     * @return - the end date of a time period for our grouping or null if the grouping type is all_time
     */
    private static Date findEndDate(Date startDate, TrackerHistoryGroupItem.GroupingType groupingType)
    {
        Calendar startDateCal = Calendar.getInstance();
        Calendar endDateCal = Calendar.getInstance();

        if (startDate != null) {
            startDateCal.setTime(startDate);
            endDateCal.setTime(startDate);
        }

        switch (groupingType)
        {
            case daily:
            {
                break;
            }
            case weekly:
            {
                endDateCal.add(Calendar.WEEK_OF_YEAR, 1);
                endDateCal.add(Calendar.DAY_OF_WEEK, -1);
                break;
            }
            case monthly:
            {
                endDateCal.add(Calendar.MONTH, 1);
                endDateCal.add(Calendar.DAY_OF_MONTH, -1);
                break;
            }
            case yearly:
            {
                endDateCal.add(Calendar.YEAR, 1);
                endDateCal.add(Calendar.DAY_OF_MONTH, -1);
                break;
            }
            case all_time:
            {
                return null;
            }
        }
        return endDateCal.getTime();
    }

    /**
     * Takes the full ordered list of individual history items (from most recent to least recent)
     * and groups them according to the grouping type passed.
     * @param indvItems - the individual tracker history items to group
     * @param groupingType - the grouping type to use
     * @param weeklyBoundary - the weekly boundary to use as the first day in a time period
     * @param monthlyBoundary - the monthly boundary to use as the first day in a time period
     * @param excludeResets - option to exclude reset actions from our grouping
     * @return - a list of grouped history items
     */
    private static List<TrackerHistoryGroupItem> groupIndividualHistoryItems(
            List<TrackerHistoryItem> indvItems, TrackerHistoryGroupItem.GroupingType groupingType,
            int weeklyBoundary, int monthlyBoundary, boolean excludeResets)
    {
        List<TrackerHistoryGroupItem> trackerHistoryGroupItems = new ArrayList<>();

        Date currStartDate = null;
        Date currEndDate = null;

        for (TrackerHistoryItem historyItem : indvItems)
        {
            //If we are excluding resets, check what action this history item took first
            TrackerHistoryItem.TrackerAction action = historyItem.getAction();
            boolean excludeItem = excludeResets &&
                    (action == TrackerHistoryItem.TrackerAction.userReset ||
                            action == TrackerHistoryItem.TrackerAction.autoReset);
            if (!excludeItem) {
                //Get history item into the correct time period
                Date historyItemDate = BadBudgetApplication.stringToDate(historyItem.getDateString());

                if (trackerHistoryGroupItems.isEmpty() ||
                        (groupingType != TrackerHistoryGroupItem.GroupingType.all_time && Prediction.numDaysBetween(currStartDate, historyItemDate) < 0)) {
                    currStartDate = findStartDate(historyItemDate, groupingType, weeklyBoundary, monthlyBoundary);
                    currEndDate = findEndDate(currStartDate, groupingType);
                    TrackerHistoryGroupItem groupItem = new TrackerHistoryGroupItem(groupingType, currStartDate, currEndDate);
                    trackerHistoryGroupItems.add(groupItem);
                }

                TrackerHistoryGroupItem currGroup = trackerHistoryGroupItems.get(trackerHistoryGroupItems.size() - 1);

                //Get history item into the correct budget/user description group
                Map<String, TrackerHistoryGroupBudgetItem> currGroupBudgetItems = currGroup.getGroupBudgetItemsMap();

                boolean hasUserDescription = historyItem.getUserTransactionDescription() != null &&
                        !historyItem.getUserTransactionDescription().equals("");

                TrackerHistoryGroupBudgetItem groupBudgetItem;
                if (hasUserDescription)
                {
                    if (currGroupBudgetItems.get(historyItem.getUserTransactionDescription()) == null)
                    {
                        TrackerHistoryGroupBudgetItem tempGroupBudgetItem = new TrackerHistoryGroupBudgetItem("",
                                historyItem.getUserTransactionDescription());
                        currGroupBudgetItems.put(historyItem.getUserTransactionDescription(), tempGroupBudgetItem);
                    }
                    groupBudgetItem = currGroupBudgetItems.get(historyItem.getUserTransactionDescription());
                }
                else
                {
                    if (currGroupBudgetItems.get(historyItem.getBudgetItemDescription()) == null) {

                        TrackerHistoryGroupBudgetItem tempGroupBudgetItem = new TrackerHistoryGroupBudgetItem(historyItem.getBudgetItemDescription(),
                                "");
                        currGroupBudgetItems.put(historyItem.getBudgetItemDescription(), tempGroupBudgetItem);
                    }
                    groupBudgetItem = currGroupBudgetItems.get(historyItem.getBudgetItemDescription());
                }


                //Take the action of the individual history item
                double actionAmount = historyItem.getActionAmount();

                if (action == TrackerHistoryItem.TrackerAction.add || action == TrackerHistoryItem.TrackerAction.setIncrease) {
                    groupBudgetItem.addAmount(actionAmount);
                } else if (action == TrackerHistoryItem.TrackerAction.subtract || action == TrackerHistoryItem.TrackerAction.setDecrease) {
                    groupBudgetItem.subtractAmount(actionAmount);
                } else if (action == TrackerHistoryItem.TrackerAction.userReset) {
                    groupBudgetItem.addUserResetLoss(actionAmount);
                } else if (action == TrackerHistoryItem.TrackerAction.autoReset) {
                    groupBudgetItem.addAutoResetLoss(actionAmount);
                }
            }
        }

        return trackerHistoryGroupItems;
    }

    /**
     * Method called when the ok button is pressed in the UserDescriptionDialog. The 'dialogHistoryItem' instance should
     * indicate the item we are working with. First updates the item's db User Description field then updates the in memory
     * field. Finally notifies the adapter that its data has changed. Contains a semi-hackish solution to the fact that there isn't a primary
     * key for the tracker history items in the db. So we match on every column and select the row number of a match limiting it to one (as potentially
     * could be more than one match, although unlikely) and update only one match using the first row number.
     * @param fragment - the TrackCalcFrag that ok was pressed on. Can be queried for the result of the user's action
     */

    private static final String ROW_ID = "rowid";
    public void onOkClick(UserDescriptionDialog fragment)
    {
        BBDatabaseOpenHelper dbHelper = BBDatabaseOpenHelper.getInstance(this);
        SQLiteDatabase writableDB = dbHelper.getWritableDatabase();

        //First match using all (most) columns of a tracker history item
        String[] projection = {
                ROW_ID
        };
        String strFilter =
                BBDatabaseContract.TrackerHistoryItems.COLUMN_BUDGET_ITEM_DESCRIPTION + "=?" + " AND " +
                BBDatabaseContract.TrackerHistoryItems.COLUMN_USER_TRANSACTION_DESCRIPTION + "=?" + " AND " +
                BBDatabaseContract.TrackerHistoryItems.COLUMN_DATE + "=?" + " AND " +
                BBDatabaseContract.TrackerHistoryItems.COLUMN_TIME + "=?" + " AND " +
                BBDatabaseContract.TrackerHistoryItems.COLUMN_ACTION + "=?" + " AND " +
                BBDatabaseContract.TrackerHistoryItems.COLUMN_ACTION_AMOUNT + "=?" + " AND " +
                BBDatabaseContract.TrackerHistoryItems.COLUMN_ORIGINAL_BUDGET_AMOUNT + "=?" + " AND " +
                BBDatabaseContract.TrackerHistoryItems.COLUMN_UPDATED_BUDGET_AMOUNT+ "=?";


        String[] selectionArgs = new String[] {
                        dialogHistoryItem.getBudgetItemDescription(),
                        dialogHistoryItem.getUserTransactionDescription(),
                        dialogHistoryItem.getDateString(),
                        dialogHistoryItem.getTimeString(),
                        Integer.toString(BBDatabaseContract.dbActionToInteger(dialogHistoryItem.getAction())),
                        Double.toString(dialogHistoryItem.getActionAmount()),
                        Double.toString(dialogHistoryItem.getOriginalBudgetAmount()),
                        Double.toString(dialogHistoryItem.getUpdatedBudgetAmount())
        };

        //Limit to 1 match
        Cursor cursor = writableDB.query(
                BBDatabaseContract.TrackerHistoryItems.TABLE_NAME + "_" + ((BadBudgetApplication)this.getApplication()).getSelectedBudgetId(),
                projection,
                strFilter,
                selectionArgs,
                null,
                null,
                null,
                "1"
        );

        int historyItemIdIndex = cursor.getColumnIndexOrThrow(ROW_ID);
        String updatedDescript = fragment.getUserDescription();

        //Use single matched row id to update tracker history item
        if (cursor.moveToNext())
        {
            int id = cursor.getInt(historyItemIdIndex);
            String rowFilter = ROW_ID + "=?";
            String[] rowArg = new String[] {Integer.toString(id)};

            ContentValues trackerHistoryItemValues = new ContentValues();
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_USER_TRANSACTION_DESCRIPTION, updatedDescript);

            writableDB.update(BBDatabaseContract.TrackerHistoryItems.TABLE_NAME + "_" + ((BadBudgetApplication)this.getApplication()).getSelectedBudgetId(),
                    trackerHistoryItemValues, rowFilter, rowArg);
        }

        cursor.close();

        //Update in memory and notify adapter
        dialogHistoryItem.setUserTransactionDescription(updatedDescript);
        mAdapter.notifyDataSetChanged();
    }
}
