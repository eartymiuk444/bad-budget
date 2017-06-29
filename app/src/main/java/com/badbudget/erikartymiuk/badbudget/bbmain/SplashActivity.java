package com.badbudget.erikartymiuk.badbudget.bbmain;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.badbudget.erikartymiuk.badbudget.R;

/**
 * Splash activity that displays a background image as the initial setup task is run.
 * This is the activity that should run when we need to initialize our bad budget data.
 * Created by Erik Artymiuk on 6/29/2017.
 */

public class SplashActivity extends AppCompatActivity
{
    private static final long SPLASH_DISPLAY_TIME = 100;

    /**
     * On create for the SplashActivity. First checks the database to see if the user accepted the Eula.
     * Displays the eula if not or runs the setup task if they did. If the user agrees to a displayed Eula
     * the setup task is immediately run. If they cancel the Eula the SplashActivity finishes.
     * @param savedInstanceState
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        final BadBudgetSetupTask setupTask = new BadBudgetSetupTask(this);

        int agreedEulaInt = BBDatabaseContract.getEulaAgreeStatus(BBDatabaseOpenHelper.getInstance(this).getWritableDatabase());
        if (agreedEulaInt == BBDatabaseOpenHelper.EULA_AGREED || agreedEulaInt == BBDatabaseOpenHelper.EULA_EXTREME_EARLY_ADOPT)
        {
            setupTask.execute();
        }
        else
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.eula_agreement)
                    .setPositiveButton(R.string.eula_agree, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            BBDatabaseContract.setEulaAgreeStatus(
                                    BBDatabaseOpenHelper.getInstance(SplashActivity.this)
                                            .getWritableDatabase(), BBDatabaseOpenHelper.EULA_AGREED);
                            setupTask.execute();
                        }
                    })
                    .setNegativeButton(R.string.eula_cancel, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            SplashActivity.this.finish();
                        }
                    }).setCancelable(false);
            builder.create().show();
        }
    }

    /**
     * Callback method invoked after the setup task has completed. Starts off the home activity
     * and finishes the splash activity.
     */
    public void setupTaskComplete()
    {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                Intent intent = new Intent(SplashActivity.this,
                        HomeActivity.class);

                SplashActivity.this.startActivity(intent);
                SplashActivity.this.finish();

                overridePendingTransition(android.R.anim.fade_in , android.R.anim.fade_out);
            }
        },SPLASH_DISPLAY_TIME);
    }
}
