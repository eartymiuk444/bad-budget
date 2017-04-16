package com.badbudget.erikartymiuk.badbudget.inputforms;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import com.badbudget.erikartymiuk.badbudget.inputforms.DateInputActivity;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 *  Date Picker Fragment for use in any activity that needs a date to be chosen
 *  (i.e. implements the DateInputActivity interface). The calling
 *  activity sets the default and min date via the arguments bundle and the year, month, day, currentYear,
 *  currentMonth, and currentDay values. The keys are specified as public constants. There is also the option for the calling
 *  activity to specify a code via the RETURN_CODE argument. This integer is simply passed back
 *  to the activity that created it when a date is set.
 *
 * Created by Erik Artymiuk on 7/1/2016.
 */
public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener
{
    /* Keys for setting the min date. Should represent today's date */
    public static final String TODAY_YEAR_KEY = "TODAY_YEAR";
    public static final String TODAY_MONTH_KEY = "TODAY_MONTH";
    public static final String TODAY_DAY_KEY = "TODAY_DAY";

    /* Keys for setting default date. Should be a date on or after today */
    public static final String CURRENT_CHOSEN_YEAR_KEY = "CURRENT_CHOSEN_YEAR";
    public static final String CURRENT_CHOSEN_MONTH_KEY = "CURRENT_CHOSEN_MONTH";
    public static final String CURRENT_CHOSEN_DAY_KEY = "CURRENT_CHOSEN_DAY";

    /* Key for a return code that if passed in is returned to the parent in the date set method */
    public static final String RETURN_CODE_KEY = "RETURN_CODE";

    /* Code to be returned to the parent when the date is set */
    private int returnCode;

    /** Setups the datePicker dialog using the passed in arguments to get a hold of what should
     * be considered today's date which will be considered the min date and the currently chosen
     * date to be used as the default date
     * @param savedInstanceState - unused
       */
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Bundle args = this.getArguments();
        returnCode = args.getInt(RETURN_CODE_KEY);

        //Use the passed current day arguments to set the start date.
        int year = args.getInt(TODAY_YEAR_KEY);
        int month = args.getInt(TODAY_MONTH_KEY);
        int day = args.getInt(TODAY_DAY_KEY);

        int chosenYear = args.getInt(CURRENT_CHOSEN_YEAR_KEY);
        int chosenMonth = args.getInt(CURRENT_CHOSEN_MONTH_KEY);
        int chosenDay = args.getInt(CURRENT_CHOSEN_DAY_KEY);

        Calendar todayCal = new GregorianCalendar(year, month, day);

        // Create a new instance of DatePickerDialog and return it
        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), this, chosenYear, chosenMonth, chosenDay);
        DatePicker datePicker = datePickerDialog.getDatePicker();
        datePicker.setMinDate(todayCal.getTimeInMillis());
        datePicker.setCalendarViewShown(false);
        return datePickerDialog;
    }

    /**
     * Callback method for when the user picks a date using this dialog. It is assumed that the parent
     * implements the DateInputActivity Interface so that the dateSet method can be called from this method.
     * TODO - rather than assuming the parent activity is a DateInputActivity pass it in on creation. So it must be
     * @param view - the datePicker that where the date was chosen
     * @param year - the year chosen
     * @param monthOfYear - the month chosen
     * @param dayOfMonth - the day chosen
     */
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
    {
        DateInputActivity parent = (DateInputActivity) this.getActivity();
        parent.dateSet(year, monthOfYear, dayOfMonth, returnCode);
    }
}
