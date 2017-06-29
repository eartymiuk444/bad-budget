package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseContract;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetChildActivity;
import com.badbudget.erikartymiuk.badbudget.R;
import com.erikartymiuk.badbudgetlogic.main.Account;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetInvalidValueException;
import com.erikartymiuk.badbudgetlogic.main.MoneyGain;
import com.erikartymiuk.badbudgetlogic.main.MoneyLoss;
import com.erikartymiuk.badbudgetlogic.main.MoneyOwed;
import com.erikartymiuk.badbudgetlogic.main.Payment;
import com.erikartymiuk.badbudgetlogic.main.SavingsAccount;
import com.erikartymiuk.badbudgetlogic.main.Source;


/**
 * Activity offers user input fields for adding a regular account to their budget data. It also
 * has an option to make the account they are adding a savings account which directs them to
 * a more in depth input screen. On submit the user is directed back to the accounts page after
 * their account is added, showing a progress dialog until success.
 * On cancel the user is simply directed back to the accounts page.
 *
 * This activity is also available for editing an exisiting account. If the intent passed an account
 * name matched with the edit key then the form is considered an edit form with editable fields (not
 * the name) and a delete button.
 *
 */
public class AddAccount extends BadBudgetChildActivity implements BBOperationTaskCaller {

    /* Message displayed as the account is added, deleted, or being updated from memory and the database */
    public static final String progressDialogMessage = "Adding account...";
    public static final String progressDialogMessageDelete = "Deleting account...";
    public static final String progressDialogMessageUpdate = "Updating account...";

    /* Basic account fields */
    private EditText accountNameInput;
    private EditText currentValueInput;
    private CheckBox quicklookInput;
    private CheckBox setupAsSavingsInput;

    /* Keys for saving the instance state */
    private static final String ACCOUNT_NAME_KEY = "ACCOUNT_NAME";
    private static final String CURRENT_VALUE_KEY = "CURRENT_VALUE";
    private static final String QUICKLOOK_KEY = "QUICKLOOK";
    private static final String SETUP_AS_SAVINGS_KEY = "SETUP_AS_SAVINGS";

    private Toast deleteErrorToast;

    /* Indicates if the user entered this form by clicking on an exisiting account
    with the intent to edit it */
    boolean editing;

    /* If editing an account this is the accounts name, this is not editable */
    String editName;

    /**
     * The creation method for the Add Account activity. Sets the content of this activity and
     * sets up the form for adding or editing; both if the user is returning or if this is the first
     * time the user enters the form
     * @param savedInstanceState - used to populate the form if the user is returning and the state
     *                              needs to be restored
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BadBudgetApplication application = ((BadBudgetApplication)this.getApplication());
        if (application.getBadBudgetUserData() == null)
        {
            this.finish();
            return;
        }

        setContent(R.layout.content_add_account);

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
     * Called when an activity was started for a result. The cash form only expects a result after
     * starting the add savings form. If the form was submitted we finish this activity so the user
     * ends up back at the cash table
     * @param requestCode - should be FORM_RESULT_REQUEST
     * @param resultCode - indicates what occurred in the add savings form
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
     * @param updated - true if an update to bb objects occurred false otherwise, unused
     */
    public void updateTaskCompleted(boolean updated)
    {

    }

