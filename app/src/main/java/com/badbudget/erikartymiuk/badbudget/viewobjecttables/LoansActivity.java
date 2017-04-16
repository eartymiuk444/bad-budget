package com.badbudget.erikartymiuk.badbudget.viewobjecttables;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseContract;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetChildActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetTableActivity;
import com.badbudget.erikartymiuk.badbudget.inputforms.AddLoanActivity;
import com.badbudget.erikartymiuk.badbudget.inputforms.AddLoanPayment;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.Loan;
import com.erikartymiuk.badbudgetlogic.main.MoneyLoss;
import com.erikartymiuk.badbudgetlogic.main.MoneyOwed;
import com.erikartymiuk.badbudgetlogic.main.Payment;
import com.erikartymiuk.badbudgetlogic.main.Prediction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Activity for loading and populating a table displaying all of a user's loan objects. Modeled
 * after the table for accounts. The four fields in the table are "name", "debt", "payment" and "quicklook".
 *
 */
public class LoansActivity extends BadBudgetTableActivity {

    private HashMap<String, TextView> debtViews;
    private HashMap<String, TextView> paymentViews;
    private HashMap<String, CheckBox> quicklookViews;

    /* Private instance variable used to map a view that was clicked on to the associated loan */
    private HashMap<View, Loan> clickedLoansMap;

    private TextView totalAmountView;
    private TextView totalPaymentView;

    /**
     * On create for a loan activity.
     *
     * @param savedInstanceState
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContent(R.layout.content_loans);
        populateTable(savedInstanceState);
    }

    /**
     * Method called upon completion of the add debt activity form which this activity started.
     * Updates our debt table. The specific method is determined by the result code. It may update
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
     * Method called after the update task has completed. If the bb objects were updated this
     * method updates the table so the update is reflected.
     * @param updated - true if an update to bb objects occurred false otherwise.
     */
    public void updateTaskCompleted(boolean updated)
    {
        refreshTableForUpdate();
    }

    /**
     * Private helper method to be called when the update task has completed. Updates the debt views
     * for each of the debts in our table. Also updates the total row.
     */
    private void refreshTableForUpdate()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        for (MoneyOwed debt : bbd.getDebts())
        {
            if (debt instanceof Loan)
            {
                Loan currLoan = (Loan) debt;
                TextView debtView = debtViews.get(currLoan.name());

                debtView.setText(BadBudgetApplication.roundedDoubleBB(currLoan.amount()));
            }
        }

        double totalLoan = getLoanTotal();
        this.totalAmountView.setText(BadBudgetApplication.roundedDoubleBB(totalLoan));

