package com.badbudget.erikartymiuk.badbudget.inputforms;

import java.util.Calendar;

/**
 * Class representing the day of the week. Su - Mon. Should match specs given
 * in java's utility Calendar class
 * Created by Erik Artymiuk on 8/4/2016.
 */
public class DayOfWeek {

    private int dayOfWeek;

    /**
     * Constructor. Day of week should be set using Clendar classes constant fields,
     * i.e Calendar.SUNDAY
     * @param dayOfWeek
     */
    public DayOfWeek(int dayOfWeek)
    {
        this.dayOfWeek = dayOfWeek;
    }

    /**
     * Returns the string representation of this day of the week.
     * @return - string representing this day of the week
     */
    public String toString()
    {
        switch (dayOfWeek)
        {
            case Calendar.SUNDAY:
            {
                return  "Sunday";
            }
            case Calendar.MONDAY:
            {
                return "Monday";
            }
            case Calendar.TUESDAY:
            {
                return "Tuesday";
            }
            case Calendar.WEDNESDAY:
            {
                return "Wednesday";
            }
            case Calendar.THURSDAY:
            {
                return "Thursday";
            }
            case Calendar.FRIDAY:
            {
                return "Friday";
            }
            case Calendar.SATURDAY:
            {
                return "Saturday";
            }
            default:
            {
                return "";
            }
        }
    }

    /**
     * Returns the integer representation of this day of the week, given by the Calendar java utility
     * class.
     * @return - integer representation for this day of the week.
     */
    public int getDayOfWeek()
    {
        return this.dayOfWeek;
    }
}
