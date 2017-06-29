package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.badbudget.erikartymiuk.badbudget.R;
import com.erikartymiuk.badbudgetlogic.predictdataclasses.TransactionHistoryItem;

import java.util.List;

/**
 * Budget History Activity that displays a list of the history for the current budget and
 * also has a button to clear all of the current history.
 *
 * Created by Erik Artymiuk on 12/2/2016.
 */
public class BudgetHistoryActivity extends BadBudgetChildActivity
{

    ArrayAdapter<TransactionHistoryItem> mAdapter;

    /**
     * On create for the history activity. Sets the data behind the history list as the list of general
     * history items for the application.
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

        setContent(R.layout.content_general_history);

        List<TransactionHistoryItem> transactionHistoryItemList = ((BadBudgetApplication)getApplication()).getGeneralHistoryItems();
        ListView listView = (ListView)findViewById(R.id.generalHistoryListView);
        mAdapter = new GeneralHistoryListAdapter(this, transactionHistoryItemList);
        listView.setAdapter(mAdapter);
    }

    @Override
    /**
     * Method called on completion of the update task. Notifies our adapter that its data changed
     * (as it may have).
     */
    public void updateTaskCompleted(boolean updated) {
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Method called when the clear all button is pressed. Deletes all history items from our database and
     * from in memory. Notifies the adapter that its data set has changed.
     * @param view
     */
    public void clearAllButtonClick(View view)
    {
        List<TransactionHistoryItem> transactionHistoryItems = ((BadBudgetApplication) this.getApplication()).getGeneralHistoryItems();
        if (transactionHistoryItems.size() != 0) {
            BBDatabaseOpenHelper dbHelper = BBDatabaseOpenHelper.getInstance(this);
            SQLiteDatabase writableDB = dbHelper.getWritableDatabase();
            writableDB.delete(BBDatabaseContract.GeneralHistoryItems.TABLE_NAME + "_" + ((BadBudgetApplication) this.getApplication()).getSelectedBudgetId(), null, null);

            transactionHistoryItems.clear();

            mAdapter.notifyDataSetChanged();
        }
    }
}
