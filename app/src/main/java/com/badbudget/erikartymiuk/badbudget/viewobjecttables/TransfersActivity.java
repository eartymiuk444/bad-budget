package com.badbudget.erikartymiuk.badbudget.viewobjecttables;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetTableActivity;
import com.badbudget.erikartymiuk.badbudget.inputforms.AddTransferActivity;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.MoneyLoss;
import com.erikartymiuk.badbudgetlogic.main.MoneyTransfer;
import com.erikartymiuk.badbudgetlogic.main.Prediction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Activity displaying the transfers table. Note does not contain toggle freq or a totals row.
 * Created by Erik Artymiuk on 7/5/2017.
 */
public class TransfersActivity extends BadBudgetTableActivity
{
    private HashMap<String, TextView> amountViews;
    private HashMap<String, TextView> sourceViews;
    private HashMap<String, TextView> destinationViews;

    /* Private instance variable used to map a view that was clicked on to the associated transfer */
    private HashMap<View, MoneyTransfer> clickedTransfersMap;

    /**
     * On Create for the transfers activity. Sets the content for this activity. Populate table with the
     * currently set bad budget data objects
     *
     * @param savedInstanceState - unused for transfers table as no user input
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BadBudgetApplication application = ((BadBudgetApplication)this.getApplication());
        if (application.getBadBudgetUserData() == null)
        {
            this.finish();
            return;
        }

        setContent(R.layout.content_transfers);
        populateTable(savedInstanceState);
    }

    /**
     * The method called when the add transfers button is pressed. Starts the Add Transfer activity
     * and expects a result when it is finished.
     *
     * @param view - the view the click originated from
     */
    public void addTransferClick(View view)
    {
        Intent intent = new Intent(this, AddTransferActivity.class);
        startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
    }

