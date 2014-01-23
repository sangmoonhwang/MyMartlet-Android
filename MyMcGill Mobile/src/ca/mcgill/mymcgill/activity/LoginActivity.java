package ca.mcgill.mymcgill.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import ca.mcgill.mymcgill.R;
import ca.mcgill.mymcgill.util.Connection;
import ca.mcgill.mymcgill.util.Constants;

/**
 * Author: Julien
 * Date: 22/01/14, 7:34 PM
 */
public class LoginActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Get the necessary views
        final EditText usernameView = (EditText) findViewById(R.id.login_username);
        final EditText passwordView = (EditText) findViewById(R.id.login_password);
        Button login = (Button) findViewById(R.id.login_button);

        //Check if an error message needs to be displayed, display it if so
        int connectionStatus = getIntent().getIntExtra(Constants.CONNECTION_STATUS, -1);
        if(connectionStatus == Constants.CONNECTION_WRONG_INFO){
            showErrorDialog(getResources().getString(R.string.login_error_wrong_data));
        }
        else if(connectionStatus == Constants.CONNECTION_OTHER){
            showErrorDialog(getResources().getString(R.string.login_error_other));
        }

        //Fill out the username EditText if it is stored in the SharedPrefs
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = sharedPrefs.getString(Constants.USERNAME, null);

        if(username != null){
            usernameView.setText(username);
        }

        //Set up the OnClickListener for the login button
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Get the username text
                String username = usernameView.getText().toString().trim();

                //Get the password text
                String password = passwordView.getText().toString().trim();

                //Check that both of them are not empty, create appropriate error messages if so
                if(username.isEmpty()){
                    showErrorDialog(getResources().getString(R.string.login_error_username_empty));
                    return;
                }
                else if(password.isEmpty()){
                    showErrorDialog(getResources().getString(R.string.login_error_password_empty));
                    return;
                }

                //Connect
                int connectionStatus = Connection.connect(LoginActivity.this, username, password);

                //If the connection was successful, go to MainActivity
                if(connectionStatus == Constants.CONNECTION_OK){
                    //Store the login info in the shared prefs.
                    sharedPrefs.edit()
                            .putString(Constants.USERNAME, username)
                            .putString(Constants.PASSWORD, password)
                            .commit();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }
                //If the wrong data was given, show an alert dialog explaining this
                else if(connectionStatus == Constants.CONNECTION_WRONG_INFO){
                    showErrorDialog(getResources().getString(R.string.login_error_wrong_data));
                }
                //Show general error dialog
                else if(connectionStatus == Constants.CONNECTION_OTHER){
                    showErrorDialog(getResources().getString(R.string.login_error_other));
                }
            }
        });
    }

    public void showErrorDialog(String errorMessage){
        new AlertDialog.Builder(LoginActivity.this)
                .setTitle(getResources().getString(R.string.error))
                .setMessage(errorMessage)
                .setNeutralButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }
}