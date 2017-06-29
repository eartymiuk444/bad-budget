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
import com.badbudget.erikartymiuk.badbudget.inputforms.AddCreditCardActivity;
import com.badbudget.erikartymiuk.badbudget.inputforms.AddCreditCardPayment;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.CreditCard;
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.MoneyOwed;
import com.erikartymiuk.badbudgetlogic.main.Payment;
import com.erikartymiuk.badbudgetlogic.main.Prediction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Activity for loading and populating a table displaying all of a user's credit card objects. Modeled
 * after the table for accounts. The four fields in the table are "name", "debt", "payment" and "quicklook".
 *
 */
public class CreditCardsActivity extends BadBudgetTableActivity {

    private HashMap<String, TextView> debtViews;
    private HashMap<String, TextView> paymentViews;
    private HashMap<String, CheckBox> quicklookViews;

    /* Private instance variable used to map a view that was clicked on to the associated credit card */
    private HashMap<View, CreditCard> clickedCardsMap;

    private TextView totalAmountView;
    private TextView totalPaymentView;

    /**
     * On create sets the content and populates the table using the current bad budget data.
     * @param savedInstanceState - unused as there is no state to save on this form
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

        setContent(R.layout.content_credit_cards);
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
     * for each of the debts in our table.
     */
    private void refreshTableForUpdate()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        for (MoneyOwed debt : bbd.getDebts())
        {
            if (debt instanceof CreditCard)
            {
                CreditCard currCreditCard = (CreditCard) debt;
                TextView debtView = debtViews.get(currCreditCard.name());

                debtView.setText(BadBudgetApplication.roundedDoubleBB(currCreditCard.amount()));
            }
        }

        double totalCCDebt = getCreditCardTotal();
        this.totalAmountView.setText(BadBudgetApplication.roundedDoubleBB(totalCCDebt));

