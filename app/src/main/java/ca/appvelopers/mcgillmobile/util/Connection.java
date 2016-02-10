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

package ca.appvelopers.mcgillmobile.util;

import android.content.Context;
import android.net.ConnectivityManager;

import com.guerinet.utils.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.threeten.bp.DayOfWeek;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;

import ca.appvelopers.mcgillmobile.App;
import ca.appvelopers.mcgillmobile.model.ConnectionStatus;
import ca.appvelopers.mcgillmobile.model.Course;
import ca.appvelopers.mcgillmobile.model.Term;
import ca.appvelopers.mcgillmobile.model.exception.MinervaException;
import ca.appvelopers.mcgillmobile.model.exception.NoInternetException;
import ca.appvelopers.mcgillmobile.model.prefs.PasswordPreference;
import ca.appvelopers.mcgillmobile.model.prefs.UsernamePreference;
import okhttp3.CacheControl;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import timber.log.Timber;

/**
 * All of the connection logic
 * @author Shabbir Hussain
 * @author Rafi Uddin
 * @author Joshua David Alfaro
 * @author Julien Guerinet
 * @since 1.0.0
 */
public class Connection {
	/**
	 * Login URL
	 */
	private static final String LOGIN_PAGE_URL =
			"https://horizon.mcgill.ca/pban1/twbkwbis.P_WWWLogin";
	/**
	 * Login POST URL
	 */
	private static final String LOGIN_POST_URL =
			"https://horizon.mcgill.ca/pban1/twbkwbis.P_ValLogin";
	/**
	 * Schedule URL
	 */
    private static final String SCHEDULE_URL =
			"https://horizon.mcgill.ca/pban1/bwskfshd.P_CrseSchdDetl?term_in=";
	/**
	 * Transcript URL
	 */
	public static final String TRANSCRIPT_URL =
			"https://horizon.mcgill.ca/pban1/bzsktran.P_Display_Form?user_type=S&tran_type=V";
	/**
	 * Ebill URL
	 */
	public static final String EBILL_URL = "https://horizon.mcgill.ca/pban1/bztkcbil.pm_viewbills";
	/**
	 * Course Search URL
	 */
    private static final String COURSE_SEARCH_URL =
			"https://horizon.mcgill.ca/pban1/bwskfcls.P_GetCrse?";
	/**
	 * Course Registration URL
	 */
    private static final String COURSE_REGISTRATION_URL =
			"https://horizon.mcgill.ca/pban1/bwckcoms.P_Regs?term_in=";
	/**
	 * Course Registration Errors URL
	 */
	public static final String REGISTRATION_ERROR_URL =
			"http://www.is.mcgill.ca/whelp/sis_help/rg_errors.htm";
	/**
	 * Singleton instance
	 */
	private static Connection connection;
    /**
     * {@link OkHttpClient} instance
     */
    @Inject
    protected OkHttpClient client;
    /**
     * {@link ConnectivityManager} instance
     */
    @Inject
    protected ConnectivityManager connectivityManager;
    /**
     * {@link UsernamePreference} instance
     */
    @Inject
    protected UsernamePreference usernamePref;
    /**
     * {@link PasswordPreference} passwordPref;
     */
    @Inject
    protected PasswordPreference passwordPref;
    /**
     * Username
     */
    private String username;
    /**
     * Password
     */
    private String password;
    /**
	 * The list of cookies
	 */
	private List<String> cookies;

	/**
	 * @return The connection instance
	 */
	public static Connection getInstance(){
		//If the connection is null, create it
		if(connection == null){
			connection = new Connection();
		}
		return connection;
	}

	/**
	 * Default Constructor
	 */
	private Connection() {
        //Inject this to get the username and password from Dagger
        App.component(App.getContext()).inject(this);
        username = usernamePref.full();
        password = passwordPref.get();
		//Set up the list of cookies
		cookies = new ArrayList<>();
		//Set up the cookie handler
		CookieHandler.setDefault(new CookieManager());
	}

	/* SETTERS */

	/**
	 * @param username The username to use
	 */
    public void setUsername(String username) {
        this.username = username;
    }

