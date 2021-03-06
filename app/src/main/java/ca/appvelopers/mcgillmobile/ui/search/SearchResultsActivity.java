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

package ca.appvelopers.mcgillmobile.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.guerinet.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ca.appvelopers.mcgillmobile.App;
import ca.appvelopers.mcgillmobile.R;
import ca.appvelopers.mcgillmobile.RegistrationError;
import ca.appvelopers.mcgillmobile.model.Course;
import ca.appvelopers.mcgillmobile.model.CourseResult;
import ca.appvelopers.mcgillmobile.model.Term;
import ca.appvelopers.mcgillmobile.model.exception.MinervaException;
import ca.appvelopers.mcgillmobile.ui.BaseActivity;
import ca.appvelopers.mcgillmobile.ui.dialog.DialogHelper;
import ca.appvelopers.mcgillmobile.ui.wishlist.WishlistSearchCourseAdapter;
import ca.appvelopers.mcgillmobile.util.Analytics;
import ca.appvelopers.mcgillmobile.util.Constants;
import ca.appvelopers.mcgillmobile.util.manager.McGillManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Shows the results of the search from the SearchActivity
 * @author Julien Guerinet
 * @since 1.0.0
 */
public class SearchResultsActivity extends BaseActivity {
    /**
     * The courses list
     */
    @Bind(android.R.id.list)
    RecyclerView mListView;
    /**
     * The empty view
     */
    @Bind(R.id.courses_empty)
    TextView mEmptyView;
    /**
     * The adapter for the list of results
     */
    private WishlistSearchCourseAdapter mAdapter;
    /**
     * The current term
     */
    private Term mTerm;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searchresults);
        ButterKnife.bind(this);
        setUpToolbar(true);
        analytics.sendScreen("Search Results");

        //Get the info from the intent
        mTerm = (Term)getIntent().getSerializableExtra(Constants.TERM);
        List<CourseResult> courses =
                (ArrayList<CourseResult>) getIntent().getSerializableExtra(Constants.COURSES);

        //Set the title
        setTitle(mTerm.getString(this));

        //ListView
        mAdapter = new WishlistSearchCourseAdapter(this, mTerm, courses);
        mListView.setLayoutManager(new LinearLayoutManager(this));
        mListView.setAdapter(mAdapter);
        if(mAdapter.isEmpty()){
            mListView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.course_register)
    void registerButton(){
        register(this, mTerm, mAdapter.getCheckedCourses());
    }

    @OnClick(R.id.course_wishlist)
    void wishlistButton(){
        addToWishlist(this, mAdapter.getCheckedCourses(), true, analytics);
    }

    /**
     * Tries to register the users to the given courses
     *
     * @param activity The calling activity
     * @param term     The concerned term
     * @param courses  The list of courses
     */
    public static void register(final BaseActivity activity, Term term,
            final List<CourseResult> courses) {
        //Too many courses
        if(courses.size() > 10){
            Toast.makeText(activity, activity.getString(R.string.courses_too_many_courses),
                    Toast.LENGTH_SHORT).show();
        }
        //No Courses
        else if(courses.isEmpty()){
            Toast.makeText(activity, activity.getString(R.string.courses_none_selected),
                    Toast.LENGTH_SHORT).show();
        }
        //Execute registration of checked classes in a new thread
        else if(courses.size() > 0){
            //Check that we can continue
            if (!activity.canRefresh()) {
                return;
            }

            List<Course> theCourses = new ArrayList<>();
            theCourses.addAll(courses);
            activity.getMcGillService()
                    .registration(McGillManager.getRegistrationURL(term, theCourses, false))
                    .enqueue(new Callback<List<RegistrationError>>() {
                @Override
                public void onResponse(Call<List<RegistrationError>> call,
                        Response<List<RegistrationError>> response) {
                    activity.showToolbarProgress(false);

                    //If there are no errors, show the success message
                    if (response.body() == null || response.body().isEmpty()) {
                        Utils.toast(activity, R.string.registration_success);
                        return;
                    }

                    //Prepare the error message String
                    String errorMessage = "";
                    List<Course> errorCourses = new ArrayList<>();
                    errorCourses.addAll(courses);
                    for (RegistrationError error : response.body()) {
                        errorMessage += error.getString(errorCourses);
                        errorMessage += "\n";
                    }

                    DialogHelper.error(activity, errorMessage);

                    //Remove the courses from the wishlist if they were there
                    List<CourseResult> wishlist = App.getWishlist();
                    wishlist.removeAll(courses);

                    //Set the new wishlist
                    App.setWishlist(wishlist);
                }

                @Override
                public void onFailure(Call<List<RegistrationError>> call, Throwable t) {
                    Timber.e(t, "Error (un)registering for courses");
                    activity.showToolbarProgress(false);
                    //If this is a MinervaException, broadcast it
                    if (t instanceof MinervaException) {
                        LocalBroadcastManager.getInstance(activity)
                                .sendBroadcast(new Intent(Constants.BROADCAST_MINERVA));
                    } else {
                        DialogHelper.error(activity, R.string.error_other);
                    }
                }
            });
        }
    }

    /**
     * Adds/removes the given courses to/from the wishlist
     *
     * @param activity  The calling activity
     * @param courses   The courses to add/remove
     * @param add       True if we are adding courses, false if we're removing
     * @param analytics {@link Analytics} instance
     */
    public static void addToWishlist(BaseActivity activity, List<CourseResult> courses, boolean add,
            Analytics analytics){
        String toastMessage;
        //If there are none, display error message
        if (courses.isEmpty()) {
            toastMessage = activity.getString(R.string.courses_none_selected);
        }
        //If not, it's to add a course to the wishlist
        else {
            //Get the wishlist courses
            List<CourseResult> wishlist = App.getWishlist();

            if(add){
                //Only add it if it's not already part of the wishlist
                int coursesAdded = 0;
                for (CourseResult course : courses) {
                    if (!wishlist.contains(course)) {
                        wishlist.add(course);
                        coursesAdded++;
                    }
                }

                analytics.sendEvent("Search Results", "Add to Wishlist",
                        String.valueOf(coursesAdded));

                toastMessage = activity.getString(R.string.wishlist_add, coursesAdded);
            }
            else{
                toastMessage = activity.getString(R.string.wishlist_remove, courses.size());
                wishlist.removeAll(courses);

                analytics.sendEvent("Wishlist", "Remove", String.valueOf(courses.size()));
            }

            //Save the courses to the App context
            App.setWishlist(wishlist);
        }

        //Visual feedback of what was just done
        Toast.makeText(activity, toastMessage, Toast.LENGTH_SHORT).show();
    }
}