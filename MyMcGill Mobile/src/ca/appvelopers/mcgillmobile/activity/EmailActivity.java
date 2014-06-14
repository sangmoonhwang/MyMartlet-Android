package ca.appvelopers.mcgillmobile.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import ca.appvelopers.mcgillmobile.R;
import ca.appvelopers.mcgillmobile.activity.base.BaseActivity;
import ca.appvelopers.mcgillmobile.activity.inbox.ReplyActivity;
import ca.appvelopers.mcgillmobile.object.Email;
import ca.appvelopers.mcgillmobile.util.Constants;
import ca.appvelopers.mcgillmobile.util.GoogleAnalytics;


/**
 * Created by Ryan Singzon on 14/02/14.
 * This activity will show a user's individual emails
 */
public class EmailActivity extends BaseActivity {

	Email email;
	 
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email);
        overridePendingTransition(R.anim.right_in, R.anim.left_out);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        GoogleAnalytics.sendScreen(this, "Email - Email");

        //Get email from intent
        email = (Email) getIntent().getSerializableExtra(Constants.EMAIL);

        //Display subject
        TextView emailSubject = (TextView)findViewById(R.id.email_subject);
        emailSubject.setText(email.getSubject());

        //Display email sender
        TextView emailSender = (TextView)findViewById(R.id.email_sender);
        emailSender.setText(email.getSender());

        //Display date received
        TextView emailDate = (TextView)findViewById(R.id.email_date_received);
        emailDate.setText(email.getDateString());

        //Display email body
        WebView emailBody = (WebView)findViewById(R.id.email_body);
        emailBody.getSettings().setLoadWithOverviewMode(true);
        emailBody.getSettings().setUseWideViewPort(false);
        emailBody.getSettings().setBuiltInZoomControls(true);
        emailBody.getSettings().setDisplayZoomControls(false);
        emailBody.loadData(email.getBody(), "text/html", "UTF-8");
        
        final Context context = this;
		new Thread(new Runnable() {
			@Override
			public void run() {
				email.markAsRead(context);
			};

		}).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        	//Returns to parent activity when the top left button is clicked
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.left_in, R.anim.right_out);
                return true;
            // Switches to reply activity    
            case Constants.MENU_ITEM_REPLY:
            	// TODO switch to reply activity
                Intent replyIntent = new Intent(this,ReplyActivity.class);
                replyIntent.putExtra(Constants.EMAIL, email);
                this.startActivity(replyIntent);
            	return true;
            case Constants.MENU_ITEM_FORWARD:
            	// TODO switch to forward activity
                Intent forwardIntent = new Intent(this,ReplyActivity.class);
                this.startActivity(forwardIntent);
            	return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	// reply menu item
    	menu.add(Menu.NONE, Constants.MENU_ITEM_REPLY, Menu.NONE,R.string.email_reply);
    	// forward menu item
    	menu.add(Menu.NONE, Constants.MENU_ITEM_FORWARD, Menu.NONE,R.string.email_forward);
    	return super.onCreateOptionsMenu(menu);
    }
    
    
    // Joshua David Alfaro
    public void replyMessage(View view) {
    	Intent intent = new Intent(this, ReplyActivity.class);
		intent.putExtra(Constants.EMAIL, email);
    	startActivity(intent);
    }
}