	/**
	 * @param password The password to use
	 */
    public void setPassword(String password) {
        this.password = password;
    }

	/* HELPERS */

	/**
	 * Sends a GET request and returns the body in String format
	 *
	 * @param url       The URL
	 * @param autoLogin True if we should try reconnecting the user automatically, false otherwise
	 * @return The page contents in String format
	 * @throws MinervaException
	 * @throws IOException
	 * @throws NoInternetException
	 */
	public String get(String url, boolean autoLogin) throws MinervaException, IOException,
			NoInternetException{
		//Check if the user is connected to the internet
		if (!Utils.isConnected(connectivityManager)) {
			throw new NoInternetException();
		}

		//Create the request
		Request.Builder builder = getDefaultRequest(url).get();

		//Add the cookies if there are any
		for(String cookie : cookies){
			builder.addHeader("Cookie", cookie.split(";", 1)[0]);
		}

		Timber.i("Sending 'GET' request to: %s", url);

		//Execute the request, get the response
		Response response = client.newCall(builder.build()).execute();

		int responseCode = response.code();
		Timber.i("Response Code: %d", responseCode);

		//Check the response code
		switch(responseCode){
			//If this is a redirect, go to the new link
			case HttpsURLConnection.HTTP_MOVED_TEMP:
			case HttpsURLConnection.HTTP_MOVED_PERM:
				String nextLocation = response.header("Location");
				return get(nextLocation, autoLogin);
			default:
				//All is ignored, carry on
				break;
		}

		//Check the headers
		if(!validateHeaders(response.headers())){
			//We've been logged out of Minerva. Try logging back in if needed
			if(autoLogin){
				//Launch the login process
				final ConnectionStatus status = login();

				//Successfully logged them back in, try retrieving the stuff again
				if(status == ConnectionStatus.OK){
					return get(url, false);
				}
				//No internet: show no internet dialog
				else if(status == ConnectionStatus.NO_INTERNET){
					throw new NoInternetException();
				}
				//Wrong credentials: back to login screen
				else if(status == ConnectionStatus.WRONG_INFO){
					throw new MinervaException();
				}
			}
			//If not, throw the exception
			else{
				throw new MinervaException();
			}
		}

		//Get the response cookies and set them
		cookies = response.headers("Set-Cookie");

		//Return the body in String format
		return response.body().string();
	}

	/**
	 * Sends a GET request and returns the body in String format with auto-login enabled
	 *
	 * @param url The URL
	 * @return The page contents in String format
	 * @throws MinervaException
	 * @throws IOException
	 * @throws NoInternetException
	 */
	public String get(String url) throws MinervaException, IOException, NoInternetException{
		return get(url, true);
	}

	/**
	 *  Sends a post request and returns the response body in String format
	 *
	 * @param url        The URL
	 * @param referer    The referer
	 * @param postParams The post parameters
	 * @return The response body in String format
	 * @throws IOException
	 */
	private String post(String url, String referer, final String postParams) throws IOException{
		//Create the request
		Request.Builder builder = getDefaultRequest(url)
				.post(new RequestBody() {
					@Override
					public MediaType contentType(){
						return MediaType.parse("application/x-www-form-urlencoded");
					}

					@Override
					public void writeTo(BufferedSink sink) throws IOException{
						sink.writeString(postParams, Charset.forName("UTF-8"));
					}
				})
				.header("Host", "horizon.mcgill.ca")
				.header("Origin", "https://horizon.mcgill.ca")
				.header("DNT", "1")
				.header("Connection", "keep-alive")
				.header("Referer", referer);

		//Add the cookies if there are any
		for(String cookie : cookies){
			builder.addHeader("Cookie", cookie.split(";", 1)[0]);
		}

		Timber.i("Sending 'POST' request to: %s", url);

		//Execute the request, get the result
		Response response = client.newCall(builder.build()).execute();

		Timber.i("Response Code: %d", response.code());

		//Return the response body
		return response.body().string();
	}

