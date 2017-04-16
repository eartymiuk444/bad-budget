package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.app.Application;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.TableRow;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.BuildConfig;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.bbmain.FlavorSpecific;
import com.erikartymiuk.badbudgetlogic.budget.RemainAmountAction;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.Frequency;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Class extends the base application class adding in global access to our bad budget data object
 * which can be accessed across activites.
 *
 * Created by Erik Artymiuk on 6/15/2016.
 */
public class BadBudgetApplication extends Application
{
    /* String constants available application wide */

    /** Sample values for when bad budget is first opened */
    public static final String SAMPLE_BUDGET_ACCOUNT_DESCR = "S-Account";
    public static final double SAMPLE_BUDGET_ACCOUNT = 0;
    public static final boolean SAMPLE_BUDGET_ACCOUNT_QUICKLOOK = true;

    public static final String SAMPLE_BUDGET_GAIN_DESCR = "S-Gain";
    public static final double SAMPLE_BUDGET_GAIN = 900;
    public static final Frequency SAMPLE_BUDGET_GAIN_FREQ = Frequency.biWeekly;

    /* Sample Savings Accounts */
    public static final String[] SAMPLE_BUDGET_SAVINGS_DESCR = {
            "S-Emergency Fund", "S-Retirement"
    };

    public static final double[] SAMPLE_BUDGET_SAVINGS_VALUES = {
            0, 0
    };

    public static final double[] SAMPLE_BUDGET_SAVINGS_INTEREST = {
            0, 0.05
    };

    public static final boolean[] SAMPLE_BUDGET_SAVINGS_QUICKLOOK = {
            true, true
    };

    public static final double[] SAMPLE_BUDGET_SAVINGS_GOAL = {
            -1, -1
    };

    public static final double[] SAMPLE_BUDGET_SAVINGS_CONTRI= {
            50, 50
    };

    public static final Frequency[] SAMPLE_BUDGET_SAVINGS_FREQ = {
            Frequency.monthly, Frequency.monthly
    };

    /* Sample Budget Losses */
    public static final String[] SAMPLE_BUDGET_LOSSES_DESCR = {
            "S-Gym", "S-Internet", "S-Phone", "S-Rent",
            "S-TV", "S-Utilities", "S-Health Insurance",
            "S-Car Insurance",
    };

    public static final double[] SAMPLE_BUDGET_LOSSES_VALUES = {
            40, 35, 35, 500,
            35, 40, 400,
            700,
    };

    public static final Frequency[] SAMPLE_BUDGET_LOSSES_FREQUENCIES = {
            Frequency.monthly, Frequency.monthly, Frequency.monthly, Frequency.monthly,
            Frequency.monthly, Frequency.monthly, Frequency.monthly,
            Frequency.yearly
    };

    /* Sample Budget Items */
    public static final String[] SAMPLE_BUDGET_ITEMS_DESCR = {
            "S-Snack", "S-Entertainment", "S-Grocery", "S-Gas",
            "S-Necessity", "S-Cushion",
    };
    public static final double[] SAMPLE_BUDGET_ITEMS_VALUES = {
            5, 25, 40, 50,
            40, 75,
    };
    public static final Frequency[] SAMPLE_BUDGET_ITEMS_FREQUENCIES = {
            Frequency.daily, Frequency.weekly, Frequency.weekly, Frequency.biWeekly,
            Frequency.biWeekly, Frequency.monthly
    };
    public static final double[] SAMPLE_BUDGET_ITEMS_PLUS_MINUS = {
            0.25, 1, 1, 1,
            1, 1
    };

    /* Request code for the desired response from our form */
    public static final int FORM_RESULT_REQUEST = 1;

    /* Return codes to return to the starting table activity indicating what action was taken by the
    * corresponding form */
    public static final int FORM_RESULT_ADD = 1;
    public static final int FORM_RESULT_EDIT = 2;
    public static final int FORM_RESULT_DELETE = 3;

    /* Add Activity keys for data passed to Add Savings Account Activity*/
    public static String INPUT_ACCOUNT_NAME_KEY = "INPUT_ACCOUNT_NAME_KEY";
    public static String INPUT_ACCOUNT_AMOUNT_KEY = "INPUT_ACCOUNT_AMOUNT_KEY";
    public static String INPUT_ACCOUNT_QUICKLOOK_KEY = "INPUT_ACCOUNT_QUICKLOOK_KEY";

