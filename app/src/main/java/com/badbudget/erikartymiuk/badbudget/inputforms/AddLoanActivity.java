package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseContract;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetChildActivity;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetInvalidValueException;
import com.erikartymiuk.badbudgetlogic.main.DebtType;
import com.erikartymiuk.badbudgetlogic.main.Loan;


/**
 * Activity to display a form to the user, accept their input, verify their input, and if valid add
 * a specified loan object to the bbd data and bb database. Also if setup payment is checked
 * the data input is carried over to the setup payment form where the user is directed. In that case
 * no objects are yet added.
 *
 * Also the form used to edit an existing loan that doesn't have a payment, but the user
 * can set one up for an existing account.
 */
public class AddLoanActivity extends BadBudgetChildActivity implements BBOperationTaskCaller {

    private EditText loanNameInput;
    private EditText currentDebtInput;
    private EditText interestRateInput;
    private EditText principalInput;

    private CheckBox simpleInterestCheckbox;
    private CheckBox quicklookCheckbox;
    private CheckBox setupPaymentCheckbox;

    private Button submitNextButton;

    private static final String progressDialogMessage = "Adding Loan...";
    private static final String progressDialogMessageDelete = "Deleting Loan...";
    private static final String progressDialogMessageUpdate = "Updating Loan...";

    private boolean editing;
    private String editName;

    /* Saved state keys */
    private static final String DEBT_NAME_KEY = "DEBT_NAME";
    private static final String CURRENT_DEBT_KEY = "CURRENT_DEBT_VALUE";
    private static final String QUICKLOOK_KEY = "QUICKLOOK";
    private static final String SETUP_PAYMENT_KEY = "SETUP_PAYMENT";
    private static final String INTEREST_RATE_KEY = "INTEREST_RATE";
    private static final String PRINCIPAL_KEY = "PRINCIPAL";
    private static final String SIMPLE_INTEREST_KEY = "SIMPLE_INTEREST";

    /**
     * On create for the add loan activity. Sets the content view and sets up both for
     * a first time entry and for when we have saved state to restore
     * @param savedInstanceState - the state to restore
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(R.layout.content_add_loan);

        setup();

        if (savedInstanceState == null)
        {
            firstTimeSetup();
        }
        else
        {
            savedStateSetup(savedInstanceState);
        }
    }

    /**
     * Called when an activity was started for a result. The debt form only expects a result after
     * starting the add debt with payment form. If the form was submitted we finish this activity so the user
     * ends up back at the debts table
     * @param requestCode - should be FORM_RESULT_REQUEST
     * @param resultCode - indicates what occurred in the setup payment form
     * @param data - unused
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == BadBudgetApplication.FORM_RESULT_REQUEST)
        {
            if (resultCode == BadBudgetApplication.FORM_RESULT_ADD ||
                    resultCode == BadBudgetApplication.FORM_RESULT_EDIT ||
                    resultCode == BadBudgetApplication.FORM_RESULT_DELETE)
            {
                setResult(resultCode);
                finish();
            }
        }
    }

    /**
     * Method called on completion of the update task. Simply returns
     * @param updated - true if an update to bb objects occurred false otherwise.
     */
    public void updateTaskCompleted(boolean updated)
    {

    }

    /**
     * Private helper method to setup necessary views for the debt form; invoked regardless
     * of whether first time setup or restoring from a saved state. Sets necessary conditions
     * for editing also.
     */
    private void setup()
    {
        loanNameInput = (EditText) findViewById(R.id.inputLoanName);
        currentDebtInput = (EditText) findViewById(R.id.inputCurrentDebt);
        interestRateInput = (EditText) findViewById(R.id.inputInterestRate);
        principalInput = (EditText) findViewById(R.id.principalInput);
        principalInput.setEnabled(false);
        simpleInterestCheckbox = (CheckBox) findViewById(R.id.simpleInterestCheckbox);
        quicklookCheckbox = (CheckBox) findViewById(R.id.addLoanQuicklookCheckbox);
        setupPaymentCheckbox = (CheckBox) findViewById(R.id.setupPaymentCheckbox);

        submitNextButton = (Button) findViewById(R.id.submitButton);

        //Get anything the user already filled out for just a debt without a payment
        Bundle args = getIntent().getExtras();
        if (args != null)
        {
            editing = args.getBoolean(BadBudgetApplication.EDIT_KEY);
            if (editing)
            {
                this.setToolbarTitle(R.string.edit_loan_title);

                //Get the name of the account we are editing
                editName = args.getString(BadBudgetApplication.EDIT_OBJECT_ID_KEY);
                Loan editLoan = (Loan)((BadBudgetApplication)this.getApplication()).getBadBudgetUserData().getDebtWithName(editName);
                loanNameInput.setText(editLoan.name());

                //Make loan name not editable
                loanNameInput.setEnabled(false);

                //Enable the delete button and make it visible
                Button deleteButton = (Button) this.findViewById(R.id.deleteButton);
                deleteButton.setEnabled(true);
                deleteButton.setVisibility(View.VISIBLE);
            }
        }
        else
        {
            editing = false;
        }
    }

