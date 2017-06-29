package com.badbudget.erikartymiuk.badbudget.viewobjecttables;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseContract;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetBaseActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetChildActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetTableActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.PredictActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.TrackActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.UpdateTask;
import com.badbudget.erikartymiuk.badbudget.bbmain.UpdateTaskCaller;
import com.badbudget.erikartymiuk.badbudget.inputforms.AddSavingsActivity;
import com.erikartymiuk.badbudgetlogic.main.Account;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.Contribution;
import com.erikartymiuk.badbudgetlogic.main.CreditCard;
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.MoneyOwed;
import com.erikartymiuk.badbudgetlogic.main.Prediction;
import com.erikartymiuk.badbudgetlogic.main.SavingsAccount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * This activity displays all savings account in a table form (similar to cash accounts activity)
 * Each savings account shows four of the more pertinent fields for a savings account (name, value, goal,
 * quicklook toggle) in a row in the table. Each row name can be clicked on taking the user to a form
 * where they can view and edit every field for a savings account.
 * There is also a button below the table that is used to add savings accounts.
 */
public class SavingsActivity extends BadBudgetTableActivity {

    private HashMap<String, TextView> amountViews;
    private HashMap<String, TextView> contributionViews;
    private HashMap<String, CheckBox> quicklookViews;

    private TextView totalAmountView;
    private TextView totalContributionView;


    /* Private instance variable used to map a view that was clicked on to the associated account */
    private HashMap<View, SavingsAccount> clickedSavingsAccountsMap;

    /**
     *
     * @param savedInstanceState
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BadBudgetApplication application = ((BadBudgetApplication)this.getApplication());
        if (application.getBadBudgetUserData() == null)
        {
            this.finish();
            return;
        }

        setContent(R.layout.content_savings);
        populateTable(savedInstanceState);
    }

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
     * Method called after the update task has completed. Updates any necessary table views
     * @param updated - true if an update to bb objects occurred false otherwise.
     */
    public void updateTaskCompleted(boolean updated)
    {
        refreshTableForUpdate();
    }

    /**
     * Private helper method to be called when the update task has completed. Updates the amount views
     * for each of the savings accounts in our table and also the total row
     */
    private void refreshTableForUpdate()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        for (Account a : bbd.getAccounts())
        {
            if (a instanceof SavingsAccount) {
                SavingsAccount sa = (SavingsAccount) a;

                TextView amountView = amountViews.get(sa.name());
                amountView.setText(BadBudgetApplication.roundedDoubleBB(sa.value()));
            }
        }

