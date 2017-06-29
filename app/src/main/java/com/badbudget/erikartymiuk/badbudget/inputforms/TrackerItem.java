package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseContract;
import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseOpenHelper;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetChildActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.TrackerHistoryItem;
import com.erikartymiuk.badbudgetlogic.budget.BudgetItem;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetInvalidValueException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Edit form for the tracker fields of a budget item (include plus, minus amounts, remaining amount,
 */
public class TrackerItem extends BadBudgetChildActivity implements BBOperationTaskCaller {

    /* Layout fields */
    private TextView trackerItemDescriptionView;
    private EditText trackerItemRemainView;
    private EditText trackerItemPlusMinusView;

    private boolean editing;
    private String editName;

    private static final String progressDialogMessageUpdate = "Updating Item...";

    /*
    * Need to save the original amount on entry as if the user updates it we make a tracker
    * history entry
    */
    private double originalAmount;

    /* Saved state keys */
    private static final String ORIGINAL_AMOUNT_KEY = "ORIGINAL AMOUNT";
    private static final String TRACKER_NAME_KEY = "TRACKER_NAME";
    private static final String REMAINING_KEY = "REMAINING";
    private static final String PLUS_MINUS_KEY = "PLUS_MINUS";

    /**
     * On create for the tracker item activity. This activity is a form for editing (never adding or
     * deleting) the tracker fields for a passed budget item. Sets up the fields using the passed
     * name of the budget item. Should have been passed using the extras with the two keys
     * EDIT_KEY and EDIT_OBJECT_ID_KEY. If these are unspecified or EDIT_KEY is false then behavior
     * is undefined. Sets up both for a first time entry and for when we have saved state to restore
     * @param savedInstanceState - the state to restore
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BadBudgetApplication application = ((BadBudgetApplication)this.getApplication());
        if (application.getBadBudgetUserData() == null)
        {
            this.finish();
            return;
        }

        setContent(R.layout.content_tracker_item);

        setup();

        if (savedInstanceState == null)
        {
            firstTimeSetup();
        }
        else
        {
            savedStateSetup(savedInstanceState);
        }
    }

    /**
     * Private helper method to setup necessary views for the tracker item edit form; invoked regardless
     * of whether first time setup or restoring from a saved state.
     */
    private void setup()
    {
        trackerItemDescriptionView = (TextView) this.findViewById(R.id.trackerItemDescriptionInput);
        trackerItemRemainView = (EditText) this.findViewById(R.id.trackerItemRemainAmountInput);
        trackerItemPlusMinusView = (EditText) this.findViewById(R.id.trackerItemPlusMinusAmountInput);

        Bundle args = this.getIntent().getExtras();
        if (args != null)
        {
            editing = args.getBoolean(BadBudgetApplication.EDIT_KEY);
            if (editing) {
                //Get the name of the budget item we are editing
                editName = args.getString(BadBudgetApplication.EDIT_OBJECT_ID_KEY);
                BudgetItem item = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData().getBudget().getAllBudgetItems().get(editName);
                trackerItemDescriptionView.setText(item.expenseDescription());
            }
        }
    }

    /**
     * Private helper method that sets up the form for being used when the user first enters.
     */
    private void firstTimeSetup()
    {
        if (editing) {
            BudgetItem item = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData().getBudget().getAllBudgetItems().get(editName);
            originalAmount = item.getCurrAmount();
            trackerItemRemainView.setText(Double.toString(item.getCurrAmount()));
            trackerItemPlusMinusView.setText(Double.toString(item.getPlusAmount()));
        }

    }

    /**
     * Sets up our form when the user returns to it the state they left it in should be restored.
     * @param savedInstanceState - the state to restore
     */
    private void savedStateSetup(Bundle savedInstanceState)
    {
        originalAmount = savedInstanceState.getDouble(ORIGINAL_AMOUNT_KEY);
        trackerItemRemainView.setText(savedInstanceState.getString(REMAINING_KEY));
        trackerItemPlusMinusView.setText(savedInstanceState.getString(PLUS_MINUS_KEY));
    }


    /**
     * Method called when the update task completes. Just returns
     * @param updated - true if an update to bb objects occurred false otherwise, unused
     */
    public void updateTaskCompleted(boolean updated)
    {

    }

    /**
     * Called prior to this activity being destroyed, saves any necessary state to restore upon
     * the users return.
     * @param outState - where to store the saved state.
     */
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putDouble(ORIGINAL_AMOUNT_KEY, originalAmount);
        outState.putString(REMAINING_KEY, trackerItemRemainView.getText().toString());
        outState.putString(PLUS_MINUS_KEY, trackerItemPlusMinusView.getText().toString());
    }

    /**
     * Method called when the cancel button is clicked by the user. Behaves identically to if the
     * back button were pressed.
     * @param view - the source of the click
     */
    public void cancelClick(View view)
    {
        this.onBackPressed();
    }

    /**
     * Method called when the submit button is pressed. Kicks off a background task to
     * edit the budget item's tracker fields in the database and in memory.
     * //TODO 10/14 - Verify values (maybe disallow negative values, or positive values for plus, minus)
     * @param view
     */
    public void submitClick(View view)
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        BudgetItem item = bbd.getBudget().getAllBudgetItems().get(editName);
        BudgetItem wrapperItem = null;

        double remainAmount = Double.parseDouble(trackerItemRemainView.getText().toString());
        double plusAmount = Double.parseDouble(trackerItemPlusMinusView.getText().toString());
        double minusAmount = Double.parseDouble(trackerItemPlusMinusView.getText().toString());

        try
        {
            wrapperItem = new BudgetItem(item.getDescription(), item.lossAmount(), item.lossFrequency(), item.nextLoss(), item.endDate(),
                    item.isProratedStart(), bbd.getBudget().getBudgetSource());
            wrapperItem.setCurrAmount(remainAmount);
            wrapperItem.setPlusAmount(plusAmount);
            wrapperItem.setMinusAmount(minusAmount);

             /* Prepare the values for insertion into the database */
            ContentValues values = new ContentValues();
            values.put(BBDatabaseContract.BudgetItems.COLUMN_MINUS_AMOUNT, minusAmount);
            values.put(BBDatabaseContract.BudgetItems.COLUMN_PLUS_AMOUNT, plusAmount);
            values.put(BBDatabaseContract.BudgetItems.COLUMN_REMAINING_AMOUNT, remainAmount);

            EditBBObjectTask task = new EditBBObjectTask(this, progressDialogMessageUpdate, wrapperItem, values, this, BBObjectType.BUDGETITEM);
            task.execute();
        }
        catch (BadBudgetInvalidValueException e)
        {
            //TODO
            e.printStackTrace();
        }
    }

    /**
     * Unimplemented, only editBBObjectFinished should be called
     */
    public void addBBObjectFinished() {


    }

    /**
     * Method called on completion of the edit BB object task.
     * After the task for editing the budget/tracker item completes we still need to add an entry in
     * our tracker history both in memory and our database if the reamaining amount changed.
     * This single insertion occurs on the UI
     * thread. Once the insertion is complete this method then finishes this activity
     *
     * TODO 4/11/2017 - integrate with updateItem in TrackActivity class
     */
    public void editBBObjectFinished()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        BudgetItem item = bbd.getBudget().getAllBudgetItems().get(editName);
        double remainAmount = item.getCurrAmount();

        if (originalAmount != remainAmount)
        {
            BBDatabaseOpenHelper dbHelper = BBDatabaseOpenHelper.getInstance(this);
            SQLiteDatabase writableDB = dbHelper.getWritableDatabase();

            String budgetItemDescription = item.getDescription();

            Date applicationDate = ((BadBudgetApplication)getApplication()).getToday();
            SimpleDateFormat dayFormat = new SimpleDateFormat(BadBudgetApplication.TRACKER_DAY_FORMAT);
            SimpleDateFormat timeFormat = new SimpleDateFormat(BadBudgetApplication.TRACKER_TIME_FORMAT);

            String dateString = BadBudgetApplication.dateString(applicationDate);
            String dayString = dayFormat.format(applicationDate);
            String timeString = timeFormat.format(applicationDate);

            TrackerHistoryItem.TrackerAction action = null;
            if (remainAmount > originalAmount)
            {
                action = TrackerHistoryItem.TrackerAction.setIncrease;
            }
            else if (remainAmount < originalAmount)
            {
                action = TrackerHistoryItem.TrackerAction.setDecrease;
            }

            double actionAmount = Math.abs(originalAmount-remainAmount);
            double originalBudgetAmount = originalAmount;
            double updatedBudgetAmount = remainAmount;

            TrackerHistoryItem historyItem = new TrackerHistoryItem(budgetItemDescription, "", dateString, dayString, timeString,
                    action, actionAmount, originalBudgetAmount, updatedBudgetAmount);
            List<TrackerHistoryItem> appTrackerHistory = ((BadBudgetApplication)getApplication()).getTrackerHistoryItems();
            appTrackerHistory.add(0, historyItem);

            ContentValues trackerHistoryItemValues = new ContentValues();
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_BUDGET_ITEM_DESCRIPTION, budgetItemDescription);
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_USER_TRANSACTION_DESCRIPTION, "");
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_DATE, dateString);
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_DAY, dayString);
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_TIME, timeString);
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_ACTION, BBDatabaseContract.dbActionToInteger(action));
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_ACTION_AMOUNT, actionAmount);
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_ORIGINAL_BUDGET_AMOUNT, originalBudgetAmount);
            trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_UPDATED_BUDGET_AMOUNT, updatedBudgetAmount);
            writableDB.insert(BBDatabaseContract.TrackerHistoryItems.TABLE_NAME + "_" + ((BadBudgetApplication)this.getApplication()).getSelectedBudgetId(), null, trackerHistoryItemValues);
        }

        setResult(BadBudgetApplication.FORM_RESULT_EDIT);
        this.finish();
    }

    /**
     * Unimplemented, only editBBObjectFinished should be called
     */
    public void deleteBBObjectFinished()
    {

    }
}
