package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.R;
import com.erikartymiuk.badbudgetlogic.main.Prediction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Custom calendar view class. A view that displays a single month of a given year.
 * Keeps track of the current day, the minimum and maximum date, and the selected day.
 * A handler can be set to be notified when a new date is selected.
 *
 * Created by Erik Artymiuk on 12/22/2016.
 */
public class CustomCalendarView extends LinearLayout
{
    private DateSelectedHandler dateSelectedHandler;

    private TextView txtDate;
    private GridView grid;
    
    private int month;
    private int year;
    private Calendar todayCal;
    private Calendar selectedCal;
    private Calendar minCal;
    private Calendar maxCal;

    private static final String DATE_FORMAT = "MMM yyyy";

    /**
     * Constructor for the custom calendar view. Sets up our calendar to show the passed
     * month and year. Any dates outside of the range are grayed out. Today's date number is
     * highlighted. The selected date's cell is highlighted.
     * @param context - the context for this calendar view
     * @param attrs - the attribute set for this custom calendar view, can be null
     * @param month - the month this calendar view will initially display
     * @param year - the year this calendar view will initially display
     * @param todayCal - the date this calendar will consider as being today's date, can be null
     * @param selectedCal - the date this calendar view will initially consider to be selected. Can be null
     * @param minCal - the minimum date supported, can be null to indicate no minimum
     * @param maxCal - the maximum date supported, can be null to indicate no maximum
     */
    public CustomCalendarView(Context context, AttributeSet attrs,
                              int year, int month, Calendar todayCal,
                              Calendar selectedCal, Calendar minCal, Calendar maxCal)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.custom_calendar_view, this);

        txtDate = (TextView)findViewById(R.id.custom_calendar_date_display);
        grid = (GridView)findViewById(R.id.custom_calendar_grid);

        grid.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> view, View cell, int position, long id)
            {

                Calendar cellCalendar = (Calendar)grid.getAdapter().getItem(position);

                if (cellCalendar != null) {
                    int cellDay = cellCalendar.get(Calendar.DAY_OF_MONTH);
                    int cellMonth = cellCalendar.get(Calendar.MONTH);
                    int cellYear = cellCalendar.get(Calendar.YEAR);

                    boolean cellSelected =  CustomCalendarView.this.selectedCal != null &&
                            (cellDay == CustomCalendarView.this.selectedCal.get(Calendar.DAY_OF_MONTH) &&
                            cellMonth == CustomCalendarView.this.selectedCal.get(Calendar.MONTH) &&
                            cellYear == CustomCalendarView.this.selectedCal.get(Calendar.YEAR));
                    boolean cellBehindMin = CustomCalendarView.this.minCal != null && (Prediction.numDaysBetween(CustomCalendarView.this.minCal.getTime(), cellCalendar.getTime()) < 0);
                    boolean cellAheadMax = CustomCalendarView.this.maxCal != null && (Prediction.numDaysBetween(CustomCalendarView.this.maxCal.getTime(), cellCalendar.getTime()) > 0);

                    if (!cellBehindMin && !cellAheadMax)
                    {
                        if (!cellSelected)
                        {
                            setSelectedDate(cellYear, cellMonth, cellDay);
                        }

                        if (dateSelectedHandler != null)
                        {
                            dateSelectedHandler.onDateSelected(cellYear, cellMonth, cellDay);
                        }
                    }
                }
            }
        });

        this.month = month;
        this.year = year;
        this.todayCal = todayCal != null ? new GregorianCalendar(todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH)) : null;
        this.selectedCal = selectedCal != null ? new GregorianCalendar(selectedCal.get(Calendar.YEAR), selectedCal.get(Calendar.MONTH), selectedCal.get(Calendar.DAY_OF_MONTH)) : null;
        this.minCal = minCal != null ? new GregorianCalendar(minCal.get(Calendar.YEAR), minCal.get(Calendar.MONTH), minCal.get(Calendar.DAY_OF_MONTH)) : null;
        this.maxCal = maxCal != null ? new GregorianCalendar(maxCal.get(Calendar.YEAR), maxCal.get(Calendar.MONTH), maxCal.get(Calendar.DAY_OF_MONTH)) : null;

        updateCalendar();
    }

    /**
     * Change the year and month this calendar should display.
     * @param year - the year to display
     * @param month - the month to display
     */
    public void setYearMonth(int year, int month)
    {
        this.year = year;
        this.month = month;
        updateCalendar();
    }

    /**
     * Change the date considered to be the today date. This date if in the calendars view has
     * its number highlighted
     * @param year - the year of today
     * @param month - the month of today
     * @param day - the day (of the month) of today
     */
    public void setTodayDate(int year, int month, int day)
    {
        todayCal = new GregorianCalendar(year, month, day);
        updateCalendar();
    }

    /**
     * Changes the date considered to be selected by this calendar view. This date if in the calendar's
     * view has its background highlighted
     * @param year - the selected year
     * @param month - the selected month
     * @param day - the selected day (of the month)
     */
    public void setSelectedDate(int year, int month, int day)
    {
        selectedCal = new GregorianCalendar(year, month, day);
        updateCalendar();
    }

    /**
     * Change the minimum date of this calendar. Dates behind this date are grayed out
     * and disabled, not clickable.
     * @param year - the year of the min
     * @param month - the month of the min
     * @param day - the day of the min
     */
    public void setMinDate(int year, int month, int day)
    {
        minCal = new GregorianCalendar(year, month, day);
        updateCalendar();
    }

    /**
     * Change the max date of this calendar. Dates beyond this date are grayed out and
     * disabled, not clickable.
     * @param year - the year of the max
     * @param month - the month of the max
     * @param day - the day of the max
     */
    public void setMaxDate(int year, int month, int day)
    {
        maxCal = new GregorianCalendar(year, month, day);
        updateCalendar();
    }

    /**
     * Sets the date selected handler for this calendar view. This handler is called when
     * a change in the selected date occurs due to a user touch on our calendar
     * @param dateSelectedHandler - the handler to set to be notified on a new date selection
     */
    public void setDateSelectedHandler(DateSelectedHandler dateSelectedHandler)
    {
        this.dateSelectedHandler = dateSelectedHandler;
    }

    /**
     * Populates a cells grid with dates corresponding to the dates to show in the calendar
     * view. Starts by padding the first week of year, month with null dates and adds
     * single days up to when the next month starts (exclusive). Updates the title text to
     * reflect the current year, month. Invalidates and requests layout
     * for this view.
     */
    public void updateCalendar()
    {
        ArrayList<Calendar> cells = new ArrayList<>();
        Calendar calendar = new GregorianCalendar(year, month, 1);

        // determine the cell for current month's beginning
        int monthBeginningCell = calendar.get(Calendar.DAY_OF_WEEK) - 1;

        //fill in the initial days with null dates
        for (int i = 0; i < monthBeginningCell; i++)
        {
            cells.add(null);
        }

        // fill cells with days until we pass into the next month
        while (calendar.get(Calendar.MONTH) == this.month)
        {
            cells.add(new GregorianCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        // update grid
        grid.setAdapter(new CustomCalendarAdapter(getContext(), cells));

        // update title
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        txtDate.setText(format.format((new GregorianCalendar(year, month, 1)).getTime()));

        this.invalidate();
        this.requestLayout();
    }

    /**
     * Private inner class that defines our custom calendar adapter. This determines how
     * the collection of dates gets displayed in a grid.
     */
    private class CustomCalendarAdapter extends ArrayAdapter<Calendar>
    {
        // for view inflation
        private LayoutInflater inflater;

        /**
         * Constructor for CustomCalendarAdapter.
         * @param context - the context of the calendar this adapter is attached to
         * @param days - the days of the calendar this adapter is backing
         */
        public CustomCalendarAdapter(Context context, ArrayList<Calendar> days)
        {
            super(context, R.layout.custom_calendar_day_view, days);
            inflater = LayoutInflater.from(context);
        }

        /**
         * Get view method for this adapter. Gets the day view for the cell at the given position.
         * Sets the necessary attributes taking into account today's date, the selected date,
         * the min and max dates of the attached enclosing CustomCalendarView.
         * @param position - the position to return a view for. Corresponds to the cell in our 7 column
         *                 grid.
         * @param convertView - a potential view to reuse. A new view is instantiated if this view
         *                    isn't appropriate/available for use.
         * @param parent - the parent view group to get this cell layout params for
         * @return the view that should be shown at position
         */
        public View getView(int position, View convertView, ViewGroup parent)
        {
            Calendar cellCalendar = getItem(position);
            TextView dayView;

            if (convertView == null)
            {
                dayView = (TextView)inflater.inflate(R.layout.custom_calendar_day_view, parent, false);
            }
            else
            {
                dayView = (TextView)convertView;
            }



            //A null date at a cell indicates that, that cell should only be considered padding
            //in our grid and will not show an actual date.
            if (cellCalendar == null)
            {
                dayView.setVisibility(View.INVISIBLE);
            }
            else
            {
                int cellDay = cellCalendar.get(Calendar.DAY_OF_MONTH);
                int cellMonth = cellCalendar.get(Calendar.MONTH);
                int cellYear = cellCalendar.get(Calendar.YEAR);

                dayView.setVisibility(View.VISIBLE);
                dayView.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.transparent));
                dayView.setTypeface(null, Typeface.NORMAL);
                dayView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.primary_text_light));

                boolean cellToday = todayCal != null && (cellDay == todayCal.get(Calendar.DAY_OF_MONTH) && cellMonth == todayCal.get(Calendar.MONTH) && cellYear == todayCal.get(Calendar.YEAR));
                boolean cellSelected =  CustomCalendarView.this.selectedCal != null &&
                        (cellDay == CustomCalendarView.this.selectedCal.get(Calendar.DAY_OF_MONTH) &&
                                cellMonth == CustomCalendarView.this.selectedCal.get(Calendar.MONTH) &&
                                cellYear == CustomCalendarView.this.selectedCal.get(Calendar.YEAR));
                boolean cellBehindMin = CustomCalendarView.this.minCal != null && (Prediction.numDaysBetween(CustomCalendarView.this.minCal.getTime(), cellCalendar.getTime()) < 0);
                boolean cellAheadMax = CustomCalendarView.this.maxCal != null && (Prediction.numDaysBetween(CustomCalendarView.this.maxCal.getTime(), cellCalendar.getTime()) > 0);

                //Highlight today's date
                if (cellToday && !cellSelected)
                {
                    dayView.setTypeface(null, Typeface.BOLD);
                    dayView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                }

                //Darken background for selected day
                if (cellSelected) {
                    dayView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                }

                if (cellBehindMin || cellAheadMax)
                {
                    dayView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));
                }

                // set text
                dayView.setText(Integer.toString(cellDay));
            }
            return dayView;
        }
    }
}
