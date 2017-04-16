package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.content.ContentValues;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseContract;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetChildActivity;
import com.erikartymiuk.badbudgetlogic.budget.Budget;
import com.erikartymiuk.badbudgetlogic.budget.RemainAmountAction;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetInvalidValueException;
import com.erikartymiuk.badbudgetlogic.main.Source;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Budget Preferences Activity. A page where the user can edit their current budget preferences.
 * These should always be set to some values. The budget source is the only value that will ever
 * be empty when the user first starts using the app.
 */
public class BudgetPrefsActivity extends BadBudgetChildActivity implements AdapterView.OnItemSelectedListener, BBOperationTaskCaller {

    private static final String PROGRESS_DIALOG_UPDATE_MESSAGE = "Updating Budget Preferences";
    private static final String BUDGET_SOURCE_NO_OPTIONS = "-- no source available --";
    private static final String BUDGET_SOURCE_NONE_SELECTED = "-- select --";

    private boolean budgetSourceSet;
    private boolean budgetSourceNoneSelectable;  //Variable indicates if it is possible for the user
                                            //to select a source from the list that indicates that
                                            //no source is set. Is only true when the user has yet
                                            //to set a source.

    CheckBox autoUpdate;
    CheckBox autoReset;

    Spinner autoResetActions;

    Spinner budgetSource;

    Spinner weekly;
    Spinner monthly;

    public void updateTaskCompleted(boolean updated)
    {
        populateFields();
    }

    /**
     * On create for the budget prefs activty. Populates the fields with the current values, and sets
     * up the spinner arrays.
     * @param savedInstanceState - unused
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(R.layout.content_budget_prefs);
    }

    private void populateFields()
    {
        //Get a hold of all layout items
        autoUpdate = (CheckBox) findViewById(R.id.autoUpdateInput);
        autoReset = (CheckBox) findViewById(R.id.autoResetInput);

        autoResetActions = (Spinner) findViewById(R.id.autoResetActions);

        budgetSource = (Spinner) findViewById(R.id.budgetSourceInput);

        weekly = (Spinner) findViewById(R.id.resetWeeklyInput);
        monthly = (Spinner) findViewById(R.id.resetMonthlyInput);

        BadBudgetApplication application = (BadBudgetApplication)this.getApplication();
        BadBudgetData bbd = application.getBadBudgetUserData();
        Budget currBudget = bbd.getBudget();

        autoUpdate.setChecked(((BadBudgetApplication)getApplication()).getAutoUpdateSelectedBudget());
        autoReset.setChecked(currBudget.isAutoReset());

        /* Setup our spinners */

        /* Auto Reset Action */
        ArrayList<String> autoResetActionsStrings = new ArrayList<String>();
        autoResetActionsStrings.add(getString(R.string.budget_prefs_accumulates));
        autoResetActionsStrings.add(getString(R.string.budget_prefs_disappear));

        //Add back is only a valid action if auto update is true
        if (autoUpdate.isChecked())
        {
            autoResetActionsStrings.add(getString(R.string.budget_prefs_add_back));
        }

