package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.bbmain.FlavorSpecific;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A dialog that displays all of the user's budgets in a single choice list. The default selection is
 * the currently selected/loaded budget. Displays three buttons: a "select" button to select the currently
 * selected budget, a "cancel" button to exit the dialog with no change, and a "create new" button to
 * move to the create budget fragment dialog where the user can create a new budget.
 * Created by Erik Artymiuk on 11/22/2016.
 */
public class SelectBudgetDialog extends DialogFragment {

    private int initialListPositionSelected = -1;
    private int listPositionSelected = -1;
    /**
     * Builds our select budget dialog with a title, a single choice list, and three buttons
     * @param savedInstanceState - unused
     * @return the newly built dialog
     */
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final BadBudgetApplication application = (BadBudgetApplication)getActivity().getApplication();
        final ArrayList<String> budgetNames = new ArrayList<String>();

        Map<Integer, String> budgetIdToNameMap = application.getBudgetMapIDToName();
        String selectedBudgetName = budgetIdToNameMap.get(application.getSelectedBudgetId());

        int currIndex = 0;
        for (String budgetName : budgetIdToNameMap.values())
        {
            budgetNames.add(budgetName);
            if (budgetName.equals(selectedBudgetName))
            {
                initialListPositionSelected = currIndex;
                listPositionSelected = initialListPositionSelected;
            }
            currIndex++;
        }

        ListAdapter listAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_single_choice, budgetNames);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.select_dialog_title)
                .setSingleChoiceItems(listAdapter, listPositionSelected, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        listPositionSelected = which;
                    }
                })
                .setPositiveButton(R.string.select_dialog_select, new DialogInterface.OnClickListener() {
                    /*
                    Positive button click is the 'select' button which, if the selection isn't the current
                    selected application budget, will update the application budget selection as well
                    as set the new selection to the default if the user exits and reenters the application
                     */
                    public void onClick(DialogInterface dialog, int id)
                    {
                        if (initialListPositionSelected != listPositionSelected)
                        {
                            SelectBudgetTask task = new SelectBudgetTask((BadBudgetBaseActivity)getActivity(), budgetNames.get(listPositionSelected), null, false, false);
                            task.execute();
                        }

                    }
                })
                .setNegativeButton(R.string.select_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                })
                .setNeutralButton(R.string.select_dialog_create_new, FlavorSpecific.createNewBudgetClickListener(this));

        return builder.create();
    }
}
