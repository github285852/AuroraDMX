package com.AuroraByteSoftware.AuroraDMX;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.Toast;

import com.AuroraByteSoftware.AuroraDMX.billing.Billing;
import com.AuroraByteSoftware.AuroraDMX.chase.ChaseObj;
import com.AuroraByteSoftware.AuroraDMX.fixture.Fixture;
import com.AuroraByteSoftware.AuroraDMX.fixture.RGBFixture;
import com.AuroraByteSoftware.AuroraDMX.fixture.StandardFixture;
import com.AuroraByteSoftware.AuroraDMX.ui.CueActivity;
import com.AuroraByteSoftware.AuroraDMX.ui.ImportFile;
import com.AuroraByteSoftware.AuroraDMX.ui.PatchActivity;
import com.AuroraByteSoftware.AuroraDMX.ui.chase.ChaseActivity;
import com.AuroraByteSoftware.AuroraDMX.ui.fontawesome.FontAwesomeIcons;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.azelart.artnetstack.domain.artpollreply.ArtPollReply;

import static com.AuroraByteSoftware.AuroraDMX.ui.fontawesome.FontAwesomeManager.addFAIcon;

public class MainActivity extends Activity implements OnSharedPreferenceChangeListener {
    private TabHost tab;

    private static SharedPreferences sharedPref;
    static Double cueCount = 1.0;// cueCount++ = new cue num
    private static boolean updatingFixtures = false;
    private int orgColor = 0;
    public Billing billing = new Billing();

    public static final ArrayList<ArtPollReply> foundServers = new ArrayList<>();
    public static ProgressDialog progressDialog = null;

    //Static Variables
    static final int ALLOWED_PATCHED_DIMMERS = 50;
    public static final int MAX_DIMMERS = 512;
    private static final int MAX_CHANNEL = 512;

    /**
     * patchList[0] is ignored. All numbers are in human format (start at 1) not computer.
     */
    public static List<ChPatch> patchList = new ArrayList<>();
    public static List<Fixture> alColumns = null;
    private static ArrayList<CueObj> alCues = null;
    private static ArrayList<ChaseObj> alChases = null;
    public static CueFade cueFade = null;

    public static ProjectManagement pm = null;

    public static CueFade getCueFade() {
        if (cueFade == null) {
            cueFade = new CueFade();
        }
        return cueFade;
    }


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tab = (TabHost)findViewById(R.id.tabhost);
        tab.setup();
        addTabSpec();

        pm = new ProjectManagement(this);
        Log.v(getClass().getSimpleName(), "onCreate");
        billing.setup(this);
        Iconify.with(new FontAwesomeModule());
        startup();
        Intent intent = getIntent();

        //Open the file if the user clicked on a file in the file system
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            try {
                pm.openFile(intent.getData(), this);
            } catch (IOException | SecurityException e) {
                Toast.makeText(MainActivity.this, R.string.cannotOpen, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            pm.open(null);
        }

        AuroraNetwork.setUpNetwork(this);
    }

    private void addTabSpec(){
//        动态载入xml
        LayoutInflater.from(this).inflate(R.layout.hsi,tab.getTabContentView());
        LayoutInflater.from(this).inflate(R.layout.scene,tab.getTabContentView());

        tab.addTab(tab.newTabSpec("tab1").setIndicator("Scene").setContent(R.id.hsi));
        tab.addTab(tab.newTabSpec("tab2").setIndicator("hsi").setContent(R.id.scene));
//     标签切换事件处理
        tab.setOnTabChangedListener(new OnTabChangeListener(){
        @Override
        public void onTabChanged(String tabId){
            if(tabId.equals("tab1"))
            {
                Log.v("TAB","标签一");
            }
            if(tabId.equals("tab2"))
            {
                Log.v("TAB","标签二");
            }
        }
        });
    }

