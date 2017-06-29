package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ScrollView;

import com.badbudget.erikartymiuk.badbudget.BuildConfig;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.bbmain.FlavorSpecific;

/**
 * Abstract class that should be extending by bad budget child activities that are intended to be
 * tables.
 * Created by Erik Artymiuk on 3/29/2017.
 */
abstract public class BadBudgetTableActivity extends BadBudgetChildActivity
{
    /**
     * Overridden on create method calls super on create and set content with the activity_bad_budget_table
     * as the layout being set.
     * @param savedInstanceState - unused
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        BadBudgetApplication application = ((BadBudgetApplication)this.getApplication());
        if (application.getBadBudgetUserData() == null)
        {
            this.finish();
            return;
        }

        super.setContent(R.layout.activity_bad_budget_table);
    }

    /**
     * Overridden set content activity that sets the content of the scroll view defined in the
     * activity_bad_budget_table layout to the passed layout. Additionally if the free version makes
     * an ad request.
     * @param layoutId - the content layout view for the derived table class
     */
    protected void setContent(int layoutId)
    {
        ScrollView scrollView = (ScrollView)findViewById(R.id.bad_budget_table_scroll_view);
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(layoutId, scrollView, true);

        if (BuildConfig.FREE_VERSION)
        {
            FlavorSpecific.adRequest(this, layoutId);
        }
    }
}
