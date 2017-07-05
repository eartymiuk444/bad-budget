package com.badbudget.erikartymiuk.bbmain;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.RelativeLayout;

import com.badbudget.erikartymiuk.badbudget.R;
import com.badbudget.erikartymiuk.badbudget.bbmain.BadBudgetApplication;
import com.badbudget.erikartymiuk.badbudget.bbmain.SelectBudgetDialog;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

/**
 * Class that contains flavor specific code for one of our flavors of bad budget. This
 * is the code for the free flavor of the bb app.
 * Created by Erik Artymiuk on 3/26/2017.
 */
public class FlavorSpecific {

    /**
     * This method initializes mobile ads for the free flavor of the bb app.
     * @param application - the application instance we are enabling ads in
     */
    public static void enableAds(BadBudgetApplication application)
    {
        MobileAds.initialize(application.getApplicationContext(), application.getString(R.string.ad_mob_app_id));
    }

    /**
     * This method loads an ad into the passed activity. It assumes that the id of relative layout containing
     * the ad, 'adRelativeLayout' is present. Sets the emulator device as a test device.
     * @param activity - the activity to load the ad on. Should have "adRelativeLayout" as the ID of a relative layout
     *                      suitable for containing the ad in the activity's content layout file.
     * @param layoutId - the layout id of the content that is having the ad loaded into it. Used to
     *                                      determine the appropriate ad unit id.
     */
    public static void adRequest(Activity activity, int layoutId)
    {
        RelativeLayout adContainer = (RelativeLayout)activity.findViewById(R.id.adRelativeLayout);
        AdView adView = new AdView(activity);
        adView.setAdSize(AdSize.SMART_BANNER);
        switch (layoutId)
        {
            case R.layout.content_cash:
            {
                adView.setAdUnitId(activity.getString(R.string.bb_cash_table_ad));
                break;
            }
            case R.layout.content_savings:
            {
                adView.setAdUnitId(activity.getString(R.string.bb_savings_table_ad));
                break;
            }
            case R.layout.content_credit_cards:
            {
                adView.setAdUnitId(activity.getString(R.string.bb_debts_table_ad));
                break;
            }
            case R.layout.content_loans:
            {
                adView.setAdUnitId(activity.getString(R.string.bb_debts_table_ad));
                break;
            }
            case R.layout.content_misc:
            {
                adView.setAdUnitId(activity.getString(R.string.bb_debts_table_ad));
                break;
            }
            case R.layout.content_gains:
            {
                adView.setAdUnitId(activity.getString(R.string.bb_gains_table_ad));
                break;
            }
            case R.layout.content_losses:
            {
                adView.setAdUnitId(activity.getString(R.string.bb_losses_table_ad));
                break;
            }
            case R.layout.content_budget_set:
            {
                adView.setAdUnitId(activity.getString(R.string.bb_budget_table_ad));
                break;
            }
            case R.layout.content_track:
            {
                adView.setAdUnitId(activity.getString(R.string.bb_tracker_main_ad));
                break;
            }
            case R.layout.content_tracker_history:
            {
                adView.setAdUnitId(activity.getString(R.string.bb_tracker_history_ad));
                break;
            }
            case R.layout.content_predict:
            {
                adView.setAdUnitId(activity.getString(R.string.bb_predict_ad));
                break;
            }
            case R.layout.content_detailed_look_home:
            {
                adView.setAdUnitId(activity.getString(R.string.bb_detailed_look_home_ad));
                break;
            }
            case R.layout.content_detailed_look_cash:
            {
                adView.setAdUnitId(activity.getString(R.string.bb_detailed_look_cash_ad));
                break;
            }
            case R.layout.content_detailed_look_debts:
            {
                adView.setAdUnitId(activity.getString(R.string.bb_detailed_look_debts_ad));
                break;
            }
            case R.layout.content_detailed_look_cash_history:
            {
                adView.setAdUnitId(activity.getString(R.string.bb_detailed_look_cash_history_ad));
                break;
            }
            case R.layout.content_detailed_look_debts_history:
            {
                adView.setAdUnitId(activity.getString(R.string.bb_detailed_look_debts_history_ad));
                break;
            }
            case R.layout.content_summary:
            {
                adView.setAdUnitId(activity.getString(R.string.bb_summary_ad));
                break;
            }
            case R.layout.content_transfers:
                adView.setAdUnitId(activity.getString(R.string.bb_transfers_table_ad));
                break;
        }

        adContainer.addView(adView);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
        adView.loadAd(adRequest);
    }

    /**
     * Flavor specific method for constructing the click listener when the 'create new budget' button is pressed in the
     * select budget dialog. This is specific to the free flavor and simply shows a dialog indicating to the user
     * that this feature is only available in the full version.
     * @param sbd - the SelectBudgetDialog the create new budget click originated from
     * @return - a click listener to be attached to the "Create New Budget" button of the SelectBudgetDialog
     */
    public static DialogInterface.OnClickListener createNewBudgetClickListener(final SelectBudgetDialog sbd)
    {
        DialogInterface.OnClickListener ocl = new DialogInterface.OnClickListener() {
            /*
            The neutral button click is the 'create new' budget action where the user is taken to
            another dialog where they will have the option of creating a new budget. Either completely new
            or by copying an existing budget
             */
            public void onClick(DialogInterface dialog, int id)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(sbd.getActivity());
                builder.setMessage(R.string.require_full_version_message).setTitle(R.string.require_full_version_title);
                builder.setNeutralButton(R.string.acknowledge_button_string, null);
                AlertDialog requireFullVersionDialog = builder.create();
                requireFullVersionDialog.show();
            }
        };
        return ocl;
    }
}
