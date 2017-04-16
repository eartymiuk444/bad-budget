package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseContract;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetBaseActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetChildActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.PredictActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.TrackActivity;
import com.badbudget.erikartymiuk.badbudget.viewobjecttables.MiscActivity;
import com.erikartymiuk.badbudgetlogic.main.Account;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetInvalidValueException;
import com.erikartymiuk.badbudgetlogic.main.CreditCard;
import com.erikartymiuk.badbudgetlogic.main.DebtType;
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.MoneyOwed;
import com.erikartymiuk.badbudgetlogic.main.Payment;
import com.erikartymiuk.badbudgetlogic.main.Prediction;
import com.erikartymiuk.badbudgetlogic.main.SavingsAccount;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Add Misc Payment Activity - activity displays a form for setting up a payment for a debt. The
 * debt information is filled in with data already taken from the add misc form,
 * but can still be edited. Heavily influenced by the Add Savings Activity
 *
 * This form is also used to edit an existing misc to either add a new payment or edit an
 * existing one.
 */
public class AddMiscPayment extends BadBudgetChildActivity
        implements AdapterView.OnItemSelectedListener, DateInputActivity,
        CleanFragmentParent, BBOperationTaskCaller
{
    private static final String progressDialogMessage = "Adding Misc With Payment...";
    private static final String progressDialogMessageUpdate = "Updating Misc With Payment...";
    private static final String progressDialogMessageDelete = "Deleting Misc With Payment...";

    /* Position of the clean items in the clean fragment - Note to reorder would need to change code
    * where items are matched with keys in clean button method */
    private static final int PAYMENT_AMOUNT_CLEAN_POSITION = 1;
    private static final int GOAL_DATE_CLEAN_POSITION = 0;

    /* Codes to identify which date is being set in the callback dateSet */
    private static final int NEXT_DATE_SET_CODE = 0;
    private static final int END_DATE_SET_CODE = 1;
    private static final int GOAL_DATE_SET_CODE = 2;

    /* Debt fields w/o payment */
    private EditText debtName;
    private EditText debtAmount;
    private EditText interestRate;
    private CheckBox quicklookCheckbox;

    /* Payment Fields */

    private EditText paymentAmount;
    private CheckBox payoffCheckbox;

    private Spinner paymentFrequencySpinner;
    private Spinner paymentSourceSpinner;

    private TextView nextDateText;

    private TextView endDateText;
    private CheckBox ongoingCheckbox;

    private TextView goalDateText;

    private Date nextDate;
    private Date endDate;

    private CheckBox setGoalCheckbox;
    private Date goalDate;

    private Frequency paymentFrequency;

    private Date tempGoalDate;
    private double tempPaymentAmount;

    /* Toasts to display for various user actions */
    private Toast alreadyCleanToast;
    private Toast missingCleanDataToast;
    private Toast submitErrorToast;
    private Toast calculateGoalErrorToast;

    /* Fields to keep track whether we are editing and what it is if we are */
    private boolean editing;
    private MoneyOwed editMisc;
    private boolean editHasExistingPayment;

    /* Saved state keys */
    private static final String DEBT_NAME_KEY = "DEBT_NAME";
    private static final String CURRENT_DEBT_KEY = "CURRENT_DEBT_VALUE";
    private static final String INTEREST_RATE_KEY = "INTEREST_RATE";
    private static final String QUICKLOOK_KEY = "QUICKLOOK";
    private static final String PAYMENT_FREQUENCY_KEY = "PAYMENT_FREQUENCY";
    private static final String PAYMENT_AMOUNT_KEY = "PAYMENT_AMOUNT";
    private static final String PAYOFF_KEY = "PAYOFF";
    private static final String SOURCE_ACCOUNT_KEY = "SOURCE_ACCOUNT";
    private static final String NEXT_DATE_KEY = "NEXT_DATE";
    private static final String END_DATE_KEY = "END_DATE";
    private static final String ONGOING_KEY = "ONGOING";
    private static final String SET_GOAL_KEY = "SET_GOAL";
    private static final String GOAL_DATE_KEY = "GOAL_DATE";

    /* Setup Methods */

    /**
     * On create for the AddPayment activity. Gets a hold of any layout views that we need to see/adjust
     * the values of. Sets up the debt values set in the add debt activity from the extras
     * passed with the intent. Keys for these arguments are found in the BadBudgetApplication class.
     * Additional values and setup are also set and done.
     * @param savedInstanceState - saved state to restore
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContent(R.layout.content_add_misc_payment);

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
     * Private helper method to setup necessary views for the debt form; invoked regardless
     * of whether first time setup or restoring from a saved state. Sets necessary conditions
     * for editing also.
     */
    private void setup()
    {
        debtName = (EditText) findViewById(R.id.addMiscPaymentDebtHint);
        debtAmount = (EditText) findViewById(R.id.addMiscPaymentCurrentValueHint);
        interestRate = (EditText) findViewById(R.id.addMiscPaymentInterestRateHint);
        quicklookCheckbox = (CheckBox) findViewById(R.id.addMiscPaymentQuicklookCheckbox);

        paymentAmount = (EditText) findViewById(R.id.addMiscPaymentAmountHint);
        payoffCheckbox = (CheckBox) findViewById(R.id.addMiscPaymentAmountPayoff);

        paymentFrequencySpinner = (Spinner) findViewById(R.id.addMiscPaymentFrequencySpinner);
        paymentSourceSpinner = (Spinner) findViewById(R.id.addMiscPaymentSourceSpinner);

        nextDateText = (TextView) findViewById(R.id.addMiscPaymentNextInput);

        endDateText = (TextView) findViewById(R.id.addMiscPaymentInputEnd);
        ongoingCheckbox = (CheckBox) findViewById(R.id.addMiscPaymentOngoingCheckbox);

        setGoalCheckbox = (CheckBox) findViewById(R.id.addMiscPaymentSetGoal);
        goalDateText = (TextView) findViewById(R.id.addMiscPaymentInputGoal);
        goalDateText.setClickable(false);

        /* Start the dates as not set (i.e should be null)*/
        goalDate = null;
        nextDate = null;
        endDate = null;

        /* Setup our spinners, we will need a listener for our frequency selection as we will update
        * the payment amount, if set, to match the new frequency. We will also need to setup our
        * adapter for our source account spinner. We will only get a hold of accounts that are
        * not savings accounts and put those names into a list of strings which we can then use
        * to reference the accounts by using the name as a key */
        paymentFrequencySpinner.setOnItemSelectedListener(this);

        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        ArrayList<String> accountNames = new ArrayList<String>();
        for (Account a : bbd.getAccounts())
        {
            if (!(a instanceof SavingsAccount))
            {
                accountNames.add(a.name());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, accountNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paymentSourceSpinner.setAdapter(adapter);

        /* Instantiate our toasts */
        alreadyCleanToast = Toast.makeText(this, R.string.add_misc_payment_data_clean_message, Toast.LENGTH_SHORT);
        alreadyCleanToast.setGravity(Gravity.CENTER, 0, 0);

        missingCleanDataToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        missingCleanDataToast.setGravity(Gravity.CENTER, 0, 0);

        submitErrorToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        submitErrorToast.setGravity(Gravity.CENTER, 0, 0);

        calculateGoalErrorToast = Toast.makeText(this, R.string.add_loan_payment_calculate_goal_error, Toast.LENGTH_LONG);
        calculateGoalErrorToast.setGravity(Gravity.CENTER, 0, 0);

        Bundle args = getIntent().getExtras();

        editing = args.getBoolean(BadBudgetApplication.EDIT_KEY);
        String editName = args.getString(BadBudgetApplication.EDIT_OBJECT_ID_KEY);
        editMisc = ((BadBudgetApplication)this.getApplication()).getBadBudgetUserData().getDebtWithName(editName);
        editHasExistingPayment = args.getBoolean(BadBudgetApplication.EDIT_PAYMENT_KEY);

        if (editing)
        {
            this.setToolbarTitle(R.string.edit_misc_payment_title);
            debtName.setEnabled(false);
            debtName.setText(editMisc.name());
            //Enable the delete button

            Button deleteButton = (Button) this.findViewById(R.id.deleteButton);
            deleteButton.setEnabled(true);
            deleteButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Private helper method that sets up the form for being used when the user first enters.
     */
    private void firstTimeSetup()
    {
        //Setup the starting frequency as monthly
        paymentFrequency = Frequency.monthly;
        paymentFrequencySpinner.setSelection(getResources().getInteger(R.integer.monthly_index));

        Bundle args = getIntent().getExtras();

        //Came directly from debts table
        if (editing && editHasExistingPayment)
        {

            //Fill out the basic debt info for the existing debt with a payment
            //We came directly from the debts table
            debtAmount.setText(Double.toString(editMisc.amount()));
            interestRate.setText(Double.toString(editMisc.interestRate()));
            quicklookCheckbox.setChecked(editMisc.quicklook());
            Payment payment = editMisc.payment();
            setupForPaymentEditing(payment);
        }
        //Came from add debt form
        else
        {
            //If we aren't editing then user could have changed debt name
            if (!editing)
            {
                debtName.setText(args.getString(BadBudgetApplication.INPUT_DEBT_NAME_KEY));
            }
            debtAmount.setText(Double.toString(args.getDouble(BadBudgetApplication.INPUT_DEBT_AMOUNT_KEY)));
            interestRate.setText(Double.toString(args.getDouble(BadBudgetApplication.INPUT_DEBT_INTEREST_RATE_KEY)));
            quicklookCheckbox.setChecked(args.getBoolean(BadBudgetApplication.INPUT_DEBT_QUICKLOOK_KEY));
        }

    }

    /**
     * Sets up our form when the user returns to it and the state they left it in should be restored.
     * @param savedInstanceState - the state to restore
     */
    private void savedStateSetup(Bundle savedInstanceState)
    {
        debtName.setText(savedInstanceState.getString(DEBT_NAME_KEY));
        debtAmount.setText(savedInstanceState.getString(CURRENT_DEBT_KEY));
        interestRate.setText(savedInstanceState.getString(INTEREST_RATE_KEY));
        quicklookCheckbox.setChecked(savedInstanceState.getBoolean(QUICKLOOK_KEY));
        setPrepopulatePaymentFreq((Frequency)savedInstanceState.getSerializable(PAYMENT_FREQUENCY_KEY));

        boolean payoff = savedInstanceState.getBoolean(PAYOFF_KEY);
        if (payoff)
        {
            payoffCheckbox.setChecked(true);
            payoffChecked();
        }
        else
        {
            paymentAmount.setText(savedInstanceState.getString(PAYMENT_AMOUNT_KEY));
        }

        setPrepopulateAccountSource(savedInstanceState.getString(SOURCE_ACCOUNT_KEY));
        setNextDate((Date)savedInstanceState.getSerializable(NEXT_DATE_KEY));

        boolean setGoal = savedInstanceState.getBoolean(SET_GOAL_KEY);

        if (setGoal)
        {
            this.setGoalCheckbox.setChecked(true);
            setGoalChecked();
            setGoalDate((Date)savedInstanceState.getSerializable(GOAL_DATE_KEY));
        }
        else
        {
            boolean ongoing = savedInstanceState.getBoolean(ONGOING_KEY);
            if (ongoing)
            {
                ongoingCheckbox.setChecked(true);
                ongoingChecked();
            }
            else
            {
                setEndDate((Date)savedInstanceState.getSerializable(END_DATE_KEY));
            }
        }
    }


    /**
     * Method called when the update task completes. Just returns
     * @param updated - true if an update to bb objects occurred false otherwise, unused
     */
    public void updateTaskCompleted(boolean updated)
    {

    }

    /**
     * Called prior to this activity being destroyed, saves any necessary state to restore upon
     * the users return.
     * @param outState - where to store the saved state.
     */
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putString(DEBT_NAME_KEY, debtName.getText().toString());
        outState.putString(CURRENT_DEBT_KEY, debtAmount.getText().toString());
        outState.putString(INTEREST_RATE_KEY, interestRate.getText().toString());
        outState.putBoolean(QUICKLOOK_KEY, quicklookCheckbox.isChecked());
        outState.putSerializable(PAYMENT_FREQUENCY_KEY, paymentFrequency);
        outState.putString(PAYMENT_AMOUNT_KEY, paymentAmount.getText().toString());
        outState.putBoolean(PAYOFF_KEY, payoffCheckbox.isChecked());
        outState.putString(SOURCE_ACCOUNT_KEY, (String)paymentSourceSpinner.getSelectedItem());
        outState.putSerializable(NEXT_DATE_KEY, nextDate);
        outState.putSerializable(END_DATE_KEY, endDate);
        outState.putBoolean(ONGOING_KEY, ongoingCheckbox.isChecked());
        outState.putBoolean(SET_GOAL_KEY, setGoalCheckbox.isChecked());
        outState.putSerializable(GOAL_DATE_KEY, goalDate);
    }
    private void setupForPaymentEditing(Payment editPayment)
    {
        this.setPrepopulatePaymentFreq(editPayment.frequency());
        if (editPayment.payOff())
        {
            payoffCheckbox.setChecked(true);
            payoffChecked();
        }
        else
        {
            paymentAmount.setText(Double.toString(editPayment.amount()));
        }

        this.setPrepopulateAccountSource(editPayment.sourceAccount().name());

        setNextDate(editPayment.nextPaymentDate());

        this.setPrepopulateGoalEndDates(editPayment);

        //Enable the delete button
        Button deleteButton = (Button) this.findViewById(R.id.deleteButton);
        deleteButton.setEnabled(true);
        deleteButton.setVisibility(View.VISIBLE);
    }

    /**
     * Prepopulates the goal and end dates depending on what values are set in the payment passed
     * @param payment - the payment to use when prepopulating
     */
    private void setPrepopulateGoalEndDates(Payment payment)
    {
        boolean hasGoal = payment.goalDate() != null;
        if (hasGoal)
        {
            this.setGoalCheckbox.setChecked(true);
            setGoalChecked();
            setGoalDate(payment.goalDate());
        }
        else
        {
            boolean ongoing = payment.ongoing();
            if (ongoing)
            {
                ongoingCheckbox.setChecked(true);
                ongoingChecked();
            }
            else
            {
                setEndDate(payment.endDate());
            }
        }
    }

    /**
     * Prepopulates the frequency spinner to the passed frequency.
     * @param frequency
     */
    private void setPrepopulatePaymentFreq(Frequency frequency)
    {
        paymentFrequency = frequency;
        switch (paymentFrequency)
        {
            case oneTime:
                paymentFrequencySpinner.setSelection(BadBudgetApplication.ONE_TIME_INDEX);
                break;
            case daily:
                paymentFrequencySpinner.setSelection(BadBudgetApplication.DAILY_INDEX);
                break;
            case weekly:
                paymentFrequencySpinner.setSelection(BadBudgetApplication.WEEKLY_INDEX);
                break;
            case biWeekly:
                paymentFrequencySpinner.setSelection(BadBudgetApplication.BIWEEKLY_INDEX);
                break;
            case monthly:
                paymentFrequencySpinner.setSelection(BadBudgetApplication.MONTHLY_INDEX);
                break;
            case yearly:
                paymentFrequencySpinner.setSelection(BadBudgetApplication.YEARLY_INDEX);
                break;
        }
    }

    /**
     * Helper method prepopulating the account source spinner to match the name of the passed in
     * account name. Assumes the sourceName is the name of a valid account in our bb data.
     * @param sourceName - the name of the source account to set our spinner to.
     */
    private void setPrepopulateAccountSource(String sourceName)
    {
        ArrayAdapter<String> sourceAccountAdapter = (ArrayAdapter<String>) paymentSourceSpinner.getAdapter();
        int index = sourceAccountAdapter.getCount() - 1;
        while (index >= 0)
        {
            String accountName = sourceAccountAdapter.getItem(index);
            if (accountName.equals(sourceName))
            {
                paymentSourceSpinner.setSelection(index);
                return;
            }
            index--;
        }
        //It is an error to reach here
    }

    /* Methods handling non-trivial updates to input values made by user or programmatically (such as
    * when setting up the form for the first time) */

    /**
     * Should be called whenever the payoff checkbox is checked by the user or programmatically.
     * Disables the payment amount field, clears any amount, and sets the payoff hint.
     */
    private void payoffChecked()
    {
        paymentAmount.setEnabled(false);
        paymentAmount.setText("");
        paymentAmount.setHint(R.string.add_misc_payment_amount_payoff);
    }

    /**
     * Should be called whenever the payoff checkbox is unchecked. Enables the payment amount field,
     * and sets the amount hint.
     */
    private void payoffUnchecked()
    {
        paymentAmount.setEnabled(true);
        paymentAmount.setHint(R.string.add_misc_payment_amount_hint);
    }

    /**
     * Should be called whenever the ongoing checkbox is checked. Disables the end date field, clears
     * any end date, and sets the ongoing hint
     */
    private void ongoingChecked()
    {
        endDateText.setClickable(false);
        clearEndDate();
        endDateText.setHint(getString(R.string.add_misc_payment_ongoing));
    }

    /**
     * Should be called whenever the ongoing checkbox is unchecked. Enables the end date field, and sets
     * the hint to the date hint.
     */
    private void ongoingUnchecked()
    {
        endDateText.setClickable(true);
        endDateText.setHint(R.string.add_misc_payment_date_hint);
    }

    /**
     * Should be called whenever the set goal checkbox is checked. Disables the payoff checkbox,
     * disables the end date input fields, and enables the goal date input fields.
     */
    private void setGoalChecked()
    {
        disablePayoffCheckbox();
        disableEndDateInput();
        enableGoalDateInput();
    }

    /**
     * Should be called whenever the set goal checkbox is unchecked. Disables the goal date input fields,
     * enables the payoff checkbox, and enables the end date input fields.
     */
    private void setGoalUnchecked()
    {
        disableGoalDateInput();
        enablePayoffCheckbox();
        enableEndDateInput();
    }

    /**
     * Disables the payoff checkbox. It is unchecked and can no longer be viewed or toggled.
     */
    private void disablePayoffCheckbox()
    {
        this.payoffCheckbox.setChecked(false);
        payoffUnchecked();

        this.payoffCheckbox.setEnabled(false);
        this.payoffCheckbox.setVisibility(View.INVISIBLE);
    }

    /**
     * Enables the payoff checkbox. It can now be viewed and toggled.
     */
    private void enablePayoffCheckbox()
    {
        this.payoffCheckbox.setEnabled(true);
        this.payoffCheckbox.setVisibility(View.VISIBLE);
    }

    /**
     * Disables the end date fields. The end date is cleared and no end date can be entered. The
     * ongoing checkbox cannot be viewed is unchecked and can no longer be toggled.
     */
    private void disableEndDateInput()
    {
        this.ongoingCheckbox.setClickable(false);
        this.ongoingCheckbox.setChecked(false);
        ongoingUnchecked();
        this.ongoingCheckbox.setVisibility(View.INVISIBLE);

        endDateText.setClickable(false);
        clearEndDate();
        endDateText.setHint(R.string.add_misc_payment_end_date_disabled);
    }

    /**
     * Enables the end date fields. The end date can be set. The ongoing checkbox can be viewed and
     * toggled.
     */
    private void enableEndDateInput()
    {
        this.ongoingCheckbox.setClickable(true);
        this.ongoingCheckbox.setVisibility(View.VISIBLE);
        this.endDateText.setClickable(true);
        this.endDateText.setHint(R.string.add_savings_end_date_hint);
    }

    /**
     * Disables the goal date fields. The goal date is cleared and can not be set.
     * The set goal checkbox can still be toggled.
     */
    private void disableGoalDateInput()
    {
        this.goalDateText.setClickable(false);
        clearGoalDate();
    }

    /**
     * Enables goal date fields. The goal date can be set.
     */
    private void enableGoalDateInput()
    {
        this.goalDateText.setClickable(true);
    }

    /**
     * Sets the goal date to the passed in date, including updating any necessary data structures
     * and any necessary views
     * @param goalDate
     */
    private void setGoalDate(Date goalDate)
    {
        this.goalDate = goalDate;
        this.goalDateText.setText(BadBudgetApplication.dateString(goalDate));
    }

    /**
     * Sets the end date to the passed in date, including updating any necessary data structures
     * and any necessary views
     * @param endDate
     */
    private void setEndDate(Date endDate)
    {
        this.endDate = endDate;
        this.endDateText.setText(BadBudgetApplication.dateString(endDate));
    }

    /**
     * Sets the next date to the passed in date, including updating any necessary data structures
     * and any necessary views
     * @param nextDate
     */
    private void setNextDate(Date nextDate)
    {
        this.nextDate = nextDate;
        this.nextDateText.setText(BadBudgetApplication.dateString(nextDate));
    }

    /**
     * Clears the goal date, including setting/clearing any necessary data structures and any necessary
     * views. The text is cleared, showing the currently set hint.
     */
    private void clearGoalDate()
    {
        this.goalDate = null;
        this.goalDateText.setText("");
    }

    /**
     * Clears the end date, including setting/clearing any necessary data structures and any necessary
     * views. The text is cleared, showing the currently set hint.
     */
    private void clearEndDate()
    {
        this.endDate = null;
        this.endDateText.setText("");
    }

    /**
     * Clears the next date, including setting/clearing any necessary data structures and any necessary
     * views. The text is cleared, showing the currently set hint.
     */
    private void clearNextDate()
    {
        this.nextDate = null;
        this.nextDateText.setText("");
    }

    /* Methods for when the user takes some action (button presses, checkbox clicks, dates clicked */

    /**
     * Method called when the next date's text is clicked. Initializes and shows to the user a
     * date picker fragment.
     * @param view - the text view that was clicked
     */
    public void nextDateClicked(View view)
    {
        dateClicked(AddMiscPayment.NEXT_DATE_SET_CODE, "nextDatePicker");
    }

    /**
     * Method called when the end date's text is clicked. Initializes and shows to the user a
     * date picker fragment.
     * @param view - the text view that was clicked
     */
    public void endDateClicked(View view)
    {
        dateClicked(AddMiscPayment.END_DATE_SET_CODE, "endDatePicker");
    }

    /**
     * Method called when the goal date's text is clicked. Initializes and shows to the user a
     * date picker fragment.
     * @param view - the text view that was clicked
     */
    public void goalDateClicked(View view)
    {
        dateClicked(AddMiscPayment.GOAL_DATE_SET_CODE, "goalDatePicker");
    }

    /**
     * Private helper method. Called when one of the three dates is clicked on indicating the user
     * wishes to set a date. Passed a code indicating which date was clicked and a tag identifying
     * the fragment that will be created for date picking. Uses the today date from the BB application class
     * @param code - the code identifying which date is being chosen (constants in AddMiscPaymentActivity)
     * @param tag - an identifying tag for the fragment that will be created
     */
    private void dateClicked(int code, String tag)
    {
        Bundle args = new Bundle();
        args.putInt(DatePickerFragment.RETURN_CODE_KEY, code);

        Date today = ((BadBudgetApplication)getApplication()).getToday();
        Calendar todayCal = Calendar.getInstance();
        todayCal.setTime(today);

        args.putInt(DatePickerFragment.TODAY_YEAR_KEY, todayCal.get(Calendar.YEAR));
        args.putInt(DatePickerFragment.TODAY_MONTH_KEY, todayCal.get(Calendar.MONTH));
        args.putInt(DatePickerFragment.TODAY_DAY_KEY, todayCal.get(Calendar.DAY_OF_MONTH));

        args.putInt(DatePickerFragment.CURRENT_CHOSEN_YEAR_KEY, todayCal.get(Calendar.YEAR));
        args.putInt(DatePickerFragment.CURRENT_CHOSEN_MONTH_KEY, todayCal.get(Calendar.MONTH));
        args.putInt(DatePickerFragment.CURRENT_CHOSEN_DAY_KEY, todayCal.get(Calendar.DAY_OF_MONTH));

        DatePickerFragment datePickerFragment = new DatePickerFragment();
        datePickerFragment.setArguments(args);
        datePickerFragment.show(getSupportFragmentManager(), tag);
    }

    /**
     * Method called when the payoff checkbox is checked or unchecked. If checked updates the hint value
     * to indicate that the payment amount will now be equal to the balance on the payment date and then
     * clears any input in the amount field so that the hint is displayed. Also makes it so that the amount
     * field is disabled and cannot be edited. If unchecked then the hint is updated to be the default hint
     * for inputing the amount and the text field is enabled.
     *
     * @param view - the checkbox view that was clicked
     */
    public void payoffCheckboxChecked(View view)
    {
        boolean checked = payoffCheckbox.isChecked();
        if (checked)
        {
            payoffChecked();
        }
        else
        {
            payoffUnchecked();
        }
    }

    /**
     * Method called when the ongoing checkbox is clicked, either checking or unchecking it.
     * If the checkbox is checked this method disables the endDate date picker and displays a
     * message indicating that the end date will now be ongoing. If unchecked the date picker is
     * enabled and the text is set to indicate that the user should pick an end date.
     * @param view - the ongoing checkbox view
     */
    public void ongoingCheckboxChecked(View view)
    {
        boolean checked = ongoingCheckbox.isChecked();
        if (checked)
        {
            ongoingChecked();
        }
        else
        {
            ongoingUnchecked();
        }
    }

    /**
     * This method is called when the setGoal checkbox is clicked. This will make the
     * goal date field editable if the checkbox was checked and disable it if unchecked.
     * If unchecked the goal date field is also cleared (instance set to null, text is empty).
     * Additionally if the checkbox is checked then the end date field is no longer editable and
     * displays a message indicating that it will match the goal date. The end date instance variable
     * is also set to null. The ongoing checkbox is
     * unchecked, disabled, and set to be invisible.
     *
     * Additionally if checked the payoff checkbox is unchecked, made invisible, and disabled with the payment amount
     * field enabled with the default hint. When unchecked the payoff checkbox is made visible again and enabled
     * @param view - the checkbox that was toggled
     */
    public void setGoalChecked(View view)
    {
        boolean goalChecked = this.setGoalCheckbox.isChecked();
        if (goalChecked)
        {
            setGoalChecked();
        }
        //goalUnchecked
        else
        {
            setGoalUnchecked();
        }
    }

    /**
     * Method called when the clean button is pressed. This method checks for consistency among the
     * payment amount (at a chosen frequency) and the goal date with the other necessary values used being
     * the current value, the next date, and the frequency.
     * If the two values are not consistent then a dialog pops up giving
     * the user two choices of updating one or the other to make them consistent. If only one value is entered
     * then the second is auto-filled with a toast indicating that this was done.
     * If we are missing one or more necessary values a toast is shown displaying to the user what is missing. If the values
     * are both entered and are already consistent then a toast pops up indicating this.
     * @param view
     */
    public void addMiscPaymentCleanClick(View view)
    {
        /* String inputs we are interested in, we consider anything other then the empty string as
        the user having input something for that field */
        String currentValueString = this.debtAmount.getText().toString();
        String interestRateString = this.interestRate.getText().toString();
        String paymentAmountString = this.paymentAmount.getText().toString();

        /* All the fields - we may need. */
        boolean currentValueEntered = !currentValueString.equals("");
        boolean interestRateEntered = !interestRateString.equals("");
        boolean paymentAmountEntered = !paymentAmountString.equals("");
        boolean nextDateEntered = nextDate != null;

        boolean goalCheckboxChecked = this.setGoalCheckbox.isChecked();
        boolean goalDateEntered = goalDate != null;
        boolean goalSet = goalCheckboxChecked && goalDateEntered;
        //TODO incorporate one time frequencies into this

        //This is good we will simply check for consistency and give options if not consistent
        boolean allEntered = currentValueEntered && paymentAmountEntered && goalCheckboxChecked && nextDateEntered && goalSet;

        //This is not ok. User needs to input more data
        boolean missingKey = !currentValueEntered || !interestRateEntered || !nextDateEntered || !goalCheckboxChecked;
        boolean missingPaymentAndGoal = !paymentAmountEntered && goalCheckboxChecked && !goalDateEntered;

        //This is ok. If only missing one of these then the other is auto-filled
        boolean missingOnlyPaymentAmount = !missingKey && goalDateEntered && !paymentAmountEntered;
        boolean missingOnlyGoalDate = !missingKey && !goalDateEntered && paymentAmountEntered;

        //If everything has been entered then we need to check to see if the payment amount and goal
        //date are consistent and offer the user choices on how to fix them if they are not.
        if (allEntered)
        {
            Date currentGoalDate = this.goalDate;
            Date correctGoalDate = calculateGoalDate();

            if (correctGoalDate == null)
            {
                //goal date could not be found.
                if (!calculateGoalErrorToast.getView().isShown())
                {
                    calculateGoalErrorToast.show();
                }
                return;
            }

            double currentPaymentAmount = Double.parseDouble(this.paymentAmount.getText().toString());
            double correctPaymentAmount = calculatePaymentAmount();

            double paymentAmountDiff = currentPaymentAmount - correctPaymentAmount;
            boolean paymentAmountGood = paymentAmountDiff >= 0 && paymentAmountDiff < 0.01;

            boolean dataConsistent = Prediction.datesEqualUpToDay(currentGoalDate, correctGoalDate) || paymentAmountGood;

            if (!dataConsistent)
            {
                //Keep track of the correct goal date in case the user indicates this is what they want
                //to update.
                this.tempGoalDate = correctGoalDate;
                this.tempPaymentAmount = Math.ceil(correctPaymentAmount*10*10)/(10.0*10.0);

                //Pass the date needed so the user can see what the old value was and what the updated
                //value would be for the three options
                Bundle bundle = new Bundle();
                bundle.putInt(CleanFragment.NUM_ITEMS_KEY, 2);

                bundle.putString(CleanFragment.POSITION_ONE_TITLE_KEY, getString(R.string.add_misc_payment_goal_date_prompt));
                bundle.putString(CleanFragment.POSITION_ONE_OLD_KEY, BadBudgetApplication.dateString(currentGoalDate));
                bundle.putString(CleanFragment.POSITION_ONE_NEW_KEY, BadBudgetApplication.dateString(correctGoalDate));

                bundle.putString(CleanFragment.POSITION_TWO_TITLE_KEY, getString(R.string.add_misc_payment_amount_prompt));
                bundle.putString(CleanFragment.POSITION_TWO_OLD_KEY, Double.toString(currentPaymentAmount));
                bundle.putString(CleanFragment.POSITION_TWO_NEW_KEY, Double.toString(tempPaymentAmount));

                CleanFragment cleanFragment = new CleanFragment();
                cleanFragment.setArguments(bundle);
                cleanFragment.show(getSupportFragmentManager(), "cleanFragment");
            }
            else
            {
                if (!alreadyCleanToast.getView().isShown())
                {
                    alreadyCleanToast.setText(getString(R.string.add_savings_data_clean_message));
                    alreadyCleanToast.show();
                }
            }
        }
        //Will auto fill payment amount in this case and show a toast to the user
        else if (missingOnlyPaymentAmount)
        {
            double correctPaymentAmount = calculatePaymentAmount();
            this.paymentAmount.setText(Double.toString(correctPaymentAmount));

            CharSequence text = getString(R.string.add_misc_payment_auto_set_payment_amount);
            text = text + " " + correctPaymentAmount;
            Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
            toast.show();
        }
        //Will auto fill goal date in this case and show a toast to the user
        else if (missingOnlyGoalDate)
        {
            Date correctGoalDate = calculateGoalDate();

            if (correctGoalDate == null)
            {
                //goal date could not be found.
                if (!calculateGoalErrorToast.getView().isShown())
                {
                    calculateGoalErrorToast.show();
                }
                return;
            }

            //To set the date we only need to do these two things. No need to check the next date
            //as the calculateGoalDate method already takes the next date into account
            setGoalDate(correctGoalDate);

            CharSequence text = getString(R.string.add_misc_payment_auto_set_goal_date);
            text = text + " " + BadBudgetApplication.dateString(correctGoalDate);
            Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
            toast.show();
        }
        //Can't clean unless the set goal checkbox is checked
        else if (!goalCheckboxChecked)
        {
            String goalCheckboxUnchecked = getString(R.string.add_misc_payment_data_goal_checkbox_unchecked);
            if (!missingCleanDataToast.getView().isShown())
            {
                missingCleanDataToast.setText(goalCheckboxUnchecked);
                missingCleanDataToast.show();
            }
        }
        //We cannot clean as we are missing key information so we display our message indicating what we
        //are missing
        else
        {
            //Construct our message for our missing data toast
            String missingMessage = getString(R.string.add_misc_payment_data_clean_missing);
            if (missingPaymentAndGoal)
            {
                missingMessage+="\n";
                missingMessage+= (getString(R.string.add_misc_payment_amount_prompt) + " " +
                        getString(R.string.add_misc_payment_data_clean_and_or) + " " +
                        getString(R.string.add_misc_payment_goal_date_prompt));
            }
            if (!currentValueEntered)
            {
                missingMessage+="\n";
                missingMessage+=getString(R.string.add_misc_payment_value_prompt);
            }
            if (!interestRateEntered)
            {
                missingMessage+="\n";
                missingMessage+=getString(R.string.add_misc_payment_interest_rate_prompt);
            }

            if (!nextDateEntered)
            {
                missingMessage+="\n";
                missingMessage+=getString(R.string.add_misc_payment_next_prompt);
            }

            if (!missingCleanDataToast.getView().isShown())
            {
                missingCleanDataToast.setText(missingMessage);
                missingCleanDataToast.show();
            }

        }
    }

    /**
     * Method called when the user presses the submit button. Verifies we have all necessary input values
     * and that they are consistent. If they are not a toast is displayed indicating what the error is.
     * If we do then a task is kicked off that adds the input into memory and into the database as
     * a new misc object with a payment.
     * @param view
     */
    public void addMiscPaymentSubmitClick(View view)
    {
        if (verifyValues())
        {
            //Prep for creation of the in memory misc object

            //Get debt specific inputs
            String name = debtName.getText().toString();
            double debt = Double.parseDouble(debtAmount.getText().toString());
            double interestRateDouble = Double.parseDouble(interestRate.getText().toString());
            boolean quicklook = quicklookCheckbox.isChecked();

            //Get payments specific inputs
            Frequency pFreq = paymentFrequency;

            double pAmount = -1;
            boolean payoff = payoffCheckbox.isChecked();
            if (!payoff)
            {
                pAmount = Double.parseDouble(paymentAmount.getText().toString());
            }

            BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
            Account pSource = bbd.getAccountWithName((String)this.paymentSourceSpinner.getSelectedItem());

            Date pNextDate = nextDate;
            Date pEndDate = endDate;
            Date pGoalDate = goalDate;

            boolean pOngoing = ongoingCheckbox.isChecked();

            //Setup our misc with a payment
            try
            {
                MoneyOwed misc = new MoneyOwed(name, debt, quicklook, interestRateDouble);

                //If the goal date is set then endDate is null so need to set what we pass for endDate to the goal date.
                if (goalDate != null)
                {
                    pEndDate = pGoalDate;
                }
                Payment payment = new Payment(pAmount, payoff, pFreq, pSource, pNextDate, pOngoing, pEndDate, misc, pGoalDate);
                misc.setupPayment(payment);
                //Prep for creation of the database object
                ContentValues values = new ContentValues();
                values.put(BBDatabaseContract.Debts.COLUMN_NAME, name);
                values.put(BBDatabaseContract.Debts.COLUMN_AMOUNT, debt);
                values.put(BBDatabaseContract.Debts.COLUMN_INTEREST_RATE, interestRateDouble);
                values.put(BBDatabaseContract.Debts.COLUMN_QUICK_LOOK, BBDatabaseContract.dbBooleanToInteger(quicklook));
                values.put(BBDatabaseContract.Debts.COLUMN_DEBT_TYPE, BBDatabaseContract.dbDebtTypeToInteger(DebtType.Other));

                values.put(BBDatabaseContract.Debts.COLUMN_PAYMENT_AMOUNT, pAmount);
                values.put(BBDatabaseContract.Debts.COLUMN_PAYMENT_SOURCE, pSource.name());
                values.put(BBDatabaseContract.Debts.COLUMN_PAYMENT_FREQUENCY, BBDatabaseContract.dbFrequencyToInteger(pFreq));
                values.put(BBDatabaseContract.Debts.COLUMN_PAYMENT_NEXT_PAYMENT, BBDatabaseContract.dbDateToString(pNextDate));
                values.put(BBDatabaseContract.Debts.COLUMN_PAYMENT_END_DATE, BBDatabaseContract.dbDateToString(pEndDate));
                values.put(BBDatabaseContract.Debts.COLUMN_PAYMENT_GOAL_DATE, BBDatabaseContract.dbDateToString(pGoalDate));
                values.put(BBDatabaseContract.Debts.COLUMN_PAYMENT_ONGOING, BBDatabaseContract.dbBooleanToInteger(pOngoing));
                values.put(BBDatabaseContract.Debts.COLUMN_PAYMENT_PAYOFF, BBDatabaseContract.dbBooleanToInteger(payoff));

                if (editing)
                {
                    EditBBObjectTask task = new EditBBObjectTask(this, progressDialogMessage, misc, values, this, BBObjectType.DEBT_MISC);
                    task.execute();
                }
                else
                {
                    AddBBObjectTask task = new AddBBObjectTask(this, progressDialogMessageUpdate, misc, values, this, BBObjectType.DEBT_MISC);
                    task.execute();
                }
            }
            catch(BadBudgetInvalidValueException e)
            {
                //TODO - handle
                e.printStackTrace();
            }
        }
    }

    /**
     * Behaves as if the user clicked the back button
     * @param view
     */
    public void addMiscPaymentCancelClick(View view)
    {
        this.onBackPressed();
    }

    /**
     * Method called when the delete button is pressed, visible only when editing an exisint account.
     * Kicks off a task to delete the Misc we are editing from our bb data.
     * @param view
     */
    public void deleteClick(View view)
    {
        DeleteBBObjectTask task = new DeleteBBObjectTask(this, progressDialogMessageDelete, editMisc.name(), this, BBObjectType.DEBT_MISC);
        task.execute();
    }

    /* Callback methods for when a task is finished or a listener method is called */

    /**
     * Interface implementation for the DateInputActivity interface. Callback method when a user has
     * chosen a date from a datePickerFragment. The return code is used to determine which date the
     * user was picking for.
     * @param year -  the year chosen
     * @param month - the month chosen
     * @param day - the day chosen
     * @param returnCode - a code that was originally passed into the fragment and that is now
     *                   passed back to us to identify which date the user was picking.
     */
    public void dateSet(int year, int month, int day, int returnCode)
    {
        switch (returnCode)
        {
            case NEXT_DATE_SET_CODE: {
                nextDateSet(year, month, day);
                break;
            }
            case END_DATE_SET_CODE: {
                endDateSet(year, month, day);
                break;
            }
            case GOAL_DATE_SET_CODE: {
                goalDateSet(year, month, day);
                break;
            }
        }
    }

    /**
     * Method called when the next date has been chosen by the user from the date fragment. Sets the
     * next date (i.e. the instance date object) and the text for the nextDate view to match what
     * the user chose. Also does some checking against any currently set end date xor goal date. If
     * one of these dates occurs before the next date set then that date is cleared. (instance set to
     * null and text emptied)
     * @param year - the year chosen
     * @param monthOfYear - the month chosen
     * @param dayOfMonth - the day chosen
     */
    public void nextDateSet(int year, int monthOfYear, int dayOfMonth)
    {
        Calendar tempCalendar = new GregorianCalendar(year, monthOfYear, dayOfMonth);
        setNextDate(tempCalendar.getTime());

        if (endDate != null && Prediction.numDaysBetween(nextDate, endDate) < 0)
        {
            clearEndDate();
        }
        else if (goalDate != null && Prediction.numDaysBetween(nextDate, goalDate) < 0)
        {
            clearGoalDate();
        }
    }

    /**
     * Method called when the end date has been set. Sets the end date object and text to match
     * what the user chose. Checks this date against the next date and if the next date occurs
     * after the end date it is cleared.
     * @param year - year chosen
     * @param month - month chosen
     * @param day - day chosen
     */
    public void endDateSet(int year, int month, int day)
    {
        Calendar tempCalendar = new GregorianCalendar(year, month, day);
        setEndDate(tempCalendar.getTime());

        //Check the next date and make sure it comes before end date. If it doesn't
        //we clear the next date.
        if (nextDate != null && Prediction.numDaysBetween(nextDate, endDate) < 0)
        {
            //Next date occurs after end date so we clear the next date
            clearNextDate();
        }
    }

    /**
     * Method called when the goal date is set by the user. Sets the goal date instance object and
     * goal date view text to match what was set. Also checks the goal date against the next date
     * if set and clears it if it comes after the goal date.
     * @param year - chosen year
     * @param month - chosen month
     * @param day - chosen day
     */
    public void goalDateSet(int year, int month, int day)
    {
        Calendar tempCalendar = new GregorianCalendar(year, month, day);
        setGoalDate(tempCalendar.getTime());

        if (nextDate != null && Prediction.numDaysBetween(nextDate, goalDate) < 0)
        {
            clearNextDate();
        }
    }

    /**
     * Implementation of the CleanFragmentParent method. This is the callback method used when a user
     * has made a selection and chosen clean for that position in a clean fragment.
     * @param selectionPosition - the position chosen that indicates which item the user wants cleaned
     */
    public void cleanSelection(int selectionPosition)
    {
        switch(selectionPosition)
        {
            case PAYMENT_AMOUNT_CLEAN_POSITION:
            {
                cleanPaymentAmount();
                break;
            }
            case GOAL_DATE_CLEAN_POSITION:
            {
                cleanGoalDate();
                break;
            }
        }
    }

    /**
     * Callback method to call when the user has chosen to clean the goal date after pressing the
     * clean button and being presented with choices. Uses the saved instance variable.
     */
    public void cleanGoalDate()
    {
        setGoalDate(tempGoalDate);
    }

    /**
     * Callback method to call when the user has chosen to clean the payment amount after being
     * presented with the clean fragment and its choices.
     */
    public void cleanPaymentAmount()
    {
        paymentAmount.setText(Double.toString(tempPaymentAmount));
    }

    /**
     * Implementation method for the spinner listener. This method is called when an item is selected
     * in our frequency dropdown. This uses the strings defined in our string resource file to
     * convert the selected dropdown string to a frequency that can be used to create our
     * SavingsAccount. Then if applicable (i.e the frequency changed and neither the old or new frequency
     * was a one time frequency and the the contribution amount field wasn't empty) we auto update
     * the amount to be the corresponding amount at the new frequency (same as the toggle function
     * in our budget item list). A one time frequency simply retains its the current amount in the
     * contribution amount field.
     * @param parent - the Adapter view where the selection happened
     * @param view - the view within the AdapterView that was clicked
     * @param position - the position of the view in the adapter
     * @param id - the row id of the item that is selected
     */
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        Frequency oldFrequency = this.paymentFrequency;
        String frequencyText = ((TextView)view).getText().toString();
        if (frequencyText.equals(this.getResources().getText(R.string.one_time).toString()))
        {
            this.paymentFrequency = Frequency.oneTime;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.daily).toString()))
        {
            this.paymentFrequency = Frequency.daily;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.weekly).toString()))
        {
            this.paymentFrequency = Frequency.weekly;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.bi_weekly).toString()))
        {
            this.paymentFrequency = Frequency.biWeekly;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.monthly).toString()))
        {
            this.paymentFrequency = Frequency.monthly;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.yearly).toString()))
        {
            this.paymentFrequency = Frequency.yearly;
        }

        boolean oneTimeFrequency = (oldFrequency == Frequency.oneTime) || (this.paymentFrequency == Frequency.oneTime);
        boolean frequencyChanged = oldFrequency != this.paymentFrequency;

        if (!oneTimeFrequency && frequencyChanged)
        {
            String paymentAmountString = this.paymentAmount.getText().toString();
            if (!payoffCheckbox.isChecked() && !paymentAmountString.equals(""))
            {
                double currAmount = Double.parseDouble(paymentAmountString);
                double convertedAmount = Prediction.toggle(currAmount, oldFrequency, this.paymentFrequency);
                this.paymentAmount.setText(Double.toString(convertedAmount));
            }
        }
    }

    //TODO - implement
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**
     * Callback indicating the invoked add task has completed.
     */
    public void addBBObjectFinished()
    {
        setResult(BadBudgetApplication.FORM_RESULT_ADD);
        this.finish();
    }

    /**
     * Callback indicating the invoked edit task has completed
     */
    public void editBBObjectFinished()
    {
        setResult(BadBudgetApplication.FORM_RESULT_EDIT);
        this.finish();
    }

    /**
     * Callback indicating the invoked delete task has completed
     */
    public void deleteBBObjectFinished()
    {
        setResult(BadBudgetApplication.FORM_RESULT_DELETE);
        this.finish();
    }

    /* The clean logic methods */

    /**
     * Private helper method that assumes all values needed to be set by the user to calculate
     * a payment amount that is consistent have been set. Uses those set values and returns
     * that payment amount.
     * @return - a payment amount consistent with the currently set input values
     */
    private Double calculatePaymentAmount()
    {
        Double currentAmount = Double.parseDouble(this.debtAmount.getText().toString());
        Double interestRate = Double.parseDouble(this.interestRate.getText().toString());

        Calendar todayCal = Calendar.getInstance();
        todayCal.setTime(((BadBudgetApplication)this.getApplication()).getToday());
        todayCal.add(Calendar.DAY_OF_YEAR, 1);
        Date interestStart = todayCal.getTime();

        int numInterestDays = Prediction.numDaysBetween(interestStart, this.nextDate);
        //Next date could be today which means there are 0 days (-1 days) before the first payment date
        //as interest for today was already calculated (or assumed to be) and interest for the day of the
        //next payment occurs after the payment is made (taken into account within the method findGoalDateCompoundInterest)
        if (numInterestDays == -1)
        {
            numInterestDays = 0;
        }

        Double correctPaymentAmount = Prediction.findPaymentAmountCompoundInterest(this.nextDate, numInterestDays, this.goalDate, this.paymentFrequency, currentAmount, interestRate);
        return correctPaymentAmount;
    }

    /**
     * Private helper method that assumes all fields that we need set are set in order to calculate
     * a goal date. Returns a goal date consistent with all the values currently set or null
     * if a reasonable goal cannot be found.
     * @return - a consistent goal date
     */
    private Date calculateGoalDate()
    {
        Double currentAmount = Double.parseDouble(this.debtAmount.getText().toString());
        Double interestRate = Double.parseDouble(this.interestRate.getText().toString());
        Double paymentAmount = Double.parseDouble(this.paymentAmount.getText().toString());

        Calendar todayCal = Calendar.getInstance();
        todayCal.setTime(((BadBudgetApplication)this.getApplication()).getToday());
        todayCal.add(Calendar.DAY_OF_YEAR, 1);
        Date interestStart = todayCal.getTime();

        int numInterestDays = Prediction.numDaysBetween(interestStart, this.nextDate);
        //Next date could be today which means there are 0 days (-1 days) before the first payment date
        //as interest for today was already calculated (or assumed to be) and interest for the day of the
        //next payment occurs after the payment is made (taken into account within the method findGoalDateCompoundInterest)
        if (numInterestDays == -1)
        {
            numInterestDays = 0;
        }

        //Calculate our limit date
        todayCal = Calendar.getInstance();
        todayCal.setTime(((BadBudgetApplication)this.getApplication()).getToday());
        Calendar maxCal = new GregorianCalendar(todayCal.get(Calendar.YEAR)+getResources().getInteger(R.integer.predict_years),
                todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH));
        Date limitDate = maxCal.getTime();

        Date correctGoalDate = Prediction.findGoalDateCompoundInterest(this.nextDate, numInterestDays , paymentAmount, this.paymentFrequency, currentAmount, interestRate, limitDate);
        return correctGoalDate;
    }

    /* Error checking methods */

    /**
     * Private helper method to check that all required fields have input values and that they
     * are clean and consistent input values. If missing or erroneous values are found a toast
     * is shown indicating what is missing or if the data needs to be cleaned. Return true if
     * everything is found to be correct and ready for submitting to the database and memory.
     * @return true if the values are all entered and consistent.
     *
     * TODO verify that interest rate isn't negative (other values also?) 3/14/2017
     */
    private boolean verifyValues()
    {
        //We will construct our toast string showing any errors
        String errorToastString = "";
        String missing = this.getString(R.string.add_misc_payment_submit_missing);

        //Things we always need
        boolean accountNameEntered = !this.debtName.getText().toString().equals("");
        boolean currentValueEntered = !this.debtAmount.getText().toString().equals("");
        boolean paymentFrequencyEntered = paymentFrequencySpinner.getSelectedItem() != null;
        boolean paymentAmountEntered = !paymentAmount.getText().toString().equals("") || payoffCheckbox.isChecked();
        boolean sourceAccountSet = paymentSourceSpinner.getSelectedItem() != null;
        boolean nextDateSet = nextDate != null;

        boolean missingBasic = !accountNameEntered || !currentValueEntered ||
                !paymentFrequencyEntered || !paymentAmountEntered || !sourceAccountSet || !nextDateSet;
        if (missingBasic)
        {
            errorToastString+=missing;
        }

        if (!accountNameEntered)
        {
            errorToastString+="\n";
            errorToastString+=this.getString(R.string.add_misc_payment_debt_prompt);
        }
        if (!currentValueEntered)
        {
            errorToastString+="\n";
            errorToastString+=this.getString(R.string.add_misc_payment_value_prompt);
        }
        if (!paymentFrequencyEntered)
        {
            errorToastString+="\n";
            errorToastString+=this.getString(R.string.add_misc_payment_frequency_prompt);
        }
        if (!paymentAmountEntered)
        {
            errorToastString+="\n";
            errorToastString+=this.getString(R.string.add_misc_payment_amount_prompt);
        }
        if (!sourceAccountSet)
        {
            errorToastString+="\n";
            errorToastString+=this.getString(R.string.add_misc_payment_source_accout_prompt);
        }
        if (!nextDateSet)
        {
            errorToastString+="\n";
            errorToastString+=this.getString(R.string.add_misc_payment_next_prompt);
        }

        boolean goalChecked = this.setGoalCheckbox.isChecked();
        if (goalChecked)
        {
            //If the user indicated they wanted a goal to be set we need the goal date
            boolean goalDateSet = goalDate != null;

            if (!goalDateSet)
            {
                if (!missingBasic)
                {
                    errorToastString+=this.getString(R.string.add_misc_payment_submit_missing);
                }
                errorToastString+="\n";
                errorToastString+=this.getString(R.string.add_misc_payment_goal_date_prompt);
            }
            //If we aren't missing anything then check to see if the data is clean.
            if (!missingBasic && goalDateSet)
            {
                Date currentGoalDate = this.goalDate;
                Date correctGoalDate = calculateGoalDate();
                if (correctGoalDate == null)
                {
                    //goal date could not be found.
                    if (!calculateGoalErrorToast.getView().isShown())
                    {
                        calculateGoalErrorToast.show();
                    }
                    return false;
                }

                boolean dataConsistent = Prediction.datesEqualUpToDay(currentGoalDate, correctGoalDate);

                //TODO - this should be condidition when goalAmount and contributionAmount can be chosen
                //currentGoalAmount == correctGoalAmount && Prediction.datesEqualUpToDay(currentGoalDate, correctGoalDate) &&
                //currentContributionAmount == correctContributionAmount;

                if (!dataConsistent)
                {
                    errorToastString+=this.getString(R.string.add_misc_payment_submit_data_dirty);
                }
            }
        }
        else
        {
            //If the user didn't want a goal set then we require either ongoing or an end date
            boolean ongoing = this.ongoingCheckbox.isChecked();
            if (!ongoing)
            {
                boolean endDateSet = endDate != null;
                if (!endDateSet)
                {
                    if (!missingBasic)
                    {
                        errorToastString+=this.getString(R.string.add_misc_payment_submit_missing);
                    }
                    errorToastString+="\n";
                    errorToastString+=this.getString(R.string.add_misc_payment_end_prompt);
                }
            }
        }

        //Make sure the account name is unique if we are not editing
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        if (!editing && bbd.getDebtWithName(this.debtName.getText().toString()) != null)
        {
            if (errorToastString.equals("")) {
                errorToastString += this.getString(R.string.add_misc_payment_duplicate_debt_name);
            }
            else
            {
                errorToastString+="\n\n";
                errorToastString += this.getString(R.string.add_misc_payment_duplicate_debt_name);
            }
        }

        boolean errorFound = !errorToastString.equals("");

        if (errorFound && !submitErrorToast.getView().isShown())
        {
            submitErrorToast.setText(errorToastString);
            submitErrorToast.show();
        }

        return !errorFound;
    }
}
