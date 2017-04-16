package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.content.ContentValues;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseContract;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetChildActivity;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetInvalidValueException;
import com.erikartymiuk.badbudgetlogic.main.Frequency;
import com.erikartymiuk.badbudgetlogic.main.MoneyLoss;
import com.erikartymiuk.badbudgetlogic.main.Prediction;
import com.erikartymiuk.badbudgetlogic.main.Source;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


/**
 * Activity to display a form to the user, accept their input, verify their input, and if valid add
 * a specified loss object to the bbd data and bb database.
 *
 * Also the form used to edit an existing loss
 */
public class AddLossActivity extends BadBudgetChildActivity implements BBOperationTaskCaller, AdapterView.OnItemSelectedListener, DateInputActivity {

    private EditText addLossDescriptionInput;
    private EditText addLossAmountInput;

    private Spinner addLossFrequencySpinner;
    private Frequency frequency;

    private TextView nextDateText;
    private Date nextDate;
    private TextView endDateText;
    private Date endDate;
    private CheckBox ongoingCheckbox;

    private Spinner addLossSourceSpinner;

    private static final String progressDialogMessage = "Adding Loss...";
    private static final String progressDialogMessageDelete = "Deleting Loss...";
    private static final String progressDialogMessageUpdate = "Updating Loss...";

    private boolean editing;
    private String editName;

    /* Codes to identify which date is being set in the callback dateSet */
    private static final int NEXT_DATE_SET_CODE = 0;
    private static final int END_DATE_SET_CODE = 1;

    /* Saved state keys */
    private static final String LOSS_NAME_KEY = "LOSS_NAME";
    private static final String FREQUENCY_KEY = "FREQUENCY";
    private static final String AMOUNT_KEY = "AMOUNT";
    private static final String NEXT_DATE_KEY = "NEXT_DATE";
    private static final String END_DATE_KEY = "END_DATE";
    private static final String ONGOING_KEY = "ONGOING";
    private static final String SOURCE_KEY = "SOURCE";

