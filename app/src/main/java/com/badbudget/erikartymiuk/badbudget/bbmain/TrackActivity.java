package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.BuildConfig;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.inputforms.TrackCalcFrag;
import com.badbudget.erikartymiuk.badbudget.inputforms.TrackerItem;
import com.badbudget.erikartymiuk.bbmain.FlavorSpecific;
import com.erikartymiuk.badbudgetlogic.budget.BudgetItem;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Activity for tracking individual budget items. Each item is displayed in a table with its current
 * remaining amount, next reset day, and the plus and minus "buttons"
 */
public class TrackActivity extends BadBudgetChildActivity implements TrackCalcFrag.TrackerCalculatorListener {

    private final static String TRACK_CALC_TAG = "TRACK_CALC_TAG";

    /*
    At any particular time their should be at most one stale item thus limiting time for
    updating the database
     */
    private BudgetItem staleItem = null;
    private double staleItemOriginalAmount = 0;

    private HashMap<String, TextView> remainViews;
    private HashMap<String, TextView> nextViews;
    private HashMap<String, Button> minusButtonViews;
    private HashMap<String, Button> plusButtonViews;

    /**
     * On create for this activity. Creates a table row for each budget item including the
     * description, plus amount, remain amount, minus amount, and the next date. Also sets
     * up the click listeners for the plus, minus buttons along with the item description for
     * editing the item's tracker values.
     * @param savedInstanceState - unused, trackers should have no state to save or restore
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContent(R.layout.content_track);
        populateTrackers();

        if (BuildConfig.FREE_VERSION)
        {
            FlavorSpecific.adRequest(this, R.layout.content_track);
        }
    }

    /**
     * Method called after the update task has completed. Refreshes the necessary tracker views
     * that may have changed (includes the remaining amount views and the next reset date views)
     * @param updated - true if an update to bb objects occurred false otherwise, unused
     */
    public void updateTaskCompleted(boolean updated)
    {
        refreshTableForUpdate();
    }

