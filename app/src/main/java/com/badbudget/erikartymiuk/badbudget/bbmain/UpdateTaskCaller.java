package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.app.Activity;

/**
 * Interface to be implemented by a caller of the UpdateTask. This method is the callback after the
 * update background task completes.
 * Created by Erik Artymiuk on 10/7/2016.
 */
public interface UpdateTaskCaller  {
    /**
     * Called after updateTask background task completes. Indicates if the bad budget data was updated
     * or not.
     * @param updated - true if an update to bb objects occurred false otherwise.
     */
    void updateTaskCompleted(boolean updated);
}
