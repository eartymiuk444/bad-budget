package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;


import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentActivity;
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
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetBaseActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetChildActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.PredictActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.TrackActivity;
import com.badbudget.erikartymiuk.badbudget.viewobjecttables.CashActivity;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.viewobjecttables.SavingsActivity;
import com.erikartymiuk.badbudgetlogic.main.Account;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetInvalidValueException;
import com.erikartymiuk.badbudgetlogic.main.Contribution;
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.Prediction;
import com.erikartymiuk.badbudgetlogic.main.SavingsAccount;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * The add savings activity class's main purpose is to present an input form to the user in order
 * to add a valid savings account object to their bad budget data. There are several means of
 * input in this activity including text, checkboxes, numbers, dates, dropdowns, and buttons.
 * Sometimes these values can conflict thus there is a dedicated button that will check the user's
 * values and present option to "clean" their data so that it is consistent. After successful cleaning
 * the user can then submit their savings account object which will be added to the database and then
 * to memory. After which the user is taken back (or directed to) the savings account table page.
 */
public class AddSavingsActivity extends BadBudgetChildActivity
        implements AdapterView.OnItemSelectedListener, DateInputActivity, CleanFragmentParent, BBOperationTaskCaller {

    private static final int GOAL_AMOUNT_POSITION = 0;
    private static final int GOAL_DATE_POSITION = 1;
    private static final int CONTRIBUTION_AMOUNT_POSITION = 2;

    /* Codes used to identify which date was chosen when the dateSet callback is called with a return code */
    private static final int NEXT_DATE_SET_CODE = 0;
    private static final int END_DATE_SET_CODE = 1;
    private static final int GOAL_DATE_SET_CODE = 2;

    /* all input fields available in our layout file */

    private TextView title;

    /* Basic account fields */
    private EditText accountNameInput;
    private EditText currentValueInput;
    private EditText interestRateInput;
    private CheckBox quicklookInput;

    /* Goal fields */
    private CheckBox setGoalInput;
    private EditText goalAmountInput;
    private TextView goalDateText;

    /* Contribution fields */
    private Spinner contributionFrequencyInput;
    private EditText contributionAmountInput;
    private Spinner sourceAccountInput;
    private TextView nextDateText;
    private TextView endDateText;
    private CheckBox ongoingCheckbox;

    /* Dates for the goal, next and end dates - null if not set */
    private Date goalDate;
    private Date nextDate;
    private Date endDate;

    private Frequency contributionFrequency;

    /* These temp values are set before showing the quick value fragment so that we don't have to
    * recalculate the correct values for whatever the user chooses to clean/update */
    private double tempGoalAmount;
    private Date tempGoalDate;
    private double tempContributionAmount;

    /*
    Toasts for the clean button situations
     */
    private Toast alreadyCleanToast;
    private Toast missingInputToast;
    private Toast submitErrorToast;
    private Toast calculateGoalErrorToast;

    /* Message displayed as the account is loaded into memory */
    public static final String progressDialogMessage = "Adding savings account...";
    /* Message display as an account is being updated (the edit is submitted) */
    public static final String progressDialogMessageUpdate = "Updating savings account...";
    /* Message displayed as an account we are editing is deleted */
    public static final String progressDialogMessageDelete = "Deleting savings account...";

    /* Indicates if this form should return to the generic cash accounts table when leaving this form */
    private boolean genericAccountSource;

    /* Indicates if the user is currently editing an existing savings account */
    private boolean editing;
    private String savingsAccountEditName;
    private Toast deleteErrorToast;

    /* Saved state keys */

    private static final String ACCOUNT_NAME_KEY = "ACCOUNT_NAME";
    private static final String CURRENT_VALUE_KEY = "CURRENT_VALUE";
    private static final String INTEREST_RATE_KEY = "INTEREST_RATE";
    private static final String QUICKLOOK_KEY = "QUICKLOOK";
    private static final String SET_GOAL_KEY = "SET_GOAL";
    private static final String GOAL_AMOUNT_KEY = "GOAL_AMOUNT";
    private static final String GOAL_DATE_KEY = "GOAL_DATE";
    private static final String CONTRIBUTION_FREQUENCY_KEY = "CONTRIBUTION_FREQUENCY";
    private static final String CONTRIBUTION_AMOUNT_KEY = "CONTRIBUTION_AMOUNT";
    private static final String SOURCE_ACOUNT_KEY = "SOURCE_ACCOUNT";
    private static final String NEXT_CONTRIBUTION_KEY = "NEXT_CONTRIBUTION";
    private static final String END_DATE_KEY = "END_DATE";
    private static final String ONGOING_KEY = "ONGOING";

    /* Setup Methods */

    /**
     * Setup for the add savings activity includes grabbing all of the views defined in our layout
     * as many of the values change in response to the users input and we also need to grab
     * many of these values on submit button press in order to create our savings account object.
     *
     * @param savedInstanceState - unused
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(R.layout.content_add_savings);

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
     * Private helper method to setup necessary views for the savings form; invoked regardless
     * of whether first time setup or restoring from a saved state. Sets necessary conditions
     * for editing also.
     */
    private void setup()
    {
        accountNameInput = (EditText) findViewById(R.id.addSavingsAccountName);
        currentValueInput = (EditText) findViewById(R.id.addSavingsCurrentValue);
        interestRateInput = (EditText) findViewById(R.id.addSavingsInterestRate);
        quicklookInput = (CheckBox) findViewById(R.id.addSavingsQuicklookCheckbox);

        setGoalInput = (CheckBox) findViewById(R.id.addSavingsSetGoal);
        goalAmountInput = (EditText) findViewById(R.id.addSavingsGoalAmount);
        goalDateText = (TextView) findViewById(R.id.addSavingsGoalDate);
        goalDateText.setClickable(false);

        contributionFrequencyInput = (Spinner) findViewById(R.id.addSavingsContributionFrequency);
        contributionAmountInput = (EditText) findViewById(R.id.addSavingsContributionAmount);
        sourceAccountInput = (Spinner) findViewById(R.id.addSavingsSourceAccount);
        nextDateText = (TextView) findViewById(R.id.addSavingsNextDate);
        endDateText = (TextView) findViewById(R.id.addSavingsEndDate);
        ongoingCheckbox = (CheckBox) findViewById(R.id.addSavingsOngoingCheckbox);

        /* Start the dates as not set (i.e should be null)*/
        goalDate = null;
        nextDate = null;
        endDate = null;

         /* Setup our spinners, we will need a listener for our frequency selection as we will update
        * the contribution amount, if set, to match the new frequency. We will also need to setup our
        * adapter for our source account spinner. We will only get a hold of accounts that are
        * not savings accounts and put those names into a list of strings which we can then use
        * to reference the accounts by using the name as a key */
        contributionFrequencyInput.setOnItemSelectedListener(this);

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
        sourceAccountInput.setAdapter(adapter);

        Bundle args = getIntent().getExtras();
        if (args != null)
        {
            editing = args.getBoolean(BadBudgetApplication.EDIT_KEY);
            genericAccountSource = args.getBoolean(BadBudgetApplication.GENERIC_ACCOUNTS_RETURN_KEY);
            if (editing)
            {
                this.setToolbarTitle(R.string.add_savings_edit_title);
                savingsAccountEditName = args.getString(BadBudgetApplication.EDIT_OBJECT_ID_KEY);
                SavingsAccount savingsAccount = (SavingsAccount)((BadBudgetApplication)getApplication()).
                        getBadBudgetUserData().getAccountWithName(savingsAccountEditName);

                accountNameInput.setText(savingsAccount.name());
                accountNameInput.setEnabled(false);

                //Enable the delete button
                Button deleteButton = (Button) this.findViewById(R.id.deleteButton);
                deleteButton.setEnabled(true);
                deleteButton.setVisibility(View.VISIBLE);
            }
        }
        else
        {
            editing = false;
            genericAccountSource = false;
        }


        /* Instantiate our toast used for messages to the user on clean button press */
        alreadyCleanToast = Toast.makeText(this, R.string.add_savings_data_clean_message, Toast.LENGTH_SHORT);
        alreadyCleanToast.setGravity(Gravity.CENTER, 0, 0);

        missingInputToast = Toast.makeText(this, R.string.add_savings_clean_missing_input, Toast.LENGTH_LONG);
        missingInputToast.setGravity(Gravity.CENTER, 0, 0);

        /* Toast for errors on submit press */
        submitErrorToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        submitErrorToast.setGravity(Gravity.CENTER, 0, 0);

        /* Toast for errors on delete press */
        deleteErrorToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        deleteErrorToast.setGravity(Gravity.CENTER, 0, 0);

        /* Toast for errors when a reasonable goal can't be found */
        calculateGoalErrorToast = Toast.makeText(this, R.string.add_loan_payment_calculate_goal_error, Toast.LENGTH_LONG);
        calculateGoalErrorToast.setGravity(Gravity.CENTER, 0, 0);
    }

    /**
     * Private helper method that sets up the form for being used when the user first enters.
     */
    private void firstTimeSetup()
    {
        //Setup the starting frequency as weekly
        contributionFrequency = Frequency.weekly;
        contributionFrequencyInput.setSelection(BadBudgetApplication.WEEKLY_INDEX);

        if (editing)
        {
            SavingsAccount accountToEdit = (SavingsAccount)((BadBudgetApplication)getApplication()).
                    getBadBudgetUserData().getAccountWithName(savingsAccountEditName);
            currentValueInput.setText(Double.toString(accountToEdit.value()));
            interestRateInput.setText(Double.toString(accountToEdit.getInterestRate()));
            quicklookInput.setChecked(accountToEdit.quickLook());

            this.setPrepopulateContrFreq(accountToEdit.contribution().getFrequency());
            contributionAmountInput.setText(Double.toString(accountToEdit.contribution().getContribution()));
            this.setPrepopulateAccountSource(accountToEdit.sourceAccount().name());

            setNextDate(accountToEdit.nextContribution());

            this.setPrepopulateGoalEndDates(accountToEdit.goalDate(), Double.toString(accountToEdit.goal()), accountToEdit.endDate());
        }
        else if (genericAccountSource)
        {
            //If we are not editing and came from the generic cash tables then the user must have been
            //on the generic add account form and we have prepopulated data to fill in
            Bundle args = getIntent().getExtras();
            accountNameInput.setText(args.getString(BadBudgetApplication.INPUT_ACCOUNT_NAME_KEY));
            currentValueInput.setText(Double.toString(args.getDouble(BadBudgetApplication.INPUT_ACCOUNT_AMOUNT_KEY)));
            quicklookInput.setChecked(args.getBoolean(BadBudgetApplication.INPUT_ACCOUNT_QUICKLOOK_KEY));
        }
    }

    /**
     * Sets up our form when the user returns to it the state they left it in should be restored.
     * @param savedInstanceState - the state to restore
     */
    private void savedStateSetup(Bundle savedInstanceState) {

        accountNameInput.setText(savedInstanceState.getString(ACCOUNT_NAME_KEY));
        currentValueInput.setText(savedInstanceState.getString(CURRENT_VALUE_KEY));
        interestRateInput.setText(savedInstanceState.getString(INTEREST_RATE_KEY));
        quicklookInput.setChecked(savedInstanceState.getBoolean(QUICKLOOK_KEY));

        boolean setGoal = savedInstanceState.getBoolean(SET_GOAL_KEY);
        if (setGoal)
        {
            setGoalInput.setChecked(true);
            setupGoalEnabled();

            String goalAmountString = savedInstanceState.getString(GOAL_AMOUNT_KEY);
            goalAmountInput.setText(goalAmountString);
            Date savedGoalDate = (Date)savedInstanceState.getSerializable(GOAL_DATE_KEY);
            setGoalDate(savedGoalDate);

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
                Date savedEndDate = (Date)savedInstanceState.getSerializable(END_DATE_KEY);
                setEndDate(savedEndDate);
            }
        }

        setPrepopulateContrFreq((Frequency) savedInstanceState.getSerializable(CONTRIBUTION_FREQUENCY_KEY));
        contributionAmountInput.setText(savedInstanceState.getString(CONTRIBUTION_AMOUNT_KEY));

        setPrepopulateAccountSource(savedInstanceState.getString(SOURCE_ACOUNT_KEY));

        Date nextDate = (Date)savedInstanceState.getSerializable(NEXT_CONTRIBUTION_KEY);
        setNextDate(nextDate);
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
        outState.putString(ACCOUNT_NAME_KEY, accountNameInput.getText().toString());
        outState.putString(CURRENT_VALUE_KEY, currentValueInput.getText().toString());
        outState.putString(INTEREST_RATE_KEY, interestRateInput.getText().toString());
        outState.putBoolean(QUICKLOOK_KEY, quicklookInput.isChecked());

        outState.putBoolean(SET_GOAL_KEY, setGoalInput.isChecked());
        outState.putSerializable(GOAL_DATE_KEY, goalDate);
        outState.putString(GOAL_AMOUNT_KEY, goalAmountInput.getText().toString());

        outState.putBoolean(ONGOING_KEY, ongoingCheckbox.isChecked());
        outState.putSerializable(END_DATE_KEY, endDate);

        outState.putSerializable(CONTRIBUTION_FREQUENCY_KEY, contributionFrequency);
        outState.putString(CONTRIBUTION_AMOUNT_KEY, contributionAmountInput.getText().toString());
        outState.putString(SOURCE_ACOUNT_KEY, (String)sourceAccountInput.getSelectedItem());
        outState.putSerializable(NEXT_CONTRIBUTION_KEY, nextDate);
    }

    /**
     * Helper method to setup the contribution frequency for when the user enters this form with
     * the intention of edition an existing savings account. Uses the application defined indices
     * for frequency to set the position for our frequency spinner.
      * @param frequency - the frequency of the item the user is editing
     */
    private void setPrepopulateContrFreq(Frequency frequency)
    {
        contributionFrequency = frequency;
        switch (contributionFrequency)
        {
            case oneTime:
                contributionFrequencyInput.setSelection(BadBudgetApplication.ONE_TIME_INDEX);
                break;
            case daily:
                contributionFrequencyInput.setSelection(BadBudgetApplication.DAILY_INDEX);
                break;
            case weekly:
                contributionFrequencyInput.setSelection(BadBudgetApplication.WEEKLY_INDEX);
                break;
            case biWeekly:
                contributionFrequencyInput.setSelection(BadBudgetApplication.BIWEEKLY_INDEX);
                break;
            case monthly:
                contributionFrequencyInput.setSelection(BadBudgetApplication.MONTHLY_INDEX);
                break;
            case yearly:
                contributionFrequencyInput.setSelection(BadBudgetApplication.YEARLY_INDEX);
                break;
        }
    }

    /**
     * Given an account name this method searches for an account with that name in our list of
     * possible source accounts. Once found it chooses that item in the spinner. It is an error
     * and the behavior undefined if the name isn't in the adapter
     * @param sourceName
     */
    private void setPrepopulateAccountSource(String sourceName)
    {
        ArrayAdapter<String> sourceAccountAdapter = (ArrayAdapter<String>) sourceAccountInput.getAdapter();
        int index = sourceAccountAdapter.getCount() - 1;
        while (index >= 0)
        {
            String accountName = sourceAccountAdapter.getItem(index);
            if (accountName.equals(sourceName))
            {
                sourceAccountInput.setSelection(index);
                return;
            }
            index--;
        }
        //It is an error to reach here
    }

    /**
     * Helper method to setup our goal and end dates along with any dependent checkboxes and text
     * fields
     * @param goalDate - the goal date to set, can be null if goal not set
     * @param goalAmount - the goal amount to set, ignored if goal date null
     * @param endDate - the end date to set, can be null if ongoing set, ignored if goal date set
     */
    private void setPrepopulateGoalEndDates(Date goalDate, String goalAmount, Date endDate)
    {
        if (goalDate != null)
        {
            setGoalInput.setChecked(true);
            setupGoalEnabled();

            goalAmountInput.setText(goalAmount);
            setGoalDate(goalDate);
        }
        else
        {
            boolean ongoing = endDate == null;
            if (ongoing)
            {
                ongoingCheckbox.setChecked(true);
                ongoingChecked();
            }
            else
            {
                setEndDate(endDate);
            }
        }
    }

    /* Methods handling non-trivial updates to input values made by user or programmatically (such as
    * when setting up the form for the first time) */

    /**
     * Private helper method to be called when the ongoing checkbox is checked, either programattically,
     * or by the user. This assumes that the ongoing checkbox is only checked after the goal fields
     * are disabled Clears the end date and disables it. Sets the message in the end date field
     * to the ongoing message.
     */
    private void ongoingChecked()
    {
        setupEndPartialDisable(getString(R.string.add_savings_end_date_ongoing));
    }

    /**
     * Private helper method to be called when the ongoing checkbox is unchecked, either programatically
     * or by the user. This assumes that the ongoing checkbox is only unchecked when the goal fields
     * are disabled. This enables the end date field and sets the hint for that field to the default
     * value.
     */
    private void ongoingUnchecked()
    {
        endDateText.setClickable(true);
        endDateText.setHint(R.string.add_savings_end_date_hint);
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

    /**
     * Private helper to set the goal fields to being enabled. This includes:
     * Enabling the goal amount and goal date inputs
     * Disabling the end date fields (see setupEndDisabled)
     */
    private void setupGoalEnabled()
    {
        this.goalAmountInput.setEnabled(true);
        this.goalDateText.setClickable(true);

        setupEndDisabled();
    }

    /**
     * Helper method clearing and disabling the goal fields. This includes:
     * Disabling the goal amount and goal date fields
     * Clearing the goal date and goal amount.
     */
    private void setupGoalDisabled()
    {
        this.goalAmountInput.setEnabled(false);
        this.goalDateText.setClickable(false);

        clearGoalDate();
        this.goalAmountInput.setText("");
    }

    /**
     * Private helper method to set the end date fields to being cleared and disabled.
     * This includes:
     * Disabling the ongoing checkbox and end date.
     * Unchecking the ongoing checkbox
     * Making the ongoing checkbox invisible and setting the hint for the end date to display
     *      a message indicating that it will match the goal date.
     */
    private void setupEndDisabled()
    {
        this.ongoingCheckbox.setChecked(false);
        this.ongoingCheckbox.setClickable(false);
        this.ongoingCheckbox.setVisibility(View.INVISIBLE);
        setupEndPartialDisable(getString(R.string.add_savings_end_date_disabled));
    }

    /**
     * Partially disables the end date field(s). Meaning the text to set a date is no longer
     * clickable and the end date is cleared. The hint for the text field is set to the passed value
     * @param hint - the hint to assign to the text for the end date field.
     */
    private void setupEndPartialDisable(String hint)
    {
        endDateText.setHint(hint);
        clearEndDate();
        endDateText.setClickable(false);
    }

    /**
     * Private helper method enabling the end date fields. Also disables the goal fields
     * Enabling the end date fields includes:
     * Enabling the ongoing checkbox and making it visible
     * Enabling the end date text and setting the message to the default hint
     * To see the disabling effects of the goal see setupGoalDisabled
     */
    private void setupEndEnabled()
    {
        this.ongoingCheckbox.setClickable(true);
        this.ongoingCheckbox.setVisibility(View.VISIBLE);
        this.endDateText.setClickable(true);
        this.endDateText.setHint(R.string.add_savings_end_date_hint);

        setupGoalDisabled();
    }

    /* Methods for when the user takes some action (button presses, checkbox clicks, dates clicked */

    /**
     * Private helper method. Called when one of the three dates is clicked on indicating the user
     * wishes to set a date. Passed a code indicating which date was clicked and a tag identifying
     * the fragment that will be created for date picking. Uses the application today for today's date
     * @param code - the code identifying which date is being chosen (contant in AddSavingsActivity)
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
     * Method called when the goal date is clickable and pressed by the user.
     * Displays the date picker fragment for picking a goal date.
     * @param view - the goal date text view that was pressed
     */
    public void goalDateClicked(View view)
    {
        dateClicked(GOAL_DATE_SET_CODE, "goalDatePicker");
    }

    /**
     * Called when the next date text is pressed. Displays to the user the next date picker fragment
     * and has them choose a date.
     * @param view - the next date text view pressed
     */
    public void nextDateClicked(View view)
    {
        dateClicked(NEXT_DATE_SET_CODE, "nextDatePicker");
    }

    /**
     * This method is called when the end date text is pressed. Display the end date fragment so the
     * user can pick a date.
     * @param view - the end date text that was clicked
     */
    public void endDateClicked(View view)
    {
        dateClicked(END_DATE_SET_CODE, "endDatePicker");
    }

    /**
     * This method is called when the setGoal checkbox is clicked. This will make the goal amount
     * and goal date fields editable if the checkbox was checked and disable them if unchecked.
     * If unchecked the goal date field is also cleared (instance set to null, text is empty).
     * Additionally if the checkbox is checked then the end date field is no longer editable and
     * displays a message indicating that it will match the goal date. The end date instance variable
     * is also set to null. The ongoing checkbox is
     * unchecked, disabled, and set to be invisible.
     * @param view - the checkbox that was toggled
     */
    public void setGoalClicked(View view)
    {
        boolean goalChecked = this.setGoalInput.isChecked();

        if (goalChecked)
        {
            setupGoalEnabled();
        }
        else //goalUnchecked
        {
            setupEndEnabled();
        }
    }

    /**
     * Method called when the clean button is pressed. This method checks for consistency among the
     * three key inputs: goal amount, goal date, and contribution (including frequency and amount).
     * If all three have values entered for them but are not consistent then a dialog pops up giving
     * the user three choices of what they could update any 1 of the 3 values to in order to make
     * it consistent. If two of the three are entered then the third value is auto filled to match
     * the other two. If one or none of the values have been entered or the goal checkbox is unchecked
     * or if we are missing other key information (current value, next date)
     * then a simple message toast is shown indicating that cleaning cannot occur yet. If the values
     * are all entered and are already consistent then a toast pops up indicating this.
     * @param view
     */
    public void addSavingsCleanClick(View view)
    {
        /* String inputs we are interested in, we consider anything other then the empty string as
        the user having input something for that field */
        String currentValueString = this.currentValueInput.getText().toString();
        String interestRateString = this.interestRateInput.getText().toString();
        String goalAmountString = this.goalAmountInput.getText().toString();
        String contributionAmountString = this.contributionAmountInput.getText().toString();

        /* All the fields - we may need. If more than one is missing we need more input from the user */
        boolean currentAmountEntered = !currentValueString.equals("");
        boolean interestRateEntered = !interestRateString.equals("");
        boolean goalAmountEntered = !goalAmountString.equals("");
        boolean goalDateEntered = goalDate != null;
        boolean contributionAmountEntered = !contributionAmountString.equals("");
        boolean nextDateEntered = nextDate != null;

        boolean goalSet = goalAmountEntered && goalDateEntered && setGoalInput.isChecked();

        boolean allEntered = currentAmountEntered && interestRateEntered && goalSet && contributionAmountEntered && nextDateEntered;
        boolean missingKey = !currentAmountEntered || !interestRateEntered || !nextDateEntered || !setGoalInput.isChecked();
        boolean missingOnlyGoalAmount = !missingKey && !goalAmountEntered && goalDateEntered && contributionAmountEntered;
        boolean missingOnlyGoalDate = !missingKey && goalAmountEntered && !goalDateEntered && contributionAmountEntered;
        boolean missingOnlyContributionAmount = !missingKey && goalAmountEntered && goalDateEntered && !contributionAmountEntered;

        if (allEntered)
        {
            double currentGoalAmount = Double.parseDouble(this.goalAmountInput.getText().toString());
            double correctGoalAmount = calculateGoalAmount();

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

            double currentContributionAmount = Double.parseDouble(this.contributionAmountInput.getText().toString());
            double correctContributionAmount = calculateContributionAmount();

            double goalAmountDiff = correctGoalAmount - currentGoalAmount;
            double contributionAmountDiff = currentContributionAmount - correctContributionAmount;

            boolean goalAmountGood = goalAmountDiff >= 0 && goalAmountDiff < 0.01;
            boolean contributionAmountGood = contributionAmountDiff >= 0 && contributionAmountDiff < 0.01;

            boolean dataConsistent = goalAmountGood ||
                    Prediction.datesEqualUpToDay(currentGoalDate, correctGoalDate) ||
                    contributionAmountGood;

            if (!dataConsistent)
            {
                //Keep track of the correct goal date in case the user indicates this is what they want
                //to update.
                this.tempGoalAmount = Math.floor(correctGoalAmount*10*10)/(10.0*10.0);
                this.tempGoalDate = correctGoalDate;
                this.tempContributionAmount =  Math.ceil(correctContributionAmount*10*10)/(10.0*10.0);

                //Pass the date needed so the user can see what the old value was and what the updated
                //value would be for the three options
                Bundle bundle = new Bundle();
                bundle.putInt(CleanFragment.NUM_ITEMS_KEY, 3);

                bundle.putString(CleanFragment.POSITION_ONE_TITLE_KEY, "");
                bundle.putString(CleanFragment.POSITION_ONE_OLD_KEY, Double.toString(currentGoalAmount));
                bundle.putString(CleanFragment.POSITION_ONE_NEW_KEY, Double.toString(tempGoalAmount));

                bundle.putString(CleanFragment.POSITION_TWO_TITLE_KEY, "");
                bundle.putString(CleanFragment.POSITION_TWO_OLD_KEY, BadBudgetApplication.dateString(currentGoalDate));
                bundle.putString(CleanFragment.POSITION_TWO_NEW_KEY, BadBudgetApplication.dateString(correctGoalDate));

                bundle.putString(CleanFragment.POSITION_THREE_TITLE_KEY, "");
                bundle.putString(CleanFragment.POSITION_THREE_OLD_KEY, Double.toString(currentContributionAmount));
                bundle.putString(CleanFragment.POSITION_THREE_NEW_KEY, Double.toString(tempContributionAmount));

                CleanFragment cleanFragment = new CleanFragment();
                cleanFragment.setArguments(bundle);
                cleanFragment.show(getSupportFragmentManager(), "cleanFragment");
            }
            else
            {
                if (!alreadyCleanToast.getView().isShown())
                {
                    alreadyCleanToast.show();
                }
            }
        }
        else if (missingOnlyGoalAmount)
        {
            double correctGoalAmount = calculateGoalAmount();
            this.goalAmountInput.setText(Double.toString(correctGoalAmount));

            CharSequence text = getResources().getText(R.string.add_savings_clean_auto_set_goal_amount);
            text = text + " " + correctGoalAmount;
            Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
            toast.show();
        }
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

            setGoalDate(correctGoalDate);

            CharSequence text = getResources().getText(R.string.add_savings_clean_auto_set_goal_date);
            text = text + " " + BadBudgetApplication.dateString(correctGoalDate);
            Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
            toast.show();
        }
        else if (missingOnlyContributionAmount)
        {
            double correctContributionAmount = calculateContributionAmount();
            this.contributionAmountInput.setText(Double.toString(correctContributionAmount));

            CharSequence text = getResources().getText(R.string.add_savings_clean_auto_set_contribution_amount);
            text = text + " " + correctContributionAmount;
            Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
            toast.show();
        }
        else
        {
            //To clean we require at least the following: currentValue, interest rate setGoal checked, and a nextDate
            // and at least 2 of the three fields: goalAmount, goalDate, and contribution amount
            if (!missingInputToast.getView().isShown())
            {
                missingInputToast.show();
            }
        }
    }

    /**
     * Method called when the user clicks the submit button. First all the fields are verified and checked
     * to see if we have all the data we need and that it is correct in order to create a savings account.
     * If it isn't then a toast is shown indicating what is missing or what is incorrect. If the data is
     * good then this method creates our savings account object for in memory and for our database. It
     * then kicks off our AddSavingsAccoutTask and shows a dialog as the task is processing. The task
     * itself adds simply inserts the savings account in the database and into memory and afterwards
     * dismisses the dialog, and takes the user to the savings account table activity.
     *
     * @param view - the submit button that was clicked
     */
    public void addSavingsSubmitClick(View view)
    {
        if (verifyValues())
        {
            String accountName = this.accountNameInput.getText().toString();
            double currentValue = Double.parseDouble(this.currentValueInput.getText().toString());
            double interestRateDouble = Double.parseDouble(this.interestRateInput.getText().toString());
            boolean quicklook = this.quicklookInput.isChecked();
            double contributionAmount = Double.parseDouble(this.contributionAmountInput.getText().toString());

            Date nextDate = this.nextDate;

            double goal = -1;
            Date goalDate = null;
            Date endDate = null;
            if (setGoalInput.isChecked())
            {
                //Have a goal
                goal = Double.parseDouble(this.goalAmountInput.getText().toString());
                goalDate = this.goalDate;
                //Set end date to be the goal date rather than null as the global variable is set to
                endDate = this.goalDate;

            }
            else
            {
                //No Goal, goal date will be null
                endDate = this.endDate;
            }

            BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
            Account sourceAccount = bbd.getAccountWithName((String)this.sourceAccountInput.getSelectedItem());

            try
            {
                Contribution contribution = new Contribution(contributionAmount, this.contributionFrequency);
                SavingsAccount sa = new SavingsAccount(accountName, currentValue, quicklook, goalDate != null,
                        goal, goalDate, contribution, sourceAccount, nextDate, endDate, endDate == null, interestRateDouble);

                //Prep the input for insertion into the database
                ContentValues values = new ContentValues();
                values.put(BBDatabaseContract.CashAccounts.COLUMN_NAME, accountName);
                values.put(BBDatabaseContract.CashAccounts.COLUMN_VALUE, currentValue);
                values.put(BBDatabaseContract.CashAccounts.COLUMN_INTEREST_RATE, interestRateDouble);

                if (quicklook)
                {
                    values.put(BBDatabaseContract.CashAccounts.COLUMN_QUICK_LOOK, BBDatabaseContract.TRUE_VALUE);
                }
                else
                {
                    values.put(BBDatabaseContract.CashAccounts.COLUMN_QUICK_LOOK, BBDatabaseContract.FALSE_VALUE);
                }

                values.put(BBDatabaseContract.CashAccounts.COLUMN_SAVINGS, BBDatabaseContract.TRUE_VALUE);
                values.put(BBDatabaseContract.CashAccounts.COLUMN_GOAL, goal);
                values.put(BBDatabaseContract.CashAccounts.COLUMN_CONTRIBUTION_AMOUNT, contributionAmount);
                values.put(BBDatabaseContract.CashAccounts.COLUMN_CONTRIBUTION_FREQUENCY, BBDatabaseContract.dbFrequencyToInteger(this.contributionFrequency));
                values.put(BBDatabaseContract.CashAccounts.COLUMN_NEXT_CONTRIBUTION, BBDatabaseContract.dbDateToString(nextDate));
                values.put(BBDatabaseContract.CashAccounts.COLUMN_GOAL_DATE, BBDatabaseContract.dbDateToString(goalDate));
                values.put(BBDatabaseContract.CashAccounts.COLUMN_END_DATE, BBDatabaseContract.dbDateToString(endDate));
                values.put(BBDatabaseContract.CashAccounts.COLUMN_SOURCE_NAME, sourceAccount.name());

                if (!editing)
                {
                    AddBBObjectTask task = new AddBBObjectTask(this, progressDialogMessage, sa, values, this, BBObjectType.ACCOUNT_SAVINGSACCOUNT);
                    task.execute();
                }
                else
                {
                    EditBBObjectTask task = new EditBBObjectTask(this, progressDialogMessageUpdate, sa, values, this, BBObjectType.ACCOUNT_SAVINGSACCOUNT);
                    task.execute();
                }
            }
            catch (BadBudgetInvalidValueException e)
            {
                //TODO - Decide how to handle this
                e.printStackTrace();
                System.out.println("Failed to Create Savings Account");
            }
        }
    }

    /**
     * Behaves as if the user clicked the back button, starting the savings (table) activity and
     * finishing this activity to prevent navigation back to the form.
     * @param view
     */
    public void addSavingsCancelClick(View view)
    {
        this.onBackPressed();
    }

    /**
     * Overridden back pressed method. This restarts the savings activity as it should have been ended
     * when add savings account was clicked. Then it finishes the add savings account activity to prevent navigation
     * back to the form. If our source was the vanilla cash form then we take the user back to the
     * generic cash accounts table as opposed to the savings specific table TODO changed 12/08
     */
    public void onBackPressed()
    {
        this.finish();
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
     * Method called when the delete button is clicked. Kicks off a task that deletes the account that
     * we are currently editing. The delete button is only visible and thus this method is called only
     * when the user is editing an exisiting account.
     * @param view
     */
    public void deleteClick(View view)
    {
        String errorString = verifyCanDelete();
        if (errorString == null) {
            DeleteBBObjectTask task = new DeleteBBObjectTask(this, progressDialogMessageDelete, this.savingsAccountEditName, this, BBObjectType.ACCOUNT_SAVINGSACCOUNT);
            task.execute();
        }
        else
        {
            //Currently this should never enter here but in the future if it a savings account cannot
            //be deleted we display a toast why
            if (!deleteErrorToast.getView().isShown())
            {
                deleteErrorToast.setText("\n" + getString(R.string.account_delete_error_base) + errorString);
                deleteErrorToast.show();
            }
        }
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
     * Called in response to the user selecting a date from the goal date picker fragment. Updates
     * the text view with the date chosen and creates and sets the goal date object. Checks the newly
     * set goal date against the next date if set. If the goal date comes before the next date then
     * we clear the next date.
     * @param year - year selected
     * @param monthOfYear - month selected (note january is 0)
     * @param dayOfMonth - day of the month selected
     */
    public void goalDateSet(int year, int monthOfYear, int dayOfMonth)
    {
        Calendar tempCalendar = new GregorianCalendar(year, monthOfYear, dayOfMonth);
        setGoalDate(tempCalendar.getTime());

        if (nextDate != null && Prediction.numDaysBetween(nextDate, goalDate) < 0)
        {
            clearNextDate();
        }
    }

    /**
     * Called when the user selects a next date from the dialog fragment. Sets up a date object and
     * updates the text view to show the date they chose. Checks to see if the next date comes before
     * either the goal date or the end date if one is set (only one should be able to be set). If the
     * end or goal comes before the next then we clear that field.
     * @param year - year
     * @param monthOfYear - month of year (Jan = 0)
     * @param dayOfMonth - day of month
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
     * Called when user chooses an end date from the date picker dialog. Sets up a date object and
     * updates the text view to display the date they chose. Checks to make sure that if next date
     * is set that it occurs before the newly set end date. If it doesn't then the next date field
     * is cleared.
     * @param year - year
     * @param monthOfYear - month of year (Jan = 0)
     * @param dayOfMonth - day of month
     */
    public void endDateSet(int year, int monthOfYear, int dayOfMonth)
    {
        Calendar tempCalendar = new GregorianCalendar(year, monthOfYear, dayOfMonth);
        setEndDate(tempCalendar.getTime());

        //Check the next date and make sure it comes before end date. If it doesn't
        //we clear the next date.
        if (nextDate != null && Prediction.numDaysBetween(nextDate, endDate) < 0)
        {
            //next date occurs after end date so we clear the next date
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
        switch (selectionPosition)
        {
            case GOAL_AMOUNT_POSITION:
            {
                cleanGoalAmount();
                break;
            }
            case GOAL_DATE_POSITION:
            {
                cleanGoalDate();
                break;
            }
            case CONTRIBUTION_AMOUNT_POSITION:
            {
                cleanContributionAmount();
                break;
            }
        }
    }

    /**
     * Callback method that should be called when it is determined that the user wants to clean
     * the goal amount after being presented with their three choices
     *
     */
    public void cleanGoalAmount()
    {
        this.goalAmountInput.setText(Double.toString(this.tempGoalAmount));
    }

    /**
     * Callback method to call when the user has chosen to clean the goal date after pressing the
     * clean button and being presented with three choices. Uses the saved instance variable.
     */
    public void cleanGoalDate()
    {
        setGoalDate(tempGoalDate);
    }

    /**
     * Callback called when the user chooses to update the contribution amount after being presented
     * with three choices on the main clean button press. Uses the saved instance variable.
     */
    public void cleanContributionAmount()
    {
        this.contributionAmountInput.setText(Double.toString(this.tempContributionAmount));
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
        Frequency oldFrequency = this.contributionFrequency;


        String frequencyText = ((TextView)view).getText().toString();
        if (frequencyText.equals(this.getResources().getText(R.string.one_time).toString()))
        {
            this.contributionFrequency = Frequency.oneTime;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.daily).toString()))
        {
            this.contributionFrequency = Frequency.daily;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.weekly).toString()))
        {
            this.contributionFrequency = Frequency.weekly;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.bi_weekly).toString()))
        {
            this.contributionFrequency = Frequency.biWeekly;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.monthly).toString()))
        {
            this.contributionFrequency = Frequency.monthly;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.yearly).toString()))
        {
            this.contributionFrequency = Frequency.yearly;
        }

        boolean oneTimeFrequency = (oldFrequency == Frequency.oneTime) || (this.contributionFrequency == Frequency.oneTime);
        boolean frequencyChanged = oldFrequency != this.contributionFrequency;

        if (!oneTimeFrequency && frequencyChanged)
        {
            String contributionAmountString = this.contributionAmountInput.getText().toString();
            if (!contributionAmountString.equals(""))
            {
                double currAmount = Double.parseDouble(contributionAmountString);
                double convertedAmount = Prediction.toggle(currAmount, oldFrequency, this.contributionFrequency);
                this.contributionAmountInput.setText(Double.toString(convertedAmount));
            }
        }
    }

    /**
     * TODO
     * @param parent
     */
    public void onNothingSelected(AdapterView<?> parent)
    {

    }

    /**
     * Callback method called after an invoked AddBBObjectTask completes.
     * Finishes this AddSavingsActivity after setting the corresponding result.
     */
    public void addBBObjectFinished()
    {
        Intent returnIntent = new Intent();
        setResult(BadBudgetApplication.FORM_RESULT_ADD, returnIntent);
        this.finish();
    }

    /**
     * Callback method called after an invoked EditBBObjectTask completes.
     * Finishes this AddSavingsActivity after setting the corresponding result.
     */
    public void editBBObjectFinished()
    {
        Intent returnIntent = new Intent();
        setResult(BadBudgetApplication.FORM_RESULT_EDIT, returnIntent);
        this.finish();
    }

    /**
     * Callback method called after an invoked DeleteBBObjectTask completes.
     * Finishes this AddSavingsActivity after setting the corresponding result.
     */
    public void deleteBBObjectFinished()
    {
        Intent returnIntent = new Intent();
        setResult(BadBudgetApplication.FORM_RESULT_DELETE, returnIntent);
        this.finish();
    }

    /* The clean logic methods */

    /**
     * Private helper method that assumes all the fields that we need to calculate the goal amount
     * have been set by the user. Returns the goal amount that is consistent with those set fields
     * (Fields are: current value, interest rate, goal date, contribution amount, and next date
     * @return - a consistent goal amount
     */
    private Double calculateGoalAmount()
    {
        Double currentAmount = Double.parseDouble(this.currentValueInput.getText().toString());
        Double interestRate = Double.parseDouble(this.interestRateInput.getText().toString());
        Double contributionAmount = Double.parseDouble(this.contributionAmountInput.getText().toString());
        Date today = ((BadBudgetApplication)this.getApplication()).getToday();

        Contribution contribution = null;
        try
        {
            contribution = new Contribution(contributionAmount, contributionFrequency);
        }
        catch (BadBudgetInvalidValueException e)
        {
            //TODO - handle?
            e.printStackTrace();
        }

        double correctGoalAmount = Prediction.findGoalAmount(today, nextDate, contribution, currentAmount, interestRate, goalDate);
        return correctGoalAmount;
    }

    /**
     * Private helper method that assumes all fields that we need set are set in order to calculate
     * a goal date. Returns a goal date consistent with all the values currently set or null
     * if a reasonable goal cannot be found.
     * @return - a consistent goal date
     */
    private Date calculateGoalDate()
    {
        Double currentAmount = Double.parseDouble(this.currentValueInput.getText().toString());
        Double interestRate = Double.parseDouble(this.interestRateInput.getText().toString());
        Double goalAmount = Double.parseDouble(this.goalAmountInput.getText().toString());
        Double contributionAmount = Double.parseDouble(this.contributionAmountInput.getText().toString());
        Date today = ((BadBudgetApplication)this.getApplication()).getToday();

        Contribution contribution = null;
        try
        {
            contribution = new Contribution(contributionAmount, contributionFrequency);
        }
        catch (BadBudgetInvalidValueException e)
        {
            //TODO - handle?
            e.printStackTrace();
        }

        //Calculate our limit date
        Calendar todayCal = Calendar.getInstance();
        todayCal.setTime(((BadBudgetApplication)this.getApplication()).getToday());
        Calendar maxCal = new GregorianCalendar(todayCal.get(Calendar.YEAR)+getResources().getInteger(R.integer.predict_years),
                todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH));
        Date limitDate = maxCal.getTime();

        Date correctGoalDate = Prediction.findGoalDateWithInterest(today, nextDate, contribution, currentAmount, goalAmount, interestRate, limitDate);
        return correctGoalDate;
    }

    /**
     * Private helper method that assumes all values needed to be set by the user to calculate
     * a contribution amount that is consistent have been set. Uses those set values and returns
     * that contribution amount.
     * @return - a contribution amount consistent with the currently set input values
     */
    private Double calculateContributionAmount()
    {
        Double currentAmount = Double.parseDouble(this.currentValueInput.getText().toString());
        Double interestRate = Double.parseDouble(this.interestRateInput.getText().toString());
        Double goalAmount = Double.parseDouble(this.goalAmountInput.getText().toString());
        Date today = ((BadBudgetApplication)this.getApplication()).getToday();

        Double correctContributionAmount = Prediction.findContributionAmount(today, nextDate, contributionFrequency, currentAmount, goalAmount, interestRate, goalDate);
        return correctContributionAmount;
    }

    /* Error checking methods */

    /**
     * Private helper method to check that all required fields have input values and that they
     * are clean and consistent input values. If missing or erroneous values are found a toast
     * is shown indicating what is missing or if the data needs to be cleaned. Return true if
     * everything is found to be correct and ready for submitting to the database and memory.
     * @return true if the values are all entered and consistent.
     */
    private boolean verifyValues()
    {
        //We will construct our toast string showing any errors
        String errorToastString = "";
        String missing = this.getString(R.string.add_savings_submit_missing);

        //Things we require no matter if there is a goal set or not
        boolean accountNameEntered = !this.accountNameInput.getText().toString().equals("");
        boolean currentValueEntered = !this.currentValueInput.getText().toString().equals("");
        boolean interestRateEntered = !this.interestRateInput.getText().toString().equals("");
        boolean contributionAmountEntered = !this.contributionAmountInput.getText().toString().equals("");
        boolean sourceAccountSet = sourceAccountInput.getSelectedItem() != null;
        boolean nextDateSet = nextDate != null;

        boolean missingBasic = !accountNameEntered || !currentValueEntered || !interestRateEntered || !contributionAmountEntered || !sourceAccountSet || !nextDateSet;
        if (missingBasic)
        {
            errorToastString+=missing;
        }

        if (!accountNameEntered)
        {
            errorToastString+="\n";
            errorToastString+=this.getString(R.string.add_savings_account_name_prompt);
        }
        if (!currentValueEntered)
        {
            errorToastString+="\n";
            errorToastString+=this.getString(R.string.add_savings_current_value_prompt);
        }
        if (!interestRateEntered)
        {
            errorToastString+="\n";
            errorToastString+=this.getString(R.string.add_savings_interest_rate_prompt);
        }
        if (!contributionAmountEntered)
        {
            errorToastString+="\n";
            errorToastString+=this.getString(R.string.add_savings_contribution_amount_prompt);
        }
        if (!sourceAccountSet)
        {
            errorToastString+="\n";
            errorToastString+=this.getString(R.string.add_savings_source_account_prompt);
        }
        if (!nextDateSet)
        {
            errorToastString+="\n";
            errorToastString+=this.getString(R.string.add_savings_next_date_prompt);
        }

        boolean goalChecked = this.setGoalInput.isChecked();
        if (goalChecked)
        {
            //If the user indicated they wanted a goal to be set we need the goal amount and date
            boolean goalAmountEntered = !this.goalAmountInput.getText().toString().equals("");
            boolean goalDateSet = goalDate != null;

            boolean missingGoalValue = !goalAmountEntered || !goalDateSet;
            if (missingGoalValue)
            {
                if (missingBasic)
                {
                    errorToastString+="\n\n";
                }
                errorToastString+=this.getString(R.string.add_savings_submit_missing_goal);
                if (!goalAmountEntered)
                {
                    errorToastString+="\n";
                    errorToastString+=this.getString(R.string.add_savings_goal_amount_prompt);
                }
                if (!goalDateSet)
                {
                    errorToastString+="\n";
                    errorToastString+=this.getString(R.string.add_savings_goal_date_prompt);
                }
            }
            //If we aren't missing anything then check to see if the data is clean.
            if (!missingBasic && !missingGoalValue)
            {
                double currentGoalAmount = Double.parseDouble(this.goalAmountInput.getText().toString());
                double correctGoalAmount = calculateGoalAmount();

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

                double currentContributionAmount = Double.parseDouble(this.contributionAmountInput.getText().toString());
                double correctContributionAmount = calculateContributionAmount();

                double goalAmountDiff = correctGoalAmount - currentGoalAmount;
                double contributionAmountDiff = currentContributionAmount - correctContributionAmount;

                boolean goalAmountGood = goalAmountDiff >= 0 && goalAmountDiff < 0.01;
                boolean contributionAmountGood = contributionAmountDiff >= 0 && contributionAmountDiff < 0.01;

                boolean dataConsistent = goalAmountGood ||
                        Prediction.datesEqualUpToDay(currentGoalDate, correctGoalDate) ||
                        contributionAmountGood;

                if (!dataConsistent)
                {
                    errorToastString+=this.getString(R.string.add_savings_submit_data_dirty);
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
                        errorToastString+=this.getString(R.string.add_savings_submit_missing);
                    }
                    errorToastString+="\n";
                    errorToastString+=this.getString(R.string.add_savings_end_date_prompt);
                }
            }
        }

        //Make sure the account name is unique unless we are editing an account
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        if (!editing && bbd.getAccountWithName(this.accountNameInput.getText().toString()) != null)
        {
            if (errorToastString.equals("")) {
                errorToastString += this.getString(R.string.add_savings_duplicate_account_name);
            }
            else
            {
                errorToastString+="\n\n";
                errorToastString += this.getString(R.string.add_savings_duplicate_account_name);
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

    /**
     * Placeholder method that always returns null, since savings accounts cannot be used for
     * a source (contribution, payment, loss, budget) or a destination (gain), it will not be
     * linked to any other bb object thus deletion is always valid. If in the future a savings account
     * deletion causes inconsistencies then this method will return a string indicating what errors prevent
     * a savings account from being deleted.
     * @return currently always returns null. In the future may return an error string to be appended
     *          to the error toast base string.
     */
    private String verifyCanDelete()
    {
        return null;
    }
}