    /**
     * On create for the add loss activity. Sets the content view and sets up both for
     * a first time entry and for when we have saved state to restore
     * @param savedInstanceState - the state to restore
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(R.layout.content_add_loss);

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
     * Private helper method to setup necessary views for the loss form; invoked regardless
     * of whether first time setup or restoring from a saved state. Sets necessary conditions
     * for editing also.
     */
    private void setup()
    {
        addLossDescriptionInput = (EditText) findViewById(R.id.addLossDescriptionInput);
        addLossAmountInput = (EditText) findViewById(R.id.addLossAmountInput);

        /* Setup Frequency Spinner, we will need a listener for our frequency selection as we will update
        * the amount, if set, to match the new frequency. */
        addLossFrequencySpinner = (Spinner) findViewById(R.id.addLossFrequencySpinner);
        addLossFrequencySpinner.setOnItemSelectedListener(this);

        nextDateText = (TextView) findViewById(R.id.addLossNextInput);
        endDateText = (TextView) findViewById(R.id.addLossEndInput);
        ongoingCheckbox = (CheckBox) findViewById(R.id.addLossOngoingCheckbox);

        nextDate = null;
        endDate = null;

        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
        ArrayList<String> sourceNames = new ArrayList<String>();
        for (Source source : bbd.getSourcesExcludeSavingAccounts())
        {
            sourceNames.add(source.name());
        }

        addLossSourceSpinner = (Spinner) findViewById(R.id.addLossSourceSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sourceNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        addLossSourceSpinner.setAdapter(adapter);

        //Check if we are editing an existing loss
        Bundle args = getIntent().getExtras();

        if (args != null)
        {
            editing = args.getBoolean(BadBudgetApplication.EDIT_KEY);
            if (editing)
            {
                this.setToolbarTitle(R.string.add_loss_activity__edit_title);
                //Get the name of the loss we are editing
                editName = args.getString(BadBudgetApplication.EDIT_OBJECT_ID_KEY);
                MoneyLoss editLoss = ((BadBudgetApplication)this.getApplication()).getBadBudgetUserData().getLossWithDescription(editName);

                //Make loss description not editable
                addLossDescriptionInput.setEnabled(false);
                addLossDescriptionInput.setText(editLoss.expenseDescription());

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
        //Setup the starting frequency as monthly
        frequency = Frequency.monthly;
        addLossFrequencySpinner.setSelection(getResources().getInteger(R.integer.monthly_index));

        if (editing)
        {
            MoneyLoss editLoss = ((BadBudgetApplication)this.getApplication()).getBadBudgetUserData().getLossWithDescription(editName);
            addLossAmountInput.setText(Double.toString(editLoss.lossAmount()));
            setPrepopulateAddLossFreq(editLoss.lossFrequency());
            setNextDate(editLoss.nextLoss());

            Date tempEndDate = editLoss.endDate();
            if (tempEndDate != null)
            {
                setEndDate(editLoss.endDate());
            }
            else
            {
                ongoingCheckbox.setChecked(true);
                ongoingChecked();
            }

            setPrepopulateAccountSource(editLoss.source().name());
        }
    }

    /**
     * Sets up our form when the user returns to it the state they left it in should be restored.
     * @param savedInstanceState - the state to restore
     */
    private void savedStateSetup(Bundle savedInstanceState)
    {
        addLossDescriptionInput.setText(savedInstanceState.getString(LOSS_NAME_KEY));
        setPrepopulateAddLossFreq((Frequency)savedInstanceState.getSerializable(FREQUENCY_KEY));
        addLossAmountInput.setText(savedInstanceState.getString(AMOUNT_KEY));
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

        setPrepopulateAccountSource(savedInstanceState.getString(SOURCE_KEY));
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
        outState.putString(LOSS_NAME_KEY, addLossDescriptionInput.getText().toString());
        outState.putSerializable(FREQUENCY_KEY, frequency);
        outState.putString(AMOUNT_KEY, addLossAmountInput.getText().toString());
        outState.putSerializable(NEXT_DATE_KEY, nextDate);
        outState.putSerializable(END_DATE_KEY, endDate);
        outState.putBoolean(ONGOING_KEY, ongoingCheckbox.isChecked());
        outState.putString(SOURCE_KEY, (String)addLossSourceSpinner.getSelectedItem());
    }

    /**
     * Method called when the next date's text is clicked. Initializes and shows to the user a
     * date picker fragment.
     * @param view - the text view that was clicked
     */
    public void nextDateClicked(View view)
    {
        dateClicked(AddLossActivity.NEXT_DATE_SET_CODE, "nextDatePicker");
    }

    /**
     * Method called when the end date's text is clicked. Initializes and shows to the user a
     * date picker fragment.
     * @param view - the text view that was clicked
     */
    public void endDateClicked(View view)
    {
        dateClicked(AddLossActivity.END_DATE_SET_CODE, "endDatePicker");
    }

    /**
     * Private helper method. Called when one of the three dates is clicked on indicating the user
     * wishes to set a date. Passed a code indicating which date was clicked and a tag identifying
     * the fragment that will be created for date picking. Uses the today date from the BB application class
     * TODO - put constants in Application class as used multiple places
     * @param code - the code identifying which date is being chosen (constants in AddLossActivity, this class)
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
            case NEXT_DATE_SET_CODE: {
                nextDateSet(year, month, day);
                break;
            }
            case END_DATE_SET_CODE: {
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
     * Helper method prepopulating the account source spinner to match the name of the passed in
     * account name. Assumes the sourceName is the name of a valid account in our bb data.
     * @param sourceName - the name of the source account to set our spinner to.
     */
    private void setPrepopulateAccountSource(String sourceName)
    {
        ArrayAdapter<String> sourceAccountAdapter = (ArrayAdapter<String>) addLossSourceSpinner.getAdapter();
        int index = sourceAccountAdapter.getCount() - 1;
        while (index >= 0)
        {
            String accountName = sourceAccountAdapter.getItem(index);
            if (accountName.equals(sourceName))
            {
                addLossSourceSpinner.setSelection(index);
                return;
            }
            index--;
        }
        //It is an error to reach here
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
     * Prepopulates the frequency spinner to the passed frequency.
     * @param frequency
     */
    private void setPrepopulateAddLossFreq(Frequency frequency)
    {
        this.frequency = frequency;
        switch (this.frequency)
        {
            case oneTime:
                addLossFrequencySpinner.setSelection(BadBudgetApplication.ONE_TIME_INDEX);
                break;
            case daily:
                addLossFrequencySpinner.setSelection(BadBudgetApplication.DAILY_INDEX);
                break;
            case weekly:
                addLossFrequencySpinner.setSelection(BadBudgetApplication.WEEKLY_INDEX);
                break;
            case biWeekly:
                addLossFrequencySpinner.setSelection(BadBudgetApplication.BIWEEKLY_INDEX);
                break;
            case monthly:
                addLossFrequencySpinner.setSelection(BadBudgetApplication.MONTHLY_INDEX);
                break;
            case yearly:
                addLossFrequencySpinner.setSelection(BadBudgetApplication.YEARLY_INDEX);
                break;
        }
    }

    /**
     * Method called when user clicks the submit (or next) button for adding a loss. If the inputs are
     * valid then this method kicks off a task that adds/edits the loss
     * to the database and to memory.
     * The user is shown a progress dialog until the task is complete. If the inputs are invalid then
     * the submit button does nothing. (the user simply remains on the page)
     * @param view
     */
    public void submitClick(View view)
    {
        if (verifyValues())
        {
            //Prep for creation of the in memory loss object
            String description = addLossDescriptionInput.getText().toString();
            double lossAmount = Double.parseDouble(addLossAmountInput.getText().toString());

            BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
            Source source = bbd.getSourceWithNameExcludeSavingAccounts((String)this.addLossSourceSpinner.getSelectedItem());

            try
            {
                MoneyLoss loss = new MoneyLoss(description, lossAmount, this.frequency, this.nextDate, this.endDate, source);

                //Prep for creation of the database object
                ContentValues values = new ContentValues();
                values.put(BBDatabaseContract.Losses.COLUMN_EXPENSE, description);
                values.put(BBDatabaseContract.Losses.COLUMN_AMOUNT, lossAmount);
                values.put(BBDatabaseContract.Losses.COLUMN_FREQUENCY, BBDatabaseContract.dbFrequencyToInteger(this.frequency));
                values.put(BBDatabaseContract.Losses.COLUMN_NEXT_LOSS, BBDatabaseContract.dbDateToString(nextDate));
                values.put(BBDatabaseContract.Losses.COLUMN_END_DATE, BBDatabaseContract.dbDateToString(endDate));
                values.put(BBDatabaseContract.Losses.COLUMN_SOURCE, source.name());

                if (!editing)
                {
                    AddBBObjectTask task = new AddBBObjectTask(this, progressDialogMessage, loss, values, this, BBObjectType.LOSS);
                    task.execute();
                }
                else
                {
                    EditBBObjectTask task = new EditBBObjectTask(this, progressDialogMessageUpdate, loss, values, this, BBObjectType.LOSS);
                    task.execute();
                }
            }
            catch (BadBudgetInvalidValueException e)
            {
                //TODO
                e.printStackTrace();
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
     * Verfies that the user has input a unique name/description (if they are not editing)
     * an amount, a next date, an end date (or ongoing is checked), and a destination account.
     * @return true if input values are valid
     */
    public boolean verifyValues()
    {
        //Check to make sure the user input a unique name and an amount for the loss
        BadBudgetData bbd = ((BadBudgetApplication)this.getApplication()).getBadBudgetUserData();
        String description = addLossDescriptionInput.getText().toString();
        if (description.equals("") || (!editing && bbd.getLossWithDescription(description) != null))
        {
            return false;
        }

        //Make sure the user input an amount for this loss
        String lossAmount = addLossAmountInput.getText().toString();
        if (lossAmount.equals(""))
        {
            return false;
        }

        //Make sure their is a next date.
        if (nextDate == null)
        {
            return false;
        }

        //Make sure there is an end date or that the ongoing checkbox is checked
        if (endDate == null && !ongoingCheckbox.isChecked())
        {
            return false;
        }

        //Make sure source account is set to something
        if (addLossSourceSpinner.getSelectedItem() == null)
        {
            return false;
        }

        return true;
    }

    /**
     * Method called after the task adding the loss has completed. Ends this activity.
     */
    public void addBBObjectFinished() {
        setResult(BadBudgetApplication.FORM_RESULT_ADD);
        this.finish();
    }

    /**
     * Method called after the task to edit a loss has finished. Ends this activity
     */
    public void editBBObjectFinished()
    {
        setResult(BadBudgetApplication.FORM_RESULT_EDIT);
        this.finish();
    }

    /**
     * Method called after the task deleting a loss has completed. Finishes this activty.
     */
    public void deleteBBObjectFinished()
    {
        setResult(BadBudgetApplication.FORM_RESULT_DELETE);
        this.finish();
    }

    /**
     * Method called when the delete button is pressed. Kicks off the task to remove the loss
     * from memory and from the database.
     * @param view - the button pressed
     */
    public void deleteClick(View view)
    {
        DeleteBBObjectTask task = new DeleteBBObjectTask(this, progressDialogMessageDelete, editName, this, BBObjectType.LOSS);
        task.execute();
    }

    /**
     * Implementation method for the frequency spinner listener. This method is called when an item is selected
     * in our frequency dropdown. This uses the strings defined in our string resource file to
     * convert the selected dropdown string to a frequency that can be used to create our
     * MoneyLoss. Then if applicable (i.e the frequency changed and neither the old or new frequency
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
            String lossAmountString = this.addLossAmountInput.getText().toString();
            if (!lossAmountString.equals(""))
            {
                double currAmount = Double.parseDouble(lossAmountString);
                double convertedAmount = Prediction.toggle(currAmount, oldFrequency, this.frequency);
                this.addLossAmountInput.setText(Double.toString(convertedAmount));
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