    /**
     * Called prior to this activity being destroyed. Saves any necessary state in order to restore
     * this activity when/if the user returns.
     * @param outState - Bundle to place saved state in
     */
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putString(ACCOUNT_NAME_KEY, accountNameInput.getText().toString());
        outState.putString(CURRENT_VALUE_KEY, currentValueInput.getText().toString());
        outState.putBoolean(QUICKLOOK_KEY, quicklookInput.isChecked());
        outState.putBoolean(SETUP_AS_SAVINGS_KEY, setupAsSavingsInput.isChecked());
    }

    /**
     * Private helper method that sets up necessary elements of our form. Specifically handles putting
     * views into correct state for when the user is editing; regardless of if the first time in the form
     * or if the user is reentering the form.
     *
     * If editing changes our title, disables our setup as savings and makes the disabled delete
     * button visible and enabled. Also disables the name field from being editable
     */
    private void setup()
    {
        accountNameInput = (EditText) this.findViewById(R.id.inputAccountName);
        currentValueInput = (EditText) this.findViewById(R.id.inputAccountAmount);
        quicklookInput = (CheckBox) this.findViewById(R.id.checkboxQuicklook);
        setupAsSavingsInput = (CheckBox) this.findViewById(R.id.savingsCheckbox);

        Bundle args = this.getIntent().getExtras();
        if (args != null)
        {
            editing = args.getBoolean(BadBudgetApplication.EDIT_KEY);
            if (editing)
            {
                this.setToolbarTitle(R.string.add_account_edit_title);
                //Get the name of the account we are editing
                editName = args.getString(BadBudgetApplication.EDIT_OBJECT_ID_KEY);
                Account editAccount = ((BadBudgetApplication)this.getApplication()).getBadBudgetUserData().getAccountWithName(editName);

                //pre populate the fields
                accountNameInput.setEnabled(false);
                accountNameInput.setText(editAccount.name());

                setupAsSavingsInput.setVisibility(View.INVISIBLE);
                setupAsSavingsInput.setEnabled(false);

                /* Instantiate our toast used for messages to the user when a delete fails */
                deleteErrorToast = Toast.makeText(this, getString(R.string.account_delete_error_base), Toast.LENGTH_SHORT);
                deleteErrorToast.setGravity(Gravity.CENTER, 0, 0);

                //Enable the delete button
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
     * Private helper method that restores the form using the passed saved instance state
     * @param savedInstanceState - the saved state of the form
     */
    private void savedStateSetup(Bundle savedInstanceState)
    {
        accountNameInput.setText(savedInstanceState.getString(ACCOUNT_NAME_KEY));
        currentValueInput.setText(savedInstanceState.getString(CURRENT_VALUE_KEY));
        quicklookInput.setChecked(savedInstanceState.getBoolean(QUICKLOOK_KEY));
        setupAsSavingsInput.setChecked(savedInstanceState.getBoolean(SETUP_AS_SAVINGS_KEY));
    }

    /**
     * Private helper method that sets up the form for the first time (i.e. the user isn't returning)
     */
    private void firstTimeSetup()
    {
        if (editing) {
            Account editAccount = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData().getAccountWithName(editName);

            //pre populate the fields
            EditText valueInputField = (EditText) this.findViewById(R.id.inputAccountAmount);
            CheckBox quicklookField = (CheckBox) this.findViewById(R.id.checkboxQuicklook);

            valueInputField.setText(Double.toString(editAccount.value()));
            quicklookField.setChecked(editAccount.quickLook());
        }
    }

    /**
     * Method called when the submit button is clicked on the add account page. Verifies the user's
     * input values are not empty and adds the resultant account to the applications memory bad budget data
     * and the bad budget database. If either the name or amount for the account is empty then
     * this method simply returns leaving the user on the add account page.
     *
     * If the user is editing an account then the submit button click updates the in memory and database
     * objects for that account
     *
     * @param view - the view the click originated from.
     */
    public void submitClick(View view)
    {
        //First get ahold of the user's inputs
        EditText editText = (EditText) findViewById(R.id.inputAccountName);
        String userInputAccountName = editText.getText().toString();

        editText = (EditText) findViewById(R.id.inputAccountAmount);
        String userInputAccountAmount = editText.getText().toString();

        CheckBox quicklookCheckbox = (CheckBox) findViewById(R.id.checkboxQuicklook);

        CheckBox savingsCheckbox = (CheckBox) findViewById(R.id.savingsCheckbox);

        //Basic input validation
        //if the user doesn't enter anything for the text fields we don't
        //process anything. (i.e. the user simply remains on the add account page)
        //TODO - add toasts and probably separate method for validation (duplicate accounts)
        if (!userInputAccountName.equals("") && !userInputAccountAmount.equals(""))
        {
            //Check if the user checked the savings account checkbox
            if (savingsCheckbox.isChecked())
            {
                //Need to go to the savings account form and prepopulate any input the user put in
                double accountInitialAmount = Double.parseDouble(userInputAccountAmount);
                boolean quicklook = quicklookCheckbox.isChecked();

                Intent intent = new Intent(this, AddSavingsActivity.class);
                intent.putExtra(BadBudgetApplication.EDIT_KEY, false);
                intent.putExtra(BadBudgetApplication.GENERIC_ACCOUNTS_RETURN_KEY, true);
                intent.putExtra(BadBudgetApplication.INPUT_ACCOUNT_NAME_KEY, userInputAccountName);
                intent.putExtra(BadBudgetApplication.INPUT_ACCOUNT_AMOUNT_KEY, accountInitialAmount);
                intent.putExtra(BadBudgetApplication.INPUT_ACCOUNT_QUICKLOOK_KEY, quicklook);
                startActivityForResult(intent, BadBudgetApplication.FORM_RESULT_REQUEST);
            }
            else
            {
                //Prep the input for creation of the account object
                double accountInitialAmount = Double.parseDouble(userInputAccountAmount);
                boolean quicklook = quicklookCheckbox.isChecked();

                Account account = null;

                try
                {
                    account = new Account(userInputAccountName, accountInitialAmount, quicklook);
                    //Prep the input for insertion into the database
                    ContentValues values = new ContentValues();
                    values.put(BBDatabaseContract.CashAccounts.COLUMN_NAME, userInputAccountName);
                    values.put(BBDatabaseContract.CashAccounts.COLUMN_VALUE, accountInitialAmount);

                    if (quicklook)
                    {
                        values.put(BBDatabaseContract.CashAccounts.COLUMN_QUICK_LOOK, BBDatabaseContract.TRUE_VALUE);
                    }
                    else
                    {
                        values.put(BBDatabaseContract.CashAccounts.COLUMN_QUICK_LOOK, BBDatabaseContract.FALSE_VALUE);
                    }

                    if (!editing)
                    {
                        AddBBObjectTask task = new AddBBObjectTask(this, progressDialogMessage, account, values, this, BBObjectType.ACCOUNT);
                        task.execute();
                    }
                    else
                    {
                        EditBBObjectTask task = new EditBBObjectTask(this, progressDialogMessageUpdate, account, values, this, BBObjectType.ACCOUNT);
                        task.execute();
                    }
                }
                catch (BadBudgetInvalidValueException e)
                {
                    //TODO - handle?
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Method called when the cancel button is clicked by the user. Behaves identically to if the
     * back button were pressed
     * @param view - the source of the click
     */
    public void cancelClick(View view)
    {
        this.onBackPressed();
    }

    /**
     * Verifies that deletion of the current account we are editing doesn't result in an inconsistent
     * bad budget data (both in memory and in our database). Constructs a string that should be appended
     * to the base error message which informs the user of all the reasons the account cannot be deleted
     * or if there are no errors the method returns null.
     *
     * @return - a string that indicates all errors for why account cannot be deleted (should be appended to
     *              base message) or null if there are no errors.
     */
    private String verifyCanDelete()
    {
        BadBudgetData bbd = ((BadBudgetApplication)getApplication()).getBadBudgetUserData();
        String errorString = "";
        //Need to check if this account is the source for a savings account's contribution
        for (Account account : bbd.getAccounts())
        {
            if (account instanceof SavingsAccount)
            {
                SavingsAccount savingsAccount = (SavingsAccount) account;
                if (savingsAccount.sourceAccount().name().equals(editName))
                {
                    errorString += "\n" + getString(R.string.account_delete_error_savings_contribution_source) + " " + savingsAccount.name();
                }
            }
        }
        //Need to check if this account is the source for a debt's payment
        for (MoneyOwed debt : bbd.getDebts())
        {
            Payment payment = debt.payment();
            if (payment != null)
            {
                if (payment.sourceAccount().name().equals(editName))
                {
                    errorString += "\n" + getString(R.string.account_delete_error_debts_payment_source) + " " + debt.name();
                }
            }
        }

        //Need to check if this account is the destination for a gain
        for (MoneyGain gain : bbd.getGains())
        {
            if (gain.destinationAccount().name().equals(editName))
            {
                errorString += "\n" + getString(R.string.account_delete_error_gains_destination) + " " + gain.sourceDescription();
            }
        }

        //Need to check if this account is the source for a loss TODO - check so not Credit Card or make unique Source names
        for (MoneyLoss loss : bbd.getLosses())
        {
            if (loss.source().name().equals(editName))
            {
                errorString += "\n" + getString(R.string.account_delete_error_losses_source) + " " + loss.expenseDescription();
            }
        }

        //Need to check if this account is the source for our budget items TODO - check so not Credit Card or make unique Source names
        Source budgetSource = bbd.getBudget().getBudgetSource();
        if (budgetSource != null && budgetSource.name().equals(editName))
        {
            errorString += "\n" + getString(R.string.account_delete_error_budget_source);
        }

        if (errorString.equals(""))
        {
            return null;
        }
        else
        {
            return errorString;
        }
    }

    /**
     * Method called when the delete button is clicked. Kicks off a task that deletes the account that
     * we are currently editing. The delete button is only visible and thus this method is called only
     * when the user is editing an existing account.
     * @param view
     */
    public void deleteClick(View view)
    {
        String errorString = verifyCanDelete();
        if (errorString == null) {
            DeleteBBObjectTask task = new DeleteBBObjectTask(this, progressDialogMessageDelete, this.editName, this, BBObjectType.ACCOUNT);
            task.execute();
        }
        else
        {
            if (!deleteErrorToast.getView().isShown())
            {
                deleteErrorToast.setText(getString(R.string.account_delete_error_base) + errorString);
                deleteErrorToast.show();
            }
        }
    }

    /**
     * Callback method called after an invoked AddBBObjectTask completes.
     * Finishes the add form activity with the corresponding result set.
     */
    public void addBBObjectFinished()
    {
        setResult(BadBudgetApplication.FORM_RESULT_ADD);
        this.finish();
    }

    /**
     * Callback method called after an invoked EditBBObjectTask completes.
     * Finishes this AddAccount activity with the corresponding result set.
     */
    public void editBBObjectFinished()
    {
        setResult(BadBudgetApplication.FORM_RESULT_EDIT);
        this.finish();
    }

    /**
     * Callback method called after an invoked DeleteBBObjectTask completes.
     * Finishes this AddAccount activity with the corresponding result set.
     */
    public void deleteBBObjectFinished()
    {
        setResult(BadBudgetApplication.FORM_RESULT_DELETE);
        this.finish();
    }
}
