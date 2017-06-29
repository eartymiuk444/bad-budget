package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseContract;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetBaseActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetChildActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.PredictActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.TrackActivity;
import com.badbudget.erikartymiuk.badbudget.viewobjecttables.BudgetSetActivity;
import com.badbudget.erikartymiuk.badbudget.viewobjecttables.LossesActivity;
import com.erikartymiuk.badbudgetlogic.budget.Budget;
import com.erikartymiuk.badbudgetlogic.budget.BudgetItem;
import com.erikartymiuk.badbudgetlogic.main.Account;
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
 * Input form for adding or editing a budget item.
 */
public class AddBudgetItemActivity extends BadBudgetChildActivity implements AdapterView.OnItemSelectedListener, BBOperationTaskCaller, DateInputActivity, TextWatcher {

    /* Layout fields */
    private EditText addItemDescription;
    private Spinner addItemFreqSpinner;
    private EditText addItemAmount;
    private TextView nextDateText;
    private CheckBox proratedCheckbox;
    private TextView endDateText;
    private CheckBox ongoingCheckbox;
    
    /* Instance fields that need to be kept up to date */
    private Date nextDate;
    private Date endDate;
    private Frequency frequency;
    
    /* Dialog Message for diplay when adding, editing, or deleting an item */
    private static final String progressDialogMessage = "Adding Item...";
    private static final String progressDialogMessageDelete = "Deleting Item...";
    private static final String progressDialogMessageUpdate = "Updating Item...";

    /* Fields to know if we are editing and what we are if we are */
    private boolean editing;
    private String editItemName;

    /* Codes to identify which date is being set in the callback dateSet */
    private static final int NEXT_DATE_SET_CODE = 0;
    private static final int END_DATE_SET_CODE = 1;

    /* Saved state keys */
    private static final String LOSS_NAME_KEY = "LOSS_NAME";
    private static final String FREQUENCY_KEY = "FREQUENCY";
    private static final String AMOUNT_KEY = "AMOUNT";
    private static final String NEXT_DATE_KEY = "NEXT_DATE";
    private static final String PRORATED_KEY = "PRORATED";
    private static final String END_DATE_KEY = "END_DATE";
    private static final String ONGOING_KEY = "ONGOING";

