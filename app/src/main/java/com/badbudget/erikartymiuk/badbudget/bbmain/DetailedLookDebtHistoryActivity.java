package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.content.Context;
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
import java.util.List;

/**
 * History for a single debt account. Based off of the detailed look general history. TODO 1/2/2017 comment
 */
public class DetailedLookDebtHistoryActivity extends DetailedLookBaseActivity
{
    private String accountId;
    private ArrayAdapter mAdapter;
    private List<TransactionHistoryItem> predictSingleHistory;
    private boolean showingAllHistory;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.detailed_look_base_linear_layout);
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.content_detailed_look_debts_history, linearLayout, true);

        Bundle args = this.getIntent().getExtras();
        accountId = (String)args.get(BadBudgetApplication.PREDICT_ACCOUNT_ID);

        getSupportActionBar().setTitle(accountId);

        predictSingleHistory = new ArrayList<TransactionHistoryItem>();

        ListView listView = (ListView)findViewById(R.id.detailedLookDebtHistory);
        mAdapter = new GeneralHistoryListAdapter(this, predictSingleHistory);
        listView.setAdapter(mAdapter);

        showingAllHistory = false;
        updateHistoryViews();

        if (BuildConfig.FREE_VERSION)
        {
            FlavorSpecific.adRequest(this, R.layout.content_detailed_look_debts_history);
        }
    }

    protected void updateHistoryViews()
    {
        predictSingleHistory.clear();
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

        for (int i = 0; i <= numDays && dayIndex - i >= 0; i++)
        {
            MoneyOwed debt = bbd.getDebtWithName(accountId);
            List<TransactionHistoryItem> transactionHistoryItems = debt.getPredictData(dayIndex - i).transactionHistory();
            if (transactionHistoryItems != null) {
                for (TransactionHistoryItem transactionHistoryItem : transactionHistoryItems) {
                    if (!predictSingleHistory.contains(transactionHistoryItem)) {
                        predictSingleHistory.add(transactionHistoryItem);
                    }
                }
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public void toggleButtonClick(View view)
    {
        showingAllHistory = !showingAllHistory;
        updateHistoryViews();
    }
}
