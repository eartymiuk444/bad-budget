package com.badbudget.erikartymiuk.badbudget.inputforms;

/**
 * Interface for caller of add, edit, and delete bb oject tasks. These are the callback methods
 * that are called when the various tasks complete
 *
 * Created by Erik Artymiuk on 7/27/2016.
 */
public interface BBOperationTaskCaller
{

    /**
     * Callback method called on completion of the AddBBObjectTask task method.
     */
    public void addBBObjectFinished();

    /**
     * Callback method called on completion of the EditBBObjectTask task method.
     */
    public void editBBObjectFinished();

    /**
     * Callback method called on completion of the DeleteBBObjectTask task method.
     */
    public void deleteBBObjectFinished();

}
