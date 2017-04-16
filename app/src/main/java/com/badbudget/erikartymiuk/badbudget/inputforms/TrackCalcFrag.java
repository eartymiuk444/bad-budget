package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.erikartymiuk.badbudgetlogic.budget.BudgetItem;

/**
 * Class for the tracker calculator fragment that is used to input more exact transaction details
 * for the budget tracker. Can add, subtract, and reset a budget item's current value.
 * TODO 4/13/2017 - Additionally allows for the input of a description beyond the budget item name.
 */
public class TrackCalcFrag extends DialogFragment {

    public static final String ITEM_DESCRIPTION_KEY = "ITEM_DESCRIPTION";
    public static final String PLUS_CHARACTER = "+";
    public static final String MINUS_CHARACTER = "-";
    public static final String DECIMAL_CHARACTER = ".";

    private double savedResult = 0.0;   //The saved previous result that can be reused to calculate the
                                            // new running result.
    private double currResult = 0.0;    //The current result that takes into account the full input field and not
                                            //just numbers up to the last operation sign

    private BudgetItem item;

    /* Flag indicating if this operation should be marked as a reset in the tracker history if ok were to be pressed */
    private boolean resetFlag = false;

    /* Flags set to indicate what input buttons can be pressed */
    private boolean plusMinusOk = false;
    private boolean decimalOk = false;
    private boolean numbersOk = false;
    private boolean submitOk = false;

    /* The parent activity that is attached to this fragment and that implements the TrackerCalculatorListener interface*/
    private TrackerCalculatorListener mListener;

    /**
     * Public interface to be implemented by the activity class that is the parent of this tracker calculator.
     */
    public interface TrackerCalculatorListener {
        /**
         * Callback method that is called when the ok button is clicked in the tracker calculator. The
         * dialog is passed as a parameter which can then be queried for its data.
         * @param fragment - the TrackCalcFrag that ok was pressed on. Can be queried for the result of the user's action
         */
        public void onOkClick(TrackCalcFrag fragment);
    }

