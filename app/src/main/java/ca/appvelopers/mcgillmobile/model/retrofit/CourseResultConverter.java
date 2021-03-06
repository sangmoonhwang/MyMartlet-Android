/*
 * Copyright 2014-2016 Julien Guerinet
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

package ca.appvelopers.mcgillmobile.model.retrofit;

import android.support.v4.util.Pair;

import com.squareup.moshi.Types;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import ca.appvelopers.mcgillmobile.model.CourseResult;
import ca.appvelopers.mcgillmobile.model.Term;
import ca.appvelopers.mcgillmobile.util.DayUtils;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;
import timber.log.Timber;

/**
 * Retrofit converter to parse a list of course results when searching for courses
 * @author Julien Guerinet
 * @since 2.2.0
 */
public class CourseResultConverter extends Converter.Factory
        implements Converter<ResponseBody, List<CourseResult>> {
    /**
     * {@link ParameterizedType} representing a list of {@link CourseResult}s
     */
    private final ParameterizedType type =
            Types.newParameterizedType(List.class, CourseResult.class);

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
            Retrofit retrofit) {
        if (!type.toString().equals(this.type.toString())) {
            //This can only convert a list of course results
            return null;
        }
        return new CourseResultConverter();
    }

    @Override
    public List<CourseResult> convert(ResponseBody value) throws IOException {
        String html = value.string();
        List<CourseResult> courses = new ArrayList<>();
        Document document = Jsoup.parse(html, "UTF-8");
        //Parse the response body into a list of rows
        Elements rows = document.getElementsByClass("dddefault");

        // Parse the term from the page header
        Element header = document.getElementsByClass("staticheaders").get(0);
        Term term = Term.parseTerm(header.childNode(2).toString());

        int rowNumber = 0;
        boolean loop = true;
        while (loop) {
            // Create a new course object with the default values
            double credits = 99;
            String subject = "ERROR";
            String number = "ERROR";
            String title = "";
            String type = "";
            List<DayOfWeek> days = new ArrayList<>();
            int crn = 0;
            String instructor = "";
            String location = "";
            //So that the rounded start time will be 0
            LocalTime startTime = ScheduleConverter.getDefaultStartTime();
            LocalTime endTime = ScheduleConverter.getDefaultEndTime();
            int capacity = 0;
            int seatsRemaining = 0;
            int waitlistRemaining = 0;
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now();

            int i = 0;
            while (true) {
                try {
                    // Get the HTML row
                    Element row = rows.get(rowNumber);

                    // End condition: Empty row encountered or "Notes
                    if (row.toString().contains("&nbsp;") || row.toString().contains("NOTES:")) {
                        rowNumber ++;
                        break;
                    }

                    //Get the row text
                    String rowString = row.text();
                    switch (i) {
                        // CRN
                        case 1:
                            crn = Integer.parseInt(rowString);
                            break;
                        //Subject
                        case 2:
                            subject = rowString;
                            break;
                        //Number
                        case 3:
                            number = rowString;
                            break;
                        //Type
                        case 5:
                            type = rowString;
                            break;
                        // Number of credits
                        case 6:
                            credits = Double.parseDouble(rowString);
                            break;
                        // Course title
                        case 7:
                            //Remove the extra period at the end of the course title
                            title = rowString.substring(0, rowString.length() - 1);
                            break;
                        // Days of the week
                        case 8:
                            String dayString = rowString;
                            //TBA Stuff
                            if (dayString.equals("TBA")) {
                                i = 10;
                                rowNumber ++;
                            } else {
                                //Day Parsing
                                dayString = dayString.replace('\u00A0',' ').trim();
                                for (int k = 0; k < dayString.length(); k ++) {
                                    days.add(DayUtils.getDay(dayString.charAt(k)));
                                }
                            }
                            break;
                        // Time
                        case 9:
                            String[] times = rowString.split("-");
                            try {
                                int startHour =
                                        Integer.parseInt(times[0].split(" ")[0].split(":")[0]);
                                int startMinute =
                                        Integer.parseInt(times[0].split(" ")[0].split(":")[1]);
                                int endHour =
                                        Integer.parseInt(times[1].split(" ")[0].split(":")[0]);
                                int endMinute =
                                        Integer.parseInt(times[1].split(" ")[0].split(":")[1]);

                                //If it's PM, then add 12 hours to the hours for 24 hours format
                                //Make sure it isn't noon
                                String startPM = times[0].split(" ")[1];
                                if (startPM.equals("PM") && startHour != 12) {
                                    startHour += 12;
                                }

                                String endPM = times[1].split(" ")[1];
                                if (endPM.equals("PM") && endHour != 12) {
                                    endHour += 12;
                                }

                                startTime = LocalTime.of(startHour, startMinute);
                                endTime = LocalTime.of(endHour, endMinute);
                            } catch (NumberFormatException e) {
                                //Courses sometimes don't have assigned times
                                startTime = ScheduleConverter.getDefaultStartTime();
                                endTime = ScheduleConverter.getDefaultEndTime();
                            }
                            break;
                        // Capacity
                        case 10:
                            capacity = Integer.parseInt(rowString);
                            break;
                        // Seats remaining
                        case 12:
                            seatsRemaining = Integer.parseInt(rowString);
                            break;
                        // Waitlist remaining
                        case 15:
                            waitlistRemaining = Integer.parseInt(rowString);
                            break;
                        // Instructor
                        case 16:
                            instructor = rowString;
                            break;
                        // Start/end date
                        case 17:
                            Pair<LocalDate, LocalDate> dates = parseDateRange(term, rowString);
                            startDate = dates.first;
                            endDate = dates.second;
                            break;
                        // Location
                        case 18:
                            location = rowString;
                            break;
                    }
                    i ++;
                    rowNumber ++;
                } catch (IndexOutOfBoundsException e) {
                    loop = false;
                    break;
                } catch (Exception e) {
                    Timber.e(e, "Course Results Parser Error");
                }
            }
            //Don't add any courses with errors
            if (!subject.equals("ERROR") && !number.equals("ERROR")) {
                //Create a new course object and add it to list
                //TODO Should we be parsing the course section?
                courses.add(new CourseResult(term, subject, number, title, crn, "", startTime,
                        endTime, days, type, location, instructor, credits, startDate, endDate,
                        capacity, seatsRemaining, waitlistRemaining));
            }
        }

        return courses;
    }

    /**
     * Parses a String into a LocalDate object
     *
     * @param term Current term
     * @param date The date String
     * @return The corresponding local date
     */
    public LocalDate parseDate(Term term, String date) {
        String[] dateFields = date.split("/");
        return LocalDate.of(term.getYear(), Integer.parseInt(dateFields[0]),
                Integer.parseInt(dateFields[1]));
    }

    /**
     * Parses the date range String into 2 dates
     *
     * @param term      Current term
     * @param dateRange The date range String
     * @return A pair representing the starting and ending dates of the range
     * @throws IllegalArgumentException
     */
    public Pair<LocalDate, LocalDate> parseDateRange(Term term, String dateRange)
            throws IllegalArgumentException {
        //Split the range into the 2 date Strings
        String[] dates = dateRange.split("-");
        String startDate = dates[0].trim();
        String endDate = dates[1].trim();

        //Parse the dates, return them as a pair
        return new Pair<>(parseDate(term, startDate), parseDate(term, endDate));
    }
}
