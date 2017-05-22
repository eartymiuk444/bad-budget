package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.BuildConfig;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.bbmain.FlavorSpecific;
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.Prediction;

/**
 * Created by Erik Artymiuk on 5/22/2017.
 */
public class SummaryActivity extends BadBudgetChildActivity implements AdapterView.OnItemSelectedListener
{
    /**
     * On create for this activity.
     * @param savedInstanceState -
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContent(R.layout.content_summary);

        Spinner frequencySpinner = (Spinner)findViewById(R.id.summaryFrequencySpinner);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.add_budget_item_frequency_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        frequencySpinner.setAdapter(adapter);
        frequencySpinner.setOnItemSelectedListener(this);
        frequencySpinner.setSelection(0);

        BadBudgetApplication application = ((BadBudgetApplication)this.getApplication());

        TextView gainAmountView = (TextView)findViewById(R.id.summaryGainAmount);
        double gainAmount = Prediction.analyzeNetGainAtFreq(application.getBadBudgetUserData(), Frequency.monthly, application.getToday());
        gainAmountView.setText(BadBudgetApplication.roundedDoubleBB(gainAmount));

        TextView lossAmountView = (TextView)findViewById(R.id.summaryLossAmount);
        double lossAmount = Prediction.analyzeNetLossAtFreq(application.getBadBudgetUserData(), Frequency.monthly, application.getToday());
        lossAmountView.setText(BadBudgetApplication.roundedDoubleBB(lossAmount));

        TextView gainLossAmountView = (TextView)findViewById(R.id.summaryGainLossAmount);
        double gainLossAmount = Prediction.analyzeGainsLosses(application.getBadBudgetUserData(), Frequency.monthly, application.getToday());
        gainLossAmountView.setText(BadBudgetApplication.roundedDoubleBB(gainLossAmount));

        TextView contributionAmountView = (TextView)findViewById(R.id.summaryContributionAmount);
        double contributionAmount = Prediction.analyzeNetContributionsAtFreq(application.getBadBudgetUserData(), Frequency.monthly, application.getToday());
        contributionAmountView.setText(BadBudgetApplication.roundedDoubleBB(contributionAmount));

        TextView paymentAmountView = (TextView)findViewById(R.id.summaryPaymentAmount);
        double paymentAmount = Prediction.analyzeNetPaymentsAtFreq(application.getBadBudgetUserData(), Frequency.monthly, application.getToday());
        paymentAmountView.setText(BadBudgetApplication.roundedDoubleBB(paymentAmount));

        TextView moneyOutAmountView = (TextView)findViewById(R.id.summaryMoneyOutAmount);
        double moneyOutAmount = Prediction.analyzeLossesPaymentsContributions(application.getBadBudgetUserData(), Frequency.monthly, application.getToday());
        moneyOutAmountView.setText(BadBudgetApplication.roundedDoubleBB(moneyOutAmount));

        TextView gainOutAmountView = (TextView)findViewById(R.id.summaryGainOutAmount);
        double gainOutAmount = Prediction.analyzeGainsLossesPaymentsContributions(application.getBadBudgetUserData(), Frequency.monthly, application.getToday());
        gainOutAmountView.setText(BadBudgetApplication.roundedDoubleBB(gainOutAmount));

        if (BuildConfig.FREE_VERSION)
        {
            FlavorSpecific.adRequest(this, R.layout.content_summary);
        }
    }

    /**
     * Method called when the update task completes. Just returns
     * @param updated - true if an update to bb objects occurred false otherwise, unused
     */
    public void updateTaskCompleted(boolean updated)
    {

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
