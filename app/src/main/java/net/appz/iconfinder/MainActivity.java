package net.appz.iconfinder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import net.appz.iconfinder.Data.Icon;
import net.appz.iconfinder.Data.Icons;
import net.appz.iconfinder.Data.Iconsets;
import net.appz.iconfinder.Data.Style;
import net.appz.iconfinder.Data.Styles;

import java.util.List;
import java.util.Random;


public class MainActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Icons>,
        NavigationDrawerFragment.NavigationDrawerCallbacks{

    private String TAG = "MainActivity>";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;


    // Instantiate the RequestQueue.
    //private String url = "https://api.iconfinder.com/v2/styles?count=10&after=&_=1427589491914";
    private String urlStyles = "https://api.iconfinder.com" + "/v2/styles";

    private static final String urlIconSetsTmpl = "https://api.iconfinder.com" + "/v2/styles/%s/iconsets";

    private static final String urlIconsTmpl = "https://api.iconfinder.com"
            + "/v2/icons/search?query=%s&minimum_size=%d&maximum_size=%d&count=%d&offset=%d";

    private int count = 20;
    private /* static */ int offset = 0;

    private int stylesPosition = -1;

    private static final int LOADER_ICONS_ID = 1;

    private Styles styles;

    // Request for Json Download
    private RequestQueue requestQueue = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null){
            offset = savedInstanceState.getInt("offset");
            stylesPosition = savedInstanceState.getInt("stylesPosition");
        }

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        if(!AppUtils.isNetworkAvailable(this)){
            AppUtils.showDialog(this, "Internet error", "Check internet connection!", true);
        }else {

            if (requestQueue == null)
                requestQueue = Volley.newRequestQueue(this);


            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment currentFragment = fragmentManager.findFragmentById(R.id.container);
            Log.e(TAG, "onCreate : currentFragment = " + currentFragment);
            if (currentFragment == null) {
                fragmentManager.beginTransaction()
                        .replace(R.id.container,
                                PlaceholderFragment.newInstance(0, null),
                                PlaceholderFragment.class.getSimpleName())
                        .commit();
            }

            urlStyles += "?_=" + new Random().nextInt();
            Log.d(TAG, "Request => " + urlStyles);

            final GsonRequest gsonRequest = new GsonRequest(urlStyles, Styles.class, null, new Response.Listener<Styles>() {
                @Override
                public void onResponse(Styles styles) {
                    fillStyles(styles);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    if (volleyError != null)
                        Log.e(TAG, "volleyError: " + volleyError.getMessage());
                    AppUtils.showDialog(MainActivity.this, "Error", "Server request error. Try again later", false);
                }
            });
            gsonRequest.setTag("Styles");
            requestQueue.add(gsonRequest);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("offset", offset);
        savedInstanceState.putInt("stylesPosition", stylesPosition);
    }

    private void fillStyles(Styles styles) {
        this.styles = styles;
        int i=0;
        String [] stileTitles = new String[styles.getStyles().size()];
        for(Style style : styles.getStyles()) {
            stileTitles[i++] = style.getName();
        }

        // Set the adapter for the list view left menu
        mNavigationDrawerFragment.getDrawerListView().setAdapter(new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
                stileTitles));

    }


    private void fillIconSets(Iconsets iconsets, final int position) {
        stylesPosition = position;
        mTitle = styles.getStyles().get(position).getName();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(mTitle);

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        // update the main content by replacing fragments
        fragmentManager.beginTransaction()
                .replace(R.id.container,
                        PlaceholderFragment.newInstance(position, iconsets),
                        PlaceholderFragment.class.getSimpleName())
                .commit();
    }


    @Override
    public void onNavigationDrawerItemSelected(final int position) {
        if(styles != null ) {
            // Download Iconsets
            String urlIconsets = String.format(urlIconSetsTmpl, styles.getStyles().get(position).getIdentifier());
            Log.d(TAG, "urlIconsets = " + urlIconsets);
            final GsonRequest gsonRequest = new GsonRequest(urlIconsets, Iconsets.class, null, new Response.Listener<Iconsets>() {
                @Override
                public void onResponse(Iconsets iconsets) {
                    fillIconSets(iconsets, position);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    if (volleyError != null)
                        Log.e(TAG, "volleyError: " + volleyError.getMessage());
                    AppUtils.showDialog(MainActivity.this, "Error", "Server request error. Try again later", false);
                }
            });
            gsonRequest.setTag("Iconsets");
            requestQueue.add(gsonRequest);
        }
    }


    @Override
    public void onOptionsItemSelectedReset() {
        stylesPosition = -1;
        mTitle = getResources().getString(R.string.app_name);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(mTitle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fragmentManager.beginTransaction()
                .replace(R.id.container,
                        PlaceholderFragment.newInstance(0, null),
                        PlaceholderFragment.class.getSimpleName())
                .commit();
    }


    @Override
    public Loader<Icons> onCreateLoader(int id, Bundle args) {
        IconsLoader iconsLoader = new IconsLoader(this, args);
        return iconsLoader;
    }


    @Override
    public void onLoadFinished(Loader<Icons> loader, Icons data) {
        if(data == null ) {
            // In Loader happened error
            AppUtils.showDialog(MainActivity.this, "Error", "Server request error. Try again later", false);
            return;
        }

        if(loader.getId() == LOADER_ICONS_ID){
                offset += count;
                Bundle b = new Bundle();
                b.putParcelable("Icons", data);
                Message msg = mHandler.obtainMessage();
                msg.what = ICONS_HANDLER;
                msg.setData(b);
                mHandler.sendMessage(msg);
        }
    }


    final int ICONS_HANDLER = 1;
    final Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            Bundle b;
            if(msg.what == ICONS_HANDLER){
                b=msg.getData();
                Icons icons = b.getParcelable("Icons");
                fillIcons(icons);
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public void onLoaderReset(Loader<Icons> loader) {
        offset = 0;
    }


    synchronized private void fillIcons(Icons icons) {
        Log.d(">>>", "Icons size = " + icons.getIcons().size());

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.container);

        // Resolved After Loader implementation
        //if(!fragmentManager.isDestroyed()) {    // Check problem after rotation screen

        if (currentFragment != null && currentFragment instanceof IconsGridFragment) {
            ((IconsGridFragment) currentFragment).addIcons(icons);
        } else {
            Fragment iconsGridFragment = IconsGridFragment.newInstance(icons);
            // Add the fragment to the activity, pushing this transaction on to the back stack.
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.replace(R.id.container, iconsGridFragment, IconsDetailFragment.class.getSimpleName());
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.addToBackStack(null);
            ft.commit();
        }
    }


    /**
     *
     *  Get Icons by query and Stile
     */
    void queryIcons(String query){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int minimum_size = Integer.valueOf(prefs.getString("minimum_size_list", "16"));
        int maximum_size = Integer.valueOf(prefs.getString("maximum_size_list", "512"));

        String urlIcons = String.format(urlIconsTmpl, query,
                minimum_size,
                maximum_size,
                count,
                offset);

        if (styles != null && stylesPosition != -1) {
            urlIcons += "&style=" + styles.getStyles().get(stylesPosition).getIdentifier();
        }

        Log.d(">>>", "urlIcons = " + urlIcons);

        Bundle bundle = new Bundle();
        bundle.putString(IconsLoader.ARGS_URL, urlIcons);
        LoaderManager loaderManager = getSupportLoaderManager();
        loaderManager.restartLoader(LOADER_ICONS_ID, bundle, MainActivity.this);

//        if (loaderManager.getLoader(LOADER_ICONS_ID) == null) {
//            loaderManager.initLoader(LOADER_ICONS_ID, bundle, this);
//        } else {
//            loaderManager.restartLoader(LOADER_ICONS_ID, bundle, this);
//        }

    }


    /**
     *
     *  On click search button
     */
    public void onQueryIcons(String query) {
        offset = 0;
        queryIcons(query);
    }

    /**
     *
     * More Icon download to grid
     *
     */
    public void onLazyLoadMore() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String query = prefs.getString("query", "facebook");
        queryIcons(query);
    }


    /**
     *
     * Click on grid icons item
     * @param icon
     */
    public void onClickIcon(Icon icon) {
        Log.d(">>>", "Icons id = " + icon.getIconId());

        Fragment iconsDetailFragment = IconsDetailFragment.newInstance(icon);
        // Add the fragment to the activity, pushing this transaction on to the back stack.
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.container, iconsDetailFragment, IconsDetailFragment.class.getSimpleName());
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.addToBackStack(null);
        ft.commit();
    }

    /**
     *
     * Close Save Icon Fragment
     */
    public void onCloseSaveIcon() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack();
    }

    @Override
    protected void onStop () {
        super.onStop();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
        List<Fragment> allFragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : allFragments) {
            if (fragment instanceof IconsGridFragment) {
                ((IconsGridFragment)fragment).resetLoadingFlag();
            }
        }
        Log.d(TAG, "onStop");
    }


    public void onSectionAttached(int position) {
        /*
        if(styles != null && mTitle != null) {
            mTitle = styles.getStyles().get(position).getName();
            ActionBar actionBar = getSupportActionBar();
            actionBar.setTitle(mTitle);
            Log.d(TAG, "Title = " + mTitle.toString());
        }*/
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



}
