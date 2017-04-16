package com.badbudget.erikartymiuk.bbmain;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetBaseActivity;
import com.badbudget.erikartymiuk.badbudget.bbmain.SelectBudgetTask;

import java.util.Map;

/**
 * This is a dialog for the user to create a new budget; either completely new or by copying an existing
 * budget. Built dialog contains a title, a custom layout with an edit text view for the user to input
 * the new budget's name, and three buttons: a confirm button to create a new budget with the entered name,
 * a cancel button to completely exit the dialog, and a copy button to display another dialog presenting the
 * user with a list of existing budgets to chose one to copy.
 * Created by Erik Artymiuk on 11/22/2016.
 */
public class CreateBudgetDialog extends DialogFragment {

    /**
     * Builds and returns the create budget dialog.
     * @param savedInstanceState - unused
     * @return the built create budget dialog
     */
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final BadBudgetApplication application = (BadBudgetApplication)getActivity().getApplication();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View customLayout = inflater.inflate(R.layout.dialog_create_budget, null);

        final EditText userInputBudgetName = (EditText)customLayout.findViewById(R.id.userInputBudgetName);

        builder.setTitle(R.string.create_dialog_title)
                .setView(customLayout)
                .setPositiveButton(R.string.create_dialog_confirm, new DialogInterface.OnClickListener() {
                    /*
                    Positive button click uses the input name if not a duplicate or empty
                    to create a new budget and sets the default budget to that budget
                     */
                    public void onClick(DialogInterface dialog, int id)
                    {
                        String budgetName = userInputBudgetName.getText().toString();
                        Map<Integer, String> budgetIdToNameMap = application.getBudgetMapIDToName();

                        //TODO 11/25 fails silently if duplicate or empty name provided
                        if (!budgetName.equals("") && !budgetIdToNameMap.values().contains(budgetName))
                        {
                            SelectBudgetTask task = new SelectBudgetTask((BadBudgetBaseActivity)getActivity(), null, budgetName, true, false);
                            task.execute();
                        }
                    }
                })
                .setNegativeButton(R.string.create_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                })
                .setNeutralButton(R.string.create_dialog_copy, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        DialogFragment copyBudgetDialog = new CopyExistingDialog();
                        copyBudgetDialog.show(CreateBudgetDialog.this.getFragmentManager(), "copy_budget_dialog_tag");
                    }
                });
        return builder.create();
    }
}