        double total = getSavingsTotal();
        this.totalAmountView.setText(BadBudgetApplication.roundedDoubleBB(total));
    }

    /**
     * Private helper method to be called when the form returns a result indicating an item
     * was edited. Updates the amount views, goal views, and the quicklook views. Also updates the total
     * row
     */
    private void refreshTableForEdit()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        for (Account a : bbd.getAccounts())
        {
            if (a instanceof SavingsAccount) {
                SavingsAccount sa = (SavingsAccount) a;

                TextView amountView = amountViews.get(sa.name());
                amountView.setText(BadBudgetApplication.roundedDoubleBB(sa.value()));

                TextView contributionView = contributionViews.get(sa.name());
                String contributionString = BadBudgetApplication.constructAmountFreqString(sa.contribution().getContribution(), sa.contribution().getFrequency());

                contributionView.setText(contributionString);

                CheckBox quicklookCheckbox = quicklookViews.get(sa.name());
                quicklookCheckbox.setChecked(sa.quickLook());
            }
        }

        double total = getSavingsTotal();
        this.totalAmountView.setText(BadBudgetApplication.roundedDoubleBB(total));

        double totalContribution = getContributionTotal(BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY);
        this.totalContributionView.setText(BadBudgetApplication.constructAmountFreqString(totalContribution, BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY));
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
     * Clears the table of all rows except the header row.
     */
    private void clearTable()
    {
        TableLayout table = (TableLayout) findViewById(R.id.savingsAccountsTable);
        table.removeViews(1, table.getChildCount() - 1);
    }

    /**
     * Private helper method that populates the savings account table first clearing the table and
     * then formatting and adding in each cell and row.
     */
    private void populateTable(Bundle savedInstanceState)
    {
        amountViews = new HashMap<String, TextView>();
        contributionViews = new HashMap<String, TextView>();
        quicklookViews = new HashMap<String, CheckBox>();
        clickedSavingsAccountsMap = new HashMap<View, SavingsAccount>();

        /* Use the bad budget application wide data object to get a hold of all the user's accounts */
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        ArrayList<Account> accounts = bbd.getAccounts();

        Comparator<Account> comparator = new Comparator<Account>() {
            @Override
            public int compare(Account lhs, Account rhs) {
                return lhs.name().compareTo(rhs.name());
            }
        };
        Collections.sort(accounts, comparator);

        TableLayout table = (TableLayout) findViewById(R.id.savingsAccountsTable);

        /* For each savings account we setup a row in our table with the name/description, value/amount,
        goal amount, and quicklook fields.
         */
        BadBudgetApplication application = ((BadBudgetApplication)getApplication());
        for (final Account currAccount : accounts)
        {
            if (currAccount instanceof SavingsAccount) {

                final SavingsAccount currSavingsAccount = (SavingsAccount) currAccount;

                TableRow row = new TableRow(this);

                //Setup the name/description field
                final TextView descriptionView = new TextView(this);
                application.tableCellSetLayoutParams(descriptionView, currSavingsAccount.name());
                descriptionView.setPaintFlags(descriptionView.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);

            /*
            The description when clicked should take us to a page where the user can edit the savings
            account that they clicked on.
             */
                clickedSavingsAccountsMap.put(descriptionView, currSavingsAccount);
                descriptionView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {

                        SavingsAccount clickedSavingsAccount = clickedSavingsAccountsMap.get(v);
                        Intent intent = new Intent(SavingsActivity.this, AddSavingsActivity.class);
                        intent.putExtra(BadBudgetApplication.EDIT_KEY, true);
                        intent.putExtra(BadBudgetApplication.EDIT_OBJECT_ID_KEY, clickedSavingsAccount.name());
                        intent.putExtra(BadBudgetApplication.GENERIC_ACCOUNTS_RETURN_KEY, false);

                        startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
                    }
                });

                //Setup the amount field
                TextView amountView = new TextView(this);
                amountViews.put(currSavingsAccount.name(), amountView);

                double accountValue = currSavingsAccount.value();
                String accountValueString = BadBudgetApplication.roundedDoubleBB(accountValue);
                application.tableCellSetLayoutParams(amountView, accountValueString);

                //Setup the goal field
                TextView contributionView = new TextView(this);
                contributionViews.put(currSavingsAccount.name(), contributionView);

                double contributionAmount = currSavingsAccount.contribution().getContribution();
                Frequency contributionFreq = currSavingsAccount.contribution().getFrequency();
                String contributionString = BadBudgetApplication.constructAmountFreqString(contributionAmount, contributionFreq);

                application.tableCellSetLayoutParams(contributionView, contributionString);

                //Setup the quicklook field. This field is a checkbox that the user can check and
                //uncheck to add/remove this savings account from their quicklook list.
                final CheckBox quicklookCheckbox = new CheckBox(this);
                quicklookViews.put(currSavingsAccount.name(), quicklookCheckbox);
                quicklookCheckbox.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v)
                    {
                        //We update quicklook field in memory immediately
                        currAccount.setQuickLook(quicklookCheckbox.isChecked());
                        QuicklookToggleTask task = new QuicklookToggleTask(SavingsActivity.this,
                                BBDatabaseContract.CashAccounts.TABLE_NAME + "_" + ((BadBudgetApplication)SavingsActivity.this.getApplication()).getSelectedBudgetId(),
                                BBDatabaseContract.CashAccounts.COLUMN_NAME,
                                BBDatabaseContract.CashAccounts.COLUMN_QUICK_LOOK, quicklookCheckbox, currAccount.name());
                        task.execute();
                    }
                });

                application.tableQLCellSetLayoutParams(quicklookCheckbox, currSavingsAccount.quickLook());

                TableRow.LayoutParams rowLayout = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                rowLayout.gravity = Gravity.CENTER;
                row.addView(descriptionView);
                row.addView(amountView);
                row.addView(contributionView);
                row.addView(quicklookCheckbox);

                table.addView(row);
            }
        }

        //Add in the total row with a toggable frequency
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
     * Method called when the add savings account button is clicked. Starts the add savings
     * activity.
     * @param view -  the add savings button that was clicked.
     */
    public void addSavingsAccountClick(View view)
    {
        Intent intent = new Intent(this, AddSavingsActivity.class);
        startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
    }

    /**
     * Called prior to this activity being destroyed, saves any necessary state to restore upon
     * the users return.
     * @param outState - where to store the saved state.
     */
    protected void onSaveInstanceState(Bundle outState)
    {
        //Keep track of what the total freq's was toggled to
        Frequency totalFreq = BadBudgetApplication.freqFromShortHand(BadBudgetApplication.extractShortHandFreq(this.totalContributionView.getText().toString()));
        outState.putSerializable(BadBudgetApplication.TOTAL_FREQ_KEY, totalFreq);
    }

    /**
     * Private helper method that adds an empty row to our table after the current row.
     */
    private void addEmptyRow()
    {
        TableLayout table = (TableLayout) findViewById(R.id.savingsAccountsTable);
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
    private void addTotalRow(Frequency totalRowFreq)
    {
        double total = getSavingsTotal();
        double contributionTotal = getContributionTotal(totalRowFreq);

        TableLayout table = (TableLayout) findViewById(R.id.savingsAccountsTable);
        TableRow row = new TableRow(this);

        TextView totalView = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(totalView, getString(R.string.table_total), R.drawable.bordertbl);

        final TextView totalAmountView = new TextView(this);
        this.totalAmountView = totalAmountView;
        String totalAmountString = BadBudgetApplication.roundedDoubleBB(total);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(totalAmountView, totalAmountString, R.drawable.bordertbl);

        final TextView contributionView = new TextView(this);
        contributionView.setPaintFlags(contributionView.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);
        this.totalContributionView = contributionView;

        contributionView.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v)
            {
                Frequency currentToggleFreq =
                        BadBudgetApplication.freqFromShortHand(BadBudgetApplication.extractShortHandFreq(contributionView.getText().toString()));
                Frequency convertToggleFreq = BadBudgetApplication.getNextToggleFreq(currentToggleFreq);
                double convertTotal = getContributionTotal(convertToggleFreq);
                contributionView.setText(BadBudgetApplication.constructAmountFreqString(convertTotal, convertToggleFreq));
            }

        });

        String contributionString = BadBudgetApplication.constructAmountFreqString(contributionTotal, totalRowFreq);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(contributionView, contributionString, R.drawable.bordertbl);

        TextView quicklookView = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(quicklookView, "", R.drawable.borderfull);

        row.addView(totalView);
        row.addView(totalAmountView);
        row.addView(contributionView);
        row.addView(quicklookView);

        table.addView(row);
    }

    /**
     * Private helper method that computes the total value
     * @return the total of all cash accounts
     */
    private double getSavingsTotal()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        List<Account> accounts = bbd.getAccounts();
        double total = 0;
        for (Account currAccount : accounts)
        {
            if (currAccount instanceof SavingsAccount)
            {
                total += currAccount.value();
            }
        }

        return total;
    }

    /**
     * Private helper method that computes the total contribution amount at the given freq.
     * @param totalRowFreq - the frequency to use for the total
     * @return the total at the given freq of all contributions
     */
    private double getContributionTotal(Frequency totalRowFreq)
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        List<Account> accounts = bbd.getAccounts();
        double total = 0;
        for (Account currAccount : accounts)
        {
            if (currAccount instanceof SavingsAccount)
            {
                Contribution contribution = ((SavingsAccount)currAccount).contribution();
                total += Prediction.toggle(contribution.getContribution(), contribution.getFrequency(), totalRowFreq);
            }
        }

        return total;
    }
}
