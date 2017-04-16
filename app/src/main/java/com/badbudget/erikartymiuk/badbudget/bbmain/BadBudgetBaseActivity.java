package com.badbudget.erikartymiuk.badbudget.bbmain;

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
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.inputforms.BudgetPrefsActivity;
import com.badbudget.erikartymiuk.badbudget.inputforms.GlobalSettingsActivity;

/**
 * The abstract base activity class for our bad budget activities. Provides setup of the navigation
 * drawer and the support action bar. Gives options for enabling budget select, enabling tracker in
 * the toolbar, setting the navbar title, setting the toolbar title, and enabling up navigation,
 *
 * Created by Erik Artymiuk on 11/29/2016.
 */
abstract public class BadBudgetBaseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, SelectBudgetCaller, UpdateTaskCaller
{
    private ActionBarDrawerToggle mToggle;

    private boolean trackerToolbarEnabled;
    private boolean budgetSelectEnabled;

    /**
     * On resume for every bad budget base activity. Displays a progress dialog and runs
     * an updateTask to get the bad budget data up to the current date. Extending classes
     * should implement updateTaskCompleted as the callback for when the update task has finished,
     * and should update any necessary views that may have changed.
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
     * Overridden method for when an item is clicked in our toolbar. The two potential actions
     * are budget select and the tracker. The budget select action displays to the user the budget
     * select dialog and the tracker action starts the budget tracker activity
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
     * Called on initial toolbar setup and when the toolbar is invalidated. Checks if the tracker
     * and/or the budget select icons should be made visible in the toolbar.
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
        if (budgetSelectEnabled)
        {
            MenuItem budgetSelectActionItem = menu.findItem(R.id.action_budget_select);
            budgetSelectActionItem.setVisible(true);
        }
        return true;
    }
}
