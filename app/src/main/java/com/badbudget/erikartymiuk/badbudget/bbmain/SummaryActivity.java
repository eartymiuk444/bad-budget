package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.BuildConfig;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.inputforms.DateInputActivity;
import com.badbudget.erikartymiuk.badbudget.inputforms.DatePickerFragment;
import com.badbudget.erikartymiuk.bbmain.FlavorSpecific;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.Prediction;
import com.erikartymiuk.badbudgetlogic.main.Source;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

/**
 * Summary activity displays net gains, losses, contributions, and payments taking into account
 * the next dates and gives the user the option to chose a freq and date to chose to see the
 * summary data at. Also includes aggregate stats for the net (worth) flow, cash flow, and the
 * debt flow. Also displays the net money coming out from each source. Displays the flows in green
 * or red to indicate good or bad flows. Also the flows are clickable and display a popup dialog
 * giving more info. on how the flow was constructed and can be interpreted. TODO 6/4/2017
 * Created by Erik Artymiuk on 5/22/2017.
 */
public class SummaryActivity extends BadBudgetChildActivity implements AdapterView.OnItemSelectedListener
{
    /* Keys for saving the instance state */
    private static final String SELECTED_FREQ_KEY = "SELECTED_FREQ";
    private static final String SELECTED_FREQ_INDEX_KEY = "SELECTED_FREQ_INDEX";

    private Frequency currentSelectedFreq;

    /**
     * On create for the summary activity. Sets the content and creates and initializes our spinner.
     * Enables the date picker in the toolbar.
     * @param savedInstanceState - state if any to restore
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

        setContent(R.layout.content_summary);

        Spinner frequencySpinner = (Spinner)findViewById(R.id.summaryFrequencySpinner);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.summary_frequency_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        frequencySpinner.setAdapter(adapter);
        frequencySpinner.setOnItemSelectedListener(this);

        if (savedInstanceState != null)
        {
            frequencySpinner.setSelection(savedInstanceState.getInt(SELECTED_FREQ_INDEX_KEY));
            currentSelectedFreq = (Frequency)savedInstanceState.getSerializable(SELECTED_FREQ_KEY);
        }
        else
        {
            frequencySpinner.setSelection(getResources().getInteger(R.integer.monthly_summary_index));
            currentSelectedFreq = Frequency.monthly;
        }
        this.enableDatePickerToolbar();

        if (BuildConfig.FREE_VERSION)
        {
            FlavorSpecific.adRequest(this, R.layout.content_summary);
        }
    }

    /**
     * Called prior to this activity being destroyed, saves any necessary state to restore upon
     * the users return.
     * @param outState - where to store the saved state.
     */
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putSerializable(SELECTED_FREQ_KEY, currentSelectedFreq);

