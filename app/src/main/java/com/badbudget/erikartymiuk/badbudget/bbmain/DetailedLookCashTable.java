package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.BuildConfig;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.viewobjecttables.CashActivity;
import com.badbudget.erikartymiuk.bbmain.FlavorSpecific;
import com.erikartymiuk.badbudgetlogic.main.Account;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.Prediction;
import com.erikartymiuk.badbudgetlogic.main.SavingsAccount;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Detailed look cash table activity. Displays each cash account, including savings, in a single
 * table with the value showing the value of the account on the currently chosen day.
 */
public class DetailedLookCashTable extends DetailedLookBaseActivity {

    private HashMap<String, TextView> valueViews;

    /**
     * On create for the cash tables activity. Adds the cash tables content view to the base and
     * sets up the table entries initializing the value views to the chosen date using update history
     * views.
     * @param savedInstanceState - unused
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.detailed_look_base_linear_layout);
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.content_detailed_look_cash, linearLayout, true);

        valueViews = new HashMap<>();

        TableLayout table = (TableLayout) findViewById(R.id.dlCashAccountsTable);
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        ArrayList<Account> accounts = bbd.getAccounts();

        for (final Account currAccount : accounts)
        {
            TableRow row = new TableRow(this);

            TextView descriptionView = new TextView(this);
            ((BadBudgetApplication)getApplication()).tableCellSetLayoutParams(descriptionView, currAccount.name());
            descriptionView.setPaintFlags(descriptionView.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);

            descriptionView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v)
                {
                    Intent intent = new Intent(DetailedLookCashTable.this, DetailedLookCashHistoryActivity.class);
                    Calendar currentEndCal = Calendar.getInstance();
                    Calendar currentSelectedCal = Calendar.getInstance();
                    currentEndCal.setTime(currentEndDate);
                    currentSelectedCal.setTime(currentChosenDay);

                    intent.putExtra(BadBudgetApplication.PREDICT_CURRENT_END_DATE, currentEndCal);
                    intent.putExtra(BadBudgetApplication.PREDICT_CURRENT_CHOSEN_DAY, currentSelectedCal);
                    intent.putExtra(BadBudgetApplication.PREDICT_ACCOUNT_ID, currAccount.name());
                    startActivityForResult(intent, BadBudgetApplication.PREDICT_REQUEST_CODE_UPDATED_DATES);
                }
            });

            TextView amountView = new TextView(this);
            valueViews.put(currAccount.name(), amountView);

            ((BadBudgetApplication)getApplication()).tableCellSetLayoutParams(amountView, "");

            TextView savingsView = new TextView(this);
            String savingsBoolString = CashActivity.SAVINGS_STRING_NO;
            if (currAccount instanceof SavingsAccount)
            {
                savingsBoolString = CashActivity.SAVINGS_STRING_YES;
            }

            ((BadBudgetApplication)getApplication()).tableCellSetLayoutParams(savingsView, savingsBoolString);
            ((BadBudgetApplication)getApplication()).tableAdjustBorderForLastColumn(savingsView);

            TableRow.LayoutParams rowLayout = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLayout.gravity = Gravity.CENTER;
            row.addView(descriptionView);
            row.addView(amountView);
            row.addView(savingsView);

            table.addView(row);
        }
        updateHistoryViews();

        if (BuildConfig.FREE_VERSION)
        {
            FlavorSpecific.adRequest(this, R.layout.content_detailed_look_cash);
        }
    }

    /**
     * Updates the value views for each account in the cash table to the value it has on the current
     * chosen date. Expects a mapping between the account name and the text view in the value views
     * map.
     */
    protected void updateHistoryViews() {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        int dayIndex = Prediction.numDaysBetween(((BadBudgetApplication)getApplication()).getToday(), currentChosenDay);
        for (Account a : bbd.getAccounts()) {
            TextView amountView = valueViews.get(a.name());
            String accountValueString = BadBudgetApplication.roundedDoubleBB(a.getPredictData(dayIndex).value());
            amountView.setText(accountValueString);
        }
    }

}
