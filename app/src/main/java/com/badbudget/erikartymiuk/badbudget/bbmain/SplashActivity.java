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
    private static final long SPLASH_DISPLAY_TIME = 500;

    /**
     * On create for the SplashActivity. First checks the database to see if the user accepted the Eula.
     * Displays the eula if not or runs the setup task if they did. If the user agrees to a displayed Eula
     * the setup task is immediately run. If they cancel the Eula the SplashActivity finishes.
     * @param savedInstanceState
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        /*
        This check is to handle launchers where rather than resuming or recreating the activity that
        was running when the user left the application tries to start the launcher activity on top
        of the last activity.

        (See
         https://issuetracker.google.com/issues/36907463,
         https://stackoverflow.com/questions/19545889/app-restarts-rather-than-resumes,
         https://stackoverflow.com/questions/4341600/how-to-prevent-multiple-instances-of-an-activity-when-it-is-launched-with-differ/,
         https://issuetracker.google.com/issues/36941942).

        This should prevent another instance of that activity from being created and added to the stack.
         */
        if (!isTaskRoot()
                && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
                && getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_MAIN)) {

            finish();
            return;
        }

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

                overridePendingTransition(0, android.R.anim.fade_out);
            }
        },SPLASH_DISPLAY_TIME);
    }
}
