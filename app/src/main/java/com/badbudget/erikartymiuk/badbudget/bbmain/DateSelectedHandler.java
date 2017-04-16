package com.badbudget.erikartymiuk.badbudget.bbmain;

/**
 * Date selected handler interface. Defines a method of what to do when a date
 * is selected from a calendar.
 * Created by Erik Artymiuk on 12/24/2016.
 */
public interface DateSelectedHandler
{
    void onDateSelected(int year, int month, int day);
}