    private void startup() {
        updatingFixtures = true;
        // load the layout
        setContentView(R.layout.activity_main);
        setupButtons();


        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        setAlCues(new ArrayList<CueObj>());
        alColumns = new ArrayList<>();
        alChases = new ArrayList<>();
        int number_channels = Integer.parseInt(sharedPref.getString(SettingsActivity.channels, "5"));
        setNumberOfFixtures(number_channels, null, null, null, null);
        updatingFixtures = false;
    }

    private void setupButtons() {
        // Add Cue
    }

    public static SharedPreferences getSharedPref() {
        return sharedPref;
    }


    void setNumberOfFixtures(int numberFixtures, String[] channelNames, boolean[] isRGB,
                             String[] valuePresets, Boolean[] isParked) {
        updatingFixtures = true;

        updatingFixtures = false;
    }

    private void oneToOnePatch() {
        int orgSize = patchList.size();
        int endSize = calculateChannelCount();
        for (int i = orgSize; i < endSize; i++) {
            patchList.add(new ChPatch(i));
        }
    }

    // Initiating Menu XML file (menu.xml)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);
        addFAIcon(menu, R.id.menu_patch, FontAwesomeIcons.fa_th, this);
        addFAIcon(menu, R.id.menu_cues, FontAwesomeIcons.fa_caret_square_o_right, this);
        addFAIcon(menu, R.id.menu_chase, FontAwesomeIcons.fa_fast_forward, this);
        addFAIcon(menu, R.id.menu_settings, FontAwesomeIcons.fa_cog, this);
        addFAIcon(menu, R.id.menu_share, FontAwesomeIcons.fa_share_alt, this);
        addFAIcon(menu, R.id.menu_import, FontAwesomeIcons.fa_file_import, this);
        addFAIcon(menu, R.id.menu_save, FontAwesomeIcons.fa_save, this);
        addFAIcon(menu, R.id.menu_load, FontAwesomeIcons.fa_folder_open, this);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Event Handling for Individual menu item selected Identify single menu
     * item by it's id
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean fadingInProgress = false;
        for (CueObj cue : getAlCues()) {
            if (cue.isFadeInProgress()) {
                fadingInProgress = true;
            }
        }
        if (!fadingInProgress || item.getItemId() == R.id.menu_cues) {
            Intent intent;
            switch (item.getItemId()) {
                case R.id.menu_patch:
                    intent = new Intent(this, PatchActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.menu_cues:
                    intent = new Intent(this, CueActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.menu_chase:
                    intent = new Intent(this, ChaseActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.menu_settings:
                    intent = new Intent(this, SettingsActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.menu_save:
                    pm.onSaveClick();
                    return true;
                case R.id.menu_load:
                    pm.onLoadClick();
                    return true;
                case R.id.menu_share:
                    pm.onShare();
                    return true;
                case R.id.menu_import:
                    ImportFile importFile = new ImportFile();
                    importFile.onImport(this);
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        } else {
            Toast.makeText(MainActivity.this, R.string.waitingOnFade, Toast.LENGTH_SHORT).show();
            return super.onOptionsItemSelected(item);
        }

    }

    protected static List<Integer> getCurrentChannelArray() {
        List<Integer> out = new ArrayList<>();
        for (Fixture fixture : alColumns) {
            out.addAll(fixture.getChLevels());
        }
        return out;
    }

    public static int[] getCurrentDimmerLevels() {
        if (updatingFixtures) {
            return null;
        }
        int out[] = new int[MAX_DIMMERS];

        //flatten the channel values from the UI
        List<Integer> chValues = new ArrayList<>();
        for (Fixture fixture : alColumns) {
            chValues.addAll(fixture.getChLevels());
        }

        for (int ch = 1; ch <= chValues.size() && patchList.size() > ch; ch++) {
            for (Integer dim : patchList.get(ch).getDimmers()) {
                if (dim <= 0 || dim > MAX_DIMMERS) {
                    continue;
                }
                int oldLvl = out[dim - 1];
                int newVal = chValues.get(ch - 1);
                if (oldLvl < newVal) {
                    out[dim - 1] = newVal;
                }
            }
        }
        return out;
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.v(getClass().getSimpleName(), "Pref Change");
        if (key != null && key.equals(SettingsActivity.channels)) {
            setNumberOfFixtures(Integer.parseInt(sharedPreferences.getString(SettingsActivity.channels, "5")), null, null, null, null);
        }
    }

    protected void onResume() {
        super.onResume();
        Log.v(getClass().getSimpleName(), "onResume");
        getSharedPref().registerOnSharedPreferenceChangeListener(this);

        if (getSharedPref().getBoolean(SettingsActivity.restoredefaults, false)) {
            restoreDefaults();
//            getSharedPref().edit().putBoolean(SettingsActivity.restoredefaults, false).apply();
        }
        // Change the column color
        int color = Color.parseColor(getSharedPref().getString("channel_color", "#ffcc00"));
        if (color != orgColor) {// update the channel color
            for (Fixture col : alColumns) {
                col.setScrollColor(color);
            }
        }
        // Change the num of ch.
        int number_channels = 5;
        try {
            number_channels = Integer.parseInt(getSharedPref().getString(SettingsActivity.channels, "5"));
        } catch (Throwable t) {
            Log.w("ExternalStorage", "Error reading channel number", t);
        }
        setNumberOfFixtures(number_channels, null, null, null, null);

        //setup cues, the button inside the cue obj may be from the Cue Sheet
        pm.refreshCueView();

        AuroraNetwork.setUpNetwork(this);
    }

    @Override
    protected void onPause() {
        AuroraNetwork.stopNetwork();
        Log.v(getClass().getSimpleName(), "onPause");
        pm.save(null);
        if (getSharedPref() != null) {
            getSharedPref().unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onPause();
    }

    // We're being destroyed. It's important to dispose of the helper here!
    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        Log.d(getClass().getSimpleName(), "Destroying helper.");
    }

    private void restoreDefaults() {
        Log.v(getClass().getSimpleName(), "Restoring Defaults");

    }

    /**
     * Listener that is called after the user selects a file to import
     *
     * @param requestCode
     * @param resultCode
     * @param resultData
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == ImportFile.READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            if (resultData != null) {
                Uri uri = resultData.getData();
                Log.i("AuroraDMX", "Uri: " + uri.toString());
                //Open the file if the user clicked on a file in the file system
                try {
                    pm.openFile(uri, this);
                } catch (IOException | SecurityException e) {
                    Toast.makeText(MainActivity.this, R.string.cannotOpen, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }

                AuroraNetwork.setUpNetwork(this);

            }
        }
    }

    public void recalculateFixtureNumbers() {
        int currentFixtureNum = 1;
        for (Fixture fixture : alColumns) {
            fixture.setFixtureNumber(currentFixtureNum);
            currentFixtureNum += fixture.getChLevels().size();
        }
    }

    private int calculateChannelCount() {
        int currentFixtureNum = 1;
        for (Fixture fixture : alColumns) {
            currentFixtureNum += fixture.getChLevels().size();
        }
        return currentFixtureNum;
    }

    public static List<Fixture> getAlColumns() {
        return alColumns;
    }

    public static ArrayList<ChaseObj> getAlChases() {
        if (alChases == null) {
            alChases = new ArrayList<>();
        }
        return alChases;
    }

    public static void setAlChases(ArrayList<ChaseObj> alChases) {
        MainActivity.alChases = alChases;
    }

    public static void setUpdatingFixtures(boolean updatingFixtures) {
        MainActivity.updatingFixtures = updatingFixtures;
    }

    public static ArrayList<CueObj> getAlCues() {
        if (alCues == null) {
            alCues = new ArrayList<>();
        }
        return alCues;
    }

    public static void setAlCues(ArrayList<CueObj> alCues) {
        MainActivity.alCues = alCues;
    }
}
