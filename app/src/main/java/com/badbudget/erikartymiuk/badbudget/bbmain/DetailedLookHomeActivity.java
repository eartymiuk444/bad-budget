package com.badbudget.erikartymiuk.badbudget.bbmain;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.badbudget.erikartymiuk.badbudget.BuildConfig;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.bbmain.FlavorSpecific;
import com.erikartymiuk.badbudgetlogic.main.Account;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.MoneyOwed;
import com.erikartymiuk.badbudgetlogic.main.Prediction;
import com.erikartymiuk.badbudgetlogic.predictdataclasses.TransactionHistoryItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Detailed look home activity. An activity home screen for looking more deeply at all the transactions
 * that occur on a specific day, up to a specific day, or either of these looking at a
 * particular account or debt.
 * Created by Erik Artymiuk on 11/7/2016.
 */
public class DetailedLookHomeActivity extends DetailedLookBaseActivity
{
    private ArrayAdapter mAdapter;
    private List<TransactionHistoryItem> predictHistory;
    private boolean showingAllHistory;

    /**
     * On create for the detailed look home activity. Adds the home content view to our base and
     * sets up our list view adapter with initial transaction data
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

        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.detailed_look_base_linear_layout);
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.content_detailed_look_home, linearLayout, true);

        predictHistory = new ArrayList<TransactionHistoryItem>();
        ListView listView = (ListView)findViewById(R.id.detailedLookHomeHistory);
        mAdapter = new GeneralHistoryListAdapter(this, predictHistory);
        listView.setAdapter(mAdapter);
        showingAllHistory = false;
        updateHistoryViews();

        if (BuildConfig.FREE_VERSION)
        {
            FlavorSpecific.adRequest(this, R.layout.content_detailed_look_home);
        }
    }

    /**
     * Updates all the home activity's views that contain history transaction data. Updates the
     * predict history instance and applications today date and instance current chosen date to
     * determine the history to populate.Also uses instance showingAllHistory to determine how
     * many days of history to show (1 or MAX_DAYS_HISTORY).
     */
    protected void updateHistoryViews()
    {
        predictHistory.clear();
        BadBudgetData bbd = ((BadBudgetApplication)this.getApplication()).getBadBudgetUserData();
        int dayIndex = Prediction.numDaysBetween(((BadBudgetApplication)getApplication()).getToday(), currentChosenDay);

        int numDays;
        if (showingAllHistory)
        {
            numDays = MAX_DAYS_HISTORY;
        }
        else
        {
            numDays = 0;
        }

        for (int i = 0; i <= numDays && dayIndex - i >= 0; i++) {
            for (MoneyOwed currDebt : bbd.getDebts()) {
                List<TransactionHistoryItem> transactionHistoryItems = currDebt.getPredictData(dayIndex - i).transactionHistory();
                if (transactionHistoryItems != null) {
                    for (TransactionHistoryItem transactionHistoryItem : transactionHistoryItems) {
                        if (!predictHistory.contains(transactionHistoryItem)) {
                            predictHistory.add(transactionHistoryItem);
                        }
                    }
                }
            }
            for (Account currAccount : bbd.getAccounts()) {
                List<TransactionHistoryItem> transactionHistoryItems = currAccount.getPredictData(dayIndex - i).transactionHistory();
                if (transactionHistoryItems != null) {
                    for (TransactionHistoryItem transactionHistoryItem : transactionHistoryItems) {
                        if (!predictHistory.contains(transactionHistoryItem)) {
                            predictHistory.add(transactionHistoryItem);
                        }
                    }
                }
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Method called on cash button click. Starts the DetailedLookCashTable activity and expects
     * a result, i.e. the updated dates, back. Passes these dates as arguments to this activity also.
     * @param view - the button view clicked on
     */
    public void cashButtonClick(View view)
    {
        Intent intent = new Intent(this, DetailedLookCashTable.class);
        Calendar currentEndCal = Calendar.getInstance();
        Calendar currentSelectedCal = Calendar.getInstance();
        currentEndCal.setTime(currentEndDate);
        currentSelectedCal.setTime(currentChosenDay);

        intent.putExtra(BadBudgetApplication.PREDICT_CURRENT_END_DATE, currentEndCal);
        intent.putExtra(BadBudgetApplication.PREDICT_CURRENT_CHOSEN_DAY, currentSelectedCal);
        startActivityForResult(intent, BadBudgetApplication.PREDICT_REQUEST_CODE_UPDATED_DATES);
    }

    /**
     * Method called on debts button click. Starts the DetailedLookDebtsTable activity and expects
     * a result, i.e. the updated dates, back. Passes these dates as arguments to this activity also.
     * @param view - the button view clicked on
     */
    public void debtsButtonClick(View view)
    {
        Intent intent = new Intent(this, DetailedLookDebtsTable.class);
        Calendar currentEndCal = Calendar.getInstance();
        Calendar currentSelectedCal = Calendar.getInstance();
        currentEndCal.setTime(currentEndDate);
        currentSelectedCal.setTime(currentChosenDay);

        intent.putExtra(BadBudgetApplication.PREDICT_CURRENT_END_DATE, currentEndCal);
        intent.putExtra(BadBudgetApplication.PREDICT_CURRENT_CHOSEN_DAY, currentSelectedCal);
        startActivityForResult(intent, BadBudgetApplication.PREDICT_REQUEST_CODE_UPDATED_DATES);
    }

    /**
     * Toggle between showing only a single day of history and a fuller MAX_DAYS history. Does
     * so by updating showingAllHistory and calling updateHistoryViews
     * @param view - the button view clicked on
     */
    public void toggleButtonClick(View view)
    {
        showingAllHistory = !showingAllHistory;
        updateHistoryViews();
    }
}
