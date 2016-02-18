/*
 * Copyright 2014-2016 Appvelopers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.appvelopers.mcgillmobile.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.guerinet.utils.Utils;
import com.guerinet.utils.prefs.BooleanPreference;
import com.guerinet.utils.prefs.IntPreference;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ca.appvelopers.mcgillmobile.App;
import ca.appvelopers.mcgillmobile.R;
import ca.appvelopers.mcgillmobile.model.ConnectionStatus;
import ca.appvelopers.mcgillmobile.model.Semester;
import ca.appvelopers.mcgillmobile.model.Term;
import ca.appvelopers.mcgillmobile.model.exception.MinervaException;
import ca.appvelopers.mcgillmobile.model.prefs.PasswordPreference;
import ca.appvelopers.mcgillmobile.model.prefs.PrefsModule;
import ca.appvelopers.mcgillmobile.model.prefs.UsernamePreference;
import ca.appvelopers.mcgillmobile.ui.dialog.DialogHelper;
import ca.appvelopers.mcgillmobile.ui.settings.AgreementActivity;
import ca.appvelopers.mcgillmobile.util.Analytics;
import ca.appvelopers.mcgillmobile.util.Constants;
import ca.appvelopers.mcgillmobile.util.Parser;
import ca.appvelopers.mcgillmobile.util.Test;
import ca.appvelopers.mcgillmobile.util.Update;
import ca.appvelopers.mcgillmobile.util.manager.HomepageManager;
import ca.appvelopers.mcgillmobile.util.manager.McGillManager;
import ca.appvelopers.mcgillmobile.util.storage.Clear;
import ca.appvelopers.mcgillmobile.util.thread.ConfigDownloader;
import dagger.Lazy;
import timber.log.Timber;

/**
 * First activity that is opened when the app is started
 * @author Julien Guerinet
 * @since 1.0.0
 */