        Spinner frequencySpinner = (Spinner)findViewById(R.id.summaryFrequencySpinner);
        outState.putInt(SELECTED_FREQ_INDEX_KEY, frequencySpinner.getSelectedItemPosition());
    }

    /**
     * Method called when the update task completes. Since an update shouldn't impact the values of our summary
     * fields (unless the chosen date is now before today's date, in which case this is covered by the setCurrentChosenDay method)
     * this method doesn't need to take any action beyond calling super.
     * @param updated - true if an update to bb objects occurred false otherwise, unused
     */
    public void updateTaskCompleted(boolean updated)
    {
        super.updateTaskCompleted(updated);
    }

    @Override
    /**
     * Method called when an item is selected in our frequency spinner. Updates the current selected
     * frequency and updates all the summary fields to reflect a newly chosen frequency.
     * @param parent - unused
     * @param position - the selected items position
     * @param id - unused
     */
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        String selectedFrequency = (String) parent.getItemAtPosition(position);

        if (this.getResources().getString(R.string.daily).equals(selectedFrequency))
        {
            currentSelectedFreq = Frequency.daily;
        }
        else if (this.getResources().getString(R.string.weekly).equals(selectedFrequency))
        {
            currentSelectedFreq = Frequency.weekly;
        }
        else if (this.getResources().getString(R.string.bi_weekly).equals(selectedFrequency))
        {
            currentSelectedFreq = Frequency.biWeekly;
        }
        else if (this.getResources().getString(R.string.monthly).equals(selectedFrequency))
        {
            currentSelectedFreq = Frequency.monthly;
        }
        else if (this.getResources().getString(R.string.yearly).equals(selectedFrequency))
        {
            currentSelectedFreq = Frequency.yearly;
        }

        BadBudgetApplication application = ((BadBudgetApplication)this.getApplication());
        populateSummary(application.getBadBudgetUserData(), currentSelectedFreq, this.getCurrentChosenDay());
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**
     * Calculates and sets all of our summary fields.
     * @param bbd - the current application bad budget data
     * @param frequency - the frequency to use when populating our summary fields
     * @param chosenDate - the chosen date to use when populating our summary fields
     */
    private void populateSummary(BadBudgetData bbd, Frequency frequency, Date chosenDate)
    {
        //Calculate our limit date
        Calendar limitCal = Calendar.getInstance();
        limitCal.setTime(((BadBudgetApplication)this.getApplication()).getToday());
        limitCal.add(Calendar.YEAR, getResources().getInteger(R.integer.predict_years));

        TextView gainAmountView = (TextView)findViewById(R.id.summaryGainAmount);
        double gainAmount = Prediction.analyzeNetGainAtFreq(bbd, frequency, chosenDate);
        gainAmountView.setText(BadBudgetApplication.roundedDoubleBB(gainAmount));

        TextView lossAmountView = (TextView)findViewById(R.id.summaryLossAmount);
        double lossAmount = Prediction.analyzeNetLossAtFreq(bbd, frequency, chosenDate);
        lossAmountView.setText(BadBudgetApplication.roundedDoubleBB(lossAmount));

        TextView contributionAmountView = (TextView)findViewById(R.id.summaryContributionAmount);
        double contributionAmount = Prediction.analyzeNetContributionsAtFreq(bbd, frequency, chosenDate);
        contributionAmountView.setText(BadBudgetApplication.roundedDoubleBB(contributionAmount));

        TextView paymentAmountView = (TextView)findViewById(R.id.summaryPaymentAmount);
        BadBudgetApplication application = (BadBudgetApplication)this.getApplication();
        double paymentAmount = Prediction.analyzeNetPaymentsAtFreq(bbd, frequency, chosenDate, application.getToday(), limitCal.getTime());
        paymentAmountView.setText(BadBudgetApplication.roundedDoubleBB(paymentAmount));

        TextView netFlowAmountView = (TextView)findViewById(R.id.summaryNetFlowAmount);
        double netFlowAmount = Prediction.analyzeGainsLosses(bbd, frequency, chosenDate);
        if (netFlowAmount > 0)
        {
            netFlowAmountView.setTextColor(ContextCompat.getColor(this.getApplicationContext(), R.color.colorPrimary));
        }
        else
        {
            netFlowAmountView.setTextColor(ContextCompat.getColor(this.getApplicationContext(), R.color.deleteButtonColor));
        }
        netFlowAmountView.setText(BadBudgetApplication.roundedDoubleBB(netFlowAmount));

        TextView cashFlowAmountView = (TextView)findViewById(R.id.summaryCashFlowAmount);
        double cashFlowAmount = Prediction.analyzeCashFlow(bbd, frequency, chosenDate, application.getToday(), limitCal.getTime());
        if (cashFlowAmount > 0)
        {
            cashFlowAmountView.setTextColor(ContextCompat.getColor(this.getApplicationContext(), R.color.colorPrimary));
        }
        else
        {
            cashFlowAmountView.setTextColor(ContextCompat.getColor(this.getApplicationContext(), R.color.deleteButtonColor));
        }
        cashFlowAmountView.setText(BadBudgetApplication.roundedDoubleBB(cashFlowAmount));

        TextView debtFlowAmountView = (TextView)findViewById(R.id.summaryDebtFlowAmount);
        double debtFlowAmount = Prediction.analyzeDebtFlow(bbd, frequency, chosenDate, application.getToday(), limitCal.getTime());
        if (debtFlowAmount < 0)
        {
            debtFlowAmountView.setTextColor(ContextCompat.getColor(this.getApplicationContext(), R.color.colorPrimary));
        }
        else
        {
            debtFlowAmountView.setTextColor(ContextCompat.getColor(this.getApplicationContext(), R.color.deleteButtonColor));
        }
        debtFlowAmountView.setText(BadBudgetApplication.roundedDoubleBB(debtFlowAmount));

        LinearLayout sourcesMoneyOut = (LinearLayout)findViewById(R.id.summarySourcesMoneyOut);
        View titleRow = sourcesMoneyOut.getChildAt(0);
        HashMap<Source, Double> map = Prediction.analyzeSourceMoneyOut(bbd, frequency, chosenDate, application.getToday(), limitCal.getTime());
        sourcesMoneyOut.removeAllViews();
        sourcesMoneyOut.addView(titleRow);

        for (Source s : map.keySet())
        {
            LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout singleRow = (LinearLayout) inflater.inflate(R.layout.summary_single_row, null);

            TextView first = (TextView)singleRow.findViewById(R.id.summarySingleFirst);
            TextView second = (TextView)singleRow.findViewById(R.id.summarySingleSecond);

            first.setText(s.name());
            second.setText(BadBudgetApplication.roundedDoubleBB(map.get(s)));
            sourcesMoneyOut.addView(singleRow);
        }
    }

    /**
     * Overridden method, that is called whenever the chosen date is set or changes. Calls super
     * and repopulates our summary fields using the current frequency and the newly selected chosen
     * date.
     * @param date - the date to set our current chosen day to.
     */
    public void setCurrentChosenDay(Date date)
    {
        super.setCurrentChosenDay(date);
        BadBudgetApplication application = ((BadBudgetApplication)this.getApplication());
        populateSummary(application.getBadBudgetUserData(), currentSelectedFreq, date);
    }
}
