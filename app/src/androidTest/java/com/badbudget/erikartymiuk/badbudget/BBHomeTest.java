package com.badbudget.erikartymiuk.badbudget;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.contrib.PickerActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.TableLayout;

import com.badbudget.erikartymiuk.badbudget.bbmain.BBDatabaseContract;
import com.badbudget.erikartymiuk.badbudget.bbmain.HomeActivity;

import java.util.Calendar;
import java.util.Date;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Created by Erik Artymiuk on 9/30/2016.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class BBHomeTest {

    //Frequencies
    private static final String ONE_TIME = "One Time";
    private static final String DAILY = "Daily";
    private static final String WEEKLY = "Weekly";
    private static final String BIWEEKLY = "Bi-Weekly";
    private static final String MONTHLY = "Monthly";
    private static final String YEARLY = "Yearly";

    //Cash Accounts

    //Covantage
    private static final String COVANTAGE = "Covantage";
    private static final String COVANTAGE_AMOUNT = "1160.62";

    //Chase
    private static final String CHASE_CHECKING = "Chase Checking";
    private static final String CHASE_CHECKING_AMOUNT = "3408.09";

    //Acorns
    private static final String ACORNS = "Acorns";
    private static final String ACORNS_AMOUNT = "70.07";

    //Vanguard
    private static final String VANGUARD = "Vanguard";
    private static final String VANGUARD_AMOUNT = "2988.81";

    //Saving Accounts

    //Emergency
    private static final String EMERGENCY = "Emergency";
    private static final String EMERGENCY_AMOUNT = "4";
    private static final String EMERGENCY_FREQ = MONTHLY;
    private static final String EMERGENCY_CONTRIBUTION_AMOUNT = "150";
    private static final String EMERGENCY_SOURCE = COVANTAGE;
    private static final int EMERGENCY_NEXT_DAY_OFFSET = 0;

    //Fun (>600)
    private static final String FUN = "Fun (>600)";
    private static final String FUN_AMOUNT = "0";
    private static final String FUN_GOAL_AMOUNT = "600";
    private static final String FUN_CONTRIBUTION_FREQ = MONTHLY;
    private static final String FUN_CONTRIBUTION_AMOUNT = "75";
    private static final String FUN_SOURCE = COVANTAGE;
    private static final int FUN_NEXT_DAY_OFFSET = 0;

    //Debts

    //CreditCards

    //Chase Freedom
    private static final String FREEDOM = "Chase Freedom";
    private static final String FREEDOM_AMOUNT = "92.01";
    private static final String FREEDOM_FREQ = MONTHLY;
    private static final String FREEDOM_PAY_AMOUNT = null;
    private static final boolean FREEDOM_PAYOFF = true;
    private static final String FREEDOM_SOURCE = CHASE_CHECKING;
    private static final int FREEDOM_NEXT_OFFSET = 0;
    private static final int FREEDOM_END_OFFSET = -1;
    private static final int FREEDOM_GOAL_OFFSET = -1;

    //Discover Card
    private static final String DISCOVER = "Discover";
    private static final String DISCOVER_AMOUNT = "0";
    private static final String DISCOVER_FREQ = MONTHLY;
    private static final String DISCOVER_PAY_AMOUNT = null;
    private static final boolean DISCOVER_PAYOFF = true;
    private static final String DISCOVER_SOURCE = COVANTAGE;
    private static final int DISCOVER_NEXT_OFFSET = 0;
    private static final int DISCOVER_END_OFFSET = -1;
    private static final int DISCOVER_GOAL_OFFSET = -1;

    //Loans - TODO
    private static final String JEEP = "Jeep";
    private static final String JEEP_AMOUNT = "1201.06";
    private static final String JEEP_FREQ = MONTHLY;
    private static final String JEEP_PAY_AMOUNT = "90";
    private static final boolean JEEP_PAYOFF = false;
    private static final String JEEP_SOURCE = COVANTAGE;
    private static final int JEEP_NEXT_OFFSET = 0;
    private static final int JEEP_END_OFFSET = -1;
    private static final int JEEP_GOAL_OFFSET = -1;

    //Jeep

    //Gains
    private static final String TEACH = "Teach";
    private static final String TEACH_FREQ = BIWEEKLY;
    private static final String TEACH_AMOUNT = "1495.26";
    private static final int TEACH_NEXT_OFFSET = 180;
    private static final int TEACH_END_OFFSET = -1;
    private static final String TEACH_DESTINATION = COVANTAGE;

    //TODO
    private static final String MATHLAB = "Math Lab";
    private static final String MATHLAB_FREQ = BIWEEKLY;
    private static final String MATHLAB_AMOUNT = "90";
    private static final int MATHLAB_NEXT_OFFSET = 0;
    private static final int MATHLAB_END_OFFSET = -1;
    private static final String MATHLAB_DESTINATION = COVANTAGE;

    //TODO
    private static final String FASPRING = "Financial Aid Spring";
    private static final String FASPRING_FREQ = BIWEEKLY;
    private static final String FASPRING_AMOUNT = "90";
    private static final int FASPRING_NEXT_OFFSET = 0;
    private static final int FASPRING_END_OFFSET = -1;
    private static final String FASPRING_DESTINATION = COVANTAGE;

    //Losses
    private static final String PSVUE = "PS Vue";
    private static final String PSVUE_FREQ = MONTHLY;
    private static final String PSVUE_AMOUNT = "32";
    private static final int PSVUE_NEXT_OFFSET = 0;
    private static final int PSVUE_END_OFFSET = -1;
    private static final String PSVUE_SOURCE = FREEDOM;

    private static final String CRICKET = "Cricket";
    private static final String CRICKET_FREQ = MONTHLY;
    private static final String CRICKET_AMOUNT = "35";
    private static final int CRICKET_NEXT_OFFSET = 0;
    private static final int CRICKET_END_OFFSET = -1;
    private static final String CRICKET_SOURCE = FREEDOM;

    private static final String RENT = "Rent";
    private static final String RENT_FREQ = MONTHLY;
    private static final String RENT_AMOUNT = "350";
    private static final int RENT_NEXT_OFFSET = 0;
    private static final int RENT_END_OFFSET = 180;
    private static final String RENT_SOURCE = CHASE_CHECKING;

    private static final String INSURANCE = "Insurance";
    private static final String INSURANCE_FREQ = MONTHLY;
    private static final String INSURANCE_AMOUNT = "42";
    private static final int INSURANCE_NEXT_OFFSET = 0;
    private static final int INSURANCE_END_OFFSET = -1;
    private static final String INSURANCE_SOURCE = FREEDOM;

    //BudgetItems
    private static final String JUNKFOOD = "Junk Food";
    private static final String JUNKFOOD_FREQ = DAILY;
    private static final String JUNKFOOD_AMOUNT = "3";
    private static final int JUNKFOOD_NEXT_OFFSET = 0;
    private static final int JUNKFOOD_END_OFFSET = -1;

    private static final String GROCERY = "Grocery";
    private static final String GROCERY_FREQ = WEEKLY;
    private static final String GROCERY_AMOUNT = "35";
    private static final int GROCERY_NEXT_OFFSET = 0;
    private static final int GROCERY_END_OFFSET = -1;

    private static final String ENTERTAINMENT = "Entertainment";
    private static final String ENTERTAINMENT_FREQ = WEEKLY;
    private static final String ENTERTAINMENT_AMOUNT = "15";
    private static final int ENTERTAINMENT_NEXT_OFFSET = 0;
    private static final int ENTERTAINMENT_END_OFFSET = -1;

    private static final String NECESSITY = "Necessity";
    private static final String NECESSITY_FREQ = BIWEEKLY;
    private static final String NECESSITY_AMOUNT = "30";
    private static final int NECESSITY_NEXT_OFFSET = 0;
    private static final int NECESSITY_END_OFFSET = -1;

    private static final String GAS = "Gas";
    private static final String GAS_FREQ = BIWEEKLY;
    private static final String GAS_AMOUNT = "40";
    private static final int GAS_NEXT_OFFSET = 0;
    private static final int GAS_END_OFFSET = -1;

    private static final String CUSHION = "Cushion";
    private static final String CUSHION_FREQ = MONTHLY;
    private static final String CUSHION_AMOUNT = "100";
    private static final int CUSHION_NEXT_OFFSET = 0;
    private static final int CUSHION_END_OFFSET = -1;

    //String to identify various views
    private static final String ADD_ACCOUNT = "Add Account";
    private static final String HOME_SAVINGS_BUTTON = "Savings";
    private static final String ADD_SAVINGS_ACCOUNT = "Add Savings Account";

    @Rule
    public ActivityTestRule<HomeActivity> mActivityRule = new ActivityTestRule<>(
            HomeActivity.class);

    @Before
    public void initValidString() {
        // Specify a valid string.
    }


    //@Test
    public void test1DeleteAll()
    {
        //Gains
        clickId(R.id.gainsButton);
        clickText(TEACH);
        clickIdScroll(R.id.deleteButton);
        Espresso.pressBack();

        //Losses
        clickId(R.id.lossesButton);
        clickText(PSVUE);
        clickIdScroll(R.id.deleteButton);
        clickText(CRICKET);
        clickIdScroll(R.id.deleteButton);
        clickText(RENT);
        clickIdScroll(R.id.deleteButton);
        clickText(INSURANCE);
        clickIdScroll(R.id.deleteButton);
        Espresso.pressBack();

        //Budget Items
        clickId(R.id.budgetButton);
        clickText(JUNKFOOD);
        clickId(R.id.budgetButton);
        clickText(GROCERY);
        clickId(R.id.budgetButton);
        clickText(ENTERTAINMENT);
        clickId(R.id.budgetButton);
        clickText(NECESSITY);
        clickId(R.id.budgetButton);
        clickText(GAS);
        clickId(R.id.budgetButton);
        clickText(CUSHION);
        Espresso.pressBack();

        //Debts
        clickId(R.id.debtsButton);
        clickId(R.id.credit_cards_button);
        clickText(FREEDOM);
        clickIdScroll(R.id.deleteButton);
        clickText(DISCOVER);
        clickIdScroll(R.id.deleteButton);
        Espresso.pressBack();
        Espresso.pressBack();

        //Savings
        clickId(R.id.savingsButton);
        clickText(EMERGENCY);
        clickIdScroll(R.id.deleteButton);
        clickText(FUN);
        clickIdScroll(R.id.deleteButton);
        Espresso.pressBack();

        //Cash
        clickId(R.id.cashButton);
        clickText(COVANTAGE);
        clickId(R.id.deleteButton);
        clickText(CHASE_CHECKING);
        clickId(R.id.deleteButton);
        clickText(ACORNS);
        clickId(R.id.deleteButton);
        clickText(VANGUARD);
        clickId(R.id.deleteButton);
        Espresso.pressBack();
    }

    @Test
    public void test2Cash() {

        clickId(R.id.cashButton);
        clickId(R.id.addAccountButton);
        addAccount(COVANTAGE, COVANTAGE_AMOUNT, true, false);
        clickId(R.id.addAccountButton);
        addAccount(CHASE_CHECKING, CHASE_CHECKING_AMOUNT, true, false);
        clickId(R.id.addAccountButton);
        addAccount(ACORNS, ACORNS_AMOUNT, true, false);
        clickId(R.id.addAccountButton);
        addAccount(VANGUARD, VANGUARD_AMOUNT, true, false);
        Espresso.pressBack();
    }

    @Test
    public void test3AddSavings() {
        clickId(R.id.savingsButton);
        clickId(R.id.addAccountButton);
        addSavingsAccount(EMERGENCY, EMERGENCY_AMOUNT, true, false, null, -1, EMERGENCY_FREQ, EMERGENCY_CONTRIBUTION_AMOUNT, EMERGENCY_SOURCE, EMERGENCY_NEXT_DAY_OFFSET, -1, true);

        clickId(R.id.addAccountButton);
        addSavingsAccount(FUN, FUN_AMOUNT, true, true, "600", -1, FUN_CONTRIBUTION_FREQ, FUN_CONTRIBUTION_AMOUNT, FUN_SOURCE, FUN_NEXT_DAY_OFFSET, -1, false);
        Espresso.pressBack();

        onView(withId(R.id.savingsButton)).check(matches(withText(HOME_SAVINGS_BUTTON)));
    }

    @Test
    public void test4addDebts() {

        clickId(R.id.debtsButton);
        clickId(R.id.credit_cards_button);
        clickId(R.id.addCreditCardButton);

        addCreditCardWithPay(FREEDOM, FREEDOM_AMOUNT, true, FREEDOM_FREQ, FREEDOM_PAY_AMOUNT, FREEDOM_PAYOFF, FREEDOM_SOURCE, FREEDOM_NEXT_OFFSET, FREEDOM_END_OFFSET, FREEDOM_GOAL_OFFSET);
        clickId(R.id.addCreditCardButton);
        addCreditCardWithPay(DISCOVER, DISCOVER_AMOUNT, true, DISCOVER_FREQ, DISCOVER_PAY_AMOUNT, DISCOVER_PAYOFF, DISCOVER_SOURCE, DISCOVER_NEXT_OFFSET, DISCOVER_END_OFFSET, DISCOVER_GOAL_OFFSET);

        Espresso.pressBack();
        Espresso.pressBack();
        onView(withId(R.id.savingsButton)).check(matches(withText(HOME_SAVINGS_BUTTON)));
    }

    @Test
    public void test5AddGains()
    {
        clickId(R.id.gainsButton);
        clickId(R.id.addGainButton);
        addGain(TEACH, TEACH_FREQ, TEACH_AMOUNT, TEACH_NEXT_OFFSET, TEACH_END_OFFSET, TEACH_DESTINATION);
        Espresso.pressBack();
        onView(withId(R.id.savingsButton)).check(matches(withText(HOME_SAVINGS_BUTTON)));
    }

    @Test
    public void test6AddLosses()
    {
        clickId(R.id.lossesButton);
        clickId(R.id.addLossButton);

        addLoss(PSVUE, PSVUE_FREQ, PSVUE_AMOUNT, PSVUE_NEXT_OFFSET, PSVUE_END_OFFSET, PSVUE_SOURCE);
        clickId(R.id.addLossButton);

        addLoss(CRICKET, CRICKET_FREQ, CRICKET_AMOUNT, CRICKET_NEXT_OFFSET, CRICKET_END_OFFSET, CRICKET_SOURCE);
        clickId(R.id.addLossButton);

        addLoss(RENT, RENT_FREQ, RENT_AMOUNT, RENT_NEXT_OFFSET, RENT_END_OFFSET, RENT_SOURCE);
        clickId(R.id.addLossButton);

        addLoss(INSURANCE, INSURANCE_FREQ, INSURANCE_AMOUNT, INSURANCE_NEXT_OFFSET, INSURANCE_END_OFFSET, INSURANCE_SOURCE);
        clickId(R.id.addLossButton);
    }

    @Test
    public void test7AddBudgetPrefs()
    {
        clickId(R.id.budgetButton);
        selectSpinnerItemInId(R.id.budgetSourceInput, FREEDOM);
        clickIdScroll(R.id.submitButton);
    }

    @Test
    public void test8AddBudgetItems()
    {
        clickId(R.id.budgetButton);
        clickId(R.id.addLossButton);

        addItem(JUNKFOOD, JUNKFOOD_FREQ, JUNKFOOD_AMOUNT, JUNKFOOD_NEXT_OFFSET, JUNKFOOD_END_OFFSET);
        clickId(R.id.addLossButton);

        addItem(GROCERY, GROCERY_FREQ, GROCERY_AMOUNT, GROCERY_NEXT_OFFSET, GROCERY_END_OFFSET);
        clickId(R.id.addLossButton);

        addItem(ENTERTAINMENT, ENTERTAINMENT_FREQ, ENTERTAINMENT_AMOUNT, ENTERTAINMENT_NEXT_OFFSET, ENTERTAINMENT_END_OFFSET);
        clickId(R.id.addLossButton);

        addItem(NECESSITY, NECESSITY_FREQ, NECESSITY_AMOUNT, NECESSITY_NEXT_OFFSET, NECESSITY_END_OFFSET);
        clickId(R.id.addLossButton);

        addItem(GAS, GAS_FREQ, GAS_AMOUNT, GAS_NEXT_OFFSET, GAS_END_OFFSET);
        clickId(R.id.addLossButton);

        addItem(CUSHION, CUSHION_FREQ, CUSHION_AMOUNT, CUSHION_NEXT_OFFSET, CUSHION_END_OFFSET);
        clickId(R.id.addLossButton);
    }

    private void addItem(String name, String freq, String amount, int next, int end)
    {
        typeTextId(R.id.addBudgetItemDescriptionInput, name);
        selectSpinnerItemInId(R.id.addBudgetItemFrequencySpinner, freq);
        typeTextId(R.id.addBudgetItemAmountInput, amount);
        setDateWithId(R.id.addBudgetItemNextInput, next);

        if (end != -1)
        {
            setDateWithId(R.id.addBudgetItemEndInput, end);
        }
        else
        {
            clickIdScroll(R.id.addBudgetItemOngoingCheckbox);
        }

        clickIdScroll(R.id.submitButton);
    }

    private void addLoss(String name, String freq, String amount, int next, int end, String source)
    {
        typeTextId(R.id.addLossDescriptionInput, name);
        selectSpinnerItemInId(R.id.addLossFrequencySpinner, freq);
        typeTextId(R.id.addLossAmountInput, amount);
        setDateWithId(R.id.addLossNextInput, next);

        if (end != -1)
        {
            setDateWithId(R.id.addLossEndInput, end);
        }
        else
        {
            clickIdScroll(R.id.addLossOngoingCheckbox);
        }

        selectSpinnerItemInId(R.id.addLossSourceSpinner, source);
        clickIdScroll(R.id.submitButton);
    }

    private void addGain(String name, String freq, String amount, int next, int end, String destination)
    {
        typeTextId(R.id.addGainDescriptionInput, name);
        selectSpinnerItemInId(R.id.addGainFrequencySpinner, freq);
        typeTextId(R.id.addGainAmountInput, amount);
        setDateWithId(R.id.addGainNextInput, next);

        if (end != -1)
        {
            setDateWithId(R.id.addGainEndInput, end);
        }
        else
        {
            clickIdScroll(R.id.addGainOngoingCheckbox);
        }

        selectSpinnerItemInId(R.id.addGainDestinationSpinner, destination);
        clickIdScroll(R.id.submitButton);
    }

    private void addCreditCard(String name, String value, boolean quicklook, boolean setupPayment)
    {
        typeTextId(R.id.inputCardName, name);
        typeTextId(R.id.inputCurrentDebt, value);

        if (quicklook)
        {
            clickId(R.id.addCCQuicklookCheckbox);
        }

        if (setupPayment)
        {
            clickId(R.id.setupPaymentCheckbox);
        }

        clickId(R.id.submitButton);
    }

    private void addCreditCardWithPay(String name, String value, boolean quicklook, String frequency, String payAmount,
                                      boolean payoff, String source, int nextOffset, int endOffset, int goalOffset)
    {
        addCreditCard(name, value, quicklook, true);

        selectSpinnerItemInId(R.id.addCreditCardPaymentFrequencySpinner, frequency);

        if (payAmount != null)
        {
            typeTextIdScroll(R.id.addCreditCardPaymentAmountHint, payAmount);
        }

        if (payoff)
        {
            clickIdScroll(R.id.addCreditCardPaymentAmountPayoff);
        }

        selectSpinnerItemInId(R.id.addCreditCardPaymentSourceSpinner, source);
        setDateWithId(R.id.addCreditCardPaymentNextInput, nextOffset);
        if (endOffset != -1) {
            setDateWithId(R.id.addCreditCardPaymentInputEnd, endOffset);
        }
        else
        {
            clickIdScroll(R.id.addCreditCardPaymentOngoingCheckbox);
        }

        if (goalOffset != -1)
        {
            clickIdScroll(R.id.addCreditCardPaymentSetGoal);
            setDateWithId(R.id.addCreditCardPaymentInputGoal, goalOffset);
        }

        clickIdScroll(R.id.addCreditCardPaymentSubmitButton);
    }

    private void addSavingsAccount(String name, String value, boolean quicklook, boolean setGoal, String goalAmount, int goalOffset,
                                   String frquency, String contrAmount, String sourceAccount, int nextOffset, int endOffset, boolean ongoing)
    {
        typeTextIdScroll(R.id.addSavingsAccountName, name);
        typeTextIdScroll(R.id.addSavingsCurrentValue, value);

        if (quicklook)
        {
            clickIdScroll(R.id.addSavingsQuicklookCheckbox);
        }

        if (setGoal)
        {
            clickIdScroll(R.id.addSavingsSetGoal);
        }

        if (goalAmount != null)
        {
            typeTextIdScroll(R.id.addSavingsGoalAmount, goalAmount);
        }

        if (goalOffset != -1)
        {
            setDateWithId(R.id.addSavingsGoalDate, goalOffset);
        }

        selectSpinnerItemInId(R.id.addSavingsContributionFrequency, frquency);
        typeTextIdScroll(R.id.addSavingsContributionAmount, contrAmount);
        selectSpinnerItemInId(R.id.addSavingsSourceAccount, sourceAccount);
        setDateWithId(R.id.addSavingsNextDate, nextOffset);

        if (ongoing)
        {
            clickIdScroll(R.id.addSavingsOngoingCheckbox);
        }
        else
        {
            if (!setGoal) {
                setDateWithId(R.id.addSavingsEndDate, endOffset);
            }
        }

        if (setGoal)
        {
            clickIdScroll(R.id.addSavingsCleanButton);
        }

        clickIdScroll(R.id.addSavingsSubmitButton);
    }

    private void addAccount(String name, String value, boolean quicklook, boolean setupAsSavings)
    {
        typeTextId(R.id.inputAccountName, name);
        typeTextId(R.id.inputAccountAmount, value);

        if (quicklook) {
            clickId(R.id.checkboxQuicklook);
        }
        if (setupAsSavings)
        {
            clickId(R.id.setupPaymentCheckbox);
        }

        clickId(R.id.submitButton);
    }

    private ViewInteraction selectSpinnerItemInId(int id, String itemText)
    {
        onView(withId(id)).perform(scrollTo()).perform(click());
        return onData(allOf(is(instanceOf(String.class)), is(itemText))).perform(click());
    }

    private ViewInteraction setDateWithId(int id, int offset)
    {
        Calendar today = Calendar.getInstance();
        today.add(Calendar.DAY_OF_YEAR, offset);

        onView(withId(id)).perform(scrollTo()).perform(click());
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName()))).perform(PickerActions.setDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH )+1, today.get(Calendar.DAY_OF_MONTH)));
        return onView(withId(android.R.id.button1)).perform(click());
    }

    private ViewInteraction typeTextId(int id, String text)
    {
        return onView(withId(id))
                .perform(typeText(text));
    }

    private ViewInteraction clickId(int id)
    {
        return onView(withId(id)).perform(click());
    }

    private ViewInteraction clickText(String text)
    {
        return onView(withText(text)).perform(click());
    }


    private ViewInteraction clickIdScroll(int id)
    {
        onView(withId(id)).perform(scrollTo());
        return clickId(id);
    }

    private ViewInteraction typeTextIdScroll(int id, String text)
    {
        onView(withId(id)).perform(scrollTo());
        return typeTextId(id, text);
    }
}
