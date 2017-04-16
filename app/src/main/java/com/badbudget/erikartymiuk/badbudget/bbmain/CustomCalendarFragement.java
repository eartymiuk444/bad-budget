package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.erikartymiuk.badbudgetlogic.main.Prediction;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Custom calendar fragment class. Uses a custom calendar view as its view and receives updates when
 * the selected date changes in its calendar view.
 * Created by Erik Artymiuk on 12/22/2016.
 */
public class CustomCalendarFragement extends Fragment implements DateSelectedHandler
{
    public static final String YEAR_KEY = "YEAR";
    public static final String MONTH_KEY = "MONTH";
    public static final String TODAY_CAL_KEY = "TODAY_CAL";
    public static final String SELECTED_CAL_KEY = "SELECTED_CAL";
    public static final String MIN_CAL_KEY = "MIN_CAL";
    public static final String MAX_CAL_KEY = "MAX_CAL";

    private CustomCalendarView rootView;

    private int year;
    private int month;
    private Calendar todayCal;
    private Calendar selectedCal;
    private Calendar minCal;
    private Calendar maxCal;

    private DateSelectedHandler dateSelectedHandler;

    /**
     * On create for a custom calendar view. Expects year, month, today cal, selected cal,
     * min, max cal to be passed in the arguments bundle using the defined keys.
     * @param savedInstanceState - to restore state upon recreation of this fragment.
     */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle args = this.getArguments();
        year = args.getInt(YEAR_KEY);
        month = args.getInt(MONTH_KEY);

        todayCal = (Calendar)args.getSerializable(TODAY_CAL_KEY);
        selectedCal = (Calendar)args.getSerializable(SELECTED_CAL_KEY);
        minCal = (Calendar)args.getSerializable(MIN_CAL_KEY);
        maxCal = (Calendar)args.getSerializable(MAX_CAL_KEY);

        this.todayCal = todayCal != null ? new GregorianCalendar(todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH)) : null;
        this.selectedCal = selectedCal != null ? new GregorianCalendar(selectedCal.get(Calendar.YEAR), selectedCal.get(Calendar.MONTH), selectedCal.get(Calendar.DAY_OF_MONTH)) : null;
        this.minCal = minCal != null ? new GregorianCalendar(minCal.get(Calendar.YEAR), minCal.get(Calendar.MONTH), minCal.get(Calendar.DAY_OF_MONTH)) : null;
        this.maxCal = maxCal != null ? new GregorianCalendar(maxCal.get(Calendar.YEAR), maxCal.get(Calendar.MONTH), maxCal.get(Calendar.DAY_OF_MONTH)) : null;
    }

    /**
     * Overridden on create view for the custom calendar fragment class. Uses a custom calendar
     * view as its root view. Sets the dates using the dates extracted per creation of this object.
     * @param inflater - unused
     * @param container - unused
     * @param savedInstanceState - unused
     * @return the custom calendar view to be used as the root view of this fragment
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        rootView = new CustomCalendarView(getContext(), null, year, month, todayCal, selectedCal, minCal, maxCal);
        rootView.setDateSelectedHandler(dateSelectedHandler);
        return rootView;
    }

    /**
     * Sets the date selected handler, the object to be called when a new date is selected.
     * @param dateSelectedHandler - the object to call when a new date is selected
     */
    public void setDateSelectedHandler(DateSelectedHandler dateSelectedHandler)
    {
        this.dateSelectedHandler = dateSelectedHandler;
    }

    /**
     * Callback method for when the root custom calendar view indicates the user has selected a
     * new date.
     * @param year - the newly selected year
     * @param month - the newly selected month
     * @param day - the newly selected day
     */
    public void onDateSelected(int year, int month, int day)
    {
        selectedCal = new GregorianCalendar(year, month, day);
        if (dateSelectedHandler != null)
        {
            dateSelectedHandler.onDateSelected(year, month, day);
        }
    }

    /**
     * Sets this fragments year and month that it should display.
     * @param year - the year to display
     * @param month - the month to display
     */
    public void setYearMonth(int year, int month)
    {
        this.year = year;
        this.month = month;
        rootView.setYearMonth(year, month);
    }

    /**
     * Sets what this fragment should consider its today date.
     * @param year - the today year
     * @param month - the today month
     * @param day - the today day
     */
    public void setTodayDate(int year, int month, int day)
    {
        todayCal = new GregorianCalendar(year, month, day);
        rootView.setTodayDate(year, month, day);
    }

    /**
     * Sets what this fragment should consider the currently selected date.
     * @param year - the selected year
     * @param month - the selected month
     * @param day - the selected day
     */
    public void setSelectedDate(int year, int month, int day)
    {
        selectedCal = new GregorianCalendar(year, month, day);
        rootView.setSelectedDate(year, month, day);
    }

    /**
     * Sets what this fragment should consider the current minimum date.
     * @param year - the minimum year
     * @param month - the minimum month
     * @param day - the minimum day
     */
    public void setMinDate(int year, int month, int day)
    {
        minCal = new GregorianCalendar(year, month, day);
        rootView.setMinDate(year, month, day);
    }

    /**
     * Sets what this fragment should consider the current maximum date.
     * @param year - the max year
     * @param month - the max month
     * @param day - the max day
     */
    public void setMaxDate(int year, int month, int day)
    {
        maxCal = new GregorianCalendar(year, month, day);
        rootView.setMaxDate(year, month, day);
    }
}