    /* Keys for data passed to the Add Payments activity when the user wants to setup a payment right away when
    adding a debt */
    public static String INPUT_DEBT_NAME_KEY = "INPUT_DEBT_NAME_KEY";
    public static String INPUT_DEBT_AMOUNT_KEY = "INPUT_DEBT_AMOUNT_KEY";
    public static String INPUT_DEBT_INTEREST_RATE_KEY = "INPUT_DEBT_INTEREST_RATE";
    public static String INPUT_DEBT_QUICKLOOK_KEY = "INPUT_DEBT_QUICKLOOK_KEY";

    public static String INPUT_LOAN_PRINCIPAL_KEY = "INPUT_LOAN_PRINCIPAL_KEY";
    public static String INPUT_LOAN_SIMPLE_INTEREST_KEY = "INPUT_LOAN_SIMPLE_INTEREST_KEY";

    /* Keys for letting the add forms know if they should behave as edit forms
    * and also keys for what is being edited */
    public static String EDIT_KEY = "EDIT_KEY";
    public static String EDIT_OBJECT_ID_KEY = "EDIT_OBJECT_ID_KEY";

    /* Key for indicating to the add payment activity if we are editing a debt with an existing payment */
    public static String EDIT_PAYMENT_KEY = "EDIT_PAYMENT_KEY";

    /* Key indicating if the add savings form should return to the generic cash tables on cancel */
    public static String GENERIC_ACCOUNTS_RETURN_KEY = "GENERIC_ACCOUNTS_RETURN_KEY";

    /* Keys for Prediction Data */
    public static String PREDICT_CURRENT_CHOSEN_DAY = "PREDICT_CURRENT_CHOSEN_DAY";
    public static String PREDICT_CURRENT_END_DATE = "PREDICT_CURRENT_END_DATE";
    public static int PREDICT_REQUEST_CODE_UPDATED_DATES = 0;
    public static String PREDICT_ACCOUNT_ID = "PREDICT_ACCOUNT_ID";

    /* Key for Detailed Look BBD Updated */
    public static String DETAILED_LOOK_BBD_UPDATED = "DETAILED_LOOK_BBD_UPDATED";

    /* Message for progress dialog if an update is occurring on activity resume */
    public static final String progressDialogMessage = "Checking for date change...";

    /* Auto reset time string for tracker history item */
    public static final String AUTO_RESET_TRACKER_HISTORY_TIME = "";
    public static final String TRACKER_DAY_FORMAT = "EE";
    public static final String TRACKER_TIME_FORMAT = "hh:mm a";
    public static final String TRACKER_DATE_FORMAT = "MM-dd-yyyy";

    /* Positions of the frequencies in our various frequency arrays
    * TODO Clean up this, better implementation */
    public static final int ONE_TIME_INDEX = 0;
    public static final int DAILY_INDEX = 1;
    public static final int WEEKLY_INDEX = 2;
    public static final int BIWEEKLY_INDEX = 3;
    public static final int MONTHLY_INDEX = 4;
    public static final int YEARLY_INDEX = 5;

    /* String to append to a frequency amount that is only temporary */
    public static final String TEMP_FREQUENCY_AMOUNT_MESSAGE = "(TOG)";

    /* Saved state key prefix for budget items toggled frequencies */
    public static final String TOGGLED_FREQUENCY_PREFIX_KEY = "TOGGLED_FREQUENCY_";

    /* Default frequency for totals in all tables and budget in the loss table */
    public final static Frequency DEFAULT_TOTAL_FREQUENCY = Frequency.monthly;

    /* Saved state key for total freq */
    public static final String TOTAL_FREQ_KEY = "TOTAL_FREQ";

    /* Saved state key for budget total freq in loss table */
    public static final String BUDGET_TOTAL_FREQ_KEY = "BUDGET_TOTAL_FREQ";

    /* Application Global Variables */
    private int selectedBudgetId = -1;   //Currently selected budget id
    private Map<Integer, String> budgetMapIDToName;

    private BadBudgetData badBudgetUserData;
    private boolean autoUpdateSelectedBudget;
    private RemainAmountAction remainAmountActionSelectedBudget;
    private List<TrackerHistoryItem> trackerHistoryItems;

    private boolean predictBBDUpdated;

    /* The date when the application first started up. This is the date that should be used
    as today's date throughout the application. */
    private Date today;

    /* Overridden on create method for our application. Calls the super on create then gets the current
    date to be used as todays date throughout the application */
    public void onCreate()
    {
        super.onCreate();

        today = Calendar.getInstance().getTime();

        if (BuildConfig.FREE_VERSION)
        {
            FlavorSpecific.enableAds(this);
        }
    }

