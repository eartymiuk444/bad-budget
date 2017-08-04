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
import com.badbudget.erikartymiuk.bbmain.FlavorSpecific;
import com.erikartymiuk.badbudgetlogic.main.Account;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.CreditCard;
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.Loan;
import com.erikartymiuk.badbudgetlogic.main.MoneyOwed;
import com.erikartymiuk.badbudgetlogic.main.Payment;
import com.erikartymiuk.badbudgetlogic.main.Prediction;
import com.erikartymiuk.badbudgetlogic.main.SavingsAccount;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Detailed Look Debts Table activity. Displays all debts in a single table with the value on
 * the currently selected day.
 */
public class DetailedLookDebtsTable extends DetailedLookBaseActivity {

    private HashMap<String, TextView> valueViews;
    private HashMap<String, TextView> interestViews;

    private TextView totalDebtView;
    private TextView totalInterestView;
    private TextView changeValueView;

    public static final String DEBT_TYPE_CREDIT_CARD = "cc";
    public static final String DEBT_TYPE_LOAN = "loan";
    public static final String DEBT_TYPE_MISC = "misc";

    /**
     * On create for the dl debts table activity. Adds the content for the dl debts table to the base
     * and sets up the cell entries for each debt row on the currently selected day by calling update
     * history views.
     *
     * Sorts debts by name
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
        inflater.inflate(R.layout.content_detailed_look_debts, linearLayout, true);

        valueViews = new HashMap<>();
        interestViews = new HashMap<>();

        TableLayout table = (TableLayout) findViewById(R.id.dlDebtAccountsTable);
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        ArrayList<MoneyOwed> debts = bbd.getDebts();

        Comparator<MoneyOwed> comparator = new Comparator<MoneyOwed>() {
            @Override
            public int compare(MoneyOwed lhs, MoneyOwed rhs) {
                return lhs.name().compareTo(rhs.name());
            }
        };
        Collections.sort(debts, comparator);

        for (final MoneyOwed currDebt : debts)
        {
            TableRow row = new TableRow(this);

            TextView descriptionView = new TextView(this);
            ((BadBudgetApplication)getApplication()).tableCellSetLayoutParams(descriptionView, currDebt.name());
            descriptionView.setPaintFlags(descriptionView.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);

            descriptionView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v)
                {
                    Intent intent = new Intent(DetailedLookDebtsTable.this, DetailedLookDebtHistoryActivity.class);
                    Calendar currentEndCal = Calendar.getInstance();
                    Calendar currentSelectedCal = Calendar.getInstance();
                    currentEndCal.setTime(currentEndDate);
                    currentSelectedCal.setTime(currentChosenDay);

                    intent.putExtra(BadBudgetApplication.PREDICT_CURRENT_END_DATE, currentEndCal);
                    intent.putExtra(BadBudgetApplication.PREDICT_CURRENT_CHOSEN_DAY, currentSelectedCal);
                    intent.putExtra(BadBudgetApplication.PREDICT_ACCOUNT_ID, currDebt.name());
                    startActivityForResult(intent, BadBudgetApplication.PREDICT_REQUEST_CODE_UPDATED_DATES);
                }
            });

            TextView debtView = new TextView(this);
            valueViews.put(currDebt.name(), debtView);

            ((BadBudgetApplication)getApplication()).tableCellSetLayoutParams(debtView, "");

            TextView interestView = new TextView(this);
            interestViews.put(currDebt.name(), interestView);

            ((BadBudgetApplication)getApplication()).tableCellSetLayoutParams(interestView, "");

            //Setup the payment field - Leaving commented code in place 7/25/2017
            /*
            TextView paymentView = new TextView(this);
            String paymentString = getString(R.string.debt_no_payment);

            Payment payment = currDebt.payment();
            if (payment != null) {

                Frequency pFreq = payment.frequency();
                String freqString = " (" + BadBudgetApplication.shortHandFreq(pFreq) + ")";
                if (payment.payOff())
                {
                    paymentString = getString(R.string.dollar_sign) + getString(R.string.payoff_shorthand) + freqString;
                }
                else
                {
                    paymentString = getString(R.string.dollar_sign) + payment.amount() + freqString;
                }
            }
            ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(paymentView, paymentString);
            */

