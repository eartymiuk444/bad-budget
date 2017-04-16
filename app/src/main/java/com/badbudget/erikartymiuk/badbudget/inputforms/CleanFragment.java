package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.badbudget.erikartymiuk.badbudget.R;

import java.util.ArrayList;

/**
 * Dialog Fragment to be presented on a clean button press with inconsistent data.
 * Presents to the user choices (up to 4) of how they could update their
 * values so that they are clean. The values are determined by the parent activity and this fragment
 * simply presents them and determines which one they want cleaned and then calls back the parent with
 * the position of the selected item. This is done using the keys specified in the Clean Fragment class and
 * a arguments bundle. The user can also cancel this fragment in which case nothing is done. (no callback)
 * Created by Erik Artymiuk on 7/2/2016.
 */
public class CleanFragment extends DialogFragment {

    private int positionSet = 0;

    public static final String NUM_ITEMS_KEY = "CLEAN_NUM_ITEMS_KEY";

    public static final String POSITION_ONE_TITLE_KEY = "CLEAN_ONE_TITLE_KEY";
    public static final String POSITION_ONE_OLD_KEY = "CLEAN_ONE_OLD_KEY";
    public static final String POSITION_ONE_NEW_KEY = "CLEAN_ONE_NEW_KEY";

    public static final String POSITION_TWO_TITLE_KEY = "CLEAN_TWO_TITLE_KEY";
    public static final String POSITION_TWO_OLD_KEY = "CLEAN_TWO_OLD_KEY";
    public static final String POSITION_TWO_NEW_KEY = "CLEAN_TWO_NEW_KEY";

    public static final String POSITION_THREE_TITLE_KEY = "CLEAN_THREE_TITLE_KEY";
    public static final String POSITION_THREE_OLD_KEY = "CLEAN_THREE_OLD_KEY";
    public static final String POSITION_THREE_NEW_KEY = "CLEAN_THREE_NEW_KEY";

    public static final String POSITION_FOUR_TITLE_KEY = "CLEAN_FOUR_TITLE_KEY";
    public static final String POSITION_FOUR_OLD_KEY = "CLEAN_FOUR_OLD_KEY";
    public static final String POSITION_FOUR_NEW_KEY = "CLEAN_FOUR_NEW_KEY";

    /**
     * Overriden DialogFragment method that is called to do initial creation of our Clean Fragment.
     * Setups a new dialog instance with a two buttons a positive "clean" button and the negative
     * "cancel" button. It displays a list of 2-4 choices that the user should choose one from.
     * It gets the values for these choices from the getArguments() bundle object set by the
     * parent activity. It displays the choices in a single choice item list where the user can
     * only select one of the items and if they hit clean that choice is relayed back to the parent
     * activity so it can update that value. The dialog can be canceled either via button or
     * clicking outside the dialogs view in which case nothing is done.
     * @param savedInstanceState - unused
     * @return
     */
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        /*
        The clean button checks which position is currently selected (only one is possible) and calls
        the correct parent method based on the value.
         */
        builder.setPositiveButton(R.string.clean_fragment_update, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id)
            {
                CleanFragmentParent parent = (CleanFragmentParent) CleanFragment.this.getActivity();
                parent.cleanSelection(positionSet);
            }
        });

        /*
         No action is taken if the user chooses to click the cancel button
         */
        builder.setNegativeButton(R.string.clean_fragment_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id)
            {
            }
        });

        Bundle bundle = this.getArguments();

        int numItems = bundle.getInt(CleanFragment.NUM_ITEMS_KEY);
        ArrayList<CharSequence> itemStrings = new ArrayList<CharSequence>();

        String item1Title = bundle.getString(CleanFragment.POSITION_ONE_TITLE_KEY);
        String item1Old = bundle.getString(CleanFragment.POSITION_ONE_OLD_KEY);
        String item1New = bundle.getString(CleanFragment.POSITION_ONE_NEW_KEY);

        String item1String = item1Title + " " + item1Old + " -> " + item1New;

        String item2Title = bundle.getString(CleanFragment.POSITION_TWO_TITLE_KEY);
        String item2Old = bundle.getString(CleanFragment.POSITION_TWO_OLD_KEY);
        String item2New = bundle.getString(CleanFragment.POSITION_TWO_NEW_KEY);

        String item2String = item2Title + " " + item2Old + " -> " + item2New;

        itemStrings.add(item1String);
        itemStrings.add(item2String);

        if (numItems > 2)
        {
            String item3Title = bundle.getString(CleanFragment.POSITION_THREE_TITLE_KEY);
            String item3Old = bundle.getString(CleanFragment.POSITION_THREE_OLD_KEY);
            String item3New = bundle.getString(CleanFragment.POSITION_THREE_NEW_KEY);

            String item3String = item3Title + " " + item3Old + " -> " + item3New;
            itemStrings.add(item3String);

            if (numItems > 3)
            {
                String item4Title = bundle.getString(CleanFragment.POSITION_FOUR_TITLE_KEY);
                String item4Old = bundle.getString(CleanFragment.POSITION_FOUR_OLD_KEY);
                String item4New = bundle.getString(CleanFragment.POSITION_FOUR_NEW_KEY);

                String item4String = item4Title + " " + item4Old + " -> " + item4New;
                itemStrings.add(item4String);
            }
        }

        /*
        Chaining method calls to set the dialogs attributes. Also need to update the position that is
        chosen when a new item is selected.
         */
        builder.setTitle(R.string.clean_fragment_title).setSingleChoiceItems(itemStrings.toArray(new CharSequence[0]), positionSet,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int position)
                    {
                        positionSet = position;
                    }
                });
        return builder.create();
    }
}
