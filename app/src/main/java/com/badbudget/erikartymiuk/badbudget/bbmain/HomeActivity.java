package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.badbudget.erikartymiuk.badbudget.viewobjecttables.DebtsActivity;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.viewobjecttables.BudgetSetActivity;
import com.badbudget.erikartymiuk.badbudget.viewobjecttables.CashActivity;
import com.badbudget.erikartymiuk.badbudget.viewobjecttables.GainsActivity;
import com.badbudget.erikartymiuk.badbudget.viewobjecttables.LossesActivity;
import com.badbudget.erikartymiuk.badbudget.viewobjecttables.SavingsActivity;

/**
 * Home activity class for the home page of the bad budget application.
 * First page loaded for the bad budget application. This activity first
 * loads all the user's budget data from the sql database into memory,
 * showing the user a progress spinner until the load is complete. Contains all
 * the buttons to navigate to the users various financial objects/accounts.
 */
public class HomeActivity extends BadBudgetBaseActivity {

    /* Message displayed as the budget is loaded into memory */
    public static final String progressDialogMessage = "Loading budget data...";

    /**
     * Method called when the the update background task completes. No views need to be
     * updated so this method simply returns
     * @param updated - true if an update to bb objects occurred false otherwise.
     */
    public void updateTaskCompleted(boolean updated)
    {

    }

    /**
     * The on create method for the home activity will create and show the user a progress
     * spinner as it loads all the user's budget data into memory.
     * @param savedInstanceState - unused
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContent(R.layout.content_home);
        enableBudgetSelect();
        enableTrackerToolbar();

        HomeActivitySetupTask setupTask = new HomeActivitySetupTask(this);
        setupTask.execute();
    }

    /**
     * Interface callback method invoked after a budget selection has completed. Sets the toolbar title
     * and nav bar title to be the name of the newly selected budget.
     */
    public void budgetSelected()
    {
        BadBudgetApplication application = ((BadBudgetApplication)this.getApplication());
        String title = application.getBudgetMapIDToName().get(application.getSelectedBudgetId());
        this.setToolbarTitle(title);
        this.setNavBarTitle(title);
    }

    /**
     * Method called on cash button click from the home page. Creates an intent and starts
     * the Cash Activity using that intent. The cash activity is where the user can view
     * all their cash accounts (including savings) and also click a button to add a new account.
     *
     * @param view - the view where the click originated
     */
    public void cashButtonClick(View view)
    {
        Intent intent = new Intent(this, CashActivity.class);
        startActivity(intent);
    }

    /**
     * Method called with the savings button is clicked. Starts the savings activity.
     * @param view - the button clicked
     */
    public void savingsButtonClick(View view)
    {
        Intent intent = new Intent(this, SavingsActivity.class);
        startActivity(intent);
    }

    /**
     * Method called when debts button is clicked. Starts the Debts Activity,
     * which displays Credit Cards, Loans, and Other options for debts.
     * @param view - the view the click originated from
     */
    public void debtsButtonClick(View view)
    {
        Intent intent = new Intent(this, DebtsActivity.class);
        startActivity(intent);
    }

    /**
     * Method called when the gains button is clicked. Navigates user to
     * page with a table listing all the gains currently associated with their account.
     * @param view - the source of the click
     */
    public void gainsButtonClick(View view)
    {
        Intent intent = new Intent(this, GainsActivity.class);
        startActivity(intent);
    }

    /**
     * Method called when the losses button is pressed on the home screen. Starts losses activity.
     * @param view - source of the click
     */
    public void lossesButtonClick(View view)
    {
        Intent intent = new Intent(this, LossesActivity.class);
        startActivity(intent);
    }

    /**
     * Method called when the user presses the budget (budget set) button. Takes user to the
     * page where they can view their current budget information and add new budget items/edit
     * existing items.
     * @param view - the source of the click.
     */
    public void budgetButtonClick(View view)
    {
        Intent intent = new Intent(this, BudgetSetActivity.class);
        startActivity(intent);
    }

    /**
     * Method called when the user clicks on the track budget button. Shows the user their budget
     * information in a tracking format where they can indicate that they've spent part of their
     * allocated budget for a time period.
     * @param view - the source of the click.
     */
    public void trackButtonClick(View view)
    {
        Intent intent = new Intent(this, TrackActivity.class);
        startActivity(intent);
    }

    /**
     * Method called when the predict button on the home page is clicked. Will start the predict view
     * which will display a calendar so the user can pick a date to center around.
     * @param view - view the click originated from, should be the button
     */
    public void predictButtonClick(View view)
    {
        Intent intent = new Intent(this, PredictActivity.class);
        startActivity(intent);
    }

    /**
     * Method called when the summary button on the home page is clicked. Will start the summary view
     * which will display summary stats of all the bad budget data.
     * @param view - view the click originated from, should be the button
     */
    public void summaryButtonClick(View view)
    {
        Intent intent = new Intent(this, SummaryActivity.class);
        startActivity(intent);
    }
}
