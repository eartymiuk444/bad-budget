package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.badbudget.erikartymiuk.badbudget.R;

/**
 * Dialog that displays a title, an edit text field, and two buttons which allow the user to edit
 * an existing tracker history item's user description.
 */
public class UserDescriptionDialog extends DialogFragment {

    private UserDescriptionDialogListener mListener;
    private EditText userDescriptDialogEditText;

    /**
     * Public interface to be implemented by the activity class that is the parent of this user description dialog.
     */
    public interface UserDescriptionDialogListener {
        /**
         * Callback method that is called when the ok button is clicked in the user description dialog. The
         * dialog is passed as a parameter which can then be queried for its data (i.e. the user input in the text field)
         * @param fragment - the UserDescriptionDialog that ok was pressed on. Can be queried for the user's input
         */
        public void onOkClick(UserDescriptionDialog fragment);
    }

    /**
     * @Override
     * Overridden onAttach method that attaches the passed activity to this dialog. Checks that the parent
     * activity implements the UserDescriptionDialog interface as it should be able to receive the callback
     * onOkClick.
     *
     * @param activity - the activity to attach this dialog to.
     */
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (UserDescriptionDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString());
        }
    }

    /**
     * Builds our user description dialog with a title, an edit text field, and two buttons.
     * Should be passed in its arguments the budget and user descriptions using the BUDGET_DESCRIPTION_KEY
     * and the USER_DESCRIPTION_KEY (constants in the BadBudgetApplication class).
     * @param savedInstanceState - unused
     * @return the newly built dialog
     */
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Bundle args = this.getArguments();

        String budgetItemDescript = args.getString(BadBudgetApplication.BUDGET_DESCRIPTION_KEY);
        String currentUserDescript = args.getString(BadBudgetApplication.USER_DESCRIPTION_KEY);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View customLayout = inflater.inflate(R.layout.dialog_user_description, null);

        userDescriptDialogEditText = (EditText)customLayout.findViewById(R.id.userInputDescription);

        if (currentUserDescript != null && !currentUserDescript.equals(""))
        {
            userDescriptDialogEditText.setText(currentUserDescript);
        }
        userDescriptDialogEditText.setHint(budgetItemDescript + " - " + getString(R.string.user_description_optional_description));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(customLayout)
        .setTitle(R.string.user_description_dialog_title)
        .setPositiveButton(R.string.user_description_dialog_ok, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                mListener.onOkClick(UserDescriptionDialog.this);
            }
        })
        .setNegativeButton(R.string.user_description_dialog_cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {

            }
        });
        return builder.create();
    }

    /**
     * Gets the input of the user description field. Should be queried in onOkClick to see what the user
     * input for the user description field after clicking ok on the UserDescriptionDialog
     * @return - the value of the edit text field of this dialog
     */
    public String getUserDescription()
    {
        return this.userDescriptDialogEditText.getText().toString();
    }
}
