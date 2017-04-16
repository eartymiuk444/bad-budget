package com.badbudget.erikartymiuk.badbudget.viewobjecttables;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetChildActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetTableActivity;
import com.badbudget.erikartymiuk.badbudget.inputforms.AddBudgetItemActivity;
import com.badbudget.erikartymiuk.badbudget.inputforms.BBOperationTaskCaller;
import com.badbudget.erikartymiuk.badbudget.inputforms.BudgetPrefsActivity;
import com.badbudget.erikartymiuk.badbudget.inputforms.SelectBudgetSourceDialog;
import com.erikartymiuk.badbudgetlogic.budget.BudgetItem;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.Prediction;
import com.erikartymiuk.badbudgetlogic.main.Source;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Activity for displaying all the users budget items and for allowing the user to edit and add new
 * items. The three columns in the table are "item", "budget", "toggle". Toggle is simply a convenience
 * to see what the budget looks like if it were given a different frequency it doesn't actually update
 * the users data.
 */
public class BudgetSetActivity extends BadBudgetTableActivity implements BBOperationTaskCaller {

    private HashMap<String, TextView> budgetAmountViews;
    private HashMap<String, TextView> frequencyViews;

    private TextView totalAmountView;
    private TextView totalFreqView;

    /* Tag for the select budget source dialog */
    private static final String SELECT_SOURCE_DIALOG_TAG = "SELECT_SOURCE_DIALOG_TAG";

    /**
     * On create for the budget set table activity. Populates our table with the current bad budget data.
     * Takes into account saved state which includes toggled frequencies.
     *
     * @param savedInstanceState - saved state to restore, including toggled frequencies
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContent(R.layout.content_budget_set);
        populateTable(savedInstanceState);
    }

    /**
     * Called prior to this activity being destroyed, saves any necessary state to restore upon
     * the users return.
     * @param outState - where to store the saved state.
     */
    protected void onSaveInstanceState(Bundle outState)
    {
        /* Use the bad budget application wide data object to get a hold of all the user's budget items */
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        Collection<BudgetItem> budgetItemsCollection = bbd.getBudget().getAllBudgetItems().values();

        for (BudgetItem currItem : budgetItemsCollection)
        {
            String currFreqShortHand = frequencyViews.get(currItem.expenseDescription()).getText().toString();
            Frequency currFrequency = BadBudgetApplication.freqFromShortHand(currFreqShortHand);
            outState.putSerializable(BadBudgetApplication.TOGGLED_FREQUENCY_PREFIX_KEY + currItem.expenseDescription(), currFrequency);
        }

        //Keep track of what the total freq's was toggled to
        Frequency totalFreq = BadBudgetApplication.freqFromShortHand(this.totalFreqView.getText().toString());
        outState.putSerializable(BadBudgetApplication.TOTAL_FREQ_KEY, totalFreq);
    }

    /**
     * Method called after the update task has completed. Refreshes the necessary table views
     * that may have changed
     * @param updated - true if an update to bb objects occurred false otherwise, unused
     */
    public void updateTaskCompleted(boolean updated)
    {
        refreshTableForUpdate();
    }

    /**
     * Method called upon completion of the add budget item activity form which this activity started.
     * Updates our table. The specific method is determined by the result code. It may update
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
     * for budget items
     */
    private void refreshTableForUpdate()
    {

    }

    /**
     * Private helper method to be called when the form returns a result indicating an item
     * was edited. Updates the budget amount views and the frequency views. Also updates the total
     * amount view
     */
    private void refreshTableForEdit()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();

        for (BudgetItem item : bbd.getBudget().getAllBudgetItems().values())
        {
            TextView budgetAmountView = budgetAmountViews.get(item.expenseDescription());
            TextView frequencyView = frequencyViews.get(item.expenseDescription());

            budgetAmountView.setText(BadBudgetApplication.roundedDoubleBB(item.lossAmount()));
            frequencyView.setText(BadBudgetApplication.shortHandFreq(item.lossFrequency()));
        }

        double totalAmount = getBudgetItemTotal(this, BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY);
        this.totalAmountView.setText(BadBudgetApplication.roundedDoubleBB(totalAmount));
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
     * Private helper method that clears the budget items table of all rows except the header
     * row.
     */
    private void clearTable()
    {
        TableLayout table = (TableLayout) findViewById(R.id.budgetTable);
        table.removeViews(1, table.getChildCount() - 1);
    }

    /**
     * Method called when the user clicks the budget new item button. If the user hasn't set a source
     * for their budget yet a popup is displayed allowing them to do so. Once it is this
     * press directs them to the input
     * form for a budget item and ends this activity so it has to be reloaded.
     * @param view - the button clicked
     */
    public void budgetNewItemClick(View view)
    {
        Source budgetSource = ((BadBudgetApplication)getApplication()).getBadBudgetUserData().getBudget().getBudgetSource();
        if (budgetSource == null)
        {
            SelectBudgetSourceDialog dialog = new SelectBudgetSourceDialog();
            dialog.show(getSupportFragmentManager(), SELECT_SOURCE_DIALOG_TAG);
        }
        else
        {
            Intent intent = new Intent(this, AddBudgetItemActivity.class);
            intent.putExtra(BadBudgetApplication.EDIT_KEY, false);
            startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
        }
    }

