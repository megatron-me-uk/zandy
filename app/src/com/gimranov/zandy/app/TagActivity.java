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

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
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
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.task.APIRequest;
import com.gimranov.zandy.app.task.ZoteroAPITask;

/**
 * This Activity handles displaying and editing tags. It works almost the same as
 * ItemDataActivity, using a simple ArrayAdapter on Bundles with the tag info.
 * 
 * @author ajlyon
 *
 */
public class TagActivity extends FragmentActivity implements DialogClickMethods {

	private static final String TAG = "com.gimranov.zandy.app.TagActivity";

	private static final int CONTENT_VIEW_ID = 1;	
	
	private Item item;
	
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
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	        		// If we have a click on an entry, prompt to view that tag's items.
	        		@SuppressWarnings("unchecked")
					ArrayAdapter<Bundle> adapter = (ArrayAdapter<Bundle>) parent.getAdapter();
	        		Bundle row = adapter.getItem(position);
	        		row.putInt("id",ZandyDialogFragment.DIALOG_CONFIRM_NAVIGATE);
	        		row.putInt("title",R.string.tag_view_confirm);
					ZandyDialogFragment newFragment = ZandyDialogFragment.newInstance(TagActivity.this,row);
			        newFragment.show(getSupportFragmentManager(), "tag_view_confirm");
	        	}
	        });
	        
	        /*
	         * On long click, we bring up an edit dialog.
	         */
	        lv.setOnItemLongClickListener(new OnItemLongClickListener() {
	        	/*
	        	 * Same annotation as in onItemClick(..), above.
	        	 */
	        	@SuppressWarnings("unchecked")
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
	     			// If we have a long click on an entry, show an editor
	        		ArrayAdapter<Bundle> adapter = (ArrayAdapter<Bundle>) parent.getAdapter();
	        		Bundle row = adapter.getItem(position);
	        		
    				row.putInt("id",ZandyDialogFragment.DIALOG_TAG);
	        		row.putInt("title",R.string.tag_edit);
    				ZandyDialogFragment newFragment = ZandyDialogFragment.newInstance(TagActivity.this,row);
    		        newFragment.show(getSupportFragmentManager(), "tag_edit");
	        		return true;
	          }
	        });
		}
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        db = new Database(this);
                
        /* Get the incoming data from the calling activity */
        String itemKey = getIntent().getStringExtra("com.gimranov.zandy.app.itemKey");
        Item item = Item.load(itemKey, db);
        this.item = item;
        
        this.setTitle(getResources().getString(R.string.tags_for_item, item.getTitle()));
        
        ArrayList<Bundle> rows = item.tagsToBundleArray();
        
        /* 
         * We use the standard ArrayAdapter, passing in our data as a Bundle.
         * Since it's no longer a simple TextView, we need to override getView, but
         * we can do that anonymously.
         */
        listFragment=new MyListFragment();
        listFragment.setListAdapter(new ArrayAdapter<Bundle>(this, R.layout.list_data, rows) {
        	@Override
        	public View getView(int position, View convertView, ViewGroup parent) {
        		View row;
        		
                // We are reusing views, but we need to initialize it if null
        		if (null == convertView) {
                    LayoutInflater inflater = getLayoutInflater();
        			row = inflater.inflate(R.layout.list_data, null);
        		} else {
        			row = convertView;
        		}
         
        		/* Our layout has just two fields */
        		TextView tvLabel = (TextView) row.findViewById(R.id.data_label);
        		TextView tvContent = (TextView) row.findViewById(R.id.data_content);
        		
        		if (getItem(position).getInt("type") == 1)
        			tvLabel.setText(getResources().getString(R.string.tag_auto));
        		else
        			tvLabel.setText(getResources().getString(R.string.tag_user));
        		tvContent.setText(getItem(position).getString("tag"));
         
        		return row;
        	}
        });
        FrameLayout frame = new FrameLayout(this);
        frame.setId(CONTENT_VIEW_ID);
        setContentView(frame);
        
        if (savedInstanceState == null) {
        	getSupportFragmentManager().beginTransaction().add(CONTENT_VIEW_ID, listFragment).commit();
        }
        

    }
    
    @Override
    public void onDestroy() {
    	if (db != null) db.close();
    	super.onDestroy();
    }
    
    @Override
    public void onResume() {
    	if (db == null) db = new Database(this);
    	super.onResume();
    }
               
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.zotero_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.do_sync:
        	if (!ServerCredentials.check(getApplicationContext())) {
            	Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_log_in_first), 
        				Toast.LENGTH_SHORT).show();
            	return true;
        	}
        	Log.d(TAG, "Preparing sync requests");
        	new ZoteroAPITask(getBaseContext()).execute(APIRequest.update(this.item));
        	Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_started), 
    				Toast.LENGTH_SHORT).show();
        	return true;
        case R.id.do_new:
    		Bundle row = new Bundle();
    		row.putString("tag", "");
    		row.putString("itemKey", this.item.getKey());
    		row.putInt("type", 0);

			row.putInt("id",ZandyDialogFragment.DIALOG_TAG);
    		row.putInt("title",R.string.tag_edit);
			ZandyDialogFragment newFragment = ZandyDialogFragment.newInstance(TagActivity.this,row);
	        newFragment.show(getSupportFragmentManager(), "tag_edit");
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
		switch (bundle.getInt("id")) {
		case ZandyDialogFragment.DIALOG_TAG:
            Item.setTag(bundle.getString("itemKey"), bundle.getString("tag"), bundle.getString("fixed"), 0, db);
            Item item = Item.load(bundle.getString("itemKey"), db);
            Log.d(TAG, "Have JSON: "+item.getContent().toString());
            @SuppressWarnings("unchecked")
			ArrayAdapter<Bundle> la = (ArrayAdapter<Bundle>) listFragment.getListAdapter();
            la.clear();
            for (Bundle b : item.tagsToBundleArray()) {
            	la.add(b);
            }
            la.notifyDataSetChanged();
            break;
		case ZandyDialogFragment.DIALOG_CONFIRM_NAVIGATE:
			Intent i = new Intent(getBaseContext(), ItemActivity.class);
	    	i.putExtra("com.gimranov.zandy.app.tag", bundle.getString("tag"));
	    	startActivity(i);
	    	break;
		}
	}

	@Override
	public void doNegativeClick(Bundle savedInstanceState) {
		
	}

	@Override
	public void doNeutralClick(Bundle savedInstanceState) {
		
	}
}
