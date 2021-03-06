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

package ca.appvelopers.mcgillmobile.ui.transcript.semester;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.appvelopers.mcgillmobile.R;
import ca.appvelopers.mcgillmobile.model.TranscriptCourse;

/**
 * Populates the courses list in SemesterActivity
 * @author Julien Guerinet
 * @since 1.0.0
 */
public class SemesterAdapter extends RecyclerView.Adapter<SemesterAdapter.CourseHolder> {
    /**
     * List of courses
     */
    private List<TranscriptCourse> mCourses;

    /**
     * Default Constructor
     *
     * @param courses Courses to display
     */
    public SemesterAdapter(List<TranscriptCourse> courses) {
        mCourses = courses;
    }

    @Override
    public CourseHolder onCreateViewHolder(ViewGroup parent, int viewType){
        return new CourseHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transcript_course, parent, false));
    }

    @Override
    public void onBindViewHolder(CourseHolder holder, int position) {
        holder.bind(mCourses.get(position));
    }

    @Override
    public int getItemCount() {
        return mCourses.size();
    }

    class CourseHolder extends RecyclerView.ViewHolder {
        /**
         * Course Code
         */
        @Bind(R.id.course_code)
        protected TextView mCode;
        /**
         * User grade
         */
        @Bind(R.id.course_grade)
        protected TextView mGrade;
        /**
         * Course title
         */
        @Bind(R.id.course_title)
        protected TextView mTitle;
        /**
         * Course credits
         */
        @Bind(R.id.course_credits)
        protected TextView mCredits;
        /**
         * Course average grade
         */
        @Bind(R.id.course_average)
        protected TextView mAverageGrade;

        public CourseHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(TranscriptCourse course) {
            Context context = itemView.getContext();

            mCode.setText(course.getCourseCode());
            mGrade.setText(course.getUserGrade());
            mTitle.setText(course.getCourseTitle());
            mCredits.setText(context.getString(R.string.course_credits, course.getCredits()));

            //Don't display average if it does not exist
            if (!course.getAverageGrade().equals("")) {
                mAverageGrade.setText(
                        context.getString(R.string.course_average, course.getAverageGrade()));
            }
        }
    }
}
