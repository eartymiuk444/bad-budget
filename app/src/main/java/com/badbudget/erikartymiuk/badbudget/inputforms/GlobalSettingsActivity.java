package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetChildActivity;

/**
 * Global Settings Activity. Currently the Delete Budgets Activity, as it contains only a button
 * that pulls up a dialog where they user can a select a budget to delete. Might be expanded in the
 * future to include settings that pertain to more than just a single budget.
 * Created by Erik Artymiuk on 1/17/2017.
 */
public class GlobalSettingsActivity extends BadBudgetChildActivity {

    /**
     * On create for the global settings activity. Sets the content to content_global_settings
     * @param savedInstanceState - unused
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(R.layout.content_global_settings);
    }

    /**
     * Method called on completion of the update task. No action is needed as only a static
     * button is shown here or a static budget list.
     * @param updated - true if an update to bb objects occurred false otherwise.
     */
    public void updateTaskCompleted(boolean updated) {

    }

    /**
     * Method called on click of the 'delete a budget' button. Pulls up the delete budget dialog
     * which displays all budgets in a single choice list (minus the currently selected budget).
     * @param view
     */
    public void deleteClick(View view)
    {
        DialogFragment deleteBudgetDialog = new DeleteBudgetDialog();
        deleteBudgetDialog.show(getSupportFragmentManager(), "delete_dialog_tag");
    }

}
