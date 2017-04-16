package com.badbudget.erikartymiuk.badbudget.bbmain;

import java.util.Date;

/**
 * Interface a class should implement if it is going to envoke a predictTask.
 * Created by Erik Artymiuk on 11/10/2016.
 */
public interface PredictTaskCaller {

    /**
     * Callback method after the PredictTask envoked has completed.
     * @param endDate - the new end date
     */
    public void predictionSet(Date endDate);

}
