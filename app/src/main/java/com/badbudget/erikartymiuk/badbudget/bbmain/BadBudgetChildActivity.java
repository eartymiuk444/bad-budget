package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.os.Bundle;

/**
 * Abstract extension of the bad budget base activity that is to be subclassed by any bad budget
 * activity that should have up navigation, the nav bar title set initially to the selected budget,
 * and the tracker enabled in its toolbar.
 * Created by Erik Artymiuk on 12/6/2016.
 */
abstract public class BadBudgetChildActivity extends BadBudgetBaseActivity {

    /**
     * On create for the BBChildActivity. Sets the nav bar title to the currently selected app
     * budget, enable the tracker in the toolbar, and enables up navigation.
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

        String title = application.getBudgetMapIDToName().get(application.getSelectedBudgetId());
        this.setNavBarTitle(title);
        this.enableTrackerToolbar();
        this.setToolbarDrawerUpEnabled();
    }

}