        ArrayAdapter<String> autoResetAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, autoResetActionsStrings);
        autoResetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        autoResetActions.setAdapter(autoResetAdapter);

        switch (application.getRemainAmountActionSelectedBudget())
        {
            case accumulates:
            {
                autoResetActions.setSelection(0);
                break;
            }
            case disappear:
            {
                autoResetActions.setSelection(1);
                break;
            }
            case addBack:
            {
                autoResetActions.setSelection(2);
                break;
            }
            default:
            {
                autoResetActions.setSelection(-1);
            }
        }

        autoResetActions.setEnabled(autoReset.isChecked());

        /* Source */

        /*
         * Setting up our list of choices for source selection. The only time the user can have
         * no source is when they first start using the app and until they add a source and
         * set it, they are unable to add budget items. Once set the source can not be set to nothing
         * again. We prevent them from deleting a source acting as the budgets source.
         */
        ArrayList<String> sourceNames = new ArrayList<String>();
        ArrayList<Source> validBudgetSources = bbd.getSourcesExcludeSavingAccounts();
        /* Their are no valid sources in the user's budget data. We display a message indicating this
        * at position 0 */
        if (validBudgetSources.size() == 0)
        {
            budgetSourceNoneSelectable = true;
            sourceNames.add(BUDGET_SOURCE_NO_OPTIONS);
        }
        else
        {
            /* There are valid sources but the user has never selected one as the budget source. They
            * cannot submit unless they choose a source although they can cancel and keep the source
            * from being set. */
            if (currBudget.getBudgetSource() == null)
            {
                /* We display an invalid choice so we don't confuse the user by picking the source
                for them. If this position is set on submit we don't attempt to add the prefs.
                 */
                budgetSourceNoneSelectable = true;
                sourceNames.add(BUDGET_SOURCE_NONE_SELECTED);
            }
            else
            {
                budgetSourceNoneSelectable = false;
            }

            for (Source source : validBudgetSources)
            {
                sourceNames.add(source.name());
            }
        }

        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sourceNames);
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        budgetSource.setAdapter(sourceAdapter);
        budgetSource.setOnItemSelectedListener(this);

        /* If there is a source then select that item as our default. Their is no invalid source set
        * at position 0 and the user can only update what the source is.
        *
        * If there is not a source set then we display to the user the item at position 0 as it will
        * indicate if they should select a source or if there is no options for a source
        * */
        if (currBudget.getBudgetSource() != null)
        {
            budgetSourceSet = true;
            budgetSource.setSelection(sourceAdapter.getPosition(currBudget.getBudgetSource().name()));
        }
        else
        {
            budgetSourceSet = false;
            budgetSource.setSelection(0);
        }

        /* Weekly */
        ArrayList<DayOfWeek> weekDays = new ArrayList<DayOfWeek>();
        for (int day = 1; day <= 7; day++)
        {
            weekDays.add(new DayOfWeek(day));
        }
        ArrayAdapter<DayOfWeek> weeklyAdapter = new ArrayAdapter<DayOfWeek>(this, android.R.layout.simple_spinner_item, weekDays);
        weeklyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weekly.setAdapter(weeklyAdapter);
        weekly.setSelection(currBudget.getWeeklyReset()-1);

        /* Monthly */
        ArrayList<Integer> days = new ArrayList<Integer>();
        for (int day = 1; day <= 31; day++)
        {
            days.add(day);
        }
        ArrayAdapter<Integer> monthlyAdapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, days);
        monthlyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthly.setAdapter(monthlyAdapter);
        monthly.setSelection(currBudget.getMonthlyReset()-1);
    }

    /**
     * Method called when the cancel button is clicked.  Identical to pressing the back button.
     * @param view - the button view pressed
     */
    public void cancelClick(View view)
    {
        this.onBackPressed();
    }

    /**
     * Method called on submit button press. Propagates any changes made to the db and to memory for
     * the budget preferences. Does nothing if the budget source is not selected.
     * @param view
     */
    public void submitClick(View view)
    {
        //First check if the budget source has been set to a valid source. Do nothing if it isn't
        if (budgetSourceSet)
        {
            //Prepare all the necessary objects then invoke the task to update the budget preferences
            //in memory and in the database.
            BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();
            Source source = bbd.getSourceWithNameExcludeSavingAccounts((String)budgetSource.getSelectedItem());
            boolean autoUpdate = this.autoUpdate.isChecked();
            boolean autoReset = this.autoReset.isChecked();

            String selectedAutoResetActionString = (String)autoResetActions.getSelectedItem();
            RemainAmountAction remainAmountAction;
            if (selectedAutoResetActionString.equals(getString(R.string.budget_prefs_accumulates)))
            {
                remainAmountAction = RemainAmountAction.accumulates;
            }
            else if (selectedAutoResetActionString.equals((String)getString(R.string.budget_prefs_disappear)))
            {
                remainAmountAction = RemainAmountAction.disappear;
            }
            else if (selectedAutoResetActionString.equals((String)getString(R.string.budget_prefs_add_back)))
            {
                remainAmountAction = RemainAmountAction.addBack;
            }
            else
            {
                remainAmountAction = null;
            }

            int weeklyReset = ((DayOfWeek)weekly.getSelectedItem()).getDayOfWeek();
            int monthlyReset = ((Integer)monthly.getSelectedItem());

            Budget budget = null;
            try
            {
                budget = new Budget(source, autoReset, weeklyReset, monthlyReset);
                ContentValues values = new ContentValues();
                values.put(BBDatabaseContract.BudgetPreferences.COLUMN_BUDGET_SOURCE, source.name());
                values.put(BBDatabaseContract.BudgetPreferences.COLUMN_AUTO_RESET, autoReset);
                values.put(BBDatabaseContract.BudgetPreferences.COLUMN_WEEKLY_RESET, weeklyReset);
                values.put(BBDatabaseContract.BudgetPreferences.COLUMN_MONTHLY_RESET, monthlyReset);

                //Auto update and remain amount action are not part of the budget object but are budget prefs
                values.put(BBDatabaseContract.BudgetPreferences.COLUMN_AUTO_UPDATE, autoUpdate);
                values.put(BBDatabaseContract.BudgetPreferences.COLUMN_REMAIN_AMOUNT_ACTION, BBDatabaseContract.dbRemainAmountActionToInteger(remainAmountAction));

                EditBBObjectTask task = new EditBBObjectTask(this, PROGRESS_DIALOG_UPDATE_MESSAGE, budget, autoUpdate, remainAmountAction, values, this);
                task.execute();
            }
            catch(BadBudgetInvalidValueException e)
            {
                //TODO - handle
                e.printStackTrace();
            }
        }
    }

    /**
     * Method called when the budget source adapter detects that the selected item has changed.
     * Checks if the user selected the name of a valid source or the position
     * for indicating that no source has been selected and updates the budgetSourceSet instance
     * variable accordingly.
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        // Check if user selected the position indicating no source is set
        if (budgetSourceNoneSelectable)
        {
            if (position == 0)
            {
                budgetSourceSet = false;
            }
            else
            {
                budgetSourceSet = true;
            }
        }
        else
        {
            budgetSourceSet = true;
        }
    }

    /**
     * TODO
     * @param parent
     */
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**
     * There only ever exists one budget prefs object, thus only the edit task is implemented/needed
     * for callbacks
     */
    public void addBBObjectFinished() {

    }

    /**
     * Method called after completion of the edit task. Ends
     * this activity.
     */
    public void editBBObjectFinished()
    {
        this.finish();
    }

    /**
     * There only ever exists one budget prefs object, thus only the edit task is implemented/needed
     * for callbacks
     */
    public void deleteBBObjectFinished() {

    }

    /**
     * Method called whenever the auto reset checkbox is clicked on and toggled. If it was
     * checked enables the remaining amount action spinner. If unchecked disables that spinner.
     * @param view - the view that was clicked
     */
    public void autoResetClicked(View view)
    {
        if (autoReset.isChecked())
        {
            autoResetActions.setEnabled(true);
        }
        else
        {
            autoResetActions.setEnabled(false);
        }
    }

    /**
     * Method called when auto update is clicked. If auto update is checked adds the add back
     * to the list of auto reset actions. If unchecked removed add back from the list of auto
     * reset actions.
     * @param view - the view that was clicked
     */
    public void autoUpdateClicked(View view)
    {
        if (autoUpdate.isChecked())
        {
            ((ArrayAdapter<String>)autoResetActions.getAdapter()).add(getString(R.string.budget_prefs_add_back));
        }
        else
        {
            ((ArrayAdapter<String>)autoResetActions.getAdapter()).remove(getString(R.string.budget_prefs_add_back));
        }
    }
}
