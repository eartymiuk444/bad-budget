package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

/**
 * Custom pager for calendar views. The items in this pager are customCalendarFragments which show
 * one month for a set year and month. This pager only considers a fragment as having a single month
 * for a given year even though the custom calendar fragment can update its view to show other months.
 *
 * Created by Erik Artymiuk on 12/22/2016.
 */
public class CustomCalendarPagerAdapter extends FragmentStatePagerAdapter implements DateSelectedHandler {

    private HashMap<Integer, CustomCalendarFragement> currentInstantiatedItems;

    private Calendar todayCal;
    private Calendar selectedCal;
    private Calendar minCal;
    private Calendar maxCal;

    private DateSelectedHandler dateSelectedHandler;

    /**
     * Constructor for the CustomCalendarPagerAdapter.
     * @param fragmentManager - fragment manager to use for setting up this pager
     * @param todayCal - the date to consider today's date, can be null
     * @param selectedCal - the date to consider as our selected date, can be null
     * @param minCal - the date to consider as our minimum date, cannot be null
     * @param maxCal - the date to consider as our max date, cannot be null
     */
    public CustomCalendarPagerAdapter(FragmentManager fragmentManager, Calendar todayCal,
                                      Calendar selectedCal, Calendar minCal, Calendar maxCal)
    {
        super(fragmentManager);

        currentInstantiatedItems = new HashMap<>();

        this.todayCal = todayCal != null ? new GregorianCalendar(todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH)) : null;
        this.selectedCal = selectedCal != null ? new GregorianCalendar(selectedCal.get(Calendar.YEAR), selectedCal.get(Calendar.MONTH), selectedCal.get(Calendar.DAY_OF_MONTH)) : null;
        this.minCal = minCal != null ? new GregorianCalendar(minCal.get(Calendar.YEAR), minCal.get(Calendar.MONTH), minCal.get(Calendar.DAY_OF_MONTH)) : null;
        this.maxCal = maxCal != null ? new GregorianCalendar(maxCal.get(Calendar.YEAR), maxCal.get(Calendar.MONTH), maxCal.get(Calendar.DAY_OF_MONTH)) : null;