        double totalPayment = getPaymentTotal(BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY);
        this.totalPaymentView.setText(BadBudgetApplication.constructAmountFreqString(totalPayment, BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY));
    }

    /**
     * Private helper method to be called when the form returns a result indicating an item
     * was edited. Updates the debt, payment, and quicklook views of all the debts in our table.
     * Also updates the total row
     */
    private void refreshTableForEdit()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        for (MoneyOwed debt : bbd.getDebts())
        {
            if (debt instanceof Loan)
            {
                Loan currLoan = (Loan) debt;
                TextView debtView = debtViews.get(currLoan.name());
                TextView paymentView = paymentViews.get(currLoan.name());
                CheckBox quicklookCheckbox = quicklookViews.get(currLoan.name());

                debtView.setText(BadBudgetApplication.roundedDoubleBB(currLoan.amount()));

                String paymentString = getString(R.string.debt_no_payment);

                Payment payment = currLoan.payment();
                if (payment != null) {

                    Frequency pFreq = payment.frequency();
                    String freqString = " (" + BadBudgetApplication.shortHandFreq(pFreq) + ")";
                    if (payment.payOff())
                    {
                        paymentString = getString(R.string.payoff_shorthand) + freqString;
                    }
                    else
                    {
                        paymentString = BadBudgetApplication.roundedDoubleBB(payment.amount()) + freqString;
                    }
                }
                paymentView.setText(paymentString);
                quicklookCheckbox.setChecked(currLoan.quicklook());
            }
        }

        double totalLoan = getLoanTotal();
        this.totalAmountView.setText(BadBudgetApplication.roundedDoubleBB(totalLoan));

        double totalPayment = getPaymentTotal(BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY);
        this.totalPaymentView.setText(BadBudgetApplication.constructAmountFreqString(totalPayment, BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY));
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
        TableLayout table = (TableLayout) findViewById(R.id.loansTable);
        table.removeViews(1, table.getChildCount() - 1);
    }

    /**
     * Private helper that populates our loans table, adding
     * a row for each loan currently in our bad budget data.
     */
    private void populateTable(Bundle savedInstanceState)
    {
        debtViews = new HashMap<String, TextView>();
        paymentViews = new HashMap<String, TextView>();
        quicklookViews = new HashMap<String, CheckBox>();

        clickedLoansMap = new HashMap<View, Loan>();

        /* Use the bad budget application wide data object to get a hold of all the user's debts */
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        ArrayList<MoneyOwed> debts = bbd.getDebts();

        Comparator<MoneyOwed> comparator = new Comparator<MoneyOwed>() {
            @Override
            public int compare(MoneyOwed lhs, MoneyOwed rhs) {
                return lhs.name().compareTo(rhs.name());
            }
        };
        Collections.sort(debts, comparator);

        TableLayout table = (TableLayout) findViewById(R.id.loansTable);

        /* For each loan we setup a row in our table with the name/description, value/amount,
        payment (if there is one), and quicklook fields.
         */
        for (final MoneyOwed debt : debts) {
            if (debt instanceof Loan) {

                final Loan currLoan = (Loan) debt;

                TableRow row = new TableRow(this);

                //Setup the name/description field
                final TextView descriptionView = new TextView(this);
                ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(descriptionView, currLoan.name());
                descriptionView.setPaintFlags(descriptionView.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);

                /*
                The description when clicked should take us to a page where the user can edit the
                loan that they clicked on.
                 */
                clickedLoansMap.put(descriptionView, currLoan);
                descriptionView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v)
                    {
                        Loan clickedLoan = clickedLoansMap.get(v);

                        //If the loan already has a payment setup then we need to direct the user
                        //to the payment form regardless of whether they want to edit the payment or not
                        //as the debt amount changing could influence a valid payment.
                        Intent intent = null;
                        boolean hasExistingPayment = clickedLoan.payment() != null;
                        if (!hasExistingPayment)
                        {
                            intent = new Intent(LoansActivity.this, AddLoanActivity.class);
                        }
                        else
                        {
                            intent = new Intent(LoansActivity.this, AddLoanPayment.class);
                            intent.putExtra(BadBudgetApplication.EDIT_PAYMENT_KEY, true);
                        }
                        intent.putExtra(BadBudgetApplication.EDIT_KEY, true);
                        intent.putExtra(BadBudgetApplication.EDIT_OBJECT_ID_KEY, clickedLoan.name());
                        startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
                    }
                });

                //Setup the debt field
                TextView debtAmountView = new TextView(this);
                debtViews.put(currLoan.name(), debtAmountView);

                double loanValue = currLoan.amount();
                String loanValueString = BadBudgetApplication.roundedDoubleBB(loanValue);
                ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(debtAmountView, loanValueString);

                //Setup the payment field
                TextView paymentView = new TextView(this);
                paymentViews.put(currLoan.name(), paymentView);

                String paymentString = getString(R.string.debt_no_payment);

                Payment payment = currLoan.payment();
                if (payment != null) {

                    Frequency pFreq = payment.frequency();
                    String freqString = " (" + BadBudgetApplication.shortHandFreq(pFreq) + ")";
                    if (payment.payOff())
                    {
                        paymentString = getString(R.string.payoff_shorthand) + freqString;
                    }
                    else
                    {
                        paymentString = BadBudgetApplication.roundedDoubleBB(payment.amount()) + freqString;
                    }
                }
                ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(paymentView, paymentString);

                //Setup the quicklook field.
                final CheckBox quicklookCheckbox = new CheckBox(this);
                quicklookViews.put(currLoan.name(), quicklookCheckbox);
                quicklookCheckbox.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v)
                    {
                        //We update quicklook field in memory immediately
                        currLoan.setQuicklook(quicklookCheckbox.isChecked());
                        QuicklookToggleTask task = new QuicklookToggleTask(LoansActivity.this,
                                BBDatabaseContract.Debts.TABLE_NAME + "_" + ((BadBudgetApplication)LoansActivity.this.getApplication()).getSelectedBudgetId(), BBDatabaseContract.Debts.COLUMN_NAME,
                                BBDatabaseContract.Debts.COLUMN_QUICK_LOOK, quicklookCheckbox, currLoan.name());
                        task.execute();
                    }
                });

                ((BadBudgetApplication)this.getApplication()).tableQLCellSetLayoutParams(quicklookCheckbox, currLoan.quicklook());

                TableRow.LayoutParams rowLayout = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                rowLayout.gravity = Gravity.CENTER;

                row.addView(descriptionView);
                row.addView(debtAmountView);
                row.addView(paymentView);
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
     * Called prior to this activity being destroyed, saves any necessary state to restore upon
     * the users return.
     * @param outState - where to store the saved state.
     */
    protected void onSaveInstanceState(Bundle outState)
    {
        //Keep track of what the total freq's was toggled to
        Frequency totalFreq = BadBudgetApplication.freqFromShortHand(BadBudgetApplication.extractShortHandFreq(this.totalPaymentView.getText().toString()));
        outState.putSerializable(BadBudgetApplication.TOTAL_FREQ_KEY, totalFreq);
    }

    /**
     * Method called when the add loan button is clicked. Starts the add loan
     * activity
     * @param view
     */
    public void addLoanClick(View view)
    {
        Intent intent = new Intent(this, AddLoanActivity.class);
        intent.putExtra(BadBudgetApplication.EDIT_KEY, false);
        startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
    }

    /**
     * Private helper method that adds an empty row to our table after the current row.
     */
    private void addEmptyRow()
    {
        TableLayout table = (TableLayout) findViewById(R.id.loansTable);
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
        double loanTotal = getLoanTotal();
        double paymentTotal = getPaymentTotal(totalRowFreq);

        TableLayout table = (TableLayout) findViewById(R.id.loansTable);
        TableRow row = new TableRow(this);

        TextView totalView = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(totalView, getString(R.string.table_total), R.drawable.bordertbl);

        TextView totalAmountView = new TextView(this);
        this.totalAmountView = totalAmountView;
        String totalAmountString = BadBudgetApplication.roundedDoubleBB(loanTotal);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(totalAmountView, totalAmountString, R.drawable.bordertbl);

        final TextView paymentView = new TextView(this);
        this.totalPaymentView = paymentView;

        paymentView.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v)
            {
                Frequency currentToggleFreq =
                        BadBudgetApplication.freqFromShortHand(BadBudgetApplication.extractShortHandFreq(paymentView.getText().toString()));
                Frequency convertToggleFreq = BadBudgetApplication.getNextToggleFreq(currentToggleFreq);
                double convertTotal = getPaymentTotal(convertToggleFreq);
                paymentView.setText(BadBudgetApplication.constructAmountFreqString(convertTotal, convertToggleFreq));
            }

        });

        String paymentString = BadBudgetApplication.constructAmountFreqString(paymentTotal, totalRowFreq);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(paymentView, paymentString, R.drawable.bordertbl);
        paymentView.setPaintFlags(paymentView.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);

        TextView quicklookView = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(quicklookView, "", R.drawable.borderfull);

        row.addView(totalView);
        row.addView(totalAmountView);
        row.addView(paymentView);
        row.addView(quicklookView);

        table.addView(row);
    }

    /**
     * Private helper method that computes the total value of the loans
     * @return the total of all loan amounts
     */
    private double getLoanTotal()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        List<MoneyOwed> debts = bbd.getDebts();
        double total = 0;
        for (MoneyOwed currDebt : debts)
        {
            if (currDebt instanceof Loan)
            {
                total += currDebt.amount();
            }
        }

        return total;
    }

    /**
     * Private helper method that computes the total payment amount at the given freq.
     * @param totalRowFreq - the frequency to use for the total
     * @return the total at the given freq of all loan payments
     */
    private double getPaymentTotal(Frequency totalRowFreq)
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        List<MoneyOwed> debts = bbd.getDebts();
        double total = 0;
        for (MoneyOwed currDebt : debts)
        {
            if (currDebt instanceof Loan && currDebt.payment() != null && !currDebt.payment().payOff() &&
                    !(currDebt.payment().frequency() == Frequency.oneTime))
            {
                total += Prediction.toggle(currDebt.payment().amount(), currDebt.payment().frequency(), totalRowFreq);
            }
        }
        return total;
    }
}
