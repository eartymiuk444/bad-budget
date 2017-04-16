package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.BuildConfig;
import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.bbmain.FlavorSpecific;
import com.erikartymiuk.badbudgetlogic.main.Account;
import com.erikartymiuk.badbudgetlogic.main.BadBudgetData;
import com.erikartymiuk.badbudgetlogic.main.MoneyOwed;
import com.erikartymiuk.badbudgetlogic.main.Prediction;
import com.erikartymiuk.badbudgetlogic.predictdataclasses.PredictDataAccount;
import com.erikartymiuk.badbudgetlogic.predictdataclasses.PredictDataMoneyOwed;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * This is the predict activity class where users can quickly move between dates and see how all
 * their accounts look in a small popup window. It displays a calendar so the user can choose a month
 * and day, and a dedicated scrollable spinner to quickly navigate to the year they are interested in.
 * The popup window contains a button to navigate to a more detailed activity displaying the results
 * of prediction they have run.
 */
public class PredictActivity extends BadBudgetChildActivity implements
        Spinner.OnItemSelectedListener, PredictTaskCaller, DateSelectedHandler
{
    /* The popup window that displays all the quicklook data for the user's accounts on a certain
    * day. This window can be freely moved and resized.*/
    private PopupWindow quicklookPopup;

    /* Message displayed as a prediction is run */
    public static final String progressDialogMessage = "Running Prediction...";

    private Calendar currentEndCal;    //The date up to (and including this) which we have prediction data for
    private Calendar currentSelectedCal;  //The current date that the user has selected

    private Spinner yearSpinner;    //The view containing our spinner for selecting years.
    private boolean updateCalendarOnSpin = true;

    /* Pager and adapter for our calendar view */
    CustomCalendarPagerAdapter mCalendarPagerAdapter;
    ViewPager mViewPager;

    /* Keys for saving the instance state */
    private static final String SELECTED_DATE_KEY = "SELECTED_DATE";
    private static final String END_DATE_KEY = "END_DATE";
    private static final String SHOWN_YEAR_KEY = "SHOWN_YEAR";
    private static final String SHOWN_MONTH_KEY = "SHOWN_MONTH";
    private static final String SHOWN_SPIN_YEAR_POS_KEY = "SHOWN_SPIN_YEAR_POS";

    /**
     * Method called when the detailed look predict activity is finished. Sends back the
     * current end date as it may have changed in one of the detailed look activities
     * @param requestCode - the request code indicating what is being returned (should be the updated end date)
     * @param resultCode - the result code indicating if the result returned was ok
     * @param data
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == BadBudgetApplication.PREDICT_REQUEST_CODE_UPDATED_DATES)
        {
            if (resultCode == RESULT_OK)
            {
                Bundle args = data.getExtras();
                currentEndCal = (Calendar)args.get(BadBudgetApplication.PREDICT_CURRENT_END_DATE);
            }
        }
    }

    /**
     * Method called when the detailed look button is pressed on the quicklook popup.
     * Starts the detailed look home activity expecting a result, namely the current end date
     * we have prediction date up to.
     * @param view - the detailed look button that was pressed
     */
    public void popupQuicklookDetailedLookClick(View view)
    {
        Intent intent = new Intent(this, DetailedLookHomeActivity.class);
        intent.putExtra(BadBudgetApplication.PREDICT_CURRENT_CHOSEN_DAY, currentSelectedCal);
        intent.putExtra(BadBudgetApplication.PREDICT_CURRENT_END_DATE, currentEndCal);
        startActivityForResult(intent, BadBudgetApplication.PREDICT_REQUEST_CODE_UPDATED_DATES);
    }

    /**
     * Overridden back pressed method. Dismisses the quicklook popup if it is showing else
     * finishes this activity.
     */
    public void onBackPressed()
    {
        if (quicklookPopup.isShowing())
        {
            this.quicklookPopup.dismiss();
        }
        else
        {
            this.finish();
        }
    }

    /**
     * Dismisses our quicklook popup when the user navigates away from our activity.
     */
    protected void onPause()
    {
        if (quicklookPopup.isShowing())
        {
            quicklookPopup.dismiss();
        }
        super.onPause();
    }

    /**
     * Sets the quicklook popup to null when our activity is removed from memory. Should be dismissed
     * if necessary in onPause. Any predict data is also cleared.
     */
    protected void onDestroy()
    {
        quicklookPopup = null;
        super.onDestroy();
    }

    /**
     * On create method for the predict activity. Setups  our calendar
     * view which is used as our date picker and initializes our spinner to contain all possible years.
     * Also initializes our quicklook popup so it is ready
     * to be shown and moved/resized, which includes creating and setting two listeners (one for moving,
     * the other for resizing).
     * @param savedInstanceState - bundle to restore state
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContent(R.layout.content_predict);
        setupQuicklookPopup();

        int year;
        int month;
        int spinYear;

        if (savedInstanceState == null)
        {
            currentSelectedCal = null;
            currentEndCal = null;
            year = -1;
            month = -1;
            spinYear = -1;
        }
        else
        {
            currentSelectedCal = (Calendar)savedInstanceState.getSerializable(SELECTED_DATE_KEY);
            currentEndCal = (Calendar)savedInstanceState.getSerializable(END_DATE_KEY);
            year = savedInstanceState.getInt(SHOWN_YEAR_KEY);
            month = savedInstanceState.getInt(SHOWN_MONTH_KEY);
            spinYear = savedInstanceState.getInt(SHOWN_SPIN_YEAR_POS_KEY);
        }

        setupCalendar(year, month, spinYear);

        if (BuildConfig.FREE_VERSION)
        {
            FlavorSpecific.adRequest(this, R.layout.content_predict);
        }
    }

    /**
     * Method called on completion of the update task. If the predict data is stale this method
     * will re-setup the calendar using the updated bad budget data.
     * @param updated - true if an update to bb objects occurred false otherwise, unused
     */
    public void updateTaskCompleted(boolean updated)
    {
        BadBudgetApplication application = ((BadBudgetApplication) this.getApplication());
        if (application.getPredictBBDUpdated())
        {
            BadBudgetData bbd = application.getBadBudgetUserData();
            bbd.clearPredictData();

            currentEndCal = null;
            currentSelectedCal = null;

            if (quicklookPopup.isShowing())
            {
                quicklookPopup.dismiss();
            }

            setupCalendar(-1, -1, -1);
        }
    }

    /**
     * Called prior to this activity being destroyed. Saves any necessary state in order to restore
     * this activity when/if the user returns.
     * @param outState - Bundle to place saved state in
     */
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putSerializable(SELECTED_DATE_KEY, currentSelectedCal);
        outState.putSerializable(END_DATE_KEY, currentEndCal);

        //Min
        BadBudgetApplication application = ((BadBudgetApplication) this.getApplication());
        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(application.getToday());
        tempCal.add(Calendar.MONTH, mViewPager.getCurrentItem());

        outState.putInt(SHOWN_YEAR_KEY, tempCal.get(Calendar.YEAR));
        outState.putInt(SHOWN_MONTH_KEY, tempCal.get(Calendar.MONTH));
        outState.putInt(SHOWN_SPIN_YEAR_POS_KEY, yearSpinner.getSelectedItemPosition());
    }

    /**
     * Method called when the year in our year spinner changes. This method is called when
     * the user selects a new year, when they select a new date which causes the year to change,
     * and when the dropdown year is set programmatically.
     * In the first typical case this method updates our calendar so that it is set to show the currently shown
     * month but in the chosen year.
     * In the second case the calendar is already showing the correct month and year
     * (the month and year the user selected)
     * In the third case the calendar is not updated.
     *
     * @param parent - the adapter view for our year spinner
     * @param view - the text view in our spinner that was clicked
     * @param position - the position of the view in the view adapter - unused
     * @param id - the row id of the item that is selected - unused
     */
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        if (updateCalendarOnSpin) {

            BadBudgetApplication application = ((BadBudgetApplication) this.getApplication());

            Calendar tempCal = Calendar.getInstance();
            Calendar tempMinCal = Calendar.getInstance();
            tempCal.setTime(application.getToday());
            tempMinCal.setTime(application.getToday());

            tempCal.add(Calendar.MONTH, mViewPager.getCurrentItem());

            int dropSelectedYear = (Integer) parent.getSelectedItem();
            int currentShownMonth = tempCal.get(Calendar.MONTH);

            Calendar toShow = new GregorianCalendar(dropSelectedYear, currentShownMonth, 1);

            int pagePosition = CustomCalendarPagerAdapter.numMonthsBetween(tempMinCal, toShow);

            mViewPager.setCurrentItem(pagePosition, false);
        }
        else
        {
            updateCalendarOnSpin = true;
        }
    }

    /**
     *
     */
    /**
     * Private helper that sets up our year dropdown. Uses the applications today date for the
     * min and max years. The selected year is set to the passed position if not -1 otherwise is set
     * to the year of the current selected date if
     * set otherwise it is set to be today's year.
     * @param spinYearPos - the position of the year to select or -1 if we should use the selected or
     *                    today year.
     */
    private void setupYearDropdown(int spinYearPos)
    {
        /* Setup our year spinner */
        yearSpinner = (Spinner)findViewById(R.id.predictYearSpinner);

        //Today & Min
        BadBudgetApplication application = ((BadBudgetApplication) this.getApplication());
        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(application.getToday());
        Calendar todayCal = new GregorianCalendar(tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH), tempCal.get(Calendar.DAY_OF_MONTH));

        //Max
        Calendar maxCal = new GregorianCalendar(todayCal.get(Calendar.YEAR)+getResources().getInteger(R.integer.predict_years),
                todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH));

        ArrayList<Integer> yearArray = new ArrayList<Integer>();
        for (int i = todayCal.get(Calendar.YEAR); i <= maxCal.get(Calendar.YEAR); i++)
        {
            yearArray.add(i);
        }

        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, yearArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(adapter);
        yearSpinner.setOnItemSelectedListener(this);

        updateCalendarOnSpin = false;

        if (spinYearPos != -1)
        {
            yearSpinner.setSelection(spinYearPos);
        }
        else if (currentSelectedCal != null)
        {
            yearSpinner.setSelection(currentSelectedCal.get(Calendar.YEAR) - todayCal.get(Calendar.YEAR));
        }
        else
        {
            yearSpinner.setSelection(0);
        }
    }

    /**
     * Private helper method that sets up our calendar and our year drop down. Uses the current
     * selected date of this predict activity to determine the calendar's selected date, the application's
     * today date to determine the calendar's today, min, and max dates. Determines what month and year
     * to initially show using the passed values
     * @param year - the year to show initially (or -1 if the calendar should show today's date)
     * @param month - the month to show initially (or -1 if the calendar should show today's date)
     * @param spinYearPos - the position of the year to set in our spinner or -1 if we should use
     *                    the selected year or today's(min) year.
     */
    private void setupCalendar(int year, int month, int spinYearPos)
    {
        mViewPager = (ViewPager) findViewById(R.id.pager);

        //Today & Min
        BadBudgetApplication application = ((BadBudgetApplication) this.getApplication());
        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(application.getToday());
        Calendar todayCal = new GregorianCalendar(tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH), tempCal.get(Calendar.DAY_OF_MONTH));

        //Max
        Calendar maxCal = new GregorianCalendar(todayCal.get(Calendar.YEAR)+getResources().getInteger(R.integer.predict_years),
                todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH));

        mCalendarPagerAdapter = new CustomCalendarPagerAdapter(getSupportFragmentManager(), todayCal, currentSelectedCal, todayCal, maxCal);
        mCalendarPagerAdapter.setDateSelectedHandler(this);
        mViewPager.setAdapter(mCalendarPagerAdapter);

        if (year != -1 || month != -1)
        {
            //Min
            Calendar tempMinCal = Calendar.getInstance();
            tempMinCal.setTime(application.getToday());

            Calendar toShow = new GregorianCalendar(year, month, 1);
            int pagePosition = CustomCalendarPagerAdapter.numMonthsBetween(tempMinCal, toShow);
            mViewPager.setCurrentItem(pagePosition, false);
        }

        setupYearDropdown(spinYearPos);

        mViewPager.invalidate();
        mViewPager.requestLayout();
    }

    /**
     * A callback method called when the selected day in our calendar view changes. Indicates that
     * the quicklook popup window should be updated and shown and ensures we have the required prediction
     * data before updating and showing the popup to the user and gets that data if we don't. Showing the user
     * a progress dialog until the data has been gathered. Additionally updates the selectable year
     * field if the year has changed.
     * @param year - year of the selected day
     * @param month - month of the newly selected day
     * @param day - day of the newly selected day
     */
    public void onDateSelected(int year, int month, int day) {
        currentSelectedCal = new GregorianCalendar(year, month, day);

        BadBudgetApplication application = ((BadBudgetApplication) this.getApplication());

        //check if we need to update our spinners year
        int spinnerYear = (Integer) yearSpinner.getSelectedItem();
        if (spinnerYear != year)
        {
            Calendar todayCal = Calendar.getInstance();
            todayCal.setTime(application.getToday());
            int todayYear = todayCal.get(Calendar.YEAR);

            int index = year - todayYear;

            updateCalendarOnSpin = false;
            yearSpinner.setSelection(index);
        }

        if (currentEndCal == null)
        {
            Calendar yesterdayCalendar = Calendar.getInstance();
            yesterdayCalendar.setTime(application.getToday());
            yesterdayCalendar.add(Calendar.DAY_OF_MONTH, -1);

            currentEndCal = yesterdayCalendar;
        }
        PredictActivity.setPrediction(currentSelectedCal.getTime(), currentEndCal.getTime(), this, this);

    }

    /**
     * Static method that runs the predict task. Checks if the current end date passed is enough for
     * having date up to our requested predict date. If not runs the predict task (which continues or runs our
     * predict algorithm) using the passed context and caller. The caller will be called back via predictionSet
     * once the task completes. This method also creates a progress dialog using the context until the prediction
     * is complete. The date span goes as follows: intial_span years from today, jump years from today, then
     * jump*i years from today until date is included in our span.
     *
     * @param predictDate - the date we want prediction data up to (inclusive)
     * @param currentEndDate - the current date we have prediction data up to
     * @param context - the context to invoke the predict task from with which we get access to bad budget
     *                      app resources and the application today date
     * @param predictTaskCaller - the caller of the prediction task where the callback method predictionSet will
     *                              be sent
     */
    public static void setPrediction(Date predictDate, Date currentEndDate, Activity context, PredictTaskCaller predictTaskCaller)
    {
        int initialSpan = context.getResources().getInteger(R.integer.initial_predict_span);
        int jump = context.getResources().getInteger(R.integer.predict_jump);

        Date applicationToday = ((BadBudgetApplication)context.getApplication()).getToday();

        Calendar initialSpanCal = Calendar.getInstance();
        initialSpanCal.setTime(applicationToday);
        initialSpanCal.add(Calendar.YEAR, initialSpan);
        Date initialSpanDate = initialSpanCal.getTime();

        Calendar oneJumpCal = Calendar.getInstance();
        oneJumpCal.setTime(applicationToday);
        oneJumpCal.add(Calendar.YEAR, jump);
        Date oneJumpDate = oneJumpCal.getTime();

        //Check to see if date already falls into our date span (indicated by end date), can be
        //exactly on the end date.
        if (!(Prediction.numDaysBetween(predictDate, currentEndDate) >= 0))
        {
            /* Since we don't have the necessary data we need to run or continue the prediction algorithm */
            ProgressDialog progress = new ProgressDialog(context);
            progress.setMessage(progressDialogMessage);
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.setCancelable(false);

            PredictTask task = null;

            //Check to see if the date fits within the intial span of today
            if (Prediction.numDaysBetween(predictDate, initialSpanDate) >= 0)
            {
                task = new PredictTask(predictTaskCaller, context, progress, applicationToday, currentEndDate, initialSpanDate);
            }
            //If it doesn't fit in the intial span try one jump away
            else if (Prediction.numDaysBetween(predictDate, oneJumpDate) >= 0)
            {
                task = new PredictTask(predictTaskCaller, context, progress, applicationToday, currentEndDate, oneJumpDate);
            }
            //If it doesn't fit one jump away we try multiples of jump until date does fit
            else
            {
                Calendar endCalendar = Calendar.getInstance();
                endCalendar.setTime(applicationToday);
                endCalendar.add(Calendar.YEAR, jump);

                do
                {
                    endCalendar.add(Calendar.YEAR, jump);
                }
                while (!(Prediction.numDaysBetween(predictDate, endCalendar.getTime()) >= 0));

                task = new PredictTask(predictTaskCaller, context, progress, applicationToday, currentEndDate, endCalendar.getTime());
            }

            task.execute();
        }
        //We already had sufficient data for the passed in date.
        else
        {
            predictTaskCaller.predictionSet(currentEndDate);
        }
    }

    /**
     * Callback method called after a call to set prediction. Should only be called once the prediction
     * data we need for the current selected day has been set. Sets the currentEndDate with the passed newEndDate
     * This method then searches through all the bad budget
     * objects and finds any items with quicklook specified. It adds those items along with their quicklook
     * value to the quicklook popup for the currentSelected Date. Finally if the popup isn't showing yet it
     * makes sure to show the popup else the popup is left where it is and is simply updated.
     *
     * @param newEndDate - the new end date that is now set
     */
    public void predictionSet(Date newEndDate)
    {
        if (currentEndCal == null)
        {
            currentEndCal = Calendar.getInstance();
        }

        currentEndCal.setTime(newEndDate);

        refreshQuicklookPopup();
        if (!quicklookPopup.isShowing())
        {
            View anchor = findViewById(R.id.quicklookPopupAnchor);
            int[] anchorLocation = new int[2];
            anchor.getLocationOnScreen(anchorLocation);
            quicklookPopup.showAtLocation(findViewById(android.R.id.content), Gravity.NO_GRAVITY, anchorLocation[0], anchorLocation[1]);
        }
    }

    /**
     * Required method for the OnItemSelectedListener Interface. This method shouldn't be called in our
     * use case. It takes no action.
     * @param parent - unused
     */
    public void onNothingSelected(AdapterView<?> parent) {}

    /**
     * Private helper method that uses the current selected date and our predict data to repopulate
     * any quicklook accounts or debts in our popup. First clears the popup of any previously added
     * quicklook data. Additionally updates the quicklook popup's title to be the current selected
     * date. (Note this assumes we have predict data for the current selected date).
     */
    private void refreshQuicklookPopup()
    {
        BadBudgetApplication application = ((BadBudgetApplication) this.getApplication());
        //Remove any previously added rows
        LinearLayout linearLayout = (LinearLayout) quicklookPopup.getContentView().findViewById(R.id.quicklookLinearLayout);
        linearLayout.removeAllViews();

        //Search through our bbd for any quicklook items
        BadBudgetData bbd = ((BadBudgetApplication) this.getApplication()).getBadBudgetUserData();

        //Look through all cash accounts (including savings accounts)
        for (Account currAccount : bbd.getAccounts())
        {
            if (currAccount.quickLook())
            {
                PredictDataAccount pda = currAccount.getPredictData(Prediction.numDaysBetween(application.getToday(), currentSelectedCal.getTime()));
                String name = currAccount.name();
                String value = BadBudgetApplication.roundedDoubleBB(pda.value()); //The value is the value we show on quicklook for accounts
                addRowToLinearLayout(linearLayout, name, value);
            }
        }

        //Look through all debts
        for (MoneyOwed currDebt : bbd.getDebts())
        {
            if (currDebt.quicklook()) {
                PredictDataMoneyOwed pdmo = currDebt.getPredictData(Prediction.numDaysBetween(application.getToday(), currentSelectedCal.getTime()));
                String name = currDebt.name();
                String value = BadBudgetApplication.roundedDoubleBB(pdmo.value());
                addRowToLinearLayout(linearLayout, name, value);
            }
        }

        //Update the date in the title
        TextView title = (TextView) quicklookPopup.getContentView().findViewById(R.id.quicklookPopupDate);
        title.setText(BadBudgetApplication.dateString(currentSelectedCal.getTime()));
    }

    /**
     * Private helper that inflates and initializes our quicklook popup. This needs to be called
     * before the popup can be refreshed or shown.
     */
    private void setupQuicklookPopup()
    {
        /* Setup our quicklook popup */

        LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = layoutInflater.inflate(R.layout.popup_quicklook, null, false);
        quicklookPopup = new PopupWindow(popupView, getResources().getDimensionPixelSize(R.dimen.quicklook_popup_width),
                getResources().getDimensionPixelSize(R.dimen.quicklook_popup_height));
        quicklookPopup.setClippingEnabled(false);

        /* The touch listener for moving the popup window */
        View.OnTouchListener otlMove = new View.OnTouchListener() {

            int relOrigX;
            int relOrigY;

            public boolean onTouch(View view, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        /* see how far off we are from the top left corner - should only
                        * enter here once per finger press */
                        relOrigX = (int) event.getX();
                        relOrigY = (int) event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        /* calculate the new position of the top left corner */
                        int absPosX = (int)event.getRawX() - relOrigX;
                        int absPosY = (int)event.getRawY() - relOrigY;

                        //-1 indicates we don't update the width or the height,
                        //the true indicates we force reposition even if it doesn't seem like we need to.
                        quicklookPopup.update(absPosX, absPosY, -1, -1, true);
                        break;
                }
                //We consume the touch
                return true;
            }
        };

        /* the touch listener for the expander image/area */
        View.OnTouchListener otlExpand = new View.OnTouchListener() {
            int absOrigX;
            int absOrigY;

            int origWidth;
            int origHeight;

            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        absOrigX = (int) event.getRawX();
                        absOrigY = (int) event.getRawY();

                        origWidth = quicklookPopup.getWidth();
                        origHeight = quicklookPopup.getHeight();

                        /* Change the color of our expander to indicate the user has clicked it and that
                        the popup will expand upon release */
                        int backgroundColorAccent = ContextCompat.getColor(PredictActivity.this, R.color.colorAccent);
                        view.setBackgroundColor(backgroundColorAccent);

                        break;
                    case MotionEvent.ACTION_UP:
                        int sizeChangeX = (int)event.getRawX() - absOrigX;
                        int sizeChangeY = (int)event.getRawY() - absOrigY;

                        int backgroundColorDark = ContextCompat.getColor(PredictActivity.this, R.color.colorPrimaryDark);
                        view.setBackgroundColor(backgroundColorDark);

                        int width = origWidth + sizeChangeX;
                        int height = origHeight + sizeChangeY;

                        //Don't allow the user to negatively expand.
                        if (width > 0 && height > 0)
                        {
                            quicklookPopup.update(width, height);
                        }
                        break;
                }

                return true;
            }
        };

        /* Set our two touch listeners */
        View expander = quicklookPopup.getContentView().findViewById(R.id.quicklookExpandImage);
        expander.setOnTouchListener(otlExpand);

        quicklookPopup.getContentView().setOnTouchListener(otlMove);
    }

    /**
     * Helper method to add a row to the passed linear layout. The row is a horizontal linear layout
     * with two text views (or columns). The text in column 1 is the passed identifier and in column 2
     * is the value we want to show for whatever object this quicklook row is representing.
     * @param linearLayout - the layout to add the row to
     * @param identifier - the string identifying the quicklook object the row represents
     * @param quicklookValue - the value in string format that the object in this row has.
     */
    private void addRowToLinearLayout(LinearLayout linearLayout, String identifier, String quicklookValue)
    {
        //Horizontal linear layout which will be our row with two columns
        LinearLayout horizontalLinearLayout = new LinearLayout(this);
        horizontalLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        horizontalLinearLayout.setLayoutParams(layoutParams);

        //Layout Params for our two columns
        int gravity = Gravity.START;
        LinearLayout.LayoutParams textViewLayoutParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        textViewLayoutParams.weight = 1;

        //Column 1
        TextView quicklookAccountName = new TextView(this);
        quicklookAccountName.setText(identifier);
        quicklookAccountName.setLayoutParams(textViewLayoutParams);
        quicklookAccountName.setGravity(gravity);
        int padding = getResources().getDimensionPixelSize(R.dimen.quicklook_popup_padding);
        quicklookAccountName.setPadding(padding, 0, 0, 0);

        //Column 2
        TextView quickLookAccountValue = new TextView(this);
        quickLookAccountValue.setText(quicklookValue);
        quickLookAccountValue.setLayoutParams(textViewLayoutParams);
        quickLookAccountValue.setGravity(gravity);

        //Add the columns
        horizontalLinearLayout.addView(quicklookAccountName);
        horizontalLinearLayout.addView(quickLookAccountValue);

        //Add the row
        linearLayout.addView(horizontalLinearLayout);
    }
}