	/**
	 * Attempts to log into Minerva
	 *
	 * @return The resulting connection status
	 */
	public ConnectionStatus login(){
		try {
			if(!Utils.isConnected((ConnectivityManager)
                    App.getContext().getSystemService(Context.CONNECTIVITY_SERVICE))){
				return ConnectionStatus.NO_INTERNET;
			}

			//1. Get Minerva's login page and determine the login parameters
			Request request = new Request.Builder().get().url(LOGIN_PAGE_URL).build();
			String html = client.newCall(request).execute().body().string();
			String postParams = getLoginParameters(html);
			
			//Search for "Authorization Failure"
			if(postParams.contains("WRONG_INFO")){
				return ConnectionStatus.WRONG_INFO;
			}

			//2. Construct above post's content and then send a POST request for authentication
			String response = post(LOGIN_POST_URL, LOGIN_PAGE_URL, postParams);

			//Check that the connection was actually made
			if (!response.contains("WELCOME")){
				return ConnectionStatus.WRONG_INFO;
			}
		} catch (IOException e) {
			Timber.e(e, "IOException during login");
			return ConnectionStatus.ERROR_UNKNOWN;
		}

        return ConnectionStatus.OK;
    }

	/**
	 * Gets the parameters to use for logging into Minerva
	 *
	 * @param html The login HTML page
	 * @return String representing the parameters to use for logging into Minerva
	 * @throws UnsupportedEncodingException
	 */
	private String getLoginParameters(String html) throws UnsupportedEncodingException{
		List<String> params = new ArrayList<>();
		Timber.i("Extracting form's data...");

		//Parse the HTML document with JSoup
		Document doc = Jsoup.parse(html);

		//Google Form Id
		Elements forms = doc.getElementsByTag("form");
		//Go through the forms
		for (Element formElement : forms) {
			//Find the one with name 'loginform1'
			if (formElement.attr("name").equals("loginform1")){
				//Go through the input elements
				for (Element inputElement : formElement.getElementsByTag("input")){
					//Get the key of the input element
					String key = inputElement.attr("name");

					//Find the username and password elements
					if(key.equals("sid") || key.equals("PIN")){
						String value;
						//Username
						if(key.equals("sid")){
							value = username;
						}
						//Password
						else{
							value = password;
						}

						//Add this to the list if params
						params.add(key + "=" + URLEncoder.encode(value, "UTF-8"));
					}
				}

			}
		}

		//Go through the parameters
		StringBuilder result = new StringBuilder();
		for (String param : params){
			//No & for the first parameter
			if (result.length() == 0){
				result.append(param);
			}
			else{
				result.append("&").append(param);
			}
		}

		return result.toString();
	}

	/**
	 * Gets the request builder with the default headers set up
	 *
	 * @param url The URL to connect to
	 * @return The request builder
	 */
	private Request.Builder getDefaultRequest(String url){
		return new Request.Builder()
				.url(url)
				.cacheControl(new CacheControl.Builder().noCache().build())
				.header("User-Agent", "Mozilla/5.0 (Linux; <Android Version>; <Build Tag etc.>) " +
						"AppleWebKit/<WebKit Rev> (KHTML, like Gecko) Chrome/<Chrome Rev> " +
						"Mobile Safari/<WebKit Rev>")
				.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
				.header("Accept-Language", "en-US,en;q=0.5");
	}

	/**
	 * Check the headers for a bad connection
	 *
	 * @param headers The headers received from the response
	 * @return True if the headers are validated, false otherwise
	 */
    private boolean validateHeaders(Headers headers){
    	//Check for Minerva logout
    	List<String> setCookies = headers.values("Set-Cookie");
    	for(String cookie: setCookies){
	    	if(cookie.contains("SESSID=;")){
			    return false;
	    	}
    	}
    	return true;
    }

    /* URL BUILDERS */

	/**
	 * Returns the URL to download the schedule for a given term
	 *
	 * @param term The term
	 * @return The schedule URL
	 */
    public static String getScheduleURL(Term term) {
        return Connection.SCHEDULE_URL + term.getTermNumber();
    }

