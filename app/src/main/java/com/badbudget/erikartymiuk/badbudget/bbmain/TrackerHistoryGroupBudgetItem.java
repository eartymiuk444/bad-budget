package com.badbudget.erikartymiuk.badbudget.bbmain;

/**
 * Class representing a single row (but multiple transactions) in a time period grouping
 * Created by eartymiuk on 4/29/2017.
 */
public class TrackerHistoryGroupBudgetItem {

    private String budgetItemGroupDescription;
    private String userTransactionGroupDescription;

    private double userResetLoss;
    private double autoResetLoss;

    private double changeAmount;

    /**
     * Constructor for a TrackerHistoryGroupBudgetItem. Initializes user, auto reset, and change amount
     * to 0.
     * @param budgetItemGroupDescription - the budget item description that this group represents
     * @param userTransactionGroupDescription - the user transaction description that this group represents
     *                                              (optional, indicate null or the empty string to ignore)
     *                                              if non-null/empty this trumps the budgetItemGroupDescription
     */
    public TrackerHistoryGroupBudgetItem(String budgetItemGroupDescription, String userTransactionGroupDescription)
    {
        this.budgetItemGroupDescription = budgetItemGroupDescription;
        this.userTransactionGroupDescription = userTransactionGroupDescription;

        this.userResetLoss = 0;
        this.autoResetLoss = 0;

        this.changeAmount = 0;
    }

    /**
     * Adds the passed amount to the user reset loss amount
     * @param amount - the amount to add
     */
    public void addUserResetLoss(double amount)
    {
        this.userResetLoss+=amount;
    }

    /**
     * Adds the passed amount to the auto reset loss amount
     * @param amount - the amount to add
     */
    public void addAutoResetLoss(double amount)
    {
        this.autoResetLoss+=amount;
    }

    /**
     * Subtracts the passed amount from the change amount for this budget item group
     * @param amount - the amount to subtract
     */
    public void subtractAmount(double amount)
    {
        this.changeAmount-=amount;
    }

    /**
     * Adds the passed amount to the change amount for this budget item group
     * @param amount - the amount to add
     */
    public void addAmount(double amount)
    {
        this.changeAmount+=amount;
    }

    /* Getters */
    public String getBudgetItemGroupDescription()
    {
        return this.budgetItemGroupDescription;
    }

    public String getUserTransactionGroupDescription()
    {
        return this.userTransactionGroupDescription;
    }

    public double getChangeAmount()
    {
        return this.changeAmount;
    }

    public double getAutoResetLoss()
    {
        return this.autoResetLoss;
    }

    public double getUserResetLoss()
    {
        return this.userResetLoss;
    }
}
