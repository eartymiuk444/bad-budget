package com.badbudget.erikartymiuk.badbudget.viewobjecttables;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.Gravity;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseContract;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetTableActivity;
import com.badbudget.erikartymiuk.badbudget.inputforms.AddAccount;
import com.badbudget.erikartymiuk.badbudget.inputforms.AddSavingsActivity;
import com.erikartymiuk.badbudgetlogic.main.Account;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.SavingsAccount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * This activity displays to the user their list of cash accounts (regular and savings) in a table.
 * The user can click on any row in the table to edit an existing account or click the add account
 * button to add a new account
 */
public class CashActivity extends BadBudgetTableActivity {

    public static final String SAVINGS_STRING_NO = "No";
    public static final String SAVINGS_STRING_YES = "Yes";

    private HashMap<String, TextView> amountViews;
    private HashMap<String, AppCompatCheckBox> quicklookViews;

    private TextView totalAmountView;

    /**
     * On Create for the cash activity. Sets the content for this activity. Populate table with the
     * currently set bad budget data objects
     *
     * @param savedInstanceState - unused for table activities as no user input
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(R.layout.content_cash);
        populateTable();
    }

    /**
     * Method called after the update task has completed. Refreshes the necessary table views
     * that may have changed (i.e. the amount views).
     * @param updated - true if an update to bb objects occurred false otherwise, unused
     */
    public void updateTaskCompleted(boolean updated)
    {
        refreshTableForUpdate();
    }

    /**
     * Method called upon completion of the add account activity form which this activity started.
     * Updates our account table. The specific method is determined by the result code. It may update
     * only the necessary values that have changed or it may clear and repopulate the entire table.
     * @param requestCode - should be REQUEST_CASH_FORM_RESULT
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
     * Private helper method to be called when the update task has completed. Updates the amount views
     * for each of the accounts in our table. Also updates the total amount.
     */
    private void refreshTableForUpdate()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        for (Account a : bbd.getAccounts())
        {
            TextView amountView = amountViews.get(a.name());
            amountView.setText(BadBudgetApplication.roundedDoubleBB(a.value()));
        }

