package com.badbudget.erikartymiuk.badbudget.viewobjecttables;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetChildActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetTableActivity;
import com.badbudget.erikartymiuk.badbudget.inputforms.AddGainActivity;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.MoneyGain;
import com.erikartymiuk.badbudgetlogic.main.MoneyLoss;
import com.erikartymiuk.badbudgetlogic.main.Prediction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Activity for loading and populating a table displaying all of a user's gain objects. Modeled
 * after the table for accounts. The four fields in the table are "source description", "value", "frequency" and "destination".
 *
 */
public class GainsActivity extends BadBudgetTableActivity {

    private HashMap<String, TextView> valueViews;
    private HashMap<String, TextView> frequencyViews;
    private HashMap<String, TextView> destinationViews;

    /* Private instance variable used to map a view that was clicked on to the associated gain */
    private HashMap<View, MoneyGain> clickedGainsMap;

    private TextView totalAmountView;
    private TextView totalFreqView;

    /**
     * On create for a gain activity. Populates our table with the current bad budget data
     *
     * @param savedInstanceState - unused, tables should have no state to save or restore
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

        setContent(R.layout.content_gains);
        populateTable(savedInstanceState);
    }

    /**
     * Method called after the update task has completed. Refreshes the necessary table views
     * that may have changed (i.e. the value views).
     * @param updated - true if an update to bb objects occurred false otherwise, unused
     */
    public void updateTaskCompleted(boolean updated)
    {
        refreshTableForUpdate();
    }

    /**
     * Method called upon completion of the add gain activity form which this activity started.
     * Updates our gains table. The specific method is determined by the result code. It may update
     * only the necessary values that have changed or it may clear and repopulate the entire table.
     * @param requestCode - should be FORM_REQUEST_RESULT
     * @param resultCode - indicates what action the user took in the form and determines how we update
     *                      our table.
     * @param data - unused
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == BadBudgetApplication.FORM_RESULT_REQUEST) {
            switch (resultCode) {
                case BadBudgetApplication.FORM_RESULT_ADD: {
                    repopulateTableForAdd();
                    break;
                }
                case BadBudgetApplication.FORM_RESULT_EDIT: {
                    refreshTableForEdit();
                    break;
                }
                case BadBudgetApplication.FORM_RESULT_DELETE: {
                    repopulateTableForDelete();
                    break;
                }
                default: {
                    //Up, Back, Cancel
                    break;
                }
            }
        }
    }

    /**
     * Private helper method to be called when the update task has completed. Nothing is done here.
     */
    private void refreshTableForUpdate()
    {
        //Was updating the values but don't believe this is necessary, as like budget items
        //a gain amount will stay the same unless changed by the user (later on it might be the
        //case that it can increase/decrease by a percentage but not now...)
        /*
        /*
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        for (MoneyGain gain : bbd.getGains())
        {
            TextView valueView = valueViews.get(gain.sourceDescription());
            valueView.setText(BadBudgetApplication.roundedDoubleBB(gain.gainAmount()));
        }
        */
    }

    /**
     * Private helper method to be called when the form returns a result indicating an item
     * was edited. Updates the amount views and the quicklook views.
     */
    private void refreshTableForEdit()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        for (MoneyGain gain : bbd.getGains())
        {
            TextView valueView = valueViews.get(gain.sourceDescription());
            TextView frequencyView = frequencyViews.get(gain.sourceDescription());
            TextView destinationView = destinationViews.get(gain.sourceDescription());

            valueView.setText(BadBudgetApplication.roundedDoubleBB(gain.gainAmount()));
            frequencyView.setText(BadBudgetApplication.shortHandFreq(gain.gainFrequency()));
            destinationView.setText(gain.destinationAccount().name());
        }

