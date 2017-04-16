package com.badbudget.erikartymiuk.bbmain;
import android.app.Activity;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;

import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.SelectBudgetDialog;

/**
 * Class that contains code specific to the one of the bad budget applications flavors.
 * This class is specific to the paid flavor
 * Created by Erik Artymiuk on 3/26/2017.
 */
public class FlavorSpecific {

    /**
     * This method does nothing and should not be called. Enabling ads in the paid flavor
     * of bad budget is an error.
     * @param application - unused
     */
    public static void enableAds(BadBudgetApplication application)
    {
    }

    /**
     * This method does nothing and should not be called. Making a request for an ad
     * is an error in the pair flavor of bad budget. Method signature is needed for compilation.
     * @param activity - unused
     * @param layoutId - unused
     */
    public static void adRequest(Activity activity, int layoutId)
    {
    }

    /**
     * Flavor specific method for constructing the click listener when the 'create new budget' button is pressed in the
     * select budget dialog. This is specific to the paid flavor and creates a click listener that shows the
     * create budget dialog.
     * @param sbd - the SelectBudgetDialog the create new budget click originated from
     * @return - a click listener to be attached to the "Create New Budget" button of the SelectBudgetDialog
     */
    public static DialogInterface.OnClickListener createNewBudgetClickListener(final SelectBudgetDialog sbd)
    {
        DialogInterface.OnClickListener ocl = new DialogInterface.OnClickListener() {
            /*
            The neutral button click is the 'create new' budget action where the user is taken to
            another dialog where they will have the option of creating a new budget. Either completely new
            or by copying an existing budget
             */
            public void onClick(DialogInterface dialog, int id)
            {
                DialogFragment createBudgetDialog = new CreateBudgetDialog();
                createBudgetDialog.show(sbd.getFragmentManager(), "create_budget_dialog_tag");
            }
        };
        return ocl;
    }
}
