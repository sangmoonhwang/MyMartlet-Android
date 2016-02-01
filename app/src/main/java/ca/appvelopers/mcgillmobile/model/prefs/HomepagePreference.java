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

package ca.appvelopers.mcgillmobile.model.prefs;

import android.content.SharedPreferences;

import com.guerinet.utils.prefs.IntPreference;

import javax.inject.Inject;

import ca.appvelopers.mcgillmobile.model.Homepage;

/**
 * Extension of the {@link IntPreference} for the homepage
 * @author Julien Guerinet
 * @since 2.0.4
 */
public class HomepagePreference extends IntPreference {

    /**
     * Default Constructor
     *
     * @param prefs        {@link SharedPreferences} instance
     */
    @Inject
    public HomepagePreference(SharedPreferences prefs) {
        super(prefs, "home_page", Homepage.SCHEDULE);
    }

    @Override
    @SuppressWarnings("ResourceType")
    public @Homepage.Type int get() {
        return super.get();
    }

    public void set(@Homepage.Type int value) {
        super.set(value);
    }
}
