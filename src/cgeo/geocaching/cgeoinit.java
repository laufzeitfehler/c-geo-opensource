package cgeo.geocaching;

import gnu.android.app.appmanualclient.*;

import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.view.View;
import android.widget.EditText;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

import java.io.File;

import cgeo.geocaching.cgSettings.mapSourceEnum;

public class cgeoinit extends Activity {

	private final int SELECT_MAPFILE_REQUEST=1;

	private cgeoapplication app = null;
	private Resources res = null;
	private Activity activity = null;
	private cgSettings settings = null;
	private cgBase base = null;
	private cgWarning warning = null;
	private SharedPreferences prefs = null;
	private ProgressDialog loginDialog = null;
	private ProgressDialog webDialog = null;
	private Handler logInHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				if (loginDialog != null && loginDialog.isShowing() == true) {
					loginDialog.dismiss();
				}

				if (msg.what == 1) {
					warning.helpDialog(res.getString(R.string.init_login_popup), res.getString(R.string.init_login_popup_ok));
				} else {
					if (cgBase.errorRetrieve.containsKey(msg.what) == true) {
						warning.helpDialog(res.getString(R.string.init_login_popup),
								res.getString(R.string.init_login_popup_failed_reason) + " " + cgBase.errorRetrieve.get(msg.what) + ".");
					} else {
						warning.helpDialog(res.getString(R.string.init_login_popup), res.getString(R.string.init_login_popup_failed));
					}
				}
			} catch (Exception e) {
				warning.showToast(res.getString(R.string.err_login_failed));

				Log.e(cgSettings.tag, "cgeoinit.logInHandler: " + e.toString());
			}

			if (loginDialog != null && loginDialog.isShowing() == true) {
				loginDialog.dismiss();
			}

			init();
		}
	};
        
	private Handler webAuthHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				if (webDialog != null && webDialog.isShowing() == true) {
					webDialog.dismiss();
				}

				if (msg.what > 0) {
					warning.helpDialog(res.getString(R.string.init_sendToCgeo), res.getString(R.string.init_sendToCgeo_register_ok).replace("####", ""+msg.what));
				} else {
					warning.helpDialog(res.getString(R.string.init_sendToCgeo), res.getString(R.string.init_sendToCgeo_register_fail));
				}
			} catch (Exception e) {
				warning.showToast(res.getString(R.string.init_sendToCgeo_register_fail));

				Log.e(cgSettings.tag, "cgeoinit.webHandler: " + e.toString());
			}

			if (webDialog != null && webDialog.isShowing() == true) {
				webDialog.dismiss();
			}

			init();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init
		activity = this;
		res = this.getResources();
		app = (cgeoapplication) this.getApplication();
		prefs = getSharedPreferences(cgSettings.preferences, 0);
		settings = new cgSettings(this, prefs);
		base = new cgBase(app, settings, prefs);
		warning = new cgWarning(this);

		// set layout
		if (settings.skin == 1) {
			setTheme(R.style.light);
		} else {
			setTheme(R.style.dark);
		}
		setContentView(R.layout.init);
		base.setTitle(activity, res.getString(R.string.settings));

		// google analytics
		base.sendAnal(activity, "/init");

		init();
	}

	@Override
	public void onResume() {
		super.onResume();
		
		settings.load();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		init();
	}

	@Override
	public void onPause() {
		saveValues();
		super.onPause();
	}

	@Override
	public void onStop() {
		saveValues();
		super.onStop();
	}

	@Override
	public void onDestroy() {
		saveValues();

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, res.getString(R.string.init_clear)).setIcon(android.R.drawable.ic_menu_delete);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == 0) {
			boolean status = false;

			((EditText) findViewById(R.id.username)).setText("");
			((EditText) findViewById(R.id.password)).setText("");
			((EditText) findViewById(R.id.passvote)).setText("");

			status = saveValues();
			if (status == true) {
				warning.showToast(res.getString(R.string.init_cleared));
			} else {
				warning.showToast(res.getString(R.string.err_init_cleared));
			}

			finish();
		}

		return false;
	}

	public void init() {
		
		// geocaching.com settings
		String usernameNow = prefs.getString("username", null);
		if (usernameNow != null) {
			((EditText) findViewById(R.id.username)).setText(usernameNow);
		}
		String passwordNow = prefs.getString("password", null);
		if (usernameNow != null) {
			((EditText) findViewById(R.id.password)).setText(passwordNow);
		}
		
		Button logMeIn = (Button) findViewById(R.id.log_me_in);
		logMeIn.setOnClickListener(new logIn());

		TextView legalNote = (TextView) findViewById(R.id.legal_note);
		legalNote.setClickable(true);
		legalNote.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/about/termsofuse.aspx")));
			}
		});

		// gcvote settings
		String passvoteNow = prefs.getString("pass-vote", null);
		if (passvoteNow != null) {
			((EditText) findViewById(R.id.passvote)).setText(passvoteNow);
		}

		// go4cache settings
		TextView go4cache = (TextView) findViewById(R.id.about_go4cache);
		go4cache.setClickable(true);
		go4cache.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://go4cache.com/")));
			}
		});

		CheckBox publicButton = (CheckBox) findViewById(R.id.publicloc);
		if (prefs.getInt("publicloc", 0) == 0) {
			publicButton.setChecked(false);
		} else {
			publicButton.setChecked(true);
		}
		publicButton.setOnClickListener(new cgeoChangePublic());

		// Twitter settings
		Button authorizeTwitter = (Button) findViewById(R.id.authorize_twitter);
		authorizeTwitter.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				Intent authIntent = new Intent(activity, cgeoauth.class);
				activity.startActivity(authIntent);
			}
		});

		CheckBox twitterButton = (CheckBox) findViewById(R.id.twitter_option);
		if (prefs.getInt("twitter", 0) == 0 || settings.tokenPublic == null || settings.tokenPublic.length() == 0 || settings.tokenSecret == null || settings.tokenSecret.length() == 0) {
			twitterButton.setChecked(false);
		} else {
			twitterButton.setChecked(true);
		}
		twitterButton.setOnClickListener(new cgeoChangeTwitter());

		// Signature settings
		EditText sigEdit = (EditText) findViewById(R.id.signature);
		if (sigEdit.getText().length() == 0) {
			sigEdit.setText(settings.getSignature());
		}
		Button sigBtn = (Button) findViewById(R.id.signature_help);
		sigBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				warning.helpDialog(res.getString(R.string.init_signature_help_title), res.getString(R.string.init_signature_help_text));
			}
		});

		// Other settings
		CheckBox skinButton = (CheckBox) findViewById(R.id.skin);
		if (prefs.getInt("skin", 0) == 0) {
			skinButton.setChecked(false);
		} else {
			skinButton.setChecked(true);
		}
		skinButton.setOnClickListener(new cgeoChangeSkin());

		CheckBox addressButton = (CheckBox) findViewById(R.id.address);
		if (prefs.getInt("showaddress", 1) == 0) {
			addressButton.setChecked(false);
		} else {
			addressButton.setChecked(true);
		}
		addressButton.setOnClickListener(new cgeoChangeAddress());

		CheckBox captchaButton = (CheckBox) findViewById(R.id.captcha);
		if (prefs.getBoolean("showcaptcha", false) == false) {
			captchaButton.setChecked(false);
		} else {
			captchaButton.setChecked(true);
		}
		captchaButton.setOnClickListener(new cgeoChangeCaptcha());

		CheckBox useEnglishButton = (CheckBox) findViewById(R.id.useenglish);
		if (prefs.getBoolean("useenglish", false) == false) {
			useEnglishButton.setChecked(false);
		} else {
			useEnglishButton.setChecked(true);
		}
		useEnglishButton.setOnClickListener(new cgeoChangeUseEnglish());

		CheckBox excludeButton = (CheckBox) findViewById(R.id.exclude);
		if (prefs.getInt("excludemine", 0) == 0) {
			excludeButton.setChecked(false);
		} else {
			excludeButton.setChecked(true);
		}
		excludeButton.setOnClickListener(new cgeoChangeExclude());

		CheckBox disabledButton = (CheckBox) findViewById(R.id.disabled);
		if (prefs.getInt("excludedisabled", 0) == 0) {
			disabledButton.setChecked(false);
		} else {
			disabledButton.setChecked(true);
		}
		disabledButton.setOnClickListener(new cgeoChangeDisabled());

		CheckBox offlineButton = (CheckBox) findViewById(R.id.offline);
		if (prefs.getInt("offlinemaps", 1) == 0) {
			offlineButton.setChecked(false);
		} else {
			offlineButton.setChecked(true);
		}
		offlineButton.setOnClickListener(new cgeoChangeOffline());

		CheckBox autoloadButton = (CheckBox) findViewById(R.id.autoload);
		if (prefs.getInt("autoloaddesc", 0) == 0) {
			autoloadButton.setChecked(false);
		} else {
			autoloadButton.setChecked(true);
		}
		autoloadButton.setOnClickListener(new cgeoChangeAutoload());

		CheckBox livelistButton = (CheckBox) findViewById(R.id.livelist);
		if (prefs.getInt("livelist", 1) == 0) {
			livelistButton.setChecked(false);
		} else {
			livelistButton.setChecked(true);
		}
		livelistButton.setOnClickListener(new cgeoChangeLivelist());

		CheckBox unitsButton = (CheckBox) findViewById(R.id.units);
		if (prefs.getInt("units", cgSettings.unitsMetric) == cgSettings.unitsMetric) {
			unitsButton.setChecked(false);
		} else {
			unitsButton.setChecked(true);
		}
		unitsButton.setOnClickListener(new cgeoChangeUnits());

		CheckBox gnavButton = (CheckBox) findViewById(R.id.gnav);
		if (prefs.getInt("usegnav", 1) == 1) {
			gnavButton.setChecked(true);
		} else {
			gnavButton.setChecked(false);
		}
		gnavButton.setOnClickListener(new cgeoChangeGNav());

		CheckBox browserButton = (CheckBox) findViewById(R.id.browser);
		if (prefs.getInt("asbrowser", 1) == 0) {
			browserButton.setChecked(false);
		} else {
			browserButton.setChecked(true);
		}
		browserButton.setOnClickListener(new cgeoChangeBrowser());
		
		// Altitude settings
		EditText altitudeEdit = (EditText) findViewById(R.id.altitude);
		altitudeEdit.setText("" + prefs.getInt("altcorrection", 0));

		//Send2cgeo settings
                String webDeviceName = prefs.getString("webDeviceName", null);
                
		if ((webDeviceName != null) &&(webDeviceName.length() > 0)) {
			((EditText) findViewById(R.id.webDeviceName)).setText(webDeviceName);
		} else {
                    String s = android.os.Build.MODEL;
                    ((EditText) findViewById(R.id.webDeviceName)).setText(s);
                }

		Button webAuth = (Button) findViewById(R.id.sendToCgeo_register);
		webAuth.setOnClickListener(new webAuth());
                
                /*TextView webText = (TextView) findViewById(R.id.sendToCgeo);
		webText.setClickable(true);
		webText.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://send2cgeo.carnero.cc/")));
			}
		});*/
		
		// Map source settings
		Spinner mapSourceSelector = (Spinner) findViewById(R.id.mapsource);
	    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
	            this, R.array.map_sources, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    mapSourceSelector.setAdapter(adapter);		
		int mapsource = prefs.getInt("mapsource", 0);
		mapSourceSelector.setSelection(mapsource);
		mapSourceSelector.setOnItemSelectedListener(new cgeoChangeMapSource());
		
		initMapfileEdittext(false);
				
		Button selectMapfile = (Button) findViewById(R.id.select_mapfile);
		selectMapfile.setOnClickListener(new View.OnClickListener() {
					
			@Override
			public void onClick(View v) {
				Intent selectIntent = new Intent(activity, cgSelectMapfile.class);
				activity.startActivityForResult(selectIntent, SELECT_MAPFILE_REQUEST);
			}
		});
		
		setMapFileEditState();
		
		// Cache db backup
		TextView lastBackup = (TextView) findViewById(R.id.backup_last);
		File lastBackupFile = app.isRestoreFile();
		if (lastBackupFile != null) {
			lastBackup.setText(res.getString(R.string.init_backup_last) + " " + cgBase.timeOut.format(lastBackupFile.lastModified()) + ", " + cgBase.dateOut.format(lastBackupFile.lastModified()));
		} else {
			lastBackup.setText(res.getString(R.string.init_backup_last_no));
		}
		
	}
	
	private void initMapfileEdittext(boolean setFocus) {
		EditText mfmapFileEdit = (EditText) findViewById(R.id.mapfile);
		mfmapFileEdit.setText(prefs.getString("mfmapfile", ""));
		if (setFocus) {
			mfmapFileEdit.requestFocus();
		}
	}

	public void backup(View view) {
		final String file = app.backupDatabase();
		
		if (file != null) {
			warning.helpDialog(res.getString(R.string.init_backup_backup), res.getString(R.string.init_backup_success) + "\n" + file);
		} else {
			warning.helpDialog(res.getString(R.string.init_backup_backup), res.getString(R.string.init_backup_failed));
		}
		
		TextView lastBackup = (TextView) findViewById(R.id.backup_last);
		File lastBackupFile = app.isRestoreFile();
		if (lastBackupFile != null) {
			lastBackup.setText(res.getString(R.string.init_backup_last) + " " + cgBase.timeOut.format(lastBackupFile.lastModified()) + ", " + cgBase.dateOut.format(lastBackupFile.lastModified()));
		} else {
			lastBackup.setText(res.getString(R.string.init_backup_last_no));
		}
	}
	
	public void restore(View view) {
		final boolean status = app.restoreDatabase();
		
		if (status) {
			warning.helpDialog(res.getString(R.string.init_backup_restore), res.getString(R.string.init_restore_success));
		} else {
			warning.helpDialog(res.getString(R.string.init_backup_restore), res.getString(R.string.init_restore_failed));
		}
	}
	
	private void setMapFileEditState() {
		LinearLayout mapFileEdit = (LinearLayout) findViewById(R.id.init_mapfilegroup);
		if (settings.mapProvider == mapSourceEnum.mapsforgeOffline) {
			mapFileEdit.setVisibility(View.VISIBLE);
		} else {
			mapFileEdit.setVisibility(View.INVISIBLE);
		}
	}

	public boolean saveValues() {
		String usernameNew = ((EditText) findViewById(R.id.username)).getText().toString();
		String passwordNew = ((EditText) findViewById(R.id.password)).getText().toString();
		String passvoteNew = ((EditText) findViewById(R.id.passvote)).getText().toString();
		String signatureNew = ((EditText) findViewById(R.id.signature)).getText().toString();
		String altitudeNew = ((EditText) findViewById(R.id.altitude)).getText().toString();
		String mfmapFileNew = ((EditText) findViewById(R.id.mapfile)).getText().toString();

		if (usernameNew == null) {
			usernameNew = "";
		}
		if (passwordNew == null) {
			passwordNew = "";
		}
		if (passvoteNew == null) {
			passvoteNew = "";
		}
		if (signatureNew == null) {
			signatureNew = "";
		}
		
		int altitudeNewInt = 0;
		if (altitudeNew == null) {
			altitudeNewInt = 0;
		} else {
			altitudeNewInt = new Integer(altitudeNew);
		}
		
		if (mfmapFileNew == null) {
			mfmapFileNew = "";
		}

		final boolean status1 = settings.setLogin(usernameNew, passwordNew);
		final boolean status2 = settings.setGCvoteLogin(passvoteNew);
		final boolean status3 = settings.setSignature(signatureNew);
		final boolean status4 = settings.setAltCorrection(altitudeNewInt);
		final boolean status5 = settings.setMapFile(mfmapFileNew);

		if (status1 && status2 && status3 && status4 && status5) {
			return true;
		}

		return false;
	}

	private class cgeoChangeTwitter implements View.OnClickListener {

		public void onClick(View arg0) {
			CheckBox twitterButton = (CheckBox) findViewById(R.id.twitter_option);

			if (twitterButton.isChecked() == true) {
				settings.reloadTwitterTokens();

				SharedPreferences.Editor edit = prefs.edit();
				if (prefs.getInt("twitter", 0) == 0) {
					edit.putInt("twitter", 1);
					settings.twitter = 1;
				} else {
					edit.putInt("twitter", 0);
					settings.twitter = 0;
				}
				edit.commit();

				if (settings.twitter == 1 && (settings.tokenPublic == null || settings.tokenPublic.length() == 0 || settings.tokenSecret == null || settings.tokenSecret.length() == 0)) {
					Intent authIntent = new Intent(activity, cgeoauth.class);
					activity.startActivity(authIntent);
				}

				if (prefs.getInt("twitter", 0) == 0) {
					twitterButton.setChecked(false);
				} else {
					twitterButton.setChecked(true);
				}
			} else {
				SharedPreferences.Editor edit = prefs.edit();
				edit.putInt("twitter", 0);
				settings.twitter = 0;
				edit.commit();

				twitterButton.setChecked(false);
			}

			return;
		}
	}

	private class cgeoChangeSkin implements View.OnClickListener {

		public void onClick(View arg0) {
			SharedPreferences.Editor edit = prefs.edit();
			if (prefs.getInt("skin", 0) == 0) {
				edit.putInt("skin", 1);
				settings.setSkin(1);
			} else {
				edit.putInt("skin", 0);
				settings.setSkin(0);
			}
			edit.commit();

			CheckBox skinButton = (CheckBox) findViewById(R.id.skin);
			if (prefs.getInt("skin", 0) == 0) {
				skinButton.setChecked(false);
			} else {
				skinButton.setChecked(true);
			}

			return;
		}
	}

	private class cgeoChangeAddress implements View.OnClickListener {

		public void onClick(View arg0) {
			SharedPreferences.Editor edit = prefs.edit();
			if (prefs.getInt("showaddress", 1) == 0) {
				edit.putInt("showaddress", 1);
			} else {
				edit.putInt("showaddress", 0);
			}
			edit.commit();

			CheckBox transparentButton = (CheckBox) findViewById(R.id.address);
			if (prefs.getInt("showaddress", 1) == 0) {
				transparentButton.setChecked(false);
			} else {
				transparentButton.setChecked(true);
			}

			return;
		}
	}

	private class cgeoChangePublic implements View.OnClickListener {

		public void onClick(View arg0) {
			SharedPreferences.Editor edit = prefs.edit();
			if (prefs.getInt("publicloc", 0) == 0) {
				edit.putInt("publicloc", 1);
				settings.publicLoc = 1;
			} else {
				edit.putInt("publicloc", 0);
				settings.publicLoc = 0;
			}
			edit.commit();

			CheckBox publicloc = (CheckBox) findViewById(R.id.publicloc);
			if (prefs.getInt("publicloc", 0) == 0) {
				publicloc.setChecked(false);
			} else {
				publicloc.setChecked(true);
			}

			return;
		}
	}

	private class cgeoChangeCaptcha implements View.OnClickListener {

		public void onClick(View arg0) {
			SharedPreferences.Editor edit = prefs.edit();
			if (prefs.getBoolean("showcaptcha", false) == false) {
				edit.putBoolean("showcaptcha", true);
				settings.showCaptcha = true;
			} else {
				edit.putBoolean("showcaptcha", false);
				settings.showCaptcha = false;
			}
			edit.commit();

			CheckBox captchaButton = (CheckBox) findViewById(R.id.captcha);
			if (prefs.getBoolean("showcaptcha", false) == false) {
				captchaButton.setChecked(false);
			} else {
				captchaButton.setChecked(true);
			}

			return;
		}
	}

	private class cgeoChangeUseEnglish implements View.OnClickListener {

		public void onClick(View arg0) {
			SharedPreferences.Editor edit = prefs.edit();
			if (prefs.getBoolean("useenglish", false) == false) {
				edit.putBoolean("useenglish", true);
				settings.useEnglish = true;
				settings.setLanguage(true);
			} else {
				edit.putBoolean("useenglish", false);
				settings.useEnglish = false;
				settings.setLanguage(false);
			}
			edit.commit();

			CheckBox useEnglishButton = (CheckBox) findViewById(R.id.useenglish);
			if (prefs.getBoolean("useenglish", false) == false) {
				useEnglishButton.setChecked(false);
			} else {
				useEnglishButton.setChecked(true);
			}

			return;
		}
	}

	private class cgeoChangeExclude implements View.OnClickListener {

		public void onClick(View arg0) {
			SharedPreferences.Editor edit = prefs.edit();
			if (prefs.getInt("excludemine", 0) == 0) {
				edit.putInt("excludemine", 1);
				settings.excludeMine = 1;
			} else {
				edit.putInt("excludemine", 0);
				settings.excludeMine = 0;
			}
			edit.commit();

			CheckBox excludeButton = (CheckBox) findViewById(R.id.exclude);
			if (prefs.getInt("excludemine", 0) == 0) {
				excludeButton.setChecked(false);
			} else {
				excludeButton.setChecked(true);
			}

			return;
		}
	}

	private class cgeoChangeDisabled implements View.OnClickListener {

		public void onClick(View arg0) {
			SharedPreferences.Editor edit = prefs.edit();
			if (prefs.getInt("excludedisabled", 0) == 0) {
				edit.putInt("excludedisabled", 1);
				settings.excludeDisabled = 1;
			} else {
				edit.putInt("excludedisabled", 0);
				settings.excludeDisabled = 0;
			}
			edit.commit();

			CheckBox disabledButton = (CheckBox) findViewById(R.id.disabled);
			if (prefs.getInt("excludedisabled", 0) == 0) {
				disabledButton.setChecked(false);
			} else {
				disabledButton.setChecked(true);
			}

			return;
		}
	}

	private class cgeoChangeOffline implements View.OnClickListener {

		public void onClick(View arg0) {
			SharedPreferences.Editor edit = prefs.edit();
			if (prefs.getInt("offlinemaps", 1) == 0) {
				edit.putInt("offlinemaps", 1);
				settings.excludeDisabled = 1;
			} else {
				edit.putInt("offlinemaps", 0);
				settings.excludeDisabled = 0;
			}
			edit.commit();

			CheckBox offlineButton = (CheckBox) findViewById(R.id.offline);
			if (prefs.getInt("offlinemaps", 0) == 0) {
				offlineButton.setChecked(false);
			} else {
				offlineButton.setChecked(true);
			}

			return;
		}
	}

	private class cgeoChangeLivelist implements View.OnClickListener {

		public void onClick(View arg0) {
			SharedPreferences.Editor edit = prefs.edit();
			if (prefs.getInt("livelist", 1) == 0) {
				edit.putInt("livelist", 1);
				settings.livelist = 1;
			} else {
				edit.putInt("livelist", 0);
				settings.livelist = 0;
			}
			edit.commit();

			CheckBox livelistButton = (CheckBox) findViewById(R.id.livelist);
			if (prefs.getInt("livelist", 1) == 0) {
				livelistButton.setChecked(false);
			} else {
				livelistButton.setChecked(true);
			}

			return;
		}
	}

	private class cgeoChangeAutoload implements View.OnClickListener {

		public void onClick(View arg0) {
			SharedPreferences.Editor edit = prefs.edit();
			if (prefs.getInt("autoloaddesc", 0) == 0) {
				edit.putInt("autoloaddesc", 1);
				settings.autoLoadDesc = 1;
			} else {
				edit.putInt("autoloaddesc", 0);
				settings.autoLoadDesc = 0;
			}
			edit.commit();

			CheckBox autoloadButton = (CheckBox) findViewById(R.id.autoload);
			if (prefs.getInt("autoloaddesc", 0) == 0) {
				autoloadButton.setChecked(false);
			} else {
				autoloadButton.setChecked(true);
			}

			return;
		}
	}

	private class cgeoChangeUnits implements View.OnClickListener {

		public void onClick(View arg0) {
			SharedPreferences.Editor edit = prefs.edit();
			if (prefs.getInt("units", cgSettings.unitsMetric) == cgSettings.unitsMetric) {
				edit.putInt("units", cgSettings.unitsImperial);
				settings.units = cgSettings.unitsImperial;
			} else {
				edit.putInt("units", cgSettings.unitsMetric);
				settings.units = cgSettings.unitsMetric;
			}
			edit.commit();

			CheckBox unitsButton = (CheckBox) findViewById(R.id.units);
			if (prefs.getInt("units", cgSettings.unitsMetric) == cgSettings.unitsMetric) {
				unitsButton.setChecked(false);
			} else {
				unitsButton.setChecked(true);
			}

			return;
		}
	}

	private class cgeoChangeGNav implements View.OnClickListener {

		public void onClick(View arg0) {
			SharedPreferences.Editor edit = prefs.edit();
			if (prefs.getInt("usegnav", 1) == 1) {
				edit.putInt("usegnav", 0);
				settings.useGNavigation = 0;
			} else {
				edit.putInt("usegnav", 1);
				settings.useGNavigation = 1;
			}
			edit.commit();

			CheckBox gnavButton = (CheckBox) findViewById(R.id.gnav);
			if (prefs.getInt("usegnav", 1) == 1) {
				gnavButton.setChecked(true);
			} else {
				gnavButton.setChecked(false);
			}

			return;
		}
	}

	private class cgeoChangeBrowser implements View.OnClickListener {

		public void onClick(View arg0) {
			SharedPreferences.Editor edit = prefs.edit();
			if (prefs.getInt("asbrowser", 1) == 0) {
				edit.putInt("asbrowser", 1);
				settings.asBrowser = 1;
			} else {
				edit.putInt("asbrowser", 0);
				settings.asBrowser = 0;
			}
			edit.commit();

			CheckBox browserButton = (CheckBox) findViewById(R.id.browser);
			if (prefs.getInt("asbrowser", 1) == 0) {
				browserButton.setChecked(false);
			} else {
				browserButton.setChecked(true);
			}

			return;
		}
	}

	private class cgeoChangeMapSource implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			settings.mapProvider = mapSourceEnum.fromInt(arg2);
			SharedPreferences.Editor edit = prefs.edit();
			edit.putInt("mapsource", arg2);
			edit.commit();
			setMapFileEditState();
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			arg0.setSelection(settings.mapProvider.ordinal());
			setMapFileEditState();
		}
	}

	private class logIn implements View.OnClickListener {

		public void onClick(View arg0) {
			final String username = ((EditText) findViewById(R.id.username)).getText().toString();
			final String password = ((EditText) findViewById(R.id.password)).getText().toString();

			if (username == null || username.length() == 0 || password == null || password.length() == 0) {
				warning.showToast(res.getString(R.string.err_missing_auth));
				return;
			}

			loginDialog = ProgressDialog.show(activity, res.getString(R.string.init_login_popup), res.getString(R.string.init_login_popup_working), true);
			loginDialog.setCancelable(false);

			settings.setLogin(username, password);
			settings.deleteCookies();

			(new Thread() {

				@Override
				public void run() {
					logInHandler.sendEmptyMessage(base.login());
				}
			}).start();
		}
	}

	private class webAuth implements View.OnClickListener {

		public void onClick(View arg0) {
			final String deviceName = ((EditText) findViewById(R.id.webDeviceName)).getText().toString();
			final String deviceCode = prefs.getString("webDeviceCode", null);


			if (deviceName == null || deviceName.length() == 0) {
				warning.showToast(res.getString(R.string.err_missing_device_name));
				return;
			}

			webDialog = ProgressDialog.show(activity, res.getString(R.string.init_sendToCgeo), res.getString(R.string.init_sendToCgeo_registering), true);
			webDialog.setCancelable(false);

			(new Thread() {

				@Override
				public void run() {
                                        int pin = 0;
                                        
                                        String nam = deviceName==null?"":deviceName;
                                        String cod = deviceCode==null?"":deviceCode;
                                        
                                        String params = "name="+cgBase.urlencode_rfc3986(nam)+"&code="+cgBase.urlencode_rfc3986(cod);
                                        
                                        cgResponse response = base.request(false, "send2cgeo.carnero.cc", "/authDev.php", "GET", params, 0, true);
                                        
                                        if (response.getStatusCode() == 200)
                                        {
                                            //response was OK
                                            String[] strings = response.getData().split(",");
                                            try {
                                                pin=Integer.parseInt(strings[1].trim());
                                            } catch (Exception e) {
                                                Log.e(cgSettings.tag, "webDialog: " + e.toString());
                                            }
                                            String code = strings[0];
                                            settings.setWebNameCode(nam, code);
                                        }
                                        
					webAuthHandler.sendEmptyMessage(pin);
				}
			}).start();
		}
	}
        
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == SELECT_MAPFILE_REQUEST) {
			if (resultCode == RESULT_OK) {
				if (data.hasExtra("mapfile")) {
					settings.setMapFile(data.getStringExtra("mapfile"));
				}
			}
			initMapfileEdittext(true);
		}
	}

	public void goHome(View view) {
		base.goHome(activity);
	}

	public void goManual(View view) {
		try {
			AppManualReaderClient.openManual(
				"c-geo",
				"c:geo-configuration",
				activity,
				"http://cgeo.carnero.cc/manual/"
			);
		} catch (Exception e) {
			// nothing
		}
	}
}
