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
import com.badbudget.erikartymiuk.badbudget.inputforms.AddLossActivity;
import com.erikartymiuk.badbudgetlogic.budget.BudgetItem;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.MoneyGain;
import com.erikartymiuk.badbudgetlogic.main.MoneyLoss;
import com.erikartymiuk.badbudgetlogic.main.Prediction;
import com.erikartymiuk.badbudgetlogic.main.Source;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Activity for loading and populating a table displaying all of a user's loss objects. Modeled
 * after the table for accounts. The four fields in the table are "Description", "value", "frequency" and "source".
 *
 */
public class LossesActivity extends BadBudgetTableActivity {

    private HashMap<String, TextView> valueViews;
    private HashMap<String, TextView> frequencyViews;
    private HashMap<String, TextView> nextLossViews;

    private TextView totalAmountView;
    private TextView totalFreqView;

    private TextView budgetAmountView;
    private TextView budgetFreqView;

    /* Private instance variable used to map a view that was clicked on to the associated loss */
    private HashMap<View, MoneyLoss> clickedLossesMap;

    /**
     * On create for the loss table activity. Populates our table with the current bad budget data
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

        setContent(R.layout.content_losses);
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
     * Method called upon completion of the add loss activity form which this activity started.
     * Updates our losses table. The specific method is determined by the result code. It may update
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
     * Private helper method to be called when the update task has completed. Nothing to be done
     * for losses
     */
    private void refreshTableForUpdate()
    {
        //Was updating the values but don't believe this is necessary, as like budget items
        //a loss amount will stay the same unless changed by the user (later on it might be the
        //case that it can increase/decrease by a percentage but not now...)
        /*
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        for (MoneyLoss loss : bbd.getLosses())
        {
            TextView valueView = valueViews.get(loss.expenseDescription());
            valueView.setText(BadBudgetApplication.roundedDoubleBB(loss.lossAmount()));
        }
        */
    }

    /**
     * Private helper method to be called when the form returns a result indicating an item
     * was edited. Updates the amount views and the quicklook views. Also updates the total view
     */
    private void refreshTableForEdit()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        for (MoneyLoss loss : bbd.getLosses())
        {
            TextView valueView = valueViews.get(loss.expenseDescription());
            TextView frequencyView = frequencyViews.get(loss.expenseDescription());
            TextView nextLossView = nextLossViews.get(loss.expenseDescription());

            valueView.setText(BadBudgetApplication.roundedDoubleBB(loss.lossAmount()));
            frequencyView.setText(BadBudgetApplication.shortHandFreq(loss.lossFrequency()));
            nextLossView.setText(BadBudgetApplication.dateString(loss.nextLoss()));
        }

        double total = getLossTotal(BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY);
        this.totalAmountView.setText(BadBudgetApplication.roundedDoubleBB(total));
        this.totalFreqView.setText(BadBudgetApplication.shortHandFreq(BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY));

        //Should also reset the budget row to the default freq
        double budgetTotal = BudgetSetActivity.getBudgetItemTotal(this, BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY);
        this.budgetAmountView.setText(BadBudgetApplication.roundedDoubleBB(budgetTotal));
        this.budgetFreqView.setText(BadBudgetApplication.shortHandFreq(BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY));
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
     * Private helper method that clears the losses table of all rows except the header
     * row.
     */
    private void clearTable()
    {
        TableLayout table = (TableLayout) findViewById(R.id.lossesTable);
        table.removeViews(1, table.getChildCount() - 1);
    }

