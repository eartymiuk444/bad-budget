package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetBaseActivity;

import java.util.ArrayList;
import java.util.Map;

/**
 * Delete budget dialog fragment. This dialog displays a single choice list populated with all the
 * budgets minus the user's currently selected budget. The user then can select a budget and press
 * a delete button to invoke the delete budget task. The dialog also contains a cancel button which
 * dismisses the dialog with no action taken.
 */
public class DeleteBudgetDialog extends DialogFragment {

    private int listPositionSelected = -1;
    /**
     * Builds our delete budget dialog with a title, a single choice list, and two buttons
     * @param savedInstanceState - unused
     * @return the newly built dialog
     */
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final BadBudgetApplication application = (BadBudgetApplication)getActivity().getApplication();
        final ArrayList<String> budgetNames = new ArrayList<String>();

        Map<Integer, String> budgetIdToNameMap = application.getBudgetMapIDToName();
        String selectedBudgetName = budgetIdToNameMap.get(application.getSelectedBudgetId());

        for (String budgetName : budgetIdToNameMap.values())
        {
            //Exclude the currently selected budget as a choice for deletion
            if (!budgetName.equals(selectedBudgetName))
            {
                budgetNames.add(budgetName);
            }
        }

        ListAdapter listAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_single_choice, budgetNames);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.delete_budget_dialog_title)
                .setSingleChoiceItems(listAdapter, listPositionSelected, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        listPositionSelected = which;
                    }
                })
                .setPositiveButton(R.string.delete_budget_dialog_delete, new DialogInterface.OnClickListener() {
                    /*
                    Positive button click is the 'delete' button which invokes a delete budget task if
                    the user selected an item
                     */
                    public void onClick(DialogInterface dialog, int id)
                    {
                        if (listPositionSelected != -1)
                        {
                            DeleteBudgetTask task = new DeleteBudgetTask((BadBudgetBaseActivity)getActivity(), budgetNames.get(listPositionSelected));
                            task.execute();
                        }
                    }
                })
                .setNegativeButton(R.string.delete_budget_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });

        return builder.create();
    }
}
