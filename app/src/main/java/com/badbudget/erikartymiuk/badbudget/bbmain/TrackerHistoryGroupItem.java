package com.badbudget.erikartymiuk.badbudget.bbmain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a grouping of budget item transactions over a time period. Groups items by
 * the budget item description TODO 5/3/2017 or the user description.
 *
 * Created by eartymiuk on 4/29/2017.
 */
public class TrackerHistoryGroupItem {

    //Possible grouping time periods
    public enum GroupingType {
        daily,
        weekly,
        monthly,
        yearly,
        all_time
    }

    private GroupingType groupingType;

    //The start date of this group
    private Date startDate;
    //The end date inclusive of this group
    private Date endDate;

    //The cumulative budget items in this group
    private Map<String, TrackerHistoryGroupBudgetItem> groupBudgetItemsMap;

    /**
     * Constructor for a TrackerHistoryGroupItem
     * @param groupingType - the time period grouping of items in this group
     * @param startDate - the start date of the grouping
     * @param endDate - the end date of the grouping (inclusive)
     */
    public TrackerHistoryGroupItem(GroupingType groupingType, Date startDate, Date endDate)
    {
        this.groupingType = groupingType;
        this.startDate = startDate;
        this.endDate = endDate;

        groupBudgetItemsMap = new HashMap<>();
    }

    /**
     * Getter for the budget items map for this grouping.
     * @return - a map of budget item descriptions to the cumulative group budget item transactions
     *              over the groups time period.
     */
    public Map<String, TrackerHistoryGroupBudgetItem> getGroupBudgetItemsMap()
    {
        return this.groupBudgetItemsMap;
    }

    /**
     * Start date getter
     * @return start date of groups time period
     */
    public Date getStartDate()
    {
        return this.startDate;
    }

    /**
     * End date getter
     * @return end date of groups time period (inclusive)
     */
    public Date getEndDate()
    {
        return this.endDate;
    }

    /**
     * Returns a list of this groups budget items sorted alphabetically.
     * @return - a sorted list of this group's budget items (TrackerHistoryGroupBudgetItems)
     */
    public List<TrackerHistoryGroupBudgetItem> getSortedGroupBudgetItems()
    {
        //Need to sort group's list of budget items alphabetically
        Comparator<TrackerHistoryGroupBudgetItem> comparator = new Comparator<TrackerHistoryGroupBudgetItem>() {
            @Override
            public int compare(TrackerHistoryGroupBudgetItem lhs, TrackerHistoryGroupBudgetItem rhs) {

                boolean lhsUserDescript = lhs.getUserTransactionGroupDescription() != null && !lhs.getUserTransactionGroupDescription().equals("");
                boolean rhsUserDescript = rhs.getUserTransactionGroupDescription() != null && !rhs.getUserTransactionGroupDescription().equals("");

                if (lhsUserDescript && rhsUserDescript)
                {
                    return lhs.getUserTransactionGroupDescription().compareTo(rhs.getUserTransactionGroupDescription());
                }
                else if (lhsUserDescript && !rhsUserDescript)
                {
                    return lhs.getUserTransactionGroupDescription().compareTo(rhs.getBudgetItemGroupDescription());
                }
                else if (!lhsUserDescript && rhsUserDescript)
                {
                    return lhs.getBudgetItemGroupDescription().compareTo(rhs.getUserTransactionGroupDescription());
                }
                else
                {
                    return lhs.getBudgetItemGroupDescription().compareTo(rhs.getBudgetItemGroupDescription());
                }
            }
        };

        ArrayList sortedList = new ArrayList(groupBudgetItemsMap.values());
        Collections.sort(sortedList, comparator);
        return sortedList;
    }
}