    /**
     * Returns the URL to register for courses for the given parameters
     *
     * @param term       The course term
     * @param classes    A list of classes to (un)register for
     * @param dropCourse True if the user is dropping courses, false otherwise
     * @return The proper registration URL
     */
    public static String getRegistrationURL(Term term, List<Course> classes, boolean dropCourse){
        String registrationURL = COURSE_REGISTRATION_URL + term.getTermNumber();

        //Add random Minerva stuff that is apparently necessary
        registrationURL += "&RSTS_IN=DUMMY&assoc_term_in=DUMMY&CRN_IN=DUMMY&start_date_in=DUMMY" +
                "&end_date_in=DUMMY&SUBJ=DUMMY&CRSE=DUMMY&SEC=DUMMY&LEVL=DUMMY" +
                "&CRED=DUMMY&GMOD=DUMMY&TITLE=DUMMY&MESG=DUMMY&REG_BTN=DUMMY&MESG=DUMMY";

        if(dropCourse){
            for(Course classItem : classes){
                registrationURL += "&RSTS_IN=DW&assoc_term_in=" + term.getTermNumber() +
                        "&CRN_IN=" + classItem.getCRN() + "&start_date_in=DUMMY&end_date_in=DUMMY" +
                        "&SUBJ=DUMMY&CRSE=DUMMY&SEC=DUMMY&LEVL=DUMMY&CRED=DUMMY&GMOD=DUMMY&" +
                        "TITLE=DUMMY&MESG=DUMMY";
            }
        }
        else{
            registrationURL += "&RSTS_IN=&assoc_term_in=" + term.getTermNumber() +
		            "&CRN_IN=DUMMY&start_date_in=DUMMY&end_date_in=DUMMY&SUBJ=DUMMY&CRSE=DUMMY&" +
                    "SEC=DUMMYLEVL=DUMMY&CRED=DUMMY&GMOD=DUMMY&TITLE=DUMMY&MESG=DUMMY";
        }

        //Lots of junk
        for(int i = 0; i < 7; i++){
            registrationURL += "&RSTS_IN=&assoc_term_in=DUMMY&CRN_IN=DUMMY&start_date_in=DUMMY" +
		            "&end_date_in=DUMMY&SUBJ=DUMMY&CRSE=DUMMY&SEC=DUMMY&LEVL=DUMMY&CRED=DUMMY&" +
		            "GMOD=DUMMY&TITLE=DUMMY&MESG=DUMMY";
        }

        //More junk
        registrationURL += "&RSTS_IN=&assoc_term_in=DUMMY&CRN_IN=DUMMY&start_date_in=DUMMY" +
                "&end_date_in=DUMMY&SUBJ=DUMMY&CRSE=DUMMY&SEC=DUMMY&LEVL=DUMMY&CRED=DUMMY&" +
		        "GMOD=DUMMY&TITLE=DUMMY";

        //Insert the CRNs into the URL
        for(Course classItem : classes){
            //Use a different URL if courses are being dropped
            if(!dropCourse){
                registrationURL += "&RSTS_IN=RW&CRN_IN=" + classItem.getCRN();
            }
            else{
                registrationURL += "&RSTS_IN=&CRN_IN=";
            }
            registrationURL += "&assoc_term_in=&start_date_in=&end_date_in=";
        }

        registrationURL += "&regs_row=9&wait_row=0&add_row=10&REG_BTN=Submit+Changes";
        return registrationURL;
    }

	/**
	 * Builder for the course search URL
	 */
	public static class SearchURLBuilder {
		/**
		 * Course term
		 */
		private Term term;
		/**
		 * Course subject
		 */
		private String subject;
		/**
		 * Course number
		 */
		private String courseNumber = "";
		/**
		 * Course title
		 */
		private String title = "";
		/**
		 * Course min credit
		 */
		private int minCredit = 0;
		/**
		 * Course max credit
		 */
		private int maxCredit = 0;
		/**
		 * Course start hour
		 */
		private int startHour = 0;
		/**
		 * Course start minute
		 */
		private int startMinute = 0;
		/**
		 * True if the start time is AM, false otherwise
		 */
		private boolean startAM = true;
		/**
		 * Course end hour
		 */
		private int endHour = 0;
		/**
		 * Course end minute
		 */
		private int endMinute = 0;
		/**
		 * True if the end time is AM, false otherwise
		 */
		private boolean endAM = true;
		/**
		 * Course days
		 */
		private List<DayOfWeek> days = new ArrayList<>();

