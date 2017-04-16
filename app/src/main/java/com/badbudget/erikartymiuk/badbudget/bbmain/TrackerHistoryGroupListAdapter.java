package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.R;
import com.erikartymiuk.badbudgetlogic.main.Prediction;

import java.util.List;

/**
 * TrackerHistoryGroupListAdapter extends ArrayAdapter to be used as the backing of a list view.
 * Its items are TrackerHistoryGroupItems where each spans a time period and in itself contains
 * a list of budget groupings for its time period.
 */
public class TrackerHistoryGroupListAdapter extends ArrayAdapter<TrackerHistoryGroupItem> {

    private static final String SUBTRACTED = "Subtracted";
    private static final String ADDED = "Added";
    private static final String AUTO_RESET = "Auto Reset";
    private static final String USER_RESET = "User Reset";

    private boolean hideResets;

    /**
     * TrackerHistoryGroupListAdapter constructor.
     * @param context - context where list view this adapter backs is being shown
     * @param items - the TrackerHistoryGroupItems backing this adapter
     * @param hideResets - indicates if the reset fields should be hidden when this adapter shows
     *                   its items.
     */
    public TrackerHistoryGroupListAdapter(Context context, List<TrackerHistoryGroupItem> items, boolean hideResets) {
        super(context, -1, items);
        this.hideResets = hideResets;
    }

    /**
     * Overridden getView. Constructs or reuses the layout for single_tracker_history_group
     * which represents a time period with a list of budget item groups
     * @param position - the position of the card/time period to construct the view for
     * @param convertView - a potential view to reuse. should be of type single_tracker_hisotry_group
     * @param parent - unused (see ArrayAdapter)
     * @return - the view to show at the specified position
     */
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null)
        {
            LayoutInflater vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.single_tracker_history_group, null);
        }
        else
        {
            //Going to reuse a view. Should clear the title and remove any old budget groups
            TextView groupTitle = (TextView)v.findViewById(R.id.groupTitle);
            LinearLayout groupBudgetItems = (LinearLayout) v.findViewById(R.id.singleTrackerHistoryGroupingCardList);
            groupTitle.setText("");
            groupBudgetItems.removeAllViews();
        }

        TrackerHistoryGroupItem p = getItem(position);

        if (p != null)
        {
            //Set the title string
            TextView groupTitle = (TextView)v.findViewById(R.id.groupTitle);
            String titleString;
            if (p.getStartDate() == null && p.getEndDate() == null)
            {
                titleString = this.getContext().getString(R.string.tracker_history_all_time);
            }
            else if (Prediction.datesEqualUpToDay(p.getStartDate(), p.getEndDate()))
            {
                titleString = BadBudgetApplication.dateString(p.getStartDate());
            }
            else
            {
                titleString = BadBudgetApplication.dateString(p.getStartDate()) + " - " + BadBudgetApplication.dateString(p.getEndDate());
            }
            groupTitle.setText(titleString);

            //Populate the budget item groupings for this positions time period
            LinearLayout groupBudgetItems = (LinearLayout) v.findViewById(R.id.singleTrackerHistoryGroupingCardList);
            for (TrackerHistoryGroupBudgetItem groupBudgetItem : p.getSortedGroupBudgetItems())
            {
                LayoutInflater vi = LayoutInflater.from(getContext());
                View groupBudgetItemView = vi.inflate(R.layout.tracker_history_single_group_budget_item, null);

                TextView groupDescription = (TextView)groupBudgetItemView.findViewById(R.id.tracker_group_history_group_description);
                TextView change = (TextView)groupBudgetItemView.findViewById(R.id.tracker_group_history_change);
                TextView autoReset = (TextView)groupBudgetItemView.findViewById(R.id.tracker_group_history_auto_reset);
                TextView userReset = (TextView)groupBudgetItemView.findViewById(R.id.tracker_group_history_user_reset);

                if (groupBudgetItem.getUserTransactionGroupDescription() != null &&
                        !groupBudgetItem.getUserTransactionGroupDescription().equals(""))
                {
                    groupDescription.setText(groupBudgetItem.getUserTransactionGroupDescription());
                }
                else
                {
                    groupDescription.setText(groupBudgetItem.getBudgetItemGroupDescription());
                }

                if (groupBudgetItem.getChangeAmount() <= 0)
                {
                    double absChangeAmt = Math.abs(groupBudgetItem.getChangeAmount());
                    change.setText(SUBTRACTED + " " + BadBudgetApplication.roundedDoubleBB(absChangeAmt));
                }
                else
                {
                    change.setText(ADDED + " " + BadBudgetApplication.roundedDoubleBB(groupBudgetItem.getChangeAmount()));
                }

                if (hideResets)
                {
                    LinearLayout resetsLinearLayout = (LinearLayout)groupBudgetItemView.findViewById(R.id.linearLayoutResets);
                    resetsLinearLayout.setVisibility(View.GONE);
                }
                else
                {
                    autoReset.setText(AUTO_RESET + " " + BadBudgetApplication.roundedDoubleBB(groupBudgetItem.getAutoResetLoss()));
                    userReset.setText(USER_RESET + " " + BadBudgetApplication.roundedDoubleBB(groupBudgetItem.getUserResetLoss()));
                }

                groupBudgetItems.addView(groupBudgetItemView);
            }
        }

        return v;
    }
}