    /**
     * Private helper that populates our transfers table by adding
     * a row for each transfer currently in our bad budget data.
     *
     * @param savedInstanceState - unused
     */
    private void populateTable(Bundle savedInstanceState)
    {
        amountViews = new HashMap<String, TextView>();
        sourceViews = new HashMap<String, TextView>();
        destinationViews = new HashMap<String, TextView>();

        clickedTransfersMap = new HashMap<View, MoneyTransfer>();

        /* Use the bad budget application wide data object to get a hold of all the user's transfers */
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        List<MoneyTransfer> transfers = bbd.getTransfers();
        transfers = TransfersActivity.sortTransfers(transfers);

        TableLayout table = (TableLayout) findViewById(R.id.transfersTable);

        /* For each transfer we setup a row in our table with the description, amount (frequency),
        source and destination fields.
         */
        for (final MoneyTransfer transfer : transfers) {

            final MoneyTransfer currTransfer = transfer;

            TableRow row = new TableRow(this);

            //Setup the description field
            final TextView descriptionView = new TextView(this);
            ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(descriptionView, currTransfer.getTransferDescription());
            descriptionView.setPaintFlags(descriptionView.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);

            /*
            The description when clicked should take us to a page where the user can edit the
            loss that they clicked on.
             */
            clickedTransfersMap.put(descriptionView, currTransfer);
            descriptionView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v)
                {
                    MoneyTransfer clickedTransfer = clickedTransfersMap.get(v);

                    Intent intent = new Intent(TransfersActivity.this, AddTransferActivity.class);
                    intent.putExtra(BadBudgetApplication.EDIT_KEY, true);
                    intent.putExtra(BadBudgetApplication.EDIT_OBJECT_ID_KEY, clickedTransfer.getTransferDescription());
                    startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
                }
            });

            //Setup the amount field
            final TextView transferAmountView = new TextView(this);
            amountViews.put(currTransfer.getTransferDescription(), transferAmountView);
            double transferAmount = currTransfer.getAmount();
            String frequencyString = BadBudgetApplication.shortHandFreq(currTransfer.getFrequency());

            String transferAmountString = BadBudgetApplication.roundedDoubleBB(transferAmount) + " (" + frequencyString + ")";
            ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(transferAmountView, transferAmountString);

            //Setup the source field.
            TextView sourceView = new TextView(this);
            sourceViews.put(currTransfer.getTransferDescription(), sourceView);
            ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(sourceView, currTransfer.getSource().name());

            //Setup the destination field.
            TextView destinationView = new TextView(this);
            destinationViews.put(currTransfer.getTransferDescription(), destinationView);
            ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(destinationView, currTransfer.getDestination().name());
            ((BadBudgetApplication)this.getApplication()).tableAdjustBorderForLastColumn(destinationView);


            row.addView(descriptionView);
            row.addView(transferAmountView);
            row.addView(sourceView);
            row.addView(destinationView);

            table.addView(row);
        }
    }

    /**
     * Called prior to this activity being destroyed, saves any necessary state to restore upon
     * the users return. Nothing currently needed for transfer table
     * @param outState - where to store the saved state.
     */
    protected void onSaveInstanceState(Bundle outState)
    {

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
     * Method called upon completion of the add transfer activity form which this activity started.
     * Updates our transfers table. The specific method is determined by the result code. It may update
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
     * Private helper method to be called when the form returns a result indicating an item
     * was deleted. Clears and populates the table.
     */
    private void repopulateTableForDelete()
    {
        clearTable();
        populateTable(null);
    }

    /**
     * Private helper method that clears the transfers table of all rows except the header
     * row.
     */
    private void clearTable()
    {
        TableLayout table = (TableLayout) findViewById(R.id.transfersTable);
        table.removeViews(1, table.getChildCount() - 1);
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
     * Private helper method to be called when the update task has completed. Nothing to be done
     * for transfers
     */
    private void refreshTableForUpdate()
    {

    }

    /**
     * Private helper method to be called when the form returns a result indicating an item
     * was edited. Updates the amount views TODO 7/12/2017 (including frequency)
     * ,the source, and destination views.
     */
    private void refreshTableForEdit()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        for (MoneyTransfer transfer : bbd.getTransfers())
        {
            TextView amountView = amountViews.get(transfer.getTransferDescription());
            TextView sourceView = sourceViews.get(transfer.getTransferDescription());
            TextView destinationView = sourceViews.get(transfer.getTransferDescription());

            double transferAmount = transfer.getAmount();
            String frequencyString = BadBudgetApplication.shortHandFreq(transfer.getFrequency());

            String transferAmountString = BadBudgetApplication.roundedDoubleBB(transferAmount) + " (" + frequencyString + ")";

            amountView.setText(transferAmountString);
            sourceView.setText(transfer.getSource().name());
            destinationView.setText(transfer.getDestination().name());
        }
    }


    /**
     * Helper method to sort a collection of money transfers. First sorts items by
     * frequency (daily->yearly)
     * and then sorts alphabetically.
     * @param unsortedTransfers - a collection of transfers to sort
     * @return a list of transfers sorted first on freq then alphabetically
     */
    private static List<MoneyTransfer> sortTransfers(Collection<MoneyTransfer> unsortedTransfers)
    {
        //Sort transfers first on frequency and then alphabetically
        ArrayList<MoneyTransfer> oneTimeItems = new ArrayList<MoneyTransfer>();
        ArrayList<MoneyTransfer> dailyItems = new ArrayList<MoneyTransfer>();
        ArrayList<MoneyTransfer> weeklyItems = new ArrayList<MoneyTransfer>();
        ArrayList<MoneyTransfer> biweeklyItems = new ArrayList<MoneyTransfer>();
        ArrayList<MoneyTransfer> monthlyItems = new ArrayList<MoneyTransfer>();
        ArrayList<MoneyTransfer> yearlyItems = new ArrayList<MoneyTransfer>();

        for (MoneyTransfer transfer : unsortedTransfers)
        {
            switch (transfer.getFrequency())
            {
                case oneTime:
                {
                    oneTimeItems.add(transfer);
                    break;
                }
                case daily:
                {
                    dailyItems.add(transfer);
                    break;
                }
                case weekly:
                {
                    weeklyItems.add(transfer);
                    break;
                }
                case biWeekly:
                {
                    biweeklyItems.add(transfer);
                    break;
                }
                case monthly:
                {
                    monthlyItems.add(transfer);
                    break;
                }
                case yearly:
                {
                    yearlyItems.add(transfer);
                    break;
                }
            }
        }

        Comparator<MoneyTransfer> comparator = new Comparator<MoneyTransfer>() {
            @Override
            public int compare(MoneyTransfer lhs, MoneyTransfer rhs) {
                return lhs.getTransferDescription().compareTo(rhs.getTransferDescription());
            }
        };

        Collections.sort(oneTimeItems, comparator);
        Collections.sort(dailyItems, comparator);
        Collections.sort(weeklyItems, comparator);
        Collections.sort(biweeklyItems, comparator);
        Collections.sort(monthlyItems, comparator);
        Collections.sort(yearlyItems, comparator);

        ArrayList<MoneyTransfer> sortedItems = new ArrayList<MoneyTransfer>();
        BadBudgetApplication.appendItems(sortedItems, oneTimeItems);
        BadBudgetApplication.appendItems(sortedItems, dailyItems);
        BadBudgetApplication.appendItems(sortedItems, weeklyItems);
        BadBudgetApplication.appendItems(sortedItems, biweeklyItems);
        BadBudgetApplication.appendItems(sortedItems, monthlyItems);
        BadBudgetApplication.appendItems(sortedItems, yearlyItems);

        return sortedItems;
    }

}