    /**
     * Private helper that populates our losses table first clearing the table and then adding
     * a row for each loss currently in our bad budget data.
     */
    private void populateTable(Bundle savedInstanceState)
    {
        valueViews = new HashMap<String, TextView>();
        frequencyViews = new HashMap<String, TextView>();
        nextLossViews = new HashMap<String, TextView>();

        clickedLossesMap = new HashMap<View, MoneyLoss>();

        /* Use the bad budget application wide data object to get a hold of all the user's losses */
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        List<MoneyLoss> losses = bbd.getLosses();
        losses = LossesActivity.sortLosses(losses);

        TableLayout table = (TableLayout) findViewById(R.id.lossesTable);

        /* For each loss we setup a row in our table with the name/description, value/amount,
        frequency, and destination fields.
         */
        for (final MoneyLoss loss : losses) {

            final MoneyLoss currLoss = loss;

            TableRow row = new TableRow(this);

            //Setup the name/description field
            final TextView descriptionView = new TextView(this);
            ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(descriptionView, currLoss.expenseDescription());
            descriptionView.setPaintFlags(descriptionView.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);

            /*
            The description when clicked should take us to a page where the user can edit the
            loss that they clicked on.
             */
            clickedLossesMap.put(descriptionView, currLoss);
            descriptionView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v)
                {
                    MoneyLoss clickedLoss = clickedLossesMap.get(v);

                    Intent intent = new Intent(LossesActivity.this, AddLossActivity.class);
                    intent.putExtra(BadBudgetApplication.EDIT_KEY, true);
                    intent.putExtra(BadBudgetApplication.EDIT_OBJECT_ID_KEY, clickedLoss.expenseDescription());
                    startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
                }
            });

            //Setup the value field
            final TextView lossAmountView = new TextView(this);
            valueViews.put(currLoss.expenseDescription(), lossAmountView);
            double lossValue = currLoss.lossAmount();
            String lossValueString = BadBudgetApplication.roundedDoubleBB(lossValue);
            ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(lossAmountView, lossValueString);

            //Setup the frequency field
            final TextView frequencyView = new TextView(this);
            frequencyView.setPaintFlags(frequencyView.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);
            frequencyViews.put(currLoss.expenseDescription(), frequencyView);

            if (currLoss.lossFrequency() != Frequency.oneTime) {
                frequencyView.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View v) {
                        Frequency currentToggleFreq = BadBudgetApplication.freqFromShortHand(frequencyView.getText().toString());
                        Frequency convertToggleFreq = BadBudgetApplication.getNextToggleFreq(currentToggleFreq);
                        double toggleAmount = Prediction.toggle(currLoss.lossAmount(), currLoss.lossFrequency(), convertToggleFreq);

                        frequencyView.setText(BadBudgetApplication.shortHandFreq(convertToggleFreq));
                        lossAmountView.setText(BadBudgetApplication.roundedDoubleBB(toggleAmount));
                        if (convertToggleFreq != currLoss.lossFrequency()) {
                            lossAmountView.setText(lossAmountView.getText() + " " + BadBudgetApplication.TEMP_FREQUENCY_AMOUNT_MESSAGE);
                        }
                    }

                });
            }
            String frequencyString;
            if (savedInstanceState == null)
            {
                frequencyString = BadBudgetApplication.shortHandFreq(currLoss.lossFrequency());
            }
            else
            {
                Frequency savedFreq = (Frequency) savedInstanceState.getSerializable(BadBudgetApplication.TOGGLED_FREQUENCY_PREFIX_KEY + currLoss.expenseDescription());
                if (savedFreq != null) {
                    frequencyString = BadBudgetApplication.shortHandFreq(savedFreq);
                    if (savedFreq != currLoss.lossFrequency()) {
                        double toggleAmount = Prediction.toggle(currLoss.lossAmount(), currLoss.lossFrequency(), savedFreq);
                        lossAmountView.setText(BadBudgetApplication.roundedDoubleBB(toggleAmount) + " " + BadBudgetApplication.TEMP_FREQUENCY_AMOUNT_MESSAGE);
                    }
                }
                else
                {
                    frequencyString = BadBudgetApplication.shortHandFreq(currLoss.lossFrequency());
                }
            }
            ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(frequencyView, frequencyString);

            //Setup the next loss field.
            TextView nextLossView = new TextView(this);
            nextLossViews.put(currLoss.expenseDescription(), nextLossView);

            String nextLossString = BadBudgetApplication.dateString(currLoss.nextLoss());

            ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(nextLossView, nextLossString);
            ((BadBudgetApplication)this.getApplication()).tableAdjustBorderForLastColumn(nextLossView);

            row.addView(descriptionView);
            row.addView(lossAmountView);
            row.addView(frequencyView);
            row.addView(nextLossView);

            table.addView(row);
        }

        //Add in the total row and budget row with a toggable frequencies
        if (savedInstanceState == null)
        {
            addBudgetRow(BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY);
            addEmptyRow();
            addTotalRow(BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY);
        }
        else
        {
            addBudgetRow((Frequency)savedInstanceState.getSerializable(BadBudgetApplication.BUDGET_TOTAL_FREQ_KEY));
            addEmptyRow();
            addTotalRow((Frequency)savedInstanceState.getSerializable(BadBudgetApplication.TOTAL_FREQ_KEY));
        }
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
        List<MoneyLoss> losses = bbd.getLosses();

        for (MoneyLoss currLoss : losses)
        {
            String currFreqShortHand = frequencyViews.get(currLoss.expenseDescription()).getText().toString();
            Frequency currFrequency = BadBudgetApplication.freqFromShortHand(currFreqShortHand);
            outState.putSerializable(BadBudgetApplication.TOGGLED_FREQUENCY_PREFIX_KEY + currLoss.expenseDescription(), currFrequency);
        }

        //Keep track of what the total freq's was toggled to and also the budget's freq
        Frequency totalFreq = BadBudgetApplication.freqFromShortHand(this.totalFreqView.getText().toString());
        outState.putSerializable(BadBudgetApplication.TOTAL_FREQ_KEY, totalFreq);
        Frequency budgetFreq = BadBudgetApplication.freqFromShortHand(this.budgetFreqView.getText().toString());
        outState.putSerializable(BadBudgetApplication.BUDGET_TOTAL_FREQ_KEY, budgetFreq);
    }

    /**
     * Helper method to sort a collection of money losses. First sorts items by
     * frequency (daily->yearly)
     * and then sorts alphabetically.
     * @param unsortedLosses - a collection of losses to sort
     * @return a list of losses sorted first on freq then alphabetically
     */
    private static List<MoneyLoss> sortLosses(Collection<MoneyLoss> unsortedLosses)
    {
        //Sort budget items first on frequency and then alphabetically
        ArrayList<MoneyLoss> oneTimeItems = new ArrayList<MoneyLoss>();
        ArrayList<MoneyLoss> dailyItems = new ArrayList<MoneyLoss>();
        ArrayList<MoneyLoss> weeklyItems = new ArrayList<MoneyLoss>();
        ArrayList<MoneyLoss> biweeklyItems = new ArrayList<MoneyLoss>();
        ArrayList<MoneyLoss> monthlyItems = new ArrayList<MoneyLoss>();
        ArrayList<MoneyLoss> yearlyItems = new ArrayList<MoneyLoss>();

        for (MoneyLoss loss : unsortedLosses)
        {
            switch (loss.lossFrequency())
            {
                case oneTime:
                {
                    oneTimeItems.add(loss);
                    break;
                }
                case daily:
                {
                    dailyItems.add(loss);
                    break;
                }
                case weekly:
                {
                    weeklyItems.add(loss);
                    break;
                }
                case biWeekly:
                {
                    biweeklyItems.add(loss);
                    break;
                }
                case monthly:
                {
                    monthlyItems.add(loss);
                    break;
                }
                case yearly:
                {
                    yearlyItems.add(loss);
                    break;
                }
            }
        }

        Comparator<MoneyLoss> comparator = new Comparator<MoneyLoss>() {
            @Override
            public int compare(MoneyLoss lhs, MoneyLoss rhs) {
                return lhs.expenseDescription().compareTo(rhs.expenseDescription());
            }
        };

        Collections.sort(oneTimeItems, comparator);
        Collections.sort(dailyItems, comparator);
        Collections.sort(weeklyItems, comparator);
        Collections.sort(biweeklyItems, comparator);
        Collections.sort(monthlyItems, comparator);
        Collections.sort(yearlyItems, comparator);

        ArrayList<MoneyLoss> sortedItems = new ArrayList<MoneyLoss>();
        BadBudgetApplication.appendItems(sortedItems, oneTimeItems);
        BadBudgetApplication.appendItems(sortedItems, dailyItems);
        BadBudgetApplication.appendItems(sortedItems, weeklyItems);
        BadBudgetApplication.appendItems(sortedItems, biweeklyItems);
        BadBudgetApplication.appendItems(sortedItems, monthlyItems);
        BadBudgetApplication.appendItems(sortedItems, yearlyItems);

        return sortedItems;
    }

    /**
     * Method called when the add loss button is clicked. Starts the add loss
     * activity and finishes this activity so that the table will need to be reloaded.
     * @param view
     */
    public void addLossClick(View view)
    {
        Intent intent = new Intent(this, AddLossActivity.class);
        intent.putExtra(BadBudgetApplication.EDIT_KEY, false);
        startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
    }

    /**
     * Private helper that appends a budget row to the losses table. This row shows the budget total
     * at the given frequency. The border fully surrounds this row
     * @param budgetFreq - the frequency to display the budget total at.
     */
    private void addBudgetRow(Frequency budgetFreq)
    {
        double total = BudgetSetActivity.getBudgetItemTotal(this, budgetFreq);

        TableLayout table = (TableLayout) findViewById(R.id.lossesTable);
        TableRow row = new TableRow(this);

        TextView totalView = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(totalView, getString(R.string.budget_total), R.drawable.bordertbl);

        final TextView totalAmountView = new TextView(this);
        this.budgetAmountView = totalAmountView;
        String totalAmountString = BadBudgetApplication.roundedDoubleBB(total);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(totalAmountView, totalAmountString, R.drawable.bordertbl);

        final TextView frequencyView = new TextView(this);
        this.budgetFreqView = frequencyView;

        frequencyView.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v)
            {
                Frequency currentToggleFreq = BadBudgetApplication.freqFromShortHand(frequencyView.getText().toString());
                Frequency convertToggleFreq = BadBudgetApplication.getNextToggleFreq(currentToggleFreq);
                double convertTotal = BudgetSetActivity.getBudgetItemTotal(LossesActivity.this, convertToggleFreq);
                budgetFreqView.setText(BadBudgetApplication.shortHandFreq(convertToggleFreq));
                budgetAmountView.setText(BadBudgetApplication.roundedDoubleBB(convertTotal));
            }

        });

        String frequencyString = BadBudgetApplication.shortHandFreq(budgetFreq);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(frequencyView, frequencyString, R.drawable.bordertbl);
        frequencyView.setPaintFlags(frequencyView.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);

        TextView emptyView = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(emptyView, "", R.drawable.borderfull);

        row.addView(totalView);
        row.addView(totalAmountView);
        row.addView(frequencyView);
        row.addView(emptyView);

        table.addView(row);
    }

    /**
     * Private helper method that adds an empty row to our table after the current row.
     */
    private void addEmptyRow()
    {
        TableLayout table = (TableLayout) findViewById(R.id.lossesTable);
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
        double total = getLossTotal(totalRowFreq);

        TableLayout table = (TableLayout) findViewById(R.id.lossesTable);
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
                double convertTotal = getLossTotal(convertToggleFreq);
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
     * Private helper method that computes the total value at the given frequency for losses
     * (includes the budget items total) //TODO 3/22/2017 - may want to include interest
     * @param totalRowFreq - the frequency to use for the total
     * @return the total at the given freq of all losses
     */
    private double getLossTotal(Frequency totalRowFreq)
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        List<MoneyLoss> losses = bbd.getLosses();
        double total = BudgetSetActivity.getBudgetItemTotal(this, totalRowFreq);
        for (MoneyLoss currLoss : losses)
        {
            //Exclude OT frequencies
            if (currLoss.lossFrequency() != Frequency.oneTime)
            {
                total += Prediction.toggle(currLoss.lossAmount(), currLoss.lossFrequency(), totalRowFreq);
            }
        }

        return total;
    }
}