        double totalPayment = getPaymentTotal(BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY);
        this.totalPaymentView.setText(BadBudgetApplication.constructAmountFreqString(totalPayment, BadBudgetApplication.DEFAULT_TOTAL_FREQUENCY));
    }

    /**
     * Private helper method to be called when the form returns a result indicating an item
     * was edited. Updates the debt, payment, and quicklook views of all the debts in our table
     */
    private void refreshTableForEdit()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        for (MoneyOwed debt : bbd.getDebts())
        {
            if (debt instanceof CreditCard)
            {
                CreditCard currCreditCard = (CreditCard) debt;
                TextView debtView = debtViews.get(currCreditCard.name());
                TextView paymentView = paymentViews.get(currCreditCard.name());
                CheckBox quicklookCheckbox = quicklookViews.get(currCreditCard.name());

                debtView.setText(BadBudgetApplication.roundedDoubleBB(currCreditCard.amount()));

                String paymentString = getString(R.string.debt_no_payment);

                Payment payment = currCreditCard.payment();
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
                quicklookCheckbox.setChecked(currCreditCard.quicklook());
            }
        }

        double totalCCDebt = getCreditCardTotal();
        this.totalAmountView.setText(BadBudgetApplication.roundedDoubleBB(totalCCDebt));

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
        TableLayout table = (TableLayout) findViewById(R.id.creditCardsTable);
        table.removeViews(1, table.getChildCount() - 1);
    }

    /**
     * Populates our credit cards table, formatting and setting the values
     * for each cell/row.
     */
    private void populateTable(Bundle savedInstanceState)
    {
        debtViews = new HashMap<String, TextView>();
        paymentViews = new HashMap<String, TextView>();
        quicklookViews = new HashMap<String, CheckBox>();

        clickedCardsMap = new HashMap<View, CreditCard>();

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

        TableLayout table = (TableLayout) findViewById(R.id.creditCardsTable);

        /* For each credit card we setup a row in our table with the name/description, value/amount,
        payment (if there is one), and quicklook fields.
         */
        for (final MoneyOwed debt : debts) {
            if (debt instanceof CreditCard) {

                final CreditCard currCreditCard = (CreditCard) debt;

                TableRow row = new TableRow(this);

                //Setup the name/description field
                final TextView descriptionView = new TextView(this);
                ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(descriptionView, currCreditCard.name());
                descriptionView.setPaintFlags(descriptionView.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);

                /*
                The description when clicked should take us to a page where the user can edit the
                credit card that they clicked on.
                 */
                clickedCardsMap.put(descriptionView, currCreditCard);
                descriptionView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {

                        CreditCard clickedCreditCard = clickedCardsMap.get(v);

                        //If the credit card already has a payment setup then we need to direct the user
                        //to the payment form regardless of whether they want to edit the payment or not
                        //as the debt amount changing could influence a valid payment.
                        Intent intent = null;
                        boolean hasExistingPayment = clickedCreditCard.payment() != null;
                        if (!hasExistingPayment)
                        {
                            intent = new Intent(CreditCardsActivity.this, AddCreditCardActivity.class);
                        }
                        else
                        {
                            intent = new Intent(CreditCardsActivity.this, AddCreditCardPayment.class);
                            intent.putExtra(BadBudgetApplication.EDIT_PAYMENT_KEY, true);
                        }
                        intent.putExtra(BadBudgetApplication.EDIT_KEY, true);
                        intent.putExtra(BadBudgetApplication.EDIT_OBJECT_ID_KEY, clickedCreditCard.name());
                        startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
                    }
                });

                //Setup the debt field
                TextView debtAmountView = new TextView(this);
                debtViews.put(currCreditCard.name(), debtAmountView);

                double creditCardValue = currCreditCard.amount();
                String creditCardValueString = BadBudgetApplication.roundedDoubleBB(creditCardValue);
                ((BadBudgetApplication)this.getApplication()).tableCellSetLayoutParams(debtAmountView, creditCardValueString);

                //Setup the payment field
                TextView paymentView = new TextView(this);
                paymentViews.put(currCreditCard.name(), paymentView);

                String paymentString = getString(R.string.debt_no_payment);

                Payment payment = currCreditCard.payment();
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
                quicklookViews.put(currCreditCard.name(), quicklookCheckbox);
                quicklookCheckbox.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v)
                    {
                        //We update quicklook field in memory immediately
                        currCreditCard.setQuicklook(quicklookCheckbox.isChecked());
                        QuicklookToggleTask task = new QuicklookToggleTask(CreditCardsActivity.this,
                                BBDatabaseContract.Debts.TABLE_NAME + "_" + ((BadBudgetApplication)CreditCardsActivity.this.getApplication()).getSelectedBudgetId(),
                                BBDatabaseContract.Debts.COLUMN_NAME,
                                BBDatabaseContract.Debts.COLUMN_QUICK_LOOK, quicklookCheckbox, currCreditCard.name());
                        task.execute();
                    }
                });

                ((BadBudgetApplication)this.getApplication()).tableQLCellSetLayoutParams(quicklookCheckbox, currCreditCard.quicklook());

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
     * Method called when the add credit card button is clicked. Starts the add credit card
     * activity
     * @param view
     */
    public void addCreditCardClick(View view)
    {
        Intent intent = new Intent(this, AddCreditCardActivity.class);
        intent.putExtra(BadBudgetApplication.EDIT_KEY, false);
        startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
    }

    /**
     * Private helper method that adds an empty row to our table after the current row.
     */
    private void addEmptyRow()
    {
        TableLayout table = (TableLayout) findViewById(R.id.creditCardsTable);
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
        double ccDebtTotal = getCreditCardTotal();
        double paymentTotal = getPaymentTotal(totalRowFreq);

        TableLayout table = (TableLayout) findViewById(R.id.creditCardsTable);
        TableRow row = new TableRow(this);

        TextView totalView = new TextView(this);
        ((BadBudgetApplication)this.getApplication()).initializeTableCell(totalView, getString(R.string.table_total), R.drawable.bordertbl);

        TextView totalAmountView = new TextView(this);
        this.totalAmountView = totalAmountView;
        String totalAmountString = BadBudgetApplication.roundedDoubleBB(ccDebtTotal);
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
     * Private helper method that computes the total value of credit cards
     * @return the total of all credit card debts
     */
    private double getCreditCardTotal()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        List<MoneyOwed> debts = bbd.getDebts();
        double total = 0;
        for (MoneyOwed currDebt : debts)
        {
            if (currDebt instanceof CreditCard)
            {
                total += currDebt.amount();
            }
        }

        return total;
    }

    /**
     * Private helper method that computes the total payment amount at the given freq.
     * @param totalRowFreq - the frequency to use for the total
     * @return the total at the given freq of all credit card payments
     */
    private double getPaymentTotal(Frequency totalRowFreq)
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        List<MoneyOwed> debts = bbd.getDebts();
        double total = 0;
        for (MoneyOwed currDebt : debts)
        {
            if (currDebt instanceof CreditCard && currDebt.payment() != null && !currDebt.payment().payOff() &&
                    !(currDebt.payment().frequency() == Frequency.oneTime))
            {
                total += Prediction.toggle(currDebt.payment().amount(), currDebt.payment().frequency(), totalRowFreq);
            }
        }
        return total;
    }
}
