/*
 * Copyright 2015-2023 Soren Stoutner <soren@stoutner.com>.
 *
 * Download cookie code contributed 2017 Hendrik Knackstedt.  Copyright assigned to Soren Stoutner <soren@stoutner.com>.
 *
 * This file is part of Privacy Browser Android <https://www.stoutner.com/privacy-browser-android>.
 *
 * Privacy Browser Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Privacy Browser Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Privacy Browser Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.stoutner.privacybrowser.activities;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebViewDatabase;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import com.stoutner.privacybrowser.R;
import com.stoutner.privacybrowser.adapters.WebViewPagerAdapter;
import com.stoutner.privacybrowser.asynctasks.SaveUrl;
import com.stoutner.privacybrowser.asynctasks.SaveWebpageImage;
import com.stoutner.privacybrowser.coroutines.GetHostIpAddressesCoroutine;
import com.stoutner.privacybrowser.coroutines.PopulateBlocklistsCoroutine;
import com.stoutner.privacybrowser.coroutines.PrepareSaveDialogCoroutine;
import com.stoutner.privacybrowser.dataclasses.PendingDialogDataClass;
import com.stoutner.privacybrowser.dialogs.CreateBookmarkDialog;
import com.stoutner.privacybrowser.dialogs.CreateBookmarkFolderDialog;
import com.stoutner.privacybrowser.dialogs.CreateHomeScreenShortcutDialog;
import com.stoutner.privacybrowser.dialogs.FontSizeDialog;
import com.stoutner.privacybrowser.dialogs.HttpAuthenticationDialog;
import com.stoutner.privacybrowser.dialogs.OpenDialog;
import com.stoutner.privacybrowser.dialogs.ProxyNotInstalledDialog;
import com.stoutner.privacybrowser.dialogs.PinnedMismatchDialog;
import com.stoutner.privacybrowser.dialogs.SaveDialog;
import com.stoutner.privacybrowser.dialogs.SslCertificateErrorDialog;
import com.stoutner.privacybrowser.dialogs.UrlHistoryDialog;
import com.stoutner.privacybrowser.dialogs.ViewSslCertificateDialog;
import com.stoutner.privacybrowser.dialogs.WaitingForProxyDialog;
import com.stoutner.privacybrowser.fragments.WebViewTabFragment;
import com.stoutner.privacybrowser.helpers.BlocklistHelper;
import com.stoutner.privacybrowser.helpers.BookmarksDatabaseHelper;
import com.stoutner.privacybrowser.helpers.DomainsDatabaseHelper;
import com.stoutner.privacybrowser.helpers.ProxyHelper;
import com.stoutner.privacybrowser.helpers.SanitizeUrlHelper;
import com.stoutner.privacybrowser.helpers.UrlHelper;
import com.stoutner.privacybrowser.views.NestedScrollWebView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import java.text.NumberFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Pair;

public class MainWebViewActivity extends AppCompatActivity implements CreateBookmarkDialog.CreateBookmarkListener, CreateBookmarkFolderDialog.CreateBookmarkFolderListener,
        FontSizeDialog.UpdateFontSizeListener, NavigationView.OnNavigationItemSelectedListener, OpenDialog.OpenListener, PinnedMismatchDialog.PinnedMismatchListener,
        PopulateBlocklistsCoroutine.PopulateBlocklistsListener, SaveDialog.SaveListener, UrlHistoryDialog.NavigateHistoryListener, WebViewTabFragment.NewTabListener {

    // Define the public static variables.
    public static final ExecutorService executorService = Executors.newFixedThreadPool(4);
    public static String orbotStatus = "unknown";
    public static final ArrayList<PendingDialogDataClass> pendingDialogsArrayList =  new ArrayList<>();
    public static String proxyMode = ProxyHelper.NONE;

    // Declare the public static variables.
    public static String currentBookmarksFolder = "";
    public static boolean restartFromBookmarksActivity;
    public static WebViewPagerAdapter webViewPagerAdapter;

    // Declare the public static views.
    public static AppBarLayout appBarLayout;

    // The user agent constants are public static so they can be accessed from `SettingsFragment`, `DomainsActivity`, and `DomainSettingsFragment`.
    public final static int UNRECOGNIZED_USER_AGENT = -1;
    public final static int SETTINGS_WEBVIEW_DEFAULT_USER_AGENT = 1;
    public final static int SETTINGS_CUSTOM_USER_AGENT = 11;
    public final static int DOMAINS_SYSTEM_DEFAULT_USER_AGENT = 0;
    public final static int DOMAINS_WEBVIEW_DEFAULT_USER_AGENT = 2;
    public final static int DOMAINS_CUSTOM_USER_AGENT = 12;

    // Define the saved instance state constants.
    private final String BOOKMARKS_DRAWER_PINNED = "bookmarks_drawer_pinned";
    private final String PROXY_MODE = "proxy_mode";
    private final String SAVED_STATE_ARRAY_LIST = "saved_state_array_list";
    private final String SAVED_NESTED_SCROLL_WEBVIEW_STATE_ARRAY_LIST = "saved_nested_scroll_webview_state_array_list";
    private final String SAVED_TAB_POSITION = "saved_tab_position";

    // Define the saved instance state variables.
    private ArrayList<Bundle> savedStateArrayList;
    private ArrayList<Bundle> savedNestedScrollWebViewStateArrayList;
    private int savedTabPosition;
    private String savedProxyMode;

    // The current WebView is used in `onCreate()`, `onPrepareOptionsMenu()`, `onOptionsItemSelected()`, `onNavigationItemSelected()`, `onRestart()`, `onCreateContextMenu()`, `findPreviousOnPage()`,
    // `findNextOnPage()`, `closeFindOnPage()`, `loadUrlFromTextBox()`, `onSslMismatchBack()`, `applyProxy()`, and `applyDomainSettings()`.
    private NestedScrollWebView currentWebView;

    // The search URL is set in `applyAppSettings()` and used in `onNewIntent()`, `loadUrlFromTextBox()`, `initializeApp()`, and `initializeWebView()`.
    private String searchURL;

    // The blocklists are populated in `finishedPopulatingBlocklists()` and accessed from `initializeWebView()`.
    private ArrayList<List<String[]>> easyList;
    private ArrayList<List<String[]>> easyPrivacy;
    private ArrayList<List<String[]>> fanboysAnnoyanceList;
    private ArrayList<List<String[]>> fanboysSocialList;
    private ArrayList<List<String[]>> ultraList;
    private ArrayList<List<String[]>> ultraPrivacy;

    // The action bar drawer toggle is initialized in `onCreate()` and used in `onResume()`.
    private ActionBarDrawerToggle actionBarDrawerToggle;

    // `bookmarksCursor` is used in `onDestroy()`, `onOptionsItemSelected()`, `onCreateBookmark()`, `onCreateBookmarkFolder()`, `onSaveEditBookmark()`, `onSaveEditBookmarkFolder()`, and `loadBookmarksFolder()`.
    private Cursor bookmarksCursor;

    // `bookmarksCursorAdapter` is used in `onCreateBookmark()`, `onCreateBookmarkFolder()` `onSaveEditBookmark()`, `onSaveEditBookmarkFolder()`, and `loadBookmarksFolder()`.
    private CursorAdapter bookmarksCursorAdapter;

    // `fileChooserCallback` is used in `onCreate()` and `onActivityResult()`.
    private ValueCallback<Uri[]> fileChooserCallback;

    // The default progress view offsets are set in `onCreate()` and used in `initializeWebView()`.
    private int appBarHeight;
    private int defaultProgressViewStartOffset;
    private int defaultProgressViewEndOffset;

    // Declare the helpers.
    private BookmarksDatabaseHelper bookmarksDatabaseHelper;
    private DomainsDatabaseHelper domainsDatabaseHelper;
    private ProxyHelper proxyHelper;

    // Declare the class variables
    private boolean bookmarksDrawerPinned;
    private boolean bottomAppBar;
    private boolean displayAdditionalAppBarIcons;
    private boolean displayingFullScreenVideo;
    private boolean downloadWithExternalApp;
    private ForegroundColorSpan finalGrayColorSpan;
    private boolean fullScreenBrowsingModeEnabled;
    private boolean hideAppBar;
    private boolean inFullScreenBrowsingMode;
    private boolean incognitoModeEnabled;
    private ForegroundColorSpan initialGrayColorSpan;
    private boolean loadingNewIntent;
    private BroadcastReceiver orbotStatusBroadcastReceiver;
    private boolean reapplyAppSettingsOnRestart;
    private boolean reapplyDomainSettingsOnRestart;
    private ForegroundColorSpan redColorSpan;
    private boolean sanitizeAmpRedirects;
    private boolean sanitizeTrackingQueries;
    private boolean scrollAppBar;
    private boolean waitingForProxy;
    private String webViewDefaultUserAgent;

    // Define the class variables.
    private ObjectAnimator objectAnimator = new ObjectAnimator();
    private String saveUrlString = "";

    // Declare the class views.
    private ActionBar actionBar;
    private CoordinatorLayout coordinatorLayout;
    private ImageView bookmarksDrawerPinnedImageView;
    private DrawerLayout drawerLayout;
    private LinearLayout findOnPageLinearLayout;
    private FrameLayout fullScreenVideoFrameLayout;
    private FrameLayout rootFrameLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout tabsLinearLayout;
    private TabLayout tabLayout;
    private Toolbar toolbar;
    private EditText urlEditText;
    private RelativeLayout urlRelativeLayout;
    private ViewPager webViewPager;

    // Declare the class menus.
    private Menu optionsMenu;

    // Declare the class menu items.
    private MenuItem navigationBackMenuItem;
    private MenuItem navigationForwardMenuItem;
    private MenuItem navigationHistoryMenuItem;
    private MenuItem navigationRequestsMenuItem;
    private MenuItem optionsPrivacyMenuItem;
    private MenuItem optionsRefreshMenuItem;
    private MenuItem optionsCookiesMenuItem;
    private MenuItem optionsDomStorageMenuItem;
    private MenuItem optionsSaveFormDataMenuItem;
    private MenuItem optionsClearDataMenuItem;
    private MenuItem optionsClearCookiesMenuItem;
    private MenuItem optionsClearDomStorageMenuItem;
    private MenuItem optionsClearFormDataMenuItem;
    private MenuItem optionsBlocklistsMenuItem;
    private MenuItem optionsEasyListMenuItem;
    private MenuItem optionsEasyPrivacyMenuItem;
    private MenuItem optionsFanboysAnnoyanceListMenuItem;
    private MenuItem optionsFanboysSocialBlockingListMenuItem;
    private MenuItem optionsUltraListMenuItem;
    private MenuItem optionsUltraPrivacyMenuItem;
    private MenuItem optionsBlockAllThirdPartyRequestsMenuItem;
    private MenuItem optionsProxyMenuItem;
    private MenuItem optionsProxyNoneMenuItem;
    private MenuItem optionsProxyTorMenuItem;
    private MenuItem optionsProxyI2pMenuItem;
    private MenuItem optionsProxyCustomMenuItem;
    private MenuItem optionsUserAgentMenuItem;
    private MenuItem optionsUserAgentPrivacyBrowserMenuItem;
    private MenuItem optionsUserAgentWebViewDefaultMenuItem;
    private MenuItem optionsUserAgentFirefoxOnAndroidMenuItem;
    private MenuItem optionsUserAgentChromeOnAndroidMenuItem;
    private MenuItem optionsUserAgentSafariOnIosMenuItem;
    private MenuItem optionsUserAgentFirefoxOnLinuxMenuItem;
    private MenuItem optionsUserAgentChromiumOnLinuxMenuItem;
    private MenuItem optionsUserAgentFirefoxOnWindowsMenuItem;
    private MenuItem optionsUserAgentChromeOnWindowsMenuItem;
    private MenuItem optionsUserAgentEdgeOnWindowsMenuItem;
    private MenuItem optionsUserAgentInternetExplorerOnWindowsMenuItem;
    private MenuItem optionsUserAgentSafariOnMacosMenuItem;
    private MenuItem optionsUserAgentCustomMenuItem;
    private MenuItem optionsSwipeToRefreshMenuItem;
    private MenuItem optionsWideViewportMenuItem;
    private MenuItem optionsDisplayImagesMenuItem;
    private MenuItem optionsDarkWebViewMenuItem;
    private MenuItem optionsFontSizeMenuItem;
    private MenuItem optionsAddOrEditDomainMenuItem;

    // This variable won't be needed once the class is migrated to Kotlin, as can be seen in LogcatActivity or AboutVersionFragment.
    private Activity resultLauncherActivityHandle;

    // Define the save URL activity result launcher.  It must be defined before `onCreate()` is run or the app will crash.
    private final ActivityResultLauncher<String> saveUrlActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("*/*"),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri fileUri) {
                    // Only save the URL if the file URI is not null, which happens if the user exited the file picker by pressing back.
                    if (fileUri != null) {
                        new SaveUrl(getApplicationContext(), resultLauncherActivityHandle, fileUri, currentWebView.getSettings().getUserAgentString(), currentWebView.getAcceptCookies()).execute(saveUrlString);
                    }

                    // Reset the save URL string.
                    saveUrlString = "";
                }
            });

    // Define the save webpage archive activity result launcher.  It must be defined before `onCreate()` is run or the app will crash.
    private final ActivityResultLauncher<String> saveWebpageArchiveActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("multipart/related"),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri fileUri) {
                    // Only save the webpage archive if the file URI is not null, which happens if the user exited the file picker by pressing back.
                    if (fileUri != null) {
                        // Initialize the file name string from the file URI last path segment.
                        String temporaryFileNameString = fileUri.getLastPathSegment();

                        // Query the exact file name if the API >= 26.
                        if (Build.VERSION.SDK_INT >= 26) {
                            // Get a cursor from the content resolver.
                            Cursor contentResolverCursor = resultLauncherActivityHandle.getContentResolver().query(fileUri, null, null, null);

                            // Move to the fist row.
                            contentResolverCursor.moveToFirst();

                            // Get the file name from the cursor.
                            temporaryFileNameString = contentResolverCursor.getString(contentResolverCursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));

                            // Close the cursor.
                            contentResolverCursor.close();
                        }

                        // Save the final file name string so it can be used inside the lambdas.  This will no longer be needed once this activity has transitioned to Kotlin.
                        String finalFileNameString = temporaryFileNameString;

                        try {
                            // Create a temporary MHT file.
                            File temporaryMhtFile = File.createTempFile("temporary_mht_file", ".mht", getCacheDir());
                            currentWebView.saveWebArchive(temporaryMhtFile.toString(), false, callbackValue -> {
                                if (callbackValue != null) {  // The temporary MHT file was saved successfully.
                                    try {
                                        // Create a temporary MHT file input stream.
                                        FileInputStream temporaryMhtFileInputStream = new FileInputStream(temporaryMhtFile);

                                        // Get an output stream for the save webpage file path.
                                        OutputStream mhtOutputStream = getContentResolver().openOutputStream(fileUri);

                                        // Create a transfer byte array.
                                        byte[] transferByteArray = new byte[1024];

                                        // Create an integer to track the number of bytes read.
                                        int bytesRead;

                                        // Copy the temporary MHT file input stream to the MHT output stream.
                                        while ((bytesRead = temporaryMhtFileInputStream.read(transferByteArray)) > 0) {
                                            mhtOutputStream.write(transferByteArray, 0, bytesRead);
                                        }

                                        // Close the streams.
                                        mhtOutputStream.close();
                                        temporaryMhtFileInputStream.close();

                                        // Display a snackbar.
                                        Snackbar.make(currentWebView, getString(R.string.saved, finalFileNameString), Snackbar.LENGTH_SHORT).show();
                                    } catch (Exception exception) {
                                        // Display a snackbar with the exception.
                                        Snackbar.make(currentWebView, getString(R.string.error_saving_file, finalFileNameString, exception), Snackbar.LENGTH_INDEFINITE).show();
                                    } finally {
                                        // Delete the temporary MHT file.
                                        //noinspection ResultOfMethodCallIgnored
                                        temporaryMhtFile.delete();
                                    }
                                } else {  // There was an unspecified error while saving the temporary MHT file.
                                    // Display an error snackbar.
                                    Snackbar.make(currentWebView, getString(R.string.error_saving_file, finalFileNameString, getString(R.string.unknown_error)), Snackbar.LENGTH_INDEFINITE).show();
                                }
                            });
                        } catch (IOException ioException) {
                            // Display a snackbar with the IO exception.
                            Snackbar.make(currentWebView, getString(R.string.error_saving_file, finalFileNameString, ioException), Snackbar.LENGTH_INDEFINITE).show();
                        }
                    }
                }
            });

    // Define the save webpage image activity result launcher.  It must be defined before `onCreate()` is run or the app will crash.
    private final ActivityResultLauncher<String> saveWebpageImageActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("image/png"),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri fileUri) {
                    // Only save the webpage image if the file URI is not null, which happens if the user exited the file picker by pressing back.
                    if (fileUri != null) {
                        // Save the webpage image.
                        new SaveWebpageImage(resultLauncherActivityHandle, fileUri, currentWebView).execute();
                    }
                }
            });

    // Define the save webpage image activity result launcher.  It must be defined before `onCreate()` is run or the app will crash.
    private final ActivityResultLauncher<Intent> browseFileUploadActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult activityResult) {
                    // Pass the file to the WebView.
                    fileChooserCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(activityResult.getResultCode(), activityResult.getData()));
                }
            });

    // Remove the warning about needing to override `performClick()` when using an `OnTouchListener` with WebView.
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Run the default commands.
        super.onCreate(savedInstanceState);

        // Populate the result launcher activity.  This will no longer be needed once the activity has transitioned to Kotlin.
        resultLauncherActivityHandle = this;

        // Initialize the default preference values the first time the program is run.  `false` keeps this command from resetting any current preferences back to default.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Get a handle for the shared preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Get the preferences.
        String appTheme = sharedPreferences.getString(getString(R.string.app_theme_key), getString(R.string.app_theme_default_value));
        boolean allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false);
        bottomAppBar = sharedPreferences.getBoolean(getString(R.string.bottom_app_bar_key), false);
        displayAdditionalAppBarIcons = sharedPreferences.getBoolean(getString(R.string.display_additional_app_bar_icons_key), false);

        // Get the theme entry values string array.
        String[] appThemeEntryValuesStringArray = getResources().getStringArray(R.array.app_theme_entry_values);

        // Get the current theme status.
        int currentThemeStatus = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        // Set the app theme according to the preference.  A switch statement cannot be used because the theme entry values string array is not a compile time constant.
        if (appTheme.equals(appThemeEntryValuesStringArray[1])) {  // The light theme is selected.
            // Apply the light theme.
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (appTheme.equals(appThemeEntryValuesStringArray[2])) {  // The dark theme is selected.
            // Apply the dark theme.
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {  // The system default theme is selected.
            if (Build.VERSION.SDK_INT >= 28) {  // The system default theme is supported.
                // Follow the system default theme.
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            } else {  // The system default theme is not supported.
                // Follow the battery saver mode.
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
            }
        }

        // Do not continue if the app theme is different than the OS theme.  The app always initially starts in the OS theme.
        // If the user has specified the opposite theme should be used, the app will restart in that mode after the above `setDefaultNightMode()` code processes.  However, the restart is delayed.
        // If the blacklist coroutine starts below it will continue to run during the restart, which leads to indeterminate behavior, with the system often not knowing how many tabs exist.
        // See https://redmine.stoutner.com/issues/952.
        if (appTheme.equals(appThemeEntryValuesStringArray[0]) ||  // The system default theme is used.
                (appTheme.equals(appThemeEntryValuesStringArray[1]) && currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) ||  // The app is running in day theme as desired.
                (appTheme.equals(appThemeEntryValuesStringArray[2]) && currentThemeStatus == Configuration.UI_MODE_NIGHT_YES)) {  // The app is running in night theme as desired.

            // Disable screenshots if not allowed.
            if (!allowScreenshots) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }

            // Check to see if the activity has been restarted.
            if (savedInstanceState != null) {
                // Store the saved instance state variables.
                bookmarksDrawerPinned = savedInstanceState.getBoolean(BOOKMARKS_DRAWER_PINNED);
                savedStateArrayList = savedInstanceState.getParcelableArrayList(SAVED_STATE_ARRAY_LIST);
                savedNestedScrollWebViewStateArrayList = savedInstanceState.getParcelableArrayList(SAVED_NESTED_SCROLL_WEBVIEW_STATE_ARRAY_LIST);
                savedTabPosition = savedInstanceState.getInt(SAVED_TAB_POSITION);
                savedProxyMode = savedInstanceState.getString(PROXY_MODE);
            }

            // Enable the drawing of the entire webpage.  This makes it possible to save a website image.  This must be done before anything else happens with the WebView.
            WebView.enableSlowWholeDocumentDraw();

            // Set the content view according to the position of the app bar.
            if (bottomAppBar) setContentView(R.layout.main_framelayout_bottom_appbar);
            else setContentView(R.layout.main_framelayout_top_appbar);

            // Get handles for the views.
            rootFrameLayout = findViewById(R.id.root_framelayout);
            drawerLayout = findViewById(R.id.drawerlayout);
            coordinatorLayout = findViewById(R.id.coordinatorlayout);
            appBarLayout = findViewById(R.id.appbar_layout);
            toolbar = findViewById(R.id.toolbar);
            findOnPageLinearLayout = findViewById(R.id.find_on_page_linearlayout);
            tabsLinearLayout = findViewById(R.id.tabs_linearlayout);
            tabLayout = findViewById(R.id.tablayout);
            swipeRefreshLayout = findViewById(R.id.swiperefreshlayout);
            webViewPager = findViewById(R.id.webviewpager);
            NavigationView navigationView = findViewById(R.id.navigationview);
            bookmarksDrawerPinnedImageView = findViewById(R.id.bookmarks_drawer_pinned_imageview);
            fullScreenVideoFrameLayout = findViewById(R.id.full_screen_video_framelayout);

            // Get a handle for the navigation menu.
            Menu navigationMenu = navigationView.getMenu();

            // Get handles for the navigation menu items.
            navigationBackMenuItem = navigationMenu.findItem(R.id.back);
            navigationForwardMenuItem = navigationMenu.findItem(R.id.forward);
            navigationHistoryMenuItem = navigationMenu.findItem(R.id.history);
            navigationRequestsMenuItem = navigationMenu.findItem(R.id.requests);

            // Listen for touches on the navigation menu.
            navigationView.setNavigationItemSelectedListener(this);

            // Get a handle for the app compat delegate.
            AppCompatDelegate appCompatDelegate = getDelegate();

            // Set the support action bar.
            appCompatDelegate.setSupportActionBar(toolbar);

            // Get a handle for the action bar.
            actionBar = appCompatDelegate.getSupportActionBar();

            // Remove the incorrect lint warning below that the action bar might be null.
            assert actionBar != null;

            // Add the custom layout, which shows the URL text bar.
            actionBar.setCustomView(R.layout.url_app_bar);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

            // Get handles for the views in the URL app bar.
            urlRelativeLayout = findViewById(R.id.url_relativelayout);
            urlEditText = findViewById(R.id.url_edittext);

            // Create the hamburger icon at the start of the AppBar.
            actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_navigation_drawer, R.string.close_navigation_drawer);

            // Initially disable the sliding drawers.  They will be enabled once the blocklists are loaded.
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

            // Initially hide the user interface so that only the blocklist loading screen is shown (if reloading).
            drawerLayout.setVisibility(View.GONE);

            // Initialize the web view pager adapter.
            webViewPagerAdapter = new WebViewPagerAdapter(getSupportFragmentManager());

            // Set the pager adapter on the web view pager.
            webViewPager.setAdapter(webViewPagerAdapter);

            // Store up to 100 tabs in memory.
            webViewPager.setOffscreenPageLimit(100);

            // Instantiate the helpers.
            bookmarksDatabaseHelper = new BookmarksDatabaseHelper(this);
            domainsDatabaseHelper = new DomainsDatabaseHelper(this);
            proxyHelper = new ProxyHelper();

            // Update the bookmarks drawer pinned image view.
            updateBookmarksDrawerPinnedImageView();

            // Initialize the app.
            initializeApp();

            // Apply the app settings from the shared preferences.
            applyAppSettings();

            // Control what the system back command does.
            OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    // Process the different back options.
                    if (drawerLayout.isDrawerVisible(GravityCompat.START)) {  // The navigation drawer is open.
                        // Close the navigation drawer.
                        drawerLayout.closeDrawer(GravityCompat.START);
                    } else if (drawerLayout.isDrawerVisible(GravityCompat.END)) {  // The bookmarks drawer is open.
                        // close the bookmarks drawer.
                        drawerLayout.closeDrawer(GravityCompat.END);
                    } else if (displayingFullScreenVideo) {  // A full screen video is shown.
                        // Exit the full screen video.
                        exitFullScreenVideo();
                        // It shouldn't be possible for the currentWebView to be null, but crash logs indicate it sometimes happens.
                    } else if ((currentWebView != null) && (currentWebView.canGoBack())) {  // There is at least one item in the current WebView history.
                        // Get the current web back forward list.
                        WebBackForwardList webBackForwardList = currentWebView.copyBackForwardList();

                        // Get the previous entry URL.
                        String previousUrl = webBackForwardList.getItemAtIndex(webBackForwardList.getCurrentIndex() - 1).getUrl();

                        // Apply the domain settings.
                        applyDomainSettings(currentWebView, previousUrl, false, false, false);

                        // Go back.
                        currentWebView.goBack();
                    } else if (tabLayout.getTabCount() > 1) {  // There are at least two tabs.
                        // Close the current tab.
                        closeCurrentTab();
                    } else {  // There isn't anything to do in Privacy Browser.
                        // Run clear and exit.
                        clearAndExit();
                    }
                }
            };

            // Register the on back pressed callback.
            getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);

            // Instantiate the populate blocklists coroutine.
            PopulateBlocklistsCoroutine populateBlocklistsCoroutine = new PopulateBlocklistsCoroutine(this);

            // Populate the blocklists.
            populateBlocklistsCoroutine.populateBlocklists(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // Run the default commands.
        super.onNewIntent(intent);

        // Check to see if the app is being restarted from a saved state.
        if (savedStateArrayList == null || savedStateArrayList.size() == 0) {  // The activity is not being restarted from a saved state.
            // Get the information from the intent.
            String intentAction = intent.getAction();
            Uri intentUriData = intent.getData();
            String intentStringExtra = intent.getStringExtra(Intent.EXTRA_TEXT);

            // Determine if this is a web search.
            boolean isWebSearch = ((intentAction != null) && intentAction.equals(Intent.ACTION_WEB_SEARCH));

            // Only process the URI if it contains data or it is a web search.  If the user pressed the desktop icon after the app was already running the URI will be null.
            if (intentUriData != null || intentStringExtra != null || isWebSearch) {
                // Exit the full screen video if it is displayed.
                if (displayingFullScreenVideo) {
                    // Exit full screen video mode.
                    exitFullScreenVideo();

                    // Reload the current WebView.  Otherwise, it can display entirely black.
                    currentWebView.reload();
                }

                // Get the shared preferences.
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

                // Create a URL string.
                String url;

                // If the intent action is a web search, perform the search.
                if (isWebSearch) {  // The intent is a web search.
                    // Create an encoded URL string.
                    String encodedUrlString;

                    // Sanitize the search input and convert it to a search.
                    try {
                        encodedUrlString = URLEncoder.encode(intent.getStringExtra(SearchManager.QUERY), "UTF-8");
                    } catch (UnsupportedEncodingException exception) {
                        encodedUrlString = "";
                    }

                    // Add the base search URL.
                    url = searchURL + encodedUrlString;
                } else if (intentUriData != null) {  // The intent contains a URL formatted as a URI.
                    // Set the intent data as the URL.
                    url = intentUriData.toString();
                } else {  // The intent contains a string, which might be a URL.
                    // Set the intent string as the URL.
                    url = intentStringExtra;
                }

                // Add a new tab if specified in the preferences.
                if (sharedPreferences.getBoolean(getString(R.string.open_intents_in_new_tab_key), true)) {  // Load the URL in a new tab.
                    // Set the loading new intent flag.
                    loadingNewIntent = true;

                    // Add a new tab.
                    addNewTab(url, true);
                } else {  // Load the URL in the current tab.
                    // Make it so.
                    loadUrl(currentWebView, url);
                }

                // Close the navigation drawer if it is open.
                if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }

                // Close the bookmarks drawer if it is open.
                if (drawerLayout.isDrawerVisible(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                }
            }
        } else {  // The app has been restarted.
            // Set the saved tab position to be the size of the saved state array list.  The tab position is 0 based, meaning the at the new tab will be the tab position that is restored.
            savedTabPosition = savedStateArrayList.size();

            // Replace the intent that started the app with this one.  This will load the tab after the others have been restored.
            setIntent(intent);
        }
    }

    @Override
    public void onRestart() {
        // Run the default commands.
        super.onRestart();

        // Apply the app settings if returning from the Settings activity.
        if (reapplyAppSettingsOnRestart) {
            // Reset the reapply app settings on restart tracker.
            reapplyAppSettingsOnRestart = false;

            // Apply the app settings.
            applyAppSettings();
        }

        // Apply the domain settings if returning from the settings or domains activity.
        if (reapplyDomainSettingsOnRestart) {
            // Reset the reapply domain settings on restart tracker.
            reapplyDomainSettingsOnRestart = false;

            // Reapply the domain settings for each tab.
            for (int i = 0; i < webViewPagerAdapter.getCount(); i++) {
                // Get the WebView tab fragment.
                WebViewTabFragment webViewTabFragment = webViewPagerAdapter.getPageFragment(i);

                // Get the fragment view.
                View fragmentView = webViewTabFragment.getView();

                // Only reload the WebViews if they exist.
                if (fragmentView != null) {
                    // Get the nested scroll WebView from the tab fragment.
                    NestedScrollWebView nestedScrollWebView = fragmentView.findViewById(R.id.nestedscroll_webview);

                    // Reset the current domain name so the domain settings will be reapplied.
                    nestedScrollWebView.setCurrentDomainName("");

                    // Reapply the domain settings if the URL is not null, which can happen if an empty tab is active when returning from settings.
                    if (nestedScrollWebView.getUrl() != null) {
                        applyDomainSettings(nestedScrollWebView, nestedScrollWebView.getUrl(), false, true, false);
                    }
                }
            }
        }

        // Update the bookmarks drawer if returning from the Bookmarks activity.
        if (restartFromBookmarksActivity) {
            // Close the bookmarks drawer.
            drawerLayout.closeDrawer(GravityCompat.END);

            // Reload the bookmarks drawer.
            loadBookmarksFolder();

            // Reset `restartFromBookmarksActivity`.
            restartFromBookmarksActivity = false;
        }

        // Update the privacy icon.  `true` runs `invalidateOptionsMenu` as the last step.  This can be important if the screen was rotated.
        updatePrivacyIcons(true);
    }

    // `onStart()` runs after `onCreate()` or `onRestart()`.  This is used instead of `onResume()` so the commands aren't called every time the screen is partially hidden.
    @Override
    public void onStart() {
        // Run the default commands.
        super.onStart();

        // Resume any WebViews if the pager adapter exists.  If the app is restarting to change the initial app theme it won't have been populated yet.
        if (webViewPagerAdapter != null) {
            for (int i = 0; i < webViewPagerAdapter.getCount(); i++) {
                // Get the WebView tab fragment.
                WebViewTabFragment webViewTabFragment = webViewPagerAdapter.getPageFragment(i);

                // Get the fragment view.
                View fragmentView = webViewTabFragment.getView();

                // Only resume the WebViews if they exist (they won't when the app is first created).
                if (fragmentView != null) {
                    // Get the nested scroll WebView from the tab fragment.
                    NestedScrollWebView nestedScrollWebView = fragmentView.findViewById(R.id.nestedscroll_webview);

                    // Resume the nested scroll WebView.
                    nestedScrollWebView.onResume();
                }
            }
        }

        // Resume the nested scroll WebView JavaScript timers.  This is a global command that resumes JavaScript timers on all WebViews.
        if (currentWebView != null) {
            currentWebView.resumeTimers();
        }

        // Reapply the proxy settings if the system is using a proxy.  This redisplays the appropriate alert dialog.
        if (!proxyMode.equals(ProxyHelper.NONE)) {
            applyProxy(false);
        }

        // Reapply any system UI flags.
        if (displayingFullScreenVideo || inFullScreenBrowsingMode) {  // The system is displaying a website or a video in full screen mode.
            /* Hide the system bars.
             * SYSTEM_UI_FLAG_FULLSCREEN hides the status bar at the top of the screen.
             * SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN makes the root frame layout fill the area that is normally reserved for the status bar.
             * SYSTEM_UI_FLAG_HIDE_NAVIGATION hides the navigation bar on the bottom or right of the screen.
             * SYSTEM_UI_FLAG_IMMERSIVE_STICKY makes the status and navigation bars translucent and automatically re-hides them after they are shown.
             */
            rootFrameLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        // Show any pending dialogs.
        for (int i = 0; i < pendingDialogsArrayList.size(); i++) {
            // Get the pending dialog from the array list.
            PendingDialogDataClass pendingDialogDataClass = pendingDialogsArrayList.get(i);

            // Show the pending dialog.
            pendingDialogDataClass.dialogFragment.show(getSupportFragmentManager(), pendingDialogDataClass.tag);
        }

        // Clear the pending dialogs array list.
        pendingDialogsArrayList.clear();
    }

    // `onStop()` runs after `onPause()`.  It is used instead of `onPause()` so the commands are not called every time the screen is partially hidden.
    @Override
    public void onStop() {
        // Run the default commands.
        super.onStop();

        // Only pause the WebViews if the pager adapter is not null, which is the case if the app is restarting to change the initial app theme.
        if (webViewPagerAdapter != null) {
            // Pause each web view.
            for (int i = 0; i < webViewPagerAdapter.getCount(); i++) {
                // Get the WebView tab fragment.
                WebViewTabFragment webViewTabFragment = webViewPagerAdapter.getPageFragment(i);

                // Get the fragment view.
                View fragmentView = webViewTabFragment.getView();

                // Only pause the WebViews if they exist (they won't when the app is first created).
                if (fragmentView != null) {
                    // Get the nested scroll WebView from the tab fragment.
                    NestedScrollWebView nestedScrollWebView = fragmentView.findViewById(R.id.nestedscroll_webview);

                    // Pause the nested scroll WebView.
                    nestedScrollWebView.onPause();
                }
            }
        }

        // Pause the WebView JavaScript timers.  This is a global command that pauses JavaScript on all WebViews.
        if (currentWebView != null) {
            currentWebView.pauseTimers();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        // Run the default commands.
        super.onSaveInstanceState(savedInstanceState);

        // Only save the instance state if the WebView pager adapter is not null, which will be the case if the app is restarting to change the initial app theme.
        if (webViewPagerAdapter != null) {
            // Create the saved state array lists.
            ArrayList<Bundle> savedStateArrayList = new ArrayList<>();
            ArrayList<Bundle> savedNestedScrollWebViewStateArrayList = new ArrayList<>();

            // Get the URLs from each tab.
            for (int i = 0; i < webViewPagerAdapter.getCount(); i++) {
                // Get the WebView tab fragment.
                WebViewTabFragment webViewTabFragment = webViewPagerAdapter.getPageFragment(i);

                // Get the fragment view.
                View fragmentView = webViewTabFragment.getView();

                if (fragmentView != null) {
                    // Get the nested scroll WebView from the tab fragment.
                    NestedScrollWebView nestedScrollWebView = fragmentView.findViewById(R.id.nestedscroll_webview);

                    // Create saved state bundle.
                    Bundle savedStateBundle = new Bundle();

                    // Get the current states.
                    nestedScrollWebView.saveState(savedStateBundle);
                    Bundle savedNestedScrollWebViewStateBundle = nestedScrollWebView.saveNestedScrollWebViewState();

                    // Store the saved states in the array lists.
                    savedStateArrayList.add(savedStateBundle);
                    savedNestedScrollWebViewStateArrayList.add(savedNestedScrollWebViewStateBundle);
                }
            }

            // Get the current tab position.
            int currentTabPosition = tabLayout.getSelectedTabPosition();

            // Store the saved states in the bundle.
            savedInstanceState.putBoolean(BOOKMARKS_DRAWER_PINNED, bookmarksDrawerPinned);
            savedInstanceState.putString(PROXY_MODE, proxyMode);
            savedInstanceState.putParcelableArrayList(SAVED_STATE_ARRAY_LIST, savedStateArrayList);
            savedInstanceState.putParcelableArrayList(SAVED_NESTED_SCROLL_WEBVIEW_STATE_ARRAY_LIST, savedNestedScrollWebViewStateArrayList);
            savedInstanceState.putInt(SAVED_TAB_POSITION, currentTabPosition);
        }
    }

    @Override
    public void onDestroy() {
        // Unregister the orbot status broadcast receiver if it exists.
        if (orbotStatusBroadcastReceiver != null) {
            this.unregisterReceiver(orbotStatusBroadcastReceiver);
        }

        // Close the bookmarks cursor if it exists.
        if (bookmarksCursor != null) {
            bookmarksCursor.close();
        }

        // Close the bookmarks database if it exists.
        if (bookmarksDatabaseHelper != null) {
            bookmarksDatabaseHelper.close();
        }

        // Run the default commands.
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu.  This adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.webview_options_menu, menu);

        // Store a handle for the options menu so it can be used by `onOptionsItemSelected()` and `updatePrivacyIcons()`.
        optionsMenu = menu;

        // Get handles for the menu items.
        optionsPrivacyMenuItem = menu.findItem(R.id.javascript);
        optionsRefreshMenuItem = menu.findItem(R.id.refresh);
        MenuItem bookmarksMenuItem = menu.findItem(R.id.bookmarks);
        optionsCookiesMenuItem = menu.findItem(R.id.cookies);
        optionsDomStorageMenuItem = menu.findItem(R.id.dom_storage);
        optionsSaveFormDataMenuItem = menu.findItem(R.id.save_form_data);  // Form data can be removed once the minimum API >= 26.
        optionsClearDataMenuItem = menu.findItem(R.id.clear_data);
        optionsClearCookiesMenuItem = menu.findItem(R.id.clear_cookies);
        optionsClearDomStorageMenuItem = menu.findItem(R.id.clear_dom_storage);
        optionsClearFormDataMenuItem = menu.findItem(R.id.clear_form_data);  // Form data can be removed once the minimum API >= 26.
        optionsBlocklistsMenuItem = menu.findItem(R.id.blocklists);
        optionsEasyListMenuItem = menu.findItem(R.id.easylist);
        optionsEasyPrivacyMenuItem = menu.findItem(R.id.easyprivacy);
        optionsFanboysAnnoyanceListMenuItem = menu.findItem(R.id.fanboys_annoyance_list);
        optionsFanboysSocialBlockingListMenuItem = menu.findItem(R.id.fanboys_social_blocking_list);
        optionsUltraListMenuItem = menu.findItem(R.id.ultralist);
        optionsUltraPrivacyMenuItem = menu.findItem(R.id.ultraprivacy);
        optionsBlockAllThirdPartyRequestsMenuItem = menu.findItem(R.id.block_all_third_party_requests);
        optionsProxyMenuItem = menu.findItem(R.id.proxy);
        optionsProxyNoneMenuItem = menu.findItem(R.id.proxy_none);
        optionsProxyTorMenuItem = menu.findItem(R.id.proxy_tor);
        optionsProxyI2pMenuItem = menu.findItem(R.id.proxy_i2p);
        optionsProxyCustomMenuItem = menu.findItem(R.id.proxy_custom);
        optionsUserAgentMenuItem = menu.findItem(R.id.user_agent);
        optionsUserAgentPrivacyBrowserMenuItem = menu.findItem(R.id.user_agent_privacy_browser);
        optionsUserAgentWebViewDefaultMenuItem = menu.findItem(R.id.user_agent_webview_default);
        optionsUserAgentFirefoxOnAndroidMenuItem = menu.findItem(R.id.user_agent_firefox_on_android);
        optionsUserAgentChromeOnAndroidMenuItem = menu.findItem(R.id.user_agent_chrome_on_android);
        optionsUserAgentSafariOnIosMenuItem = menu.findItem(R.id.user_agent_safari_on_ios);
        optionsUserAgentFirefoxOnLinuxMenuItem = menu.findItem(R.id.user_agent_firefox_on_linux);
        optionsUserAgentChromiumOnLinuxMenuItem = menu.findItem(R.id.user_agent_chromium_on_linux);
        optionsUserAgentFirefoxOnWindowsMenuItem = menu.findItem(R.id.user_agent_firefox_on_windows);
        optionsUserAgentChromeOnWindowsMenuItem = menu.findItem(R.id.user_agent_chrome_on_windows);
        optionsUserAgentEdgeOnWindowsMenuItem = menu.findItem(R.id.user_agent_edge_on_windows);
        optionsUserAgentInternetExplorerOnWindowsMenuItem = menu.findItem(R.id.user_agent_internet_explorer_on_windows);
        optionsUserAgentSafariOnMacosMenuItem = menu.findItem(R.id.user_agent_safari_on_macos);
        optionsUserAgentCustomMenuItem = menu.findItem(R.id.user_agent_custom);
        optionsSwipeToRefreshMenuItem = menu.findItem(R.id.swipe_to_refresh);
        optionsWideViewportMenuItem = menu.findItem(R.id.wide_viewport);
        optionsDisplayImagesMenuItem = menu.findItem(R.id.display_images);
        optionsDarkWebViewMenuItem = menu.findItem(R.id.dark_webview);
        optionsFontSizeMenuItem = menu.findItem(R.id.font_size);
        optionsAddOrEditDomainMenuItem = menu.findItem(R.id.add_or_edit_domain);

        // Set the initial status of the privacy icons.  `false` does not call `invalidateOptionsMenu` as the last step.
        updatePrivacyIcons(false);

        // Only display the form data menu items if the API < 26.
        optionsSaveFormDataMenuItem.setVisible(Build.VERSION.SDK_INT < 26);
        optionsClearFormDataMenuItem.setVisible(Build.VERSION.SDK_INT < 26);

        // Disable the clear form data menu item if the API >= 26 so that the status of the main Clear Data is calculated correctly.
        optionsClearFormDataMenuItem.setEnabled(Build.VERSION.SDK_INT < 26);

        // Only display the dark WebView menu item if the API >= 29.
        optionsDarkWebViewMenuItem.setVisible(Build.VERSION.SDK_INT >= 29);

        // Set the status of the additional app bar icons.  Setting the refresh menu item to `SHOW_AS_ACTION_ALWAYS` makes it appear even on small devices like phones.
        if (displayAdditionalAppBarIcons) {
            optionsRefreshMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            bookmarksMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            optionsCookiesMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        } else { //Do not display the additional icons.
            optionsRefreshMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            bookmarksMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            optionsCookiesMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }

        // Replace Refresh with Stop if a URL is already loading.
        if (currentWebView != null && currentWebView.getProgress() != 100) {
            // Set the title.
            optionsRefreshMenuItem.setTitle(R.string.stop);

            // Set the icon if it is displayed in the app bar.  Once the minimum API is >= 26, the blue and black icons can be combined with a tint list.
            if (displayAdditionalAppBarIcons) {
                optionsRefreshMenuItem.setIcon(R.drawable.close_blue);
            }
        }

        // Done.
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Get a handle for the cookie manager.
        CookieManager cookieManager = CookieManager.getInstance();

        // Initialize the current user agent string and the font size.
        String currentUserAgent = getString(R.string.user_agent_privacy_browser);
        int fontSize = 100;

        // Set items that require the current web view to be populated.  It will be null when the program is first opened, as `onPrepareOptionsMenu()` is called before the first WebView is initialized.
        if (currentWebView != null) {
            // Set the add or edit domain text.
            if (currentWebView.getDomainSettingsApplied()) {
                optionsAddOrEditDomainMenuItem.setTitle(R.string.edit_domain_settings);
            } else {
                optionsAddOrEditDomainMenuItem.setTitle(R.string.add_domain_settings);
            }

            // Get the current user agent from the WebView.
            currentUserAgent = currentWebView.getSettings().getUserAgentString();

            // Get the current font size from the
            fontSize = currentWebView.getSettings().getTextZoom();

            // Set the status of the menu item checkboxes.
            optionsDomStorageMenuItem.setChecked(currentWebView.getSettings().getDomStorageEnabled());
            optionsSaveFormDataMenuItem.setChecked(currentWebView.getSettings().getSaveFormData());  // Form data can be removed once the minimum API >= 26.
            optionsEasyListMenuItem.setChecked(currentWebView.getEasyListEnabled());
            optionsEasyPrivacyMenuItem.setChecked(currentWebView.getEasyPrivacyEnabled());
            optionsFanboysAnnoyanceListMenuItem.setChecked(currentWebView.getFanboysAnnoyanceListEnabled());
            optionsFanboysSocialBlockingListMenuItem.setChecked(currentWebView.getFanboysSocialBlockingListEnabled());
            optionsUltraListMenuItem.setChecked(currentWebView.getUltraListEnabled());
            optionsUltraPrivacyMenuItem.setChecked(currentWebView.getUltraPrivacyEnabled());
            optionsBlockAllThirdPartyRequestsMenuItem.setChecked(currentWebView.getBlockAllThirdPartyRequests());
            optionsSwipeToRefreshMenuItem.setChecked(currentWebView.getSwipeToRefresh());
            optionsWideViewportMenuItem.setChecked(currentWebView.getSettings().getUseWideViewPort());
            optionsDisplayImagesMenuItem.setChecked(currentWebView.getSettings().getLoadsImagesAutomatically());

            // Initialize the display names for the blocklists with the number of blocked requests.
            optionsBlocklistsMenuItem.setTitle(getString(R.string.blocklists) + " - " + currentWebView.getRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS));
            optionsEasyListMenuItem.setTitle(currentWebView.getRequestsCount(NestedScrollWebView.EASYLIST) + " - " + getString(R.string.easylist));
            optionsEasyPrivacyMenuItem.setTitle(currentWebView.getRequestsCount(NestedScrollWebView.EASYPRIVACY) + " - " + getString(R.string.easyprivacy));
            optionsFanboysAnnoyanceListMenuItem.setTitle(currentWebView.getRequestsCount(NestedScrollWebView.FANBOYS_ANNOYANCE_LIST) + " - " + getString(R.string.fanboys_annoyance_list));
            optionsFanboysSocialBlockingListMenuItem.setTitle(currentWebView.getRequestsCount(NestedScrollWebView.FANBOYS_SOCIAL_BLOCKING_LIST) + " - " + getString(R.string.fanboys_social_blocking_list));
            optionsUltraListMenuItem.setTitle(currentWebView.getRequestsCount(NestedScrollWebView.ULTRALIST) + " - " + getString(R.string.ultralist));
            optionsUltraPrivacyMenuItem.setTitle(currentWebView.getRequestsCount(NestedScrollWebView.ULTRAPRIVACY) + " - " + getString(R.string.ultraprivacy));
            optionsBlockAllThirdPartyRequestsMenuItem.setTitle(currentWebView.getRequestsCount(NestedScrollWebView.THIRD_PARTY_REQUESTS) + " - " + getString(R.string.block_all_third_party_requests));

            // Enable DOM Storage if JavaScript is enabled.
            optionsDomStorageMenuItem.setEnabled(currentWebView.getSettings().getJavaScriptEnabled());

            // Get the current theme status.
            int currentThemeStatus = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

            // Enable dark WebView if night mode is enabled.
            optionsDarkWebViewMenuItem.setEnabled(currentThemeStatus == Configuration.UI_MODE_NIGHT_YES);

            // Set the checkbox status for dark WebView if the device is running API >= 29 and algorithmic darkening is supported.
            if ((Build.VERSION.SDK_INT >= 29) && WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
                optionsDarkWebViewMenuItem.setChecked(WebSettingsCompat.isAlgorithmicDarkeningAllowed(currentWebView.getSettings()));
        }

        // Set the cookies menu item checked status.
        optionsCookiesMenuItem.setChecked(cookieManager.acceptCookie());

        // Enable Clear Cookies if there are any.
        optionsClearCookiesMenuItem.setEnabled(cookieManager.hasCookies());

        // Get the application's private data directory, which will be something like `/data/user/0/com.stoutner.privacybrowser.standard`, which links to `/data/data/com.stoutner.privacybrowser.standard`.
        String privateDataDirectoryString = getApplicationInfo().dataDir;

        // Get a count of the number of files in the Local Storage directory.
        File localStorageDirectory = new File (privateDataDirectoryString + "/app_webview/Local Storage/");
        int localStorageDirectoryNumberOfFiles = 0;
        if (localStorageDirectory.exists()) {
            // `Objects.requireNonNull` removes a lint warning that `localStorageDirectory.list` might produce a null pointed exception if it is dereferenced.
            localStorageDirectoryNumberOfFiles = Objects.requireNonNull(localStorageDirectory.list()).length;
        }

        // Get a count of the number of files in the IndexedDB directory.
        File indexedDBDirectory = new File (privateDataDirectoryString + "/app_webview/IndexedDB");
        int indexedDBDirectoryNumberOfFiles = 0;
        if (indexedDBDirectory.exists()) {
            // `Objects.requireNonNull` removes a lint warning that `indexedDBDirectory.list` might produce a null pointed exception if it is dereferenced.
            indexedDBDirectoryNumberOfFiles = Objects.requireNonNull(indexedDBDirectory.list()).length;
        }

        // Enable Clear DOM Storage if there is any.
        optionsClearDomStorageMenuItem.setEnabled(localStorageDirectoryNumberOfFiles > 0 || indexedDBDirectoryNumberOfFiles > 0);

        // Enable Clear Form Data is there is any.  This can be removed once the minimum API >= 26.
        if (Build.VERSION.SDK_INT < 26) {
            // Get the WebView database.
            WebViewDatabase webViewDatabase = WebViewDatabase.getInstance(this);

            // Enable the clear form data menu item if there is anything to clear.
            optionsClearFormDataMenuItem.setEnabled(webViewDatabase.hasFormData());
        }

        // Enable Clear Data if any of the submenu items are enabled.
        optionsClearDataMenuItem.setEnabled(optionsClearCookiesMenuItem.isEnabled() || optionsClearDomStorageMenuItem.isEnabled() || optionsClearFormDataMenuItem.isEnabled());

        // Disable Fanboy's Social Blocking List menu item if Fanboy's Annoyance List is checked.
        optionsFanboysSocialBlockingListMenuItem.setEnabled(!optionsFanboysAnnoyanceListMenuItem.isChecked());

        // Set the proxy title and check the applied proxy.
        switch (proxyMode) {
            case ProxyHelper.NONE:
                // Set the proxy title.
                optionsProxyMenuItem.setTitle(getString(R.string.proxy) + " - " + getString(R.string.proxy_none));

                // Check the proxy None radio button.
                optionsProxyNoneMenuItem.setChecked(true);
                break;

            case ProxyHelper.TOR:
                // Set the proxy title.
                optionsProxyMenuItem.setTitle(getString(R.string.proxy) + " - " + getString(R.string.proxy_tor));

                // Check the proxy Tor radio button.
                optionsProxyTorMenuItem.setChecked(true);
                break;

            case ProxyHelper.I2P:
                // Set the proxy title.
                optionsProxyMenuItem.setTitle(getString(R.string.proxy) + " - " + getString(R.string.proxy_i2p));

                // Check the proxy I2P radio button.
                optionsProxyI2pMenuItem.setChecked(true);
                break;

            case ProxyHelper.CUSTOM:
                // Set the proxy title.
                optionsProxyMenuItem.setTitle(getString(R.string.proxy) + " - " + getString(R.string.proxy_custom));

                // Check the proxy Custom radio button.
                optionsProxyCustomMenuItem.setChecked(true);
                break;
        }

        // Select the current user agent menu item.  A switch statement cannot be used because the user agents are not compile time constants.
        if (currentUserAgent.equals(getResources().getStringArray(R.array.user_agent_data)[0])) {  // Privacy Browser.
            // Update the user agent menu item title.
            optionsUserAgentMenuItem.setTitle(getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_privacy_browser));

            // Select the Privacy Browser radio box.
            optionsUserAgentPrivacyBrowserMenuItem.setChecked(true);
        } else if (currentUserAgent.equals(webViewDefaultUserAgent)) {  // WebView Default.
            // Update the user agent menu item title.
            optionsUserAgentMenuItem.setTitle(getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_webview_default));

            // Select the WebView Default radio box.
            optionsUserAgentWebViewDefaultMenuItem.setChecked(true);
        } else if (currentUserAgent.equals(getResources().getStringArray(R.array.user_agent_data)[2])) {  // Firefox on Android.
            // Update the user agent menu item title.
            optionsUserAgentMenuItem.setTitle(getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_firefox_on_android));

            // Select the Firefox on Android radio box.
            optionsUserAgentFirefoxOnAndroidMenuItem.setChecked(true);
        } else if (currentUserAgent.equals(getResources().getStringArray(R.array.user_agent_data)[3])) {  // Chrome on Android.
            // Update the user agent menu item title.
            optionsUserAgentMenuItem.setTitle(getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_chrome_on_android));

            // Select the Chrome on Android radio box.
            optionsUserAgentChromeOnAndroidMenuItem.setChecked(true);
        } else if (currentUserAgent.equals(getResources().getStringArray(R.array.user_agent_data)[4])) {  // Safari on iOS.
            // Update the user agent menu item title.
            optionsUserAgentMenuItem.setTitle(getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_safari_on_ios));

            // Select the Safari on iOS radio box.
            optionsUserAgentSafariOnIosMenuItem.setChecked(true);
        } else if (currentUserAgent.equals(getResources().getStringArray(R.array.user_agent_data)[5])) {  // Firefox on Linux.
            // Update the user agent menu item title.
            optionsUserAgentMenuItem.setTitle(getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_firefox_on_linux));

            // Select the Firefox on Linux radio box.
            optionsUserAgentFirefoxOnLinuxMenuItem.setChecked(true);
        } else if (currentUserAgent.equals(getResources().getStringArray(R.array.user_agent_data)[6])) {  // Chromium on Linux.
            // Update the user agent menu item title.
            optionsUserAgentMenuItem.setTitle(getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_chromium_on_linux));

            // Select the Chromium on Linux radio box.
            optionsUserAgentChromiumOnLinuxMenuItem.setChecked(true);
        } else if (currentUserAgent.equals(getResources().getStringArray(R.array.user_agent_data)[7])) {  // Firefox on Windows.
            // Update the user agent menu item title.
            optionsUserAgentMenuItem.setTitle(getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_firefox_on_windows));

            // Select the Firefox on Windows radio box.
            optionsUserAgentFirefoxOnWindowsMenuItem.setChecked(true);
        } else if (currentUserAgent.equals(getResources().getStringArray(R.array.user_agent_data)[8])) {  // Chrome on Windows.
            // Update the user agent menu item title.
            optionsUserAgentMenuItem.setTitle(getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_chrome_on_windows));

            // Select the Chrome on Windows radio box.
            optionsUserAgentChromeOnWindowsMenuItem.setChecked(true);
        } else if (currentUserAgent.equals(getResources().getStringArray(R.array.user_agent_data)[9])) {  // Edge on Windows.
            // Update the user agent menu item title.
            optionsUserAgentMenuItem.setTitle(getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_edge_on_windows));

            // Select the Edge on Windows radio box.
            optionsUserAgentEdgeOnWindowsMenuItem.setChecked(true);
        } else if (currentUserAgent.equals(getResources().getStringArray(R.array.user_agent_data)[10])) {  // Internet Explorer on Windows.
            // Update the user agent menu item title.
            optionsUserAgentMenuItem.setTitle(getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_internet_explorer_on_windows));

            // Select the Internet on Windows radio box.
            optionsUserAgentInternetExplorerOnWindowsMenuItem.setChecked(true);
        } else if (currentUserAgent.equals(getResources().getStringArray(R.array.user_agent_data)[11])) {  // Safari on macOS.
            // Update the user agent menu item title.
            optionsUserAgentMenuItem.setTitle(getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_safari_on_macos));

            // Select the Safari on macOS radio box.
            optionsUserAgentSafariOnMacosMenuItem.setChecked(true);
        } else {  // Custom user agent.
            // Update the user agent menu item title.
            optionsUserAgentMenuItem.setTitle(getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_custom));

            // Select the Custom radio box.
            optionsUserAgentCustomMenuItem.setChecked(true);
        }

        // Set the font size title.
        optionsFontSizeMenuItem.setTitle(getString(R.string.font_size) + " - " + fontSize + "%");

        // Run all the other default commands.
        super.onPrepareOptionsMenu(menu);

        // Display the menu.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        // Get a handle for the shared preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Get a handle for the cookie manager.
        CookieManager cookieManager = CookieManager.getInstance();

        // Get the selected menu item ID.
        int menuItemId = menuItem.getItemId();

        // Run the commands that correlate to the selected menu item.
        if (menuItemId == R.id.javascript) {  // JavaScript.
            // Toggle the JavaScript status.
            currentWebView.getSettings().setJavaScriptEnabled(!currentWebView.getSettings().getJavaScriptEnabled());

            // Update the privacy icon.
            updatePrivacyIcons(true);

            // Display a `Snackbar`.
            if (currentWebView.getSettings().getJavaScriptEnabled()) {  // JavaScrip is enabled.
                Snackbar.make(webViewPager, R.string.javascript_enabled, Snackbar.LENGTH_SHORT).show();
            } else if (cookieManager.acceptCookie()) {  // JavaScript is disabled, but first-party cookies are enabled.
                Snackbar.make(webViewPager, R.string.javascript_disabled, Snackbar.LENGTH_SHORT).show();
            } else {  // Privacy mode.
                Snackbar.make(webViewPager, R.string.privacy_mode, Snackbar.LENGTH_SHORT).show();
            }

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.refresh) {  // Refresh.
            // Run the command that correlates to the current status of the menu item.
            if (menuItem.getTitle().equals(getString(R.string.refresh))) {  // The refresh button was pushed.
                // Reload the current WebView.
                currentWebView.reload();
            } else {  // The stop button was pushed.
                // Stop the loading of the WebView.
                currentWebView.stopLoading();
            }

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.bookmarks) {  // Bookmarks.
            // Open the bookmarks drawer.
            drawerLayout.openDrawer(GravityCompat.END);

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.cookies) {  // Cookies.
            // Switch the first-party cookie status.
            cookieManager.setAcceptCookie(!cookieManager.acceptCookie());

            // Store the cookie status.
            currentWebView.setAcceptCookies(cookieManager.acceptCookie());

            // Update the menu checkbox.
            menuItem.setChecked(cookieManager.acceptCookie());

            // Update the privacy icon.
            updatePrivacyIcons(true);

            // Display a snackbar.
            if (cookieManager.acceptCookie()) {  // Cookies are enabled.
                Snackbar.make(webViewPager, R.string.cookies_enabled, Snackbar.LENGTH_SHORT).show();
            } else if (currentWebView.getSettings().getJavaScriptEnabled()) {  // JavaScript is still enabled.
                Snackbar.make(webViewPager, R.string.cookies_disabled, Snackbar.LENGTH_SHORT).show();
            } else {  // Privacy mode.
                Snackbar.make(webViewPager, R.string.privacy_mode, Snackbar.LENGTH_SHORT).show();
            }

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.dom_storage) {  // DOM storage.
            // Toggle the status of domStorageEnabled.
            currentWebView.getSettings().setDomStorageEnabled(!currentWebView.getSettings().getDomStorageEnabled());

            // Update the menu checkbox.
            menuItem.setChecked(currentWebView.getSettings().getDomStorageEnabled());

            // Update the privacy icon.
            updatePrivacyIcons(true);

            // Display a snackbar.
            if (currentWebView.getSettings().getDomStorageEnabled()) {
                Snackbar.make(webViewPager, R.string.dom_storage_enabled, Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(webViewPager, R.string.dom_storage_disabled, Snackbar.LENGTH_SHORT).show();
            }

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.save_form_data) {  // Form data.  This can be removed once the minimum API >= 26.
            // Switch the status of saveFormDataEnabled.
            currentWebView.getSettings().setSaveFormData(!currentWebView.getSettings().getSaveFormData());

            // Update the menu checkbox.
            menuItem.setChecked(currentWebView.getSettings().getSaveFormData());

            // Display a snackbar.
            if (currentWebView.getSettings().getSaveFormData()) {
                Snackbar.make(webViewPager, R.string.form_data_enabled, Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(webViewPager, R.string.form_data_disabled, Snackbar.LENGTH_SHORT).show();
            }

            // Update the privacy icon.
            updatePrivacyIcons(true);

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.clear_cookies) {  // Clear cookies.
            // Create a snackbar.
            Snackbar.make(webViewPager, R.string.cookies_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo, v -> {
                        // Do nothing because everything will be handled by `onDismissed()` below.
                    })
                    .addCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {  // The snackbar was dismissed without the undo button being pushed.
                                // Delete the cookies.
                                cookieManager.removeAllCookies(null);
                            }
                        }
                    })
                    .show();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.clear_dom_storage) {  // Clear DOM storage.
            // Create a snackbar.
            Snackbar.make(webViewPager, R.string.dom_storage_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo, v -> {
                        // Do nothing because everything will be handled by `onDismissed()` below.
                    })
                    .addCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {  // The snackbar was dismissed without the undo button being pushed.
                                // Delete the DOM Storage.
                                WebStorage webStorage = WebStorage.getInstance();
                                webStorage.deleteAllData();

                                // Initialize a handler to manually delete the DOM storage files and directories.
                                Handler deleteDomStorageHandler = new Handler();

                                // Setup a runnable to manually delete the DOM storage files and directories.
                                Runnable deleteDomStorageRunnable = () -> {
                                    try {
                                        // Get a handle for the runtime.
                                        Runtime runtime = Runtime.getRuntime();

                                        // Get the application's private data directory, which will be something like `/data/user/0/com.stoutner.privacybrowser.standard`,
                                        // which links to `/data/data/com.stoutner.privacybrowser.standard`.
                                        String privateDataDirectoryString = getApplicationInfo().dataDir;

                                        // A string array must be used because the directory contains a space and `Runtime.exec` will otherwise not escape the string correctly.
                                        Process deleteLocalStorageProcess = runtime.exec(new String[]{"rm", "-rf", privateDataDirectoryString + "/app_webview/Local Storage/"});

                                        // Multiple commands must be used because `Runtime.exec()` does not like `*`.
                                        Process deleteIndexProcess = runtime.exec("rm -rf " + privateDataDirectoryString + "/app_webview/IndexedDB");
                                        Process deleteQuotaManagerProcess = runtime.exec("rm -f " + privateDataDirectoryString + "/app_webview/QuotaManager");
                                        Process deleteQuotaManagerJournalProcess = runtime.exec("rm -f " + privateDataDirectoryString + "/app_webview/QuotaManager-journal");
                                        Process deleteDatabasesProcess = runtime.exec("rm -rf " + privateDataDirectoryString + "/app_webview/databases");

                                        // Wait for the processes to finish.
                                        deleteLocalStorageProcess.waitFor();
                                        deleteIndexProcess.waitFor();
                                        deleteQuotaManagerProcess.waitFor();
                                        deleteQuotaManagerJournalProcess.waitFor();
                                        deleteDatabasesProcess.waitFor();
                                    } catch (Exception exception) {
                                        // Do nothing if an error is thrown.
                                    }
                                };

                                // Manually delete the DOM storage files after 200 milliseconds.
                                deleteDomStorageHandler.postDelayed(deleteDomStorageRunnable, 200);
                            }
                        }
                    })
                    .show();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.clear_form_data) {  // Clear form data.  This can be remove once the minimum API >= 26.
            // Create a snackbar.
            Snackbar.make(webViewPager, R.string.form_data_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo, v -> {
                        // Do nothing because everything will be handled by `onDismissed()` below.
                    })
                    .addCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {  // The snackbar was dismissed without the undo button being pushed.
                                // Get a handle for the webView database.
                                WebViewDatabase webViewDatabase = WebViewDatabase.getInstance(getApplicationContext());

                                // Delete the form data.
                                webViewDatabase.clearFormData();
                            }
                        }
                    })
                    .show();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.easylist) {  // EasyList.
            // Toggle the EasyList status.
            currentWebView.setEasyListEnabled(!currentWebView.getEasyListEnabled());

            // Update the menu checkbox.
            menuItem.setChecked(currentWebView.getEasyListEnabled());

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.easyprivacy) {  // EasyPrivacy.
            // Toggle the EasyPrivacy status.
            currentWebView.setEasyPrivacyEnabled(!currentWebView.getEasyPrivacyEnabled());

            // Update the menu checkbox.
            menuItem.setChecked(currentWebView.getEasyPrivacyEnabled());

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.fanboys_annoyance_list) {  // Fanboy's Annoyance List.
            // Toggle Fanboy's Annoyance List status.
            currentWebView.setFanboysAnnoyanceListEnabled(!currentWebView.getFanboysAnnoyanceListEnabled());

            // Update the menu checkbox.
            menuItem.setChecked(currentWebView.getFanboysAnnoyanceListEnabled());

            // Update the status of Fanboy's Social Blocking List.
            optionsFanboysSocialBlockingListMenuItem.setEnabled(!currentWebView.getFanboysAnnoyanceListEnabled());

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.fanboys_social_blocking_list) {  // Fanboy's Social Blocking List.
            // Toggle Fanboy's Social Blocking List status.
            currentWebView.setFanboysSocialBlockingListEnabled(!currentWebView.getFanboysSocialBlockingListEnabled());

            // Update the menu checkbox.
            menuItem.setChecked(currentWebView.getFanboysSocialBlockingListEnabled());

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.ultralist) {  // UltraList.
            // Toggle the UltraList status.
            currentWebView.setUltraListEnabled(!currentWebView.getUltraListEnabled());

            // Update the menu checkbox.
            menuItem.setChecked(currentWebView.getUltraListEnabled());

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.ultraprivacy) {  // UltraPrivacy.
            // Toggle the UltraPrivacy status.
            currentWebView.setUltraPrivacyEnabled(!currentWebView.getUltraPrivacyEnabled());

            // Update the menu checkbox.
            menuItem.setChecked(currentWebView.getUltraPrivacyEnabled());

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.block_all_third_party_requests) {  // Block all third-party requests.
            //Toggle the third-party requests blocker status.
            currentWebView.setBlockAllThirdPartyRequests(!currentWebView.getBlockAllThirdPartyRequests());

            // Update the menu checkbox.
            menuItem.setChecked(currentWebView.getBlockAllThirdPartyRequests());

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.proxy_none) {  // Proxy - None.
            // Update the proxy mode.
            proxyMode = ProxyHelper.NONE;

            // Apply the proxy mode.
            applyProxy(true);

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.proxy_tor) {  // Proxy - Tor.
            // Update the proxy mode.
            proxyMode = ProxyHelper.TOR;

            // Apply the proxy mode.
            applyProxy(true);

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.proxy_i2p) {  // Proxy - I2P.
            // Update the proxy mode.
            proxyMode = ProxyHelper.I2P;

            // Apply the proxy mode.
            applyProxy(true);

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.proxy_custom) {  // Proxy - Custom.
            // Update the proxy mode.
            proxyMode = ProxyHelper.CUSTOM;

            // Apply the proxy mode.
            applyProxy(true);

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.user_agent_privacy_browser) {  // User Agent - Privacy Browser.
            // Update the user agent.
            currentWebView.getSettings().setUserAgentString(getResources().getStringArray(R.array.user_agent_data)[0]);

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.user_agent_webview_default) {  // User Agent - WebView Default.
            // Update the user agent.
            currentWebView.getSettings().setUserAgentString("");

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.user_agent_firefox_on_android) {  // User Agent - Firefox on Android.
            // Update the user agent.
            currentWebView.getSettings().setUserAgentString(getResources().getStringArray(R.array.user_agent_data)[2]);

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.user_agent_chrome_on_android) {  // User Agent - Chrome on Android.
            // Update the user agent.
            currentWebView.getSettings().setUserAgentString(getResources().getStringArray(R.array.user_agent_data)[3]);

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.user_agent_safari_on_ios) {  // User Agent - Safari on iOS.
            // Update the user agent.
            currentWebView.getSettings().setUserAgentString(getResources().getStringArray(R.array.user_agent_data)[4]);

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.user_agent_firefox_on_linux) {  // User Agent - Firefox on Linux.
            // Update the user agent.
            currentWebView.getSettings().setUserAgentString(getResources().getStringArray(R.array.user_agent_data)[5]);

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.user_agent_chromium_on_linux) {  // User Agent - Chromium on Linux.
            // Update the user agent.
            currentWebView.getSettings().setUserAgentString(getResources().getStringArray(R.array.user_agent_data)[6]);

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.user_agent_firefox_on_windows) {  // User Agent - Firefox on Windows.
            // Update the user agent.
            currentWebView.getSettings().setUserAgentString(getResources().getStringArray(R.array.user_agent_data)[7]);

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.user_agent_chrome_on_windows) {  // User Agent - Chrome on Windows.
            // Update the user agent.
            currentWebView.getSettings().setUserAgentString(getResources().getStringArray(R.array.user_agent_data)[8]);

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.user_agent_edge_on_windows) {  // User Agent - Edge on Windows.
            // Update the user agent.
            currentWebView.getSettings().setUserAgentString(getResources().getStringArray(R.array.user_agent_data)[9]);

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.user_agent_internet_explorer_on_windows) {  // User Agent - Internet Explorer on Windows.
            // Update the user agent.
            currentWebView.getSettings().setUserAgentString(getResources().getStringArray(R.array.user_agent_data)[10]);

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.user_agent_safari_on_macos) {  // User Agent - Safari on macOS.
            // Update the user agent.
            currentWebView.getSettings().setUserAgentString(getResources().getStringArray(R.array.user_agent_data)[11]);

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.user_agent_custom) {  // User Agent - Custom.
            // Update the user agent.
            currentWebView.getSettings().setUserAgentString(sharedPreferences.getString(getString(R.string.custom_user_agent_key), getString(R.string.custom_user_agent_default_value)));

            // Reload the current WebView.
            currentWebView.reload();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.font_size) {  // Font size.
            // Instantiate the font size dialog.
            DialogFragment fontSizeDialogFragment = FontSizeDialog.displayDialog(currentWebView.getSettings().getTextZoom());

            // Show the font size dialog.
            fontSizeDialogFragment.show(getSupportFragmentManager(), getString(R.string.font_size));

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.swipe_to_refresh) {  // Swipe to refresh.
            // Toggle the stored status of swipe to refresh.
            currentWebView.setSwipeToRefresh(!currentWebView.getSwipeToRefresh());

            // Update the swipe refresh layout.
            if (currentWebView.getSwipeToRefresh()) {  // Swipe to refresh is enabled.
                // Only enable the swipe refresh layout if the WebView is scrolled to the top.  It is updated every time the scroll changes.
                swipeRefreshLayout.setEnabled(currentWebView.getScrollY() == 0);
            } else {  // Swipe to refresh is disabled.
                // Disable the swipe refresh layout.
                swipeRefreshLayout.setEnabled(false);
            }

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.wide_viewport) {  // Wide viewport.
            // Toggle the viewport.
            currentWebView.getSettings().setUseWideViewPort(!currentWebView.getSettings().getUseWideViewPort());

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.display_images) {  // Display images.
            // Toggle the displaying of images.
            if (currentWebView.getSettings().getLoadsImagesAutomatically()) {  // Images are currently loaded automatically.
                // Disable loading of images.
                currentWebView.getSettings().setLoadsImagesAutomatically(false);

                // Reload the website to remove existing images.
                currentWebView.reload();
            } else {  // Images are not currently loaded automatically.
                // Enable loading of images.  Missing images will be loaded without the need for a reload.
                currentWebView.getSettings().setLoadsImagesAutomatically(true);
            }

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.dark_webview) {  // Dark WebView.
            // Toggle dark WebView if supported.
            if ((Build.VERSION.SDK_INT >= 29) && WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(currentWebView.getSettings(), !WebSettingsCompat.isAlgorithmicDarkeningAllowed(currentWebView.getSettings()));

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.find_on_page) {  // Find on page.
            // Get a handle for the views.
            Toolbar toolbar = findViewById(R.id.toolbar);
            LinearLayout findOnPageLinearLayout = findViewById(R.id.find_on_page_linearlayout);
            EditText findOnPageEditText = findViewById(R.id.find_on_page_edittext);

            // Set the minimum height of the find on page linear layout to match the toolbar.
            findOnPageLinearLayout.setMinimumHeight(toolbar.getHeight());

            // Hide the toolbar.
            toolbar.setVisibility(View.GONE);

            // Show the find on page linear layout.
            findOnPageLinearLayout.setVisibility(View.VISIBLE);

            // Display the keyboard.  The app must wait 200 ms before running the command to work around a bug in Android.
            // http://stackoverflow.com/questions/5520085/android-show-softkeyboard-with-showsoftinput-is-not-working
            findOnPageEditText.postDelayed(() -> {
                // Set the focus on the find on page edit text.
                findOnPageEditText.requestFocus();

                // Get a handle for the input method manager.
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

                // Remove the lint warning below that the input method manager might be null.
                assert inputMethodManager != null;

                // Display the keyboard.  `0` sets no input flags.
                inputMethodManager.showSoftInput(findOnPageEditText, 0);
            }, 200);

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.print) {  // Print.
            // Get a print manager instance.
            PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);

            // Remove the lint error below that print manager might be null.
            assert printManager != null;

            // Create a print document adapter from the current WebView.
            PrintDocumentAdapter printDocumentAdapter = currentWebView.createPrintDocumentAdapter(getString(R.string.print));

            // Print the document.
            printManager.print(getString(R.string.privacy_browser_webpage), printDocumentAdapter, null);

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.save_url) {  // Save URL.
            // Check the download preference.
            if (downloadWithExternalApp) {  // Download with an external app.
                downloadUrlWithExternalApp(currentWebView.getCurrentUrl());
            } else {  // Handle the download inside of Privacy Browser.
                // Prepare the save dialog.  The dialog will be displayed once the file size and the content disposition have been acquired.
                PrepareSaveDialogCoroutine.prepareSaveDialog(this, getSupportFragmentManager(), currentWebView.getCurrentUrl(), currentWebView.getSettings().getUserAgentString(),
                        currentWebView.getAcceptCookies());
            }

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.save_archive) {
            // Open the file picker with a default file name built from the current domain name.
            saveWebpageArchiveActivityResultLauncher.launch(currentWebView.getCurrentDomainName() + ".mht");

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.save_image) {  // Save image.
            // Open the file picker with a default file name built from the current domain name.
            saveWebpageImageActivityResultLauncher.launch(currentWebView.getCurrentDomainName() + ".png");

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.add_to_homescreen) {  // Add to homescreen.
            // Instantiate the create home screen shortcut dialog.
            DialogFragment createHomeScreenShortcutDialogFragment = CreateHomeScreenShortcutDialog.createDialog(currentWebView.getTitle(), currentWebView.getUrl(),
                    currentWebView.getFavoriteIcon());

            // Show the create home screen shortcut dialog.
            createHomeScreenShortcutDialogFragment.show(getSupportFragmentManager(), getString(R.string.create_shortcut));

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.view_source) {  // View source.
            // Create an intent to launch the view source activity.
            Intent viewSourceIntent = new Intent(this, ViewSourceActivity.class);

            // Add the variables to the intent.
            viewSourceIntent.putExtra(ViewSourceActivityKt.CURRENT_URL, currentWebView.getUrl());
            viewSourceIntent.putExtra(ViewSourceActivityKt.USER_AGENT, currentWebView.getSettings().getUserAgentString());

            // Make it so.
            startActivity(viewSourceIntent);

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.share_message) {  // Share a message.
            // Prepare the share string.
            String shareString = currentWebView.getTitle() + " – " + currentWebView.getUrl();

            // Create the share intent.
            Intent shareMessageIntent = new Intent(Intent.ACTION_SEND);

            // Add the share string to the intent.
            shareMessageIntent.putExtra(Intent.EXTRA_TEXT, shareString);

            // Set the MIME type.
            shareMessageIntent.setType("text/plain");

            // Set the intent to open in a new task.
            shareMessageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Make it so.
            startActivity(Intent.createChooser(shareMessageIntent, getString(R.string.share_message)));

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.share_url) {  // Share URL.
            // Create the share intent.
            Intent shareUrlIntent = new Intent(Intent.ACTION_SEND);

            // Add the URL to the intent.
            shareUrlIntent.putExtra(Intent.EXTRA_TEXT, currentWebView.getUrl());

            // Add the title to the intent.
            shareUrlIntent.putExtra(Intent.EXTRA_SUBJECT, currentWebView.getTitle());

            // Set the MIME type.
            shareUrlIntent.setType("text/plain");

            // Set the intent to open in a new task.
            shareUrlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            //Make it so.
            startActivity(Intent.createChooser(shareUrlIntent, getString(R.string.share_url)));

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.open_with_app) {  // Open with app.
            // Open the URL with an outside app.
            openWithApp(currentWebView.getUrl());

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.open_with_browser) {  // Open with browser.
            // Open the URL with an outside browser.
            openWithBrowser(currentWebView.getUrl());

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.add_or_edit_domain) {  // Add or edit domain.
            // Reapply the domain settings on returning to `MainWebViewActivity`.
            reapplyDomainSettingsOnRestart = true;

            // Check if domain settings currently exist.
            if (currentWebView.getDomainSettingsApplied()) {  // Edit the current domain settings.
                // Create an intent to launch the domains activity.
                Intent domainsIntent = new Intent(this, DomainsActivity.class);

                // Add the extra information to the intent.
                domainsIntent.putExtra(DomainsActivity.LOAD_DOMAIN, currentWebView.getDomainSettingsDatabaseId());
                domainsIntent.putExtra(DomainsActivity.CLOSE_ON_BACK, true);
                domainsIntent.putExtra(DomainsActivity.CURRENT_URL, currentWebView.getUrl());
                domainsIntent.putExtra(DomainsActivity.CURRENT_IP_ADDRESSES, currentWebView.getCurrentIpAddresses());

                // Get the current certificate.
                SslCertificate sslCertificate = currentWebView.getCertificate();

                // Check to see if the SSL certificate is populated.
                if (sslCertificate != null) {
                    // Extract the certificate to strings.
                    String issuedToCName = sslCertificate.getIssuedTo().getCName();
                    String issuedToOName = sslCertificate.getIssuedTo().getOName();
                    String issuedToUName = sslCertificate.getIssuedTo().getUName();
                    String issuedByCName = sslCertificate.getIssuedBy().getCName();
                    String issuedByOName = sslCertificate.getIssuedBy().getOName();
                    String issuedByUName = sslCertificate.getIssuedBy().getUName();
                    long startDateLong = sslCertificate.getValidNotBeforeDate().getTime();
                    long endDateLong = sslCertificate.getValidNotAfterDate().getTime();

                    // Add the certificate to the intent.
                    domainsIntent.putExtra(DomainsActivity.SSL_ISSUED_TO_CNAME, issuedToCName);
                    domainsIntent.putExtra(DomainsActivity.SSL_ISSUED_TO_ONAME, issuedToOName);
                    domainsIntent.putExtra(DomainsActivity.SSL_ISSUED_TO_UNAME, issuedToUName);
                    domainsIntent.putExtra(DomainsActivity.SSL_ISSUED_BY_CNAME, issuedByCName);
                    domainsIntent.putExtra(DomainsActivity.SSL_ISSUED_BY_ONAME, issuedByOName);
                    domainsIntent.putExtra(DomainsActivity.SSL_ISSUED_BY_UNAME, issuedByUName);
                    domainsIntent.putExtra(DomainsActivity.SSL_START_DATE, startDateLong);
                    domainsIntent.putExtra(DomainsActivity.SSL_END_DATE, endDateLong);
                }

                // Make it so.
                startActivity(domainsIntent);
            } else {  // Add a new domain.
                // Get the current URI.
                Uri currentUri = Uri.parse(currentWebView.getUrl());

                // Get the current domain from the URI.
                String currentDomain = currentUri.getHost();

                // Set an empty domain if it is null.
                if (currentDomain == null)
                    currentDomain = "";

                // Create the domain and store the database ID.
                int newDomainDatabaseId = domainsDatabaseHelper.addDomain(currentDomain);

                // Create an intent to launch the domains activity.
                Intent domainsIntent = new Intent(this, DomainsActivity.class);

                // Add the extra information to the intent.
                domainsIntent.putExtra(DomainsActivity.LOAD_DOMAIN, newDomainDatabaseId);
                domainsIntent.putExtra(DomainsActivity.CLOSE_ON_BACK, true);
                domainsIntent.putExtra(DomainsActivity.CURRENT_URL, currentWebView.getUrl());
                domainsIntent.putExtra(DomainsActivity.CURRENT_IP_ADDRESSES, currentWebView.getCurrentIpAddresses());

                // Get the current certificate.
                SslCertificate sslCertificate = currentWebView.getCertificate();

                // Check to see if the SSL certificate is populated.
                if (sslCertificate != null) {
                    // Extract the certificate to strings.
                    String issuedToCName = sslCertificate.getIssuedTo().getCName();
                    String issuedToOName = sslCertificate.getIssuedTo().getOName();
                    String issuedToUName = sslCertificate.getIssuedTo().getUName();
                    String issuedByCName = sslCertificate.getIssuedBy().getCName();
                    String issuedByOName = sslCertificate.getIssuedBy().getOName();
                    String issuedByUName = sslCertificate.getIssuedBy().getUName();
                    long startDateLong = sslCertificate.getValidNotBeforeDate().getTime();
                    long endDateLong = sslCertificate.getValidNotAfterDate().getTime();

                    // Add the certificate to the intent.
                    domainsIntent.putExtra(DomainsActivity.SSL_ISSUED_TO_CNAME, issuedToCName);
                    domainsIntent.putExtra(DomainsActivity.SSL_ISSUED_TO_ONAME, issuedToOName);
                    domainsIntent.putExtra(DomainsActivity.SSL_ISSUED_TO_UNAME, issuedToUName);
                    domainsIntent.putExtra(DomainsActivity.SSL_ISSUED_BY_CNAME, issuedByCName);
                    domainsIntent.putExtra(DomainsActivity.SSL_ISSUED_BY_ONAME, issuedByOName);
                    domainsIntent.putExtra(DomainsActivity.SSL_ISSUED_BY_UNAME, issuedByUName);
                    domainsIntent.putExtra(DomainsActivity.SSL_START_DATE, startDateLong);
                    domainsIntent.putExtra(DomainsActivity.SSL_END_DATE, endDateLong);
                }

                // Make it so.
                startActivity(domainsIntent);
            }

            // Consume the event.
            return true;
        } else {  // There is no match with the options menu.  Pass the event up to the parent method.
            // Don't consume the event.
            return super.onOptionsItemSelected(menuItem);
        }
    }

    // removeAllCookies is deprecated, but it is required for API < 21.
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        // Get a handle for the shared preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Get the menu item ID.
        int menuItemId = menuItem.getItemId();

        // Run the commands that correspond to the selected menu item.
        if (menuItemId == R.id.clear_and_exit) {  // Clear and exit.
            // Clear and exit Privacy Browser.
            clearAndExit();
        } else if (menuItemId == R.id.home) {  // Home.
            // Load the homepage.
            loadUrl(currentWebView, sharedPreferences.getString("homepage", getString(R.string.homepage_default_value)));
        } else if (menuItemId == R.id.back) {  // Back.
            // Check if the WebView can go back.
            if (currentWebView.canGoBack()) {
                // Get the current web back forward list.
                WebBackForwardList webBackForwardList = currentWebView.copyBackForwardList();

                // Get the previous entry URL.
                String previousUrl = webBackForwardList.getItemAtIndex(webBackForwardList.getCurrentIndex() - 1).getUrl();

                // Apply the domain settings.
                applyDomainSettings(currentWebView, previousUrl, false, false, false);

                // Load the previous website in the history.
                currentWebView.goBack();
            }
        } else if (menuItemId == R.id.forward) {  // Forward.
            // Check if the WebView can go forward.
            if (currentWebView.canGoForward()) {
                // Get the current web back forward list.
                WebBackForwardList webBackForwardList = currentWebView.copyBackForwardList();

                // Get the next entry URL.
                String nextUrl = webBackForwardList.getItemAtIndex(webBackForwardList.getCurrentIndex() + 1).getUrl();

                // Apply the domain settings.
                applyDomainSettings(currentWebView, nextUrl, false, false, false);

                // Load the next website in the history.
                currentWebView.goForward();
            }
        } else if (menuItemId == R.id.history) {  // History.
            // Instantiate the URL history dialog.
            DialogFragment urlHistoryDialogFragment = UrlHistoryDialog.loadBackForwardList(currentWebView.getWebViewFragmentId());

            // Show the URL history dialog.
            urlHistoryDialogFragment.show(getSupportFragmentManager(), getString(R.string.history));
        } else if (menuItemId == R.id.open) {  // Open.
            // Instantiate the open file dialog.
            DialogFragment openDialogFragment = new OpenDialog();

            // Show the open file dialog.
            openDialogFragment.show(getSupportFragmentManager(), getString(R.string.open));
        } else if (menuItemId == R.id.requests) {  // Requests.
            // Populate the resource requests.
            RequestsActivity.resourceRequests = currentWebView.getResourceRequests();

            // Create an intent to launch the Requests activity.
            Intent requestsIntent = new Intent(this, RequestsActivity.class);

            // Add the block third-party requests status to the intent.
            requestsIntent.putExtra("block_all_third_party_requests", currentWebView.getBlockAllThirdPartyRequests());

            // Make it so.
            startActivity(requestsIntent);
        } else if (menuItemId == R.id.downloads) {  // Downloads.
            // Try the default system download manager.
            try {
                // Launch the default system Download Manager.
                Intent defaultDownloadManagerIntent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);

                // Launch as a new task so that the download manager and Privacy Browser show as separate windows in the recent tasks list.
                defaultDownloadManagerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                // Make it so.
                startActivity(defaultDownloadManagerIntent);
            } catch (Exception defaultDownloadManagerException) {
                // Try a generic file manager.
                try {
                    // Create a generic file manager intent.
                    Intent genericFileManagerIntent = new Intent(Intent.ACTION_VIEW);

                    // Open the download directory.
                    genericFileManagerIntent.setDataAndType(Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()), DocumentsContract.Document.MIME_TYPE_DIR);

                    // Launch as a new task so that the file manager and Privacy Browser show as separate windows in the recent tasks list.
                    genericFileManagerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    // Make it so.
                    startActivity(genericFileManagerIntent);
                } catch (Exception genericFileManagerException) {
                    // Try an alternate file manager.
                    try {
                        // Create an alternate file manager intent.
                        Intent alternateFileManagerIntent = new Intent(Intent.ACTION_VIEW);

                        // Open the download directory.
                        alternateFileManagerIntent.setDataAndType(Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()), "resource/folder");

                        // Launch as a new task so that the file manager and Privacy Browser show as separate windows in the recent tasks list.
                        alternateFileManagerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        // Open the alternate file manager.
                        startActivity(alternateFileManagerIntent);
                    } catch (Exception alternateFileManagerException) {
                        // Display a snackbar.
                        Snackbar.make(currentWebView, R.string.no_file_manager_detected, Snackbar.LENGTH_INDEFINITE).show();
                    }
                }
            }
        } else if (menuItemId == R.id.domains) {  // Domains.
            // Set the flag to reapply the domain settings on restart when returning from Domain Settings.
            reapplyDomainSettingsOnRestart = true;

            // Launch the domains activity.
            Intent domainsIntent = new Intent(this, DomainsActivity.class);

            // Add the extra information to the intent.
            domainsIntent.putExtra(DomainsActivity.CURRENT_URL, currentWebView.getUrl());
            domainsIntent.putExtra(DomainsActivity.CURRENT_IP_ADDRESSES, currentWebView.getCurrentIpAddresses());

            // Get the current certificate.
            SslCertificate sslCertificate = currentWebView.getCertificate();

            // Check to see if the SSL certificate is populated.
            if (sslCertificate != null) {
                // Extract the certificate to strings.
                String issuedToCName = sslCertificate.getIssuedTo().getCName();
                String issuedToOName = sslCertificate.getIssuedTo().getOName();
                String issuedToUName = sslCertificate.getIssuedTo().getUName();
                String issuedByCName = sslCertificate.getIssuedBy().getCName();
                String issuedByOName = sslCertificate.getIssuedBy().getOName();
                String issuedByUName = sslCertificate.getIssuedBy().getUName();
                long startDateLong = sslCertificate.getValidNotBeforeDate().getTime();
                long endDateLong = sslCertificate.getValidNotAfterDate().getTime();

                // Add the certificate to the intent.
                domainsIntent.putExtra("ssl_issued_to_cname", issuedToCName);
                domainsIntent.putExtra("ssl_issued_to_oname", issuedToOName);
                domainsIntent.putExtra("ssl_issued_to_uname", issuedToUName);
                domainsIntent.putExtra("ssl_issued_by_cname", issuedByCName);
                domainsIntent.putExtra("ssl_issued_by_oname", issuedByOName);
                domainsIntent.putExtra("ssl_issued_by_uname", issuedByUName);
                domainsIntent.putExtra("ssl_start_date", startDateLong);
                domainsIntent.putExtra("ssl_end_date", endDateLong);
            }

            // Make it so.
            startActivity(domainsIntent);
        } else if (menuItemId == R.id.settings) {  // Settings.
            // Set the flag to reapply app settings on restart when returning from Settings.
            reapplyAppSettingsOnRestart = true;

            // Set the flag to reapply the domain settings on restart when returning from Settings.
            reapplyDomainSettingsOnRestart = true;

            // Launch the settings activity.
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        } else if (menuItemId == R.id.import_export) { // Import/Export.
            // Create an intent to launch the import/export activity.
            Intent importExportIntent = new Intent(this, ImportExportActivity.class);

            // Make it so.
            startActivity(importExportIntent);
        } else if (menuItemId == R.id.logcat) {  // Logcat.
            // Create an intent to launch the logcat activity.
            Intent logcatIntent = new Intent(this, LogcatActivity.class);

            // Make it so.
            startActivity(logcatIntent);
        } else if (menuItemId == R.id.webview_devtools) {  // WebView Dev.
            // Create a WebView DevTools intent.
            Intent webViewDevToolsIntent = new Intent("com.android.webview.SHOW_DEV_UI");

            // Launch as a new task so that the WebView DevTools and Privacy Browser show as a separate windows in the recent tasks list.
            webViewDevToolsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Make it so.
            startActivity(webViewDevToolsIntent);
        } else if (menuItemId == R.id.guide) {  // Guide.
            // Create an intent to launch the guide activity.
            Intent guideIntent = new Intent(this, GuideActivity.class);

            // Make it so.
            startActivity(guideIntent);
        } else if (menuItemId == R.id.about) {  // About
            // Create an intent to launch the about activity.
            Intent aboutIntent = new Intent(this, AboutActivity.class);

            // Create a string array for the blocklist versions.
            String[] blocklistVersions = new String[]{easyList.get(0).get(0)[0], easyPrivacy.get(0).get(0)[0], fanboysAnnoyanceList.get(0).get(0)[0], fanboysSocialList.get(0).get(0)[0],
                    ultraList.get(0).get(0)[0], ultraPrivacy.get(0).get(0)[0]};

            // Add the blocklist versions to the intent.
            aboutIntent.putExtra(AboutActivity.BLOCKLIST_VERSIONS, blocklistVersions);

            // Make it so.
            startActivity(aboutIntent);
        }

        // Close the navigation drawer.
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        // Run the default commands.
        super.onPostCreate(savedInstanceState);

        // Sync the state of the DrawerToggle after the default `onRestoreInstanceState()` has finished.  This creates the navigation drawer icon.
        // If the app is restarting to change the app theme the action bar drawer toggle will not yet be populated.
        if (actionBarDrawerToggle != null)
            actionBarDrawerToggle.syncState();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        // Get the hit test result.
        final WebView.HitTestResult hitTestResult = currentWebView.getHitTestResult();

        // Define the URL strings.
        final String imageUrl;
        final String linkUrl;

        // Get handles for the system managers.
        final ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        // Remove the lint errors below that the clipboard manager might be null.
        assert clipboardManager != null;

        // Process the link according to the type.
        switch (hitTestResult.getType()) {
            // `SRC_ANCHOR_TYPE` is a link.
            case WebView.HitTestResult.SRC_ANCHOR_TYPE:
                // Get the target URL.
                linkUrl = hitTestResult.getExtra();

                // Set the target URL as the title of the `ContextMenu`.
                menu.setHeaderTitle(linkUrl);

                // Add an Open in New Tab entry.
                menu.add(R.string.open_in_new_tab).setOnMenuItemClickListener((MenuItem item) -> {
                    // Load the link URL in a new tab and move to it.
                    addNewTab(linkUrl, true);

                    // Consume the event.
                    return true;
                });

                // Add an Open in Background entry.
                menu.add(R.string.open_in_background).setOnMenuItemClickListener((MenuItem item) -> {
                    // Load the link URL in a new tab but do not move to it.
                    addNewTab(linkUrl, false);

                    // Consume the event.
                    return true;
                });

                // Add an Open with App entry.
                menu.add(R.string.open_with_app).setOnMenuItemClickListener((MenuItem item) -> {
                    openWithApp(linkUrl);

                    // Consume the event.
                    return true;
                });

                // Add an Open with Browser entry.
                menu.add(R.string.open_with_browser).setOnMenuItemClickListener((MenuItem item) -> {
                    openWithBrowser(linkUrl);

                    // Consume the event.
                    return true;
                });

                // Add a Copy URL entry.
                menu.add(R.string.copy_url).setOnMenuItemClickListener((MenuItem item) -> {
                    // Save the link URL in a `ClipData`.
                    ClipData srcAnchorTypeClipData = ClipData.newPlainText(getString(R.string.url), linkUrl);

                    // Set the `ClipData` as the clipboard's primary clip.
                    clipboardManager.setPrimaryClip(srcAnchorTypeClipData);

                    // Consume the event.
                    return true;
                });

                // Add a Save URL entry.
                menu.add(R.string.save_url).setOnMenuItemClickListener((MenuItem item) -> {
                    // Check the download preference.
                    if (downloadWithExternalApp) {  // Download with an external app.
                        downloadUrlWithExternalApp(linkUrl);
                    } else {  // Handle the download inside of Privacy Browser.
                        // Prepare the save dialog.  The dialog will be displayed once the file size and the content disposition have been acquired.
                        PrepareSaveDialogCoroutine.prepareSaveDialog(this, getSupportFragmentManager(), linkUrl, currentWebView.getSettings().getUserAgentString(), currentWebView.getAcceptCookies());
                    }

                    // Consume the event.
                    return true;
                });

                // Add an empty Cancel entry, which by default closes the context menu.
                menu.add(R.string.cancel);
                break;

            // `IMAGE_TYPE` is an image.
            case WebView.HitTestResult.IMAGE_TYPE:
                // Get the image URL.
                imageUrl = hitTestResult.getExtra();

                // Remove the incorrect lint warning below that the image URL might be null.
                assert imageUrl != null;

                // Set the context menu title.
                if (imageUrl.startsWith("data:")) {  // The image data is contained in within the URL, making it exceedingly long.
                    // Truncate the image URL before making it the title.
                    menu.setHeaderTitle(imageUrl.substring(0, 100));
                } else {  // The image URL does not contain the full image data.
                    // Set the image URL as the title of the context menu.
                    menu.setHeaderTitle(imageUrl);
                }

                // Add an Open in New Tab entry.
                menu.add(R.string.open_image_in_new_tab).setOnMenuItemClickListener((MenuItem item) -> {
                    // Load the image in a new tab.
                    addNewTab(imageUrl, true);

                    // Consume the event.
                    return true;
                });

                // Add an Open with App entry.
                menu.add(R.string.open_with_app).setOnMenuItemClickListener((MenuItem item) -> {
                    // Open the image URL with an external app.
                    openWithApp(imageUrl);

                    // Consume the event.
                    return true;
                });

                // Add an Open with Browser entry.
                menu.add(R.string.open_with_browser).setOnMenuItemClickListener((MenuItem item) -> {
                    // Open the image URL with an external browser.
                    openWithBrowser(imageUrl);

                    // Consume the event.
                    return true;
                });

                // Add a View Image entry.
                menu.add(R.string.view_image).setOnMenuItemClickListener(item -> {
                    // Load the image in the current tab.
                    loadUrl(currentWebView, imageUrl);

                    // Consume the event.
                    return true;
                });

                // Add a Save Image entry.
                menu.add(R.string.save_image).setOnMenuItemClickListener((MenuItem item) -> {
                    // Check the download preference.
                    if (downloadWithExternalApp) {  // Download with an external app.
                        downloadUrlWithExternalApp(imageUrl);
                    } else {  // Handle the download inside of Privacy Browser.
                        // Prepare the save dialog.  The dialog will be displayed once the file size and the content disposition have been acquired.
                        PrepareSaveDialogCoroutine.prepareSaveDialog(this, getSupportFragmentManager(), imageUrl, currentWebView.getSettings().getUserAgentString(), currentWebView.getAcceptCookies());
                    }

                    // Consume the event.
                    return true;
                });

                // Add a Copy URL entry.
                menu.add(R.string.copy_url).setOnMenuItemClickListener((MenuItem item) -> {
                    // Save the image URL in a clip data.
                    ClipData imageTypeClipData = ClipData.newPlainText(getString(R.string.url), imageUrl);

                    // Set the clip data as the clipboard's primary clip.
                    clipboardManager.setPrimaryClip(imageTypeClipData);

                    // Consume the event.
                    return true;
                });

                // Add an empty Cancel entry, which by default closes the context menu.
                menu.add(R.string.cancel);
                break;

            // `SRC_IMAGE_ANCHOR_TYPE` is an image that is also a link.
            case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
                // Get the image URL.
                imageUrl = hitTestResult.getExtra();

                // Instantiate a handler.
                Handler handler = new Handler();

                // Get a message from the handler.
                Message message = handler.obtainMessage();

                // Request the image details from the last touched node be returned in the message.
                currentWebView.requestFocusNodeHref(message);

                // Get the link URL from the message data.
                linkUrl = message.getData().getString("url");

                // Set the link URL as the title of the context menu.
                menu.setHeaderTitle(linkUrl);

                // Add an Open in New Tab entry.
                menu.add(R.string.open_in_new_tab).setOnMenuItemClickListener((MenuItem item) -> {
                    // Load the link URL in a new tab and move to it.
                    addNewTab(linkUrl, true);

                    // Consume the event.
                    return true;
                });

                // Add an Open in Background entry.
                menu.add(R.string.open_in_background).setOnMenuItemClickListener((MenuItem item) -> {
                    // Lod the link URL in a new tab but do not move to it.
                    addNewTab(linkUrl, false);

                    // Consume the event.
                    return true;
                });

                // Add an Open Image in New Tab entry.
                menu.add(R.string.open_image_in_new_tab).setOnMenuItemClickListener((MenuItem item) -> {
                    // Load the image in a new tab and move to it.
                    addNewTab(imageUrl, true);

                    // Consume the event.
                    return true;
                });

                // Add an Open with App entry.
                menu.add(R.string.open_with_app).setOnMenuItemClickListener((MenuItem item) -> {
                    // Open the link URL with an external app.
                    openWithApp(linkUrl);

                    // Consume the event.
                    return true;
                });

                // Add an Open with Browser entry.
                menu.add(R.string.open_with_browser).setOnMenuItemClickListener((MenuItem item) -> {
                    // Open the link URL with an external browser.
                    openWithBrowser(linkUrl);

                    // Consume the event.
                    return true;
                });

                // Add a View Image entry.
                menu.add(R.string.view_image).setOnMenuItemClickListener((MenuItem item) -> {
                   // View the image in the current tab.
                   loadUrl(currentWebView, imageUrl);

                   // Consume the event.
                   return true;
                });

                // Add a Save Image entry.
                menu.add(R.string.save_image).setOnMenuItemClickListener((MenuItem item) -> {
                    // Check the download preference.
                    if (downloadWithExternalApp) {  // Download with an external app.
                        downloadUrlWithExternalApp(imageUrl);
                    } else {  // Handle the download inside of Privacy Browser.
                        // Prepare the save dialog.  The dialog will be displayed once the file size and the content disposition have been acquired.
                        PrepareSaveDialogCoroutine.prepareSaveDialog(this, getSupportFragmentManager(), imageUrl, currentWebView.getSettings().getUserAgentString(), currentWebView.getAcceptCookies());
                    }

                    // Consume the event.
                    return true;
                });

                // Add a Copy URL entry.
                menu.add(R.string.copy_url).setOnMenuItemClickListener((MenuItem item) -> {
                    // Save the link URL in a clip data.
                    ClipData srcImageAnchorTypeClipData = ClipData.newPlainText(getString(R.string.url), linkUrl);

                    // Set the clip data as the clipboard's primary clip.
                    clipboardManager.setPrimaryClip(srcImageAnchorTypeClipData);

                    // Consume the event.
                    return true;
                });

                // Add a Save URL entry.
                menu.add(R.string.save_url).setOnMenuItemClickListener((MenuItem item) -> {
                    // Check the download preference.
                    if (downloadWithExternalApp) {  // Download with an external app.
                        downloadUrlWithExternalApp(linkUrl);
                    } else {  // Handle the download inside of Privacy Browser.
                        // Prepare the save dialog.  The dialog will be displayed once the file size and the content disposition have been acquired.
                        PrepareSaveDialogCoroutine.prepareSaveDialog(this, getSupportFragmentManager(), linkUrl, currentWebView.getSettings().getUserAgentString(), currentWebView.getAcceptCookies());
                    }

                    // Consume the event.
                    return true;
                });

                // Add an empty Cancel entry, which by default closes the context menu.
                menu.add(R.string.cancel);
                break;

            case WebView.HitTestResult.EMAIL_TYPE:
                // Get the target URL.
                linkUrl = hitTestResult.getExtra();

                // Set the target URL as the title of the `ContextMenu`.
                menu.setHeaderTitle(linkUrl);

                // Add a Write Email entry.
                menu.add(R.string.write_email).setOnMenuItemClickListener(item -> {
                    // Use `ACTION_SENDTO` instead of `ACTION_SEND` so that only email programs are launched.
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);

                    // Parse the url and set it as the data for the `Intent`.
                    emailIntent.setData(Uri.parse("mailto:" + linkUrl));

                    // `FLAG_ACTIVITY_NEW_TASK` opens the email program in a new task instead as part of Privacy Browser.
                    emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    try {
                        // Make it so.
                        startActivity(emailIntent);
                    } catch (ActivityNotFoundException exception) {
                        // Display a snackbar.
                        Snackbar.make(currentWebView, getString(R.string.error) + "  " + exception, Snackbar.LENGTH_INDEFINITE).show();
                    }

                    // Consume the event.
                    return true;
                });

                // Add a Copy Email Address entry.
                menu.add(R.string.copy_email_address).setOnMenuItemClickListener(item -> {
                    // Save the email address in a `ClipData`.
                    ClipData srcEmailTypeClipData = ClipData.newPlainText(getString(R.string.email_address), linkUrl);

                    // Set the `ClipData` as the clipboard's primary clip.
                    clipboardManager.setPrimaryClip(srcEmailTypeClipData);

                    // Consume the event.
                    return true;
                });

                // Add an empty Cancel entry, which by default closes the context menu.
                menu.add(R.string.cancel);
                break;
        }
    }

    @Override
    public void onCreateBookmark(DialogFragment dialogFragment, Bitmap favoriteIconBitmap) {
        // Get a handle for the bookmarks list view.
        ListView bookmarksListView = findViewById(R.id.bookmarks_drawer_listview);

        // Get the dialog.
        Dialog dialog = dialogFragment.getDialog();

        // Remove the incorrect lint warning below that the dialog might be null.
        assert dialog != null;

        // Get the views from the dialog fragment.
        EditText createBookmarkNameEditText = dialog.findViewById(R.id.create_bookmark_name_edittext);
        EditText createBookmarkUrlEditText = dialog.findViewById(R.id.create_bookmark_url_edittext);

        // Extract the strings from the edit texts.
        String bookmarkNameString = createBookmarkNameEditText.getText().toString();
        String bookmarkUrlString = createBookmarkUrlEditText.getText().toString();

        // Create a favorite icon byte array output stream.
        ByteArrayOutputStream favoriteIconByteArrayOutputStream = new ByteArrayOutputStream();

        // Convert the favorite icon bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
        favoriteIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, favoriteIconByteArrayOutputStream);

        // Convert the favorite icon byte array stream to a byte array.
        byte[] favoriteIconByteArray = favoriteIconByteArrayOutputStream.toByteArray();

        // Display the new bookmark below the current items in the (0 indexed) list.
        int newBookmarkDisplayOrder = bookmarksListView.getCount();

        // Create the bookmark.
        bookmarksDatabaseHelper.createBookmark(bookmarkNameString, bookmarkUrlString, currentBookmarksFolder, newBookmarkDisplayOrder, favoriteIconByteArray);

        // Update the bookmarks cursor with the current contents of this folder.
        bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentBookmarksFolder);

        // Update the list view.
        bookmarksCursorAdapter.changeCursor(bookmarksCursor);

        // Scroll to the new bookmark.
        bookmarksListView.setSelection(newBookmarkDisplayOrder);
    }

    @Override
    public void onCreateBookmarkFolder(DialogFragment dialogFragment, @NonNull Bitmap favoriteIconBitmap) {
        // Get a handle for the bookmarks list view.
        ListView bookmarksListView = findViewById(R.id.bookmarks_drawer_listview);

        // Get the dialog.
        Dialog dialog = dialogFragment.getDialog();

        // Remove the incorrect lint warning below that the dialog might be null.
        assert dialog != null;

        // Get handles for the views in the dialog fragment.
        EditText folderNameEditText = dialog.findViewById(R.id.folder_name_edittext);
        RadioButton defaultIconRadioButton = dialog.findViewById(R.id.default_icon_radiobutton);
        ImageView defaultIconImageView = dialog.findViewById(R.id.default_icon_imageview);

        // Get new folder name string.
        String folderNameString = folderNameEditText.getText().toString();

        // Create a folder icon bitmap.
        Bitmap folderIconBitmap;

        // Set the folder icon bitmap according to the dialog.
        if (defaultIconRadioButton.isChecked()) {  // Use the default folder icon.
            // Get the default folder icon drawable.
            Drawable folderIconDrawable = defaultIconImageView.getDrawable();

            // Convert the folder icon drawable to a bitmap drawable.
            BitmapDrawable folderIconBitmapDrawable = (BitmapDrawable) folderIconDrawable;

            // Convert the folder icon bitmap drawable to a bitmap.
            folderIconBitmap = folderIconBitmapDrawable.getBitmap();
        } else {  // Use the WebView favorite icon.
            // Copy the favorite icon bitmap to the folder icon bitmap.
            folderIconBitmap = favoriteIconBitmap;
        }

        // Create a folder icon byte array output stream.
        ByteArrayOutputStream folderIconByteArrayOutputStream = new ByteArrayOutputStream();

        // Convert the folder icon bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
        folderIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, folderIconByteArrayOutputStream);

        // Convert the folder icon byte array stream to a byte array.
        byte[] folderIconByteArray = folderIconByteArrayOutputStream.toByteArray();

        // Move all the bookmarks down one in the display order.
        for (int i = 0; i < bookmarksListView.getCount(); i++) {
            int databaseId = (int) bookmarksListView.getItemIdAtPosition(i);
            bookmarksDatabaseHelper.updateDisplayOrder(databaseId, i + 1);
        }

        // Create the folder, which will be placed at the top of the `ListView`.
        bookmarksDatabaseHelper.createFolder(folderNameString, currentBookmarksFolder, folderIconByteArray);

        // Update the bookmarks cursor with the current contents of this folder.
        bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentBookmarksFolder);

        // Update the list view.
        bookmarksCursorAdapter.changeCursor(bookmarksCursor);

        // Scroll to the new folder.
        bookmarksListView.setSelection(0);
    }

    private void loadUrlFromTextBox() {
        // Get the text from urlTextBox and convert it to a string.  trim() removes white spaces from the beginning and end of the string.
        String unformattedUrlString = urlEditText.getText().toString().trim();

        // Initialize the formatted URL string.
        String url = "";

        // Check to see if the unformatted URL string is a valid URL.  Otherwise, convert it into a search.
        if (unformattedUrlString.startsWith("content://")) {  // This is a Content URL.
            // Load the entire content URL.
            url = unformattedUrlString;
        } else if (Patterns.WEB_URL.matcher(unformattedUrlString).matches() || unformattedUrlString.startsWith("http://") || unformattedUrlString.startsWith("https://") ||
                unformattedUrlString.startsWith("file://")) {  // This is a standard URL.
            // Add `https://` at the beginning if there is no protocol.  Otherwise the app will segfault.
            if (!unformattedUrlString.startsWith("http") && !unformattedUrlString.startsWith("file://")) {
                unformattedUrlString = "https://" + unformattedUrlString;
            }

            // Initialize the unformatted URL.
            URL unformattedUrl = null;

            // Convert the unformatted URL string to a URL, then to a URI, and then back to a string, which sanitizes the input and adds in any missing components.
            try {
                unformattedUrl = new URL(unformattedUrlString);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            // The ternary operator (? :) makes sure that a null pointer exception is not thrown, which would happen if `.get` was called on a `null` value.
            String scheme = unformattedUrl != null ? unformattedUrl.getProtocol() : null;
            String authority = unformattedUrl != null ? unformattedUrl.getAuthority() : null;
            String path = unformattedUrl != null ? unformattedUrl.getPath() : null;
            String query = unformattedUrl != null ? unformattedUrl.getQuery() : null;
            String fragment = unformattedUrl != null ? unformattedUrl.getRef() : null;

            // Build the URI.
            Uri.Builder uri = new Uri.Builder();
            uri.scheme(scheme).authority(authority).path(path).query(query).fragment(fragment);

            // Decode the URI as a UTF-8 string in.
            try {
                url = URLDecoder.decode(uri.build().toString(), "UTF-8");
            } catch (UnsupportedEncodingException exception) {
                // Do nothing.  The formatted URL string will remain blank.
            }
        } else if (!unformattedUrlString.isEmpty()){  // This is not a URL, but rather a search string.
            // Create an encoded URL String.
            String encodedUrlString;

            // Sanitize the search input.
            try {
                encodedUrlString = URLEncoder.encode(unformattedUrlString, "UTF-8");
            } catch (UnsupportedEncodingException exception) {
                encodedUrlString = "";
            }

            // Add the base search URL.
            url = searchURL + encodedUrlString;
        }

        // Clear the focus from the URL edit text.  Otherwise, proximate typing in the box will retain the colorized formatting instead of being reset during refocus.
        urlEditText.clearFocus();

        // Make it so.
        loadUrl(currentWebView, url);
    }

    private void loadUrl(NestedScrollWebView nestedScrollWebView, String url) {
        // Sanitize the URL.
        url = sanitizeUrl(url);

        // Apply the domain settings and load the URL.
        applyDomainSettings(nestedScrollWebView, url, true, false, true);
    }

    public void findPreviousOnPage(View view) {
        // Go to the previous highlighted phrase on the page.  `false` goes backwards instead of forwards.
        currentWebView.findNext(false);
    }

    public void findNextOnPage(View view) {
        // Go to the next highlighted phrase on the page. `true` goes forwards instead of backwards.
        currentWebView.findNext(true);
    }

    public void closeFindOnPage(View view) {
        // Get a handle for the views.
        Toolbar toolbar = findViewById(R.id.toolbar);
        LinearLayout findOnPageLinearLayout = findViewById(R.id.find_on_page_linearlayout);
        EditText findOnPageEditText = findViewById(R.id.find_on_page_edittext);

        // Delete the contents of `find_on_page_edittext`.
        findOnPageEditText.setText(null);

        // Clear the highlighted phrases if the WebView is not null.
        if (currentWebView != null) {
            currentWebView.clearMatches();
        }

        // Hide the find on page linear layout.
        findOnPageLinearLayout.setVisibility(View.GONE);

        // Show the toolbar.
        toolbar.setVisibility(View.VISIBLE);

        // Get a handle for the input method manager.
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // Remove the lint warning below that the input method manager might be null.
        assert inputMethodManager != null;

        // Hide the keyboard.
        inputMethodManager.hideSoftInputFromWindow(toolbar.getWindowToken(), 0);
    }

    @Override
    public void onApplyNewFontSize(DialogFragment dialogFragment) {
        // Remove the incorrect lint warning below that the dialog fragment might be null.
        assert dialogFragment != null;

        // Get the dialog.
        Dialog dialog = dialogFragment.getDialog();

        // Remove the incorrect lint warning below tha the dialog might be null.
        assert dialog != null;

        // Get a handle for the font size edit text.
        EditText fontSizeEditText = dialog.findViewById(R.id.font_size_edittext);

        // Initialize the new font size variable with the current font size.
        int newFontSize = currentWebView.getSettings().getTextZoom();

        // Get the font size from the edit text.
        try {
            newFontSize = Integer.parseInt(fontSizeEditText.getText().toString());
        } catch (Exception exception) {
            // If the edit text does not contain a valid font size do nothing.
        }

        // Apply the new font size.
        currentWebView.getSettings().setTextZoom(newFontSize);
    }

    @Override
    public void onOpen(DialogFragment dialogFragment) {
        // Get the dialog.
        Dialog dialog = dialogFragment.getDialog();

        // Remove the incorrect lint warning below that the dialog might be null.
        assert dialog != null;

        // Get handles for the views.
        EditText fileNameEditText = dialog.findViewById(R.id.file_name_edittext);
        CheckBox mhtCheckBox = dialog.findViewById(R.id.mht_checkbox);

        // Get the file path string.
        String openFilePath = fileNameEditText.getText().toString();

        // Apply the domain settings.  This resets the favorite icon and removes any domain settings.
        applyDomainSettings(currentWebView, openFilePath, true, false, false);

        // Open the file according to the type.
        if (mhtCheckBox.isChecked()) {  // Force opening of an MHT file.
            try {
                // Get the MHT file input stream.
                InputStream mhtFileInputStream = getContentResolver().openInputStream(Uri.parse(openFilePath));

                // Create a temporary MHT file.
                File temporaryMhtFile = File.createTempFile("temporary_mht_file", ".mht", getCacheDir());

                // Get a file output stream for the temporary MHT file.
                FileOutputStream temporaryMhtFileOutputStream = new FileOutputStream(temporaryMhtFile);

                // Create a transfer byte array.
                byte[] transferByteArray = new byte[1024];

                // Create an integer to track the number of bytes read.
                int bytesRead;

                // Copy the temporary MHT file input stream to the MHT output stream.
                while ((bytesRead = mhtFileInputStream.read(transferByteArray)) > 0) {
                    temporaryMhtFileOutputStream.write(transferByteArray, 0, bytesRead);
                }

                // Flush the temporary MHT file output stream.
                temporaryMhtFileOutputStream.flush();

                // Close the streams.
                temporaryMhtFileOutputStream.close();
                mhtFileInputStream.close();

                // Load the temporary MHT file.
                currentWebView.loadUrl(temporaryMhtFile.toString());
            } catch (Exception exception) {
                // Display a snackbar.
                Snackbar.make(currentWebView, getString(R.string.error) + "  " + exception, Snackbar.LENGTH_INDEFINITE).show();
            }
        } else {  // Let the WebView handle opening of the file.
            // Open the file.
            currentWebView.loadUrl(openFilePath);
        }
    }

    private void downloadUrlWithExternalApp(String url) {
        // Create a download intent.  Not specifying the action type will display the maximum number of options.
        Intent downloadIntent = new Intent();

        // Set the URI and the mime type.
        downloadIntent.setDataAndType(Uri.parse(url), "text/html");

        // Flag the intent to open in a new task.
        downloadIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Show the chooser.
        startActivity(Intent.createChooser(downloadIntent, getString(R.string.download_with_external_app)));
    }

    public void onSaveUrl(@NonNull String originalUrlString, @NonNull String fileNameString, @NonNull DialogFragment dialogFragment) {
        // Store the URL.  This will be used in the save URL activity result launcher.
        if (originalUrlString.startsWith("data:")) {
            // Save the original URL.
            saveUrlString = originalUrlString;
        } else {
            // Get the dialog.
            Dialog dialog = dialogFragment.getDialog();

            // Remove the incorrect lint warning below that the dialog might be null.
            assert dialog != null;

            // Get a handle for the dialog URL edit text.
            EditText dialogUrlEditText = dialog.findViewById(R.id.url_edittext);

            // Get the URL from the edit text, which may have been modified.
            saveUrlString = dialogUrlEditText.getText().toString();
        }

        // Open the file picker.
        saveUrlActivityResultLauncher.launch(fileNameString);
    }
    
    // Remove the warning that `OnTouchListener()` needs to override `performClick()`, as the only purpose of setting the `OnTouchListener()` is to make it do nothing.
    @SuppressLint("ClickableViewAccessibility")
    private void initializeApp() {
        // Get a handle for the input method.
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // Remove the lint warning below that the input method manager might be null.
        assert inputMethodManager != null;

        // Initialize the color spans for highlighting the URLs.
        initialGrayColorSpan = new ForegroundColorSpan(getColor(R.color.gray_500));
        finalGrayColorSpan = new ForegroundColorSpan(getColor(R.color.gray_500));
        redColorSpan = new ForegroundColorSpan(getColor(R.color.red_text));

        // Remove the formatting from the URL edit text when the user is editing the text.
        urlEditText.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if (hasFocus) {  // The user is editing the URL text box.
                // Remove the syntax highlighting.
                urlEditText.getText().removeSpan(redColorSpan);
                urlEditText.getText().removeSpan(initialGrayColorSpan);
                urlEditText.getText().removeSpan(finalGrayColorSpan);
            } else {  // The user has stopped editing the URL text box.
                // Move to the beginning of the string.
                urlEditText.setSelection(0);

                // Reapply the syntax highlighting.
                UrlHelper.highlightSyntax(urlEditText, initialGrayColorSpan, finalGrayColorSpan, redColorSpan);
            }
        });

        // Set the go button on the keyboard to load the URL in `urlTextBox`.
        urlEditText.setOnKeyListener((View v, int keyCode, KeyEvent event) -> {
            // If the event is a key-down event on the `enter` button, load the URL.
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                // Load the URL into the mainWebView and consume the event.
                loadUrlFromTextBox();

                // If the enter key was pressed, consume the event.
                return true;
            } else {
                // If any other key was pressed, do not consume the event.
                return false;
            }
        });

        // Create an Orbot status broadcast receiver.
        orbotStatusBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Store the content of the status message in `orbotStatus`.
                orbotStatus = intent.getStringExtra("org.torproject.android.intent.extra.STATUS");

                // If Privacy Browser is waiting on the proxy, load the website now that Orbot is connected.
                if ((orbotStatus != null) && orbotStatus.equals(ProxyHelper.ORBOT_STATUS_ON) && waitingForProxy) {
                    // Reset the waiting for proxy status.
                    waitingForProxy = false;

                    // Get a list of the current fragments.
                    List<Fragment> fragmentList = getSupportFragmentManager().getFragments();

                    // Check each fragment to see if it is a waiting for proxy dialog.  Sometimes more than one is displayed.
                    for (int i = 0; i < fragmentList.size(); i++) {
                        // Get the fragment tag.
                        String fragmentTag = fragmentList.get(i).getTag();

                        // Check to see if it is the waiting for proxy dialog.
                        if ((fragmentTag!= null) && fragmentTag.equals(getString(R.string.waiting_for_proxy_dialog))) {
                            // Dismiss the waiting for proxy dialog.
                            ((DialogFragment) fragmentList.get(i)).dismiss();
                        }
                    }

                    // Reload existing URLs and load any URLs that are waiting for the proxy.
                    for (int i = 0; i < webViewPagerAdapter.getCount(); i++) {
                        // Get the WebView tab fragment.
                        WebViewTabFragment webViewTabFragment = webViewPagerAdapter.getPageFragment(i);

                        // Get the fragment view.
                        View fragmentView = webViewTabFragment.getView();

                        // Only process the WebViews if they exist.
                        if (fragmentView != null) {
                            // Get the nested scroll WebView from the tab fragment.
                            NestedScrollWebView nestedScrollWebView = fragmentView.findViewById(R.id.nestedscroll_webview);

                            // Get the waiting for proxy URL string.
                            String waitingForProxyUrlString = nestedScrollWebView.getWaitingForProxyUrlString();

                            // Load the pending URL if it exists.
                            if (!waitingForProxyUrlString.isEmpty()) {  // A URL is waiting to be loaded.
                                // Load the URL.
                                loadUrl(nestedScrollWebView, waitingForProxyUrlString);

                                // Reset the waiting for proxy URL string.
                                nestedScrollWebView.setWaitingForProxyUrlString("");
                            } else {  // No URL is waiting to be loaded.
                                // Reload the existing URL.
                                nestedScrollWebView.reload();
                            }
                        }
                    }
                }
            }
        };

        // Register the Orbot status broadcast receiver on `this` context.
        this.registerReceiver(orbotStatusBroadcastReceiver, new IntentFilter("org.torproject.android.intent.action.STATUS"));

        // Get handles for views that need to be modified.
        LinearLayout bookmarksHeaderLinearLayout = findViewById(R.id.bookmarks_header_linearlayout);
        ListView bookmarksListView = findViewById(R.id.bookmarks_drawer_listview);
        FloatingActionButton launchBookmarksActivityFab = findViewById(R.id.launch_bookmarks_activity_fab);
        FloatingActionButton createBookmarkFolderFab = findViewById(R.id.create_bookmark_folder_fab);
        FloatingActionButton createBookmarkFab = findViewById(R.id.create_bookmark_fab);
        EditText findOnPageEditText = findViewById(R.id.find_on_page_edittext);

        // Update the web view pager every time a tab is modified.
        webViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Do nothing.
            }

            @Override
            public void onPageSelected(int position) {
                // Close the find on page bar if it is open.
                closeFindOnPage(null);

                // Set the current WebView.
                setCurrentWebView(position);

                // Select the corresponding tab if it does not match the currently selected page.  This will happen if the page was scrolled by creating a new tab.
                if (tabLayout.getSelectedTabPosition() != position) {
                    // Wait until the new tab has been created.
                    tabLayout.post(() -> {
                        // Get a handle for the tab.
                        TabLayout.Tab tab = tabLayout.getTabAt(position);

                        // Assert that the tab is not null.
                        assert tab != null;

                        // Select the tab.
                        tab.select();
                    });
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // Do nothing.
            }
        });

        // Display the View SSL Certificate dialog when the currently selected tab is reselected.
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Select the same page in the view pager.
                webViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Do nothing.
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Instantiate the View SSL Certificate dialog.
                DialogFragment viewSslCertificateDialogFragment = ViewSslCertificateDialog.displayDialog(currentWebView.getWebViewFragmentId(), currentWebView.getFavoriteIcon());

                // Display the View SSL Certificate dialog.
                viewSslCertificateDialogFragment.show(getSupportFragmentManager(), getString(R.string.view_ssl_certificate));
            }
        });

        // Set a touch listener on the bookmarks header linear layout so that touches don't pass through to the button underneath.
        bookmarksHeaderLinearLayout.setOnTouchListener((view, motionEvent) -> {
            // Consume the touch.
            return true;
        });

        // Set the launch bookmarks activity FAB to launch the bookmarks activity.
        launchBookmarksActivityFab.setOnClickListener(v -> {
            // Get a copy of the favorite icon bitmap.
            Bitmap favoriteIconBitmap = currentWebView.getFavoriteIcon();

            // Create a favorite icon byte array output stream.
            ByteArrayOutputStream favoriteIconByteArrayOutputStream = new ByteArrayOutputStream();

            // Convert the favorite icon bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
            favoriteIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, favoriteIconByteArrayOutputStream);

            // Convert the favorite icon byte array stream to a byte array.
            byte[] favoriteIconByteArray = favoriteIconByteArrayOutputStream.toByteArray();

            // Create an intent to launch the bookmarks activity.
            Intent bookmarksIntent = new Intent(getApplicationContext(), BookmarksActivity.class);

            // Add the extra information to the intent.
            bookmarksIntent.putExtra("current_url", currentWebView.getUrl());
            bookmarksIntent.putExtra("current_title", currentWebView.getTitle());
            bookmarksIntent.putExtra("current_folder", currentBookmarksFolder);
            bookmarksIntent.putExtra("favorite_icon_byte_array", favoriteIconByteArray);

            // Make it so.
            startActivity(bookmarksIntent);
        });

        // Set the create new bookmark folder FAB to display an alert dialog.
        createBookmarkFolderFab.setOnClickListener(v -> {
            // Create a create bookmark folder dialog.
            DialogFragment createBookmarkFolderDialog = CreateBookmarkFolderDialog.createBookmarkFolder(currentWebView.getFavoriteIcon());

            // Show the create bookmark folder dialog.
            createBookmarkFolderDialog.show(getSupportFragmentManager(), getString(R.string.create_folder));
        });

        // Set the create new bookmark FAB to display an alert dialog.
        createBookmarkFab.setOnClickListener(view -> {
            // Instantiate the create bookmark dialog.
            DialogFragment createBookmarkDialog = CreateBookmarkDialog.createBookmark(currentWebView.getUrl(), currentWebView.getTitle(), currentWebView.getFavoriteIcon());

            // Display the create bookmark dialog.
            createBookmarkDialog.show(getSupportFragmentManager(), getString(R.string.create_bookmark));
        });

        // Search for the string on the page whenever a character changes in the `findOnPageEditText`.
        findOnPageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing.
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Search for the text in the WebView if it is not null.  Sometimes on resume after a period of non-use the WebView will be null.
                if (currentWebView != null) {
                    currentWebView.findAllAsync(findOnPageEditText.getText().toString());
                }
            }
        });

        // Set the `check mark` button for the `findOnPageEditText` keyboard to close the soft keyboard.
        findOnPageEditText.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {  // The `enter` key was pressed.
                // Hide the soft keyboard.
                inputMethodManager.hideSoftInputFromWindow(currentWebView.getWindowToken(), 0);

                // Consume the event.
                return true;
            } else {  // A different key was pressed.
                // Do not consume the event.
                return false;
            }
        });

        // Implement swipe to refresh.
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Reload the website.
            currentWebView.reload();
        });

        // Store the default progress view offsets for use later in `initializeWebView()`.
        defaultProgressViewStartOffset = swipeRefreshLayout.getProgressViewStartOffset();
        defaultProgressViewEndOffset = swipeRefreshLayout.getProgressViewEndOffset();

        // Set the refresh color scheme according to the theme.
        swipeRefreshLayout.setColorSchemeResources(R.color.blue_text);

        // Initialize a color background typed value.
        TypedValue colorBackgroundTypedValue = new TypedValue();

        // Get the color background from the theme.
        getTheme().resolveAttribute(android.R.attr.colorBackground, colorBackgroundTypedValue, true);

        // Get the color background int from the typed value.
        int colorBackgroundInt = colorBackgroundTypedValue.data;

        // Set the swipe refresh background color.
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(colorBackgroundInt);

        // The drawer titles identify the drawer layouts in accessibility mode.
        drawerLayout.setDrawerTitle(GravityCompat.START, getString(R.string.navigation_drawer));
        drawerLayout.setDrawerTitle(GravityCompat.END, getString(R.string.bookmarks));

        // Load the bookmarks folder.
        loadBookmarksFolder();

        // Handle clicks on bookmarks.
        bookmarksListView.setOnItemClickListener((parent, view, position, id) -> {
            // Convert the id from long to int to match the format of the bookmarks database.
            int databaseId = (int) id;

            // Get the bookmark cursor for this ID.
            Cursor bookmarkCursor = bookmarksDatabaseHelper.getBookmark(databaseId);

            // Move the bookmark cursor to the first row.
            bookmarkCursor.moveToFirst();

            // Act upon the bookmark according to the type.
            if (bookmarkCursor.getInt(bookmarkCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.IS_FOLDER)) == 1) {  // The selected bookmark is a folder.
                // Store the new folder name in `currentBookmarksFolder`.
                currentBookmarksFolder = bookmarkCursor.getString(bookmarkCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_NAME));

                // Load the new folder.
                loadBookmarksFolder();
            } else {  // The selected bookmark is not a folder.
                // Load the bookmark URL.
                loadUrl(currentWebView, bookmarkCursor.getString(bookmarkCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_URL)));

                // Close the bookmarks drawer if it is not pinned.
                if (!bookmarksDrawerPinned)
                    drawerLayout.closeDrawer(GravityCompat.END);
            }

            // Close the cursor.
            bookmarkCursor.close();
        });

        // Handle long-presses on bookmarks.
        bookmarksListView.setOnItemLongClickListener((parent, view, position, id) -> {
            // Convert the database ID from `long` to `int`.
            int databaseId = (int) id;

            // Find out if the selected bookmark is a folder.
            boolean isFolder = bookmarksDatabaseHelper.isFolder(databaseId);

            // Check to see if the bookmark is a folder.
            if (isFolder) {  // The bookmark is a folder.
                // Get a cursor of all the bookmarks in the folder.
                Cursor bookmarksCursor = bookmarksDatabaseHelper.getFolderBookmarks(databaseId);

                // Move to the first entry in the cursor.
                bookmarksCursor.moveToFirst();

                // Open each bookmark
                for (int i = 0; i < bookmarksCursor.getCount(); i++) {
                    // Load the bookmark in a new tab, moving to the tab for the first bookmark if the drawer is not pinned.
                    addNewTab(bookmarksCursor.getString(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_URL)), (!bookmarksDrawerPinned && (i == 0)));

                    // Move to the next bookmark.
                    bookmarksCursor.moveToNext();
                }

                // Close the cursor.
                bookmarksCursor.close();
            } else {  // The bookmark is not a folder.
                // Get the bookmark cursor for this ID.
                Cursor bookmarkCursor = bookmarksDatabaseHelper.getBookmark(databaseId);

                // Move the bookmark cursor to the first row.
                bookmarkCursor.moveToFirst();

                // Load the bookmark in a new tab and move to the tab if the drawer is not pinned.
                addNewTab(bookmarkCursor.getString(bookmarkCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_URL)), !bookmarksDrawerPinned);

                // Close the cursor.
                bookmarkCursor.close();
            }

            // Close the bookmarks drawer if it is not pinned.
            if (!bookmarksDrawerPinned)
                drawerLayout.closeDrawer(GravityCompat.END);

            // Consume the event.
            return true;
        });

        // The drawer listener is used to update the navigation menu.
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                // Reset the drawer icon when the drawer is closed.  Otherwise, it is an arrow if the drawer is open when the app is restarted.
                actionBarDrawerToggle.syncState();
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                if ((newState == DrawerLayout.STATE_SETTLING) || (newState == DrawerLayout.STATE_DRAGGING)) {  // A drawer is opening or closing.
                    // Update the navigation menu items if the WebView is not null.
                    if (currentWebView != null) {
                        navigationBackMenuItem.setEnabled(currentWebView.canGoBack());
                        navigationForwardMenuItem.setEnabled(currentWebView.canGoForward());
                        navigationHistoryMenuItem.setEnabled((currentWebView.canGoBack() || currentWebView.canGoForward()));
                        navigationRequestsMenuItem.setTitle(getString(R.string.requests) + " - " + currentWebView.getRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS));

                        // Hide the keyboard (if displayed).
                        inputMethodManager.hideSoftInputFromWindow(currentWebView.getWindowToken(), 0);
                    }

                    // Clear the focus from from the URL text box.  This removes any text selection markers and context menus, which otherwise draw above the open drawers.
                    urlEditText.clearFocus();

                    // Clear the focus from from the WebView if it is not null, which can happen if a user opens a drawer while the browser is being resumed.
                    if (currentWebView != null) {
                        // Clearing the focus from the WebView removes any text selection markers and context menus, which otherwise draw above the open drawers.
                        currentWebView.clearFocus();
                    }
                }
            }
        });

        // Inflate a bare WebView to get the default user agent.  It is not used to render content on the screen.
        @SuppressLint("InflateParams") View webViewLayout = getLayoutInflater().inflate(R.layout.bare_webview, null, false);

        // Get a handle for the WebView.
        WebView bareWebView = webViewLayout.findViewById(R.id.bare_webview);

        // Store the default user agent.
        webViewDefaultUserAgent = bareWebView.getSettings().getUserAgentString();

        // Destroy the bare WebView.
        bareWebView.destroy();
    }

    private void applyAppSettings() {
        // Get a handle for the shared preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Store the values from the shared preferences in variables.
        incognitoModeEnabled = sharedPreferences.getBoolean(getString(R.string.incognito_mode_key), false);
        sanitizeTrackingQueries = sharedPreferences.getBoolean(getString(R.string.tracking_queries_key), true);
        sanitizeAmpRedirects = sharedPreferences.getBoolean(getString(R.string.amp_redirects_key), true);
        proxyMode = sharedPreferences.getString(getString(R.string.proxy_key), getString(R.string.proxy_default_value));
        fullScreenBrowsingModeEnabled = sharedPreferences.getBoolean(getString(R.string.full_screen_browsing_mode_key), false);
        hideAppBar = sharedPreferences.getBoolean(getString(R.string.hide_app_bar_key), true);
        downloadWithExternalApp = sharedPreferences.getBoolean(getString(R.string.download_with_external_app_key), false);
        scrollAppBar = sharedPreferences.getBoolean(getString(R.string.scroll_app_bar_key), true);

        // Apply the saved proxy mode if the app has been restarted.
        if (savedProxyMode != null) {
            // Apply the saved proxy mode.
            proxyMode = savedProxyMode;

            // Reset the saved proxy mode.
            savedProxyMode = null;
        }

        // Get the search string.
        String searchString = sharedPreferences.getString(getString(R.string.search_key), getString(R.string.search_default_value));

        // Set the search string.
        if (searchString.equals(getString(R.string.custom_url_item)))
            searchURL = sharedPreferences.getString(getString(R.string.search_custom_url_key), getString(R.string.search_custom_url_default_value));
        else
            searchURL = searchString;

        // Apply the proxy.
        applyProxy(false);

        // Adjust the layout and scrolling parameters according to the position of the app bar.
        if (bottomAppBar) {  // The app bar is on the bottom.
            // Adjust the UI.
            if (scrollAppBar || (inFullScreenBrowsingMode && hideAppBar)) {  // The app bar scrolls or full screen browsing mode is engaged with the app bar hidden.
                // Reset the WebView padding to fill the available space.
                swipeRefreshLayout.setPadding(0, 0, 0, 0);
            } else {  // The app bar doesn't scroll or full screen browsing mode is not engaged with the app bar hidden.
                // Move the WebView above the app bar layout.
                swipeRefreshLayout.setPadding(0, 0, 0, appBarHeight);

                // Show the app bar if it is scrolled off the screen.
                if (appBarLayout.getTranslationY() != 0) {
                    // Animate the bottom app bar onto the screen.
                    objectAnimator = ObjectAnimator.ofFloat(appBarLayout, "translationY", 0);

                    // Make it so.
                    objectAnimator.start();
                }
            }
        } else {  // The app bar is on the top.
            // Get the current layout parameters.  Using coordinator layout parameters allows the `setBehavior()` command and using app bar layout parameters allows the `setScrollFlags()` command.
            CoordinatorLayout.LayoutParams swipeRefreshLayoutParams = (CoordinatorLayout.LayoutParams) swipeRefreshLayout.getLayoutParams();
            AppBarLayout.LayoutParams toolbarLayoutParams = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
            AppBarLayout.LayoutParams findOnPageLayoutParams = (AppBarLayout.LayoutParams) findOnPageLinearLayout.getLayoutParams();
            AppBarLayout.LayoutParams tabsLayoutParams = (AppBarLayout.LayoutParams) tabsLinearLayout.getLayoutParams();

            // Add the scrolling behavior to the layout parameters.
            if (scrollAppBar) {
                // Enable scrolling of the app bar.
                swipeRefreshLayoutParams.setBehavior(new AppBarLayout.ScrollingViewBehavior());
                toolbarLayoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS | AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP);
                findOnPageLayoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS | AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP);
                tabsLayoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS | AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP);
            } else {
                // Disable scrolling of the app bar.
                swipeRefreshLayoutParams.setBehavior(null);
                toolbarLayoutParams.setScrollFlags(0);
                findOnPageLayoutParams.setScrollFlags(0);
                tabsLayoutParams.setScrollFlags(0);

                // Expand the app bar if it is currently collapsed.
                appBarLayout.setExpanded(true);
            }

            // Set the app bar scrolling for each WebView.
            for (int i = 0; i < webViewPagerAdapter.getCount(); i++) {
                // Get the WebView tab fragment.
                WebViewTabFragment webViewTabFragment = webViewPagerAdapter.getPageFragment(i);

                // Get the fragment view.
                View fragmentView = webViewTabFragment.getView();

                // Only modify the WebViews if they exist.
                if (fragmentView != null) {
                    // Get the nested scroll WebView from the tab fragment.
                    NestedScrollWebView nestedScrollWebView = fragmentView.findViewById(R.id.nestedscroll_webview);

                    // Set the app bar scrolling.
                    nestedScrollWebView.setNestedScrollingEnabled(scrollAppBar);
                }
            }
        }

        // Update the full screen browsing mode settings.
        if (fullScreenBrowsingModeEnabled && inFullScreenBrowsingMode) {  // Privacy Browser is currently in full screen browsing mode.
            // Update the visibility of the app bar, which might have changed in the settings.
            if (hideAppBar) {
                // Hide the tab linear layout.
                tabsLinearLayout.setVisibility(View.GONE);

                // Hide the action bar.
                actionBar.hide();
            } else {
                // Show the tab linear layout.
                tabsLinearLayout.setVisibility(View.VISIBLE);

                // Show the action bar.
                actionBar.show();
            }

            /* Hide the system bars.
             * SYSTEM_UI_FLAG_FULLSCREEN hides the status bar at the top of the screen.
             * SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN makes the root frame layout fill the area that is normally reserved for the status bar.
             * SYSTEM_UI_FLAG_HIDE_NAVIGATION hides the navigation bar on the bottom or right of the screen.
             * SYSTEM_UI_FLAG_IMMERSIVE_STICKY makes the status and navigation bars translucent and automatically re-hides them after they are shown.
             */
            rootFrameLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {  // Privacy Browser is not in full screen browsing mode.
            // Reset the full screen tracker, which could be true if Privacy Browser was in full screen mode before entering settings and full screen browsing was disabled.
            inFullScreenBrowsingMode = false;

            // Show the tab linear layout.
            tabsLinearLayout.setVisibility(View.VISIBLE);

            // Show the action bar.
            actionBar.show();

            // Remove the `SYSTEM_UI` flags from the root frame layout.
            rootFrameLayout.setSystemUiVisibility(0);
        }
    }

    @Override
    public void navigateHistory(@NonNull String url, int steps) {
        // Apply the domain settings.
        applyDomainSettings(currentWebView, url, false, false, false);

        // Load the history entry.
        currentWebView.goBackOrForward(steps);
    }

    @Override
    public void pinnedErrorGoBack() {
        // Get the current web back forward list.
        WebBackForwardList webBackForwardList = currentWebView.copyBackForwardList();

        // Get the previous entry URL.
        String previousUrl = webBackForwardList.getItemAtIndex(webBackForwardList.getCurrentIndex() - 1).getUrl();

        // Apply the domain settings.
        applyDomainSettings(currentWebView, previousUrl, false, false, false);

        // Go back.
        currentWebView.goBack();
    }

    // `reloadWebsite` is used if returning from the Domains activity.  Otherwise JavaScript might not function correctly if it is newly enabled.
    @SuppressLint("SetJavaScriptEnabled")
    private void applyDomainSettings(NestedScrollWebView nestedScrollWebView, String url, boolean resetTab, boolean reloadWebsite, boolean loadUrl) {
        // Store the current URL.
        nestedScrollWebView.setCurrentUrl(url);

        // Parse the URL into a URI.
        Uri uri = Uri.parse(url);

        // Extract the domain from `uri`.
        String newHostName = uri.getHost();

        // Strings don't like to be null.
        if (newHostName == null) {
            newHostName = "";
        }

        // Apply the domain settings if a new domain is being loaded or if the new domain is blank.  This allows the user to set temporary settings for JavaScript, cookies, DOM storage, etc.
        if (!nestedScrollWebView.getCurrentDomainName().equals(newHostName) || newHostName.equals("")) {
            // Set the new host name as the current domain name.
            nestedScrollWebView.setCurrentDomainName(newHostName);

            // Reset the ignoring of pinned domain information.
            nestedScrollWebView.setIgnorePinnedDomainInformation(false);

            // Clear any pinned SSL certificate or IP addresses.
            nestedScrollWebView.clearPinnedSslCertificate();
            nestedScrollWebView.setPinnedIpAddresses("");

            // Reset the favorite icon if specified.
            if (resetTab) {
                // Initialize the favorite icon.
                nestedScrollWebView.initializeFavoriteIcon();

                // Get the current page position.
                int currentPagePosition = webViewPagerAdapter.getPositionForId(nestedScrollWebView.getWebViewFragmentId());

                // Get the corresponding tab.
                TabLayout.Tab tab = tabLayout.getTabAt(currentPagePosition);

                // Update the tab if it isn't null, which sometimes happens when restarting from the background.
                if (tab != null) {
                    // Get the tab custom view.
                    View tabCustomView = tab.getCustomView();

                    // Remove the warning below that the tab custom view might be null.
                    assert tabCustomView != null;

                    // Get the tab views.
                    ImageView tabFavoriteIconImageView = tabCustomView.findViewById(R.id.favorite_icon_imageview);
                    TextView tabTitleTextView = tabCustomView.findViewById(R.id.title_textview);

                    // Set the default favorite icon as the favorite icon for this tab.
                    tabFavoriteIconImageView.setImageBitmap(Bitmap.createScaledBitmap(nestedScrollWebView.getFavoriteIcon(), 64, 64, true));

                    // Set the loading title text.
                    tabTitleTextView.setText(R.string.loading);
                }
            }

            // Get a full domain name cursor.
            Cursor domainNameCursor = domainsDatabaseHelper.getDomainNameCursorOrderedByDomain();

            // Initialize `domainSettingsSet`.
            Set<String> domainSettingsSet = new HashSet<>();

            // Get the domain name column index.
            int domainNameColumnIndex = domainNameCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.DOMAIN_NAME);

            // Populate the domain settings set.
            for (int i = 0; i < domainNameCursor.getCount(); i++) {
                // Move the domains cursor to the current row.
                domainNameCursor.moveToPosition(i);

                // Store the domain name in the domain settings set.
                domainSettingsSet.add(domainNameCursor.getString(domainNameColumnIndex));
            }

            // Close the domain name cursor.
            domainNameCursor.close();

            // Initialize the domain name in database variable.
            String domainNameInDatabase = null;

            // Check the hostname against the domain settings set.
            if (domainSettingsSet.contains(newHostName)) {  // The hostname is contained in the domain settings set.
                // Record the domain name in the database.
                domainNameInDatabase = newHostName;

                // Set the domain settings applied tracker to true.
                nestedScrollWebView.setDomainSettingsApplied(true);
            } else {  // The hostname is not contained in the domain settings set.
                // Set the domain settings applied tracker to false.
                nestedScrollWebView.setDomainSettingsApplied(false);
            }

            // Check all the subdomains of the host name against wildcard domains in the domain cursor.
            while (!nestedScrollWebView.getDomainSettingsApplied() && newHostName.contains(".")) {  // Stop checking if domain settings are already applied or there are no more `.` in the hostname.
                if (domainSettingsSet.contains("*." + newHostName)) {  // Check the host name prepended by `*.`.
                    // Set the domain settings applied tracker to true.
                    nestedScrollWebView.setDomainSettingsApplied(true);

                    // Store the applied domain names as it appears in the database.
                    domainNameInDatabase = "*." + newHostName;
                }

                // Strip out the lowest subdomain of of the host name.
                newHostName = newHostName.substring(newHostName.indexOf(".") + 1);
            }


            // Get a handle for the shared preferences.
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

            // Store the general preference information.
            String defaultFontSizeString = sharedPreferences.getString(getString(R.string.font_size_key), getString(R.string.font_size_default_value));
            String defaultUserAgentName = sharedPreferences.getString(getString(R.string.user_agent_key), getString(R.string.user_agent_default_value));
            boolean defaultSwipeToRefresh = sharedPreferences.getBoolean(getString(R.string.swipe_to_refresh_key), true);
            String webViewTheme = sharedPreferences.getString(getString(R.string.webview_theme_key), getString(R.string.webview_theme_default_value));
            boolean wideViewport = sharedPreferences.getBoolean(getString(R.string.wide_viewport_key), true);
            boolean displayWebpageImages = sharedPreferences.getBoolean(getString(R.string.display_webpage_images_key), true);

            // Get the WebView theme entry values string array.
            String[] webViewThemeEntryValuesStringArray = getResources().getStringArray(R.array.webview_theme_entry_values);

            // Get a handle for the cookie manager.
            CookieManager cookieManager = CookieManager.getInstance();

            // Initialize the user agent array adapter and string array.
            ArrayAdapter<CharSequence> userAgentNamesArray = ArrayAdapter.createFromResource(this, R.array.user_agent_names, R.layout.spinner_item);
            String[] userAgentDataArray = getResources().getStringArray(R.array.user_agent_data);

            if (nestedScrollWebView.getDomainSettingsApplied()) {  // The url has custom domain settings.
                // Remove the incorrect lint warning below that the domain name in database might be null.
                assert domainNameInDatabase != null;

                // Get a cursor for the current host.
                Cursor currentDomainSettingsCursor = domainsDatabaseHelper.getCursorForDomainName(domainNameInDatabase);

                // Move to the first position.
                currentDomainSettingsCursor.moveToFirst();

                // Get the settings from the cursor.
                nestedScrollWebView.setDomainSettingsDatabaseId(currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ID)));
                nestedScrollWebView.getSettings().setJavaScriptEnabled(currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_JAVASCRIPT)) == 1);
                nestedScrollWebView.setAcceptCookies(currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.COOKIES)) == 1);
                nestedScrollWebView.getSettings().setDomStorageEnabled(currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_DOM_STORAGE)) == 1);
                // Form data can be removed once the minimum API >= 26.
                boolean saveFormData = (currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_FORM_DATA)) == 1);
                nestedScrollWebView.setEasyListEnabled(currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_EASYLIST)) == 1);
                nestedScrollWebView.setEasyPrivacyEnabled(currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_EASYPRIVACY)) == 1);
                nestedScrollWebView.setFanboysAnnoyanceListEnabled(currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(
                        DomainsDatabaseHelper.ENABLE_FANBOYS_ANNOYANCE_LIST)) == 1);
                nestedScrollWebView.setFanboysSocialBlockingListEnabled(currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(
                        DomainsDatabaseHelper.ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST)) == 1);
                nestedScrollWebView.setUltraListEnabled(currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ULTRALIST)) == 1);
                nestedScrollWebView.setUltraPrivacyEnabled(currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_ULTRAPRIVACY)) == 1);
                nestedScrollWebView.setBlockAllThirdPartyRequests(currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(
                        DomainsDatabaseHelper.BLOCK_ALL_THIRD_PARTY_REQUESTS)) == 1);
                String userAgentName = currentDomainSettingsCursor.getString(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.USER_AGENT));
                int fontSize = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.FONT_SIZE));
                int swipeToRefreshInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SWIPE_TO_REFRESH));
                int webViewThemeInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.WEBVIEW_THEME));
                int wideViewportInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.WIDE_VIEWPORT));
                int displayWebpageImagesInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.DISPLAY_IMAGES));
                boolean pinnedSslCertificate = (currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.PINNED_SSL_CERTIFICATE)) == 1);
                String pinnedSslIssuedToCName = currentDomainSettingsCursor.getString(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_TO_COMMON_NAME));
                String pinnedSslIssuedToOName = currentDomainSettingsCursor.getString(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_TO_ORGANIZATION));
                String pinnedSslIssuedToUName = currentDomainSettingsCursor.getString(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_TO_ORGANIZATIONAL_UNIT));
                String pinnedSslIssuedByCName = currentDomainSettingsCursor.getString(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_BY_COMMON_NAME));
                String pinnedSslIssuedByOName = currentDomainSettingsCursor.getString(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_BY_ORGANIZATION));
                String pinnedSslIssuedByUName = currentDomainSettingsCursor.getString(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_BY_ORGANIZATIONAL_UNIT));
                Date pinnedSslStartDate = new Date(currentDomainSettingsCursor.getLong(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_START_DATE)));
                Date pinnedSslEndDate = new Date(currentDomainSettingsCursor.getLong(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_END_DATE)));
                boolean pinnedIpAddresses = (currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.PINNED_IP_ADDRESSES)) == 1);
                String pinnedHostIpAddresses = currentDomainSettingsCursor.getString(currentDomainSettingsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.IP_ADDRESSES));

                // Close the current host domain settings cursor.
                currentDomainSettingsCursor.close();

                // If there is a pinned SSL certificate, store it in the WebView.
                if (pinnedSslCertificate) {
                    nestedScrollWebView.setPinnedSslCertificate(pinnedSslIssuedToCName, pinnedSslIssuedToOName, pinnedSslIssuedToUName, pinnedSslIssuedByCName, pinnedSslIssuedByOName, pinnedSslIssuedByUName,
                            pinnedSslStartDate, pinnedSslEndDate);
                }

                // If there is a pinned IP address, store it in the WebView.
                if (pinnedIpAddresses) {
                    nestedScrollWebView.setPinnedIpAddresses(pinnedHostIpAddresses);
                }

                // Apply the cookie domain settings.
                cookieManager.setAcceptCookie(nestedScrollWebView.getAcceptCookies());

                // Apply the form data setting if the API < 26.
                if (Build.VERSION.SDK_INT < 26) {
                    nestedScrollWebView.getSettings().setSaveFormData(saveFormData);
                }

                // Apply the font size.
                try {  // Try the specified font size to see if it is valid.
                    if (fontSize == 0) {  // Apply the default font size.
                            // Try to set the font size from the value in the app settings.
                            nestedScrollWebView.getSettings().setTextZoom(Integer.parseInt(defaultFontSizeString));
                    } else {  // Apply the font size from domain settings.
                        nestedScrollWebView.getSettings().setTextZoom(fontSize);
                    }
                } catch (Exception exception) {  // The specified font size is invalid
                    // Set the font size to be 100%
                    nestedScrollWebView.getSettings().setTextZoom(100);
                }

                // Set the user agent.
                if (userAgentName.equals(getString(R.string.system_default_user_agent))) {  // Use the system default user agent.
                    // Get the array position of the default user agent name.
                    int defaultUserAgentArrayPosition = userAgentNamesArray.getPosition(defaultUserAgentName);

                    // Set the user agent according to the system default.
                    switch (defaultUserAgentArrayPosition) {
                        case UNRECOGNIZED_USER_AGENT:  // The default user agent name is not on the canonical list.
                            // This is probably because it was set in an older version of Privacy Browser before the switch to persistent user agent names.
                            nestedScrollWebView.getSettings().setUserAgentString(defaultUserAgentName);
                            break;

                        case SETTINGS_WEBVIEW_DEFAULT_USER_AGENT:
                            // Set the user agent to `""`, which uses the default value.
                            nestedScrollWebView.getSettings().setUserAgentString("");
                            break;

                        case SETTINGS_CUSTOM_USER_AGENT:
                            // Set the default custom user agent.
                            nestedScrollWebView.getSettings().setUserAgentString(sharedPreferences.getString(getString(R.string.custom_user_agent_key), getString(R.string.custom_user_agent_default_value)));
                            break;

                        default:
                            // Get the user agent string from the user agent data array
                            nestedScrollWebView.getSettings().setUserAgentString(userAgentDataArray[defaultUserAgentArrayPosition]);
                    }
                } else {  // Set the user agent according to the stored name.
                    // Get the array position of the user agent name.
                    int userAgentArrayPosition = userAgentNamesArray.getPosition(userAgentName);

                    switch (userAgentArrayPosition) {
                        case UNRECOGNIZED_USER_AGENT:  // The user agent name contains a custom user agent.
                            nestedScrollWebView.getSettings().setUserAgentString(userAgentName);
                            break;

                        case SETTINGS_WEBVIEW_DEFAULT_USER_AGENT:
                            // Set the user agent to `""`, which uses the default value.
                            nestedScrollWebView.getSettings().setUserAgentString("");
                            break;

                        default:
                            // Get the user agent string from the user agent data array.
                            nestedScrollWebView.getSettings().setUserAgentString(userAgentDataArray[userAgentArrayPosition]);
                    }
                }

                // Set swipe to refresh.
                switch (swipeToRefreshInt) {
                    case DomainsDatabaseHelper.SYSTEM_DEFAULT:
                        // Store the swipe to refresh status in the nested scroll WebView.
                        nestedScrollWebView.setSwipeToRefresh(defaultSwipeToRefresh);

                        // Update the swipe refresh layout.
                        if (defaultSwipeToRefresh) {  // Swipe to refresh is enabled.
                            // Update the status of the swipe refresh layout if the current WebView is not null (crash reports indicate that in some unexpected way it sometimes is null).
                            if (currentWebView != null) {
                                // Only enable the swipe refresh layout if the WebView is scrolled to the top.  It is updated every time the scroll changes.
                                swipeRefreshLayout.setEnabled(currentWebView.getScrollY() == 0);
                            }
                        } else {  // Swipe to refresh is disabled.
                            // Disable the swipe refresh layout.
                            swipeRefreshLayout.setEnabled(false);
                        }
                        break;

                    case DomainsDatabaseHelper.ENABLED:
                        // Store the swipe to refresh status in the nested scroll WebView.
                        nestedScrollWebView.setSwipeToRefresh(true);


                        // Update the status of the swipe refresh layout if the current WebView is not null (crash reports indicate that in some unexpected way it sometimes is null).
                        if (currentWebView != null) {
                            // Only enable the swipe refresh layout if the WebView is scrolled to the top.  It is updated every time the scroll changes.
                            swipeRefreshLayout.setEnabled(currentWebView.getScrollY() == 0);
                        }
                        break;

                    case DomainsDatabaseHelper.DISABLED:
                        // Store the swipe to refresh status in the nested scroll WebView.
                        nestedScrollWebView.setSwipeToRefresh(false);

                        // Disable swipe to refresh.
                        swipeRefreshLayout.setEnabled(false);
                        break;
                }

                // Set the WebView theme if device is running API >= 29 and algorithmic darkening is supported.
                if ((Build.VERSION.SDK_INT >= 29) && WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    // Set the WebView theme.
                    switch (webViewThemeInt) {
                        case DomainsDatabaseHelper.SYSTEM_DEFAULT:
                            // Set the WebView theme.  A switch statement cannot be used because the WebView theme entry values string array is not a compile time constant.
                            if (webViewTheme.equals(webViewThemeEntryValuesStringArray[1])) {  // The light theme is selected.
                                // Turn off algorithmic darkening.
                                WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.getSettings(), false);
                            } else if (webViewTheme.equals(webViewThemeEntryValuesStringArray[2])) {  // The dark theme is selected.
                                // Turn on algorithmic darkening.
                                WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.getSettings(), true);
                            } else {  // The system default theme is selected.
                                // Get the current system theme status.
                                int currentThemeStatus = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

                                // Set the algorithmic darkening according to the current system theme status.
                                WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.getSettings(), (currentThemeStatus == Configuration.UI_MODE_NIGHT_YES));
                            }
                            break;

                        case DomainsDatabaseHelper.LIGHT_THEME:
                            // Turn off algorithmic darkening.
                            WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.getSettings(), false);
                            break;

                        case DomainsDatabaseHelper.DARK_THEME:
                            // Turn on algorithmic darkening.
                            WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.getSettings(), true);
                            break;
                    }
                }

                // Set the viewport.
                switch (wideViewportInt) {
                    case DomainsDatabaseHelper.SYSTEM_DEFAULT:
                        nestedScrollWebView.getSettings().setUseWideViewPort(wideViewport);
                        break;

                    case DomainsDatabaseHelper.ENABLED:
                        nestedScrollWebView.getSettings().setUseWideViewPort(true);
                        break;

                    case DomainsDatabaseHelper.DISABLED:
                        nestedScrollWebView.getSettings().setUseWideViewPort(false);
                        break;
                }

                // Set the loading of webpage images.
                switch (displayWebpageImagesInt) {
                    case DomainsDatabaseHelper.SYSTEM_DEFAULT:
                        nestedScrollWebView.getSettings().setLoadsImagesAutomatically(displayWebpageImages);
                        break;

                    case DomainsDatabaseHelper.ENABLED:
                        nestedScrollWebView.getSettings().setLoadsImagesAutomatically(true);
                        break;

                    case DomainsDatabaseHelper.DISABLED:
                        nestedScrollWebView.getSettings().setLoadsImagesAutomatically(false);
                        break;
                }

                // Set a background on the URL relative layout to indicate that custom domain settings are being used.
                urlRelativeLayout.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.domain_settings_url_background, null));
            } else {  // The new URL does not have custom domain settings.  Load the defaults.
                // Store the values from the shared preferences.
                nestedScrollWebView.getSettings().setJavaScriptEnabled(sharedPreferences.getBoolean(getString(R.string.javascript_key), false));
                nestedScrollWebView.setAcceptCookies(sharedPreferences.getBoolean(getString(R.string.cookies_key), false));
                nestedScrollWebView.getSettings().setDomStorageEnabled(sharedPreferences.getBoolean(getString(R.string.dom_storage_key), false));
                boolean saveFormData = sharedPreferences.getBoolean(getString(R.string.save_form_data_key), false);  // Form data can be removed once the minimum API >= 26.
                nestedScrollWebView.setEasyListEnabled(sharedPreferences.getBoolean(getString(R.string.easylist_key), true));
                nestedScrollWebView.setEasyPrivacyEnabled(sharedPreferences.getBoolean(getString(R.string.easyprivacy_key), true));
                nestedScrollWebView.setFanboysAnnoyanceListEnabled(sharedPreferences.getBoolean(getString(R.string.fanboys_annoyance_list_key), true));
                nestedScrollWebView.setFanboysSocialBlockingListEnabled(sharedPreferences.getBoolean(getString(R.string.fanboys_social_blocking_list_key), true));
                nestedScrollWebView.setUltraListEnabled(sharedPreferences.getBoolean(getString(R.string.ultralist_key), true));
                nestedScrollWebView.setUltraPrivacyEnabled(sharedPreferences.getBoolean(getString(R.string.ultraprivacy_key), true));
                nestedScrollWebView.setBlockAllThirdPartyRequests(sharedPreferences.getBoolean(getString(R.string.block_all_third_party_requests_key), false));

                // Apply the default cookie setting.
                cookieManager.setAcceptCookie(nestedScrollWebView.getAcceptCookies());

                // Apply the default font size setting.
                try {
                    // Try to set the font size from the value in the app settings.
                    nestedScrollWebView.getSettings().setTextZoom(Integer.parseInt(defaultFontSizeString));
                } catch (Exception exception) {
                    // If the app settings value is invalid, set the font size to 100%.
                    nestedScrollWebView.getSettings().setTextZoom(100);
                }

                // Apply the form data setting if the API < 26.
                if (Build.VERSION.SDK_INT < 26) {
                    nestedScrollWebView.getSettings().setSaveFormData(saveFormData);
                }

                // Store the swipe to refresh status in the nested scroll WebView.
                nestedScrollWebView.setSwipeToRefresh(defaultSwipeToRefresh);

                // Update the swipe refresh layout.
                if (defaultSwipeToRefresh) {  // Swipe to refresh is enabled.
                    // Update the status of the swipe refresh layout if the current WebView is not null (crash reports indicate that in some unexpected way it sometimes is null).
                    if (currentWebView != null) {
                        // Only enable the swipe refresh layout if the WebView is scrolled to the top.  It is updated every time the scroll changes.
                        swipeRefreshLayout.setEnabled(currentWebView.getScrollY() == 0);
                    }
                } else {  // Swipe to refresh is disabled.
                    // Disable the swipe refresh layout.
                    swipeRefreshLayout.setEnabled(false);
                }

                // Reset the pinned variables.
                nestedScrollWebView.setDomainSettingsDatabaseId(-1);

                // Get the array position of the user agent name.
                int userAgentArrayPosition = userAgentNamesArray.getPosition(defaultUserAgentName);

                // Set the user agent.
                switch (userAgentArrayPosition) {
                    case UNRECOGNIZED_USER_AGENT:  // The default user agent name is not on the canonical list.
                        // This is probably because it was set in an older version of Privacy Browser before the switch to persistent user agent names.
                        nestedScrollWebView.getSettings().setUserAgentString(defaultUserAgentName);
                        break;

                    case SETTINGS_WEBVIEW_DEFAULT_USER_AGENT:
                        // Set the user agent to `""`, which uses the default value.
                        nestedScrollWebView.getSettings().setUserAgentString("");
                        break;

                    case SETTINGS_CUSTOM_USER_AGENT:
                        // Set the default custom user agent.
                        nestedScrollWebView.getSettings().setUserAgentString(sharedPreferences.getString(getString(R.string.custom_user_agent_key), getString(R.string.custom_user_agent_default_value)));
                        break;

                    default:
                        // Get the user agent string from the user agent data array
                        nestedScrollWebView.getSettings().setUserAgentString(userAgentDataArray[userAgentArrayPosition]);
                }

                // Set the WebView theme if device is running API >= 29 and algorithmic darkening is supported.
                if ((Build.VERSION.SDK_INT >= 29) && WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    // Set the WebView theme.  A switch statement cannot be used because the WebView theme entry values string array is not a compile time constant.
                    if (webViewTheme.equals(webViewThemeEntryValuesStringArray[1])) {  // the light theme is selected.
                        // Turn off algorithmic darkening.
                        WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.getSettings(), false);
                    } else if (webViewTheme.equals(webViewThemeEntryValuesStringArray[2])) {  // The dark theme is selected.
                        // Turn on algorithmic darkening.
                        WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.getSettings(), true);
                    } else {  // The system default theme is selected.
                        // Get the current system theme status.
                        int currentThemeStatus = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

                        // Set the algorithmic darkening according to the current system theme status.
                        WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.getSettings(), currentThemeStatus == Configuration.UI_MODE_NIGHT_YES);
                    }
                }

                // Set the viewport.
                nestedScrollWebView.getSettings().setUseWideViewPort(wideViewport);

                // Set the loading of webpage images.
                nestedScrollWebView.getSettings().setLoadsImagesAutomatically(displayWebpageImages);

                // Set a transparent background on the URL relative layout.
                urlRelativeLayout.setBackground(ResourcesCompat.getDrawable(getResources(), R.color.transparent, null));
            }

            // Close the domains database helper.
            domainsDatabaseHelper.close();

            // Update the privacy icons.
            updatePrivacyIcons(true);
        }

        // Reload the website if returning from the Domains activity.
        if (reloadWebsite) {
            nestedScrollWebView.reload();
        }

        // Load the URL if directed.  This makes sure that the domain settings are properly loaded before the URL.  By using `loadUrl()`, instead of `loadUrlFromBase()`, the Referer header will never be sent.
        if (loadUrl) {
            nestedScrollWebView.loadUrl(url);
        }
    }

    private void applyProxy(boolean reloadWebViews) {
        // Set the proxy according to the mode.
        proxyHelper.setProxy(getApplicationContext(), appBarLayout, proxyMode);

        // Reset the waiting for proxy tracker.
        waitingForProxy = false;

        // Get the current theme status.
        int currentThemeStatus = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        // Update the user interface and reload the WebViews if requested.
        switch (proxyMode) {
            case ProxyHelper.NONE:
                // Initialize a color background typed value.
                TypedValue colorBackgroundTypedValue = new TypedValue();

                // Get the color background from the theme.
                getTheme().resolveAttribute(android.R.attr.colorBackground, colorBackgroundTypedValue, true);

                // Get the color background int from the typed value.
                int colorBackgroundInt = colorBackgroundTypedValue.data;

                // Set the default app bar layout background.
                appBarLayout.setBackgroundColor(colorBackgroundInt);
                break;

            case ProxyHelper.TOR:
                // Set the app bar background to indicate proxying through Orbot is enabled.
                if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                    appBarLayout.setBackgroundResource(R.color.blue_50);
                } else {
                    appBarLayout.setBackgroundResource(R.color.dark_blue_30);
                }

                // Check to see if Orbot is installed.
                try {
                    // Get the package manager.
                    PackageManager packageManager = getPackageManager();

                    // Check to see if Orbot is in the list.  This will throw an error and drop to the catch section if it isn't installed.
                    packageManager.getPackageInfo("org.torproject.android", 0);

                    // Check to see if the proxy is ready.
                    if (!orbotStatus.equals(ProxyHelper.ORBOT_STATUS_ON)) {  // Orbot is not ready.
                        // Set the waiting for proxy status.
                        waitingForProxy = true;

                        // Show the waiting for proxy dialog if it isn't already displayed.
                        if (getSupportFragmentManager().findFragmentByTag(getString(R.string.waiting_for_proxy_dialog)) == null) {
                            // Get a handle for the waiting for proxy alert dialog.
                            DialogFragment waitingForProxyDialogFragment = new WaitingForProxyDialog();

                            // Try to show the dialog.  Sometimes the window is not yet active if returning from Settings.
                            try {
                                // Show the waiting for proxy alert dialog.
                                waitingForProxyDialogFragment.show(getSupportFragmentManager(), getString(R.string.waiting_for_proxy_dialog));
                            } catch (Exception waitingForTorException) {
                                // Add the dialog to the pending dialog array list.  It will be displayed in `onStart()`.
                                pendingDialogsArrayList.add(new PendingDialogDataClass(waitingForProxyDialogFragment, getString(R.string.waiting_for_proxy_dialog)));
                            }
                        }
                    }
                } catch (PackageManager.NameNotFoundException exception) {  // Orbot is not installed.
                    // Show the Orbot not installed dialog if it is not already displayed.
                    if (getSupportFragmentManager().findFragmentByTag(getString(R.string.proxy_not_installed_dialog)) == null) {
                        // Get a handle for the Orbot not installed alert dialog.
                        DialogFragment orbotNotInstalledDialogFragment = ProxyNotInstalledDialog.displayDialog(proxyMode);

                        // Try to show the dialog.  Sometimes the window is not yet active if returning from Settings.
                        try {
                            // Display the Orbot not installed alert dialog.
                            orbotNotInstalledDialogFragment.show(getSupportFragmentManager(), getString(R.string.proxy_not_installed_dialog));
                        } catch (Exception orbotNotInstalledException) {
                            // Add the dialog to the pending dialog array list.  It will be displayed in `onStart()`.
                            pendingDialogsArrayList.add(new PendingDialogDataClass(orbotNotInstalledDialogFragment, getString(R.string.proxy_not_installed_dialog)));
                        }
                    }
                }
                break;

            case ProxyHelper.I2P:
                // Set the app bar background to indicate proxying through Orbot is enabled.
                if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                    appBarLayout.setBackgroundResource(R.color.blue_50);
                } else {
                    appBarLayout.setBackgroundResource(R.color.dark_blue_30);
                }
                // Get the package manager.
                PackageManager packageManager = getPackageManager();

                // Check to see if I2P is installed.
                try {
                    // Check to see if the F-Droid flavor is installed.  This will throw an error and drop to the catch section if it isn't installed.
                    packageManager.getPackageInfo("net.i2p.android.router", 0);
                } catch (PackageManager.NameNotFoundException fdroidException) {  // The F-Droid flavor is not installed.
                    try {
                        // Check to see if the Google Play flavor is installed.  This will throw an error and drop to the catch section if it isn't installed.
                        packageManager.getPackageInfo("net.i2p.android", 0);
                    } catch (PackageManager.NameNotFoundException googlePlayException) {  // The Google Play flavor is not installed.
                        // Sow the I2P not installed dialog if it is not already displayed.
                        if (getSupportFragmentManager().findFragmentByTag(getString(R.string.proxy_not_installed_dialog)) == null) {
                            // Get a handle for the waiting for proxy alert dialog.
                            DialogFragment i2pNotInstalledDialogFragment = ProxyNotInstalledDialog.displayDialog(proxyMode);

                            // Try to show the dialog.  Sometimes the window is not yet active if returning from Settings.
                            try {
                                // Display the I2P not installed alert dialog.
                                i2pNotInstalledDialogFragment.show(getSupportFragmentManager(), getString(R.string.proxy_not_installed_dialog));
                            } catch (Exception i2pNotInstalledException) {
                                // Add the dialog to the pending dialog array list.  It will be displayed in `onStart()`.
                                pendingDialogsArrayList.add(new PendingDialogDataClass(i2pNotInstalledDialogFragment, getString(R.string.proxy_not_installed_dialog)));
                            }
                        }
                    }
                }
                break;

            case ProxyHelper.CUSTOM:
                // Set the app bar background to indicate proxying through Orbot is enabled.
                if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                    appBarLayout.setBackgroundResource(R.color.blue_50);
                } else {
                    appBarLayout.setBackgroundResource(R.color.dark_blue_30);
                }
                break;
        }

        // Reload the WebViews if requested and not waiting for the proxy.
        if (reloadWebViews && !waitingForProxy) {
            // Reload the WebViews.
            for (int i = 0; i < webViewPagerAdapter.getCount(); i++) {
                // Get the WebView tab fragment.
                WebViewTabFragment webViewTabFragment = webViewPagerAdapter.getPageFragment(i);

                // Get the fragment view.
                View fragmentView = webViewTabFragment.getView();

                // Only reload the WebViews if they exist.
                if (fragmentView != null) {
                    // Get the nested scroll WebView from the tab fragment.
                    NestedScrollWebView nestedScrollWebView = fragmentView.findViewById(R.id.nestedscroll_webview);

                    // Reload the WebView.
                    nestedScrollWebView.reload();
                }
            }
        }
    }

    private void updatePrivacyIcons(boolean runInvalidateOptionsMenu) {
        // Only update the privacy icons if the options menu and the current WebView have already been populated.
        if ((optionsMenu != null) && (currentWebView != null)) {
            // Update the privacy icon.
            if (currentWebView.getSettings().getJavaScriptEnabled()) {  // JavaScript is enabled.
                optionsPrivacyMenuItem.setIcon(R.drawable.javascript_enabled);
            } else if (currentWebView.getAcceptCookies()) {  // JavaScript is disabled but cookies are enabled.
                optionsPrivacyMenuItem.setIcon(R.drawable.warning);
            } else {  // All the dangerous features are disabled.
                optionsPrivacyMenuItem.setIcon(R.drawable.privacy_mode);
            }

            // Update the cookies icon.
            if (currentWebView.getAcceptCookies()) {
                optionsCookiesMenuItem.setIcon(R.drawable.cookies_enabled);
            } else {
                optionsCookiesMenuItem.setIcon(R.drawable.cookies_disabled);
            }

            // Update the refresh icon.
            if (optionsRefreshMenuItem.getTitle() == getString(R.string.refresh)) {  // The refresh icon is displayed.
                // Set the icon.  Once the minimum API is >= 26, the blue and black icons can be combined with a tint list.
                optionsRefreshMenuItem.setIcon(R.drawable.refresh_enabled);
            } else {  // The stop icon is displayed.
                // Set the icon.  Once the minimum API is >= 26, the blue and black icons can be combined with a tint list.
                optionsRefreshMenuItem.setIcon(R.drawable.close_blue);
            }

            // `invalidateOptionsMenu()` calls `onPrepareOptionsMenu()` and redraws the icons in the app bar.
            if (runInvalidateOptionsMenu) {
                invalidateOptionsMenu();
            }
        }
    }

    private void loadBookmarksFolder() {
        // Update the bookmarks cursor with the contents of the bookmarks database for the current folder.
        bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentBookmarksFolder);

        // Populate the bookmarks cursor adapter.
        bookmarksCursorAdapter = new CursorAdapter(this, bookmarksCursor, false) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                // Inflate the individual item layout.
                return getLayoutInflater().inflate(R.layout.bookmarks_drawer_item_linearlayout, parent, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                // Get handles for the views.
                ImageView bookmarkFavoriteIcon = view.findViewById(R.id.bookmark_favorite_icon);
                TextView bookmarkNameTextView = view.findViewById(R.id.bookmark_name);

                // Get the favorite icon byte array from the cursor.
                byte[] favoriteIconByteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.FAVORITE_ICON));

                // Convert the byte array to a `Bitmap` beginning at the first byte and ending at the last.
                Bitmap favoriteIconBitmap = BitmapFactory.decodeByteArray(favoriteIconByteArray, 0, favoriteIconByteArray.length);

                // Display the bitmap in `bookmarkFavoriteIcon`.
                bookmarkFavoriteIcon.setImageBitmap(favoriteIconBitmap);

                // Get the bookmark name from the cursor and display it in `bookmarkNameTextView`.
                String bookmarkNameString = cursor.getString(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_NAME));
                bookmarkNameTextView.setText(bookmarkNameString);

                // Make the font bold for folders.
                if (cursor.getInt(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.IS_FOLDER)) == 1) {
                    bookmarkNameTextView.setTypeface(Typeface.DEFAULT_BOLD);
                } else {  // Reset the font to default for normal bookmarks.
                    bookmarkNameTextView.setTypeface(Typeface.DEFAULT);
                }
            }
        };

        // Get a handle for the bookmarks list view.
        ListView bookmarksListView = findViewById(R.id.bookmarks_drawer_listview);

        // Populate the list view with the adapter.
        bookmarksListView.setAdapter(bookmarksCursorAdapter);

        // Get a handle for the bookmarks title text view.
        TextView bookmarksTitleTextView = findViewById(R.id.bookmarks_title_textview);

        // Set the bookmarks drawer title.
        if (currentBookmarksFolder.isEmpty()) {
            bookmarksTitleTextView.setText(R.string.bookmarks);
        } else {
            bookmarksTitleTextView.setText(currentBookmarksFolder);
        }
    }

    private void openWithApp(String url) {
        // Create an open with app intent with `ACTION_VIEW`.
        Intent openWithAppIntent = new Intent(Intent.ACTION_VIEW);

        // Set the URI but not the MIME type.  This should open all available apps.
        openWithAppIntent.setData(Uri.parse(url));

        // Flag the intent to open in a new task.
        openWithAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Try the intent.
        try {
            // Show the chooser.
            startActivity(openWithAppIntent);
        } catch (ActivityNotFoundException exception) {  // There are no apps available to open the URL.
            // Show a snackbar with the error.
            Snackbar.make(currentWebView, getString(R.string.error) + "  " + exception, Snackbar.LENGTH_INDEFINITE).show();
        }
    }

    private void openWithBrowser(String url) {
        // Create an open with browser intent with `ACTION_VIEW`.
        Intent openWithBrowserIntent = new Intent(Intent.ACTION_VIEW);

        // Set the URI and the MIME type.  `"text/html"` should load browser options.
        openWithBrowserIntent.setDataAndType(Uri.parse(url), "text/html");

        // Flag the intent to open in a new task.
        openWithBrowserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Try the intent.
        try {
            // Show the chooser.
            startActivity(openWithBrowserIntent);
        } catch (ActivityNotFoundException exception) {  // There are no browsers available to open the URL.
            // Show a snackbar with the error.
            Snackbar.make(currentWebView, getString(R.string.error) + "  " + exception, Snackbar.LENGTH_INDEFINITE).show();
        }
    }

    private String sanitizeUrl(String url) {
        // Sanitize tracking queries.
        if (sanitizeTrackingQueries)
            url = SanitizeUrlHelper.sanitizeTrackingQueries(url);

        // Sanitize AMP redirects.
        if (sanitizeAmpRedirects)
            url = SanitizeUrlHelper.sanitizeAmpRedirects(url);

        // Return the sanitized URL.
        return url;
    }

    public void finishedPopulatingBlocklists(ArrayList<ArrayList<List<String[]>>> combinedBlocklists) {
        // Store the blocklists.
        easyList = combinedBlocklists.get(0);
        easyPrivacy = combinedBlocklists.get(1);
        fanboysAnnoyanceList = combinedBlocklists.get(2);
        fanboysSocialList = combinedBlocklists.get(3);
        ultraList = combinedBlocklists.get(4);
        ultraPrivacy = combinedBlocklists.get(5);

        // Check to see if the activity has been restarted with a saved state.
        if ((savedStateArrayList == null) || (savedStateArrayList.size() == 0)) {  // The activity has not been restarted or it was restarted on start to force the night theme.
            // Add the first tab.
            addNewTab("", true);
        } else {  // The activity has been restarted.
            // Restore each tab.
            for (int i = 0; i < savedStateArrayList.size(); i++) {
                // Add a new tab.
                tabLayout.addTab(tabLayout.newTab());

                // Get the new tab.
                TabLayout.Tab newTab = tabLayout.getTabAt(i);

                // Remove the lint warning below that the current tab might be null.
                assert newTab != null;

                // Set a custom view on the new tab.
                newTab.setCustomView(R.layout.tab_custom_view);

                // Add the new page.
                webViewPagerAdapter.restorePage(savedStateArrayList.get(i), savedNestedScrollWebViewStateArrayList.get(i));
            }

            // Reset the saved state variables.
            savedStateArrayList = null;
            savedNestedScrollWebViewStateArrayList = null;

            // Restore the selected tab position.
            if (savedTabPosition == 0) {  // The first tab is selected.
                // Set the first page as the current WebView.
                setCurrentWebView(0);
            } else {  // The first tab is not selected.
                // Move to the selected tab.
                webViewPager.setCurrentItem(savedTabPosition);
            }

            // Get the intent that started the app.
            Intent intent = getIntent();

            // Reset the intent.  This prevents a duplicate tab from being created on restart.
            setIntent(new Intent());

            // Get the information from the intent.
            String intentAction = intent.getAction();
            Uri intentUriData = intent.getData();
            String intentStringExtra = intent.getStringExtra(Intent.EXTRA_TEXT);

            // Determine if this is a web search.
            boolean isWebSearch = ((intentAction != null) && intentAction.equals(Intent.ACTION_WEB_SEARCH));

            // Only process the URI if it contains data or it is a web search.  If the user pressed the desktop icon after the app was already running the URI will be null.
            if (intentUriData != null || intentStringExtra != null || isWebSearch) {
                // Get the shared preferences.
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

                // Create a URL string.
                String url;

                // If the intent action is a web search, perform the search.
                if (isWebSearch) {  // The intent is a web search.
                    // Create an encoded URL string.
                    String encodedUrlString;

                    // Sanitize the search input and convert it to a search.
                    try {
                        encodedUrlString = URLEncoder.encode(intent.getStringExtra(SearchManager.QUERY), "UTF-8");
                    } catch (UnsupportedEncodingException exception) {
                        encodedUrlString = "";
                    }

                    // Add the base search URL.
                    url = searchURL + encodedUrlString;
                } else if (intentUriData != null) {  // The intent contains a URL formatted as a URI.
                    // Set the intent data as the URL.
                    url = intentUriData.toString();
                } else {  // The intent contains a string, which might be a URL.
                    // Set the intent string as the URL.
                    url = intentStringExtra;
                }

                // Add a new tab if specified in the preferences.
                if (sharedPreferences.getBoolean(getString(R.string.open_intents_in_new_tab_key), true)) {  // Load the URL in a new tab.
                    // Set the loading new intent flag.
                    loadingNewIntent = true;

                    // Add a new tab.
                    addNewTab(url, true);
                } else {  // Load the URL in the current tab.
                    // Make it so.
                    loadUrl(currentWebView, url);
                }
            }
        }
    }

    public void addTab(View view) {
        // Add a new tab with a blank URL.
        addNewTab("", true);
    }

    private void addNewTab(String url, boolean moveToTab) {
        // Clear the focus from the URL edit text, so that it will be populated with the information from the new tab.
        urlEditText.clearFocus();

        // Get the new page number.  The page numbers are 0 indexed, so the new page number will match the current count.
        int newTabNumber = tabLayout.getTabCount();

        // Add a new tab.
        tabLayout.addTab(tabLayout.newTab());

        // Get the new tab.
        TabLayout.Tab newTab = tabLayout.getTabAt(newTabNumber);

        // Remove the lint warning below that the current tab might be null.
        assert newTab != null;

        // Set a custom view on the new tab.
        newTab.setCustomView(R.layout.tab_custom_view);

        // Add the new WebView page.
        webViewPagerAdapter.addPage(newTabNumber, webViewPager, url, moveToTab);

        // Show the app bar if it is at the bottom of the screen and the new tab is taking focus.
        if (bottomAppBar && moveToTab && (appBarLayout.getTranslationY() != 0)) {
            // Animate the bottom app bar onto the screen.
            objectAnimator = ObjectAnimator.ofFloat(appBarLayout, "translationY", 0);

            // Make it so.
            objectAnimator.start();
        }
    }

    public void closeTab(View view) {
        // Run the command according to the number of tabs.
        if (tabLayout.getTabCount() > 1) {  // There is more than one tab open.
            // Close the current tab.
            closeCurrentTab();
        } else {  // There is only one tab open.
            clearAndExit();
        }
    }

    private void closeCurrentTab() {
        // Get the current tab number.
        int currentTabNumber = tabLayout.getSelectedTabPosition();

        // Delete the current tab.
        tabLayout.removeTabAt(currentTabNumber);

        // Delete the current page.  If the selected page number did not change during the delete (because the newly selected tab has has same number as the previously deleted tab), it will return true,
        // meaning that the current WebView must be reset.  Otherwise it will happen automatically as the selected tab number changes.
        if (webViewPagerAdapter.deletePage(currentTabNumber, webViewPager)) {
            setCurrentWebView(currentTabNumber);
        }
    }

    private void exitFullScreenVideo() {
        // Re-enable the screen timeout.
        fullScreenVideoFrameLayout.setKeepScreenOn(false);

        // Unset the full screen video flag.
        displayingFullScreenVideo = false;

        // Remove all the views from the full screen video frame layout.
        fullScreenVideoFrameLayout.removeAllViews();

        // Hide the full screen video frame layout.
        fullScreenVideoFrameLayout.setVisibility(View.GONE);

        // Enable the sliding drawers.
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        // Show the coordinator layout.
        coordinatorLayout.setVisibility(View.VISIBLE);

        // Apply the appropriate full screen mode flags.
        if (fullScreenBrowsingModeEnabled && inFullScreenBrowsingMode) {  // Privacy Browser is currently in full screen browsing mode.
            // Hide the app bar if specified.
            if (hideAppBar) {
                // Hide the tab linear layout.
                tabsLinearLayout.setVisibility(View.GONE);

                // Hide the action bar.
                actionBar.hide();
            }

            /* Hide the system bars.
             * SYSTEM_UI_FLAG_FULLSCREEN hides the status bar at the top of the screen.
             * SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN makes the root frame layout fill the area that is normally reserved for the status bar.
             * SYSTEM_UI_FLAG_HIDE_NAVIGATION hides the navigation bar on the bottom or right of the screen.
             * SYSTEM_UI_FLAG_IMMERSIVE_STICKY makes the status and navigation bars translucent and automatically re-hides them after they are shown.
             */
            rootFrameLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {  // Switch to normal viewing mode.
            // Remove the `SYSTEM_UI` flags from the root frame layout.
            rootFrameLayout.setSystemUiVisibility(0);
        }
    }

    private void clearAndExit() {
        // Get a handle for the shared preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Close the bookmarks cursor and database.
        bookmarksCursor.close();
        bookmarksDatabaseHelper.close();

        // Get the status of the clear everything preference.
        boolean clearEverything = sharedPreferences.getBoolean(getString(R.string.clear_everything_key), true);

        // Get a handle for the runtime.
        Runtime runtime = Runtime.getRuntime();

        // Get the application's private data directory, which will be something like `/data/user/0/com.stoutner.privacybrowser.standard`,
        // which links to `/data/data/com.stoutner.privacybrowser.standard`.
        String privateDataDirectoryString = getApplicationInfo().dataDir;

        // Clear cookies.
        if (clearEverything || sharedPreferences.getBoolean(getString(R.string.clear_cookies_key), true)) {
            // Request the cookies be deleted.
            CookieManager.getInstance().removeAllCookies(null);

            // Manually delete the cookies database, as `CookieManager` sometimes will not flush its changes to disk before `System.exit(0)` is run.
            try {
                // Two commands must be used because `Runtime.exec()` does not like `*`.
                Process deleteCookiesProcess = runtime.exec("rm -f " + privateDataDirectoryString + "/app_webview/Cookies");
                Process deleteCookiesJournalProcess = runtime.exec("rm -f " + privateDataDirectoryString + "/app_webview/Cookies-journal");

                // Wait until the processes have finished.
                deleteCookiesProcess.waitFor();
                deleteCookiesJournalProcess.waitFor();
            } catch (Exception exception) {
                // Do nothing if an error is thrown.
            }
        }

        // Clear DOM storage.
        if (clearEverything || sharedPreferences.getBoolean(getString(R.string.clear_dom_storage_key), true)) {
            // Ask `WebStorage` to clear the DOM storage.
            WebStorage webStorage = WebStorage.getInstance();
            webStorage.deleteAllData();

            // Manually delete the DOM storage files and directories, as `WebStorage` sometimes will not flush its changes to disk before `System.exit(0)` is run.
            try {
                // A `String[]` must be used because the directory contains a space and `Runtime.exec` will otherwise not escape the string correctly.
                Process deleteLocalStorageProcess = runtime.exec(new String[] {"rm", "-rf", privateDataDirectoryString + "/app_webview/Local Storage/"});

                // Multiple commands must be used because `Runtime.exec()` does not like `*`.
                Process deleteIndexProcess = runtime.exec("rm -rf " + privateDataDirectoryString + "/app_webview/IndexedDB");
                Process deleteQuotaManagerProcess = runtime.exec("rm -f " + privateDataDirectoryString + "/app_webview/QuotaManager");
                Process deleteQuotaManagerJournalProcess = runtime.exec("rm -f " + privateDataDirectoryString + "/app_webview/QuotaManager-journal");
                Process deleteDatabaseProcess = runtime.exec("rm -rf " + privateDataDirectoryString + "/app_webview/databases");

                // Wait until the processes have finished.
                deleteLocalStorageProcess.waitFor();
                deleteIndexProcess.waitFor();
                deleteQuotaManagerProcess.waitFor();
                deleteQuotaManagerJournalProcess.waitFor();
                deleteDatabaseProcess.waitFor();
            } catch (Exception exception) {
                // Do nothing if an error is thrown.
            }
        }

        // Clear form data if the API < 26.
        if ((Build.VERSION.SDK_INT < 26) && (clearEverything || sharedPreferences.getBoolean(getString(R.string.clear_form_data_key), true))) {
            WebViewDatabase webViewDatabase = WebViewDatabase.getInstance(this);
            webViewDatabase.clearFormData();

            // Manually delete the form data database, as `WebViewDatabase` sometimes will not flush its changes to disk before `System.exit(0)` is run.
            try {
                // A string array must be used because the database contains a space and `Runtime.exec` will not otherwise escape the string correctly.
                Process deleteWebDataProcess = runtime.exec(new String[] {"rm", "-f", privateDataDirectoryString + "/app_webview/Web Data"});
                Process deleteWebDataJournalProcess = runtime.exec(new String[] {"rm", "-f", privateDataDirectoryString + "/app_webview/Web Data-journal"});

                // Wait until the processes have finished.
                deleteWebDataProcess.waitFor();
                deleteWebDataJournalProcess.waitFor();
            } catch (Exception exception) {
                // Do nothing if an error is thrown.
            }
        }

        // Clear the logcat.
        if (clearEverything || sharedPreferences.getBoolean(getString(R.string.clear_logcat_key), true)) {
            try {
                // Clear the logcat.  `-c` clears the logcat.  `-b all` clears all the buffers (instead of just crash, main, and system).
                Process process = Runtime.getRuntime().exec("logcat -b all -c");

                // Wait for the process to finish.
                process.waitFor();
            } catch (IOException|InterruptedException exception) {
                // Do nothing.
            }
        }

        // Clear the cache.
        if (clearEverything || sharedPreferences.getBoolean(getString(R.string.clear_cache_key), true)) {
            // Clear the cache from each WebView.
            for (int i = 0; i < webViewPagerAdapter.getCount(); i++) {
                // Get the WebView tab fragment.
                WebViewTabFragment webViewTabFragment = webViewPagerAdapter.getPageFragment(i);

                // Get the WebView fragment view.
                View webViewFragmentView = webViewTabFragment.getView();

                // Only clear the cache if the WebView exists.
                if (webViewFragmentView != null) {
                    // Get the nested scroll WebView from the tab fragment.
                    NestedScrollWebView nestedScrollWebView = webViewFragmentView.findViewById(R.id.nestedscroll_webview);

                    // Clear the cache for this WebView.
                    nestedScrollWebView.clearCache(true);
                }
            }

            // Manually delete the cache directories.
            try {
                // Delete the main cache directory.
                Process deleteCacheProcess = runtime.exec("rm -rf " + privateDataDirectoryString + "/cache");

                // Delete the secondary `Service Worker` cache directory.
                // A string array must be used because the directory contains a space and `Runtime.exec` will otherwise not escape the string correctly.
                Process deleteServiceWorkerProcess = runtime.exec(new String[] {"rm", "-rf", privateDataDirectoryString + "/app_webview/Default/Service Worker/"});

                // Wait until the processes have finished.
                deleteCacheProcess.waitFor();
                deleteServiceWorkerProcess.waitFor();
            } catch (Exception exception) {
                // Do nothing if an error is thrown.
            }
        }

        // Wipe out each WebView.
        for (int i = 0; i < webViewPagerAdapter.getCount(); i++) {
            // Get the WebView tab fragment.
            WebViewTabFragment webViewTabFragment = webViewPagerAdapter.getPageFragment(i);

            // Get the WebView frame layout.
            FrameLayout webViewFrameLayout = (FrameLayout) webViewTabFragment.getView();

            // Only wipe out the WebView if it exists.
            if (webViewFrameLayout != null) {
                // Get the nested scroll WebView from the tab fragment.
                NestedScrollWebView nestedScrollWebView = webViewFrameLayout.findViewById(R.id.nestedscroll_webview);

                // Clear SSL certificate preferences for this WebView.
                nestedScrollWebView.clearSslPreferences();

                // Clear the back/forward history for this WebView.
                nestedScrollWebView.clearHistory();

                // Remove all the views from the frame layout.
                webViewFrameLayout.removeAllViews();

                // Destroy the internal state of the WebView.
                nestedScrollWebView.destroy();
            }
        }

        // Manually delete the `app_webview` folder, which contains the cookies, DOM storage, form data, and `Service Worker` cache.
        // See `https://code.google.com/p/android/issues/detail?id=233826&thanks=233826&ts=1486670530`.
        if (clearEverything) {
            try {
                // Delete the folder.
                Process deleteAppWebviewProcess = runtime.exec("rm -rf " + privateDataDirectoryString + "/app_webview");

                // Wait until the process has finished.
                deleteAppWebviewProcess.waitFor();
            } catch (Exception exception) {
                // Do nothing if an error is thrown.
            }
        }

        // Close Privacy Browser.  `finishAndRemoveTask` also removes Privacy Browser from the recent app list.
        finishAndRemoveTask();

        // Remove the terminated program from RAM.  The status code is `0`.
        System.exit(0);
    }

    public void bookmarksBack(View view) {
        if (currentBookmarksFolder.isEmpty()) {  // The home folder is displayed.
            // close the bookmarks drawer.
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {  // A subfolder is displayed.
            // Place the former parent folder in `currentFolder`.
            currentBookmarksFolder = bookmarksDatabaseHelper.getParentFolderName(currentBookmarksFolder);

            // Load the new folder.
            loadBookmarksFolder();
        }
    }

    public void toggleBookmarksDrawerPinned(View view) {
        // Toggle the bookmarks drawer pinned tracker.
        bookmarksDrawerPinned = !bookmarksDrawerPinned;

        // Update the bookmarks drawer pinned image view.
        updateBookmarksDrawerPinnedImageView();
    }

    private void updateBookmarksDrawerPinnedImageView() {
        // Set the current icon.
        if (bookmarksDrawerPinned)
            bookmarksDrawerPinnedImageView.setImageResource(R.drawable.pin_selected);
        else
            bookmarksDrawerPinnedImageView.setImageResource(R.drawable.pin);
    }

    private void setCurrentWebView(int pageNumber) {
        // Stop the swipe to refresh indicator if it is running
        swipeRefreshLayout.setRefreshing(false);

        // Get the WebView tab fragment.
        WebViewTabFragment webViewTabFragment = webViewPagerAdapter.getPageFragment(pageNumber);

        // Get the fragment view.
        View webViewFragmentView = webViewTabFragment.getView();

        // Set the current WebView if the fragment view is not null.
        if (webViewFragmentView != null) {  // The fragment has been populated.
            // Store the current WebView.
            currentWebView = webViewFragmentView.findViewById(R.id.nestedscroll_webview);

            // Update the status of swipe to refresh.
            if (currentWebView.getSwipeToRefresh()) {  // Swipe to refresh is enabled.
                // Enable the swipe refresh layout if the WebView is scrolled all the way to the top.  It is updated every time the scroll changes.
                swipeRefreshLayout.setEnabled(currentWebView.getScrollY() == 0);
            } else {  // Swipe to refresh is disabled.
                // Disable the swipe refresh layout.
                swipeRefreshLayout.setEnabled(false);
            }

            // Get a handle for the cookie manager.
            CookieManager cookieManager = CookieManager.getInstance();

            // Set the cookie status.
            cookieManager.setAcceptCookie(currentWebView.getAcceptCookies());

            // Update the privacy icons.  `true` redraws the icons in the app bar.
            updatePrivacyIcons(true);

            // Get a handle for the input method manager.
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

            // Remove the lint warning below that the input method manager might be null.
            assert inputMethodManager != null;

            // Get the current URL.
            String url = currentWebView.getUrl();

            // Update the URL edit text if not loading a new intent.  Otherwise, this will be handled by `onPageStarted()` (if called) and `onPageFinished()`.
            if (!loadingNewIntent) {  // A new intent is not being loaded.
                if ((url == null) || url.equals("about:blank")) {  // The WebView is blank.
                    // Display the hint in the URL edit text.
                    urlEditText.setText("");

                    // Request focus for the URL text box.
                    urlEditText.requestFocus();

                    // Display the keyboard.
                    inputMethodManager.showSoftInput(urlEditText, 0);
                } else {  // The WebView has a loaded URL.
                    // Clear the focus from the URL text box.
                    urlEditText.clearFocus();

                    // Hide the soft keyboard.
                    inputMethodManager.hideSoftInputFromWindow(currentWebView.getWindowToken(), 0);

                    // Display the current URL in the URL text box.
                    urlEditText.setText(url);

                    // Highlight the URL syntax.
                    UrlHelper.highlightSyntax(urlEditText, initialGrayColorSpan, finalGrayColorSpan, redColorSpan);
                }
            } else {  // A new intent is being loaded.
                // Reset the loading new intent tracker.
                loadingNewIntent = false;
            }

            // Set the background to indicate the domain settings status.
            if (currentWebView.getDomainSettingsApplied()) {
                // Set a background on the URL relative layout to indicate that custom domain settings are being used.
                urlRelativeLayout.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.domain_settings_url_background, null));
            } else {
                // Remove any background on the URL relative layout.
                urlRelativeLayout.setBackground(ResourcesCompat.getDrawable(getResources(), R.color.transparent, null));
            }
        } else if (pageNumber == savedTabPosition){  // The app is being restored but the saved tab position fragment has not been populated yet.  Try again in 100 milliseconds.
            // Create a handler to set the current WebView.
            Handler setCurrentWebViewHandler = new Handler();

            // Create a runnable to set the current WebView.
            Runnable setCurrentWebWebRunnable = () -> {
                // Set the current WebView.
                setCurrentWebView(pageNumber);
            };

            // Try setting the current WebView again after 100 milliseconds.
            setCurrentWebViewHandler.postDelayed(setCurrentWebWebRunnable, 100);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void initializeWebView(@NonNull NestedScrollWebView nestedScrollWebView, int pageNumber, @NonNull ProgressBar progressBar, @NonNull String url, boolean restoringState) {
        // Get a handle for the shared preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Get the WebView theme.
        String webViewTheme = sharedPreferences.getString(getString(R.string.webview_theme_key), getString(R.string.webview_theme_default_value));

        // Get the WebView theme entry values string array.
        String[] webViewThemeEntryValuesStringArray = getResources().getStringArray(R.array.webview_theme_entry_values);

        // Set the WebView theme if device is running API >= 29 and algorithmic darkening is supported.
        if ((Build.VERSION.SDK_INT >= 29) && WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            // Set the WebView them.  A switch statement cannot be used because the WebView theme entry values string array is not a compile time constant.
            if (webViewTheme.equals(webViewThemeEntryValuesStringArray[1])) {  // The light theme is selected.
                // Turn off algorithmic darkening.
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.getSettings(), false);

                // Make the WebView visible. The WebView was created invisible in `webview_framelayout` to prevent a white background splash in night mode.
                // If the system is currently in night mode, showing the WebView will be handled in `onProgressChanged()`.
                nestedScrollWebView.setVisibility(View.VISIBLE);
            } else if (webViewTheme.equals(webViewThemeEntryValuesStringArray[2])) {  // The dark theme is selected.
                // Turn on algorithmic darkening.
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.getSettings(), true);
            } else {
                // The system default theme is selected.
                int currentThemeStatus = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

                // Set the algorithmic darkening according to the current system theme status.
                if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {  // The system is in day mode.
                    // Turn off algorithmic darkening.
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.getSettings(), false);

                    // Make the WebView visible. The WebView was created invisible in `webview_framelayout` to prevent a white background splash in night mode.
                    // If the system is currently in night mode, showing the WebView will be handled in `onProgressChanged()`.
                    nestedScrollWebView.setVisibility(View.VISIBLE);
                } else {  // The system is in night mode.
                    // Turn on algorithmic darkening.
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.getSettings(), true);
                }
            }
        }

        // Get a handle for the activity
        Activity activity = this;

        // Get a handle for the input method manager.
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // Instantiate the blocklist helper.
        BlocklistHelper blocklistHelper = new BlocklistHelper();

        // Remove the lint warning below that the input method manager might be null.
        assert inputMethodManager != null;

        // Set the app bar scrolling.
        nestedScrollWebView.setNestedScrollingEnabled(scrollAppBar);

        // Allow pinch to zoom.
        nestedScrollWebView.getSettings().setBuiltInZoomControls(true);

        // Hide zoom controls.
        nestedScrollWebView.getSettings().setDisplayZoomControls(false);

        // Don't allow mixed content (HTTP and HTTPS) on the same website.
        nestedScrollWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        // Set the WebView to load in overview mode (zoomed out to the maximum width).
        nestedScrollWebView.getSettings().setLoadWithOverviewMode(true);

        // Explicitly disable geolocation.
        nestedScrollWebView.getSettings().setGeolocationEnabled(false);

        // Allow loading of file:// URLs.  This is necessary for opening MHT web archives, which are copies into a temporary cache location.
        nestedScrollWebView.getSettings().setAllowFileAccess(true);

        // Create a double-tap gesture detector to toggle full-screen mode.
        GestureDetector doubleTapGestureDetector = new GestureDetector(getApplicationContext(), new GestureDetector.SimpleOnGestureListener() {
            // Override `onDoubleTap()`.  All other events are handled using the default settings.
            @Override
            public boolean onDoubleTap(MotionEvent event) {
                if (fullScreenBrowsingModeEnabled) {  // Only process the double-tap if full screen browsing mode is enabled.
                    // Toggle the full screen browsing mode tracker.
                    inFullScreenBrowsingMode = !inFullScreenBrowsingMode;

                    // Toggle the full screen browsing mode.
                    if (inFullScreenBrowsingMode) {  // Switch to full screen mode.
                        // Hide the app bar if specified.
                        if (hideAppBar) {  // The app bar is hidden.
                            // Close the find on page bar if it is visible.
                            closeFindOnPage(null);

                            // Hide the tab linear layout.
                            tabsLinearLayout.setVisibility(View.GONE);

                            // Hide the action bar.
                            actionBar.hide();

                            // Set layout and scrolling parameters according to the position of the app bar.
                            if (bottomAppBar) {  // The app bar is at the bottom.
                                // Reset the WebView padding to fill the available space.
                                swipeRefreshLayout.setPadding(0, 0, 0, 0);
                            } else {  // The app bar is at the top.
                                // Check to see if the app bar is normally scrolled.
                                if (scrollAppBar) {  // The app bar is scrolled when it is displayed.
                                    // Get the swipe refresh layout parameters.
                                    CoordinatorLayout.LayoutParams swipeRefreshLayoutParams = (CoordinatorLayout.LayoutParams) swipeRefreshLayout.getLayoutParams();

                                    // Remove the off-screen scrolling layout.
                                    swipeRefreshLayoutParams.setBehavior(null);
                                } else {  // The app bar is not scrolled when it is displayed.
                                    // Remove the padding from the top of the swipe refresh layout.
                                    swipeRefreshLayout.setPadding(0, 0, 0, 0);

                                    // The swipe refresh circle must be moved above the now removed status bar location.
                                    swipeRefreshLayout.setProgressViewOffset(false, -200, defaultProgressViewEndOffset);
                                }
                            }
                        } else {  // The app bar is not hidden.
                            // Adjust the UI for the bottom app bar.
                            if (bottomAppBar) {
                                // Adjust the UI according to the scrolling of the app bar.
                                if (scrollAppBar) {
                                    // Reset the WebView padding to fill the available space.
                                    swipeRefreshLayout.setPadding(0, 0, 0, 0);
                                } else {
                                    // Move the WebView above the app bar layout.
                                    swipeRefreshLayout.setPadding(0, 0, 0, appBarHeight);
                                }
                            }
                        }

                        /* Hide the system bars.
                         * SYSTEM_UI_FLAG_FULLSCREEN hides the status bar at the top of the screen.
                         * SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN makes the root frame layout fill the area that is normally reserved for the status bar.
                         * SYSTEM_UI_FLAG_HIDE_NAVIGATION hides the navigation bar on the bottom or right of the screen.
                         * SYSTEM_UI_FLAG_IMMERSIVE_STICKY makes the status and navigation bars translucent and automatically re-hides them after they are shown.
                         */
                        rootFrameLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    } else {  // Switch to normal viewing mode.
                        // Show the app bar if it was hidden.
                        if (hideAppBar) {
                            // Show the tab linear layout.
                            tabsLinearLayout.setVisibility(View.VISIBLE);

                            // Show the action bar.
                            actionBar.show();
                        }

                        // Set layout and scrolling parameters according to the position of the app bar.
                        if (bottomAppBar) {  // The app bar is at the bottom.
                            // Adjust the UI.
                            if (scrollAppBar) {
                                // Reset the WebView padding to fill the available space.
                                swipeRefreshLayout.setPadding(0, 0, 0, 0);
                            } else {
                                // Move the WebView above the app bar layout.
                                swipeRefreshLayout.setPadding(0, 0, 0, appBarHeight);
                            }
                        } else {  // The app bar is at the top.
                            // Check to see if the app bar is normally scrolled.
                            if (scrollAppBar) {  // The app bar is scrolled when it is displayed.
                                // Get the swipe refresh layout parameters.
                                CoordinatorLayout.LayoutParams swipeRefreshLayoutParams = (CoordinatorLayout.LayoutParams) swipeRefreshLayout.getLayoutParams();

                                // Add the off-screen scrolling layout.
                                swipeRefreshLayoutParams.setBehavior(new AppBarLayout.ScrollingViewBehavior());
                            } else {  // The app bar is not scrolled when it is displayed.
                                // The swipe refresh layout must be manually moved below the app bar layout.
                                swipeRefreshLayout.setPadding(0, appBarHeight, 0, 0);

                                // The swipe to refresh circle doesn't always hide itself completely unless it is moved up 10 pixels.
                                swipeRefreshLayout.setProgressViewOffset(false, defaultProgressViewStartOffset - 10 + appBarHeight, defaultProgressViewEndOffset + appBarHeight);
                            }
                        }

                        // Remove the `SYSTEM_UI` flags from the root frame layout.
                        rootFrameLayout.setSystemUiVisibility(0);
                    }

                    // Consume the double-tap.
                    return true;
                } else { // Do not consume the double-tap because full screen browsing mode is disabled.
                    return false;
                }
            }

            @Override
            public boolean onFling(MotionEvent motionEvent1, MotionEvent motionEvent2, float velocityX, float velocityY) {
                // Scroll the bottom app bar if enabled.
                if (bottomAppBar && scrollAppBar && !objectAnimator.isRunning()) {
                    // Calculate the Y change.
                    float motionY = motionEvent2.getY() - motionEvent1.getY();

                    // Scroll the app bar if the change is greater than 50 pixels.
                    if (motionY > 50) {
                        // Animate the bottom app bar onto the screen.
                        objectAnimator = ObjectAnimator.ofFloat(appBarLayout, "translationY", 0);
                    } else if (motionY < -50) {
                        // Animate the bottom app bar off the screen.
                        objectAnimator = ObjectAnimator.ofFloat(appBarLayout, "translationY", appBarLayout.getHeight());
                    }

                    // Make it so.
                    objectAnimator.start();
                }

                // Do not consume the event.
                return false;
            }
        });

        // Pass all touch events on the WebView through the double-tap gesture detector.
        nestedScrollWebView.setOnTouchListener((View view, MotionEvent event) -> {
            // Call `performClick()` on the view, which is required for accessibility.
            view.performClick();

            // Send the event to the gesture detector.
            return doubleTapGestureDetector.onTouchEvent(event);
        });

        // Register the WebView for a context menu.  This is used to see link targets and download images.
        registerForContextMenu(nestedScrollWebView);

        // Allow the downloading of files.
        nestedScrollWebView.setDownloadListener((String downloadUrl, String userAgent, String contentDisposition, String mimetype, long contentLength) -> {
            // Check the download preference.
            if (downloadWithExternalApp) {  // Download with an external app.
                downloadUrlWithExternalApp(downloadUrl);
            } else {  // Handle the download inside of Privacy Browser.
                // Define a formatted file size string.
                String formattedFileSizeString;

                // Process the content length if it contains data.
                if (contentLength > 0) {  // The content length is greater than 0.
                    // Format the content length as a string.
                    formattedFileSizeString = NumberFormat.getInstance().format(contentLength) + " " + getString(R.string.bytes);
                } else {  // The content length is not greater than 0.
                    // Set the formatted file size string to be `unknown size`.
                    formattedFileSizeString = getString(R.string.unknown_size);
                }

                // Get the file name from the content disposition.
                String fileNameString = UrlHelper.getFileName(this, contentDisposition, mimetype, downloadUrl);

                // Instantiate the save dialog.
                DialogFragment saveDialogFragment = SaveDialog.saveUrl(downloadUrl, fileNameString, formattedFileSizeString, userAgent,
                        nestedScrollWebView.getAcceptCookies());

                // Try to show the dialog.  The download listener continues to function even when the WebView is paused.  Attempting to display a dialog in that state leads to a crash.
                try {
                    // Show the save dialog.  It must be named `save_dialog` so that the file picker can update the file name.
                    saveDialogFragment.show(getSupportFragmentManager(), getString(R.string.save_dialog));
                } catch (Exception exception) {  // The dialog could not be shown.
                    // Add the dialog to the pending dialog array list.  It will be displayed in `onStart()`.
                    pendingDialogsArrayList.add(new PendingDialogDataClass(saveDialogFragment, getString(R.string.save_dialog)));
                }
            }
        });

        // Update the find on page count.
        nestedScrollWebView.setFindListener(new WebView.FindListener() {
            // Get a handle for `findOnPageCountTextView`.
            final TextView findOnPageCountTextView = findViewById(R.id.find_on_page_count_textview);

            @Override
            public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
                if ((isDoneCounting) && (numberOfMatches == 0)) {  // There are no matches.
                    // Set `findOnPageCountTextView` to `0/0`.
                    findOnPageCountTextView.setText(R.string.zero_of_zero);
                } else if (isDoneCounting) {  // There are matches.
                    // `activeMatchOrdinal` is zero-based.
                    int activeMatch = activeMatchOrdinal + 1;

                    // Build the match string.
                    String matchString = activeMatch + "/" + numberOfMatches;

                    // Set `findOnPageCountTextView`.
                    findOnPageCountTextView.setText(matchString);
                }
            }
        });

        // Process scroll changes.
        nestedScrollWebView.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            // Set the swipe to refresh status.
            if (nestedScrollWebView.getSwipeToRefresh()) {
                // Only enable swipe to refresh if the WebView is scrolled to the top.
                swipeRefreshLayout.setEnabled(nestedScrollWebView.getScrollY() == 0);
            } else {
                // Disable swipe to refresh.
                swipeRefreshLayout.setEnabled(false);
            }

            // Reinforce the system UI visibility flags if in full screen browsing mode.
            // This hides the status and navigation bars, which are displayed if other elements are shown, like dialog boxes, the options menu, or the keyboard.
            if (inFullScreenBrowsingMode) {
                /* Hide the system bars.
                 * SYSTEM_UI_FLAG_FULLSCREEN hides the status bar at the top of the screen.
                 * SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN makes the root frame layout fill the area that is normally reserved for the status bar.
                 * SYSTEM_UI_FLAG_HIDE_NAVIGATION hides the navigation bar on the bottom or right of the screen.
                 * SYSTEM_UI_FLAG_IMMERSIVE_STICKY makes the status and navigation bars translucent and automatically re-hides them after they are shown.
                 */
                rootFrameLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        });

        // Set the web chrome client.
        nestedScrollWebView.setWebChromeClient(new WebChromeClient() {
            // Update the progress bar when a page is loading.
            @Override
            public void onProgressChanged(WebView view, int progress) {
                // Update the progress bar.
                progressBar.setProgress(progress);

                // Set the visibility of the progress bar.
                if (progress < 100) {
                    // Show the progress bar.
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    // Hide the progress bar.
                    progressBar.setVisibility(View.GONE);

                    //Stop the swipe to refresh indicator if it is running
                    swipeRefreshLayout.setRefreshing(false);

                    // Make the current WebView visible.  If this is a new tab, the current WebView would have been created invisible in `webview_framelayout` to prevent a white background splash in night mode.
                    nestedScrollWebView.setVisibility(View.VISIBLE);
                }
            }

            // Set the favorite icon when it changes.
            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                // Only update the favorite icon if the website has finished loading and the new favorite icon height is greater than the current favorite icon height.
                // This prevents low resolution icons from replacing high resolution one.
                // The check for the visibility of the progress bar can possibly be removed once https://redmine.stoutner.com/issues/747 is fixed.
                if ((progressBar.getVisibility() == View.GONE) && (icon.getHeight() > nestedScrollWebView.getFavoriteIconHeight())) {
                    // Store the new favorite icon.
                    nestedScrollWebView.setFavoriteIcon(icon);

                    // Get the current page position.
                    int currentPosition = webViewPagerAdapter.getPositionForId(nestedScrollWebView.getWebViewFragmentId());

                    // Get the current tab.
                    TabLayout.Tab tab = tabLayout.getTabAt(currentPosition);

                    // Check to see if the tab has been populated.
                    if (tab != null) {
                        // Get the custom view from the tab.
                        View tabView = tab.getCustomView();

                        // Check to see if the custom tab view has been populated.
                        if (tabView != null) {
                            // Get the favorite icon image view from the tab.
                            ImageView tabFavoriteIconImageView = tabView.findViewById(R.id.favorite_icon_imageview);

                            // Display the favorite icon in the tab.
                            tabFavoriteIconImageView.setImageBitmap(Bitmap.createScaledBitmap(icon, 64, 64, true));
                        }
                    }
                }
            }

            // Save a copy of the title when it changes.
            @Override
            public void onReceivedTitle(WebView view, String title) {
                // Get the current page position.
                int currentPosition = webViewPagerAdapter.getPositionForId(nestedScrollWebView.getWebViewFragmentId());

                // Get the current tab.
                TabLayout.Tab tab = tabLayout.getTabAt(currentPosition);

                // Only populate the title text view if the tab has been fully created.
                if (tab != null) {
                    // Get the custom view from the tab.
                    View tabView = tab.getCustomView();

                    // Only populate the title text view if the tab view has been fully populated.
                    if (tabView != null) {
                        // Get the title text view from the tab.
                        TextView tabTitleTextView = tabView.findViewById(R.id.title_textview);

                        // Set the title according to the URL.
                        if (title.equals("about:blank")) {
                            // Set the title to indicate a new tab.
                            tabTitleTextView.setText(R.string.new_tab);
                        } else {
                            // Set the title as the tab text.
                            tabTitleTextView.setText(title);
                        }
                    }
                }
            }

            // Enter full screen video.
            @Override
            public void onShowCustomView(View video, CustomViewCallback callback) {
                // Set the full screen video flag.
                displayingFullScreenVideo = true;

                // Hide the keyboard.
                inputMethodManager.hideSoftInputFromWindow(nestedScrollWebView.getWindowToken(), 0);

                // Hide the coordinator layout.
                coordinatorLayout.setVisibility(View.GONE);

                /* Hide the system bars.
                 * SYSTEM_UI_FLAG_FULLSCREEN hides the status bar at the top of the screen.
                 * SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN makes the root frame layout fill the area that is normally reserved for the status bar.
                 * SYSTEM_UI_FLAG_HIDE_NAVIGATION hides the navigation bar on the bottom or right of the screen.
                 * SYSTEM_UI_FLAG_IMMERSIVE_STICKY makes the status and navigation bars translucent and automatically re-hides them after they are shown.
                 */
                rootFrameLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

                // Disable the sliding drawers.
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

                // Add the video view to the full screen video frame layout.
                fullScreenVideoFrameLayout.addView(video);

                // Show the full screen video frame layout.
                fullScreenVideoFrameLayout.setVisibility(View.VISIBLE);

                // Disable the screen timeout while the video is playing.  YouTube does this automatically, but not all other videos do.
                fullScreenVideoFrameLayout.setKeepScreenOn(true);
            }

            // Exit full screen video.
            @Override
            public void onHideCustomView() {
                // Exit the full screen video.
                exitFullScreenVideo();
            }

            // Upload files.
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                // Store the file path callback.
                fileChooserCallback = filePathCallback;

                // Create an intent to open a chooser based on the file chooser parameters.
                Intent fileChooserIntent = fileChooserParams.createIntent();

                // Get a handle for the package manager.
                PackageManager packageManager = getPackageManager();

                // Check to see if the file chooser intent resolves to an installed package.
                if (fileChooserIntent.resolveActivity(packageManager) != null) {  // The file chooser intent is fine.
                    // Launch the file chooser intent.
                    browseFileUploadActivityResultLauncher.launch(fileChooserIntent);
                } else {  // The file chooser intent will cause a crash.
                    // Create a generic intent to open a chooser.
                    Intent genericFileChooserIntent = new Intent(Intent.ACTION_GET_CONTENT);

                    // Request an openable file.
                    genericFileChooserIntent.addCategory(Intent.CATEGORY_OPENABLE);

                    // Set the file type to everything.
                    genericFileChooserIntent.setType("*/*");

                    // Launch the generic file chooser intent.
                    browseFileUploadActivityResultLauncher.launch(genericFileChooserIntent);
                }
                return true;
            }
        });

        nestedScrollWebView.setWebViewClient(new WebViewClient() {
            // `shouldOverrideUrlLoading` makes this WebView the default handler for URLs inside the app, so that links are not kicked out to other apps.
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest webResourceRequest) {
                // Get the URL from the web resource request.
                String url = webResourceRequest.getUrl().toString();

                // Sanitize the url.
                url = sanitizeUrl(url);

                // Handle the URL according to the type.
                if (url.startsWith("http")) {  // Load the URL in Privacy Browser.
                    // Load the URL.  By using `loadUrl()`, instead of `loadUrlFromBase()`, the Referer header will never be sent.
                    loadUrl(nestedScrollWebView, url);

                    // Returning true indicates that Privacy Browser is manually handling the loading of the URL.
                    // Custom headers cannot be added if false is returned and the WebView handles the loading of the URL.
                    return true;
                } else if (url.startsWith("mailto:")) {  // Load the email address in an external email program.
                    // Use `ACTION_SENDTO` instead of `ACTION_SEND` so that only email programs are launched.
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);

                    // Parse the url and set it as the data for the intent.
                    emailIntent.setData(Uri.parse(url));

                    // Open the email program in a new task instead of as part of Privacy Browser.
                    emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    try {
                        // Make it so.
                        startActivity(emailIntent);
                    } catch (ActivityNotFoundException exception) {
                        // Display a snackbar.
                        Snackbar.make(currentWebView, getString(R.string.error) + "  " + exception, Snackbar.LENGTH_INDEFINITE).show();
                    }


                    // Returning true indicates Privacy Browser is handling the URL by creating an intent.
                    return true;
                } else if (url.startsWith("tel:")) {  // Load the phone number in the dialer.
                    // Open the dialer and load the phone number, but wait for the user to place the call.
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL);

                    // Add the phone number to the intent.
                    dialIntent.setData(Uri.parse(url));

                    // Open the dialer in a new task instead of as part of Privacy Browser.
                    dialIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    try {
                        // Make it so.
                        startActivity(dialIntent);
                    } catch (ActivityNotFoundException exception) {
                        // Display a snackbar.
                        Snackbar.make(currentWebView, getString(R.string.error) + "  " + exception, Snackbar.LENGTH_INDEFINITE).show();
                    }

                    // Returning true indicates Privacy Browser is handling the URL by creating an intent.
                    return true;
                } else {  // Load a system chooser to select an app that can handle the URL.
                    // Open an app that can handle the URL.
                    Intent genericIntent = new Intent(Intent.ACTION_VIEW);

                    // Add the URL to the intent.
                    genericIntent.setData(Uri.parse(url));

                    // List all apps that can handle the URL instead of just opening the first one.
                    genericIntent.addCategory(Intent.CATEGORY_BROWSABLE);

                    // Open the app in a new task instead of as part of Privacy Browser.
                    genericIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    // Start the app or display a snackbar if no app is available to handle the URL.
                    try {
                        startActivity(genericIntent);
                    } catch (ActivityNotFoundException exception) {
                        Snackbar.make(nestedScrollWebView, getString(R.string.unrecognized_url) + "  " + url, Snackbar.LENGTH_SHORT).show();
                    }

                    // Returning true indicates Privacy Browser is handling the URL by creating an intent.
                    return true;
                }
            }

            // Check requests against the block lists.  The deprecated `shouldInterceptRequest()` must be used until minimum API >= 21.
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest webResourceRequest) {
                // Get the URL.
                String url = webResourceRequest.getUrl().toString();

                // Check to see if the resource request is for the main URL.
                if (url.equals(nestedScrollWebView.getCurrentUrl())) {
                    // `return null` loads the resource request, which should never be blocked if it is the main URL.
                    return null;
                }

                // Wait until the blocklists have been populated.  When Privacy Browser is being resumed after having the process killed in the background it will try to load the URLs immediately.
                while (ultraPrivacy == null) {
                    // The wait must be synchronized, which only lets one thread run on it at a time, or `java.lang.IllegalMonitorStateException` is thrown.
                    synchronized (this) {
                        try {
                            // Check to see if the blocklists have been populated after 100 ms.
                            wait(100);
                        } catch (InterruptedException exception) {
                            // Do nothing.
                        }
                    }
                }

                // Create an empty web resource response to be used if the resource request is blocked.
                WebResourceResponse emptyWebResourceResponse = new WebResourceResponse("text/plain", "utf8", new ByteArrayInputStream("".getBytes()));

                // Reset the whitelist results tracker.
                String[] whitelistResultStringArray = null;

                // Initialize the third party request tracker.
                boolean isThirdPartyRequest = false;

                // Get the current URL.  `.getUrl()` throws an error because operations on the WebView cannot be made from this thread.
                String currentBaseDomain = nestedScrollWebView.getCurrentDomainName();

                // Store a copy of the current domain for use in later requests.
                String currentDomain = currentBaseDomain;

                // Get the request host name.
                String requestBaseDomain = webResourceRequest.getUrl().getHost();

                // Only check for third-party requests if the current base domain is not empty and the request domain is not null.
                if (!currentBaseDomain.isEmpty() && (requestBaseDomain != null)) {
                    // Determine the current base domain.
                    while (currentBaseDomain.indexOf(".", currentBaseDomain.indexOf(".") + 1) > 0) {  // There is at least one subdomain.
                        // Remove the first subdomain.
                        currentBaseDomain = currentBaseDomain.substring(currentBaseDomain.indexOf(".") + 1);
                    }

                    // Determine the request base domain.
                    while (requestBaseDomain.indexOf(".", requestBaseDomain.indexOf(".") + 1) > 0) {  // There is at least one subdomain.
                        // Remove the first subdomain.
                        requestBaseDomain = requestBaseDomain.substring(requestBaseDomain.indexOf(".") + 1);
                    }

                    // Update the third party request tracker.
                    isThirdPartyRequest = !currentBaseDomain.equals(requestBaseDomain);
                }

                // Get the current WebView page position.
                int webViewPagePosition = webViewPagerAdapter.getPositionForId(nestedScrollWebView.getWebViewFragmentId());

                // Determine if the WebView is currently displayed.
                boolean webViewDisplayed = (webViewPagePosition == tabLayout.getSelectedTabPosition());

                // Block third-party requests if enabled.
                if (isThirdPartyRequest && nestedScrollWebView.getBlockAllThirdPartyRequests()) {
                    // Add the result to the resource requests.
                    nestedScrollWebView.addResourceRequest(new String[]{BlocklistHelper.REQUEST_THIRD_PARTY, url});

                    // Increment the blocked requests counters.
                    nestedScrollWebView.incrementRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS);
                    nestedScrollWebView.incrementRequestsCount(NestedScrollWebView.THIRD_PARTY_REQUESTS);

                    // Update the titles of the blocklist menu items if the WebView is currently displayed.
                    if (webViewDisplayed) {
                        // Updating the UI must be run from the UI thread.
                        activity.runOnUiThread(() -> {
                            // Update the menu item titles.
                            navigationRequestsMenuItem.setTitle(getString(R.string.requests) + " - " + nestedScrollWebView.getRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS));

                            // Update the options menu if it has been populated.
                            if (optionsMenu != null) {
                                optionsBlocklistsMenuItem.setTitle(getString(R.string.blocklists) + " - " + nestedScrollWebView.getRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS));
                                optionsBlockAllThirdPartyRequestsMenuItem.setTitle(nestedScrollWebView.getRequestsCount(NestedScrollWebView.THIRD_PARTY_REQUESTS) + " - " +
                                        getString(R.string.block_all_third_party_requests));
                            }
                        });
                    }

                    // Return an empty web resource response.
                    return emptyWebResourceResponse;
                }

                // Check UltraList if it is enabled.
                if (nestedScrollWebView.getUltraListEnabled()) {
                    // Check the URL against UltraList.
                    String[] ultraListResults = blocklistHelper.checkBlocklist(currentDomain, url, isThirdPartyRequest, ultraList);

                    // Process the UltraList results.
                    if (ultraListResults[0].equals(BlocklistHelper.REQUEST_BLOCKED)) {  // The resource request matched UltraList's blacklist.
                        // Add the result to the resource requests.
                        nestedScrollWebView.addResourceRequest(new String[] {ultraListResults[0], ultraListResults[1], ultraListResults[2], ultraListResults[3], ultraListResults[4], ultraListResults[5]});

                        // Increment the blocked requests counters.
                        nestedScrollWebView.incrementRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS);
                        nestedScrollWebView.incrementRequestsCount(NestedScrollWebView.ULTRALIST);

                        // Update the titles of the blocklist menu items if the WebView is currently displayed.
                        if (webViewDisplayed) {
                            // Updating the UI must be run from the UI thread.
                            activity.runOnUiThread(() -> {
                                // Update the menu item titles.
                                navigationRequestsMenuItem.setTitle(getString(R.string.requests) + " - " + nestedScrollWebView.getRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS));

                                // Update the options menu if it has been populated.
                                if (optionsMenu != null) {
                                    optionsBlocklistsMenuItem.setTitle(getString(R.string.blocklists) + " - " + nestedScrollWebView.getRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS));
                                    optionsUltraListMenuItem.setTitle(nestedScrollWebView.getRequestsCount(NestedScrollWebView.ULTRALIST) + " - " + getString(R.string.ultralist));
                                }
                            });
                        }

                        // The resource request was blocked.  Return an empty web resource response.
                        return emptyWebResourceResponse;
                    } else if (ultraListResults[0].equals(BlocklistHelper.REQUEST_ALLOWED)) {  // The resource request matched UltraList's whitelist.
                        // Add a whitelist entry to the resource requests array.
                        nestedScrollWebView.addResourceRequest(new String[] {ultraListResults[0], ultraListResults[1], ultraListResults[2], ultraListResults[3], ultraListResults[4], ultraListResults[5]});

                        // The resource request has been allowed by UltraPrivacy.  `return null` loads the requested resource.
                        return null;
                    }
                }

                // Check UltraPrivacy if it is enabled.
                if (nestedScrollWebView.getUltraPrivacyEnabled()) {
                    // Check the URL against UltraPrivacy.
                    String[] ultraPrivacyResults = blocklistHelper.checkBlocklist(currentDomain, url, isThirdPartyRequest, ultraPrivacy);

                    // Process the UltraPrivacy results.
                    if (ultraPrivacyResults[0].equals(BlocklistHelper.REQUEST_BLOCKED)) {  // The resource request matched UltraPrivacy's blacklist.
                        // Add the result to the resource requests.
                        nestedScrollWebView.addResourceRequest(new String[] {ultraPrivacyResults[0], ultraPrivacyResults[1], ultraPrivacyResults[2], ultraPrivacyResults[3], ultraPrivacyResults[4],
                                ultraPrivacyResults[5]});

                        // Increment the blocked requests counters.
                        nestedScrollWebView.incrementRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS);
                        nestedScrollWebView.incrementRequestsCount(NestedScrollWebView.ULTRAPRIVACY);

                        // Update the titles of the blocklist menu items if the WebView is currently displayed.
                        if (webViewDisplayed) {
                            // Updating the UI must be run from the UI thread.
                            activity.runOnUiThread(() -> {
                                // Update the menu item titles.
                                navigationRequestsMenuItem.setTitle(getString(R.string.requests) + " - " + nestedScrollWebView.getRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS));

                                // Update the options menu if it has been populated.
                                if (optionsMenu != null) {
                                    optionsBlocklistsMenuItem.setTitle(getString(R.string.blocklists) + " - " + nestedScrollWebView.getRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS));
                                    optionsUltraPrivacyMenuItem.setTitle(nestedScrollWebView.getRequestsCount(NestedScrollWebView.ULTRAPRIVACY) + " - " + getString(R.string.ultraprivacy));
                                }
                            });
                        }

                        // The resource request was blocked.  Return an empty web resource response.
                        return emptyWebResourceResponse;
                    } else if (ultraPrivacyResults[0].equals(BlocklistHelper.REQUEST_ALLOWED)) {  // The resource request matched UltraPrivacy's whitelist.
                        // Add a whitelist entry to the resource requests array.
                        nestedScrollWebView.addResourceRequest(new String[] {ultraPrivacyResults[0], ultraPrivacyResults[1], ultraPrivacyResults[2], ultraPrivacyResults[3], ultraPrivacyResults[4],
                                ultraPrivacyResults[5]});

                        // The resource request has been allowed by UltraPrivacy.  `return null` loads the requested resource.
                        return null;
                    }
                }

                // Check EasyList if it is enabled.
                if (nestedScrollWebView.getEasyListEnabled()) {
                    // Check the URL against EasyList.
                    String[] easyListResults = blocklistHelper.checkBlocklist(currentDomain, url, isThirdPartyRequest, easyList);

                    // Process the EasyList results.
                    if (easyListResults[0].equals(BlocklistHelper.REQUEST_BLOCKED)) {  // The resource request matched EasyList's blacklist.
                        // Add the result to the resource requests.
                        nestedScrollWebView.addResourceRequest(new String[] {easyListResults[0], easyListResults[1], easyListResults[2], easyListResults[3], easyListResults[4], easyListResults[5]});

                        // Increment the blocked requests counters.
                        nestedScrollWebView.incrementRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS);
                        nestedScrollWebView.incrementRequestsCount(NestedScrollWebView.EASYLIST);

                        // Update the titles of the blocklist menu items if the WebView is currently displayed.
                        if (webViewDisplayed) {
                            // Updating the UI must be run from the UI thread.
                            activity.runOnUiThread(() -> {
                                // Update the menu item titles.
                                navigationRequestsMenuItem.setTitle(getString(R.string.requests) + " - " + nestedScrollWebView.getRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS));

                                // Update the options menu if it has been populated.
                                if (optionsMenu != null) {
                                    optionsBlocklistsMenuItem.setTitle(getString(R.string.blocklists) + " - " + nestedScrollWebView.getRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS));
                                    optionsEasyListMenuItem.setTitle(nestedScrollWebView.getRequestsCount(NestedScrollWebView.EASYLIST) + " - " + getString(R.string.easylist));
                                }
                            });
                        }

                        // The resource request was blocked.  Return an empty web resource response.
                        return emptyWebResourceResponse;
                    } else if (easyListResults[0].equals(BlocklistHelper.REQUEST_ALLOWED)) {  // The resource request matched EasyList's whitelist.
                        // Update the whitelist result string array tracker.
                        whitelistResultStringArray = new String[] {easyListResults[0], easyListResults[1], easyListResults[2], easyListResults[3], easyListResults[4], easyListResults[5]};
                    }
                }

                // Check EasyPrivacy if it is enabled.
                if (nestedScrollWebView.getEasyPrivacyEnabled()) {
                    // Check the URL against EasyPrivacy.
                    String[] easyPrivacyResults = blocklistHelper.checkBlocklist(currentDomain, url, isThirdPartyRequest, easyPrivacy);

                    // Process the EasyPrivacy results.
                    if (easyPrivacyResults[0].equals(BlocklistHelper.REQUEST_BLOCKED)) {  // The resource request matched EasyPrivacy's blacklist.
                        // Add the result to the resource requests.
                        nestedScrollWebView.addResourceRequest(new String[] {easyPrivacyResults[0], easyPrivacyResults[1], easyPrivacyResults[2], easyPrivacyResults[3], easyPrivacyResults[4],
                                easyPrivacyResults[5]});

                        // Increment the blocked requests counters.
                        nestedScrollWebView.incrementRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS);
                        nestedScrollWebView.incrementRequestsCount(NestedScrollWebView.EASYPRIVACY);

                        // Update the titles of the blocklist menu items if the WebView is currently displayed.
                        if (webViewDisplayed) {
                            // Updating the UI must be run from the UI thread.
                            activity.runOnUiThread(() -> {
                                // Update the menu item titles.
                                navigationRequestsMenuItem.setTitle(getString(R.string.requests) + " - " + nestedScrollWebView.getRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS));

                                // Update the options menu if it has been populated.
                                if (optionsMenu != null) {
                                    optionsBlocklistsMenuItem.setTitle(getString(R.string.blocklists) + " - " + nestedScrollWebView.getRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS));
                                    optionsEasyPrivacyMenuItem.setTitle(nestedScrollWebView.getRequestsCount(NestedScrollWebView.EASYPRIVACY) + " - " + getString(R.string.easyprivacy));
                                }
                            });
                        }

                        // The resource request was blocked.  Return an empty web resource response.
                        return emptyWebResourceResponse;
                    } else if (easyPrivacyResults[0].equals(BlocklistHelper.REQUEST_ALLOWED)) {  // The resource request matched EasyPrivacy's whitelist.
                        // Update the whitelist result string array tracker.
                        whitelistResultStringArray = new String[] {easyPrivacyResults[0], easyPrivacyResults[1], easyPrivacyResults[2], easyPrivacyResults[3], easyPrivacyResults[4], easyPrivacyResults[5]};
                    }
                }

                // Check Fanboy’s Annoyance List if it is enabled.
                if (nestedScrollWebView.getFanboysAnnoyanceListEnabled()) {
                    // Check the URL against Fanboy's Annoyance List.
                    String[] fanboysAnnoyanceListResults = blocklistHelper.checkBlocklist(currentDomain, url, isThirdPartyRequest, fanboysAnnoyanceList);

                    // Process the Fanboy's Annoyance List results.
                    if (fanboysAnnoyanceListResults[0].equals(BlocklistHelper.REQUEST_BLOCKED)) {  // The resource request matched Fanboy's Annoyance List's blacklist.
                        // Add the result to the resource requests.
                        nestedScrollWebView.addResourceRequest(new String[] {fanboysAnnoyanceListResults[0], fanboysAnnoyanceListResults[1], fanboysAnnoyanceListResults[2], fanboysAnnoyanceListResults[3],
                                fanboysAnnoyanceListResults[4], fanboysAnnoyanceListResults[5]});

                        // Increment the blocked requests counters.
                        nestedScrollWebView.incrementRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS);
                        nestedScrollWebView.incrementRequestsCount(NestedScrollWebView.FANBOYS_ANNOYANCE_LIST);

                        // Update the titles of the blocklist menu items if the WebView is currently displayed.
                        if (webViewDisplayed) {
                            // Updating the UI must be run from the UI thread.
                            activity.runOnUiThread(() -> {
                                // Update the menu item titles.
                                navigationRequestsMenuItem.setTitle(getString(R.string.requests) + " - " + nestedScrollWebView.getRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS));

                                // Update the options menu if it has been populated.
                                if (optionsMenu != null) {
                                    optionsBlocklistsMenuItem.setTitle(getString(R.string.blocklists) + " - " + nestedScrollWebView.getRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS));
                                    optionsFanboysAnnoyanceListMenuItem.setTitle(nestedScrollWebView.getRequestsCount(NestedScrollWebView.FANBOYS_ANNOYANCE_LIST) + " - " +
                                            getString(R.string.fanboys_annoyance_list));
                                }
                            });
                        }

                        // The resource request was blocked.  Return an empty web resource response.
                        return emptyWebResourceResponse;
                    } else if (fanboysAnnoyanceListResults[0].equals(BlocklistHelper.REQUEST_ALLOWED)){  // The resource request matched Fanboy's Annoyance List's whitelist.
                        // Update the whitelist result string array tracker.
                        whitelistResultStringArray = new String[] {fanboysAnnoyanceListResults[0], fanboysAnnoyanceListResults[1], fanboysAnnoyanceListResults[2], fanboysAnnoyanceListResults[3],
                                fanboysAnnoyanceListResults[4], fanboysAnnoyanceListResults[5]};
                    }
                } else if (nestedScrollWebView.getFanboysSocialBlockingListEnabled()) {  // Only check Fanboy’s Social Blocking List if Fanboy’s Annoyance List is disabled.
                    // Check the URL against Fanboy's Annoyance List.
                    String[] fanboysSocialListResults = blocklistHelper.checkBlocklist(currentDomain, url, isThirdPartyRequest, fanboysSocialList);

                    // Process the Fanboy's Social Blocking List results.
                    if (fanboysSocialListResults[0].equals(BlocklistHelper.REQUEST_BLOCKED)) {  // The resource request matched Fanboy's Social Blocking List's blacklist.
                        // Add the result to the resource requests.
                        nestedScrollWebView.addResourceRequest(new String[] {fanboysSocialListResults[0], fanboysSocialListResults[1], fanboysSocialListResults[2], fanboysSocialListResults[3],
                                fanboysSocialListResults[4], fanboysSocialListResults[5]});

                        // Increment the blocked requests counters.
                        nestedScrollWebView.incrementRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS);
                        nestedScrollWebView.incrementRequestsCount(NestedScrollWebView.FANBOYS_SOCIAL_BLOCKING_LIST);

                        // Update the titles of the blocklist menu items if the WebView is currently displayed.
                        if (webViewDisplayed) {
                            // Updating the UI must be run from the UI thread.
                            activity.runOnUiThread(() -> {
                                // Update the menu item titles.
                                navigationRequestsMenuItem.setTitle(getString(R.string.requests) + " - " + nestedScrollWebView.getRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS));

                                // Update the options menu if it has been populated.
                                if (optionsMenu != null) {
                                    optionsBlocklistsMenuItem.setTitle(getString(R.string.blocklists) + " - " + nestedScrollWebView.getRequestsCount(NestedScrollWebView.BLOCKED_REQUESTS));
                                    optionsFanboysSocialBlockingListMenuItem.setTitle(nestedScrollWebView.getRequestsCount(NestedScrollWebView.FANBOYS_SOCIAL_BLOCKING_LIST) + " - " +
                                            getString(R.string.fanboys_social_blocking_list));
                                }
                            });
                        }

                        // The resource request was blocked.  Return an empty web resource response.
                        return emptyWebResourceResponse;
                    } else if (fanboysSocialListResults[0].equals(BlocklistHelper.REQUEST_ALLOWED)) {  // The resource request matched Fanboy's Social Blocking List's whitelist.
                        // Update the whitelist result string array tracker.
                        whitelistResultStringArray = new String[] {fanboysSocialListResults[0], fanboysSocialListResults[1], fanboysSocialListResults[2], fanboysSocialListResults[3],
                                fanboysSocialListResults[4], fanboysSocialListResults[5]};
                    }
                }

                // Add the request to the log because it hasn't been processed by any of the previous checks.
                if (whitelistResultStringArray != null) {  // The request was processed by a whitelist.
                    nestedScrollWebView.addResourceRequest(whitelistResultStringArray);
                } else {  // The request didn't match any blocklist entry.  Log it as a default request.
                    nestedScrollWebView.addResourceRequest(new String[]{BlocklistHelper.REQUEST_DEFAULT, url});
                }

                // The resource request has not been blocked.  `return null` loads the requested resource.
                return null;
            }

            // Handle HTTP authentication requests.
            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                // Store the handler.
                nestedScrollWebView.setHttpAuthHandler(handler);

                // Instantiate an HTTP authentication dialog.
                DialogFragment httpAuthenticationDialogFragment = HttpAuthenticationDialog.displayDialog(host, realm, nestedScrollWebView.getWebViewFragmentId());

                // Show the HTTP authentication dialog.
                httpAuthenticationDialogFragment.show(getSupportFragmentManager(), getString(R.string.http_authentication));
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                // Get the app bar layout height.  This can't be done in `applyAppSettings()` because the app bar is not yet populated there.
                // This should only be populated if it is greater than 0 because otherwise it will be reset to 0 if the app bar is hidden in full screen browsing mode.
                if (appBarLayout.getHeight() > 0) appBarHeight = appBarLayout.getHeight();

                // Set the padding and layout settings according to the position of the app bar.
                if (bottomAppBar) {  // The app bar is on the bottom.
                    // Adjust the UI.
                    if (scrollAppBar || (inFullScreenBrowsingMode && hideAppBar)) {  // The app bar scrolls or full screen browsing mode is engaged with the app bar hidden.
                        // Reset the WebView padding to fill the available space.
                        swipeRefreshLayout.setPadding(0, 0, 0, 0);
                    } else {  // The app bar doesn't scroll or full screen browsing mode is not engaged with the app bar hidden.
                        // Move the WebView above the app bar layout.
                        swipeRefreshLayout.setPadding(0, 0, 0, appBarHeight);
                    }
                } else {  // The app bar is on the top.
                    // Set the top padding of the swipe refresh layout according to the app bar scrolling preference.  This can't be done in `appAppSettings()` because the app bar is not yet populated there.
                    if (scrollAppBar || (inFullScreenBrowsingMode && hideAppBar)) {
                        // No padding is needed because it will automatically be placed below the app bar layout due to the scrolling layout behavior.
                        swipeRefreshLayout.setPadding(0, 0, 0, 0);

                        // The swipe to refresh circle doesn't always hide itself completely unless it is moved up 10 pixels.
                        swipeRefreshLayout.setProgressViewOffset(false, defaultProgressViewStartOffset - 10, defaultProgressViewEndOffset);
                    } else {
                        // The swipe refresh layout must be manually moved below the app bar layout.
                        swipeRefreshLayout.setPadding(0, appBarHeight, 0, 0);

                        // The swipe to refresh circle doesn't always hide itself completely unless it is moved up 10 pixels.
                        swipeRefreshLayout.setProgressViewOffset(false, defaultProgressViewStartOffset - 10 + appBarHeight, defaultProgressViewEndOffset + appBarHeight);
                    }
                }

                // Reset the list of resource requests.
                nestedScrollWebView.clearResourceRequests();

                // Reset the requests counters.
                nestedScrollWebView.resetRequestsCounters();

                // Get the current page position.
                int currentPagePosition = webViewPagerAdapter.getPositionForId(nestedScrollWebView.getWebViewFragmentId());

                // Update the URL text bar if the page is currently selected and the URL edit text is not currently being edited.
                if ((tabLayout.getSelectedTabPosition() == currentPagePosition) && !urlEditText.hasFocus()) {
                    // Display the formatted URL text.
                    urlEditText.setText(url);

                    // Highlight the URL syntax.
                    UrlHelper.highlightSyntax(urlEditText, initialGrayColorSpan, finalGrayColorSpan, redColorSpan);

                    // Hide the keyboard.
                    inputMethodManager.hideSoftInputFromWindow(nestedScrollWebView.getWindowToken(), 0);
                }

                // Reset the list of host IP addresses.
                nestedScrollWebView.setCurrentIpAddresses("");

                // Get a URI for the current URL.
                Uri currentUri = Uri.parse(url);

                // Get the current domain name.
                String currentDomainName = currentUri.getHost();

                if ((currentDomainName != null) && !currentDomainName.isEmpty()) {
                    // Get the IP addresses for the current URI.
                    GetHostIpAddressesCoroutine.getAddresses(currentDomainName, nestedScrollWebView, getSupportFragmentManager(), getString(R.string.pinned_mismatch));
                }

                // Replace Refresh with Stop if the options menu has been created.  (The first WebView typically begins loading before the menu items are instantiated.)
                if (optionsMenu != null) {
                    // Set the title.
                    optionsRefreshMenuItem.setTitle(R.string.stop);

                    // Set the icon if it is displayed in the AppBar.
                    if (displayAdditionalAppBarIcons)
                        optionsRefreshMenuItem.setIcon(R.drawable.close_blue);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // Flush any cookies to persistent storage.  The cookie manager has become very lazy about flushing cookies in recent versions.
                if (nestedScrollWebView.getAcceptCookies()) {
                    CookieManager.getInstance().flush();
                }

                // Update the Refresh menu item if the options menu has been created.
                if (optionsMenu != null) {
                    // Reset the Refresh title.
                    optionsRefreshMenuItem.setTitle(R.string.refresh);

                    // Reset the icon if it is displayed in the app bar.
                    if (displayAdditionalAppBarIcons)
                        optionsRefreshMenuItem.setIcon(R.drawable.refresh_enabled);
                }

                 // Get the application's private data directory, which will be something like `/data/user/0/com.stoutner.privacybrowser.standard`,
                // which links to `/data/data/com.stoutner.privacybrowser.standard`.
                String privateDataDirectoryString = getApplicationInfo().dataDir;

                // Clear the cache, history, and logcat if Incognito Mode is enabled.
                if (incognitoModeEnabled) {
                    // Clear the cache.  `true` includes disk files.
                    nestedScrollWebView.clearCache(true);

                    // Clear the back/forward history.
                    nestedScrollWebView.clearHistory();

                    // Manually delete cache folders.
                    try {
                        // Delete the main cache directory.
                        Runtime.getRuntime().exec("rm -rf " + privateDataDirectoryString + "/cache");
                    } catch (IOException exception) {
                        // Do nothing if an error is thrown.
                    }

                    // Clear the logcat.
                    try {
                        // Clear the logcat.  `-c` clears the logcat.  `-b all` clears all the buffers (instead of just crash, main, and system).
                        Runtime.getRuntime().exec("logcat -b all -c");
                    } catch (IOException exception) {
                        // Do nothing.
                    }
                }

                // Clear the `Service Worker` directory.
                try {
                    // A `String[]` must be used because the directory contains a space and `Runtime.exec` will not escape the string correctly otherwise.
                    Runtime.getRuntime().exec(new String[]{"rm", "-rf", privateDataDirectoryString + "/app_webview/Default/Service Worker/"});
                } catch (IOException exception) {
                    // Do nothing.
                }

                // Get the current page position.
                int currentPagePosition = webViewPagerAdapter.getPositionForId(nestedScrollWebView.getWebViewFragmentId());

                // Get the current URL from the nested scroll WebView.  This is more accurate than using the URL passed into the method, which is sometimes not the final one.
                String currentUrl = nestedScrollWebView.getUrl();

                // Get the current tab.
                TabLayout.Tab tab = tabLayout.getTabAt(currentPagePosition);

                // Update the URL text bar if the page is currently selected and the user is not currently typing in the URL edit text.
                // Crash records show that, in some crazy way, it is possible for the current URL to be blank at this point.
                // Probably some sort of race condition when Privacy Browser is being resumed.
                if ((tabLayout.getSelectedTabPosition() == currentPagePosition) && !urlEditText.hasFocus() && (currentUrl != null)) {
                    // Check to see if the URL is `about:blank`.
                    if (currentUrl.equals("about:blank")) {  // The WebView is blank.
                        // Display the hint in the URL edit text.
                        urlEditText.setText("");

                        // Request focus for the URL text box.
                        urlEditText.requestFocus();

                        // Display the keyboard.
                        inputMethodManager.showSoftInput(urlEditText, 0);

                        // Apply the domain settings.  This clears any settings from the previous domain.
                        applyDomainSettings(nestedScrollWebView, "", true, false, false);

                        // Only populate the title text view if the tab has been fully created.
                        if (tab != null) {
                            // Get the custom view from the tab.
                            View tabView = tab.getCustomView();

                            // Remove the incorrect warning below that the current tab view might be null.
                            assert tabView != null;

                            // Get the title text view from the tab.
                            TextView tabTitleTextView = tabView.findViewById(R.id.title_textview);

                            // Set the title as the tab text.
                            tabTitleTextView.setText(R.string.new_tab);
                        }
                    } else {  // The WebView has loaded a webpage.
                        // Update the URL edit text if it is not currently being edited.
                        if (!urlEditText.hasFocus()) {
                            // Sanitize the current URL.  This removes unwanted URL elements that were added by redirects, so that they won't be included if the URL is shared.
                            String sanitizedUrl = sanitizeUrl(currentUrl);

                            // Display the final URL.  Getting the URL from the WebView instead of using the one provided by `onPageFinished()` makes websites like YouTube function correctly.
                            urlEditText.setText(sanitizedUrl);

                            // Highlight the URL syntax.
                            UrlHelper.highlightSyntax(urlEditText, initialGrayColorSpan, finalGrayColorSpan, redColorSpan);
                        }

                        // Only populate the title text view if the tab has been fully created.
                        if (tab != null) {
                            // Get the custom view from the tab.
                            View tabView = tab.getCustomView();

                            // Remove the incorrect warning below that the current tab view might be null.
                            assert tabView != null;

                            // Get the title text view from the tab.
                            TextView tabTitleTextView = tabView.findViewById(R.id.title_textview);

                            // Set the title as the tab text.  Sometimes `onReceivedTitle()` is not called, especially when navigating history.
                            tabTitleTextView.setText(nestedScrollWebView.getTitle());
                        }
                    }
                }
            }

            // Handle SSL Certificate errors.  Suppress the lint warning that ignoring the error might be dangerous.
            @SuppressLint("WebViewClientOnReceivedSslError")
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // Get the current website SSL certificate.
                SslCertificate currentWebsiteSslCertificate = error.getCertificate();

                // Extract the individual pieces of information from the current website SSL certificate.
                String currentWebsiteIssuedToCName = currentWebsiteSslCertificate.getIssuedTo().getCName();
                String currentWebsiteIssuedToOName = currentWebsiteSslCertificate.getIssuedTo().getOName();
                String currentWebsiteIssuedToUName = currentWebsiteSslCertificate.getIssuedTo().getUName();
                String currentWebsiteIssuedByCName = currentWebsiteSslCertificate.getIssuedBy().getCName();
                String currentWebsiteIssuedByOName = currentWebsiteSslCertificate.getIssuedBy().getOName();
                String currentWebsiteIssuedByUName = currentWebsiteSslCertificate.getIssuedBy().getUName();
                Date currentWebsiteSslStartDate = currentWebsiteSslCertificate.getValidNotBeforeDate();
                Date currentWebsiteSslEndDate = currentWebsiteSslCertificate.getValidNotAfterDate();

                // Proceed to the website if the current SSL website certificate matches the pinned domain certificate.
                if (nestedScrollWebView.hasPinnedSslCertificate()) {
                    // Get the pinned SSL certificate.
                    Pair<String[], Date[]> pinnedSslCertificatePair = nestedScrollWebView.getPinnedSslCertificate();

                    // Extract the arrays from the array list.
                    String[] pinnedSslCertificateStringArray = pinnedSslCertificatePair.getFirst();
                    Date[] pinnedSslCertificateDateArray = pinnedSslCertificatePair.getSecond();

                    // Check if the current SSL certificate matches the pinned certificate.
                    if (currentWebsiteIssuedToCName.equals(pinnedSslCertificateStringArray[0]) && currentWebsiteIssuedToOName.equals(pinnedSslCertificateStringArray[1]) &&
                        currentWebsiteIssuedToUName.equals(pinnedSslCertificateStringArray[2]) && currentWebsiteIssuedByCName.equals(pinnedSslCertificateStringArray[3]) &&
                        currentWebsiteIssuedByOName.equals(pinnedSslCertificateStringArray[4]) && currentWebsiteIssuedByUName.equals(pinnedSslCertificateStringArray[5]) &&
                        currentWebsiteSslStartDate.equals(pinnedSslCertificateDateArray[0]) && currentWebsiteSslEndDate.equals(pinnedSslCertificateDateArray[1])) {

                        // An SSL certificate is pinned and matches the current domain certificate.  Proceed to the website without displaying an error.
                        handler.proceed();
                    }
                } else {  // Either there isn't a pinned SSL certificate or it doesn't match the current website certificate.
                    // Store the SSL error handler.
                    nestedScrollWebView.setSslErrorHandler(handler);

                    // Instantiate an SSL certificate error alert dialog.
                    DialogFragment sslCertificateErrorDialogFragment = SslCertificateErrorDialog.displayDialog(error, nestedScrollWebView.getWebViewFragmentId());

                    // Try to show the dialog.  The SSL error handler continues to function even when the WebView is paused.  Attempting to display a dialog in that state leads to a crash.
                    try {
                        // Show the SSL certificate error dialog.
                        sslCertificateErrorDialogFragment.show(getSupportFragmentManager(), getString(R.string.ssl_certificate_error));
                    } catch (Exception exception) {
                        // Add the dialog to the pending dialog array list.  It will be displayed in `onStart()`.
                        pendingDialogsArrayList.add(new PendingDialogDataClass(sslCertificateErrorDialogFragment, getString(R.string.ssl_certificate_error)));
                    }
                }
            }
        });

        // Check to see if the state is being restored.
        if (restoringState) {  // The state is being restored.
            // Resume the nested scroll WebView JavaScript timers.
            nestedScrollWebView.resumeTimers();
        } else if (pageNumber == 0) {  // The first page is being loaded.
            // Set this nested scroll WebView as the current WebView.
            currentWebView = nestedScrollWebView;

            // Initialize the URL to load string.
            String urlToLoadString;

            // Get the intent that started the app.
            Intent launchingIntent = getIntent();

            // Reset the intent.  This prevents a duplicate tab from being created on restart.
            setIntent(new Intent());

            // Get the information from the intent.
            String launchingIntentAction = launchingIntent.getAction();
            Uri launchingIntentUriData = launchingIntent.getData();
            String launchingIntentStringExtra = launchingIntent.getStringExtra(Intent.EXTRA_TEXT);

            // Parse the launching intent URL.
            if ((launchingIntentAction != null) && launchingIntentAction.equals(Intent.ACTION_WEB_SEARCH)) {  // The intent contains a search string.
                // Create an encoded URL string.
                String encodedUrlString;

                // Sanitize the search input and convert it to a search.
                try {
                    encodedUrlString = URLEncoder.encode(launchingIntent.getStringExtra(SearchManager.QUERY), "UTF-8");
                } catch (UnsupportedEncodingException exception) {
                    encodedUrlString = "";
                }

                // Store the web search as the URL to load.
                urlToLoadString = searchURL + encodedUrlString;
            } else if (launchingIntentUriData != null) {  // The launching intent contains a URL formatted as a URI.
                // Store the URI as a URL.
                urlToLoadString = launchingIntentUriData.toString();
            } else if (launchingIntentStringExtra != null) {  // The launching intent contains text that might be a URL.
                // Store the URL.
                urlToLoadString = launchingIntentStringExtra;
            } else if (!url.equals("")) {  // The activity has been restarted.
                // Load the saved URL.
                urlToLoadString = url;
            } else {  // The is no URL in the intent.
                // Store the homepage to be loaded.
                urlToLoadString = sharedPreferences.getString("homepage", getString(R.string.homepage_default_value));
            }

            // Load the website if not waiting for the proxy.
            if (waitingForProxy) {  // Store the URL to be loaded in the Nested Scroll WebView.
                nestedScrollWebView.setWaitingForProxyUrlString(urlToLoadString);
            } else {  // Load the URL.
                loadUrl(nestedScrollWebView, urlToLoadString);
            }

            // Reset the intent.  This prevents a duplicate tab from being created on a subsequent restart if loading an link from a new intent on restart.
            // For example, this prevents a duplicate tab if a link is loaded from the Guide after changing the theme in the guide and then changing the theme again in the main activity.
            setIntent(new Intent());
        } else {  // This is not the first tab.
            // Load the URL.
            loadUrl(nestedScrollWebView, url);

            // Set the focus and display the keyboard if the URL is blank.
            if (url.equals("")) {
                // Request focus for the URL text box.
                urlEditText.requestFocus();

                // Create a display keyboard handler.
                Handler displayKeyboardHandler = new Handler();

                // Create a display keyboard runnable.
                Runnable displayKeyboardRunnable = () -> {
                    // Display the keyboard.
                    inputMethodManager.showSoftInput(urlEditText, 0);
                };

                // Display the keyboard after 100 milliseconds, which leaves enough time for the tab to transition.
                displayKeyboardHandler.postDelayed(displayKeyboardRunnable, 100);
            }
        }
    }
}
