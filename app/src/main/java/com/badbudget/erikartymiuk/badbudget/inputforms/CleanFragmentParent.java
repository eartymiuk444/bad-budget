package com.badbudget.erikartymiuk.badbudget.inputforms;

/**
 * Interface to implement when an activity wishes to use the Clean Fragment. The only method (cleanSelection)
 * is a callback method indicating what position (0-3) a user chose from the list presented to them.
 *
 * Created by Erik Artymiuk on 7/20/2016.
 */
public interface CleanFragmentParent
{
    /**
     * Callback method called when a user clicks the clean button in a Clean Fragment. It passes
     * to the caller the position the user chose.
     * @param selectionPosition - the position chosen that indicates which item the user wants cleaned
     */
    public void cleanSelection(int selectionPosition);
}
