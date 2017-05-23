package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.inputforms.BudgetPrefsActivity;
import com.badbudget.erikartymiuk.badbudget.inputforms.DateInputActivity;
import com.badbudget.erikartymiuk.badbudget.inputforms.DatePickerFragment;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * The base activity for all detailed look activity classes. Any activity invoked
 * on top of the predict activity should extend this class.
 *
 * Created by Erik Artymiuk on 11/19/2016.
 */
public abstract class DetailedLookBaseActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener, DateInputActivity, PredictTaskCaller {
    public static final int MAX_DAYS_HISTORY = 365;

    protected Date currentEndDate;
    protected Date currentChosenDay;

    private ActionBarDrawerToggle mToggle;
    private MenuItem mDatePickerActionItem;

    /* Keys for saving the instance state */
    private static final String CURRENT_END_DATE_KEY = "CURRENT_END_DATE";
    private static final String CURRENT_CHOSEN_DATE_KEY = "CURRENT_CHOSEN_DATE";

    public static final String DL_DATE_PICKER_TAG = "dl_cash_date_picker_fragment";

    /**
     * On create for detailed look base activity. Sets the base content view and
     * extracts expected arguments from invoking activity including currentEndDate
     * and currentChosenDay, sets up the toolbar and the navigation drawer. First checks
     * if the bbd was updated via getPredictBBDUpdated. If it was the detailed look activity
     * is finished with a result of canceled.
     *
     * @param savedInstanceState - unused
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BadBudgetApplication application = ((BadBudgetApplication) this.getApplication());
        if (application.getPredictBBDUpdated())
        {
            Intent returnIntent = new Intent();
            returnIntent.putExtra(BadBudgetApplication.DETAILED_LOOK_BBD_UPDATED, true);
            setResult(Activity.RESULT_CANCELED, returnIntent);
            finish();
        }
        else
        {
            setContentView(R.layout.activity_detailed_look_base);
            //TODO - Quickfix for publishing 3/17/2017 - look at more closely?
            if (savedInstanceState != null)
            {
                currentChosenDay = (Date)savedInstanceState.getSerializable(CURRENT_CHOSEN_DATE_KEY);
                currentEndDate = (Date)savedInstanceState.getSerializable(CURRENT_END_DATE_KEY);
            }
            else
            {
                Bundle args = this.getIntent().getExtras();
                currentChosenDay = ((Calendar) args.get(BadBudgetApplication.PREDICT_CURRENT_CHOSEN_DAY)).getTime();
                currentEndDate = ((Calendar) args.get(BadBudgetApplication.PREDICT_CURRENT_END_DATE)).getTime();
            }

            //Toolbar setup
            Toolbar myToolbar = (Toolbar) findViewById(R.id.bad_budget_toolbar);
            setSupportActionBar(myToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            //Navigation Drawer setup
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_detailed_look_base_layout);
            mToggle = new ActionBarDrawerToggle(this, drawer, myToolbar, R.string.nav_drawer_opening, R.string.nav_drawer_closing);
            mToggle.setDrawerIndicatorEnabled(false);
            drawer.addDrawerListener(mToggle);

            mToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });

            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_detailed_look_base_view);
            navigationView.setNavigationItemSelectedListener(this);

            String title = application.getBudgetMapIDToName().get(application.getSelectedBudgetId());
            this.setNavBarTitle(title);
        }
    }

    /**
     * Sets the title in the nav drawer's header
     * @param title - the string to set the title as
     */
    protected void setNavBarTitle(String title)
    {
        NavigationView navView = (NavigationView) this.findViewById(R.id.nav_detailed_look_base_view);
        View headView = navView.getHeaderView(0);

        TextView selectedBudgetView = (TextView)headView.findViewById(R.id.nav_drawer_selected_budget);
        selectedBudgetView.setText(title);
    }

    /**
     * checks if the bbd was updated via getPredictBBDUpdated. If it was the detailed look activity
     * is finished with a result of canceled.
     */
    protected void onResume()
    {
        super.onResume();
        BadBudgetApplication application = ((BadBudgetApplication) this.getApplication());
        if (application.getPredictBBDUpdated())
        {
            Intent returnIntent = new Intent();
            returnIntent.putExtra(BadBudgetApplication.DETAILED_LOOK_BBD_UPDATED, true);
            setResult(Activity.RESULT_CANCELED, returnIntent);
            finish();
        }
    }

