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
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.CreditCard;
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.Loan;
import com.erikartymiuk.badbudgetlogic.main.MoneyOwed;
import com.erikartymiuk.badbudgetlogic.main.Payment;
import com.erikartymiuk.badbudgetlogic.main.Prediction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Detailed Look Debts Table activity. Displays all debts in a single table with the value on
 * the currently selected day.
 */
public class DetailedLookDebtsTable extends DetailedLookBaseActivity {

    private HashMap<String, TextView> valueViews;

    public static final String DEBT_TYPE_CREDIT_CARD = "cc";
    public static final String DEBT_TYPE_LOAN = "loan";
    public static final String DEBT_TYPE_MISC = "misc";

    /**
     * On create for the dl debts table activity. Adds the content for the dl debts table to the base
     * and sets up the cell entries for each debt row on the currently selected day by calling update
     * history views.
     * @param savedInstanceState - unused
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.detailed_look_base_linear_layout);
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.content_detailed_look_debts, linearLayout, true);

        valueViews = new HashMap<>();

        TableLayout table = (TableLayout) findViewById(R.id.dlDebtAccountsTable);
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        ArrayList<MoneyOwed> debts = bbd.getDebts();

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

            //Setup the payment field
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
            row.addView(paymentView);
            row.addView(typeView);

            table.addView(row);
        }
        updateHistoryViews();

        if (BuildConfig.FREE_VERSION)
        {
            FlavorSpecific.adRequest(this, R.layout.content_detailed_look_debts);
        }
    }

    /**
     * Updates the values for each row in our debts table to be the value of that debt on the
     * currently selected date.
     */
    protected void updateHistoryViews() {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        int dayIndex = Prediction.numDaysBetween(((BadBudgetApplication)getApplication()).getToday(), currentChosenDay);
        for (MoneyOwed currDebt : bbd.getDebts()) {
            TextView debtView = valueViews.get(currDebt.name());
            String debtValueString = BadBudgetApplication.roundedDoubleBB(currDebt.getPredictData(dayIndex).value());
            debtView.setText(debtValueString);
        }
    }

}
