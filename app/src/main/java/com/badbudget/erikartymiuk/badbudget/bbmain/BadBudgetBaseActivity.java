package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NavUtils;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.inputforms.BudgetPrefsActivity;
import com.badbudget.erikartymiuk.badbudget.inputforms.DateInputActivity;
import com.badbudget.erikartymiuk.badbudget.inputforms.DatePickerFragment;
import com.badbudget.erikartymiuk.badbudget.inputforms.GlobalSettingsActivity;
import com.erikartymiuk.badbudgetlogic.main.Prediction;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * The abstract base activity class for our bad budget activities. Provides setup of the navigation
 * drawer and the support action bar. Gives options for enabling budget select, enabling tracker in
 * the toolbar, enabling a date picker in the toolbar, setting the navbar title, setting the toolbar title,
 * and enabling up navigation.
 *
 * Created by Erik Artymiuk on 11/29/2016.
 */
abstract public class BadBudgetBaseActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SelectBudgetCaller,
                    UpdateTaskCaller, DateInputActivity
{
    private ActionBarDrawerToggle mToggle;

    private boolean trackerToolbarEnabled;
    private boolean budgetSelectEnabled;
    private boolean datePickerEnabled;

    private Date savedChosenDay;
    private Date currentChosenDay;

    private static final String BASE_DATE_PICKER_TAG = "BASE_DATE_PICKER_TAG";
    private static final String BASE_CHOSEN_DATE_KEY = "CHOSEN_DATE";

    /**
     * On resume for every bad budget base activity. Displays a progress dialog and runs
     * an updateTask to get the bad budget data up to the current date. Extending classes
     * should override the updateTaskCompleted as the callback for when the update task has finished,
     * and should call super and then update any necessary views that may have changed.
     */
    protected void onResume()
    {
        super.onResume();

        //Progress dialog to display to the user as the Update task runs in the background
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage(BadBudgetApplication.progressDialogMessage);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCancelable(false);

        UpdateTask task = new UpdateTask(this, this, progress);
        task.execute();
    }

    /**
     * Method called upon completion of the update task run in onResume. Checks if our today date comes
     * after our chosen date and if it does updates the chosen date to be today's date. Extending classes
     * should override this method and call super first, followed by updating any of its views that may change
     * upon an update.
     *
     * @param updated - unused
     */
    public void updateTaskCompleted(boolean updated) {
        Date today = ((BadBudgetApplication)getApplication()).getToday();
        if (Prediction.numDaysBetween(currentChosenDay, today) > 0)
        {
            setCurrentChosenDay(today);
        }
    }

    /**
     * Called prior to this activity being destroyed, saves any necessary state to restore upon
     * the users return.
     * @param outState - where to store the saved state.
     */
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putSerializable(BASE_CHOSEN_DATE_KEY, currentChosenDay);
    }

    /**
     * On create for the base bad budget activity. Sets the content view and sets up the toolbar and navigation drawer.
     * @param savedInstanceState - unused
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bad_budget_base);

        //Toolbar setup
        Toolbar myToolbar = (Toolbar) findViewById(R.id.bad_budget_toolbar);
        setSupportActionBar(myToolbar);

        if (savedInstanceState != null)
        {
            this.savedChosenDay = (Date) savedInstanceState.getSerializable(BASE_CHOSEN_DATE_KEY);
        }
        //Navigation Drawer setup
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_base_layout);
        mToggle = new ActionBarDrawerToggle(this, drawer, myToolbar, R.string.nav_drawer_opening, R.string.nav_drawer_closing);
        drawer.addDrawerListener(mToggle);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_base_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    /**
     * Called by derived classes to enable the tracker action in the toolbar
     */
    protected void enableTrackerToolbar()
    {
        trackerToolbarEnabled = true;
        supportInvalidateOptionsMenu();
    }

    /**
     * Called by derived classes to enable the budget select button and action in the nav header
     * and the toolbar.
     */
    protected void enableBudgetSelect()
    {
        budgetSelectEnabled = true;
        supportInvalidateOptionsMenu();

        NavigationView navView = (NavigationView) findViewById(R.id.nav_base_view);
        View headView = navView.getHeaderView(0);

        Button budgetSelectButton = (Button) headView.findViewById(R.id.nav_drawer_select_budget_icon);
        budgetSelectButton.setVisibility(View.VISIBLE);
    }

    /**
     * Called by derived classes to enable the date picker action in the toolbar. Chosen date is initially set as
     * application's today date or the saved current chosen date if the activity was destroyed and is being
     * recreated.
     */
    protected void enableDatePickerToolbar()
    {
        datePickerEnabled = true;

        if (savedChosenDay == null)
        {
            Date today = ((BadBudgetApplication)getApplication()).getToday();
            setCurrentChosenDay(today);
        }
        else
        {
            setCurrentChosenDay(savedChosenDay);
        }
    }

    /**
     * Called by derived classes to set the title in the nav drawer's header
     * @param title - the string to set the title as
     */
    protected void setNavBarTitle(String title)
    {
        NavigationView navView = (NavigationView) this.findViewById(R.id.nav_base_view);
        View headView = navView.getHeaderView(0);

        TextView selectedBudgetView = (TextView)headView.findViewById(R.id.nav_drawer_selected_budget);
        selectedBudgetView.setText(title);
    }

    /**
     * Called by derived classes to set the title in the toolbar
     * @param title - the string to set the title as
     */
    protected void setToolbarTitle(String title)
    {
        this.getSupportActionBar().setTitle(title);
    }

    /**
     * Called by derived classes to set the title in the toolbar
     * @param stringResId - the string resource to set the title as
     */
    protected void setToolbarTitle(int stringResId)
    {
        this.getSupportActionBar().setTitle(stringResId);
    }

    /**
     * Called by derived classes to set their content view
     * @param layoutId - the content layout view for the derived activity class
     */
    protected void setContent(int layoutId)
    {
        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.bad_budget_base_linear_layout);
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(layoutId, linearLayout, true);
    }

    /**
     * Derived classes call this method to enable up navigation in the toolbar
     * and disable the drawer indicator icon.
     */
    protected void setToolbarDrawerUpEnabled()
    {
        mToggle.setDrawerIndicatorEnabled(false);
        mToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(BadBudgetBaseActivity.this);
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Callback interface method for SelectBudgetCaller interface. Called after a budget selection
     * is complete.
     * Default implementation sets the nav bar title to the currently selected application budget name.
     */
    public void budgetSelected()
    {
        BadBudgetApplication application = ((BadBudgetApplication)this.getApplication());
        String title = application.getBudgetMapIDToName().get(application.getSelectedBudgetId());
        setNavBarTitle(title);
    }

    /**
     * Method called when the budget select button/icon is clicked in the navigation drawer
     * header. Displays the select budget dialog for the user to select a budget.
     * @param view - the budget select button that was clicked on
     */
    public void navDrawerBudgetSelectClick(View view)
    {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_base_layout);
        drawer.closeDrawer(GravityCompat.START);

        DialogFragment selectBudgetDialog = new SelectBudgetDialog();
        selectBudgetDialog.show(getSupportFragmentManager(), "select_dialog_tag");
    }

    /**
     * Overridden method for when an item is clicked in our toolbar. Three potential actions
     * are budget select, the tracker, and the date picker. The budget select action displays to the user the budget
     * select dialog, the tracker action starts the budget tracker activity, and the date picker pulls up
     * a date picker dialog and allows the user to select a chosen date.
     *
     * @param item - the menu item that was pressed
     * @return boolean Return false to allow normal menu processing to
     *         proceed, true to consume it here.
     */
    public boolean onOptionsItemSelected(MenuItem item) {

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // Handle toolbar items
        switch (item.getItemId()) {
            case R.id.action_budget_select: {
                DialogFragment selectBudgetDialog = new SelectBudgetDialog();
                selectBudgetDialog.show(getSupportFragmentManager(), "select_dialog_tag");
                return true;
            }
            case R.id.action_tracker: {
                Intent intent = new Intent(this,  TrackActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            case R.id.action_date_picker:
            {
                this.dateClicked(BASE_DATE_PICKER_TAG);
                return true;
            }
            default:
            {
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
            }
        }
    }

    /**
     * Method call when the user clicks the date in the toolbar indicating they would like to select a date.
     * Displays a date picker dialog with today's date set to the application's today date and the chosen
     * date set as this activities currently chosen date.
     * @param tag - Identifying tag for the date picker dialog
     */
    public void dateClicked(String tag)
    {
        Bundle args = new Bundle();
        args.putInt(DatePickerFragment.RETURN_CODE_KEY, -1);

        Date today = ((BadBudgetApplication)getApplication()).getToday();
        Calendar todayCal = Calendar.getInstance();
        todayCal.setTime(today);

        Calendar currentChosenCal = Calendar.getInstance();
        currentChosenCal.setTime(currentChosenDay);

        args.putInt(DatePickerFragment.TODAY_YEAR_KEY, todayCal.get(Calendar.YEAR));
        args.putInt(DatePickerFragment.TODAY_MONTH_KEY, todayCal.get(Calendar.MONTH));
        args.putInt(DatePickerFragment.TODAY_DAY_KEY, todayCal.get(Calendar.DAY_OF_MONTH));

        args.putInt(DatePickerFragment.CURRENT_CHOSEN_YEAR_KEY, currentChosenCal.get(Calendar.YEAR));
        args.putInt(DatePickerFragment.CURRENT_CHOSEN_MONTH_KEY, currentChosenCal.get(Calendar.MONTH));
        args.putInt(DatePickerFragment.CURRENT_CHOSEN_DAY_KEY, currentChosenCal.get(Calendar.DAY_OF_MONTH));

        DatePickerFragment datePickerFragment = new DatePickerFragment();
        datePickerFragment.setArguments(args);
        datePickerFragment.show(getSupportFragmentManager(), tag);
    }

    /**
     * Method called when the user selects a date in the date picker dialog, displayed after clicking the
     * date picker action in the toolbar.
     * @param year - the year set
     * @param month - the month set
     * @param day - the day set
     * @param returnCode - unused (a code if passed into the DatePickerFragment is passed back when the date is set)
     */
    public void dateSet(int year, int month, int day, int returnCode)
    {
        Calendar tempCalendar = new GregorianCalendar(year, month, day);
        Date date = tempCalendar.getTime();
        setCurrentChosenDay(date);
    }

    /**
     * This method is called to set the current chosen date, both programmatically and after
     * the user selects the date in the dialog. Invalidates the options menu so that the title
     * in the toolbar for the date is updated. Derived classes can override this method to
     * take action when the current chosen date is set but should call super first.
     * @param date - the date to set our current chosen day to.
     */
    public void setCurrentChosenDay(Date date)
    {
        currentChosenDay = date;
        supportInvalidateOptionsMenu();
    }

    /**
     * Gets the current chosen day.
     * @return - the currently chosen day
     * @return - the currently chosen day
     */
    public Date getCurrentChosenDay()
    {
        return this.currentChosenDay;
    }

    /**
     * Method called when one of our icons in the navigation drawer is selected. Potential icons include
     * the home, history, predict, tracker, budget settings, and general settings icons. Each of these
     * starts the corresponding activity that will be either the top of our stack or the second item on our
     * stack.
     * @param item - the menu item in or navigation drawer that was clicked on
     * @return - true to consume the selection
     */
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId())
        {
            case R.id.nav_home:
            {
                Intent intent = new Intent(this,  HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                break;
            }
            //TODO - implement this page 3/17/2017
            case R.id.nav_history:
            {
                Intent intent = new Intent(this,  BudgetHistoryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                break;
            }
            case R.id.nav_predict:
            {
                Intent intent = new Intent(this,  PredictActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                break;
            }
            case R.id.nav_tracker:
            {
                Intent intent = new Intent(this,  TrackActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                break;
            }
            case R.id.nav_budget_settings:
            {
                Intent intent = new Intent(this,  BudgetPrefsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                break;
            }
            case R.id.nav_delete_budget:
            {
                Intent intent = new Intent(this,  GlobalSettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                break;
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_base_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Need to override this method to notify our ActionBarDrawerToggle
     * @param savedInstanceState - passed to super onPostCreate
     */
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mToggle.syncState();
    }

    /**
     * Need to override this method to notify our ActionBarDrawerToggle
     * @param newConfig - passed to super onConfiguarationChanged
     */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Overridden method to inflate the menu resource file for our toolbar
     * @param menu - menu passed to inflate
     * @return true
     */
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * Called on initial toolbar setup and when the toolbar is invalidated. Checks if the tracker,
     * the budget select, and/or date picker icons should be made visible in the toolbar. If the date
     * picker should be visible, its title is set to the currently selected date.
     * @param menu - the toolbar menu
     * @return true so that the menu is always displayed.
     */
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        if (trackerToolbarEnabled)
        {
            MenuItem trackerActionItem = menu.findItem(R.id.action_tracker);
            trackerActionItem.setVisible(true);
        }
        if (datePickerEnabled)
        {
            MenuItem datePickerActionItem = menu.findItem(R.id.action_date_picker);
            datePickerActionItem.setVisible(true);
            datePickerActionItem.setTitle(BadBudgetApplication.dateString(currentChosenDay));
        }
        if (budgetSelectEnabled)
        {
            MenuItem budgetSelectActionItem = menu.findItem(R.id.action_budget_select);
            budgetSelectActionItem.setVisible(true);
        }
        return true;
    }
}