    /**
     * Private helper method that sets up the form for being used when the user first enters.
     */
    private void firstTimeSetup()
    {
        if (editing)
        {
            //Prepopulate the basic fields
            Loan editLoan = (Loan)((BadBudgetApplication)this.getApplication()).getBadBudgetUserData().getDebtWithName(editName);
            currentDebtInput.setText(Double.toString(editLoan.amount()));
            interestRateInput.setText(Double.toString(editLoan.interestRate()));

            boolean simpleInterest = editLoan.isSimpleInterest();
            if (simpleInterest)
            {
                simpleInterestCheckbox.setChecked(true);
                simpleInterestCheckboxChecked();
                principalInput.setText(Double.toString(editLoan.getPrincipalBalance()));
            }
            else
            {
                simpleInterestCheckbox.setChecked(false);
                simpleInterestCheckboxUnchecked();
            }

            quicklookCheckbox.setChecked(editLoan.quicklook());
        }
    }

    /**
     * Sets up our form when the user returns to it the state they left it in should be restored.
     * @param savedInstanceState - the state to restore
     */
    private void savedStateSetup(Bundle savedInstanceState)
    {
        loanNameInput.setText(savedInstanceState.getString(DEBT_NAME_KEY));
        currentDebtInput.setText(savedInstanceState.getString(CURRENT_DEBT_KEY));
        interestRateInput.setText(savedInstanceState.getString(INTEREST_RATE_KEY));

        boolean simpleInterest = savedInstanceState.getBoolean(SIMPLE_INTEREST_KEY);
        if (simpleInterest)
        {
            simpleInterestCheckbox.setChecked(true);
            simpleInterestCheckboxChecked();
            principalInput.setText(savedInstanceState.getString(PRINCIPAL_KEY));
        }
        else
        {
            simpleInterestCheckbox.setChecked(false);
            simpleInterestCheckboxUnchecked();
        }
        quicklookCheckbox.setChecked(savedInstanceState.getBoolean(QUICKLOOK_KEY));
        setupPaymentCheckbox.setChecked(savedInstanceState.getBoolean(SETUP_PAYMENT_KEY));
        if (setupPaymentCheckbox.isChecked())
        {
            submitNextButton.setText(R.string.add_loan_next);
        }
        else
        {
            submitNextButton.setText(R.string.add_loan_submit);
        }
    }

    /**
     * Called prior to this activity being destroyed, saves any necessary state to restore upon
     * the users return.
     * @param outState - where to store the saved state.
     */
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putString(DEBT_NAME_KEY, loanNameInput.getText().toString());
        outState.putString(CURRENT_DEBT_KEY, currentDebtInput.getText().toString());
        outState.putString(INTEREST_RATE_KEY, interestRateInput.getText().toString());
        outState.putString(PRINCIPAL_KEY, principalInput.getText().toString());

