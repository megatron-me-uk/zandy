package com.gimranov.zandy.app;

import com.gimranov.zandy.app.data.Attachment;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.task.APIRequest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView.BufferType;


public class ZandyDialogFragment extends DialogFragment {
	static final int DIALOG_CONFIRM_DELETE = 5;
	static final int DIALOG_CONFIRM_NAVIGATE = 4;	
	static final int DIALOG_FILE_PROGRESS = 6;	
	static final int DIALOG_NOTE = 3;
	static final int DIALOG_NEW = 1;
	private static final String TAG = "com.gimranov.zandy.app.ZandyDialogFragment";
	DialogClickMethods parent;
	private Bundle bundle;
	
    public static ZandyDialogFragment newInstance(DialogClickMethods parent,final Bundle bundleRef) {
    	final ZandyDialogFragment frag = new ZandyDialogFragment();
    	frag.bundle=bundleRef;
    	frag.parent=parent;
        return frag;
    }
    
    @Override
    public Dialog onCreateDialog(final Bundle savedInstagggnceState) {
    	//savedInstanceState=bundle;
        final int id = bundle.getInt("id");
        int title = bundle.getInt("title");

		ProgressDialog mProgressDialog;
		switch (id) {
			case DIALOG_CONFIRM_DELETE:
				return new AlertDialog.Builder(getActivity())
	                //.setIcon(R.drawable.alert_dialog_icon)
	                .setTitle(title)
	                .setPositiveButton(R.string.menu_delete,
	                    new DialogInterface.OnClickListener() {
	                        public void onClick(DialogInterface dialog, int whichButton) {
	                        	parent.doPositiveClick(bundle);
	                        }
	                    }
	                )
	                .setNegativeButton(R.string.cancel,
	                    new DialogInterface.OnClickListener() {
	                        public void onClick(DialogInterface dialog, int whichButton) {
	                        	parent.doNegativeClick(bundle);
	                        }
	                    }
	                ).create();
			case DIALOG_CONFIRM_NAVIGATE:
				return new AlertDialog.Builder(getActivity())
	    	    .setTitle(title)
	    	    .setPositiveButton(getResources().getString(R.string.view), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
                    	parent.doPositiveClick(bundle);
	    	        }
	    	    }).setNeutralButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
	    	        public void onClick(DialogInterface dialog, int whichButton) {
                    	parent.doNeutralClick(bundle);
	    	        }
	    	    }).create();
			case DIALOG_NOTE:
				final EditText input = new EditText(getActivity().getBaseContext() );
				input.setText(bundle.getString("content"), BufferType.EDITABLE);
				
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity().getBaseContext())
		    	    .setTitle(getResources().getString(R.string.note))
		    	    .setView(input)
		    	    .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
						@SuppressWarnings("unchecked")
						public void onClick(DialogInterface dialog, int whichButton) {
							Editable value = input.getText();
		    	            String fixed = value.toString().replaceAll("\n\n", "\n<br>");
		    	            bundle.putString("fixed", fixed);
							parent.doPositiveClick(bundle);
		    	        }
		    	    }).setNeutralButton(getResources().getString(R.string.cancel),
		    	    		new DialogInterface.OnClickListener() {
		    	        public void onClick(DialogInterface dialog, int whichButton) {
		    	        	// do nothing
		    	        }
		    	    });
				return builder.create();
			case DIALOG_FILE_PROGRESS:
				mProgressDialog = new ProgressDialog(getActivity().getBaseContext());
				mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				mProgressDialog.setMessage(getResources().getString(R.string.attachment_downloading, bundle.getString("title")));
				mProgressDialog.setIndeterminate(true);
				return mProgressDialog;
			default:
				Log.e(TAG, "Invalid dialog requested");
				return null;
		}
    }
}

interface DialogClickMethods {
public void doPositiveClick(Bundle savedInstanceState);
public void doNegativeClick(Bundle savedInstanceState);
public void doNeutralClick(Bundle savedInstanceState);
}