package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.badbudget.erikartymiuk.badbudget.BuildConfig;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.bbmain.FlavorSpecific;
import com.erikartymiuk.badbudgetlogic.main.Prediction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Tracker History Activity that displays the full history currently persisted for the user. Also
 * has a button option for clearing the full history list.
 */
public class TrackHistoryActivity extends BadBudgetChildActivity implements UpdateTaskCaller{

    TrackerHistoryListAdapter mAdapter;
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
     * as it data source. Also sets up the toolbar and navigation drawer
     * @param savedInstanceState - unused
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContent(R.layout.content_tracker_history);

        ListView trackerHistoryListView = (ListView) findViewById(R.id.trackerHistoryListView);

         /* Use the bad budget application wide data tracker history items to get a hold of all the tracker history */
        List<TrackerHistoryItem> trackerHistoryItems = ((BadBudgetApplication) this.getApplication()).getTrackerHistoryItems();

        //TODO - rough draft 4/14/2017
        /*
        //Group into days
        Date currCumulativeDate = null;
        ArrayList<TrackerHistoryItem> dayItems = new ArrayList<TrackerHistoryItem>();
        int currCumulativeIndex = -1;

        for (int i = 0; i < trackerHistoryItems.size(); i++)
        {
            TrackerHistoryItem historyItem = trackerHistoryItems.get(i);
            try{

                Date currDate = new SimpleDateFormat(BadBudgetApplication.TRACKER_DATE_FORMAT).parse(historyItem.getDateString());
                if (currCumulativeDate == null || !Prediction.datesEqualUpToDay(currCumulativeDate, currDate))
                {
                    currCumulativeDate = currDate;
                    currCumulativeIndex++;
                    dayItems.add(new TrackerHistoryItem(historyItem.getBudgetItemDescription(),
                            historyItem.getUserTransactionDescription(), historyItem.getDateString(),
                            historyItem.getDayString(), "", historyItem.getAction(),
                            historyItem.getActionAmount(), historyItem.getOriginalBudgetAmount(),
                            historyItem.getUpdatedBudgetAmount()));
                }
                else
                {
                    TrackerHistoryItem currCumualtiveItem = dayItems.get(currCumulativeIndex);
                    double totalActionAmount = currCumualtiveItem.getActionAmount() + historyItem.getActionAmount();
                    System.out.println(totalActionAmount);
                }

            }
            catch (ParseException pe)
            {
                pe.printStackTrace();
            }
        }
        */
        //TODO
        mAdapter = new TrackerHistoryListAdapter(this, trackerHistoryItems);
        trackerHistoryListView.setAdapter(mAdapter);

        if (BuildConfig.FREE_VERSION)
        {
            FlavorSpecific.adRequest(this, R.layout.content_tracker_history);
        }
    }

    /**
     * Method called when the user clicks the clear all button. Immediately deletes all items in
     * our tracker history from the database and clears the tracker history application items list
     * Then notifies our listAdapter that the data set has changed
     * @param view - the button that was clicked on
     */
    public void clearAllButtonClick(View view)
    {
        BBDatabaseOpenHelper dbHelper = BBDatabaseOpenHelper.getInstance(this);
        SQLiteDatabase writableDB = dbHelper.getWritableDatabase();
        writableDB.delete(BBDatabaseContract.TrackerHistoryItems.TABLE_NAME + "_" + ((BadBudgetApplication)this.getApplication()).getSelectedBudgetId(), null, null);

        List<TrackerHistoryItem> trackerHistoryItems = ((BadBudgetApplication) this.getApplication()).getTrackerHistoryItems();
        trackerHistoryItems.clear();
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Method called upon completion of the update task. Checks if any update occurred and if
     * so it notifies our list adapter that it's data may have changed.
     * @param updated - indicates if the Prediction.update method was run potentially changing
     *                our bb data objects.
     */
    public void updateTaskCompleted(boolean updated)
    {
        if (updated)
        {
            mAdapter.notifyDataSetChanged();
        }
    }
}