            TextView typeView = new TextView(this);
            String typeString = DEBT_TYPE_MISC;
            if (currDebt instanceof CreditCard)
            {
                typeString = DEBT_TYPE_CREDIT_CARD;
            }
            else if (currDebt instanceof Loan)
            {
                typeString = DEBT_TYPE_LOAN;
            }

            ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(typeView, typeString);
            ((BadBudgetApplication)getApplication()).tableAdjustBorderForLastColumn(typeView);

            TableRow.LayoutParams rowLayout = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLayout.gravity = Gravity.CENTER;
            row.addView(descriptionView);
            row.addView(debtView);
            row.addView(interestView);
            row.addView(typeView);

            table.addView(row);
        }

        addEmptyRow();
        addTotalRow();
        addInterestChangeRateRow(Frequency.monthly);

        updateHistoryViews();

        if (BuildConfig.FREE_VERSION)
        {
            FlavorSpecific.adRequest(this, R.layout.content_detailed_look_debts);
        }
    }

    /**
     * Updates the values for each row in our debts table to be the value of that debt on the
     * currently selected date. Also updates the interest to be the accumulated interest from the
     * current date to the selected date.
     *
     * Updates the total row values as well.
     */
    protected void updateHistoryViews() {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        int dayIndex = Prediction.numDaysBetween(((BadBudgetApplication)getApplication()).getToday(), currentChosenDay);
        for (MoneyOwed currDebt : bbd.getDebts()) {
            TextView debtView = valueViews.get(currDebt.name());
            String debtValueString = BadBudgetApplication.roundedDoubleBB(currDebt.getPredictData(dayIndex).value());
            debtView.setText(debtValueString);

            TextView interestView = interestViews.get(currDebt.name());
            String interestString = BadBudgetApplication.roundedDoubleBB(currDebt.getPredictData(dayIndex).getAccumulatedInterest());
            interestView.setText(interestString);
        }

        totalDebtView.setText(BadBudgetApplication.roundedDoubleBB(getDebtTotal()));
        totalInterestView.setText(BadBudgetApplication.roundedDoubleBB(getInterestTotal()));

        Frequency startFreq = Frequency.monthly;
        double interestChangeRate = Prediction.analyzeInterestChangeDebts(bbd, dayIndex, startFreq);
        String interestChangeRateString = BadBudgetApplication.constructAmountFreqString(interestChangeRate, startFreq);
        changeValueView.setText(interestChangeRateString);
    }

    /**
     * Private helper method that adds an empty row to our table after the current row.
     */
    private void addEmptyRow()
    {
        TableLayout table = (TableLayout) findViewById(R.id.dlDebtAccountsTable);
        TableRow row = new TableRow(this);

        TextView emptyView1 = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(emptyView1, "", R.drawable.emptyborder);
        TextView emptyView2 = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(emptyView2, "", R.drawable.emptyborder);
        TextView emptyView3 = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(emptyView3, "", R.drawable.emptyborder);
        TextView emptyView4 = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(emptyView4, "", R.drawable.emptyborder);

        row.addView(emptyView1);
        row.addView(emptyView2);
        row.addView(emptyView3);
        row.addView(emptyView4);

        table.addView(row);
    }

    /**
     * Private helper method that adds the total row to the table.
     */
    private void addTotalRow()
    {
        double debtTotal = getDebtTotal();
        double interestTotal = getInterestTotal();

        TableLayout table = (TableLayout) findViewById(R.id.dlDebtAccountsTable);
        TableRow row = new TableRow(this);

        TextView totalView = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(totalView, getString(R.string.table_total), R.drawable.bordertbl);

        TextView totalDebtView = new TextView(this);
        this.totalDebtView = totalDebtView;
        String totalDebtString = BadBudgetApplication.roundedDoubleBB(debtTotal);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(totalDebtView, totalDebtString, R.drawable.bordertbl);

        TextView totalInterestView = new TextView(this);
        this.totalInterestView = totalInterestView;
        String totalInterestString = BadBudgetApplication.roundedDoubleBB(interestTotal);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(totalInterestView, totalInterestString, R.drawable.bordertbl);

        TextView notAppView = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(notAppView, "", R.drawable.borderfull);

        row.addView(totalView);
        row.addView(totalDebtView);
        row.addView(totalInterestView);
        row.addView(notAppView);

        table.addView(row);
    }

    /**
     * Private helper method that adds the interest change rate row to the end of our table
     */
    private void addInterestChangeRateRow(Frequency freq)
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        int dayIndex = Prediction.numDaysBetween(((BadBudgetApplication)getApplication()).getToday(), currentChosenDay);

        double interestChangeRate = Prediction.analyzeInterestChangeDebts(bbd, dayIndex, freq);

        TableLayout table = (TableLayout) findViewById(R.id.dlDebtAccountsTable);
        TableRow row = new TableRow(this);

        TextView interestChangeRateView = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(interestChangeRateView, getString(R.string.interest_rate_change), R.drawable.emptyborder);

        final TextView changeValueView = new TextView(this);
        this.changeValueView = changeValueView;
        String interestChangeRateString = BadBudgetApplication.constructAmountFreqString(interestChangeRate, freq);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(changeValueView, interestChangeRateString, R.drawable.emptyborder);

        changeValueView.setPaintFlags(changeValueView.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);

        changeValueView.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v)
            {
                Frequency currentToggleFreq =
                        BadBudgetApplication.freqFromShortHand(BadBudgetApplication.extractShortHandFreq(changeValueView.getText().toString()));
                Frequency convertToggleFreq = BadBudgetApplication.getNextToggleFreq(currentToggleFreq);

                BadBudgetData bbd = ((BadBudgetApplication) DetailedLookDebtsTable.this.getApplication()).getBadBudgetUserData();
                int dayIndex = Prediction.numDaysBetween(((BadBudgetApplication)getApplication()).getToday(), currentChosenDay);

                double convertTotal = Prediction.analyzeInterestChangeDebts(bbd, dayIndex, convertToggleFreq);
                changeValueView.setText(BadBudgetApplication.constructAmountFreqString(convertTotal, convertToggleFreq));
            }

        });

        TextView emptyView1 = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(emptyView1, "", R.drawable.emptyborder);

        TextView emptyView2 = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(emptyView2, "", R.drawable.emptyborder);

        row.addView(interestChangeRateView);
        row.addView(emptyView1);
        row.addView(changeValueView);
        row.addView(emptyView2);
        table.addView(row);
    }

    /**
     * Private helper that returns the total value of the debts on the currentChosenDay.
     * @return total value of the accounts
     */
    private double getDebtTotal()
    {
        double total = 0;
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        int dayIndex = Prediction.numDaysBetween(((BadBudgetApplication)getApplication()).getToday(), currentChosenDay);
        for (MoneyOwed currDebt : bbd.getDebts()) {

            total+=currDebt.getPredictData(dayIndex).value();
        }
        return total;
    }

    /**
     * Private helper that returns the total accumulated interest of the debts on the currentChosenDay.
     * @return - the total accumulated interest
     */
    private double getInterestTotal()
    {
        double total = 0;
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        int dayIndex = Prediction.numDaysBetween(((BadBudgetApplication)getApplication()).getToday(), currentChosenDay);
        for (MoneyOwed currDebt : bbd.getDebts()) {

            total+=currDebt.getPredictData(dayIndex).getAccumulatedInterest();
        }
        return total;
    }

}