    /**
     * Private helper that populates our budget items table by adding
     * a row for each budget item currently in our bad budget data.
     * @param savedInstanceState - any state that should be restored as we populate the table.
     *                           Includes user toggled frequencies they left off with.
     */
    private void populateTable(Bundle savedInstanceState)
    {
        budgetAmountViews = new HashMap<String, TextView>();
        frequencyViews = new HashMap<String, TextView>();

        /* Use the bad budget application wide data object to get a hold of all the user's budget items */
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        Collection<BudgetItem> budgetItemsCollection = bbd.getBudget().getAllBudgetItems().values();

        List<BudgetItem> budgetItemsList = BudgetSetActivity.sortItems(budgetItemsCollection);

        TableLayout table = (TableLayout) findViewById(R.id.budgetTable);

        /* For each budget item we setup a row in our table with the item name, budget amount, and
        frequency (w/ toggle),
         */
        for (final BudgetItem bItem : budgetItemsList) {

            TableRow row = new TableRow(this);

            //Setup the item field
            final TextView itemView = new TextView(this);
            ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(itemView, bItem.expenseDescription());
            itemView.setPaintFlags(itemView.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);

            /*
            The item when clicked should take us to a page where the user can edit the
            budget item that they clicked on.
             */
            itemView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v)
                {

                    Intent intent = new Intent(BudgetSetActivity.this, AddBudgetItemActivity.class);
                    intent.putExtra(BadBudgetApplication.EDIT_KEY, true);
                    intent.putExtra(BadBudgetApplication.EDIT_OBJECT_ID_KEY, bItem.expenseDescription());
                    startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
                }
            });

            //Setup the budget amount field
            final TextView amountView = new TextView(this);
            budgetAmountViews.put(bItem.getDescription(), amountView);
            double budgetAmount = bItem.lossAmount();
            String budgetAmountString = BadBudgetApplication.roundedDoubleBB(budgetAmount);
            ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(amountView, budgetAmountString);

            //Setup the frequency field TODO with the toggle button
            final TextView frequencyView = new TextView(this);
            frequencyViews.put(bItem.getDescription(), frequencyView);
            if (bItem.lossFrequency() != Frequency.oneTime) {
                frequencyView.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View v) {
                        Frequency currentToggleFreq = BadBudgetApplication.freqFromShortHand(frequencyView.getText().toString());
                        Frequency convertToggleFreq = BadBudgetApplication.getNextToggleFreq(currentToggleFreq);
                        double toggleAmount = Prediction.toggle(bItem.lossAmount(), bItem.lossFrequency(), convertToggleFreq);

                        frequencyView.setText(BadBudgetApplication.shortHandFreq(convertToggleFreq));
                        amountView.setText(BadBudgetApplication.roundedDoubleBB(toggleAmount));
                        if (convertToggleFreq != bItem.lossFrequency()) {
                            amountView.setText(amountView.getText() + " " + BadBudgetApplication.TEMP_FREQUENCY_AMOUNT_MESSAGE);
                        }
                    }

                });
            }
            String frequencyString;
            if (savedInstanceState == null)
            {
                frequencyString = BadBudgetApplication.shortHandFreq(bItem.lossFrequency());
            }
            else
            {
                Frequency savedFreq = (Frequency) savedInstanceState.getSerializable(BadBudgetApplication.TOGGLED_FREQUENCY_PREFIX_KEY + bItem.expenseDescription());
                if (savedFreq != null) {
                    frequencyString = BadBudgetApplication.shortHandFreq(savedFreq);
                    if (savedFreq != bItem.lossFrequency()) {
                        double toggleAmount = Prediction.toggle(bItem.lossAmount(), bItem.lossFrequency(), savedFreq);
                        amountView.setText(BadBudgetApplication.roundedDoubleBB(toggleAmount) + " " + BadBudgetApplication.TEMP_FREQUENCY_AMOUNT_MESSAGE);
                    }
                }
                else
                {
                    frequencyString = BadBudgetApplication.shortHandFreq(bItem.lossFrequency());
                }
            }

            ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(frequencyView, frequencyString);
            ((BadBudgetApplication)getApplication()).tableAdjustBorderForLastColumn(frequencyView);
            frequencyView.setPaintFlags(frequencyView.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);

            row.addView(itemView);
            row.addView(amountView);
            row.addView(frequencyView);

            table.addView(row);

        }

        //Add in the total row with a toggable frequency
        addEmptyRow();
        if (savedInstanceState == null)
        {
            addTotalRow(BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY);
        }
        else
        {
            addTotalRow((Frequency)savedInstanceState.getSerializable(BadBudgetApplication.TOTAL_FREQ_KEY));
        }
    }

    /**
     * Private helper method that adds an empty row to our table after the current row.
     */
    private void addEmptyRow()
    {
        TableLayout table = (TableLayout) findViewById(R.id.budgetTable);
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
     * Private helper method that adds the total row to the table. The row uses the passed
     * frequency as the initial frequency for our total. The freq of the total row is also
     * able to be toggled.
     * @param totalRowFreq - the initial frequency of our total row
     */
    private void addTotalRow(Frequency totalRowFreq)
    {
        double total = getBudgetItemTotal(this, totalRowFreq);

        TableLayout table = (TableLayout) findViewById(R.id.budgetTable);
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
                double convertTotal = getBudgetItemTotal(BudgetSetActivity.this, convertToggleFreq);
                frequencyView.setText(BadBudgetApplication.shortHandFreq(convertToggleFreq));
                totalAmountView.setText(BadBudgetApplication.roundedDoubleBB(convertTotal));
            }

        });

        String frequencyString = BadBudgetApplication.shortHandFreq(totalRowFreq);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(frequencyView, frequencyString, R.drawable.borderfull);
        frequencyView.setPaintFlags(frequencyView.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);

        row.addView(totalView);
        row.addView(totalAmountView);
        row.addView(frequencyView);

        table.addView(row);
    }

    /**
     * Method that computes the total value at the given frequency for budget items
     * @param activity - the activity this method is called from (used to get our bbd)
     * @param totalRowFreq - the frequency to use for budget items
     * @return the total at the given freq of all budget items
     */
    public static double getBudgetItemTotal(Activity activity, Frequency totalRowFreq)
    {
        BadBudgetData bbd = ((BadBudgetApplication) activity.getApplication()).getBadBudgetUserData();
        Collection<BudgetItem> budgetItemsCollection = bbd.getBudget().getAllBudgetItems().values();
        double total = 0;
        for (BudgetItem currItem : budgetItemsCollection)
        {
            //Exclude OT frequencies
            if (currItem.lossFrequency() != Frequency.oneTime)
            {
                total += Prediction.toggle(currItem.lossAmount(), currItem.lossFrequency(), totalRowFreq);
            }
        }
        return total;
    }

    /**
     * Helper method to sort a collection of budget items. First sorts items by
     * frequency (daily->yearly)
     * and then sorts alphabetically.
     * @param unsortedItems - a collection of items to sort
     * @return a list of items sorted first on freq then alphabetically
     */
    private static List<BudgetItem> sortItems(Collection<BudgetItem> unsortedItems)
    {
        //Sort budget items first on frequency and then alphabetically
        ArrayList<BudgetItem> oneTimeItems = new ArrayList<BudgetItem>();
        ArrayList<BudgetItem> dailyItems = new ArrayList<BudgetItem>();
        ArrayList<BudgetItem> weeklyItems = new ArrayList<BudgetItem>();
        ArrayList<BudgetItem> biweeklyItems = new ArrayList<BudgetItem>();
        ArrayList<BudgetItem> monthlyItems = new ArrayList<BudgetItem>();
        ArrayList<BudgetItem> yearlyItems = new ArrayList<BudgetItem>();

        for (BudgetItem item : unsortedItems)
        {
            switch (item.lossFrequency())
            {
                case oneTime:
                {
                    oneTimeItems.add(item);
                    break;
                }
                case daily:
                {
                    dailyItems.add(item);
                    break;
                }
                case weekly:
                {
                    weeklyItems.add(item);
                    break;
                }
                case biWeekly:
                {
                    biweeklyItems.add(item);
                    break;
                }
                case monthly:
                {
                    monthlyItems.add(item);
                    break;
                }
                case yearly:
                {
                    yearlyItems.add(item);
                    break;
                }
            }
        }

        Comparator<BudgetItem> comparator = new Comparator<BudgetItem>() {
            @Override
            public int compare(BudgetItem lhs, BudgetItem rhs) {
                return lhs.expenseDescription().compareTo(rhs.expenseDescription());
            }
        };

        Collections.sort(oneTimeItems, comparator);
        Collections.sort(dailyItems, comparator);
        Collections.sort(weeklyItems, comparator);
        Collections.sort(biweeklyItems, comparator);
        Collections.sort(monthlyItems, comparator);
        Collections.sort(yearlyItems, comparator);

        ArrayList<BudgetItem> sortedItems = new ArrayList<BudgetItem>();
        BadBudgetApplication.appendItems(sortedItems, oneTimeItems);
        BadBudgetApplication.appendItems(sortedItems, dailyItems);
        BadBudgetApplication.appendItems(sortedItems, weeklyItems);
        BadBudgetApplication.appendItems(sortedItems, biweeklyItems);
        BadBudgetApplication.appendItems(sortedItems, monthlyItems);
        BadBudgetApplication.appendItems(sortedItems, yearlyItems);

        return sortedItems;
    }

    @Override //TODO - implement 12/09
    public void addBBObjectFinished() {

    }

    @Override
    public void editBBObjectFinished() {

    }

    @Override
    public void deleteBBObjectFinished() {

    }
}