public class SplashActivity extends BaseActivity {
    /**
     * Code used when starting the AgreementActivity
     */
    private static final int AGREEMENT_CODE = 100;
    /**
     * Container if the min version is not satisfied
     */
    @Bind(R.id.version_container)
    protected LinearLayout minVersionContainer;
    /**
     * Container with the login views
     */
    @Bind(R.id.login_container)
    protected LinearLayout loginContainer;
    /**
     * The login {@link Button}
     */
    @Bind(R.id.login_button)
    protected Button loginButton;
    /**
     * {@link EditText} where the user enters their username
     */
    @Bind(R.id.login_username)
    protected EditText usernameView;
    /**
     * {@link EditText} where the user enters their password
     */
    @Bind(R.id.login_password)
    protected EditText passwordView;
    /**
     * {@link CheckBox} where the user decides if their username should be remembered
     */
    @Bind(R.id.login_remember_username)
    protected CheckBox rememberUsername;
    /**
     * The {@link McGillManager} instance
     */
    @Inject
    protected McGillManager mcGillManager;
    /**
     * Hide loading {@link BooleanPreference}
     */
    @Inject
    @Named(PrefsModule.HIDE_LOADING)
    protected BooleanPreference hideLoadingPref;
    /**
     * Remember username {@link BooleanPreference}
     */
    @Inject
    @Named(PrefsModule.REMEMBER_USERNAME)
    protected BooleanPreference rememberUsernamePref;
    /**
     * Version {@link IntPreference}
     */
    @Inject
    @Named(PrefsModule.VERSION)
    protected IntPreference versionPref;
    /**
     * Min version {@link IntPreference}
     */
    @Inject
    @Named(PrefsModule.MIN_VERSION)
    protected IntPreference minVersionPref;
    /**
     * EULA {@link BooleanPreference}
     */
    @Inject
    @Named(PrefsModule.EULA)
    protected BooleanPreference eulaPref;
    /**
     * {@link UsernamePreference} instance
     */
    @Inject
    protected UsernamePreference usernamePref;
    /**
     * {@link PasswordPreference} instance
     */
    @Inject
    protected PasswordPreference passwordPref;
    /**
     * The {@link HomepageManager} instance
     */
    @Inject
    protected HomepageManager homepageManager;
    /**
     * The {@link InputMethodManager}
     */
    @Inject
    protected Lazy<InputMethodManager> imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);
        App.component(this).inject(this);

        //Run the update code, if any
        Update.update(this, versionPref);

        //Start downloading the config
        new ConfigDownloader(this).start();

        if (!eulaPref.get()) {
            //If the user has not accepted the EULA, show it before continuing
            Intent intent = new Intent(this, AgreementActivity.class)
                    .putExtra(Constants.EULA_REQUIRED, true);
            startActivityForResult(intent, AGREEMENT_CODE);
        } else {
            //If they have, go to the next screen
            showNextScreen();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        switch (requestCode) {
            case AGREEMENT_CODE:
                if (resultCode == RESULT_OK) {
                    //If they agreed, run the config downloader
                    showNextScreen();
                } else {
                    //If not, close the app
                    finish();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    /**
     * Shows the first screen to the user depending on their situation
     */
    private void showNextScreen() {
        if (minVersionPref.get() > Utils.versionCode(this)) {
            //If we don't have the min required version, show the right container
            minVersionContainer.setVisibility(View.VISIBLE);
        } else if (usernamePref.get() == null || passwordPref.get() == null) {
            //If we are missing some login info, show the login screen with no error message
            showLoginScreen((ConnectionStatus) getIntent()
                    .getSerializableExtra(Constants.CONNECTION_STATUS));
        } else {
            //Try logging the user in and download their info
            new AppInitializer(false).execute();
        }
    }

    private void showLoginScreen(ConnectionStatus error) {
        //Move the logo to the top
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        View logo = findViewById(R.id.logo);
        logo.setLayoutParams(params);

        //Show the login container
        final LinearLayout loginContainer = (LinearLayout)findViewById(R.id.login_container);
        loginContainer.setVisibility(View.VISIBLE);

        //Delete of the previous user's info
        Clear.all(rememberUsernamePref, usernamePref, passwordPref, homepageManager);

        //Fill out username text if it is present
        if (usernamePref.get() != null) {
            usernameView.setText(usernamePref.get());
        }

        //Set it to that when clicking the IME Action button it tries to log you in directly
        passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    loginButton.performClick();
                    return true;
                }
                return false;
            }
        });

        //Remember Me box checked based on user's previous preference
        rememberUsername.setChecked(rememberUsernamePref.get());

        //Check if an error message needs to be displayed, display it if so
        if (error != null) {
            DialogHelper.error(this, error.getErrorStringId());
        }

        Analytics.get().sendScreen("Login");
    }

    /**
     * Initializes the app by logging the user in and downloading the required information
     */
    public class AppInitializer extends AsyncTask<Void, String, Void>{
        /**
         * True if the user has already logged in (first open), false otherwise
         */
        private boolean mLoggedIn;
        /**
         * The connection status
         */
        private ConnectionStatus mConnectionStatus;
        /**
         * True if we need to download everything (first open), false otherwise
         */
        private boolean mDownloadEverything;
        /**
         * The loading container
         */
        private LinearLayout mLoadingContainer;
        /**
         * The TextView showing the progress
         */
        private TextView mProgressTextView;
        /**
         * The skip button
         */
        private Button mSkip;
        /**
         * True if a bug was found, false otherwise
         */
        private boolean mBugPresent = false;
        /**
         * True if it was a transcript bug, false if it was a schedule bug
         */
        private boolean mTranscriptBug = false;
        /**
         * The term that the bug was found in, if any
         */
        private String mTermBug;

        //The passed boolean is true when they sign in for the first time,
        //  false when it's on auto-login.

        /**
         * Default Constructor
         *
         * @param loggedIn True if the user has already logged in (first open), false otherwise
         */
        public AppInitializer(boolean loggedIn){
            this.mLoggedIn = loggedIn;
        }

        @Override
        protected void onPreExecute(){
            //Check if we need to download everything or only the essential stuff
            //We need to download everything if there is null info or if we are forcing a reload
            mDownloadEverything = App.getTranscript() == null || App.getUser() == null ||
                    App.getEbill() == null;

            //Loading Container
            mLoadingContainer = (LinearLayout)findViewById(R.id.loading_container);
            mLoadingContainer.setVisibility(View.VISIBLE);

            //Progress dialog
            mProgressTextView = (TextView)findViewById(R.id.loading_title);
            //Reset the text (if it was set during a previous login attempt
            mProgressTextView.setText("");

            //Skip button
            mSkip = (Button)findViewById(R.id.skip);
            //If we are not downloading everything, show the skip button
            if(!mDownloadEverything){
                mSkip.setVisibility(View.VISIBLE);
            }
            mSkip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    //If it's already been cancelled, no need to skip it again
                    if(isCancelled()){
                        return;
                    }

                    //If the user has checked the "Do Not Show" option previously, skip directly
                    if (hideLoadingPref.get()) {
                        publishNewProgress(getString(R.string.skipping));
                        cancel(false);
                        return;
                    }

                    //If no, show the explanation dialog. Inflate the layout, bind the checkbox
                    View layout = View.inflate(SplashActivity.this, R.layout.dialog_checkbox, null);
                    final CheckBox doNotShow = (CheckBox)layout.findViewById(R.id.skip);

                    //Set up the dialog
                    new AlertDialog.Builder(SplashActivity.this)
                            .setCancelable(false)
                            .setView(layout)
                            .setTitle(getString(R.string.warning))
                            .setMessage(getString(R.string.skip_loading))
                            .setPositiveButton(getString(android.R.string.yes),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which){
                                            //Save the do not show option
                                            hideLoadingPref.set(doNotShow.isChecked());

                                            //Cancel the info downloader
                                            publishNewProgress(getString(R.string.skipping));
                                            cancel(false);

                                            dialog.dismiss();
                                        }
                                    })
                            .setNegativeButton(getString(android.R.string.no),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which){
                                            //Save the do not show option
                                            hideLoadingPref.set(doNotShow.isChecked());
                                            dialog.dismiss();
                                        }
                                    })
                            .create().show();
                }
            });
        }

        @Override
        protected Void doInBackground(Void... params) {
            Analytics.get().sendEvent("Splash", "Auto-Login", "true");

            //Set up a while loop to go through everything while checking if the user cancelled
            //  every time
            int downloadIndex = 0;
            downloadLoop: while(!isCancelled()){
                //Use a switch to figure out what to download next based on the index
                switch(downloadIndex){
                    //Log him in
                    case 0:
                        publishNewProgress(getString(R.string.logging_in));

                        //If he's already logged in, the connection is OK
                        mConnectionStatus = mLoggedIn ? ConnectionStatus.OK : mcGillManager.login();

                        //If we did not connect, break the loop now
                        if(mConnectionStatus != ConnectionStatus.OK){
                            break downloadLoop;
                        }
                        break;
                    //Transcript
                    case 1:
                        publishNewProgress(getString(mDownloadEverything ?
                                R.string.downloading_transcript : R.string.updating_transcript));

                        //Download the transcript
                        try{
                            String transcriptBug;
                            if(Test.LOCAL_TRANSCRIPT){
                                transcriptBug = Test.testTranscript();
                            }
                            else{
                                transcriptBug = Parser.parseTranscript(
                                        mcGillManager.get(mcGillService.transcript()));
                            }
                            //If there was an error, show it
                            if(transcriptBug != null){
                                reportBug(true, transcriptBug);
                            }

                        } catch(MinervaException e){
                            //Set the connection status and break the loop
                            mConnectionStatus = ConnectionStatus.WRONG_INFO;
                            break downloadLoop;
                        } catch(Exception e){
                            //IOException or no internet - continue in any case
                            Timber.e(e, "");
                        }
                        break;
                    //Semesters
                    case 2:
                        String scheduleBug = null;

                        //Test mode : only one semester to do
                        if(Test.LOCAL_SCHEDULE){
                            scheduleBug = Test.testSchedule();
                        }
                        else {
                            //List of semesters
                            List<Semester> semesters = App.getTranscript().getSemesters();
                            //The current term
                            Term currentTerm = Term.getCurrentTerm();

                            //Go through the semesters
                            for(Semester semester: semesters){
                                //If the AsyncTask was cancelled, stop everything
                                if (isCancelled()) {
                                    break downloadLoop;
                                }

                                //Get the term of this semester
                                Term term = semester.getTerm();

                                //If we are not downloading everything, only download it if it's the
                                //  current or future term
                                if(mDownloadEverything || term.equals(currentTerm) ||
                                        term.isAfter(currentTerm)){
                                    publishNewProgress(getString(mDownloadEverything ?
                                                    R.string.downloading_semester :
                                                    R.string.updating_semester,
                                            term.getString(SplashActivity.this)));

                                    //Download the schedule
                                    try{
                                        scheduleBug = Parser.parseCourses(term,
                                                mcGillManager.get(mcGillService.schedule(term)));
                                    } catch(MinervaException e){
                                        //Set the connection status and break the loop
                                        mConnectionStatus = ConnectionStatus.WRONG_INFO;
                                        break downloadLoop;
                                    } catch(Exception e){
                                        //IOException or no internet - continue in any case
                                    }
                                }
                            }

                            //Set the default term if there is none set yet
                            if(App.getDefaultTerm() == null){
                                App.setDefaultTerm(currentTerm);
                            }
                        }

                        //If there was an error, show it
                        if(scheduleBug != null){
                            reportBug(false, scheduleBug);
                        }
                        break;
                    //Ebill + user info
                    case 3:
                        //Ebill
                        publishNewProgress(getString(mDownloadEverything ?
                                R.string.downloading_ebill : R.string.updating_ebill));

                        //Download the eBill and user info
                        try{
                            String ebillString = mcGillManager.get(mcGillService.ebill());
                            Parser.parseEbill(ebillString);
                        } catch(MinervaException e){
                            //Set the connection status and break the loop
                            mConnectionStatus = ConnectionStatus.WRONG_INFO;
                            break downloadLoop;
                        } catch(Exception e){
                            //IOException or no internet - continue in any case
                        }
                        break;
                    //We've reached the end, break the loop
                    default:
                        break downloadLoop;
                }

                //Increment the download index
                downloadIndex ++;
            }
            return null;
        }

        public void publishNewProgress(String title){
            publishProgress(title);
        }

        @Override
        protected void onProgressUpdate(String... progress){
            //Update the TextView
            mProgressTextView.setText(progress[0]);
        }

        @Override
        protected void onCancelled(){
            onPostExecute(null);
        }

        @Override
        protected void onPostExecute(Void result) {
            //Hide the container
            mLoadingContainer.setVisibility(View.GONE);
            //Hide the skip button
            mSkip.setVisibility(View.GONE);

            //Connection successful: home page
            if(mConnectionStatus == ConnectionStatus.OK ||
                    mConnectionStatus == ConnectionStatus.NO_INTERNET){

                Intent intent = new Intent(SplashActivity.this, homepageManager.getActivity());
                //If there's a bug, add it to the intent
                if(mBugPresent){
                    intent.putExtra(Constants.BUG, mTranscriptBug ? Constants.TRANSCRIPT : "")
                        .putExtra(Constants.TERM, mTermBug);
                }
                startActivity(intent);
                finish();
            }
            //Connection not successful : login
            else{
                showLoginScreen(mConnectionStatus);
            }
        }

        /**
         * Reports a bug in the parsing of the transcript or schedule
         *
         * @param transcript True if it was the transcript, false otherwise
         * @param term       The term that the bug was in, if applicable
         */
        private void reportBug(boolean transcript, String term){
            mBugPresent = true;
            mTranscriptBug = transcript;
            mTermBug = term;
        }
    }

    /**
     * Called when the min version button is clicked
     */
    @OnClick(R.id.version_button)
    protected void downloadNewVersion() {
        //Redirect them to the play store
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse(Constants.PLAY_STORE_LINK));
        startActivity(intent);
    }

    @OnClick(R.id.login_button)
    protected void login() {
        //Hide the keyboard
        usernameView.clearFocus();
        imm.get().hideSoftInputFromWindow(usernameView.getWindowToken(), 0);

        //Get the inputted username and password
        final String username = usernameView.getText().toString().trim();
        final String password = passwordView.getText().toString().trim();

        //Check that both of them are not empty, create appropriate error messages if so
        if (TextUtils.isEmpty(username)) {
            DialogHelper.error(this, R.string.login_error_username_empty);
            return;
        } else if (TextUtils.isEmpty(password)) {
            DialogHelper.error(this, R.string.login_error_password_empty);
            return;
        }

        //TODO Replace this with spinner on page
        final ProgressDialog progressDialog = new ProgressDialog(SplashActivity.this);
        progressDialog.setMessage(getString(R.string.please_wait));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                final ConnectionStatus status = mcGillManager
                        .login(username + getString(R.string.login_email), password);
                // If the connection was successful, start the app initializer
                if (status == ConnectionStatus.OK) {
                    // Store the login info.
                    usernamePref.set(username);
                    passwordPref.set(password);
                    rememberUsernamePref.set(rememberUsername.isChecked());
                    Analytics.get().sendEvent("Login", "Remember Username",
                            String.valueOf(rememberUsername.isChecked()));

                    //set the background receiver after successful login
//                            if(!App.isAlarmActive()){
//                            	App.SetAlarm(LoginActivity.this);
//                            }

                    //Dismiss the progress dialog
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();

                            //Hide the login container
                            loginContainer.setVisibility(View.GONE);

                            //Start the downloading of information
                            new AppInitializer(true).execute();
                        }
                    });
                } else {
                    //Else show error dialog
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Analytics.get().sendEvent("Login", "Login Error", status.getGAString());
                            progressDialog.dismiss();
                            DialogHelper.error(SplashActivity.this, status.getErrorStringId());
                        }
                    });
                }
            }
        }).start();
    }
}