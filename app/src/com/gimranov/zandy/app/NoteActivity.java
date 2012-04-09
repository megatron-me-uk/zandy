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

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gimranov.zandy.app.data.Attachment;
import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.task.APIRequest;

/**
 * This Activity handles displaying and editing of notes.
 * 
 * @author mlt
 *
 */
public class NoteActivity extends FragmentActivity implements DialogClickMethods{

	private static final String TAG = "com.gimranov.zandy.app.NoteActivity";
	
	static final int DIALOG_NOTE = 3;
	
	public Attachment att;
	private Database db;
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.note);

        db = new Database(this);
        
        /* Get the incoming data from the calling activity */
        final String attKey = getIntent().getStringExtra("com.gimranov.zandy.app.attKey");
        final Attachment att = Attachment.load(attKey, db);
        
        if (att == null) {
        	Log.e(TAG, "NoteActivity started without attKey; finishing.");
        	finish();
        	return;
        }
        
        Item item = Item.load(att.parentKey, db);
        this.att = att;
        
        setTitle(getResources().getString(R.string.note_for_item, item.getTitle()));

        TextView text = (TextView) findViewById(R.id.noteText);
        TextView title = (TextView) findViewById(R.id.noteTitle);
        title.setText(att.title);
        text.setText(Html.fromHtml(att.content.optString("note", "")));
        
        Button editButton = (Button) findViewById(R.id.editNote);
		editButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Bundle bundle =new Bundle();
				bundle.putInt("title",R.string.note);
				bundle.putString("content", att.content.optString("note", ""));
				bundle.putInt("id", ZandyDialogFragment.DIALOG_NOTE);
				bundle.putString("mode","new");
		        ZandyDialogFragment newFragment = ZandyDialogFragment.newInstance(NoteActivity.this,bundle);
		        newFragment.show(getSupportFragmentManager(), "note");
			}
		});
		
        /* Warn that this won't propagate for attachment notes */
        if (!"note".equals(att.getType())) {
			Toast.makeText(this, R.string.attachment_note_warning, Toast.LENGTH_LONG).show();
        }
    }

	@Override
	public void doPositiveClick(Bundle bundle) {
		switch (bundle.getInt("id")) {
		case ZandyDialogFragment.DIALOG_NOTE:
			att.setNoteText(bundle.getString("fixed"));
            att.dirty = APIRequest.API_DIRTY;
            att.save(db);
            
	        TextView text = (TextView) findViewById(R.id.noteText);
	        TextView title = (TextView) findViewById(R.id.noteTitle);
	        title.setText(att.title);
	        text.setText(Html.fromHtml(att.content.optString("note", "")));
			break;
		}
		
	}

	@Override
	public void doNegativeClick(Bundle savedInstanceState) {
		// Do nothing
		
	}

	@Override
	public void doNeutralClick(Bundle savedInstanceState) {
		// DO nothing
		
	}
}
