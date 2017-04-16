package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.view.View;
import android.widget.TextView;

/**
 * Extension abstract class of View.OnClickListener interface. Should be used for click listeners of
 * number buttons in the TrackCalcFrag. Includes a number field to be set on creation that indicates
 * what number the pressed button is considered to display.
 */
public abstract class clickListTCalcNum implements View.OnClickListener {

    private int number;

    /**
     * Constructor that takes a number int as a param. This num should be considered the number that
     * the button this click handler is attached to is displaying.
     * @param number - the number this click handler's button is displaying
     */
    public clickListTCalcNum(int number)
    {
        this.number = number;
    }

    /**
     * Returns this click handler's view's number.
     * @return The number this click handler's view is displaying
     */
    public int getNumber()
    {
        return this.number;
    }
}