    /**
     * Returns the currently set today date.
     * @return - the application's today date
     */
    public Date getToday()
    {
        return this.today;
    }

    /**
     * Set app's today date
     * @param today - the date to set today to
     */
    public void setToday(Date today)
    {
        this.today = today;
    }

    /**
     * Gets the auto update status of the currently selected budget.
     * @return auto update flag of the current budget selected
     */
    public boolean getAutoUpdateSelectedBudget()
    {
        return this.autoUpdateSelectedBudget;
    }

    /**
     * Sets the auto update flag of the currently selected budget in memory.
     * @param autoAupdateSelectedBudget - the auto update status to set for the currently selected budget
     */
    public void setAutoUpdateSelectedBudget(boolean autoAupdateSelectedBudget)
    {
        this.autoUpdateSelectedBudget = autoAupdateSelectedBudget;
    }

    /**
     * Gets the remain amount action of the currently selected budget.
     * @return - the remain action of the currently selected budget
     */
    public RemainAmountAction getRemainAmountActionSelectedBudget()
    {
        return this.remainAmountActionSelectedBudget;
    }

    /**
     * Sets the remain amount action in memory for the currently selected budget
     * @param remainAmountActionSelectedBudget - the remain action to set as the remain action for the currently selected budget
     */
    public void setRemainAmountActionSelectedBudget(RemainAmountAction remainAmountActionSelectedBudget)
    {
        this.remainAmountActionSelectedBudget = remainAmountActionSelectedBudget;
    }

    /**
     * Get the current status of if the prediction activity needs to run a new prediction as the
     * bad budget data was updated.
     * @return - true if the predict activity has an unhandled bad budget data update, false if not
     */
    public boolean getPredictBBDUpdated()
    {
        return this.predictBBDUpdated;
    }

    /**
     * Sets the status of whether an update was handled or needs to be handled yet by the predict
     * activity. Should be set when a change to the bad budget data occurs and not set when the
     * predict activity runs a prediction and has the most recent predict data for the current
     * bad budget data.
     * @param predictBBDUpdated - true if a change to the bad budget data occurred and thus the
     *                          predict activity must invalidate any current prediction data. false
     *                          when the predict activity runs a current prediction.
     */
    public void setPredictBBDUpdated(boolean predictBBDUpdated)
    {
        this.predictBBDUpdated = predictBBDUpdated;
    }

    /**
     * Get the applications currently set budget id
     * @return the current id of the set budget
     */
    public int getSelectedBudgetId()
    {
        return this.selectedBudgetId;
    }

    /**
     * Updates the selected budget id to the passed id
     * @param selectedBudgetId - the new id of the budget that was selected
     */
    public void setSelectedBudgetId(int selectedBudgetId)
    {
        this.selectedBudgetId = selectedBudgetId;
    }

    /**
     * Get the application tracker history for use across activities
     *
     * @return the user's tracker history
     */
    public List<TrackerHistoryItem> getTrackerHistoryItems()
    {
        return this.trackerHistoryItems;
    }

    /**
     * Sets the application's tracker history list
     * @param trackerHistoryItems - the list of tracker history items to use as the app's tracker history
     */
    public void setTrackerHistoryItems(List<TrackerHistoryItem> trackerHistoryItems)
    {
        this.trackerHistoryItems = trackerHistoryItems;
    }

    /**
     * Get the application bad budget data for use across activities
     *
     * @return the user's bad budget data
     */
    public BadBudgetData getBadBudgetUserData()
    {
        return this.badBudgetUserData;
    }

    /**
     * Set the global bad budget data
     * @param bbd - the bad budget data to set as the user data
     */
    public void setBadBudgetUserData(BadBudgetData bbd)
    {
        this.badBudgetUserData = bbd;
    }

    /**
     * Get the application map of budget ids to budget names
     * @return - a map of the current budget ids to budget names
     */
    public Map<Integer, String> getBudgetMapIDToName()
    {
        return this.budgetMapIDToName;
    }

    /**
     * Set the current application map of budget ids to budget names
     * @param budgetMapIDToName - the map to set as the application current map of budget ids to names
     */
    public void setBudgetMapIDToName(Map<Integer, String> budgetMapIDToName)
    {
        this.budgetMapIDToName = budgetMapIDToName;
    }

