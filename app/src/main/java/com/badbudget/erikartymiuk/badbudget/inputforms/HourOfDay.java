package com.badbudget.erikartymiuk.badbudget.inputforms;

/**
 * Class to represent the hours of a 24 hour day. Valid hours are 0, 1, 2, 3... 23
 * Hours below 12 are AM and hours above are PM. Useful for getting a typical string
 * representation of a particular hour of the day.
 * Created by Erik Artymiuk on 8/4/2016.
 */
public class HourOfDay
{
    private int hourOfDay;

    /**
     * Constructor. Valid hours are 0,1,2,...23
     * @param hourOfDay - the hour of the day
     */
    public HourOfDay(int hourOfDay)
    {
        this.hourOfDay = hourOfDay;
    }

    /**
     * Returns the string representation of this hour of day in the format HH:00 (AM/PM).
     * @return
     */
    public String toString()
    {
        if (hourOfDay == 0)
        {
            return "12:00 AM";
        }
        else if (hourOfDay == 12)
        {
            return "12:00 PM";
        }
        else if (hourOfDay < 12 && hourOfDay > 0)
        {
            return "" + hourOfDay + ":00 AM";
        }
        else if (hourOfDay > 12 && hourOfDay < 24)
        {
            return "" + hourOfDay%12 + ":00 PM";
        }
        else
        {
            return "";
        }
    }

    /**
     * Get the hour of the day as an integer. Values are 0, 1, 2,...23 with 0 for 12:00AM and then
     * goes up by an hour every increase in 1.
     * @return - the hour of the day as an integer
     */
    public int getHourOfDay()
    {
        return this.hourOfDay;
    }
}