    /**
     * Method called upon completion of the edit tracker item activity form which this activity started.
     * Updates the tracker edited.
     * @param requestCode - should be FORM_REQUEST_RESULT
     * @param resultCode - indicates what action the user took in the form. for trackers this should
     *                   only be the FORM_RESULT_EDIT result.
     * @param data - unused
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == BadBudgetApplication.FORM_RESULT_REQUEST) {
            switch (resultCode) {
                case BadBudgetApplication.FORM_RESULT_EDIT: {
                    refreshTableForEdit();
                    break;
                }
                default: {
                    //Up, Back, Cancel
                    break;
                }
            }
        }
    }

    /**
     * Private helper method to be called when the update task has completed. Updates the remain views
     * and next date views for each of the trackers in our list
     */
    private void refreshTableForUpdate()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        for (BudgetItem item : bbd.getBudget().getAllBudgetItems().values()) {
            TextView remainView = remainViews.get(item.getDescription());
            remainView.setText(Double.toString(item.getCurrAmount()));

            TextView nextView = nextViews.get(item.getDescription());
            nextView.setText(BadBudgetApplication.dateString(item.nextLoss()));
        }
    }

    /**
     * Private helper method to be called when the form returns a result indicating an item
     * was edited. Updates the minus/plus amount, the remain views, and the next date views
     */
    private void refreshTableForEdit()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        for (BudgetItem item : bbd.getBudget().getAllBudgetItems().values()) {
            TextView remainView = remainViews.get(item.getDescription());
            remainView.setText(Double.toString(item.getCurrAmount()));

            Button minusButton = minusButtonViews.get(item.getDescription());
            double minusAmount = item.getMinusAmount();
            String minusAmountString = getString(R.string.tracker_minus_prefix) + Double.toString(minusAmount);
            minusButton.setText(minusAmountString);

            Button plusButton = plusButtonViews.get(item.getDescription());
            double plusAmount = item.getPlusAmount();
            String plusAmountString = getString(R.string.tracker_plus_prefix) + Double.toString(plusAmount);
            plusButton.setText(plusAmountString);
        }
    }

    /**
     * Private helper method that clears all trackers from our list.
     */
    private void clearTable()
    {
        LinearLayout cardsLinearLayout = (LinearLayout) findViewById(R.id.trackerLinearLayout);
        cardsLinearLayout.removeAllViews();
    }

    /**
     * Overridden on pause method for whenever the tracker activity is being moved to the background
     * Updates the stale item if their is one.
     */
    protected void onPause()
    {
        super.onPause();
        updateStaleItem();
    }

    /**
     * Checks to see if their is a stale item (i.e. staleItem != null) and updates that item in our
     * database if there is and sets stale item to null indicating everything is up to date. Additionally,
     * adds an entry to the applications tracker history. If, however, the stale
     * item current amount is equal to the original amount than no update is needed and staleItem is
     * simply set to null.
     * After a call to this method
     * the in memory budget item should have its amount synced with what the stale item is set to and
     * any views that display the item's remaining amount should be updated prior to this call as
     * well.
     */
    public void updateStaleItem()
    {
        if (staleItem != null && staleItem.getCurrAmount() - staleItemOriginalAmount != 0)
        {
            double diff = staleItem.getCurrAmount() - staleItemOriginalAmount;
            if (diff > 0)
            {
                updateItem(staleItem.getDescription(), TrackerHistoryItem.TrackerAction.add,
                        Math.abs(diff), staleItemOriginalAmount, staleItem.getCurrAmount());
            }
            else if (diff < 0)
            {
                updateItem(staleItem.getDescription(), TrackerHistoryItem.TrackerAction.subtract,
                        Math.abs(diff), staleItemOriginalAmount, staleItem.getCurrAmount());
            }
        }

        //This method should always result in handling of a set stale item whether or not any update
        //is needed.
        staleItem = null;
    }

    /**
     * Private helper method that updates a budget item in our database and adds an entry
     * to our tracker history using the passed parameters. After a call to this method
     * the in memory budget item should have its amount synced with what is passed here and
     * any views that display the remaining amount should be updated prior to this call as
     * well.
     * @param itemDescription - the description of the budget item that is being updated
     * @param action - the action that is causing the update (user reset, add, subtract...,)
     * @param actionAmount - the amount to display after the action verb in the tracker history (could be change amount, updated amount...)
     * @param originalAmount - the original amount of the item prior to the action
     * @param updatedAmount - the updated amount of the item after the action
     */
    private void updateItem(String itemDescription, TrackerHistoryItem.TrackerAction action,
                           double actionAmount, double originalAmount, double updatedAmount)
    {
        String strFilter = BBDatabaseContract.BudgetItems.COLUMN_DESCRIPTION + "=?";

        BBDatabaseOpenHelper dbHelper = BBDatabaseOpenHelper.getInstance(this);
        SQLiteDatabase writableDB = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BBDatabaseContract.BudgetItems.COLUMN_REMAINING_AMOUNT, updatedAmount);

        writableDB.update(BBDatabaseContract.BudgetItems.TABLE_NAME + "_" + ((BadBudgetApplication)this.getApplication()).getSelectedBudgetId(),
                values, strFilter, new String[]{itemDescription});

        Date applicationDate = ((BadBudgetApplication)getApplication()).getToday();
        SimpleDateFormat dayFormat = new SimpleDateFormat(BadBudgetApplication.TRACKER_DAY_FORMAT);
        SimpleDateFormat timeFormat = new SimpleDateFormat(BadBudgetApplication.TRACKER_TIME_FORMAT);

        String dateString = BadBudgetApplication.dateString(applicationDate);
        String dayString = dayFormat.format(applicationDate);
        String timeString = timeFormat.format(applicationDate);

        TrackerHistoryItem historyItem = new TrackerHistoryItem(itemDescription, "", dateString, dayString, timeString,
                action, actionAmount, originalAmount, updatedAmount);
        List<TrackerHistoryItem> appTrackerHistory = ((BadBudgetApplication)getApplication()).getTrackerHistoryItems();
        appTrackerHistory.add(0, historyItem);

        ContentValues trackerHistoryItemValues = new ContentValues();
        trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_BUDGET_ITEM_DESCRIPTION, itemDescription);
        trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_USER_TRANSACTION_DESCRIPTION, "");
        trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_DATE, dateString);
        trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_DAY, dayString);
        trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_TIME, timeString);
        trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_ACTION, BBDatabaseContract.dbActionToInteger(action));
        trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_ACTION_AMOUNT, actionAmount);
        trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_ORIGINAL_BUDGET_AMOUNT, originalAmount);
        trackerHistoryItemValues.put(BBDatabaseContract.TrackerHistoryItems.COLUMN_UPDATED_BUDGET_AMOUNT, updatedAmount);
        writableDB.insert(BBDatabaseContract.TrackerHistoryItems.TABLE_NAME + "_" + ((BadBudgetApplication)this.getApplication()).getSelectedBudgetId(), null, trackerHistoryItemValues);
    }

    /**
     * Private static helper method to sort a collection of budget items. First sorts items by
     * frequency (daily->yearly)
     * and then sorts alphabetically.
     * @param unsortedBudgetItems - a collection of budget items to sort
     * @return a list of sorted budget items first on freq then alphabetically
     */
    private static List<BudgetItem> sortItems(Collection<BudgetItem> unsortedBudgetItems)
    {
        //Sort budget items first on frequency and then alphabetically
        ArrayList<BudgetItem> oneTimeItems = new ArrayList<BudgetItem>();
        ArrayList<BudgetItem> dailyItems = new ArrayList<BudgetItem>();
        ArrayList<BudgetItem> weeklyItems = new ArrayList<BudgetItem>();
        ArrayList<BudgetItem> biweeklyItems = new ArrayList<BudgetItem>();
        ArrayList<BudgetItem> monthlyItems = new ArrayList<BudgetItem>();
        ArrayList<BudgetItem> yearlyItems = new ArrayList<BudgetItem>();

        for (BudgetItem item : unsortedBudgetItems)
        {
            switch (item.lossFrequency())
            {
                case oneTime:
                {
                    oneTimeItems.add(item);
                    break;
                }
                case daily:
                {
                    dailyItems.add(item);
                    break;
                }
                case weekly:
                {
                    weeklyItems.add(item);
                    break;
                }
                case biWeekly:
                {
                    biweeklyItems.add(item);
                    break;
                }
                case monthly:
                {
                    monthlyItems.add(item);
                    break;
                }
                case yearly:
                {
                    yearlyItems.add(item);
                    break;
                }
            }
        }

        Comparator<BudgetItem> comparator = new Comparator<BudgetItem>() {
            @Override
            public int compare(BudgetItem lhs, BudgetItem rhs) {
                return lhs.getDescription().compareTo(rhs.getDescription());
            }
        };

        Collections.sort(oneTimeItems, comparator);
        Collections.sort(dailyItems, comparator);
        Collections.sort(weeklyItems, comparator);
        Collections.sort(biweeklyItems, comparator);
        Collections.sort(monthlyItems, comparator);
        Collections.sort(yearlyItems, comparator);

        ArrayList<BudgetItem> sortedItems = new ArrayList<BudgetItem>();
        BadBudgetApplication.appendItems(sortedItems, oneTimeItems);
        BadBudgetApplication.appendItems(sortedItems, dailyItems);
        BadBudgetApplication.appendItems(sortedItems, weeklyItems);
        BadBudgetApplication.appendItems(sortedItems, biweeklyItems);
        BadBudgetApplication.appendItems(sortedItems, monthlyItems);
        BadBudgetApplication.appendItems(sortedItems, yearlyItems);

        return sortedItems;
    }

    /**
     * Private helper method that sets up our list of trackers, adding a tracker for each budget
     * item currently in the application's bad budget data.
     */
    private void populateTrackers()
    {
        /* Use the bad budget application wide data object to get a hold of all the user's budget items */
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        Collection<BudgetItem> budgetItems = bbd.getBudget().getAllBudgetItems().values();

        LinearLayout cardsLinearLayout = (LinearLayout) findViewById(R.id.trackerLinearLayout);

        List<BudgetItem> sortedBudgedItems = sortItems(budgetItems);

        remainViews = new HashMap<>();
        nextViews = new HashMap<>();
        minusButtonViews = new HashMap<>();
        plusButtonViews = new HashMap<>();

        /* For each budget item we setup a row in our table */
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);


        for (final BudgetItem bItem : sortedBudgedItems) {

            LinearLayout cardContainer = (LinearLayout) inflater.inflate(R.layout.single_tracker, null);
            CardView cardView = (CardView)cardContainer.getChildAt(0);
            View imageView = cardView.getChildAt(0);
            switch (bItem.lossFrequency())
            {
                case oneTime:
                {
                    break;
                }
                case daily:
                {
                    imageView.setBackgroundResource(R.drawable.tracker_daily_background);
                    break;
                }
                case weekly:
                {
                    imageView.setBackgroundResource(R.drawable.tracker_weekly_background);
                    break;
                }
                case biWeekly:
                {
                    imageView.setBackgroundResource(R.drawable.tracker_biweekly_background);
                    break;
                }
                case monthly:
                {
                    imageView.setBackgroundResource(R.drawable.tracker_monthly_background);
                    break;
                }
                case yearly:
                {
                    imageView.setBackgroundResource(R.drawable.tracker_yearly_background);
                    break;
                }
            }

            TextView descriptionView = (TextView)cardContainer.findViewById(R.id.tracker_item_description);
            descriptionView.setPaintFlags(descriptionView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            descriptionView.setText(bItem.getDescription());

            TextView nextDateView = (TextView)cardContainer.findViewById(R.id.tracker_item_next_date);
            nextDateView.setText(BadBudgetApplication.dateString(bItem.nextLoss()));

            final TextView remainView = (TextView)cardContainer.findViewById(R.id.tracker_item_remaining);
            remainView.setText(Double.toString(bItem.getCurrAmount()));
            remainView.setPaintFlags(remainView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

            remainView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //Update any stale items before showing the dialog
                    if (staleItem != null)
                    {
                        updateStaleItem();
                    }

                    DialogFragment trackerCalculatorFragment = new TrackCalcFrag();
                    Bundle args = new Bundle();
                    args.putString(TrackCalcFrag.ITEM_DESCRIPTION_KEY, bItem.getDescription());
                    trackerCalculatorFragment.setArguments(args);
                    trackerCalculatorFragment.show(TrackActivity.this.getSupportFragmentManager(), TrackActivity.TRACK_CALC_TAG);
                }
            });

            Button minusButton = (Button)cardContainer.findViewById(R.id.tracker_item_minus);
            minusButtonViews.put(bItem.getDescription(), minusButton);
            double minusAmount = bItem.getMinusAmount();
            String minusAmountSring = getString(R.string.tracker_minus_prefix) + Double.toString(minusAmount);
            minusButton.setText(minusAmountSring);

            Button plusButton = (Button)cardContainer.findViewById(R.id.tracker_item_plus);
            plusButtonViews.put(bItem.getDescription(), plusButton);
            double plusAmount = bItem.getPlusAmount();
            String plusAmountSring = getString(R.string.tracker_plus_prefix) + Double.toString(plusAmount);
            plusButton.setText(plusAmountSring);

            cardsLinearLayout.addView(cardContainer, cardsLinearLayout.getChildCount());

            remainViews.put(bItem.getDescription(), remainView);
            nextViews.put(bItem.getDescription(), nextDateView);

            descriptionView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v)
                {

                    Intent intent = null;
                    intent = new Intent(TrackActivity.this, TrackerItem.class);

                    intent.putExtra(BadBudgetApplication.EDIT_KEY, true);
                    intent.putExtra(BadBudgetApplication.EDIT_OBJECT_ID_KEY, bItem.expenseDescription());
                    startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
                }
            });

            /**
             * We update a stale item on plus or minus click only if the stale item isn't the item just clicked
             */
            plusButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v)
                {

                    if (staleItem != null && bItem != staleItem)
                    {
                        updateStaleItem();
                    }

                    if (bItem != staleItem)
                    {
                        staleItemOriginalAmount = bItem.getCurrAmount();
                    }
                    bItem.setCurrAmount(bItem.getCurrAmount()+bItem.getPlusAmount());
                    remainView.setText(Double.toString(bItem.getCurrAmount()));
                    staleItem = bItem;
                }
            });

            minusButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    if (staleItem != null && bItem != staleItem)
                    {
                        updateStaleItem();
                    }

                    if (bItem != staleItem)
                    {
                        staleItemOriginalAmount = bItem.getCurrAmount();
                    }
                    bItem.setCurrAmount(bItem.getCurrAmount()-bItem.getMinusAmount());
                    remainView.setText(Double.toString(bItem.getCurrAmount()));
                    staleItem = bItem;
                }
            });
        }
    }

    /**
     * TODO - 10/28
     * @param view
     */
    public void historyButtonClick(View view)
    {
        Intent intent = new Intent(this, TrackHistoryActivity.class);
        startActivity(intent);
    }

    /**
     * Interface method for the TrackerCalculatorListener. This callback method uses the result, item description,
     * and reset fields of the trackCalcFrag to update the budget item to the result indicated both in the database and in memory.
     * Then it decides what entry, if any, to make in the the tracker history
     * @param trackCalcFrag - the TrackCalcFrag that the user clicked ok on after completing an operation on
     *                          the owned budget item.
     */
    public void onOkClick(TrackCalcFrag trackCalcFrag)
    {
        double resultDouble = trackCalcFrag.result();
        String itemDescription = trackCalcFrag.budgetItemDescription();
        boolean reset = trackCalcFrag.userReset();

        BadBudgetApplication app = (BadBudgetApplication)(this.getApplication());
        BudgetItem item = app.getBadBudgetUserData().getBudget().getAllBudgetItems().get(itemDescription);

        double diff = resultDouble - item.getCurrAmount();
        if (reset)
        {
            updateItem(itemDescription, TrackerHistoryItem.TrackerAction.userReset,
                    item.lossAmount(), item.getCurrAmount(), resultDouble);
        }
        else
        {
            if (diff > 0)
            {
                updateItem(itemDescription, TrackerHistoryItem.TrackerAction.add,
                        Math.abs(diff), item.getCurrAmount(), resultDouble);
            }
            else if (diff < 0)
            {
                updateItem(itemDescription, TrackerHistoryItem.TrackerAction.subtract,
                        Math.abs(diff), item.getCurrAmount(), resultDouble);
            }
        }

        item.setCurrAmount(resultDouble);
        TextView remainView = this.remainViews.get(itemDescription);
        remainView.setText(Double.toString(resultDouble));
    }
}
