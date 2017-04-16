package com.badbudget.erikartymiuk.badbudget.viewobjecttables;

import android.app.Activity;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.widget.CheckBox;

import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseContract;
import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseOpenHelper;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetBaseActivity;

/**
 * Invoke this task to update the quicklook field in the database for a given row in a given table.
 * Disables the passed checkbox as the task executes and re-enables the checkbox when finished.
 * Created by Erik Artymiuk on 8/10/2016.
 */
public class QuicklookToggleTask extends AsyncTask<Void, Void, Void> {

    private BadBudgetBaseActivity caller;
    private CheckBox checkbox;
    private ContentValues updateQuicklookValue;
    private String tableName;
    private String identifierColumn;
    private String quicklookColumn;
    private String identifier;

    /**
     * Constructor for a quicklook task
     * @param caller -  the activity calling this task
     * @param tableName - the tablename the row we are updating is in
     * @param identifierColumn - the column name for the identifying value
     * @param quicklookColumn - the column name for the quicklook field
     * @param checkBox - the checkbox that was toggled that is disabled and renabled over the task run
     * @param identifier - the identifying value for the row we are updating
     */
    public QuicklookToggleTask(BadBudgetBaseActivity caller,
                               String tableName, String identifierColumn, String quicklookColumn,
                               CheckBox checkBox, String identifier)
    {
        this.caller = caller;
        this.tableName = tableName;
        this.identifierColumn = identifierColumn;
        this.quicklookColumn = quicklookColumn;
        this.checkbox = checkBox;
        this.identifier = identifier;
    }

    /**
     * Background thread run off ui thread that runs the update operation on the database to update
     * the quicklook field.
     * @param params - unused
     * @return - unused
     */
    protected Void doInBackground(Void... params) {

        String strFilter = identifierColumn + "=?";

        BBDatabaseOpenHelper dbHelper = BBDatabaseOpenHelper.getInstance(caller);
        SQLiteDatabase writableDB = dbHelper.getWritableDatabase();
        writableDB.update(tableName, updateQuicklookValue, strFilter, new String[]{identifier});
        return null;
    }

    /**
     * Called prior to execution of the background task and is run on the ui thread. Disables the
     * checkbox and gets the current checked value of the passed checkbox to put into the database.
     */
    protected void onPreExecute() {
        super.onPreExecute();
        checkbox.setEnabled(false);

        this.updateQuicklookValue = new ContentValues();
        updateQuicklookValue.put(quicklookColumn, BBDatabaseContract.dbBooleanToInteger(this.checkbox.isChecked()));
    }

    /**
     * Called after completion of the background task on the ui thread. Reenables the checkbox.
     * @param aVoid
     */
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        checkbox.setEnabled(true);
    }
}
