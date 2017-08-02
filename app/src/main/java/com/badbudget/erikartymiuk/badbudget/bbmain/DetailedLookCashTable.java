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
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.Prediction;
import com.erikartymiuk.badbudgetlogic.main.SavingsAccount;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Detailed look cash table activity. Displays each cash account, including savings, in a single
 * table with the value showing the value of the account on the currently chosen day. Also displays
 * the accumulated interest up to the selected day from today's date.
 */
public class DetailedLookCashTable extends DetailedLookBaseActivity {

    private HashMap<String, TextView> valueViews;
    private HashMap<String, TextView> interestViews;

    private TextView totalValueView;
    private TextView totalInterestView;

    /**
     * On create for the cash tables activity. Adds the cash tables content view to the base and
     * sets up the table entries initializing the value views and interest views
     * to the chosen date using update history
     * views.
     *
     * Sorts accounts by name.
     * @param savedInstanceState - unused
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BadBudgetApplication application = ((BadBudgetApplication)this.getApplication());
        if (application.getBadBudgetUserData() == null)
        {
            this.finish();
            return;
        }

        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.detailed_look_base_linear_layout);
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.content_detailed_look_cash, linearLayout, true);

        valueViews = new HashMap<>();
        interestViews = new HashMap<>();

        TableLayout table = (TableLayout) findViewById(R.id.dlCashAccountsTable);
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        ArrayList<Account> accounts = bbd.getAccounts();

        Comparator<Account> comparator = new Comparator<Account>() {
            @Override
            public int compare(Account lhs, Account rhs) {
                return lhs.name().compareTo(rhs.name());
            }
        };
        Collections.sort(accounts, comparator);

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

            TextView interestView = new TextView(this);
            interestViews.put(currAccount.name(), interestView);

            ((BadBudgetApplication)getApplication()).tableCellSetLayoutParams(interestView, "");
            ((BadBudgetApplication)getApplication()).tableAdjustBorderForLastColumn(interestView);

            /*
            TextView savingsView = new TextView(this);
            String savingsBoolString = CashActivity.SAVINGS_STRING_NO;
            if (currAccount instanceof SavingsAccount)
            {
                savingsBoolString = CashActivity.SAVINGS_STRING_YES;
            }

            ((BadBudgetApplication)getApplication()).tableCellSetLayoutParams(savingsView, savingsBoolString);
            ((BadBudgetApplication)getApplication()).tableAdjustBorderForLastColumn(savingsView);
            */

            TableRow.LayoutParams rowLayout = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLayout.gravity = Gravity.CENTER;
            row.addView(descriptionView);
            row.addView(amountView);
            row.addView(interestView);

            table.addView(row);
        }

        addEmptyRow();
        addTotalRow();

        updateHistoryViews();

        if (BuildConfig.FREE_VERSION)
        {
            FlavorSpecific.adRequest(this, R.layout.content_detailed_look_cash);
        }
    }

    /**
     * Updates the value views for each account in the cash table to the value it has on the current
     * chosen date. Expects a mapping between the account name and the text view in the value views
     * map. Also updates the interest views to be the accumulated interest up to the chosen date from
     * today's date to the selected date.
     *
     * Updates the totals in the total row as well.
     */
    protected void updateHistoryViews() {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        int dayIndex = Prediction.numDaysBetween(((BadBudgetApplication)getApplication()).getToday(), currentChosenDay);
        for (Account a : bbd.getAccounts()) {
            TextView amountView = valueViews.get(a.name());
            String accountValueString = BadBudgetApplication.roundedDoubleBB(a.getPredictData(dayIndex).value());
            amountView.setText(accountValueString);

            String interestString = "N/A";
            TextView interestView = interestViews.get(a.name());

            if (a instanceof SavingsAccount)
            {
                SavingsAccount sa = (SavingsAccount)a;
                interestString = BadBudgetApplication.roundedDoubleBB(sa.getPredictData(dayIndex).getAccumulatedInterest());
            }
            interestView.setText(interestString);
        }

        totalValueView.setText(BadBudgetApplication.roundedDoubleBB(getValueTotal()));
        totalInterestView.setText(BadBudgetApplication.roundedDoubleBB(getInterestTotal()));
    }

    /**
     * Private helper method that adds an empty row to our table after the current row.
     */
    private void addEmptyRow()
    {
        TableLayout table = (TableLayout) findViewById(R.id.dlCashAccountsTable);
        TableRow row = new TableRow(this);

        TextView emptyView1 = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(emptyView1, "", R.drawable.emptyborder);
        TextView emptyView2 = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(emptyView2, "", R.drawable.emptyborder);
        TextView emptyView3 = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(emptyView3, "", R.drawable.emptyborder);

        row.addView(emptyView1);
        row.addView(emptyView2);
        row.addView(emptyView3);

        table.addView(row);
    }

    /**
     * Private helper method that adds the total row to the table.
     */
    private void addTotalRow()
    {
        double valueTotal = getValueTotal();
        double interestTotal = getInterestTotal();

        TableLayout table = (TableLayout) findViewById(R.id.dlCashAccountsTable);
        TableRow row = new TableRow(this);

        TextView totalView = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(totalView, getString(R.string.table_total), R.drawable.bordertbl);

        TextView totalValueView = new TextView(this);
        this.totalValueView = totalValueView;
        String valueTotalString = BadBudgetApplication.roundedDoubleBB(valueTotal);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(totalValueView, valueTotalString, R.drawable.bordertbl);

        TextView totalInterestView = new TextView(this);
        this.totalInterestView = totalInterestView;
        String interestTotalString = BadBudgetApplication.roundedDoubleBB(interestTotal);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(totalInterestView, interestTotalString, R.drawable.borderfull);

        row.addView(totalView);
        row.addView(totalValueView);
        row.addView(totalInterestView);

        table.addView(row);
    }

    /**
     * Private helper that returns the total value of the accounts on the currentChosenDay.
     * @return total value of the accounts
     */
    private double getValueTotal()
    {
        double total = 0;
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        int dayIndex = Prediction.numDaysBetween(((BadBudgetApplication)getApplication()).getToday(), currentChosenDay);
        for (Account a : bbd.getAccounts()) {

            total+=a.getPredictData(dayIndex).value();
        }
        return total;
    }

    /**
     * Private helper that returns the total accumulated interest of the savings accounts on the currentChosenDay.
     * @return - the total accumulated interest
     */
    private double getInterestTotal()
    {
        double total = 0;
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        int dayIndex = Prediction.numDaysBetween(((BadBudgetApplication)getApplication()).getToday(), currentChosenDay);
        for (Account a : bbd.getAccounts()) {

            if (a instanceof SavingsAccount)
            {
                SavingsAccount sa = (SavingsAccount)a;
                total+=sa.getPredictData(dayIndex).getAccumulatedInterest();
            }
        }
        return total;
    }

}