		/**
		 * Default Constructor
		 *
		 * @param term    Course term
		 * @param subject Course subject
		 */
		public SearchURLBuilder(Term term, String subject) {
			this.term = term;
			this.subject = subject;
		}

		/* SETTERS */

		/**
		 * @param courseNumber Course number
		 * @return The builder instance
		 */
		public SearchURLBuilder courseNumber(String courseNumber) {
			this.courseNumber = courseNumber;
			return this;
		}

		/**
		 * @param title Course title
		 * @return The builder instance
		 */
		public SearchURLBuilder title(String title) {
			this.title = title;
			return this;
		}

		/**
		 * @param minCredit Course min credits
		 * @return The builder instance
		 */
		public SearchURLBuilder minCredits(int minCredit) {
			this.minCredit = minCredit;
			return this;
		}

		/**
		 * @param maxCredit Course max credits
		 * @return The builder instance
		 */
		public SearchURLBuilder maxCredits(int maxCredit) {
			this.maxCredit = maxCredit;
			return this;
		}

		/**
		 * @param startHour Course start hour
		 * @return The builder instance
		 */
		public SearchURLBuilder startHour(int startHour) {
			this.startHour = startHour;
			return this;
		}

		/**
		 * @param startMinute Course start minute
		 * @return The builder instance
		 */
		public SearchURLBuilder startMinute(int startMinute) {
			this.startMinute = startMinute;
			return this;
		}

		/**
		 * @param startAM True if the course is AM, false if PM
		 * @return The builder instance
		 */
		public SearchURLBuilder startAM(boolean startAM) {
			this.startAM = startAM;
			return this;
		}

		/**
		 * @param endHour Course end hour
		 * @return The builder instance
		 */
		public SearchURLBuilder endHour(int endHour) {
			this.endHour = endHour;
			return this;
		}

		/**
		 * @param endMinute Course end minute
		 * @return The builder instance
		 */
		public SearchURLBuilder endMinute(int endMinute) {
			this.endMinute = endMinute;
			return this;
		}

		/**
		 * @param endAM True if the ending time is AM, false if PM
		 * @return The builder instance
		 */
		public SearchURLBuilder endAM(boolean endAM) {
			this.endAM = endAM;
			return this;
		}

		/**
		 * @param day Adds a course day
		 * @return The builder instance
		 */
		public SearchURLBuilder addDay(DayOfWeek day) {
			this.days.add(day);
			return this;
		}

		/**
		 * Builds the Course Search URL String
		 *
		 * @return The course search URL to use for this course search
		 */
		public String build() {
			String url = COURSE_SEARCH_URL +
					"term_in=" + term.getTermNumber() +
					"&sel_subj=dummy" +
					"&sel_day=dummy" +
					"&sel_schd=dummy" +
					"&sel_insm=dummy" +
					"&sel_camp=dummy" +
					"&sel_levl=dummy" +
					"&sel_sess=dummy" +
					"&sel_instr=dummy" +
					"&sel_ptrm=dummy" +
					"&sel_attr=dummy" +
					"&sel_subj=" + subject +
					"&sel_crse=" + courseNumber +
					"&sel_title=" + title +
					"&sel_schd=%25" +
					"&sel_from_cred=" + minCredit +
					"&sel_to_cred=" + maxCredit +
					"&sel_levl=%25" +
					"&sel_ptrm=%25" +
					"&sel_instr=%25" +
					"&sel_attr=%25" +
					"&begin_hh=" + startHour +
					"&begin_mi=" + startMinute +
					"&begin_ap=" + (startAM ? 'a' : 'p') +
					"&end_hh=" + endHour +
					"&end_mi=" + endMinute +
					"&end_ap=" + (endAM ? 'a' : 'p');

            for (DayOfWeek day : days) {
				url += "&sel_day=" + DayUtils.getDayChar(day);
			}

			return url;
		}
	}
}
