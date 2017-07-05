package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.content.ContentValues;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseContract;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetChildActivity;
import com.erikartymiuk.badbudgetlogic.main.Account;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetInvalidValueException;
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.MoneyTransfer;
import com.erikartymiuk.badbudgetlogic.main.Prediction;
import com.erikartymiuk.badbudgetlogic.main.SavingsAccount;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Add Transfer activity form
 *
 * Created by Erik Artymiuk on 7/6/2017.
 */
public class AddTransferActivity extends BadBudgetChildActivity implements BBOperationTaskCaller, AdapterView.OnItemSelectedListener
{
    private EditText addTransferDescription;
    private Spinner addTransferSourceSpinner;
    private Spinner addTransferDestinationSpinner;

    private EditText addTransferAmount;

    private Spinner addTransferFrequencySpinner;
    private Frequency frequency;

    private TextView nextDateText;
    private Date nextDate;
    private TextView endDateText;
    private Date endDate;
    private CheckBox ongoingCheckbox;

    private boolean editing;
    private String editName;

    /* Saved state keys */
    private static final String TRANSFER_DESCRIPTION_KEY = "TRANSFER_DESCRIPTION";
    private static final String SOURCE_KEY = "SOURCE";
    private static final String DESTINATION_KEY = "DESTINATION";
    private static final String AMOUNT_KEY = "AMOUNT";
    private static final String FREQUENCY_KEY = "FREQUENCY";
    private static final String NEXT_DATE_KEY = "NEXT_DATE";
    private static final String END_DATE_KEY = "END_DATE";
    private static final String ONGOING_KEY = "ONGOING";

    private static final String progressDialogMessage = "Adding Transfer...";
    private static final String progressDialogMessageDelete = "Deleting Transfer...";
    private static final String progressDialogMessageUpdate = "Updating Transfer...";

    private Toast submitErrorToast;

    /**
     * The creation method for the Add Transfer activity. Sets the content of this activity and
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

        setContent(R.layout.content_add_transfer);

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
     * Private helper method that sets up the form for being used when the user first enters.
     */
    private void firstTimeSetup()
    {
        //Setup the starting frequency as monthly
        frequency = Frequency.monthly;
        addTransferFrequencySpinner.setSelection(getResources().getInteger(R.integer.monthly_index));

        if (editing)
        {
            MoneyTransfer editTransfer = ((BadBudgetApplication)this.getApplication()).getBadBudgetUserData().getTransferWithDescription(editName);

            setPrepopulateAccountSource(editTransfer.getSource().name());
            setPrepopulateAccountDestination(editTransfer.getDestination().name());

            addTransferAmount.setText(Double.toString(editTransfer.getAmount()));
            setPrepopulateAddTransferFreq(editTransfer.getFrequency());
            setNextDate(editTransfer.getNextTransfer());

            Date tempEndDate = editTransfer.getEndDate();
            if (tempEndDate != null)
            {
                setEndDate(editTransfer.getEndDate());
            }
            else
            {
                ongoingCheckbox.setChecked(true);
                ongoingChecked();
            }
        }
    }

