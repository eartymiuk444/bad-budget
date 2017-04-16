package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.badbudget.erikartymiuk.badbudget.R;
import com.erikartymiuk.badbudgetlogic.predictdataclasses.TransactionHistoryItem;

import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Custom List Adapter for our General History. Displays each history entry inflating
 * the budget_history_single
 * Created by Erik Artymiuk.
 */
public class GeneralHistoryListAdapter extends ArrayAdapter<TransactionHistoryItem>
{
    /**
     * Overriden constructor. Doesn't accept a resource id as the resoure id is fixed for this
     * adapter. (R.layout.budget_history_single)
     * @param context - The context the list items will be shown in
     * @param items - a list of the transaction history items to display
     */
    public GeneralHistoryListAdapter(Context context, List<TransactionHistoryItem> items)
    {
        super(context, -1, items);
    }

    /**
     * Overriden getView method. Inflates the R.layout.budget_history_single layout resource for
     * each transaction history item in our list of items.(or resuses an already inflated view)
     * @param position - position of item to inflate
     * @param convertView - potential view to reuse (should be checked if compatible and non-null)
     * @param parent - the parent the view will eventually be attached to (note attachment doesn't occur
     *               here)
     * @return
     */
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null)
        {
            LayoutInflater vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.budget_history_single, null);
        }

        TransactionHistoryItem historyItem = getItem(position);

        if (historyItem != null)
        {
            TextView historyDate = (TextView)v.findViewById(R.id.detailed_look_history_date);
            TextView transactionAmount = (TextView)v.findViewById(R.id.detailed_look_transaction_amount);
            TextView source = (TextView)v.findViewById(R.id.detailed_look_source_text);
            TextView destination = (TextView)v.findViewById(R.id.detailed_look_destination_text);

            historyDate.setText(BadBudgetApplication.dateString(historyItem.getTransactionDate()));
            transactionAmount.setText(BadBudgetApplication.roundedDoubleBB(historyItem.getTransactionAmount()));

            String sourceText = historyItem.getSourceActionString() + " " + historyItem.getTransactionSource();
            String destinationText = historyItem.getDestinationActionString() + " " + historyItem.getTransactionDestination();

            if (historyItem.isSourceCanShowChange())
            {
                sourceText = sourceText + " (" + BadBudgetApplication.roundedDoubleBB(historyItem.getSourceOriginal()) + "->" + BadBudgetApplication.roundedDoubleBB(historyItem.getSourceUpdated()) + ")";
            }
            if (historyItem.isDestinationCanShowChange())
            {
                destinationText = destinationText + " (" + BadBudgetApplication.roundedDoubleBB(historyItem.getDestinationOriginal()) + "->" + BadBudgetApplication.roundedDoubleBB(historyItem.getDestinationUpdated()) + ")";
            }

            source.setText(sourceText);
            destination.setText(destinationText);
        }

        return v;
    }
}