    /**
     * Method called on return of a child detailed look activity. The dates may have changed and
     * this method gets a hold of any date changes that occurrred from the child activity. Also
     * checks for a canceled result that occurs when a change to the bbd is detected (i.e. when
     * user uses navigation drawer to go to tracker and bbd is updated). If this is detected
     * this activity is finished with the same result of canceled.
     * @param requestCode - Request code identifying the result being returned
     * @param resultCode - the status code of the returned activity
     * @param data - the data returned as part of the request when the child activity is finished
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == BadBudgetApplication.PREDICT_REQUEST_CODE_UPDATED_DATES)
        {
            if (resultCode == RESULT_OK)
            {
                Bundle args = data.getExtras();
                currentChosenDay = ((Calendar)args.get(BadBudgetApplication.PREDICT_CURRENT_CHOSEN_DAY)).getTime();
                currentEndDate = ((Calendar)args.get(BadBudgetApplication.PREDICT_CURRENT_END_DATE)).getTime();
                System.out.println(mDatePickerActionItem);
                //TODO 3/17/2017 - Quickfix for publishing.
                if (mDatePickerActionItem != null)
                {
                    mDatePickerActionItem.setTitle(BadBudgetApplication.dateString(currentChosenDay));
                }
            }
            else if (resultCode == RESULT_CANCELED)
            {
                Bundle args = data.getExtras();
                if (args.getBoolean(BadBudgetApplication.DETAILED_LOOK_BBD_UPDATED))
                {
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra(BadBudgetApplication.DETAILED_LOOK_BBD_UPDATED, true);
                    setResult(Activity.RESULT_CANCELED, returnIntent);
                    finish();
                }
            }
        }
    }

    /**
     * Called prior to this activity being destroyed. Saves any necessary state in order to restore
     * this activity when/if the user returns.
     * @param outState - Bundle to place saved state in
     */
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putSerializable(CURRENT_END_DATE_KEY, currentEndDate);
        outState.putSerializable(CURRENT_CHOSEN_DATE_KEY, currentChosenDay);
    }

    /**
     * Private helper method. Called when the date picker is clicked on in our toolbar
     * Passed a tag identifying
     * the fragment that will be created for date picking. Uses the application today for today's date
     * and the currentChosenDay as the current chosen date.
     * @param tag - an identifying tag for the fragment that will be created
     */
    private void dateClicked(String tag)
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
     * Interface implementation for the DateInputActivity interface. Callback method when a user has
     * chosen a date from a datePickerFragment. The return code is unused here. Makes a call to set
     * prediction which runs the prediction task if necessary to have data up to the chosen day.
     * @param year -  the year chosen
     * @param month - the month chosen
     * @param day - the day chosen
     * @param returnCode - unused
     */
    public void dateSet(int year, int month, int day, int returnCode)
    {
        Calendar tempCalendar = new GregorianCalendar(year, month, day);
        Date date = tempCalendar.getTime();
        currentChosenDay = date;
        mDatePickerActionItem.setTitle(BadBudgetApplication.dateString(date));
        PredictActivity.setPrediction(currentChosenDay, currentEndDate, this, this);
    }

    /**
     * Abstract method to be implemented by subclasses. Should update all necessary views that need
     * to be updated on a date change. By default called after the prediction has been set.
     */
    protected abstract void updateHistoryViews();

    /**
     * Method called once the prediction data has been populated for our chosen date.
     * Updates the current end date with the new end date we have predict data up to.
     * @param endDate - the new end date
     */
    public void predictionSet(Date endDate)
    {
        currentEndDate = endDate;
        updateHistoryViews();
    }

    /**
     * Method called when the back button is pressed. Sets up an intent returning the current end
     * date we have prediction data for and the current chosen date and sets these as the result.
     * Then calls finish on this activity.
     */
    public void onBackPressed()
    {
        Intent returnIntent = new Intent();

        Calendar currentEndCal = Calendar.getInstance();
        Calendar currentSelectedCal = Calendar.getInstance();
        currentEndCal.setTime(currentEndDate);
        currentSelectedCal.setTime(currentChosenDay);

        returnIntent.putExtra(BadBudgetApplication.PREDICT_CURRENT_END_DATE, currentEndCal);
        returnIntent.putExtra(BadBudgetApplication.PREDICT_CURRENT_CHOSEN_DAY, currentSelectedCal);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    /**
     * Overridden method for when an item is clicked in our toolbar. Detailed look has only one
     * action which is the date picker action. This displays a date picker where the user can
     * choose a date to view the predict data for.
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
            case R.id.action_date_picker:
            {
                dateClicked(DL_DATE_PICKER_TAG);
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
     * Method called when one of our icons in the navigation drawer is selected. Potential icons include
     * the home, history, predict, tracker, budget settings, and general settings icons. Each of these
     * starts the corresponding activity.
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
            /*case R.id.nav_history:
            {
                Intent intent = new Intent(this,  BudgetHistoryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                break;
            }*/
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
                //TODO 12/9
                break;
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_detailed_look_base_layout);
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
     * Overridden method to inflate the menu resource file for our toolbar. Sets the date picker
     * action item to show the current chosen date.
     * @param menu - menu passed to inflate
     * @return true
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        mDatePickerActionItem = menu.findItem(R.id.action_date_picker);
        mDatePickerActionItem.setVisible(true);
        mDatePickerActionItem.setTitle(BadBudgetApplication.dateString(currentChosenDay));

        return true;
    }
}