    /**
     * Converts the passed in date to a simple string in the format MM-dd-yyyy,
     * if date is null returns the empty string
     * @param date - the date to return a string representation of
     * @return the string representation of the date, or the empty string if the
     *          date is null
     */
    public static String dateString(Date date)
    {
        if (date == null)
        {
            return "";
        }
        else {
            SimpleDateFormat format = new SimpleDateFormat(TRACKER_DATE_FORMAT);
            return format.format(date);
        }
    }

    /**
     * Sets up the text cell view params for use in a BB table row. Background resource id
     * must be set correctly depending on the intended position in the row/table.
     * @param textView - the text view that will end up in our table.
     * @param text - the text to be set in the text view
     * @param backgroundResourceId - the resource id that will depend on the position of the cell in the row,
     *                                  and whether the row is empty or the total row.
     */
    public void initializeTableCell(TextView textView, String text, int backgroundResourceId)
    {
        int layoutWidth = 0;
        int layoutHeight = (int) this.getResources().getDimension(R.dimen.cell_height);
        float weight = 1.0f;
        TextUtils.TruncateAt ellipsize = TextUtils.TruncateAt.END;
        int textSize = (int) this.getResources().getDimension(R.dimen.cell_text_size);
        int padding = (int) this.getResources().getDimension(R.dimen.table_padding);
        int gravity = Gravity.CENTER;

        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(layoutWidth, layoutHeight);
        layoutParams.weight = weight;
        textView.setLayoutParams(layoutParams);
        textView.setSingleLine();
        textView.setEllipsize(ellipsize);
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        textView.setPadding(padding, padding, padding, padding);
        textView.setBackgroundResource(backgroundResourceId);
        textView.setGravity(gravity);
    }

    /**
     * Method to set the text view params in any of our bbd tables. Doesn't include a cell with
     * the quicklook checkbox. (See tableQLCellSetLayoutParams)
     * @param textView - the textView to set the layout params and other attrs for
     * @param text - the text for this text view
     */
    public void tableCellSetLayoutParams(TextView textView, String text)
    {
        int layoutWidth = 0;
        int layoutHeight = (int) this.getResources().getDimension(R.dimen.cell_height);
        float weight = 1.0f;
        int maxLines = 1;
        TextUtils.TruncateAt ellipsize = TextUtils.TruncateAt.END;
        int textSize = (int) this.getResources().getDimension(R.dimen.cell_text_size);
        int padding = (int) this.getResources().getDimension(R.dimen.table_padding);
        int resid = R.drawable.borderlb;
        int gravity = Gravity.CENTER;

        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(layoutWidth, layoutHeight);
        layoutParams.weight = weight;
        textView.setLayoutParams(layoutParams);
        textView.setSingleLine();
        //textView.setMaxLines(maxLines);
        textView.setEllipsize(ellipsize);
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        textView.setPadding(padding, padding, padding, padding);
        textView.setBackgroundResource(resid);
        textView.setGravity(gravity);
    }

    /**
     * Adjusts the given text view border so that it fits in as the last column of a table.
     * @param textView - the view to adjust the border of
     */
    public void tableAdjustBorderForLastColumn(TextView textView)
    {
        int resid = R.drawable.borderlbr;
        textView.setBackgroundResource(resid);
    }

    /**
     * Method used to set the layout params of the quicklook cell in any of our tables
     * This cell contains a checkbox that the user can toggle to enable/disable
     * the quicklook attribute of an bbd object.
     * @param checkBox - the checkbox to set the layout for
     * @param checked - whether or not this checkbox should start out as checked or unchecked.
     */
    public void tableQLCellSetLayoutParams(CheckBox checkBox, boolean checked)
    {
        int layoutWidth = 0;
        int layoutHeight = (int) this.getResources().getDimension(R.dimen.cell_height);
        float weight = 1.0f;
        int maxLines = 1;
        TextUtils.TruncateAt ellipsize = TextUtils.TruncateAt.END;
        int textSize = (int) this.getResources().getDimension(R.dimen.cell_text_size);
        int padding = (int) this.getResources().getDimension(R.dimen.table_padding);
        int resid = R.drawable.borderlbr;
        int gravity = Gravity.CENTER;

        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(layoutWidth, layoutHeight);
        layoutParams.weight = weight;
        checkBox.setLayoutParams(layoutParams);
        checkBox.setSingleLine();
        //checkBox.setMaxLines(maxLines);
        checkBox.setEllipsize(ellipsize);
        checkBox.setChecked(checked);
        checkBox.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        checkBox.setPadding(padding, padding, padding, padding);
        checkBox.setBackgroundResource(resid);
        //checkBox.setGravity(gravity);
    }

