/*
 * Copyright 2014-2015 Appvelopers
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

package ca.appvelopers.mcgillmobile.ui.wishlist;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import ca.appvelopers.mcgillmobile.App;
import ca.appvelopers.mcgillmobile.R;
import ca.appvelopers.mcgillmobile.model.Course;
import ca.appvelopers.mcgillmobile.model.Term;
import ca.appvelopers.mcgillmobile.model.TranscriptCourse;
import ca.appvelopers.mcgillmobile.ui.ChangeSemesterDialog;
import ca.appvelopers.mcgillmobile.ui.base.BaseFragment;
import ca.appvelopers.mcgillmobile.ui.search.SearchResultsActivity;
import ca.appvelopers.mcgillmobile.util.Analytics;
import ca.appvelopers.mcgillmobile.util.Connection;
import ca.appvelopers.mcgillmobile.util.Parser;
import ca.appvelopers.mcgillmobile.util.thread.DownloaderThread;

/**
 * Displays the user's wishlist
 * @author Ryan Singzon
 * @author Julien Guerinet
 * @version 2.0
 * @since 1.0
 */
public class WishlistFragment extends BaseFragment {
    /**
     * The empty view
     */
    @InjectView(R.id.courses_empty)
    TextView mEmptyView;
    /**
     * The wishlist button
     */
    @InjectView(R.id.course_wishlist)
    TextView mWishlistButton;
    /**
     * The wishlist
     */
    @InjectView(android.R.id.list)
    RecyclerView mListView;
    /**
     * The ListView adapter
     */
    private WishlistSearchCourseAdapter mAdapter;
    /**
     * The list of classes to display
     */
    private List<Course> mCourses;
    /**
     * The current term
     */
    private Term mTerm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Fragment has a menu
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        super.onCreateView(inflater, container, savedInstanceState);
        View view = View.inflate(mActivity, R.layout.fragment_wishlist, container);
        ButterKnife.inject(this, view);
        lockPortraitMode();
        Analytics.getInstance().sendScreen("Wishlist");

        //Check if there are any terms to register for
        if(App.getRegisterTerms().isEmpty()){
            //Hide all of the main content, show explanatory text, and return the view
            mEmptyView.setText(getString(R.string.registration_no_semesters));
            mEmptyView.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);

            hideLoadingIndicator();

            return view;
        }

        //Change the text on the wishlist button
        mWishlistButton.setText(getResources().getString(R.string.courses_remove_wishlist));

        //Load the first registration term
        mTerm = App.getRegisterTerms().get(0);

        //Load the wishlist
        mCourses = App.getClassWishlist();

        //Update the wishlist
        updateWishlist();

        //Hide the loading indicator
        hideLoadingIndicator();

        return view;
    }

    @Override
    public void onResume(){
        super.onResume();
        loadInfo();
    }

    @OnClick(R.id.course_register)
    void register(){
        SearchResultsActivity.register(mActivity, mTerm, mAdapter.getCheckedCourses());

        //Reload the adapter
        loadInfo();
    }

    @OnClick(R.id.course_wishlist)
    void removeFromWishlist(){
        SearchResultsActivity.addToWishlist(mActivity, mAdapter.getCheckedCourses(), false);

        //Reload the adapter
        loadInfo();
    }

    private void loadInfo(){
        //Only load the info if there is info to load
        if(!App.getRegisterTerms().isEmpty()){
            //Set the title
            mActivity.setTitle(mTerm.toString());

            //Reload the adapter
            mAdapter = new WishlistSearchCourseAdapter(mActivity, mTerm, mCourses);
            mListView.setAdapter(mAdapter);

            //If there are no classes, show the empty view
            if(mAdapter.isEmpty()){
                mListView.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        //Only inflate the menu with the change semester if there is more than 1 semester to
        //  register for
        if(App.getRegisterTerms().size() > 1){
            inflater.inflate(R.menu.refresh_change_semester, menu);
        }
        //If there is at least one semester to register for, show the refresh button
        else if(!App.getRegisterTerms().isEmpty()){
            inflater.inflate(R.menu.refresh, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_change_semester){
            final ChangeSemesterDialog dialog = new ChangeSemesterDialog(mActivity, mTerm, true);
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    Term term = dialog.getTerm();

                    //If there is a term selected, refresh the view
                    if(term != null){
                        mTerm = term;
                        loadInfo();
                    }
                }
            });
            dialog.show();
            return true;
        }
        else if(item.getItemId() == R.id.action_refresh){
            updateWishlist();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Updates the information of the courses on the current wishlist
     */
    private void updateWishlist(){
        mActivity.showToolbarProgress(true);

        //Sort Courses into TranscriptCourses
        List<TranscriptCourse> coursesList = new ArrayList<>();
        for(Course course : mCourses){
            boolean courseExists = false;
            //Check if course exists in list
            for(TranscriptCourse addedCourse : coursesList){
                if(addedCourse.getCourseCode().equals(course.getCode())){
                    courseExists = true;
                }
            }
            //Add course if it has not already been added
            if(!courseExists){
                coursesList.add(new TranscriptCourse(course.getTerm(), course.getCode(),
                        course.getTitle(), course.getCredits(), "N/A", "N/A"));
            }
        }

        //For each course, obtain its Minerva registration page
        for(TranscriptCourse course : coursesList){
            //Get the course registration URL
            String code[] = course.getCourseCode().split(" ");
            if(code.length < 2){
                //TODO: Get a String for this
                Toast.makeText(mActivity, "Cannot update " + course.getCourseCode(),
                        Toast.LENGTH_SHORT).show();
                continue;
            }

            String subject = code[0];
            String number = code[1];
            String url = new Connection.SearchURLBuilder(course.getTerm(), subject)
                            .courseNumber(number)
                            .build();

            String html = new DownloaderThread(mActivity, "Wishlist Download", url).execute();

            if(html != null){
                //TODO: Figure out a way to parse only some course sections instead of re-parsing all course sections for a given Course
                //This parses all ClassItems for a given course
                List<Course> updatedCourses = Parser.parseClassResults(course.getTerm(), html);

                //Update the course object with an updated class size
                for(Course updatedClass : updatedCourses){
                    for(Course wishlistClass : mCourses){
                        if(wishlistClass.equals(updatedClass)){
                            int i = mCourses.indexOf(wishlistClass);
                            mCourses.remove(wishlistClass);
                            mCourses.add(i, updatedClass);
                        }
                    }
                }
            }
        }

        //Set the new wishlist
        App.setClassWishlist(mCourses);
        //Reload the adapter
        loadInfo();

        mActivity.showToolbarProgress(false);
    }
}