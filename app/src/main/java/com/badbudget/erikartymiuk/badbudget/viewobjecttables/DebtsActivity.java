package com.badbudget.erikartymiuk.badbudget.viewobjecttables;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
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
import android.widget.LinearLayout;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetBaseActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetChildActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.PredictActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.TrackActivity;

/**
 * Intermediate organization activity for debts objects. Functions to break up credit cards, loans,
 * and misc debts using three buttons that take the user to a table with only the chosen objects.
 *
 */
public class DebtsActivity extends BadBudgetChildActivity {

    /**
     * Method called on completion of the update task, simply returns.
     * @param updated - true if an update to bb objects occurred false otherwise.
     */
    public void updateTaskCompleted(boolean updated)
    {

    }

    /**
     * Debts Activity onCreate, nothing unusual done here.
     * @param savedInstanceState - unused
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BadBudgetApplication application = ((BadBudgetApplication)this.getApplication());
        if (application.getBadBudgetUserData() == null)
        {
            this.finish();
            return;
        }

        setContent(R.layout.content_debts);
    }

    /**
     * Method called when the credit cards button is clicked. Starts the credit cards activity
     * taking the user to an activity with all their credit cards displayed in a table and also
     * a page where they have they option to add credit cards.
     * @param view - the button that was clicked
     */
    public void creditCardsButtonClick(View view)
    {
        Intent intent = new Intent(this, CreditCardsActivity.class);
        startActivity(intent);
    }

    /**
     * Method called when the loans buttons is clicked. Takes user to loans activity.
     * @param view - the loans button that was clicked.
     */
    public void loansButtonClick(View view)
    {
        Intent intent = new Intent(this, LoansActivity.class);
        startActivity(intent);
    }

    /**
     * Method called when the misc. button is clicked. Directs user to table with any misc. debts
     * objects
     * @param view - button that was clicked.
     */
    public void miscellaneousButtonClick(View view)
    {
        Intent intent = new Intent(this, MiscActivity.class);
        startActivity(intent);
    }
}