        double total = getGainTotal(BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY);
        this.totalAmountView.setText(BadBudgetApplication.roundedDoubleBB(total));
        this.totalFreqView.setText(BadBudgetApplication.shortHandFreq(BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY));
    }

    /**
     * Private helper method to be called when the form returns a result indicating an item
     * was added. Clears and populates the table.
     */
    private void repopulateTableForAdd()
    {
        clearTable();
        populateTable(null);
    }

    /**
     * Private helper method to be called when the form returns a result indicating an item
     * was deleted. Clears and populates the table.
     */
    private void repopulateTableForDelete()
    {
        clearTable();
        populateTable(null);
    }

    /**
     * Private helper method that clears the gains table of all rows except the header
     * row.
     */
    private void clearTable()
    {
        TableLayout table = (TableLayout) findViewById(R.id.gainsTable);
        table.removeViews(1, table.getChildCount() - 1);
    }

    /**
     * Private helper method that repopulates our gains table putting the gains
     * in a sorted order, first by freq then alphabetically.
     */
    private void populateTable(Bundle savedInstanceState)
    {
        clickedGainsMap = new HashMap<View, MoneyGain>();

        valueViews = new HashMap<String, TextView>();
        frequencyViews = new HashMap<String, TextView>();
        destinationViews = new HashMap<String, TextView>();

        /* Use the bad budget application wide data object to get a hold of all the user's gains */
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        List<MoneyGain> gains = bbd.getGains();
        gains = GainsActivity.sortGains(gains);

        TableLayout table = (TableLayout) findViewById(R.id.gainsTable);

        /* For each gain we setup a row in our table with the name/description, value/amount,
        frequency, and destination fields.
         */
        for (final MoneyGain gain : gains) {

            final MoneyGain currGain = gain;

            TableRow row = new TableRow(this);

            //Setup the name/description field
            final TextView descriptionView = new TextView(this);
            ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(descriptionView, currGain.sourceDescription());
            descriptionView.setPaintFlags(descriptionView.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);


            /*
            The description when clicked should take us to a page where the user can edit the
            gain that they clicked on.
             */
            clickedGainsMap.put(descriptionView, currGain);
            descriptionView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v)
                {
                    MoneyGain clickedGain = clickedGainsMap.get(v);

                    Intent intent = new Intent(GainsActivity.this, AddGainActivity.class);
                    intent.putExtra(BadBudgetApplication.EDIT_KEY, true);
                    intent.putExtra(BadBudgetApplication.EDIT_OBJECT_ID_KEY, clickedGain.sourceDescription());
                    startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
                }
            });

            //Setup the value field
            final TextView gainAmountView = new TextView(this);
            valueViews.put(currGain.sourceDescription(), gainAmountView);
            double gainValue = currGain.gainAmount();
            String gainValueString = BadBudgetApplication.roundedDoubleBB(gainValue);
            ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(gainAmountView, gainValueString);

            //Setup the frequency field
            final TextView frequencyView = new TextView(this);
            frequencyView.setPaintFlags(frequencyView.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);
            frequencyViews.put(currGain.sourceDescription(), frequencyView);

            if (currGain.gainFrequency() != Frequency.oneTime) {
                frequencyView.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View v) {
                        Frequency currentToggleFreq = BadBudgetApplication.freqFromShortHand(frequencyView.getText().toString());
                        Frequency convertToggleFreq = BadBudgetApplication.getNextToggleFreq(currentToggleFreq);
                        double toggleAmount = Prediction.toggle(currGain.gainAmount(), currGain.gainFrequency(), convertToggleFreq);

                        frequencyView.setText(BadBudgetApplication.shortHandFreq(convertToggleFreq));
                        gainAmountView.setText(BadBudgetApplication.roundedDoubleBB(toggleAmount));
                        if (convertToggleFreq != currGain.gainFrequency()) {
                            gainAmountView.setText(gainAmountView.getText() + " " + BadBudgetApplication.TEMP_FREQUENCY_AMOUNT_MESSAGE);
                        }
                    }

                });
            }
            String frequencyString;
            if (savedInstanceState == null)
            {
                frequencyString = BadBudgetApplication.shortHandFreq(currGain.gainFrequency());
            }
            else
            {
                Frequency savedFreq = (Frequency) savedInstanceState.getSerializable(BadBudgetApplication.TOGGLED_FREQUENCY_PREFIX_KEY + currGain.sourceDescription());
                if (savedFreq != null) {
                    frequencyString = BadBudgetApplication.shortHandFreq(savedFreq);
                    if (savedFreq != currGain.gainFrequency()) {
                        double toggleAmount = Prediction.toggle(currGain.gainAmount(), currGain.gainFrequency(), savedFreq);
                        gainAmountView.setText(BadBudgetApplication.roundedDoubleBB(toggleAmount) + " " + BadBudgetApplication.TEMP_FREQUENCY_AMOUNT_MESSAGE);
                    }
                }
                else
                {
                    frequencyString = BadBudgetApplication.shortHandFreq(currGain.gainFrequency());
                }
            }
            ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(frequencyView, frequencyString);

            //Setup the destination field.
            TextView destinationView = new TextView(this);
            destinationViews.put(currGain.sourceDescription(), destinationView);

            ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(destinationView, currGain.destinationAccount().name());
            ((BadBudgetApplication)this.getApplication()).tableAdjustBorderForLastColumn(destinationView);

            row.addView(descriptionView);
            row.addView(gainAmountView);
            row.addView(frequencyView);
            row.addView(destinationView);

            table.addView(row);
        }

        //Add in the total row with toggable frequency
        if (savedInstanceState == null)
        {
            addEmptyRow();
            addTotalRow(BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY);
        }
        else
        {
            addEmptyRow();
            addTotalRow((Frequency)savedInstanceState.getSerializable(BadBudgetApplication.TOTAL_FREQ_KEY));

        }
    }

    /**
     * Private static helper method to sort a collection of money gains. First sorts items by
     * frequency (daily->yearly)
     * and then sorts alphabetically.
     * @param unsortedGains - a collection of gains to sort
     * @return a list of gains sorted first on freq then alphabetically
     */
    private static List<MoneyGain> sortGains(Collection<MoneyGain> unsortedGains)
    {
        //Sort budget items first on frequency and then alphabetically
        ArrayList<MoneyGain> oneTimeItems = new ArrayList<MoneyGain>();
        ArrayList<MoneyGain> dailyItems = new ArrayList<MoneyGain>();
        ArrayList<MoneyGain> weeklyItems = new ArrayList<MoneyGain>();
        ArrayList<MoneyGain> biweeklyItems = new ArrayList<MoneyGain>();
        ArrayList<MoneyGain> monthlyItems = new ArrayList<MoneyGain>();
        ArrayList<MoneyGain> yearlyItems = new ArrayList<MoneyGain>();

        for (MoneyGain gain : unsortedGains)
        {
            switch (gain.gainFrequency())
            {
                case oneTime:
                {
                    oneTimeItems.add(gain);
                    break;
                }
                case daily:
                {
                    dailyItems.add(gain);
                    break;
                }
                case weekly:
                {
                    weeklyItems.add(gain);
                    break;
                }
                case biWeekly:
                {
                    biweeklyItems.add(gain);
                    break;
                }
                case monthly:
                {
                    monthlyItems.add(gain);
                    break;
                }
                case yearly:
                {
                    yearlyItems.add(gain);
                    break;
                }
            }
        }

        Comparator<MoneyGain> comparator = new Comparator<MoneyGain>() {
            @Override
            public int compare(MoneyGain lhs, MoneyGain rhs) {
                return lhs.sourceDescription().compareTo(rhs.sourceDescription());
            }
        };

        Collections.sort(oneTimeItems, comparator);
        Collections.sort(dailyItems, comparator);
        Collections.sort(weeklyItems, comparator);
        Collections.sort(biweeklyItems, comparator);
        Collections.sort(monthlyItems, comparator);
        Collections.sort(yearlyItems, comparator);

        ArrayList<MoneyGain> sortedItems = new ArrayList<MoneyGain>();
        BadBudgetApplication.appendItems(sortedItems, oneTimeItems);
        BadBudgetApplication.appendItems(sortedItems, dailyItems);
        BadBudgetApplication.appendItems(sortedItems, weeklyItems);
        BadBudgetApplication.appendItems(sortedItems, biweeklyItems);
        BadBudgetApplication.appendItems(sortedItems, monthlyItems);
        BadBudgetApplication.appendItems(sortedItems, yearlyItems);

        return sortedItems;
    }

    /**
     * Method called when the add gain button is clicked. Starts the add gain
     * activity.
     * @param view
     */
    public void addGainClick(View view)
    {
        Intent intent = new Intent(this, AddGainActivity.class);
        intent.putExtra(BadBudgetApplication.EDIT_KEY, false);
        startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
    }

    /**
     * Called prior to this activity being destroyed, saves any necessary state to restore upon
     * the users return.
     * @param outState - where to store the saved state.
     */
    protected void onSaveInstanceState(Bundle outState)
    {
         /* Use the bad budget application wide data object to get a hold of all the user's losses */
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        List<MoneyGain> gains = bbd.getGains();

        for (MoneyGain currGain : gains)
        {
            String currFreqShortHand = frequencyViews.get(currGain.sourceDescription()).getText().toString();
            Frequency currFrequency = BadBudgetApplication.freqFromShortHand(currFreqShortHand);
            outState.putSerializable(BadBudgetApplication.TOGGLED_FREQUENCY_PREFIX_KEY + currGain.sourceDescription(), currFrequency);
        }

        //Keep track of what the total freq's was toggled to
        Frequency totalFreq = BadBudgetApplication.freqFromShortHand(this.totalFreqView.getText().toString());
        outState.putSerializable(BadBudgetApplication.TOTAL_FREQ_KEY, totalFreq);
    }

    /**
     * Private helper method that adds an empty row to our table after the current row.
     */
    private void addEmptyRow()
    {
        TableLayout table = (TableLayout) findViewById(R.id.gainsTable);
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
     * Private helper method that adds the total row to the table. The row uses the passed
     * frequency as the initial frequency for our total. The freq of the total row is also
     * able to be toggled.
     * @param totalRowFreq - the initial frequency of our total row
     */
    private void addTotalRow(Frequency totalRowFreq)
    {
        double total = getGainTotal(totalRowFreq);

        TableLayout table = (TableLayout) findViewById(R.id.gainsTable);
        TableRow row = new TableRow(this);

        TextView totalView = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(totalView, getString(R.string.table_total), R.drawable.bordertbl);

        final TextView totalAmountView = new TextView(this);
        this.totalAmountView = totalAmountView;
        String totalAmountString = BadBudgetApplication.roundedDoubleBB(total);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(totalAmountView, totalAmountString, R.drawable.bordertbl);

        final TextView frequencyView = new TextView(this);
        this.totalFreqView = frequencyView;

        frequencyView.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v)
            {
                Frequency currentToggleFreq = BadBudgetApplication.freqFromShortHand(frequencyView.getText().toString());
                Frequency convertToggleFreq = BadBudgetApplication.getNextToggleFreq(currentToggleFreq);
                double convertTotal = getGainTotal(convertToggleFreq);
                frequencyView.setText(BadBudgetApplication.shortHandFreq(convertToggleFreq));
                totalAmountView.setText(BadBudgetApplication.roundedDoubleBB(convertTotal));
            }

        });

        String frequencyString = BadBudgetApplication.shortHandFreq(totalRowFreq);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(frequencyView, frequencyString, R.drawable.bordertbl);
        frequencyView.setPaintFlags(frequencyView.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);

        TextView notAppView = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(notAppView, "", R.drawable.borderfull);

        row.addView(totalView);
        row.addView(totalAmountView);
        row.addView(frequencyView);
        row.addView(notAppView);

        table.addView(row);
    }

    /**
     * Private helper method that computes the total value at the given frequency for gains
     * //TODO 3/22/2017 - may want to include interest
     * @param totalRowFreq - the frequency to use for the total
     * @return the total at the given freq of all gains
     */
    private double getGainTotal(Frequency totalRowFreq)
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        List<MoneyGain> gains = bbd.getGains();
        double total = 0;
        for (MoneyGain currGain : gains)
        {
            //Exclude OT frequencies
            if (currGain.gainFrequency() != Frequency.oneTime)
            {
                total += Prediction.toggle(currGain.gainAmount(), currGain.gainFrequency(), totalRowFreq);
            }
        }

        return total;
    }
}
