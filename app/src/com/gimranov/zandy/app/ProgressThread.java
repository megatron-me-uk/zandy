package com.gimranov.zandy.app;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.gimranov.zandy.app.data.Attachment;
import com.gimranov.zandy.app.data.Database;

public class ProgressThread extends Thread {
	
	private static final String TAG = "com.gimranov.zandy.app.ProgressThread";
	
	Handler mHandler;
	Bundle arguments;
	Activity parent;
	Database db;
	ArrayList<File> tmpFiles;
	final SharedPreferences settings;
	final static int STATE_DONE = 5;
	final static int STATE_FAILED = 3;
	final static int STATE_RUNNING = 1;
	final static int STATE_UNZIPPING = 6;
	
	ProgressThread(Handler h, Bundle b, SharedPreferences settingsRef,Database dbRef,ArrayList<File> tmpFilesRef) {
		mHandler = h;
		arguments = b;
		settings=settingsRef;
		db=dbRef;
		tmpFiles=tmpFilesRef;
	}
	
	@SuppressWarnings("unchecked")
	public void run() {
		// Setup
		final String attachmentKey = arguments.getString("attachmentKey");
		final String mode = arguments.getString("mode");
		URL url;
		File file;
		String urlstring;
		Attachment att = Attachment.load(attachmentKey, db);
		
		String sanitized = att.title.replace(' ', '_');
		
		// If no 1-6-character extension, try to add one using MIME type
		if (!sanitized.matches(".*\\.[a-zA-Z0-9]{1,6}$")) {
			String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(att.getType());
			if (extension != null) sanitized = sanitized + "." + extension;
		}
		sanitized = sanitized.replaceFirst("^(.*?)(\\.?[^.]*)$", "$1"+"_"+att.key+"$2");
		
		file = new File(ServerCredentials.sDocumentStorageDir,sanitized);
		if (!ServerCredentials.sBaseStorageDir.exists())
			ServerCredentials.sBaseStorageDir.mkdirs();
		if (!ServerCredentials.sDocumentStorageDir.exists())
			ServerCredentials.sDocumentStorageDir.mkdirs();
		
		if ("webdav".equals(mode)) {
			//urlstring = "https://dfs.humnet.ucla.edu/home/ajlyon/zotero/223RMC7C.zip";
			//urlstring = "http://www.gimranov.com/research/zotero/223RMC7C.zip";
			urlstring = settings.getString("webdav_path", "")+"/"+att.key+".zip";
			
			Authenticator.setDefault (new Authenticator() {
			    protected PasswordAuthentication getPasswordAuthentication() {
			        return new PasswordAuthentication (settings.getString("webdav_username", ""),
			        		settings.getString("webdav_password", "").toCharArray());
			    }
			});
		} else {
			urlstring = att.url+"?key="+settings.getString("user_key","");
		}
		
		try {
			try {
				url = new URL(urlstring);
			} catch (MalformedURLException e) {
				// Alert that we don't have a valid download URL and return
				Message msg = mHandler.obtainMessage();
	        	msg.arg2 = STATE_FAILED;
	        	msg.setData(arguments);
	        	mHandler.sendMessage(msg);
	        	
	        	Log.e(TAG, "Download URL not valid: "+urlstring, e);
	        	return;
			}
			//this is the downloader method
            long startTime = System.currentTimeMillis();
            Log.d(TAG, "download beginning");
            Log.d(TAG, "download url:" + url.toString());
            Log.d(TAG, "downloaded file name:" + file.getPath());
                            
            /* Open a connection to that URL. */
            URLConnection ucon = url.openConnection();
            ucon.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
            ucon.setRequestProperty("Accept","*/*");
            Message msg = mHandler.obtainMessage();
            msg.arg1 = ucon.getContentLength();
            msg.arg2 = STATE_RUNNING;
            mHandler.sendMessage(msg);

            /*
             * Define InputStreams to read from the URLConnection.
             */
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is, 16000);

            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            
            /*
             * Read bytes to the Buffer until there is nothing more to read(-1).
             * TODO read in chunks instead of byte by byte
             */
            while ((current = bis.read()) != -1) {
                    baf.append((byte) current);
                    
                if (baf.length() % 2048 == 0) {
                    msg = mHandler.obtainMessage();
                    msg.arg1 = baf.length();
                    mHandler.sendMessage(msg);
                }
            }

			/* Save to temporary directory for WebDAV */
			if ("webdav".equals(mode)) {
				if (!ServerCredentials.sCacheDir.exists())
					ServerCredentials.sCacheDir.mkdirs();
				File tmpFile = File.createTempFile("zandy", ".zip",ServerCredentials.sCacheDir);
				// Keep track of temp files that we've created.
				if (tmpFiles == null) tmpFiles = new ArrayList<File>();
				tmpFiles.add(tmpFile);
				FileOutputStream fos = new FileOutputStream(tmpFile);
                fos.write(baf.toByteArray());
                fos.close();
                ZipFile zf = new ZipFile(tmpFile);
                
            	Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zf.entries();
                do {
                	ZipEntry entry = entries.nextElement();
                    // Change the message to reflect that we're unzipping now
                    msg = mHandler.obtainMessage();
                    msg.arg1 = (int) entry.getSize();
                    msg.arg2 = STATE_UNZIPPING;
                    mHandler.sendMessage(msg);

                	String name64 = entry.getName();
                	byte[] byteName = Base64.decode(name64.getBytes(), 0, name64.length() - 5, Base64.DEFAULT);
                	String name = new String(byteName);
                	Log.d(TAG, "Found file "+name+" from encoded "+name64);
                	// If the linkMode is not an imported URL (snapshot) and the MIME type isn't text/html,
                	// then we unzip it and we're happy. If either of the preceding is true, we skip the file
                	// unless the filename includes .htm (covering .html automatically)
                	if ( (!att.getType().equals("text/html")) || name.contains(".htm")) {
                		FileOutputStream fos2 = new FileOutputStream(file);
                		InputStream entryStream = zf.getInputStream(entry);
                        ByteArrayBuffer baf2 = new ByteArrayBuffer(100);
                        while ((current = entryStream.read()) != -1) {
                        	baf2.append((byte) current);

			if (baf2.length() % 2048 == 0) {
				msg = mHandler.obtainMessage();
				msg.arg1 = baf2.length();
				mHandler.sendMessage(msg);
			}
                        }
                        fos2.write(baf2.toByteArray());
                        fos2.close();
                        Log.d(TAG, "Finished reading file");
                	} else {
                		Log.d(TAG, "Skipping file: "+name);
                	}
                } while (entries.hasMoreElements());
                zf.close();
	    // We remove the file from the ArrayList if deletion succeeded;
                // otherwise deletion is put off until the activity exits.
                if (tmpFile.delete()) {
                	tmpFiles.remove(tmpFile);
                }
			} else {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(baf.toByteArray());
                fos.close();
            }
			Log.d(TAG, "download ready in "
                    + ((System.currentTimeMillis() - startTime) / 1000)
                    + " sec");
        } catch (IOException e) {
                Log.e(TAG, "Error: ",e);
        }
		att.filename = file.getPath();
		File newFile = new File(att.filename);
    	Message msg = mHandler.obtainMessage();
		if (newFile.length() > 0) {
			att.status = Attachment.LOCAL;
			Log.d(TAG,"File downloaded: "+att.filename);
			msg.obj = att;
		} else {
			Log.d(TAG, "File not downloaded: "+att.filename);
			att.status = Attachment.AVAILABLE;
			msg.obj = null;
		}
		att.save(db);
    	msg.arg2 = STATE_DONE;
    	mHandler.sendMessage(msg);
	}
}
