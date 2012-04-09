package com.gimranov.zandy.app;

import com.gimranov.zandy.app.data.Item;

import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView.BufferType;

public class ZandyDialogFragment extends DialogFragment {
	static final int DIALOG_NEW = 1;
	static final int DIALOG_TAG = 2;
	static final int DIALOG_NOTE = 3;
	static final int DIALOG_CONFIRM_NAVIGATE = 4;
	static final int DIALOG_CONFIRM_DELETE = 5;
	static final int DIALOG_CHOOSE_COLLECTION = 6;
	private static final String TAG = "com.gimranov.zandy.app.ZandyDialogFragment";
	DialogClickMethods parent;
	private Bundle bundle;

	public static ZandyDialogFragment newInstance(DialogClickMethods parent,
			final Bundle bundleRef) {
		final ZandyDialogFragment frag = new ZandyDialogFragment();
		frag.bundle = bundleRef;
		frag.parent = parent;
		return frag;
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstagggnceState) {
		// savedInstanceState=bundle;
		final int id = bundle.getInt("id");
		int title = bundle.getInt("title");

		switch (id) {
		case DIALOG_TAG:
			final String tag = bundle.getString("tag");
			final EditText input = new EditText(getActivity());
			input.setText(tag, BufferType.EDITABLE);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
	    	    	.setTitle(title)
	    	    	.setView(input)
	    	    	.setPositiveButton(R.string.ok,
	    	    			new DialogInterface.OnClickListener() {
	    	    				public void onClick(DialogInterface dialog,
	    	    						int whichButton) {
	    		    	            Editable value = input.getText();
	    		    	            String fixed=value.toString();
	    							bundle.putString("fixed", fixed);
	    	    					parent.doPositiveClick(bundle);
	    	    				}
	    	        		})
	    	        .setNeutralButton(R.string.cancel,
	    	        		new DialogInterface.OnClickListener() {
	    	        			public void onClick(DialogInterface dialog,
	    	        					int whichButton) {
									parent.doNeutralClick(bundle);
	    	        			}
	    	        		});
			if(!tag.equals(""))
				builder.setNegativeButton(R.string.menu_delete,
    	        		new DialogInterface.OnClickListener() {
        					public void onClick(DialogInterface dialog,
        								int whichButton) {
        							parent.doNegativeClick(bundle);
        						}		
        				});
			return builder.create();
		case DIALOG_CONFIRM_DELETE:
			return new AlertDialog.Builder(getActivity())
					// .setIcon(R.drawable.alert_dialog_icon)
					.setTitle(title)
					.setPositiveButton(R.string.menu_delete,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									parent.doPositiveClick(bundle);
								}
							})
					.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									parent.doNegativeClick(bundle);
								}
							}).create();
		case DIALOG_CONFIRM_NAVIGATE:
			return new AlertDialog.Builder(getActivity())
					.setTitle(title)
					.setPositiveButton(getResources().getString(R.string.view),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									parent.doPositiveClick(bundle);
								}
							})
					.setNeutralButton(
							getResources().getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									parent.doNeutralClick(bundle);
								}
							}).create();
		case DIALOG_NOTE:
			final EditText input1 = new EditText(getActivity());
			input1.setText(bundle.getString("content"), BufferType.EDITABLE);

			AlertDialog.Builder builder1 = new AlertDialog.Builder(getActivity())
					.setTitle(getResources().getString(R.string.note))
					.setView(input1)
					.setPositiveButton(getResources().getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									Editable value = input1.getText();
									String fixed = value.toString().replaceAll(
											"\n\n", "\n<br>");
									bundle.putString("fixed", fixed);
									parent.doPositiveClick(bundle);
								}
							})
					.setNeutralButton(
							getResources().getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									parent.doNeutralClick(bundle);
								}
							});
			if (bundle.getString("mode") == null
					|| !bundle.getString("mode").equals("new"))
				builder1.setNegativeButton(
						getResources().getString(R.string.menu_delete),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								parent.doNegativeClick(bundle);
							}
						});
			return builder1.create();
		case DIALOG_CHOOSE_COLLECTION:
			return new AlertDialog.Builder(getActivity()).setTitle(getResources().getString(R.string.choose_parent_collection))
		    	    .setItems(bundle.getStringArray("collectionNames"), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int pos) {
							bundle.putInt("pos",pos);
							parent.doPositiveClick(bundle);
		    	        }
		    	    }).create();
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