        if (minCal == null || maxCal == null)
        {
            throw new NullPointerException();
        }
    }

    /**
     * Overridden get item method that retrieves the custom calendar fragment for the given position.
     * This will be the custom calendar fragment that is i months away from the minimum month.
     * @param i - the position of the fragment to retrieve, the number of months from min we want for
     *          the retrieved fragment
     * @return - the fragment at position i, the fragment i months from the set min
     */
    public Fragment getItem(int i)
    {
        CustomCalendarFragement customCalendarFragement = new CustomCalendarFragement();

        Calendar fragmentCalendar = new GregorianCalendar(minCal.get(Calendar.YEAR), minCal.get(Calendar.MONTH), 1);
        fragmentCalendar.add(Calendar.MONTH, i);

        Bundle args = new Bundle();
        args.putInt(CustomCalendarFragement.YEAR_KEY, fragmentCalendar.get(Calendar.YEAR));
        args.putInt(CustomCalendarFragement.MONTH_KEY, fragmentCalendar.get(Calendar.MONTH));
        args.putSerializable(CustomCalendarFragement.TODAY_CAL_KEY, todayCal);
        args.putSerializable(CustomCalendarFragement.SELECTED_CAL_KEY, selectedCal);
        args.putSerializable(CustomCalendarFragement.MIN_CAL_KEY, minCal);
        args.putSerializable(CustomCalendarFragement.MAX_CAL_KEY, maxCal);

        customCalendarFragement.setArguments(args);
        customCalendarFragement.setDateSelectedHandler(this);

        return customCalendarFragement;
    }

    /**
     * Overridden instantiateItem method. Keeps track of currently instantiated items so we
     * can update that items view if necessary. Calls super.
     * @param container - see super.instantiateItem
     * @param position - the position of the item we are instantiating
     * @return - the fragment that was instantiated
     */
    public Object instantiateItem(ViewGroup container, int position)
    {
        CustomCalendarFragement fragment = (CustomCalendarFragement) super.instantiateItem(container, position);
        currentInstantiatedItems.put(position, fragment);
        return fragment;
    }

    /**
     * Overridden destroyItem method. Calls super.destroyItem but first removes the item being
     * destroyed from our map of currently instantiated item so as not to attempt to update
     * this fragments views.
     * @param container - see super.destroyItem
     * @param position - the position of the item being destroyed
     * @param object - see super.destroyItem
     */
    public void destroyItem(ViewGroup container, int position, Object object)
    {
        currentInstantiatedItems.remove(position);
        super.destroyItem(container, position, object);
    }

    /**
     * Updates the date this pager should consider the today date. Updates any currently instantiated
     * pages(fragments).
     * @param year - today's year
     * @param month - today's month
     * @param day - today's day of the month
     */
    public void setTodayDate(int year, int month, int day)
    {
        todayCal = new GregorianCalendar(year, month, day);
        for (Integer position : currentInstantiatedItems.keySet())
        {
            CustomCalendarFragement fragement = currentInstantiatedItems.get(position);
            fragement.setTodayDate(year, month, day);
        }
    }

    /**
     * Updates the date this pager should consider the selected date. Updates any currently instantiated
     * pages(fragments).
     * @param year - selected day's year
     * @param month - selected day's month
     * @param day - selected day's day of the month
     */
    public void setSelectedDate(int year, int month, int day)
    {
        selectedCal = new GregorianCalendar(year, month, day);
        for (Integer position : currentInstantiatedItems.keySet())
        {
            CustomCalendarFragement fragement = currentInstantiatedItems.get(position);
            fragement.setSelectedDate(year, month, day);
        }
    }

    /**
     * Updates the date this pager should consider the min day date. Updates any currently instantiated
     * pages(fragments).
     * @param year - min day's year
     * @param month - min day's month
     * @param day - min day's day of the month
     */
    public void setMinDate(int year, int month, int day)
    {
        minCal = new GregorianCalendar(year, month, day);
        for (Integer position : currentInstantiatedItems.keySet())
        {
            CustomCalendarFragement fragement = currentInstantiatedItems.get(position);
            fragement.setMinDate(year, month, day);
        }
    }

    /**
     * Updates the date this pager should consider the max day date. Updates any currently instantiated
     * pages(fragments).
     * @param year - max day's year
     * @param month - max day's month
     * @param day - max day's day of the month
     */
    public void setMaxDate(int year, int month, int day)
    {
        maxCal = new GregorianCalendar(year, month, day);
        for (Integer position : currentInstantiatedItems.keySet())
        {
            CustomCalendarFragement fragement = currentInstantiatedItems.get(position);
            fragement.setMaxDate(year, month, day);
        }
    }

    /**
     * Returns the total number of pages that this pager has. This will be the number of
     * months between the min and max dates.
     * @return - the number of pages in this pager
     */
    public int getCount()
    {
        return numMonthsBetween(minCal, maxCal) + 1;
    }

    /**
     * Helper to find the number of months between two dates
     * @param startCal - the starting date
     * @param endCal - the ending date
     * @return - the number of months between start and end, only considers the month and the year of
     *              start and end
     */
    public static int numMonthsBetween(Calendar startCal, Calendar endCal)
    {
        int startYear = startCal.get(Calendar.YEAR);
        int startMonth = startCal.get(Calendar.MONTH);

        int endYear = endCal.get(Calendar.YEAR);
        int endMonth = endCal.get(Calendar.MONTH);

        int numMonths = 12 * (endYear - startYear) + (endMonth - startMonth);
        return numMonths;
    }

    /**
     * Callback method when one of our fragments informs us that a new date was selected by the user.
     * Updates all of our instantiated fragments to reflect this change and calls our date selected
     * handler, if set.
     * @param year - the year selected
     * @param month - the month selected
     * @param day - the day selected
     */
    public void onDateSelected(int year, int month, int day) {
        this.selectedCal = new GregorianCalendar(year, month, day);

        for (Integer position : currentInstantiatedItems.keySet())
        {
            CustomCalendarFragement fragement = currentInstantiatedItems.get(position);
            fragement.setSelectedDate(year, month, day);
        }

        if (dateSelectedHandler != null)
        {
            this.dateSelectedHandler.onDateSelected(year, month, day);
        }
    }

    /**
     * Sets the date selected handler which well be called when user interaction changes our currently
     * selected date.
     * @param handler - the object that will be notified on a selected date change.
     */
    public void setDateSelectedHandler(DateSelectedHandler handler)
    {
        this.dateSelectedHandler = handler;
    }
}
