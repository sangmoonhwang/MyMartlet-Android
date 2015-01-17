package ca.appvelopers.mcgillmobile.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.facebook.Session;

import ca.appvelopers.mcgillmobile.R;
import ca.appvelopers.mcgillmobile.activity.base.BaseActivity;
import ca.appvelopers.mcgillmobile.activity.base.DrawerAdapter;
import ca.appvelopers.mcgillmobile.fragment.CourseSearchFragment;
import ca.appvelopers.mcgillmobile.fragment.MyCoursesFragment;
import ca.appvelopers.mcgillmobile.fragment.ScheduleFragment;
import ca.appvelopers.mcgillmobile.fragment.courseslist.WishlistFragment;
import ca.appvelopers.mcgillmobile.fragment.transcript.TranscriptFragment;
import ca.appvelopers.mcgillmobile.object.DrawerItem;
import ca.appvelopers.mcgillmobile.util.Clear;
import ca.appvelopers.mcgillmobile.util.Constants;
import ca.appvelopers.mcgillmobile.util.GoogleAnalytics;
import ca.appvelopers.mcgillmobile.util.Help;
import ca.appvelopers.mcgillmobile.view.DialogHelper;

/**
 * Author: Julien Guerinet
 * Date: 2015-01-12 8:39 PM
 * Copyright (c) 2014 Appvelopers Inc. All rights reserved.
 */

public class MainActivity extends BaseActivity {
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ProgressBar mToolbarProgress;

    private ActionBarDrawerToggle drawerToggle;
    private DrawerItem mCurrentDrawerItem;

    //The Fragments
    private ScheduleFragment mScheduleFragment;
    private TranscriptFragment mTranscriptFragment;
    private MyCoursesFragment mMyCoursesFragment;
    private WishlistFragment mWishlistFragment;
    private CourseSearchFragment mCourseSearchFragment;

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get the current page
        mCurrentDrawerItem = (DrawerItem)getIntent().getSerializableExtra(Constants.HOMEPAGE);
        //If it's null, set it to the Schedule
        if(mCurrentDrawerItem == null){
            mCurrentDrawerItem = DrawerItem.SCHEDULE;
        }

        //Get the toolbar
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        //Set is as the action bar
        setSupportActionBar(toolbar);

        //Bind the progress bar
        mToolbarProgress = (ProgressBar)findViewById(R.id.toolbar_progress);

        //Create the fragments
        mScheduleFragment = new ScheduleFragment();
        mTranscriptFragment = new TranscriptFragment();
        mMyCoursesFragment = new MyCoursesFragment();
        mWishlistFragment = WishlistFragment.createInstance(true, null);
        mCourseSearchFragment = new CourseSearchFragment();

        //Get the drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setFocusableInTouchMode(false);

        //Set up the drawer toggle
//        drawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, 0, 0);
//        mDrawerLayout.setDrawerListener(drawerToggle);
        drawerToggle.setDrawerIndicatorEnabled(true);

        //Set up the drawer
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        final DrawerAdapter drawerAdapter = new DrawerAdapter(this);
        mDrawerList.setAdapter(drawerAdapter);

        //Set the currently checked one
        for(int i = 0; i < drawerAdapter.getCount(); i++){
            DrawerItem drawerItem = drawerAdapter.getItem(i);

            //Set the checked item accordingly
            mDrawerList.setItemChecked(i, drawerItem == mCurrentDrawerItem);
        }

        //Set the current fragment
        setFragment(mCurrentDrawerItem);

        //OnClickListener
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Get the concerned page
                DrawerItem drawerItem = drawerAdapter.getItem(position);

                //Take the appropriate action
                setFragment(drawerItem);

                // Highlight the selected item and close the drawer
                mDrawerList.setItemChecked(position, true);
                mDrawerLayout.closeDrawer(mDrawerList);
            }
        });

        //Show the BugDialog if there is one
        String parserBug = getIntent().getStringExtra(Constants.BUG);
        if(parserBug != null){
            DialogHelper.showBugDialog(this, parserBug.equals(Constants.TRANSCRIPT),
                    getIntent().getStringExtra(Constants.TERM));
        }
    }

    private void setFragment(DrawerItem drawerItem){
        Fragment fragment = null;
        switch(drawerItem) {
            case SCHEDULE:
                fragment = mScheduleFragment;
                break;
            case TRANSCRIPT:
                fragment = mTranscriptFragment;
                break;
            case MY_COURSES:
                fragment = mMyCoursesFragment;
                break;
            case COURSES:
                //TODO
                fragment = null;
                break;
            case SEARCH_COURSES:
                fragment = mCourseSearchFragment;
                break;
            case WISHLIST:
                fragment = mWishlistFragment;
                break;
            case EBILL:
                //TODO
                fragment = null;
                break;
            case MAP:
                //TODO
                fragment = null;
                break;
            case DESKTOP:
                //TODO
                fragment = null;
                break;
            case SETTINGS:
                //TODO
                fragment = null;
                break;
            case FACEBOOK:
                Help.postOnFacebook(MainActivity.this);
                break;
            case TWITTER:
                Help.loginTwitter(MainActivity.this);
                break;
            case LOGOUT:
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.logout_dialog_title))
                        .setMessage(getString(R.string.logout_dialog_message))
                        .setPositiveButton(getString(R.string.logout_dialog_positive), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                GoogleAnalytics.sendEvent(MainActivity.this, "Logout", "Clicked", null, null);
                                Clear.clearAllInfo(MainActivity.this);
                                //Go back to SplashActivity
                                startActivity(new Intent(MainActivity.this, SplashActivity.class));
                                MyCoursesFragment.deleteCookies();
                            }

                        })
                        .setNegativeButton(getString(R.string.logout_dialog_negative), null)
                        .create()
                        .show();
                break;
        }

        //If there is a fragment, insert it by replacing any existing fragment
        if(fragment != null){
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.main_content, fragment)
                    .commit();
        }
    }

    @Override
    public void onBackPressed(){
        //If we are on a web page, check if we can go back in the web page itself
        if(mCurrentDrawerItem == DrawerItem.MY_COURSES && mMyCoursesFragment.getWebView().canGoBack()){
            mMyCoursesFragment.getWebView().goBack();
            return;
        }

        //Open the menu if it is not open
        if(!mDrawerLayout.isDrawerOpen(mDrawerList)){
            mDrawerLayout.openDrawer(mDrawerList);
        }
        //If it is open, ask the user if he wants to exit
        else{
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.drawer_exit))
                    .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.super.onBackPressed();
                        }
                    })
                    .setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        //Only show the menu in portrait mode for the schedule
        if(mCurrentDrawerItem == DrawerItem.SCHEDULE){
            return getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE;
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);

        //Reload the menu and the view if this is the schedule
        if(mCurrentDrawerItem == DrawerItem.SCHEDULE){
            //Reload the menu
            invalidateOptionsMenu();

            //Reload the view
            mScheduleFragment.loadView(newConfig.orientation);
        }
    }

    //For Facebook Sharing
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    /**
     * Method that shows or hides the spinner in the toolbar
     * @param visible True if it should be visible, false otherwise
     */
    public void showToolbarSpinner(boolean visible){
        mToolbarProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Get the schedule fragment
     * @return The schedule fragment
     */
    public ScheduleFragment getScheduleFragment(){
        return mScheduleFragment;
    }
}