    /**
     * Sets up our form when the user returns to it the state they left it in should be restored.
     * @param savedInstanceState - the state to restore
     */
    private void savedStateSetup(Bundle savedInstanceState)
    {
        addTransferDescription.setText(savedInstanceState.getString(TRANSFER_DESCRIPTION_KEY));

        setPrepopulateAccountSource(savedInstanceState.getString(SOURCE_KEY));
        setPrepopulateAccountDestination(savedInstanceState.getString(DESTINATION_KEY));

        addTransferAmount.setText(savedInstanceState.getString(AMOUNT_KEY));

        setPrepopulateAddTransferFreq((Frequency)savedInstanceState.getSerializable(FREQUENCY_KEY));

        setNextDate((Date)savedInstanceState.getSerializable(NEXT_DATE_KEY));

        boolean ongoing = savedInstanceState.getBoolean(ONGOING_KEY);
        if (ongoing)
        {
            ongoingCheckbox.setChecked(true);
            ongoingChecked();
        }
        else
        {
            setEndDate((Date) savedInstanceState.getSerializable(END_DATE_KEY));
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
        outState.putString(TRANSFER_DESCRIPTION_KEY, addTransferDescription.getText().toString());

        outState.putString(SOURCE_KEY, (String)addTransferSourceSpinner.getSelectedItem());
        outState.putString(DESTINATION_KEY, (String)addTransferDestinationSpinner.getSelectedItem());

        outState.putString(AMOUNT_KEY, addTransferAmount.getText().toString());

        outState.putSerializable(FREQUENCY_KEY, frequency);
        outState.putSerializable(NEXT_DATE_KEY, nextDate);
        outState.putSerializable(END_DATE_KEY, endDate);
        outState.putBoolean(ONGOING_KEY, ongoingCheckbox.isChecked());
    }


    /**
     * Prepopulates the frequency spinner to the passed frequency.
     * @param frequency
     */
    private void setPrepopulateAddTransferFreq(Frequency frequency)
    {
        this.frequency = frequency;
        switch (this.frequency)
        {
            case oneTime:
                addTransferFrequencySpinner.setSelection(BadBudgetApplication.ONE_TIME_INDEX);
                break;
            case daily:
                addTransferFrequencySpinner.setSelection(BadBudgetApplication.DAILY_INDEX);
                break;
            case weekly:
                addTransferFrequencySpinner.setSelection(BadBudgetApplication.WEEKLY_INDEX);
                break;
            case biWeekly:
                addTransferFrequencySpinner.setSelection(BadBudgetApplication.BIWEEKLY_INDEX);
                break;
            case monthly:
                addTransferFrequencySpinner.setSelection(BadBudgetApplication.MONTHLY_INDEX);
                break;
            case yearly:
                addTransferFrequencySpinner.setSelection(BadBudgetApplication.YEARLY_INDEX);
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
        ArrayAdapter<String> sourceAccountAdapter = (ArrayAdapter<String>) addTransferSourceSpinner.getAdapter();
        int index = sourceAccountAdapter.getCount() - 1;
        while (index >= 0)
        {
            String accountName = sourceAccountAdapter.getItem(index);
            if (accountName.equals(sourceName))
            {
                addTransferSourceSpinner.setSelection(index);
                return;
            }
            index--;
        }
        //It is an error to reach here
    }

    /**
     * Helper method prepopulating the account destination spinner to match the name of the passed in
     * account name. Assumes the destinationName is the name of a valid account in our bb data.
     * @param destinationName - the name of the destination account to set our spinner to.
     */
    private void setPrepopulateAccountDestination(String destinationName)
    {
        ArrayAdapter<String> sourceAccountAdapter = (ArrayAdapter<String>) addTransferDestinationSpinner.getAdapter();
        int index = sourceAccountAdapter.getCount() - 1;
        while (index >= 0)
        {
            String accountName = sourceAccountAdapter.getItem(index);
            if (accountName.equals(destinationName))
            {
                addTransferDestinationSpinner.setSelection(index);
                return;
            }
            index--;
        }
        //It is an error to reach here
    }

    /**
     * Private helper method to setup necessary views for the transfer form; invoked regardless
     * of whether first time setup or restoring from a saved state. Sets necessary conditions
     * for editing also.
     */
    private void setup()
    {
        addTransferDescription = (EditText) findViewById(R.id.addTransferDescription);

        addTransferSourceSpinner = (Spinner) findViewById(R.id.addTransferSource);
        addTransferDestinationSpinner = (Spinner) findViewById(R.id.addTransferDestination);

        addTransferAmount = (EditText) findViewById(R.id.addTransferAmount);


        /* Setup Frequency Spinner, we will need a listener for our frequency selection as we will update
        * the amount, if set, to match the new frequency. */
        addTransferFrequencySpinner = (Spinner) findViewById(R.id.addTransferFrequency);
        addTransferFrequencySpinner.setOnItemSelectedListener(this);

        nextDateText = (TextView) findViewById(R.id.addTransferNextDate);
        endDateText = (TextView) findViewById(R.id.addTransferEndDate);
        ongoingCheckbox = (CheckBox) findViewById(R.id.addTransferOngoingCheckbox);

        nextDate = null;
        endDate = null;

        /*
        Setup the lists of source and destination accounts. If the user selects a savings account as
        the destination then the source cannot be a savings account (if this is the case on submit should
        show a toast indicating to the user to do a contribution instead).
         */
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        ArrayList<String> sourceNames = new ArrayList<String>();
        for (Account account : bbd.getAccounts())
        {
            sourceNames.add(account.name());
        }

        ArrayList<String> destinationNames = new ArrayList<String>();
        for (Account account : bbd.getAccounts())
        {
            destinationNames.add(account.name());
        }

        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sourceNames);
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        addTransferSourceSpinner.setAdapter(sourceAdapter);

        ArrayAdapter<String> destinationAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, destinationNames);
        destinationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        addTransferDestinationSpinner.setAdapter(destinationAdapter);

        //Check if we are editing an existing transfer
        Bundle args = getIntent().getExtras();

        if (args != null)
        {
            editing = args.getBoolean(BadBudgetApplication.EDIT_KEY);
            if (editing)
            {
                this.setToolbarTitle(R.string.add_transfer_activity_edit_title);
                //Get the name of the transfer we are editing
                editName = args.getString(BadBudgetApplication.EDIT_OBJECT_ID_KEY);
                MoneyTransfer editTransfer = ((BadBudgetApplication)this.getApplication()).getBadBudgetUserData().getTransferWithDescription(editName);

                //Make loss description not editable
                addTransferDescription.setEnabled(false);
                addTransferDescription.setText(editTransfer.getTransferDescription());

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

         /* Instantiate our toast used for messages to the user on submit for errors */
        submitErrorToast = Toast.makeText(this, R.string.add_transfer_use_contribution_message, Toast.LENGTH_SHORT);
        submitErrorToast.setGravity(Gravity.CENTER, 0, 0);
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
     * Method called when the next date's text is clicked. Initializes and shows to the user a
     * date picker fragment.
     * @param view - the text view that was clicked
     */
    public void nextDateClicked(View view)
    {
        dateClicked(BadBudgetApplication.NEXT_DATE_SET_CODE, "nextDatePicker");
    }

    /**
     * Method called when the end date's text is clicked. Initializes and shows to the user a
     * date picker fragment.
     * @param view - the text view that was clicked
     */
    public void endDateClicked(View view)
    {
        dateClicked(BadBudgetApplication.END_DATE_SET_CODE, "endDatePicker");
    }

    /**
     * Private helper method. Called when one of the three dates is clicked on indicating the user
     * wishes to set a date. Passed a code indicating which date was clicked and a tag identifying
     * the fragment that will be created for date picking. Uses the today date from the BB application class
     *
     * @param code - the code identifying which date is being chosen (constants in AddTransferActivity, this class)
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
            case BadBudgetApplication.NEXT_DATE_SET_CODE: {
                nextDateSet(year, month, day);
                break;
            }
            case BadBudgetApplication.END_DATE_SET_CODE: {
                endDateSet(year, month, day);
                break;
            }
        }
    }

    /**
     * Method called when the next date has been chosen by the user from the date fragment. Sets the
     * next date (i.e. the instance date object) and the text for the nextDate view to match what
     * the user chose. Also does some checking against any currently set end date. If
     * the end date occurs before the next date set then the end date is cleared. (instance set to
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
     * Implementation method for the frequency spinner listener. This method is called when an item is selected
     * in our frequency dropdown. This uses the strings defined in our string resource file to
     * convert the selected dropdown string to a frequency that can be used to create our
     * MoneyTransfer. Then if applicable (i.e the frequency changed and neither the old or new frequency
     * was a one time frequency and the the loss amount field wasn't empty) we auto update
     * the amount to be the corresponding amount at the new frequency (same as the toggle function
     * in our budget item list). A one time frequency simply retains the current amount in the
     * amount field.
     * @param parent - the Adapter view where the selection happened
     * @param view - the view within the AdapterView that was clicked
     * @param position - the position of the view in the adapter
     * @param id - the row id of the item that is selected
     */
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        Frequency oldFrequency = this.frequency;
        String frequencyText = ((TextView)view).getText().toString();
        if (frequencyText.equals(this.getResources().getText(R.string.one_time).toString()))
        {
            this.frequency = Frequency.oneTime;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.daily).toString()))
        {
            this.frequency= Frequency.daily;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.weekly).toString()))
        {
            this.frequency = Frequency.weekly;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.bi_weekly).toString()))
        {
            this.frequency = Frequency.biWeekly;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.monthly).toString()))
        {
            this.frequency = Frequency.monthly;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.yearly).toString()))
        {
            this.frequency = Frequency.yearly;
        }

        boolean oneTimeFrequency = (oldFrequency == Frequency.oneTime) || (this.frequency == Frequency.oneTime);
        boolean frequencyChanged = oldFrequency != this.frequency;

        if (!oneTimeFrequency && frequencyChanged)
        {
            String lossAmountString = this.addTransferAmount.getText().toString();
            if (!lossAmountString.equals(""))
            {
                double currAmount = Double.parseDouble(lossAmountString);
                double convertedAmount = Prediction.toggle(currAmount, oldFrequency, this.frequency);
                this.addTransferAmount.setText(Double.toString(convertedAmount));
            }
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
     * Should be called whenever the ongoing checkbox is checked. Disables the end date field, clears
     * any end date, and sets the ongoing hint
     */
    private void ongoingChecked()
    {
        endDateText.setClickable(false);
        clearEndDate();
        endDateText.setHint(getString(R.string.add_loss_ongoing));
    }

    /**
     * Should be called whenever the ongoing checkbox is unchecked. Enables the end date field, and sets
     * the hint to the date hint.
     */
    private void ongoingUnchecked()
    {
        endDateText.setClickable(true);
        endDateText.setHint(R.string.add_loss_date_hint);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**
     *
     * Method called when user clicks the submit button for adding a transfer. If the inputs are
     * valid then this method kicks off a task that adds/edits the transfer
     * to the database and to memory.
     * The user is shown a progress dialog until the task is complete. If the inputs are invalid then
     * the submit button does nothing. (the user simply remains on the page) (a toast is shown for some errors)
     * @param view
     */
    public void submitClick(View view)
    {
        int errorCode = verifyValues();
        if (errorCode == 0)
        {
            //Prep for creation of the in memory loss object
            String description = addTransferDescription.getText().toString();

            BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();

            Account source = bbd.getAccountWithName((String)this.addTransferSourceSpinner.getSelectedItem());
            Account destination = bbd.getAccountWithName((String)this.addTransferDestinationSpinner.getSelectedItem());

            double amount = Double.parseDouble(addTransferAmount.getText().toString());

            try
            {
                MoneyTransfer transfer = new MoneyTransfer(description, source, destination, amount, frequency, nextDate, endDate);

                //Prep for creation of the database object
                ContentValues values = new ContentValues();

                values.put(BBDatabaseContract.Transfers.COLUMN_DESCRIPTION, description);
                values.put(BBDatabaseContract.Transfers.COLUMN_SOURCE, source.name());
                values.put(BBDatabaseContract.Transfers.COLUMN_DESTINATION, destination.name());
                values.put(BBDatabaseContract.Transfers.COLUMN_AMOUNT, amount);
                values.put(BBDatabaseContract.Transfers.COLUMN_FREQUENCY, BBDatabaseContract.dbFrequencyToInteger(this.frequency));
                values.put(BBDatabaseContract.Transfers.COLUMN_NEXT_TRANSFER, BBDatabaseContract.dbDateToString(nextDate));
                values.put(BBDatabaseContract.Transfers.COLUMN_END_DATE, BBDatabaseContract.dbDateToString(endDate));

                if (!editing)
                {
                    AddBBObjectTask task = new AddBBObjectTask(this, progressDialogMessage, transfer, values, this, BBObjectType.TRANSFER);
                    task.execute();
                }
                else
                {
                    EditBBObjectTask task = new EditBBObjectTask(this, progressDialogMessageUpdate, transfer, values, this, BBObjectType.TRANSFER);
                    task.execute();
                }

            }
            catch (BadBudgetInvalidValueException e)
            {
                //TODO
                e.printStackTrace();
            }

        }
        else if (errorCode == 3)
        {
            if (!submitErrorToast.getView().isShown())
            {
                submitErrorToast.setText(R.string.add_transfer_use_contribution_message);
                submitErrorToast.show();
            }
        }
    }

    /**
     * Verifies the forms current values and returns an eror code indicating the status of the current
     * filled values:
     *
     * 0 - values are valid
     * 1 - no name or duplicate name entered
     * 2 - no source and/or destination set
     * 3 - destination is a savings account and source is a regular (non-savings) account
     * 4 - no amount entered
     * 5 - negative amount entered
     * 6 - invalid next date
     * 7 - invalid end date
     *
     * @return - an error code indicating the status of the currently entered values in the form
     */
    public int verifyValues()
    {
        //Check to make sure the user input a unique name for the transfer
        BadBudgetData bbd = ((BadBudgetApplication)this.getApplication()).getBadBudgetUserData();
        String description = addTransferDescription.getText().toString();
        if (description.equals("") || (!editing && bbd.getTransferWithDescription(description) != null))
        {
            return 1;
        }

        //Make sure source and destination account are set to something and that if the destination is a
        //savings account that the source is not just a regular account
        String sourceName = (String)addTransferSourceSpinner.getSelectedItem();
        String destinationName = (String)addTransferDestinationSpinner.getSelectedItem();
        if (sourceName == null || destinationName == null)
        {
            return 2;
        }

        if (bbd.getAccountWithName(destinationName) instanceof SavingsAccount && !(bbd.getAccountWithName(sourceName) instanceof SavingsAccount))
        {
            return 3;
        }

        //Make sure the user input a valid amount for this transfer
        String transferAmount = addTransferAmount.getText().toString();
        if (transferAmount.equals(""))
        {
            return 4;
        }

        if (Double.parseDouble(transferAmount) < 0)
        {
            return 5;
        }

        //Make sure their is a next date.
        if (nextDate == null)
        {
            return 6;
        }

        //Make sure there is an end date or that the ongoing checkbox is checked
        if (endDate == null && !ongoingCheckbox.isChecked())
        {
            return 7;
        }

        return 0;
    }

    /**
     * Method called after the task adding the transfer has completed. Ends this activity.
     */
    public void addBBObjectFinished() {
        setResult(BadBudgetApplication.FORM_RESULT_ADD);
        this.finish();
    }

    /**
     * Method called after the task to edit a transfer has finished. Ends this activity
     */
    public void editBBObjectFinished()
    {
        setResult(BadBudgetApplication.FORM_RESULT_EDIT);
        this.finish();
    }

    /**
     * Method called after the task deleting a transfer has completed. Finishes this activty.
     */
    public void deleteBBObjectFinished()
    {
        setResult(BadBudgetApplication.FORM_RESULT_DELETE);
        this.finish();
    }

    /**
     *
     * Method called when the delete button is pressed. Kicks off the task to remove the transfer
     * from memory and from the database.
     * @param view - the button pressed
     */
    public void deleteClick(View view)
    {
        DeleteBBObjectTask task = new DeleteBBObjectTask(this, progressDialogMessageDelete, editName, this, BBObjectType.TRANSFER);
        task.execute();
    }
}
