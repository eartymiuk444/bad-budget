package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.R;

import java.util.List;

/**
 * Custom List Adapter for our Tracker History. Displays each history entry inflating
 * the tracker_history_single
 * Created by Erik Artymiuk on 10/31/2016.
 */
public class TrackerHistoryListAdapter extends ArrayAdapter<TrackerHistoryItem> {
    /**
     * Overriden constructor. Doesn't accept a resource id as the resoure id is fixed for this
     * adapter. (R.layout.tracker_history_single)
     *
     * @param context - The context the list items will be shown in
     * @param items   - a list of the tracker history items to display
     */
    public TrackerHistoryListAdapter(Context context, List<TrackerHistoryItem> items) {
        super(context, -1, items);
    }

    /**
     * Overriden getView method. Inflates the R.layout.tracker_history_single layout resource for
     * each tracker history item in our list of items.(or resuses an already inflated view)
     *
     * @param position    - position of item to inflate
     * @param convertView - potential view to reuse (should be checked if compatible and non-null)
     * @param parent      - the parent the view will eventually be attached to (note attachment doesn't occur
     *                    here)
     * @return
     */
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.tracker_history_single, null);
        }

        TrackerHistoryItem p = getItem(position);

        if (p != null) {
            TextView budgetItemDescriptionView = (TextView) v.findViewById(R.id.tracker_history_item_description);
            TextView dayTimeView = (TextView) v.findViewById(R.id.tracker_history_day_time);
            TextView actionView = (TextView) v.findViewById(R.id.tracker_history_action_description);
            TextView dateView = (TextView) v.findViewById(R.id.tracker_history_date);

            String descriptionString = p.getBudgetItemDescription();
            if (p.getUserTransactionDescription() != null && !p.getUserTransactionDescription().equals("")) {
                descriptionString = p.getUserTransactionDescription() + " (" + descriptionString + ")";
            }
            String dayTimeString = p.getDayString() + " " + p.getTimeString();
            String dateString = p.getDateString();
            String actionString = TrackerHistoryItem.convertTrackerActionToString(p.getAction()) + " " + Double.toString(p.getActionAmount()) + " " + "(" + p.getOriginalBudgetAmount() + "->" +
                    p.getUpdatedBudgetAmount() + ")";
            budgetItemDescriptionView.setText(descriptionString);
            dayTimeView.setText(dayTimeString);
            dateView.setText(dateString);
            actionView.setText(actionString);
        }

        return v;
    }
}