    /**
     * On create for the add budget item activity. Sets the content view and sets up both for
     * a first time entry and for when we have saved state to restore
     * @param savedInstanceState - the state to restore
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BadBudgetApplication application = ((BadBudgetApplication)this.getApplication());
        if (application.getBadBudgetUserData() == null)
        {
            this.finish();
            return;
        }

        setContent(R.layout.content_add_budget_item);

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
     * Private helper method to setup necessary views for the budget item form; invoked regardless
     * of whether first time setup or restoring from a saved state. Sets necessary conditions
     * for editing also.
     */
    private void setup()
    {
        /* Get all the layout views that we'll need */
        addItemDescription = (EditText) findViewById(R.id.addBudgetItemDescriptionInput);
        addItemFreqSpinner = (Spinner) findViewById(R.id.addBudgetItemFrequencySpinner);
        addItemAmount = (EditText) findViewById(R.id.addBudgetItemAmountInput);
        nextDateText = (TextView) findViewById(R.id.addBudgetItemNextInput);
        proratedCheckbox = (CheckBox) findViewById(R.id.addBudgetItemProratedCheckbox);
        endDateText = (TextView) findViewById(R.id.addBudgetItemEndInput);
        ongoingCheckbox = (CheckBox) findViewById(R.id.addBudgetItemOngoingCheckbox);

        /* Initialize necessary fields */
        addItemAmount.addTextChangedListener(this);
        nextDate = null;
        endDate = null;
        addItemFreqSpinner.setOnItemSelectedListener(this);

        //Check if we are editing an existing budget item
        Bundle args = getIntent().getExtras();

        if (args != null)
        {
            editing = args.getBoolean(BadBudgetApplication.EDIT_KEY);
            if (editing)
            {
                /* If we are editing do the necessary setup/adjustments */
                this.setToolbarTitle(R.string.add_budget_item_activity__edit_title);
                editItemName = args.getString(BadBudgetApplication.EDIT_OBJECT_ID_KEY);
                addItemDescription.setEnabled(false);
                addItemDescription.setText(editItemName);

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
        //Setup the starting frequency as weekly
        addItemFreqSpinner.setSelection(getResources().getInteger(R.integer.weekly_index));
        frequency = Frequency.weekly;

        if (editing)
        {
            BudgetItem editItem = ((BadBudgetApplication)this.getApplication()).getBadBudgetUserData().getBudget().retrieveBudgetItem(editItemName);
            addItemAmount.setText(Double.toString(editItem.lossAmount()));
            setPrepopulateAddItemFreq(editItem.lossFrequency());
            setNextDate(editItem.nextLoss());
            Date tempEndDate = editItem.endDate();
            if (tempEndDate != null)
            {
                setEndDate(editItem.endDate());
            }
            else
            {
                ongoingCheckbox.setChecked(true);
                ongoingChecked();
            }
            if (editItem.isProratedStart())
            {
                proratedCheckbox.setChecked(true);
                proratedChecked();
            }
        }
    }

    /**
     * Sets up our form when the user returns to it the state they left it in should be restored.
     * @param savedInstanceState - the state to restore
     */
    private void savedStateSetup(Bundle savedInstanceState)
    {
        addItemDescription.setText(savedInstanceState.getString(LOSS_NAME_KEY));
        setPrepopulateAddItemFreq((Frequency)savedInstanceState.getSerializable(FREQUENCY_KEY));
        addItemAmount.setText(savedInstanceState.getString(AMOUNT_KEY));
        setNextDate((Date)savedInstanceState.getSerializable(NEXT_DATE_KEY));

        boolean ongoing = savedInstanceState.getBoolean(ONGOING_KEY);
        if (!ongoing)
        {
            setEndDate((Date) savedInstanceState.getSerializable(END_DATE_KEY));
        }
        else
        {
            ongoingCheckbox.setChecked(true);
            ongoingChecked();
        }
        boolean prorated = savedInstanceState.getBoolean(PRORATED_KEY);
        if (prorated)
        {
            proratedCheckbox.setChecked(true);
            proratedChecked();
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
        outState.putString(LOSS_NAME_KEY, addItemDescription.getText().toString());
        outState.putSerializable(FREQUENCY_KEY, frequency);
        outState.putString(AMOUNT_KEY, addItemAmount.getText().toString());
        outState.putSerializable(NEXT_DATE_KEY, nextDate);
        outState.putBoolean(PRORATED_KEY, proratedCheckbox.isChecked());
        outState.putSerializable(END_DATE_KEY, endDate);
        outState.putBoolean(ONGOING_KEY, ongoingCheckbox.isChecked());
    }

    /**
     * Method called when the submit button is pressed. Verifies the values input by the user
     * and if they form a valid budget item then this method kicks off a background task to
     * add/edit the budget item to the database and to memory. If the inputs are invalid then
     * this task does nothing and the user simply remains on the form page.
     * @param view
     */
    public void submitClick(View view)
    {
        if (verifyValues())
        {
            /* Prepare the values for creation of the budget item object */

            String description = addItemDescription.getText().toString();
            double amount = Double.parseDouble(addItemAmount.getText().toString());
            BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();

            BudgetItem bItem = null;
            try
            {
                bItem = new BudgetItem(description, amount, this.frequency, this.nextDate, this.endDate,
                        this.proratedCheckbox.isChecked(), bbd.getBudget().getBudgetSource());

                 /* Prepare the values for insertion into the database */
                ContentValues values = new ContentValues();
                values.put(BBDatabaseContract.BudgetItems.COLUMN_DESCRIPTION, description);
                values.put(BBDatabaseContract.BudgetItems.COLUMN_AMOUNT, amount);
                values.put(BBDatabaseContract.BudgetItems.COLUMN_FREQUENCY, BBDatabaseContract.dbFrequencyToInteger(this.frequency));
                values.put(BBDatabaseContract.BudgetItems.COLUMN_NEXT_LOSS, BBDatabaseContract.dbDateToString(this.nextDate));
                values.put(BBDatabaseContract.BudgetItems.COLUMN_END_DATE, BBDatabaseContract.dbDateToString(this.endDate));
                values.put(BBDatabaseContract.BudgetItems.COLUMN_PRORATED_START, this.proratedCheckbox.isChecked());

                if (!editing)
                {
                    //Tracker fields
                    bItem.setMinusAmount(1.0);
                    bItem.setPlusAmount(1.0);
                    bItem.setCurrAmount(0.0);
                    values.put(BBDatabaseContract.BudgetItems.COLUMN_MINUS_AMOUNT, 1.0);
                    values.put(BBDatabaseContract.BudgetItems.COLUMN_PLUS_AMOUNT, 1.0);
                    values.put(BBDatabaseContract.BudgetItems.COLUMN_REMAINING_AMOUNT, 0.0);

                    AddBBObjectTask task = new AddBBObjectTask(this, progressDialogMessage, bItem, values, this, BBObjectType.BUDGETITEM);
                    task.execute();
                }
                else
                {
                    //Tracker field
                    BudgetItem editItem = ((BadBudgetApplication)this.getApplication()).getBadBudgetUserData().getBudget().retrieveBudgetItem(editItemName);
                    bItem.setMinusAmount(editItem.getMinusAmount());
                    bItem.setPlusAmount(editItem.getPlusAmount());
                    bItem.setCurrAmount(editItem.getCurrAmount());
                    values.put(BBDatabaseContract.BudgetItems.COLUMN_MINUS_AMOUNT, editItem.getMinusAmount());
                    values.put(BBDatabaseContract.BudgetItems.COLUMN_PLUS_AMOUNT, editItem.getPlusAmount());
                    values.put(BBDatabaseContract.BudgetItems.COLUMN_REMAINING_AMOUNT, editItem.getCurrAmount());

                    EditBBObjectTask task = new EditBBObjectTask(this, progressDialogMessageUpdate, bItem, values, this, BBObjectType.BUDGETITEM);
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
     * Checks the current input values for validity. Includes checking the name for uniqueness (if not editing)
     * and that it
     * is nonempty. Checking that the budget amount has been entered. That the next date is nonempty,
     * and that the end date is nonempty or if it is empty that ongoing has been checked.
     *
     * @return - true if entered values are valid for creation of a new budget item
     */
    public boolean verifyValues()
    {
        BadBudgetData bbd = ((BadBudgetApplication) getApplication()).getBadBudgetUserData();
        boolean descriptionValid = !addItemDescription.getText().toString().equals("") &&
                (editing || bbd.getBudget().retrieveBudgetItem(addItemDescription.getText().toString()) == null);
        boolean amountValid = !addItemAmount.getText().toString().equals("");
        boolean nextDateValid = nextDate != null;
        boolean endDateValid = endDate != null || ongoingCheckbox.isChecked();

        return descriptionValid && amountValid && nextDateValid && endDateValid;
    }

    /**
     * Method called when the next date's text is clicked. Initializes and shows to the user a
     * date picker fragment.
     * @param view - the text view that was clicked
     */
    public void nextDateClicked(View view)
    {
        dateClicked(AddBudgetItemActivity.NEXT_DATE_SET_CODE, "nextDatePicker");
    }

    /**
     * Method called when the end date's text is clicked. Initializes and shows to the user a
     * date picker fragment.
     * @param view - the text view that was clicked
     */
    public void endDateClicked(View view)
    {
        dateClicked(AddBudgetItemActivity.END_DATE_SET_CODE, "endDatePicker");
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
     *
     * Also updates the prorated amount if it makes sense to do so
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

        if (proratedCheckbox.isChecked())
        {
            updateProratedAmountText();
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
     * Method called when the prorated checkbox is checked. Indicates to the user that the next
     * will now be prorated
     * @param view
     */
    public void proratedCheckboxChecked(View view)
    {
        boolean checked = proratedCheckbox.isChecked();
        if (checked)
        {
            proratedChecked();
        }
        else
        {
            proratedUnchecked();
        }
    }

    /**
     * Should be called after the prorated checkbox is checked either by the user or programmatically
     * Adds a message to the prorated checkbox indicating that the next will now be prorated and
     * if the next date and amount are set TODO shows the user what that prorated amount would be.
     */
    private void proratedChecked()
    {
        updateProratedAmountText();
    }

    /**
     * If a prorated loss makes sense given the currently filled in data then this method then calculates
     * that loss and updates the prorated checkbox text such that it displays that loss. If a prorated
     * loss cannot be caluclated then this method set the prorated checkbox text to the vanilla prorated
     * message. This assumes that the prorated checkbox is checked
     */
    private void updateProratedAmountText()
    {
        if (canCalculateProratedAmount())
        {
            //TODO implement calculateProratedLoss method
            Budget userBudget = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData().getBudget();
            Double proratedLoss = Double.parseDouble(addItemAmount.getText().toString());//userBudget.calculateProratedLoss(Double.parseDouble(addItemAmount.getText().toString()), this.frequency, this.nextDate);
            //this.proratedCheckbox.setText(" (" + getString(R.string.add_budget_item_prorated) + " " + proratedLoss + ")");
            //TODO removing prorated loss holder for release original above 3/17/2017
            this.proratedCheckbox.setText(" (" + getString(R.string.add_budget_item_prorated) + " " + ")");

        }
        else
        {
            this.proratedCheckbox.setText(" (" + getString(R.string.add_budget_item_prorated) + ")");
        }
    }

    /**
     * Returns true if the frequency, amount, and next date are all setup such that a prorated amount
     * can be calculated and makes sense. Also requires that the prorated checkbox is checked.
     * Returns false otherwise
     * @return - true if a prorated amount makes sense to calculate with the currently filled in values,
     *              false otherwise
     */
    private boolean canCalculateProratedAmount()
    {
        boolean proratedCheckboxChecked = this.proratedCheckbox.isChecked();
        boolean supportedFrequency = (frequency == Frequency.weekly || frequency == Frequency.monthly);
        boolean budgetAmountSet = !addItemAmount.getText().toString().equals("");
        boolean nextDateSet = nextDate != null;
        return proratedCheckboxChecked && supportedFrequency && budgetAmountSet && nextDateSet;
    }

    /**
     * Should be called after the prorated checkbox is unchecked either by the user or programmatically
     * Removes the message from the prorated checkbox
     */
    private void proratedUnchecked()
    {
        this.proratedCheckbox.setText("");
    }

    /**
     * Should be called whenever the ongoing checkbox is checked. Disables the end date field, clears
     * any end date, and sets the ongoing hint
     */
    private void ongoingChecked()
    {
        endDateText.setClickable(false);
        clearEndDate();
        endDateText.setHint(getString(R.string.add_budget_item_ongoing));
    }

    /**
     * Should be called whenever the ongoing checkbox is unchecked. Enables the end date field, and sets
     * the hint to the date hint.
     */
    private void ongoingUnchecked()
    {
        endDateText.setClickable(true);
        endDateText.setHint(R.string.add_budget_item_date_hint);
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
        updateProratedAmountText();
    }

    /**
     * Prepopulates the frequency spinner to the passed frequency.
     * @param frequency
     */
    private void setPrepopulateAddItemFreq(Frequency frequency)
    {
        this.frequency = frequency;
        switch (this.frequency)
        {
            case oneTime:
                addItemFreqSpinner.setSelection(BadBudgetApplication.ONE_TIME_INDEX);
                break;
            case daily:
                addItemFreqSpinner.setSelection(BadBudgetApplication.DAILY_INDEX);
                break;
            case weekly:
                addItemFreqSpinner.setSelection(BadBudgetApplication.WEEKLY_INDEX);
                break;
            case biWeekly:
                addItemFreqSpinner.setSelection(BadBudgetApplication.BIWEEKLY_INDEX);
                break;
            case monthly:
                addItemFreqSpinner.setSelection(BadBudgetApplication.MONTHLY_INDEX);
                break;
            case yearly:
                addItemFreqSpinner.setSelection(BadBudgetApplication.YEARLY_INDEX);
                break;
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
     * Method called after the task adding the budget item has completed. Ends this activity.
     */
    public void addBBObjectFinished()
    {
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
     * Method called after the task deleting a budget item has completed. Finishes this activty.
     */
    public void deleteBBObjectFinished()
    {
        setResult(BadBudgetApplication.FORM_RESULT_DELETE);
        this.finish();
    }

    /**
     * Method called when the delete button is pressed. Kicks off the task to remove the budget item
     * from memory and from the database.
     * @param view - the button pressed
     */
    public void deleteClick(View view)
    {
        DeleteBBObjectTask task = new DeleteBBObjectTask(this, progressDialogMessageDelete, editItemName, this, BBObjectType.BUDGETITEM);
        task.execute();
    }

    /**
     * Implementation method for the frequency spinner listener. This method is called when an item is selected
     * in our frequency dropdown. This uses the strings defined in our string resource file to
     * convert the selected dropdown string to a frequency that can be used to create our
     * BudgetItem. Then if applicable (i.e the frequency changed and neither the old or new frequency
     * was a one time frequency and the the loss amount field wasn't empty) we auto update
     * the amount to be the corresponding amount at the new frequency (same as the toggle function
     * in our budget item list). A one time frequency simply retains the current amount in the
     * amount field.
     *
     * Additionally the prorated checkbox is enabled and made visible or disabled and made invisible
     * depending on the frequeny chosen. If weekly or monthly then it is enabled and made visible.
     * Any other frequency and it is disable, made invisible, and any text cleared
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
            this.proratedCheckbox.setChecked(false);
            proratedUnchecked();
            this.proratedCheckbox.setEnabled(false);
            this.frequency = Frequency.oneTime;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.daily).toString()))
        {
            this.proratedCheckbox.setChecked(false);
            proratedUnchecked();
            this.proratedCheckbox.setEnabled(false);
            this.frequency= Frequency.daily;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.weekly).toString()))
        {
            this.proratedCheckbox.setEnabled(true);
            if (proratedCheckbox.isChecked())
            {
                updateProratedAmountText();
            }
            this.frequency = Frequency.weekly;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.bi_weekly).toString()))
        {
            this.proratedCheckbox.setChecked(false);
            proratedUnchecked();
            this.proratedCheckbox.setEnabled(false);
            this.frequency = Frequency.biWeekly;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.monthly).toString()))
        {
            this.proratedCheckbox.setEnabled(true);
            if (proratedCheckbox.isChecked())
            {
                updateProratedAmountText();
            }
            this.frequency = Frequency.monthly;
        }
        else if (frequencyText.equals(this.getResources().getText(R.string.yearly).toString()))
        {
            //TODO - add as posible proration frequency
            this.proratedCheckbox.setChecked(false);
            proratedUnchecked();
            this.proratedCheckbox.setEnabled(false);
            this.frequency = Frequency.yearly;
        }

        boolean oneTimeFrequency = (oldFrequency == Frequency.oneTime) || (this.frequency == Frequency.oneTime);
        boolean frequencyChanged = oldFrequency != this.frequency;

        if (!oneTimeFrequency && frequencyChanged)
        {
            String amountString = this.addItemAmount.getText().toString();
            if (!amountString.equals(""))
            {
                double currAmount = Double.parseDouble(amountString);
                double convertedAmount = Prediction.toggle(currAmount, oldFrequency, this.frequency);
                this.addItemAmount.setText(Double.toString(convertedAmount));
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    /**
     * Method called when the budget amount text changes. This checks to see if the prorated checkbox
     * is checked as if it is then we must update the prorated amount in the checkbox text. This
     * does so.
     * @param s
     */
    public void afterTextChanged(Editable s)
    {
        if (proratedCheckbox.isChecked()) {
            updateProratedAmountText();
        }
    }
}
