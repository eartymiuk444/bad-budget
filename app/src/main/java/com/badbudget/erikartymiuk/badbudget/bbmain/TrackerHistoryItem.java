package com.badbudget.erikartymiuk.badbudget.bbmain;

import com.badbudget.erikartymiuk.badbudget.R;

/**
 * Class representing a tracker history item
 * Created by Erik Artymiuk on 10/29/2016.
 */
public class TrackerHistoryItem {

    public static final String ACTION_SUBTRACT_STRING = "Subtracted";
    public static final String ACTION_ADD_STRING = "Added";
    public static final String ACTION_SET_DECREASE_STRING = "Decreased";
    public static final String ACTION_SET_INCREASE_STRING = "Increased";
    public static final String ACTION_USER_RESET_STRING = "User Reset";
    public static final String ACTION_AUTO_RESET_STRING = "Auto Reset";

    /**
     * Potential actions that could have caused an entry to be made in the tracker's history
     * subtract - user uses plus/minus buttons and navigates away from that budget item, leaving
     *              it as less than it was or user subtracts using the calculator and confirms
     * add - users uses plus/minus buttons and navigates away from that budget item, leaving
     *          it as more than it was or user adds using the calculator and confirms
     * setIncrease - user set the value from the tracker item page to a new higher
     * setDecrease - user set the value from the tracker item page to a new lower value
     * userReset - user resets the budget item to its full value from the calculator
     * autoReset - the budget value is reset during the update method
     */
    public enum TrackerAction {
        subtract,
        add,
        setIncrease,
        setDecrease,
        userReset,
        autoReset
    }

    private String budgetItemDescription;
    private String userTransactionDescription;

    private String dateString;
    private String dayString;
    private String timeString;

    private TrackerAction action;

    private double actionAmount;
    private double originalBudgetAmount;
    private double updatedBudgetAmount;

    /**
     * Constructor for a TrackerHistoryItem
     * @param budgetItemDescription - the description of the budget that was changed
     * @param userTransactionDescription - an optional user input describing this history item
     * @param dateString - the date (as a string) the transaction occurred
     * @param dayString - the day of the week (as a string) the transaction occurred
     * @param timeString - the time (as a string) the transaction occurred
     * @param action - the TrackerAction to be associated with this budget item
     * @param actionAmount - the amount the value changed due to the action that occurred
     * @param originalBudgetAmount - the original amount of the budget before the action occurred
     * @param updatedBudgetAmount - the updated amount of the budget after the action occurs
     */
    public TrackerHistoryItem(String budgetItemDescription, String userTransactionDescription,
                              String dateString, String dayString, String timeString, TrackerAction action, double actionAmount,
                              double originalBudgetAmount, double updatedBudgetAmount)
    {
        this.budgetItemDescription = budgetItemDescription;
        this.userTransactionDescription = userTransactionDescription;
        this.dateString = dateString;
        this.dayString = dayString;
        this.timeString = timeString;
        this.action = action;
        this.actionAmount = actionAmount;
        this.originalBudgetAmount = originalBudgetAmount;
        this.updatedBudgetAmount = updatedBudgetAmount;
    }

    /*
    Getters
     */
    public String getBudgetItemDescription() {
        return budgetItemDescription;
    }

    public double getUpdatedBudgetAmount() {
        return updatedBudgetAmount;
    }

    public String getUserTransactionDescription() {
        return userTransactionDescription;
    }

    public String getDateString() {
        return dateString;
    }

    public String getTimeString() {
        return timeString;
    }

    public TrackerAction getAction() {
        return action;
    }

    public double getActionAmount() {
        return actionAmount;
    }

    public double getOriginalBudgetAmount() {
        return originalBudgetAmount;
    }

    public String getDayString() { return dayString; }

    /**
     * Converts the passed action into its equivalent string representation
     * @param action - the action to convert
     * @return - the converted string
     */
    public static String convertTrackerActionToString(TrackerAction action)
    {
        switch (action)
        {
            case subtract:
            {
                return ACTION_SUBTRACT_STRING;
            }
            case add:
            {
                return ACTION_ADD_STRING;
            }
            case setDecrease:
            {
                return ACTION_SET_DECREASE_STRING;
            }
            case setIncrease:
            {
                return ACTION_SET_INCREASE_STRING;
            }
            case userReset:
            {
                return ACTION_USER_RESET_STRING;
            }
            case autoReset:
            {
                return ACTION_AUTO_RESET_STRING;
            }
        }
        return null;
    }
}
