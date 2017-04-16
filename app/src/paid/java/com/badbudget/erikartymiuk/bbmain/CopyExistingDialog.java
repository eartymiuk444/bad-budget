package com.badbudget.erikartymiuk.bbmain;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetBaseActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.SelectBudgetTask;

import java.util.ArrayList;
import java.util.Map;

/**
 * The copy exisiting dialog is a dialog that presents all existing budgets in a single choice list
 * where the user can select one and then input a name to create a new budget that is copied from the
 * selected budget.
 * Created by Erik Artymiuk on 11/25/2016.
 */
public class CopyExistingDialog extends DialogFragment
{
    private int listPositionSelected = -1;

    /**
     * Builds our copy budget dialog with a title, a single choice list, an edit text field for the
     * name of the new budget and two buttons
     * @param savedInstanceState - unused
     * @return the newly built dialog
     */
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final BadBudgetApplication application = (BadBudgetApplication)getActivity().getApplication();
        final ArrayList<String> budgetNames = new ArrayList<String>();

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View customLayout = inflater.inflate(R.layout.dialog_copy_existing, null);

        final EditText userInputBudgetName = (EditText)customLayout.findViewById(R.id.userInputBudgetName);
        final ListView singleChoiceBudgetList = (ListView)customLayout.findViewById(R.id.singleChoiceBudgetList);

        Map<Integer, String> budgetIdToNameMap = application.getBudgetMapIDToName();
        String selectedBudgetName = budgetIdToNameMap.get(application.getSelectedBudgetId());

        int currIndex = 0;
        for (String budgetName : budgetIdToNameMap.values())
        {
            budgetNames.add(budgetName);
            if (budgetName.equals(selectedBudgetName))
            {
                listPositionSelected = currIndex;
            }
            currIndex++;
        }

        singleChoiceBudgetList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent) {}
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                listPositionSelected = position;
            }

        });

        ListAdapter listAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_single_choice, budgetNames);
        singleChoiceBudgetList.setAdapter(listAdapter);
        singleChoiceBudgetList.setItemChecked(listPositionSelected, true);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.copy_existing_dialog_title)
                .setView(customLayout)
                .setPositiveButton(R.string.copy_existing_dialog_copy, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id)
                    {

                        String budgetName = userInputBudgetName.getText().toString();
                        Map<Integer, String> budgetIdToNameMap = application.getBudgetMapIDToName();

                        //TODO 11/26 fails silently if duplicate or empty name provided
                        if (!budgetName.equals("") && !budgetIdToNameMap.values().contains(budgetName))
                        {
                            SelectBudgetTask task = new SelectBudgetTask((BadBudgetBaseActivity)getActivity(), budgetNames.get(listPositionSelected), budgetName, true, true);
                            task.execute();
                        }
                    }
                })
                .setNegativeButton(R.string.select_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });

        return builder.create();
    }
}