        double total = getCashTotal();
        this.totalAmountView.setText(BadBudgetApplication.roundedDoubleBB(total));
    }

    /**
     * Private helper method to be called when the form returns a result indicating an item
     * was edited. Updates the amount views, the quicklook views, and also the total view
     */
    private void refreshTableForEdit()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        for (Account a : bbd.getAccounts())
        {
            TextView amountView = amountViews.get(a.name());
            AppCompatCheckBox quicklookCheckbox = quicklookViews.get(a.name());

            amountView.setText(BadBudgetApplication.roundedDoubleBB(a.value()));
            quicklookCheckbox.setChecked(a.quickLook());
        }

        double total = getCashTotal();
        this.totalAmountView.setText(BadBudgetApplication.roundedDoubleBB(total));
    }

    /**
     * Private helper method to be called when the form returns a result indicating an item
     * was added. Clears and populates the table.
     */
    private void repopulateTableForAdd()
    {
        clearTable();
        populateTable();
    }

    /**
     * Private helper method to be called when the form returns a result indicating an item
     * was deleted. Clears and populates the table.
     */
    private void repopulateTableForDelete()
    {
        clearTable();
        populateTable();
    }

    /**
     * Private helper method that clears the cash accounts table of all rows except the header
     * row.
     */
    private void clearTable()
    {
        TableLayout table = (TableLayout) findViewById(R.id.cashAccountsTable);
        table.removeViews(1, table.getChildCount() - 1);
    }

    /**
     * Private helper that gets a hold of all of the current accounts and sorts them alphabetically. Then constructs
     * and adds all the necessary cells and rows to the cash account table. Note: does not clear an
     * already populated table.
     */
    private void populateTable()
    {
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

        TableLayout table = (TableLayout) findViewById(R.id.cashAccountsTable);

        amountViews = new HashMap<String, TextView>();
        quicklookViews = new HashMap<String, AppCompatCheckBox>();
        /* For each account we setup a row in our table with the name/description, value/amount,
        savings, and quicklook fields.
         */
        for (final Account currAccount : accounts)
        {
            TableRow row = new TableRow(this);

            //Setup the name/description field
            final TextView descriptionView = new TextView(this);
            ((BadBudgetApplication)getApplication()).tableCellSetLayoutParams(descriptionView, currAccount.name());
            descriptionView.setPaintFlags(descriptionView.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);

            /*
            The description when clicked should take us to a page where the user can edit the
            account that they clicked on.
             */
            descriptionView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v)
                {

                    Intent intent = null;
                    if (currAccount instanceof SavingsAccount)
                    {
                        intent = new Intent(CashActivity.this, AddSavingsActivity.class);
                        intent.putExtra(BadBudgetApplication.GENERIC_ACCOUNTS_RETURN_KEY, true);
                    }
                    else
                    {
                        intent = new Intent(CashActivity.this, AddAccount.class);
                    }
                    intent.putExtra(BadBudgetApplication.EDIT_KEY, true);
                    intent.putExtra(BadBudgetApplication.EDIT_OBJECT_ID_KEY, currAccount.name());
                    startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
                }
            });

            //Setup the amount field
            TextView amountView = new TextView(this);
            amountViews.put(currAccount.name(), amountView);
            double accountValue = currAccount.value();
            String accountValueString = BadBudgetApplication.roundedDoubleBB(accountValue);
            ((BadBudgetApplication)getApplication()).tableCellSetLayoutParams(amountView, accountValueString);

            //Setup the savings field
            TextView savingsView = new TextView(this);
            String savingsBoolString = SAVINGS_STRING_NO;
            if (currAccount instanceof SavingsAccount)
            {
                savingsBoolString = SAVINGS_STRING_YES;
            }
            ((BadBudgetApplication)getApplication()).tableCellSetLayoutParams(savingsView, savingsBoolString);

            //Setup the quicklook field. This field is a checkbox that the user can check and
            //uncheck to add/remove this account from their quicklook list.
            final AppCompatCheckBox quicklookCheckbox = new AppCompatCheckBox(this);
            quicklookViews.put(currAccount.name(), quicklookCheckbox);
            quicklookCheckbox.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v)
                {
                    //We update quicklook field in memory immediately
                    currAccount.setQuickLook(quicklookCheckbox.isChecked());
                    QuicklookToggleTask task = new QuicklookToggleTask(CashActivity.this,
                            BBDatabaseContract.CashAccounts.TABLE_NAME + "_" + ((BadBudgetApplication)CashActivity.this.getApplication()).getSelectedBudgetId(),
                            BBDatabaseContract.CashAccounts.COLUMN_NAME,
                            BBDatabaseContract.CashAccounts.COLUMN_QUICK_LOOK, quicklookCheckbox, currAccount.name());
                    task.execute();
                }
            });


            ((BadBudgetApplication)getApplication()).tableQLCellSetLayoutParams(quicklookCheckbox, currAccount.quickLook());

            TableRow.LayoutParams rowLayout = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLayout.gravity = Gravity.CENTER;
            row.addView(descriptionView);
            row.addView(amountView);
            row.addView(savingsView);
            row.addView(quicklookCheckbox);

            table.addView(row);
        }

        //Add in the total row
        addEmptyRow();
        addTotalRow();
    }

    /**
     * The method called when the add account button is pressed. Starts the Add Account activity
     * and expects a result when it is finished.
     *
     * @param view - the view the click originated from
     */
    public void addAccountClick(View view)
    {
        Intent intent = new Intent(this, AddAccount.class);
        startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
    }

    /**
     * Private helper method that adds an empty row to our table after the current row.
     */
    private void addEmptyRow()
    {
        TableLayout table = (TableLayout) findViewById(R.id.cashAccountsTable);
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
        double total = getCashTotal();

        TableLayout table = (TableLayout) findViewById(R.id.cashAccountsTable);
        TableRow row = new TableRow(this);

        TextView totalView = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(totalView, getString(R.string.table_total), R.drawable.bordertbl);

        final TextView totalAmountView = new TextView(this);
        this.totalAmountView = totalAmountView;
        String totalAmountString = BadBudgetApplication.roundedDoubleBB(total);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(totalAmountView, totalAmountString, R.drawable.bordertbl);

        TextView notAppView = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(notAppView, "", R.drawable.bordertbl);

        TextView quicklookView = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(quicklookView, "", R.drawable.borderfull);

        row.addView(totalView);
        row.addView(totalAmountView);
        row.addView(notAppView);
        row.addView(quicklookView);

        table.addView(row);
    }

    /**
     * Private helper method that computes the total value
     * @return the total of all cash accounts
     */
    private double getCashTotal()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        List<Account> accounts = bbd.getAccounts();
        double total = 0;
        for (Account currAccount : accounts)
        {
            total += currAccount.value();
        }

        return total;
    }
}
