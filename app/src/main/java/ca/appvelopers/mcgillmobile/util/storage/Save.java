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

package ca.appvelopers.mcgillmobile.util.storage;

import android.content.Context;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import ca.appvelopers.mcgillmobile.App;
import ca.appvelopers.mcgillmobile.util.Constants;
import timber.log.Timber;

/**
 * Saves objects to the internal storage
 * @author Julien Guerinet
 * @since 1.0.0
 */
public class Save {

    /**
     * Saves an object to internal storage
     *
     * @param tag      The tag to use in case of an error
     * @param fileName The file name to save the object under
     * @param object   The object to save
     */
    private static void saveObject(String tag, String fileName, Object object){
        try{
            FileOutputStream fos = App.getContext().openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(object);
        } catch(Exception e) {
            Timber.e(e, "Failure: %s", tag);
        }
    }

    /**
     * Saves the terms the user can currently register in
     */
    public static void registerTerms(){
        saveObject("Register Terms", Constants.REGISTER_TERMS_FILE, App.getRegisterTerms());
    }

    /**
     * Saves the user's ebill statements
     */
    public static void ebill(){
        saveObject("Ebill", Constants.EBILL_FILE, App.getEbill());
    }

    /**
     * Saves the user's default term
     */
    public static void defaultTerm(){
        saveObject("Default Term", Constants.DEFAULT_TERM_FILE, App.getDefaultTerm());
    }

    /**
     * Saves the user's wishlist
     */
    public static void wishlist(){
        saveObject("Wishlist", Constants.WISHLIST_FILE, App.getWishlist());
    }
}
