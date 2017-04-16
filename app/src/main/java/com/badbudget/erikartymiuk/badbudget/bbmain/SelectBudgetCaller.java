package com.badbudget.erikartymiuk.badbudget.bbmain;

/**
 * Any activity that is capable of selecting a budget should implement this interface to recieve
 * a callback after selection of the new budget is complete.
 * Created by Erik Artymiuk on 12/3/2016.
 */
public interface SelectBudgetCaller {

    /**
     * Take the appropriate action upon completion of a budget selection.
     */
    public void budgetSelected();

}