    /**
     * Undoes the shortHandFreq operation method defined in BadBudgetApplication class. Takes in
     * a string representing a freq as a shorthand and returns the frequency constant it represents
     *
     * @param shortHandFreq - the short hand string freq to convert
     * @return - the Frequency the short hand string represented
     */
    public static Frequency freqFromShortHand(String shortHandFreq)
    {
        if (shortHandFreq.equals("OT"))
        {
            return Frequency.oneTime;
        }
        else if (shortHandFreq.equals("D"))
        {
            return Frequency.daily;
        }
        else if (shortHandFreq.equals("W"))
        {
            return Frequency.weekly;
        }
        else if (shortHandFreq.equals("BW"))
        {
            return Frequency.biWeekly;
        }
        else if (shortHandFreq.equals("M"))
        {
            return Frequency.monthly;
        }
        else if (shortHandFreq.equals("Y"))
        {
            return Frequency.yearly;
        }
        else
        {
            return null;
        }
    }

    /**
     * Converts the passed frequency into a shorthand form to be used in the payment column of
     * a debts table.
     * @param freq - the frequency to convert
     * @return - a short hand string representation of the passed frequency
     */
    public static String shortHandFreq(Frequency freq)
    {
        switch (freq)
        {
            case oneTime:
            {
                return "OT";
            }
            case daily:
            {
                return "D";
            }
            case weekly:
            {
                return "W";
            }
            case biWeekly:
            {
                return "BW";
            }
            case monthly:
            {
                return "M";
            }
            case yearly:
            {
                return "Y";
            }
            default:
            {
                return "?";
            }
        }
    }

    /**
     * Utility method to round/format any double in the bad budget application
     * to 2 decimal places, converted into a string
     * @param toRound - the double value to round and convert into a string
     * @return - the rounded double as a string to two decimal places
     */
    public static String roundedDoubleBB(double toRound)
    {
        DecimalFormat df = new DecimalFormat("###.##");
        return df.format(toRound);
    }

    /**
     * Static utility helper method that appends the items in a list to an existing list.
     * @param fullList - list to append the second list of items to
     * @param itemsToAppend - a list of items to append to the first list.
     */
    public static <T> void appendItems(List<T> fullList, List<T> itemsToAppend)
    {
        for (T item : itemsToAppend)
        {
            fullList.add(item);
        }
    }

    /**
     * Utility method to order the toggled frequencies for the toggle column of our tables.
     * The order is defined as the circular list daily->weekly->biweekly->monthly->yearly
     * (Note a OT freq returns OT and any unknown freq returns itself)
     * @param currentFreq - the frequency to move past in our list
     * @return - the next frequency following current freq in the defined ciruclar list
     */
    public static Frequency getNextToggleFreq(Frequency currentFreq)
    {
        switch (currentFreq)
        {
            case daily:
            {
                return Frequency.weekly;
            }
            case weekly:
            {
                return Frequency.biWeekly;
            }
            case biWeekly:
            {
                return Frequency.monthly;
            }
            case monthly:
            {
                return Frequency.yearly;
            }
            case yearly:
            {
                return Frequency.daily;
            }
            default:
            {
                return currentFreq;
            }
        }
    }

    /**
     * Utility method that constructs a payment/contribution string in the following format: "amount (SHFREQ)"
     * This method naively assumes that the frequency and amount are not invalid
     * @param amount - the recurring amount
     * @param frequency - the frequency at which the amount is applied
     * @return
     */
    public static String constructAmountFreqString(double amount, Frequency frequency)
    {
        String paymentString =
                BadBudgetApplication.roundedDoubleBB(amount) + " (" + BadBudgetApplication.shortHandFreq(frequency) + ")";
        return paymentString;
    }

    /**
     * Utility method that takes a constructed amount freq string (constructed via constructAmountFreqString) and extracts
     * the shorthand payment freq string from it.
     * @param amountFreqString - the string to use to extract the shorthand frequency from.
     * @return the shorthand frequency string
     */
    public static String extractShortHandFreq(String amountFreqString)
    {
        int startIndex = amountFreqString.indexOf("(") + 1;
        int endIndex = amountFreqString.length() - 1;
        String shortHandFreq = amountFreqString.substring(startIndex, endIndex);
        return shortHandFreq;
    }
}