        outState.putBoolean(SIMPLE_INTEREST_KEY, simpleInterestCheckbox.isChecked());
        outState.putBoolean(QUICKLOOK_KEY, quicklookCheckbox.isChecked());
        outState.putBoolean(SETUP_PAYMENT_KEY, setupPaymentCheckbox.isChecked());
    }

    /**
     * Method called when user clicks the submit (or next) button for adding a loan. If the inputs are
     * valid (and setup payment isn't checked) then this method kicks off a task that adds/edits the loan
     * to the database and to memory.
     * The user is shown a progress dialog until the task is complete. If the inputs are invalid then
     * the submit button does nothing. (the user simply remains on the page)
     * @param view
     */
    public void submitClick(View view)
    {
        if (verifyValues())
        {
            //User is adding a a loan without a payment setup or a payment edit
            if (!setupPaymentCheckbox.isChecked())
            {
                //Prep for creation of the in memory loan object
                String name = loanNameInput.getText().toString();
                double debt = Double.parseDouble(currentDebtInput.getText().toString());
                double interestRateDouble = Double.parseDouble(interestRateInput.getText().toString());
                boolean simpleInterest = this.simpleInterestCheckbox.isChecked();
                double principal;
                if (simpleInterest)
                {
                    principal = Double.parseDouble(principalInput.getText().toString());
                }
                else
                {
                    principal = -1;
                }

                boolean quicklook = this.quicklookCheckbox.isChecked();

                try
                {
                    Loan loan = new Loan(name, debt, quicklook, interestRateDouble, simpleInterest, principal);
                    //Prep for creation of the database object
                    ContentValues values = new ContentValues();
                    values.put(BBDatabaseContract.Debts.COLUMN_NAME, name);
                    values.put(BBDatabaseContract.Debts.COLUMN_AMOUNT, debt);
                    values.put(BBDatabaseContract.Debts.COLUMN_DEBT_TYPE, BBDatabaseContract.dbDebtTypeToInteger(DebtType.Loan));
                    values.put(BBDatabaseContract.Debts.COLUMN_INTEREST_RATE, interestRateDouble);
                    values.put(BBDatabaseContract.Debts.COLUMN_SIMPLE_INTEREST, BBDatabaseContract.dbBooleanToInteger(simpleInterest));
                    values.put(BBDatabaseContract.Debts.COLUMN_PRINCIPLE, principal);

                    if (quicklookCheckbox.isChecked())
                    {
                        values.put(BBDatabaseContract.Debts.COLUMN_QUICK_LOOK, BBDatabaseContract.TRUE_VALUE);
                    }
                    else
                    {
                        values.put(BBDatabaseContract.Debts.COLUMN_QUICK_LOOK, BBDatabaseContract.FALSE_VALUE);
                    }

                    if (!editing)
                    {
                        AddBBObjectTask task = new AddBBObjectTask(this, progressDialogMessage, loan, values, this, BBObjectType.DEBT_LOAN);
                        task.execute();
                    }
                    else
                    {
                        EditBBObjectTask task = new EditBBObjectTask(this, progressDialogMessageUpdate, loan, values, this, BBObjectType.DEBT_LOAN);
                        task.execute();
                    }
                }
                catch (BadBudgetInvalidValueException e)
                {
                    //TODO - handle
                    e.printStackTrace();
                }
            }
            else
            {
                //User is adding or editing a loan and wants to setup (or add) a payment
                Intent intent = new Intent(AddLoanActivity.this, AddLoanPayment.class);
                if (editing)
                {
                    intent.putExtra(BadBudgetApplication.EDIT_KEY, true);
                    intent.putExtra(BadBudgetApplication.EDIT_OBJECT_ID_KEY, editName);
                    intent.putExtra(BadBudgetApplication.EDIT_PAYMENT_KEY, false); //Not editing an existing payment but adding a new one

                    /* Pass along any values that the user changed */
                    intent.putExtra(BadBudgetApplication.INPUT_DEBT_AMOUNT_KEY, Double.parseDouble(currentDebtInput.getText().toString()));

                    intent.putExtra(BadBudgetApplication.INPUT_DEBT_INTEREST_RATE_KEY, Double.parseDouble(interestRateInput.getText().toString()));
                    intent.putExtra(BadBudgetApplication.INPUT_LOAN_SIMPLE_INTEREST_KEY, simpleInterestCheckbox.isChecked());
                    if (simpleInterestCheckbox.isChecked())
                    {
                        //Only pass along principal if simple interest is checked (it should be "N/A" otherwise
                        intent.putExtra(BadBudgetApplication.INPUT_LOAN_PRINCIPAL_KEY, Double.parseDouble(principalInput.getText().toString()));
                    }

                    intent.putExtra(BadBudgetApplication.INPUT_DEBT_QUICKLOOK_KEY, quicklookCheckbox.isChecked());
                }
                else
                {
                    intent.putExtra(BadBudgetApplication.EDIT_KEY, false);
                    intent.putExtra(BadBudgetApplication.INPUT_DEBT_NAME_KEY, loanNameInput.getText().toString());
                    intent.putExtra(BadBudgetApplication.INPUT_DEBT_AMOUNT_KEY, Double.parseDouble(currentDebtInput.getText().toString()));

                    intent.putExtra(BadBudgetApplication.INPUT_DEBT_INTEREST_RATE_KEY, Double.parseDouble(interestRateInput.getText().toString()));
                    intent.putExtra(BadBudgetApplication.INPUT_LOAN_SIMPLE_INTEREST_KEY, simpleInterestCheckbox.isChecked());
                    if (simpleInterestCheckbox.isChecked())
                    {
                        //Only pass along principal if simple interest is checked (it should be "N/A" otherwise
                        intent.putExtra(BadBudgetApplication.INPUT_LOAN_PRINCIPAL_KEY, Double.parseDouble(principalInput.getText().toString()));
                    }

                    intent.putExtra(BadBudgetApplication.INPUT_DEBT_QUICKLOOK_KEY, quicklookCheckbox.isChecked());
                }

                startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
            }
        }
    }

    /**
     * Method called when the cancel button is clicked by the user. Behaves identically to if the
     * back button when pressed
     * @param view - the source of the click
     */
    public void cancelClick(View view)
    {
        this.onBackPressed();
    }

    /**
     * Verfies that the user has input a unique name (if they are not editing)
     * and a value in the current debt field and the interest rate field
     * for this loan object. Also if simple interest is checked makes sure
     * principal is not empty and is 0 or a positive value.
     * @return true if input values are valid
     */
    public boolean verifyValues()
    {
        //Check to make sure the user input a unique name and a value for the debt
        BadBudgetData bbd = ((BadBudgetApplication)this.getApplication()).getBadBudgetUserData();
        String name = loanNameInput.getText().toString();
        if (name.equals("") || (!editing && bbd.getDebtWithName(name) != null))
        {
            return false;
        }

        String debtString = currentDebtInput.getText().toString();
        if (debtString.equals(""))
        {
            return false;
        }

        String interestRateString = interestRateInput.getText().toString();
        if (interestRateString.equals("") || Double.parseDouble(interestRateString) < 0)
        {
            return false;
        }

        //If simple interest is checked make sure principal is entered and is a positive value
        if (simpleInterestCheckbox.isChecked())
        {
            String principalString = principalInput.getText().toString();
            if (principalString.equals("") || Double.parseDouble(principalString) < 0)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Method called when the payment checkbox is checked or unchecked. We have to toggle
     * between submition text and next text as when the user wants to go on to setup (or edit)
     * a payment nothing is submitted until after they submit the payment info.
     * @param view - the checkbox that was clicked
     */
    public void paymentCheckboxClicked(View view)
    {
        boolean addEditPayment = setupPaymentCheckbox.isChecked();
        if (addEditPayment)
        {
            submitNextButton.setText(R.string.add_loan_next);
        }
        else
        {
            submitNextButton.setText(R.string.add_loan_submit);
        }
    }

    /**
     * Method called after the task adding the loan has completed. Sets result and ends this activity.
     */
    public void addBBObjectFinished()
    {
        setResult(BadBudgetApplication.FORM_RESULT_ADD);
        this.finish();
    }

    /**
     * Method called after the task to edit a load has finished. Sets result and ends this activity
     */
    public void editBBObjectFinished()
    {
        setResult(BadBudgetApplication.FORM_RESULT_EDIT);
        this.finish();
    }

    /**
     * Method called after the task deleting a loan has completed. Sets result and ends this activty.
     */
    public void deleteBBObjectFinished()
    {
        setResult(BadBudgetApplication.FORM_RESULT_DELETE);
        this.finish();
    }

    /**
     * Method called when the delete button is pressed. Kicks off the task to remove the loan
     * from memory and from the database.
     * @param view - the button pressed
     */
    public void deleteClick(View view)
    {
        DeleteBBObjectTask task = new DeleteBBObjectTask(this, progressDialogMessageDelete, editName, this, BBObjectType.DEBT_LOAN);
        task.execute();
    }

    /**
     * Method called when the simple interest checkbox is toggled. Calls the simpleInterestCheckboxChecked
     * method or the simpleInterestCheckboxUnchecked methods which appropriately adjust dependent views (i.e. principalInput)
     * @param view - the checkbox that was clicked
     */
    public void simpleInterestCheckboxClicked(View view)
    {
        if (simpleInterestCheckbox.isChecked())
        {
            simpleInterestCheckboxChecked();
        }
        else
        {
            simpleInterestCheckboxUnchecked();
        }
    }

    /**
     * Private helper that should be called whenever the simple interest checkbox is checked either
     * programmatically or by the user. Enables the principal input field.
     */
    private void simpleInterestCheckboxChecked()
    {
        principalInput.setEnabled(true);
    }

    /**
     * Private helper that should be called whenever the simple interest checkbox is unchecked either
     * programmatically or by the user. Clears and disables the principal input field.
     */
    private void simpleInterestCheckboxUnchecked()
    {
        principalInput.setText("");
        principalInput.setEnabled(false);
    }
}
