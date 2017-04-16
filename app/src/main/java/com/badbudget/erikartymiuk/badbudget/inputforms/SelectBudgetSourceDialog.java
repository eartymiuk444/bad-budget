package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseContract;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.viewobjecttables.BudgetSetActivity;
import com.erikartymiuk.badbudgetlogic.budget.Budget;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetInvalidValueException;
import com.erikartymiuk.badbudgetlogic.main.Source;

import java.util.ArrayList;
import java.util.List;

/**
 * Source dialog that is displayed to the user when a budget item is trying to be added but
 * no budget source is set. Displays a list of potential sources of which the user can choose one.
 */
public class SelectBudgetSourceDialog extends DialogFragment {

    private String selectedSourceName = null;

    /**
     * On create for the select budget source dialog. Sets up our dialog with a title, a single choice
     * list populated with the potential sources for the user's budget, a positive
     * submit button, and negative cancel button.
     * @param savedInstanceState - unused
     * @return the newly built dialog
     */
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final List<String> budgetSourceNames = this.getBudgetSourceNames();

        final ListAdapter listAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_single_choice, budgetSourceNames);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.select_budget_source_dialog_title)
                .setSingleChoiceItems(listAdapter, -1, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        selectedSourceName = (String)listAdapter.getItem(which);
                    }
                })
                .setPositiveButton(R.string.select_budget_source_dialog_select, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        selectBudgetSource();
                    }
                })
                .setNegativeButton(R.string.select_budget_source_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });

        return builder.create();
    }

    /**
     * Setting up our list of choices for source selection. The only time the user can have
     * no source is when they first start using the app and until they add a source and
     * set it, they are unable to add budget items. Once set the source can not be set to nothing
     * again. We prevent them from deleting a source acting as the budgets source.
 */
    private List<String> getBudgetSourceNames()
    {
        BadBudgetData bbd = ((BadBudgetApplication) this.getActivity().getApplication()).getBadBudgetUserData();
        ArrayList<String> sourceNames = new ArrayList<String>();
        ArrayList<Source> validBudgetSources = bbd.getSourcesExcludeSavingAccounts();

        boolean budgetSourcesAvailable = (validBudgetSources.size() != 0);

        if (budgetSourcesAvailable)
        {
            /* There are valid sources but the user hasn't selected one as the budget source.
               They can cancel or submit with no selection and keep the source from being set. */
            for (Source source : validBudgetSources)
            {
                sourceNames.add(source.name());
            }
        }

        return sourceNames;
    }

    /**
     * If the user selected a budget source from the list then this method sets up our budget with
     * the selected source.
     */
    private void selectBudgetSource()
    {
        BadBudgetApplication application = (BadBudgetApplication)(this.getActivity()).getApplication();
        BadBudgetData bbd = application.getBadBudgetUserData();
        Budget currBudget = bbd.getBudget();

        Budget budget;

        if (selectedSourceName != null) {
            try {
                budget = new Budget(bbd.getSourceWithName(selectedSourceName), currBudget.isAutoReset(), currBudget.getWeeklyReset(), currBudget.getMonthlyReset());
                ContentValues values = new ContentValues();
                values.put(BBDatabaseContract.BudgetPreferences.COLUMN_BUDGET_SOURCE, selectedSourceName);
                values.put(BBDatabaseContract.BudgetPreferences.COLUMN_AUTO_RESET, currBudget.isAutoReset());
                values.put(BBDatabaseContract.BudgetPreferences.COLUMN_WEEKLY_RESET, currBudget.getWeeklyReset());
                values.put(BBDatabaseContract.BudgetPreferences.COLUMN_MONTHLY_RESET, currBudget.getMonthlyReset());

                values.put(BBDatabaseContract.BudgetPreferences.COLUMN_AUTO_UPDATE, application.getAutoUpdateSelectedBudget());
                values.put(BBDatabaseContract.BudgetPreferences.COLUMN_REMAIN_AMOUNT_ACTION,
                        BBDatabaseContract.dbRemainAmountActionToInteger(application.getRemainAmountActionSelectedBudget()));

                BudgetSetActivity contextActivity = (BudgetSetActivity) this.getActivity();

                EditBBObjectTask task = new EditBBObjectTask(contextActivity, getString(R.string.select_budget_source_progress_message),
                        budget, application.getAutoUpdateSelectedBudget(), application.getRemainAmountActionSelectedBudget(), values, contextActivity);
                task.execute();
            } catch (BadBudgetInvalidValueException e) {
                //TODO - handle
                e.printStackTrace();
            }
        }
    }
}
