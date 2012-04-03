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


import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.gimranov.zandy.app.data.Database;

public class SettingsActivity extends PreferenceActivity implements OnClickListener, DialogClickMethods {
	
	FragmentManager fm;

     @Override
     public void onCreate(Bundle savedInstanceState) {
    	 //fm.();//maybe do something here??
         super.onCreate(savedInstanceState);
         addPreferencesFromResource(R.xml.settings);
         addPreferencesFromResource(R.xml.settings2);


         LinearLayout linLayout=new LinearLayout(this);
         Button requestButton = new Button(this);
 		 requestButton.setOnClickListener(this);
 		 requestButton.setId(R.id.requestQueue);
 		 requestButton.setText(R.string.sync_pending_requests);

         Button resetButton = new Button(this);
 		 resetButton.setOnClickListener(this);
 		 resetButton.setId(R.id.resetDatabase);
 		 resetButton.setText(R.string.settings_reset_database);
         linLayout.addView(requestButton);
         linLayout.addView(resetButton);
         this.addContentView(linLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
        		 ViewGroup.LayoutParams.WRAP_CONTENT));
     }
     
/*     @Override
     public void onBuildHeaders(List<Header> target) {
         loadHeadersFromResource(R.xml.preference_headers, target);
     }

     public static class Prefs1Fragment extends PreferenceFragment {
         @Override
         public void onCreate(Bundle savedInstanceState) {
             super.onCreate(savedInstanceState);

             // Load the preferences from an XML resource
             addPreferencesFromResource(R.xml.settings);
         }
     }
     
     public static class Prefs2Fragment extends PreferenceFragment {
         @Override
         public void onCreate(Bundle savedInstanceState) {
             super.onCreate(savedInstanceState);

             // Load the preferences from an XML resource
             addPreferencesFromResource(R.xml.settings2);
         }
     }*/
     
	public void onClick(View v) {
		if (v.getId() == R.id.requestQueue) {
			Intent i = new Intent(getApplicationContext(), RequestActivity.class);
			startActivity(i);
		} else if (v.getId() == R.id.resetDatabase) {
			final Bundle bundle=new Bundle();
			bundle.putInt("id",ZandyDialogFragment.DIALOG_CONFIRM_DELETE);
			bundle.putInt("title",R.string.settings_reset_database_warning);
	        DialogFragment newFragment = ZandyDialogFragment.newInstance(this,bundle);
	        newFragment.show(newFragment.getFragmentManager(), "settings_reset_database_warning");
		}
	}
	
	public void doPositiveClick(Bundle bundle) {
		Database db = new Database(getBaseContext());
		db.resetAllData();
		finish();
	}

	public void doNegativeClick(Bundle bundle) {
		// TODO Auto-generated method stub
		
	}

	public void doNeutralClick(Bundle bundle) {
		// TODO Auto-generated method stub
		
	}
}
