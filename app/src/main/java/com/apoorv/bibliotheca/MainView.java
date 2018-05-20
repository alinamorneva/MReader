package com.apoorv.bibliotheca;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MainView extends Activity {
	protected EpubNavigator navigator;
	protected int bookSelector;
	protected int panelCount;
	protected String[] settings;
	WebView view, webView;
	View decorView;
	Timer t;
	// Идентификатор уведомления
	private static final int NOTIFY_ID = 101;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		navigator = new EpubNavigator(2, this);

		panelCount = 0;
		settings = new String[8];

		// LOADSTATE
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		loadState(preferences);
		navigator.loadViews(preferences);

		if (panelCount == 0) {
			bookSelector = 0;
			Intent goToChooser = new Intent(this, FileChooser.class);
			startActivityForResult(goToChooser, 0);
		}
	}

	protected void onResume() {
		super.onResume();
		if (panelCount == 0) {
			SharedPreferences preferences = getPreferences(MODE_PRIVATE);
			navigator.loadViews(preferences);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		Editor editor = preferences.edit();
		saveState(editor);
		editor.apply();
	}

	// load the selected book
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (panelCount == 0) {
			SharedPreferences preferences = getPreferences(MODE_PRIVATE);
			navigator.loadViews(preferences);
		}

		if (resultCode == Activity.RESULT_OK) {
			String path = data.getStringExtra(getString(R.string.bpath));
			navigator.openBook(path, bookSelector);
		}
	}

	// ---- Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.Synchronize).setVisible(false);
		menu.findItem(R.id.Align).setVisible(false);
		menu.findItem(R.id.SyncScroll).setVisible(false);

		// if there is only one view, option "changeSizes" is not visualized
		if (panelCount == 1)
			menu.findItem(R.id.changeSize).setVisible(false);
		else
			menu.findItem(R.id.changeSize).setVisible(true);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.back_button:
				Intent intent = new Intent(this, FileChooser.class);
				startActivity(intent);
				return true;
			case R.id.Fullscreen:
				decorView = getWindow().getDecorView();
				decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_FULLSCREEN	| View.SYSTEM_UI_FLAG_IMMERSIVE);
				return true;
			case R.id.EnableAutoScroll:
				NotificationCompat.Builder mBuilder =
						new NotificationCompat.Builder(this)
								.setSmallIcon(R.drawable.ic_launcher4)
								.setContentTitle("BookReader")
								.setContentText("Внимание! Автоскрол включен.");
				Intent resultIntent = new Intent(this, MainView.class);

				TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
				stackBuilder.addParentStack(MainView.class);
				stackBuilder.addNextIntent(resultIntent);
				PendingIntent resultPendingIntent =
						stackBuilder.getPendingIntent(
								0,
								PendingIntent.FLAG_UPDATE_CURRENT
						);
				mBuilder.setContentIntent(resultPendingIntent);
				NotificationManager mNotificationManager =
						(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				mNotificationManager.notify(1, mBuilder.build());

				webView = (WebView) findViewById(R.id.Viewport);
				t = new Timer();
				t.scheduleAtFixedRate(new TimerTask()
				{
					@Override
					public void run()
					{
						webView.scrollBy(0,1);
					}
				},0,50);
				return true;
			case R.id.DisableAutoScroll:
				if(t != null) {
					t.cancel();
					t.purge();
				}
				return true;
			/*case R.id.Synchronize:
				boolean sync = navigator.flipSynchronizedReadingActive();
				return true;*/
			case R.id.Metadata: // информация о книге
				navigator.displayMetadata(0);
//                Intent intent1 = new Intent(this, Metadata.class);
//                startActivity(intent1);
				return true;
			case R.id.tableOfContents:
			    navigator.displayTOC(0);
				return true;
			case R.id.changeSize:
				try {
					DialogFragment newFragment = new SetPanelSize();
					newFragment.show(getFragmentManager(), "");
				} catch (Exception e) {
					errorMessage(getString(R.string.error_cannotChangeSizes));
				}
				return true;
			case R.id.Style: // work in progress...
				try {
					DialogFragment newFragment = new Settings();
					newFragment.show(getFragmentManager(), "");
					bookSelector = 0;
				} catch (Exception e) {
					errorMessage(getString(R.string.error_CannotChangeStyle));
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	// ---- Panels Manager
	public void addPanel(SplitPanel p) {
		FragmentTransaction fragmentTransaction = getFragmentManager()
				.beginTransaction();
		fragmentTransaction.add(R.id.MainLayout, p, p.getTag());
		fragmentTransaction.commit();

		panelCount++;
	}

	public void attachPanel(SplitPanel p) {
		FragmentTransaction fragmentTransaction = getFragmentManager()
				.beginTransaction();
		fragmentTransaction.attach(p);
		fragmentTransaction.commit();

		panelCount++;
	}

	public void detachPanel(SplitPanel p) {
		FragmentTransaction fragmentTransaction = getFragmentManager()
				.beginTransaction();
		fragmentTransaction.detach(p);
		fragmentTransaction.commit();

		panelCount--;
	}

	public void removePanelWithoutClosing(SplitPanel p) {
		FragmentTransaction fragmentTransaction = getFragmentManager()
				.beginTransaction();
		fragmentTransaction.remove(p);
		fragmentTransaction.commit();

		panelCount--;
	}

	public void removePanel(SplitPanel p) {
		FragmentTransaction fragmentTransaction = getFragmentManager()
				.beginTransaction();
		fragmentTransaction.remove(p);
		fragmentTransaction.commit();

		panelCount--;
		if (panelCount <= 0)
			finish();
	}

	// ---- Change Style
	public void setCSS() {
		navigator.changeCSS(bookSelector, settings);
	}

	public void setBackColor(String my_backColor) {
		settings[1] = my_backColor;
	}

	public void setColor(String my_color) {
		settings[0] = my_color;
	}

	public void setFontType(String my_fontFamily) {
		settings[2] = my_fontFamily;
	}

	public void setFontSize(String my_fontSize) {
		settings[3] = my_fontSize;
	}

	public void setLineHeight(String my_lineHeight) {
		if (my_lineHeight != null)
			settings[4] = my_lineHeight;
	}

	public void setAlign(String my_Align) {
		settings[5] = my_Align;
	}

	public void setMarginLeft(String mLeft) {
		settings[6] = mLeft;
	}

	public void setMarginRight(String mRight) {
		settings[7] = mRight;
	}

	// ----

	// change the views size, changing the weight
	protected void changeViewsSize(float weight) {
		navigator.changeViewsSize(weight);
	}

	public int getHeight() {
		LinearLayout main = (LinearLayout) findViewById(R.id.MainLayout);
		return main.getMeasuredHeight();
	}

	public int getWidth() {
		LinearLayout main = (LinearLayout) findViewById(R.id.MainLayout);
		return main.getWidth();
	}

	// Save/Load State
	protected void saveState(Editor editor) {
		navigator.saveState(editor);
	}

	protected void loadState(SharedPreferences preferences) {
		if (!navigator.loadState(preferences))
			errorMessage(getString(R.string.error_cannotLoadState));
	}

	public void errorMessage(String message) {
		Context context = getApplicationContext();
		Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
		toast.show();
	}
}