    /**
     * @Override
     * Overridden onAttach method that attaches the passed activity to this dialog. Checks that the parent
     * activity implements the TrackerCalculatorListener interface as it should be able to recieve the callback
     * onOkClick.
     *
     * @param activity - the activity to attach this dialog to.
     */
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (TrackerCalculatorListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString());
        }
    }

    /**
     * On create for the TrackCalcFrag. Initializes the input and result fields to be the current value of
     * the passed budget item. The budget item name should be passed via setArguments() as a string with the
     * key ITEM_DESCRIPTION_KEY. Also initializes all click listeners/handlers for the calculator buttons. Sets
     * the layout as the custom layout defined by "content_tracker_calculator."
     * TODO 4/13/2017 - Sets the description field hint to be: "{Budget Item Cat} - Description (Optional)"
     * @param savedInstanceState - unused
     * @return - the newly created dialog
     */
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = this.getArguments();

        String itemName = args.getString(ITEM_DESCRIPTION_KEY);
        BadBudgetApplication app = (BadBudgetApplication) this.getActivity().getApplication();
        item = app.getBadBudgetUserData().getBudget().getAllBudgetItems().get(itemName);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View customLayout = inflater.inflate(R.layout.content_tracker_calculator, null);
        final TextView input = (TextView) customLayout.findViewById(R.id.track_calc_input);
        final TextView result = (TextView) customLayout.findViewById(R.id.track_calc_result);
        final Button plus = (Button) customLayout.findViewById(R.id.track_calc_plus);
        final Button minus = (Button) customLayout.findViewById(R.id.track_calc_minus);
        final Button decimal = (Button) customLayout.findViewById(R.id.track_calc_decimal);
        final Button clear = (Button) customLayout.findViewById(R.id.track_calc_clear);
        final Button cancel = (Button) customLayout.findViewById(R.id.track_calc_cancel);
        final Button ok = (Button) customLayout.findViewById(R.id.track_calc_ok);
        final Button reset = (Button) customLayout.findViewById(R.id.track_calc_reset);
        //final EditText description = (EditText) customLayout.findViewById(R.id.track_calc_description);

        //description.setHint(item.getDescription() + " - " + description.getHint());

        input.setText(BadBudgetApplication.roundedDoubleBB(item.getCurrAmount()));
        result.setText(BadBudgetApplication.roundedDoubleBB(item.getCurrAmount()));

        savedResult = item.getCurrAmount();
        currResult = item.getCurrAmount();

        plusMinusOk = true;
        decimalOk = false;
        numbersOk = false;
        submitOk = true;

        /*
        Click handler for the numbers 0-9 buttons.
        */
        clickListTCalcNum[] numberListeners = new clickListTCalcNum[10];
        for (int i = 0; i < 10; i++) {
            clickListTCalcNum listener = new clickListTCalcNum(i) {
                @Override
                public void onClick(View v) {
                    if (numbersOk) {
                        input.setText(input.getText().toString() + Integer.toString(this.getNumber()));
                        parseLastNumber(input.getText().toString(), result);

                        plusMinusOk = true;
                        decimalOk = decimalOk;
                        numbersOk = true;
                        submitOk = true;

                        resetFlag = false;
                    }
                }
            };
            numberListeners[i] = listener;
        }

        customLayout.findViewById(R.id.track_calc_0).setOnClickListener(numberListeners[0]);
        customLayout.findViewById(R.id.track_calc_1).setOnClickListener(numberListeners[1]);
        customLayout.findViewById(R.id.track_calc_2).setOnClickListener(numberListeners[2]);
        customLayout.findViewById(R.id.track_calc_3).setOnClickListener(numberListeners[3]);
        customLayout.findViewById(R.id.track_calc_4).setOnClickListener(numberListeners[4]);
        customLayout.findViewById(R.id.track_calc_5).setOnClickListener(numberListeners[5]);
        customLayout.findViewById(R.id.track_calc_6).setOnClickListener(numberListeners[6]);
        customLayout.findViewById(R.id.track_calc_7).setOnClickListener(numberListeners[7]);
        customLayout.findViewById(R.id.track_calc_8).setOnClickListener(numberListeners[8]);
        customLayout.findViewById(R.id.track_calc_9).setOnClickListener(numberListeners[9]);

        /* Click handler for the plus button */
        plus.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (plusMinusOk)
                {
                    input.setText(input.getText().toString() + PLUS_CHARACTER);
                    savedResult = currResult;

                    plusMinusOk = false;
                    decimalOk = true;
                    numbersOk = true;
                    submitOk = false;

                    resetFlag = false;
                }
            }
        });

        /* Click handler for the minus button */
        minus.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (plusMinusOk)
                {
                    input.setText(input.getText().toString() + MINUS_CHARACTER);
                    savedResult = currResult;

                    plusMinusOk = false;
                    decimalOk = true;
                    numbersOk = true;
                    submitOk = false;

                    resetFlag = false;
                }
            }
        });

        /* Click handler for the decimal button */
        decimal.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (decimalOk)
                {
                    input.setText(input.getText().toString() + DECIMAL_CHARACTER);

                    plusMinusOk = false;
                    decimalOk = false;
                    numbersOk = true;
                    submitOk = false;

                    resetFlag = false;
                }
            }
        });

        /* Click handler for the clear button - sets the input and result fields to the currAmt of the item*/
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                input.setText(BadBudgetApplication.roundedDoubleBB(item.getCurrAmount()));
                result.setText(BadBudgetApplication.roundedDoubleBB(item.getCurrAmount()));

                savedResult = item.getCurrAmount();
                currResult = item.getCurrAmount();

                plusMinusOk = true;
                decimalOk = false;
                numbersOk = false;
                submitOk = true;

                resetFlag = false;
            }
        });

        /* Click handler for the cancel button - sets the reset flag to false and dismisses the dialog */
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFlag = false;
                TrackCalcFrag.this.dismiss();
            }
        });

        /* Click handler for the ok button - dismisses the dialog and calls the callback onOkClick on the attached
        * listener. */
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TrackCalcFrag.this.dismiss();
                mListener.onOkClick(TrackCalcFrag.this);
            }
        });

        /* Click handler for the reset button - set the input and result field to the loss amount of the
        * item and sets the reset flag */
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                input.setText(BadBudgetApplication.roundedDoubleBB(item.lossAmount()));
                result.setText(BadBudgetApplication.roundedDoubleBB(item.lossAmount()));

                savedResult = item.lossAmount();
                currResult = item.lossAmount();

                plusMinusOk = true;
                decimalOk = false;
                numbersOk = false;
                submitOk = true;

                resetFlag = true;
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(customLayout);
        return builder.create();
    }

    /**
     * Private helper method that looks for the last number after the last operation character in the
     * input String. Takes the last number and does that operations to the savedResult setting the result
     * to the currResult field. Sets the result field text to the currResult.
     * @param input - the curr input text string
     * @param result - the result text view to update the text of
     */
    private void parseLastNumber(String input, TextView result)
    {
        int operationIndex = input.length() - 1;
        char currChar = input.charAt(operationIndex);
        while (!(currChar == '+' || currChar == '-'))
        {
            operationIndex--;
            currChar = input.charAt(operationIndex);
        }

        if (currChar == '+')
        {
            currResult = savedResult + Double.parseDouble(input.substring(operationIndex + 1));
        }
        else
        {
            currResult = savedResult - Double.parseDouble(input.substring(operationIndex + 1));
        }
        result.setText(BadBudgetApplication.roundedDoubleBB(currResult));
    }

    /**
     * After ok submit this indicates if the user manually reset the item's curr value to its loss value.
     * @return - true if the ok action should be considered as a user reset, false otherwise
     */
    public boolean userReset()
    {
        return this.resetFlag;
    }

    /**
     * After an ok sumbit this indicates what the final result is of an action conducted via the tracker calculator.
     * @return -  the result of any action taken by the tracker calculator
     */
    public double result()
    {
        return this.currResult;
    }

    /**
     * The budget item (its description) that this dialog fragement is working on.
     * @return - the description of the budget item being worked on by this dialog.
     */
    public String budgetItemDescription()
    {
        return this.item.getDescription();
    }
}
