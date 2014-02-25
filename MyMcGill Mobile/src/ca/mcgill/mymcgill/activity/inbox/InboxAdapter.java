package ca.mcgill.mymcgill.activity.inbox;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import ca.mcgill.mymcgill.R;
import ca.mcgill.mymcgill.activity.EmailActivity;
import ca.mcgill.mymcgill.object.Email;
import ca.mcgill.mymcgill.object.Inbox;
import ca.mcgill.mymcgill.util.ApplicationClass;
import ca.mcgill.mymcgill.util.Constants;

/**
 * This InboxAdapter will populate the list of adapters in InboxActivity
 * Created by Ryan Singzon on 15/02/14.
 */

public class InboxAdapter extends BaseAdapter{
    private Context mContext;
    private List<Email> mEmails;

    public InboxAdapter(Context context, Inbox inbox){
        this.mContext = context;
        this.mEmails = inbox.getEmails();
    }

    @Override
    public int getCount() {
        return mEmails.size();
    }

    @Override
    public Email getItem(int position){
        return mEmails.get(position);
    }


    @Override
    public long getItemId(int position){
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent){
        //Reuse previously used views
        if(view == null){
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.activity_inbox_email, null);
        }

        //Get the email to inflate
        final Email email = getItem(position);

        //Set up information
        TextView emailSubject = (TextView)view.findViewById(R.id.email_subject);
        emailSubject.setText(email.getSubject());

        TextView emailSender = (TextView)view.findViewById(R.id.email_sender);
        emailSender.setText(email.getSender());

        //Make subject bold if unread
        if(!email.isRead()){
            emailSubject.setTypeface(emailSubject.getTypeface(), Typeface.BOLD);
        }

        //Place chevron
        TextView chevron = (TextView)view.findViewById(R.id.email_chevron);
        chevron.setTypeface(ApplicationClass.getIconFont());

        //Set up onClickListener for view
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, EmailActivity.class);
                intent.putExtra(Constants.EMAIL, email);
                mContext.startActivity(intent);
            }
        });

        return view;
    }
}