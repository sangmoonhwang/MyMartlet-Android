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

package ca.appvelopers.mcgillmobile.ui.transcript;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import ca.appvelopers.mcgillmobile.App;
import ca.appvelopers.mcgillmobile.R;
import ca.appvelopers.mcgillmobile.model.Semester;
import ca.appvelopers.mcgillmobile.model.Transcript;
import ca.appvelopers.mcgillmobile.ui.transcript.semester.SemesterActivity;
import ca.appvelopers.mcgillmobile.util.Constants;

/**
 * List Adapter that will populate the list of adapters in TranscriptActivity
 * Author: Julien
 * Date: 31/01/14, 6:06 PM
 */
public class TranscriptAdapter extends BaseAdapter {
    private Context mContext;
    private List<Semester> mSemesters;

    public TranscriptAdapter(Context context, Transcript transcript){
        this.mContext = context;
        this.mSemesters = transcript.getSemesters();
    }

    @Override
    public int getCount() {
        return mSemesters.size();
    }

    @Override
    public Semester getItem(int position) {
        return mSemesters.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        //Reuse the view if it's already been used
        if(view == null){
            //Get the inflater
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_semester, null);
        }

        //Get the current semester we are inflating
        final Semester semester = getItem(position);

        //Set up the info
        TextView semesterName = (TextView)view.findViewById(R.id.semester_name);
        semesterName.setText(semester.getSemesterName());

        TextView semesterGPA = (TextView)view.findViewById(R.id.semester_termGPA);
        semesterGPA.setText(mContext.getResources().getString(R.string.transcript_termGPA, String.valueOf(semester.getGPA())));


        //Set up the chevron
        TextView chevron = (TextView)view.findViewById(R.id.semester_chevron);
        chevron.setTypeface(App.getIconFont());

        //Set up the onClicklistener for the view
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, SemesterActivity.class);
                intent.putExtra(Constants.SEMESTER, semester);
                mContext.startActivity(intent);
            }
        });

        return view;
    }
}
