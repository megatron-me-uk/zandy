/*******************************************************************************
 * This file is part of Zandy.
 * 
 * Zandy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Zandy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with Zandy.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.gimranov.zandy.app;

import java.io.File;
import java.util.ArrayList;

import org.json.JSONException;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.gimranov.zandy.app.data.Attachment;
import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.task.APIRequest;
import com.gimranov.zandy.app.task.ZoteroAPITask;

/**
 * This Activity handles displaying and editing attachments. It works almost the same as
 * ItemDataActivity and TagActivity, using a simple ArrayAdapter on Bundles with the creator info.
 * 
 * This currently operates by showing the attachments for a given item
 * 
 * @author ajlyon
 *
 */
public class AttachmentActivity extends FragmentActivity implements DialogClickMethods {

	private static final String TAG = "com.gimranov.zandy.app.AttachmentActivity";

	private static final int CONTENT_VIEW_ID = 1;
	
	public Item item;
	public String itemKey;
	private ProgressDialog mProgressDialog;
	private ProgressThread progressThread;
	private Database db;
	MyListFragment listFragment;
	public class MyListFragment extends ListFragment {
		public void onViewCreated(View view, Bundle savedInstanceState){
			if(this.getListView()==null)
				return;
	        ListView lv = listFragment. getListView();
	        lv.setTextFilterEnabled(true);
	        lv.setOnItemClickListener(new OnItemClickListener() {
	        	// Warning here because Eclipse can't tell whether my ArrayAdapter is
	        	// being used with the correct parametrization.
	        	@SuppressWarnings("unchecked")
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	        		// If we have a click on an entry, show its note
	        		ArrayAdapter<Attachment> adapter = (ArrayAdapter<Attachment>) parent.getAdapter();
	        		Attachment row = adapter.getItem(position);
	        		
	        		if (row.content.has("note")) {
		    	    	Log.d(TAG, "Trying to start note view activity for: "+row.key);
		    	    	Intent i = new Intent(getBaseContext(), NoteActivity.class);
		    	    	i.putExtra("com.gimranov.zandy.app.attKey", row.key);//row.content.optString("note", ""));
		    	    	startActivity(i);
					}
	        	}
	        });
	        lv.setOnItemLongClickListener(new OnItemLongClickListener() {
	        	// Warning here because Eclipse can't tell whether my ArrayAdapter is
	        	// being used with the correct parametrization.
	        	@SuppressWarnings("unchecked")
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
	        		// If we have a long click on an entry, do something...
	        		ArrayAdapter<Attachment> adapter = (ArrayAdapter<Attachment>) parent.getAdapter();
	        		Attachment row = adapter.getItem(position);
	        		String url = (row.url != null && !row.url.equals("")) ?
	        				row.url : row.content.optString("url");
	        		
					if (!row.getType().equals("note")) {
						Bundle b = new Bundle();
	        			b.putString("title", row.title);
	        			b.putString("attachmentKey", row.key);
	        			b.putString("content", url);
	    				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	        			int linkMode = row.content.optInt("linkMode", Attachment.MODE_LINKED_URL);
	        			
	        			if (settings.getBoolean("webdav_enabled", false))
	        				b.putString("mode", "webdav");
	        			else
	        				b.putString("mode", "zfs");
	        			
	        			if (linkMode == Attachment.MODE_IMPORTED_FILE
	        					|| linkMode == Attachment.MODE_IMPORTED_URL) {
	        				loadFileAttachment(b);
	        			} else {
	        				//AttachmentActivity.this.b = b;
	        				b.putInt("id",ZandyDialogFragment.DIALOG_NOTE);
			        		b.putInt("title",R.string.view_online_warning);
	        				ZandyDialogFragment newFragment = ZandyDialogFragment.newInstance(AttachmentActivity.this,b);
	        		        newFragment.show(getSupportFragmentManager(), "view_online_warning");
	        			}
					}
	        		
					if (row.getType().equals("note")) {
						Bundle b = new Bundle();
						b.putString("attachmentKey", row.key);
						b.putString("itemKey", itemKey);
						b.putString("content", row.content.optString("note", ""));
						//removeDialog(DIALOG_NOTE);
						//AttachmentActivity.this.b = b;
						b.putInt("id",ZandyDialogFragment.DIALOG_NOTE);
						b.putInt("title",R.string.view_online_warning);
	    				ZandyDialogFragment newFragment = ZandyDialogFragment.newInstance(AttachmentActivity.this,b);
	    		        newFragment.show(getSupportFragmentManager(), "view_online_warning");
					}
					return true;
	        	}
	        });
			
		}
	}
	
	/** 
	 * For <= Android 2.1 (API 7), we can't pass bundles to showDialog(), so set this instead
	 */
	//having a flexible container available over more than one thread could make debugging crazy difficult
	//private Bundle b = new Bundle();
	
	private ArrayList<File> tmpFiles;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tmpFiles = new ArrayList<File>();
        
        db = new Database(this);
        
        /* Get the incoming data from the calling activity */
        final String itemKey = getIntent().getStringExtra("com.gimranov.zandy.app.itemKey");
        Item item = Item.load(itemKey, db);
        this.item = item;
        this.itemKey=itemKey;
        
        if (item == null) {
        	Log.e(TAG, "AttachmentActivity started without itemKey; finishing.");
        	finish();
        	return;
        }
        
        this.setTitle(getResources().getString(R.string.attachments_for_item,item.getTitle()));
        
        ArrayList<Attachment> rows = Attachment.forItem(item, db);
        
        /* 
         * We use the standard ArrayAdapter, passing in our data as a Attachment.
         * Since it's no longer a simple TextView, we need to override getView, but
         * we can do that anonymously.
         */
        listFragment=new MyListFragment();
        listFragment.setListAdapter(new ArrayAdapter<Attachment>(this, R.layout.list_attach, rows) {
        	@Override
        	public View getView(int position, View convertView, ViewGroup parent) {
        		View row;
        		
                // We are reusing views, but we need to initialize it if null
        		if (null == convertView) {
                    LayoutInflater inflater = getLayoutInflater();
        			row = inflater.inflate(R.layout.list_attach, null);
        		} else {
        			row = convertView;
        		}

        		ImageView tvType = (ImageView)row.findViewById(R.id.attachment_type);
        		TextView tvSummary = (TextView)row.findViewById(R.id.attachment_summary);
        		
        		Attachment att = getItem(position);
        		Log.d(TAG, "Have an attachment: "+att.title + " fn:"+att.filename + " status:" + att.status);
        		
        		tvType.setImageResource(Item.resourceForType(att.getType()));
        		
        		try {
					Log.d(TAG, att.content.toString(4));
				} catch (JSONException e) {
					Log.e(TAG, "JSON parse exception when reading attachment content", e);
				}
        		
        		if (att.getType().equals("note")) {
        			String note = att.content.optString("note","");
        			if (note.length() > 40) {
        				note = note.substring(0,40);
        			}
        			tvSummary.setText(note);
        		} else {
        			StringBuffer status = new StringBuffer(getResources().getString(R.string.status));
        			if (att.status == Attachment.AVAILABLE)
        				status.append(getResources().getString(R.string.attachment_zfs_available));
        			else if (att.status == Attachment.LOCAL)
        				status.append(getResources().getString(R.string.attachment_zfs_local));
        			else
        				status.append(getResources().getString(R.string.attachment_unknown));
        			tvSummary.setText(att.title + " " + status.toString());
        		}
        		return row;
        	}
        });
        //MyListFragment.instantiate(getBaseContext(), listFragment.getClass().getName());
        FrameLayout frame = new FrameLayout(this);
        frame.setId(CONTENT_VIEW_ID);
        setContentView(frame);

        if (savedInstanceState == null) {
        	getSupportFragmentManager().beginTransaction().add(CONTENT_VIEW_ID, listFragment).commit();
        }
        /*
        ListView lv=listFragment.getListView();
        lv.setTextFilterEnabled(true);
        lv.setOnItemClickListener(new OnItemClickListener() {
        	// Warning here because Eclipse can't tell whether my ArrayAdapter is
        	// being used with the correct parametrization.
        	@SuppressWarnings("unchecked")
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        		// If we have a click on an entry, show its note
        		ArrayAdapter<Attachment> adapter = (ArrayAdapter<Attachment>) parent.getAdapter();
        		Attachment row = adapter.getItem(position);
        		
        		if (row.content.has("note")) {
	    	    	Log.d(TAG, "Trying to start note view activity for: "+row.key);
	    	    	Intent i = new Intent(getBaseContext(), NoteActivity.class);
	    	    	i.putExtra("com.gimranov.zandy.app.attKey", row.key);//row.content.optString("note", ""));
	    	    	startActivity(i);
				}
        	}
        });
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {
        	// Warning here because Eclipse can't tell whether my ArrayAdapter is
        	// being used with the correct parametrization.
        	@SuppressWarnings("unchecked")
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        		// If we have a long click on an entry, do something...
        		ArrayAdapter<Attachment> adapter = (ArrayAdapter<Attachment>) parent.getAdapter();
        		Attachment row = adapter.getItem(position);
        		String url = (row.url != null && !row.url.equals("")) ?
        				row.url : row.content.optString("url");
        		
				if (!row.getType().equals("note")) {
					Bundle b = new Bundle();
        			b.putString("title", row.title);
        			b.putString("attachmentKey", row.key);
        			b.putString("content", url);
    				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        			int linkMode = row.content.optInt("linkMode", Attachment.MODE_LINKED_URL);
        			
        			if (settings.getBoolean("webdav_enabled", false))
        				b.putString("mode", "webdav");
        			else
        				b.putString("mode", "zfs");
        			
        			if (linkMode == Attachment.MODE_IMPORTED_FILE
        					|| linkMode == Attachment.MODE_IMPORTED_URL) {
        				loadFileAttachment(b);
        			} else {
        				//AttachmentActivity.this.b = b;
        				b.putInt("id",ZandyDialogFragment.DIALOG_NOTE);
		        		b.putInt("title",R.string.view_online_warning);
        				ZandyDialogFragment newFragment = ZandyDialogFragment.newInstance(AttachmentActivity.this,b);
        		        newFragment.show(getSupportFragmentManager(), "view_online_warning");
        			}
				}
        		
				if (row.getType().equals("note")) {
					Bundle b = new Bundle();
					b.putString("attachmentKey", row.key);
					b.putString("itemKey", itemKey);
					b.putString("content", row.content.optString("note", ""));
					//removeDialog(DIALOG_NOTE);
					//AttachmentActivity.this.b = b;
					b.putInt("id",ZandyDialogFragment.DIALOG_NOTE);
					b.putInt("title",R.string.view_online_warning);
    				ZandyDialogFragment newFragment = ZandyDialogFragment.newInstance(AttachmentActivity.this,b);
    		        newFragment.show(getSupportFragmentManager(), "view_online_warning");
				}
				return true;
        	}
        });*/
    }
    
    @Override
    public void onDestroy() {
    	if (db != null) db.close();
    	
    	if (tmpFiles != null) {
    		for (File f : tmpFiles) {
    			if (!f.delete()) {
    				Log.e(TAG, "Failed to delete temporary file on activity close.");
    			}
    		}
    		
    		tmpFiles.clear();
    	}
    	
    	super.onDestroy();
    }
    
    @Override
    public void onResume() {
    	if (db == null) db = new Database(this);
    	super.onResume();
    }
	
	private void showAttachment(Attachment att) {
		if (att.status == Attachment.LOCAL) {
			Log.d(TAG,"Starting to display local attachment");
			Uri uri = Uri.fromFile(new File(att.filename));
			String mimeType = att.content.optString("mimeType",null);
			try {
				startActivity(new Intent(Intent.ACTION_VIEW)
				.setDataAndType(uri,mimeType));
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "No activity for intent", e);
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.attachment_intent_failed, mimeType), 
						Toast.LENGTH_SHORT).show();
			}
		}	
	}
	
	/**
	 * This mainly is to move the logic out of the onClick callback above
	 * Decides whether to download or view, and launches the appropriate action
	 * @param b
	 */
	private void loadFileAttachment(Bundle b) {
		Attachment att = Attachment.load(b.getString("attachmentKey"), db);
		if (!ServerCredentials.sBaseStorageDir.exists())
			ServerCredentials.sBaseStorageDir.mkdirs();
		if (!ServerCredentials.sDocumentStorageDir.exists())
			ServerCredentials.sDocumentStorageDir.mkdirs();
		
		File attFile = new File(att.filename);
		
		if (att.status == Attachment.AVAILABLE
				// Zero-length or nonexistent gives length == 0
				|| (attFile != null && attFile.length() == 0)) {				
			Log.d(TAG,"Starting to try and download attachment (status: "+att.status+", fn: "+att.filename+")");
			//this.b = b;

			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setMessage(getResources().getString(R.string.attachment_downloading, R.string.attachment_downloading));
			mProgressDialog.setIndeterminate(true);
			
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			progressThread = new ProgressThread(handler, b, settings, db, tmpFiles);
			progressThread.start();

		} else showAttachment(att);
	}
	
	/**
	 * Refreshes the current list adapter
	 */
	@SuppressWarnings("unchecked")
	private void refreshView() {
		ArrayAdapter<Attachment> la = (ArrayAdapter<Attachment>) listFragment.getListAdapter();
		la.clear();
		for (Attachment at : Attachment.forItem(item, db)) {
			la.add(at);
		}
	}
	
	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.arg2) {
			case ProgressThread.STATE_DONE:
				if(mProgressDialog.isShowing()){
					//dismiss
				}
				refreshView();
				if (null != msg.obj)
					showAttachment((Attachment)msg.obj);
				break;
			case ProgressThread.STATE_FAILED:
				// Notify that we failed to get anything
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.attachment_no_download_url), 
	    				Toast.LENGTH_SHORT).show();
	        	
				if(mProgressDialog.isShowing())
					mProgressDialog.dismiss();
				// Let's try to fall back on an online version
				Bundle bundle = msg.getData();
				bundle.putInt("id",ZandyDialogFragment.DIALOG_CONFIRM_NAVIGATE);
				bundle.putInt("title", R.string.view_online_warning);
		        ZandyDialogFragment newFragment = ZandyDialogFragment.newInstance(AttachmentActivity.this,bundle);
		        newFragment.show(getSupportFragmentManager(), "DIALOG_CONFIRM_NAVIGATE");
				
				refreshView();
				break;
			case ProgressThread.STATE_UNZIPPING:
				mProgressDialog.setMax(msg.arg1);
				mProgressDialog.setProgress(0);
				mProgressDialog.setMessage(getResources().getString(R.string.attachment_unzipping));
				break;
			case ProgressThread.STATE_RUNNING:
				mProgressDialog.setMax(msg.arg1);
				mProgressDialog.setProgress(0);
				mProgressDialog.setIndeterminate(false);
				break;
			default:
				mProgressDialog.setProgress(msg.arg1);
				break;

			}
		}
	};
	

               
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.zotero_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		Bundle b = new Bundle();
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.do_sync:
        	if (!ServerCredentials.check(getApplicationContext())) {
            	Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_log_in_first), 
        				Toast.LENGTH_SHORT).show();
            	return true;
        	}
        	Log.d(TAG, "Preparing sync requests, starting with present item");
        	new ZoteroAPITask(getBaseContext()).execute(APIRequest.update(this.item));
        	Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_started), 
    				Toast.LENGTH_SHORT).show();
        	
        	return true;
        case R.id.do_new:
			b.putString("itemKey", this.item.getKey());
			b.putString("mode", "new");
			//getFragmentManager().//removeDialog()?
        	//this.b = b;
			final EditText input = new EditText(this);
			input.setText(b.getString("content"), BufferType.EDITABLE);
			b.putInt("id",ZandyDialogFragment.DIALOG_NOTE);
			b.putInt("title",R.string.note);
	        ZandyDialogFragment newFragment = ZandyDialogFragment.newInstance(this,b);
	        newFragment.show(getSupportFragmentManager(), "note");
            return true;
        case R.id.do_prefs:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void doPositiveClick(Bundle bundle) {
    	switch(bundle.getInt("id")){
    	case ZandyDialogFragment.DIALOG_CONFIRM_NAVIGATE:
    		// The behavior for invalid URIs might be nasty, but
    		// we'll cross that bridge if we come to it.
    		try {
    			Uri uri = Uri.parse(bundle.getString("content"));
    			startActivity(new Intent(Intent.ACTION_VIEW)
    			.setData(uri));
    		} catch (ActivityNotFoundException e) {
    			// There can be exceptions here; not sure what would prompt us to have
    			// URIs that the browser can't load, but it apparently happens.
    			Toast.makeText(getApplicationContext(),
    					getResources().getString(R.string.attachment_intent_failed_for_uri, bundle.getString("content")), 
    					Toast.LENGTH_SHORT).show();
    		}
    		break;
    	case ZandyDialogFragment.DIALOG_CONFIRM_DELETE:
    		Attachment a = Attachment.load(bundle.getString("attachmentKey"), db);
    		a.delete(db);
    		@SuppressWarnings("unchecked")
    		ArrayAdapter<Attachment> la = (ArrayAdapter<Attachment>) listFragment.getListAdapter();
    		la.clear();
    		for (Attachment at : Attachment.forItem(Item.load(bundle.getString("itemKey"), db), db)) {
    			la.add(at);
    		}
    		break;
    	case ZandyDialogFragment.DIALOG_NOTE:
    		String fixed = bundle.getString("fixed");
    		if (bundle.getString("mode") != null && bundle.getString("mode").equals("new")) {
    			Log.d(TAG, "Attachment created with parent key: "+bundle.getString("itemKey"));
    			Attachment att = new Attachment(getBaseContext(), "note", bundle.getString("itemKey"));
    			att.setNoteText(fixed);
    			att.dirty = APIRequest.API_NEW;
    			att.save(db);
    		} else {
    			Attachment att = Attachment.load(bundle.getString("attachmentKey"), db);
    			att.setNoteText(fixed);
    			att.dirty = APIRequest.API_DIRTY;
    			att.save(db);
    		}
    		@SuppressWarnings("unchecked")
			ArrayAdapter<Attachment> la1 = (ArrayAdapter<Attachment>) listFragment.getListAdapter();
    		la1.clear();
    		for (Attachment a1 : Attachment.forItem(Item.load(bundle.getString("itemKey"), db), db)) {
    			la1.add(a1);
    		}
    		la1.notifyDataSetChanged();
    		break;
    	default:
    		Log.e(TAG, "Invalid dialog requested");
    	}
    }

	@Override
	public void doNeutralClick(Bundle bundle) {
		switch (bundle.getInt("id"))
		{
			case ZandyDialogFragment.DIALOG_NOTE:
				//do nothing
				break;
			case ZandyDialogFragment.DIALOG_CONFIRM_NAVIGATE:
				//do nothing
				break;
		    default:
		       	Log.e("doNeutralClick","Oops",new Exception(getResources().getString(bundle.getInt("title"))));
		       	break;
		}
		
	}
	
	@Override
	public void doNegativeClick(Bundle bundle) {
		switch (bundle.getInt("id"))
		{
			case ZandyDialogFragment.DIALOG_NOTE:
	            bundle.putInt("id", ZandyDialogFragment.DIALOG_CONFIRM_DELETE);
	            bundle.putInt("title", R.string.attachment_delete_confirm);
	        	//removeDialog(DIALOG_CONFIRM_DELETE);
		        ZandyDialogFragment newFragment = ZandyDialogFragment.newInstance(this,bundle);
		        newFragment.show(getSupportFragmentManager(), "attachment_delete_confirm");
		        break;
			case ZandyDialogFragment.DIALOG_CONFIRM_DELETE:
				//do nothing
				break;
		    default:
		       	Log.e("doNegativeClick","Oops",new Exception(getResources().getString(bundle.getInt("title"))));
		       	break;
		}
	}
}
