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

package ca.appvelopers.mcgillmobile.ui.dialog.list;

import android.content.Context;

import com.guerinet.utils.dialog.ListDialogInterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ca.appvelopers.mcgillmobile.model.Faculty;

/**
 * Displays a list of faculties to choose from in the walkthrough
 * @author Julien Guerinet
 * @since 2.1.0
 */
public abstract class FacultyListAdapter implements ListDialogInterface {
    /**
     * List of faculties
     */
    private List<String> mFaculties;
    /**
     * The current choice
     */
    private int mCurrentChoice;

    /**
     * Default Constructor
     *
     * @param currentFaculty Currently selected faculty, an empty String if none
     */
    public FacultyListAdapter(Context context, String currentFaculty) {
        mFaculties = new ArrayList<>();
        mFaculties.add(Faculty.getString(context, Faculty.ENVIRONMENTAL_SCIENCES));
        mFaculties.add(Faculty.getString(context, Faculty.ARTS));
        mFaculties.add(Faculty.getString(context, Faculty.CONTINUING_STUDIES));
        mFaculties.add(Faculty.getString(context, Faculty.DENTISTRY));
        mFaculties.add(Faculty.getString(context, Faculty.EDUCATION));
        mFaculties.add(Faculty.getString(context, Faculty.ENGINEERING));
        mFaculties.add(Faculty.getString(context, Faculty.GRADUATE));
        mFaculties.add(Faculty.getString(context, Faculty.LAW));
        mFaculties.add(Faculty.getString(context, Faculty.MANAGEMENT));
        mFaculties.add(Faculty.getString(context, Faculty.MEDICINE));
        mFaculties.add(Faculty.getString(context, Faculty.MUSIC));
        mFaculties.add(Faculty.getString(context, Faculty.RELIGIOUS_STUDIES));
        mFaculties.add(Faculty.getString(context, Faculty.SCIENCE));

        //Sort them alphabetically
        Collections.sort(mFaculties, new Comparator<String>() {
            @Override
            public int compare(String faculty, String faculty2) {
                return faculty.compareToIgnoreCase(faculty2);
            }
        });

        //Add undefined to the top of the list
        mFaculties.add(0, Faculty.getString(context, Faculty.NONE));

        //Get the current choice index
        mCurrentChoice = mFaculties.indexOf(currentFaculty);
    }

    @Override
    public int getCurrentChoice() {
        return mCurrentChoice;
    }

    @Override
    public CharSequence[] getChoices() {
        return mFaculties.toArray(new CharSequence[mFaculties.size()]);
    }

    @Override
    public void onChoiceSelected(int position) {
       onFacultySelected(mFaculties.get(position));
    }

    /**
     * Called when a faculty is selected
     *
     * @param faculty The faculty selected, empty if none
     */
    public abstract void onFacultySelected(String faculty);
}
