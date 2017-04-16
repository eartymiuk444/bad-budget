package com.badbudget.erikartymiuk.badbudget.inputforms;

/**
 * Activity interface to be implemented by activities using the DatePickerFragment to have users pick
 * dates.
 * Created by Erik Artymiuk on 7/21/2016.
 */
public interface DateInputActivity
{
    /**
     * Callback method called when a child date picker fragment has a date chosen. The return code
     * is what was passed into the arguments for the datePicker for its creation.
     * @param year
     * @param month
     * @param day
     * @param returnCode
     */
    public void dateSet(int year, int month, int day, int returnCode);
}
