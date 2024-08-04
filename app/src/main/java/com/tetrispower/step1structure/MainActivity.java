package com.tetrispower.step1structure;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.FragmentActivity;

// IMA SDK Imports
import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRenderingSettings;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.jetgame.tetris.FragmentTetris;

import java.util.Arrays;
import java.util.Calendar;

public class MainActivity extends FragmentActivity {
    // Show Splash State Manager
    private boolean showSplash = true;


    // WebView Variable Preparation
    private WebView webView;

    // Platforms Buttons and Labels
    private RelativeLayout youtubeButtonContainer;
    private RelativeLayout placeholderButtonContainer;
    private TextView title;
    private TextView selectPlatform;

    // Track Last Selected Button + Set to Youtube By Default
    private int lastSelectedButtonId = R.id.youtube_button_container;

    // IMA SDK
    // Defining Log String
    private static final String LOGTAG = "IMA SDK Log";
    // Defining Video URL (Mandatory) and VAST Tag URL
    private static final String PreRoll_VIDEO_URL =
            "https://ssaitest.s3.us-west-2.amazonaws.com/tetrispower.mp4";

    private static final String SAMPLE_VAST_TAG_URL =
            "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/"
                    + "single_preroll_skippable&sz=640x480&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast"
                    + "&unviewed_position_start=1&env=vp&impl=s&correlator=";


    // Factory class for creating SDK objects.
    private ImaSdkFactory sdkFactory;

    // The AdsLoader instance exposes the requestAds method.
    private AdsLoader adsLoader;

    // AdsManager exposes methods to control ad playback and listen to ad events.
    private AdsManager adsManager;

    // Media Variables
    private VideoView videoPlayer;
    private MediaController mediaController;
    private VideoAdPlayerAdapter videoAdPlayerAdapter;
    // End of IMA SDK Variables

    FragmentTetris fragmentTetris;

    boolean isGameActive = false;

    int SuccessiveKeyPresses = 0;

    long LastKeyPress = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
            {
                if (isGameActive) fragmentTetris.getViewModel().actionReset();
                return true;
            }
            case KeyEvent.KEYCODE_2:{
                if (isGameActive) fragmentTetris.getViewModel().actionUp();
                return true;
            }
            case KeyEvent.KEYCODE_3:{
                if (isGameActive) fragmentTetris.getViewModel().actionLeft();
                return true;
            }
            case KeyEvent.KEYCODE_4:{
                if (isGameActive) fragmentTetris.getViewModel().actionRight();
                return true;
            }
            case KeyEvent.KEYCODE_5:{
                if (isGameActive) fragmentTetris.getViewModel().actionDown();
                return true;
            }
            case KeyEvent.KEYCODE_6:{
                if (isGameActive) fragmentTetris.getViewModel().actionRotate();
                return true;
            }
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_8:
            {
                long CurrentKeyPress = Calendar.getInstance().getTimeInMillis();

                if (LastKeyPress == 0) {
                    LastKeyPress = CurrentKeyPress;

                    SuccessiveKeyPresses = 2;
                } else if ((CurrentKeyPress - LastKeyPress) <= 100) {
                    LastKeyPress = CurrentKeyPress;

                    ++SuccessiveKeyPresses;
                } else {
                    LastKeyPress = CurrentKeyPress;

                    SuccessiveKeyPresses = 1;
                }

                if (SuccessiveKeyPresses > 1) {
                    if (!isGameActive) {
                        isGameActive = true;

                        findViewById(R.id.game_frame).setVisibility(View.VISIBLE);
                        findViewById(R.id.youtube_frame).setVisibility(View.GONE);

                        fragmentTetris.getViewModel().actionResume();
                    } else {
                        isGameActive = false;

                        findViewById(R.id.game_frame).setVisibility(View.GONE);
                        findViewById(R.id.youtube_frame).setVisibility(View.VISIBLE);

                        fragmentTetris.getViewModel().actionPause();
                    }

                    SuccessiveKeyPresses = 0;
                }

                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    // OnCreate Main Method

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Splash Screen Initialization
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        // Setting the View
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        // Hide Splash and start IMA SDK after 5 Seconds
        splashScreen.setKeepOnScreenCondition(() -> showSplash);
        Handler handler = new Handler();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    showSplash = false;
                    setupIMASDK();
                }, 5000
        );

        fragmentTetris = (FragmentTetris) getSupportFragmentManager().findFragmentById(R.id.tetris);

        if (!isGameActive) {
            isGameActive = true;

            findViewById(R.id.game_frame).setVisibility(View.VISIBLE);
            findViewById(R.id.youtube_frame).setVisibility(View.GONE);
        } else {
            isGameActive = false;

            findViewById(R.id.game_frame).setVisibility(View.GONE);
            findViewById(R.id.youtube_frame).setVisibility(View.VISIBLE);
        }

        // Getting layout items by ID
        webView = findViewById(R.id.webview);
        youtubeButtonContainer = findViewById(R.id.youtube_button_container);
        placeholderButtonContainer = findViewById(R.id.placeholder_button_container);
        title = findViewById(R.id.title);
        selectPlatform = findViewById(R.id.selectPlatform);

        // Set initial focus
        findViewById(lastSelectedButtonId).requestFocus();

        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setJavaScriptEnabled(true);

        // Set WebViewClient with error handling
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Toast.makeText(MainActivity.this, "Failed to load page", Toast.LENGTH_SHORT).show();
                webView.setVisibility(View.GONE);
                youtubeButtonContainer.setVisibility(View.VISIBLE);
                placeholderButtonContainer.setVisibility(View.VISIBLE);
                title.setVisibility(View.VISIBLE);
                selectPlatform.setVisibility(View.VISIBLE);
                findViewById(lastSelectedButtonId).requestFocus();
            }
        });
    }

    // Setting the LastSelectedButton after button was clicked and Setting WebView URL
    public void onButtonClick(View view) {
        String url = "";
        lastSelectedButtonId = view.getId(); // Store the ID of the selected button
        if (view.getId() == R.id.youtube_button_container) {
            url = "https://youtube.com/shorts";
        } else if (view.getId() == R.id.placeholder_button_container) {
            url = "https://tiktok.com/login/qrcode";
        }
        loadWebView(url);
    }

    // Loading WebView
    private void loadWebView(String url) {
        // Hide buttons and texts
        youtubeButtonContainer.setVisibility(View.GONE);
        placeholderButtonContainer.setVisibility(View.GONE);
        title.setVisibility(View.GONE);
        selectPlatform.setVisibility(View.GONE);

        // Clear WebView history and state before loading new URL
        webView.clearHistory();
        webView.clearCache(true);
        webView.loadUrl("about:blank");

        // Configure and show WebView
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Toast.makeText(MainActivity.this, "Failed to load page", Toast.LENGTH_SHORT).show();
                webView.setVisibility(View.GONE);
                youtubeButtonContainer.setVisibility(View.VISIBLE);
                placeholderButtonContainer.setVisibility(View.VISIBLE);
                title.setVisibility(View.VISIBLE);
                selectPlatform.setVisibility(View.VISIBLE);
                findViewById(lastSelectedButtonId).requestFocus();
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(url);
        webView.setVisibility(View.VISIBLE);
    }

    // Handling back pressed to delete WebView data and return to home screen
    @Override
    public void onBackPressed() {
        if (webView.getVisibility() == View.VISIBLE) {
            // Stop WebView, clear its cache and history
            webView.stopLoading();
            webView.clearCache(true);
            webView.clearHistory();
            webView.loadUrl("about:blank");

            // Hide WebView and show buttons and texts
            webView.setVisibility(View.GONE);
            youtubeButtonContainer.setVisibility(View.VISIBLE);
            placeholderButtonContainer.setVisibility(View.VISIBLE);
            title.setVisibility(View.VISIBLE);
            selectPlatform.setVisibility(View.VISIBLE);

            // Set focus to the last selected button
            findViewById(lastSelectedButtonId).requestFocus();
        } else {
            super.onBackPressed();
        }
        super.onBackPressed();
    }

    // IMA SDK Methods
    private void setupIMASDK(){
        // Create the UI for controlling the video view.
        mediaController = new MediaController(this);
        videoPlayer = findViewById(R.id.videoView);
        mediaController.setAnchorView(videoPlayer);
        videoPlayer.setMediaController(mediaController);

        // Create an ad display container that uses a ViewGroup to listen to taps.
        ViewGroup videoPlayerContainer = findViewById(R.id.videoPlayerContainer);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        videoAdPlayerAdapter = new VideoAdPlayerAdapter(videoPlayer, audioManager);

        sdkFactory = ImaSdkFactory.getInstance();

        AdDisplayContainer adDisplayContainer =
                ImaSdkFactory.createAdDisplayContainer(videoPlayerContainer, videoAdPlayerAdapter);

        // Create an AdsLoader.
        ImaSdkSettings settings = sdkFactory.createImaSdkSettings();
        adsLoader = sdkFactory.createAdsLoader(this, settings, adDisplayContainer);

        // Add listeners for when ads are loaded and for errors.
        adsLoader.addAdErrorListener(
                new AdErrorEvent.AdErrorListener() {

                    @Override
                    public void onAdError(AdErrorEvent adErrorEvent) {
                        Log.i(LOGTAG, "Ad Error: " + adErrorEvent.getError().getMessage());
                        resumeContent();
                    }
                });
        adsLoader.addAdsLoadedListener(
                new AdsLoader.AdsLoadedListener() {
                    @Override
                    public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
                        // Ads were successfully loaded, so get the AdsManager instance. AdsManager has
                        // events for ad playback and errors.
                        adsManager = adsManagerLoadedEvent.getAdsManager();

                        // Attach event and error event listeners.
                        adsManager.addAdErrorListener(
                                new AdErrorEvent.AdErrorListener() {

                                    @Override
                                    public void onAdError(AdErrorEvent adErrorEvent) {
                                        Log.e(LOGTAG, "Ad Error: " + adErrorEvent.getError().getMessage());
                                        String universalAdIds =
                                                Arrays.toString(adsManager.getCurrentAd().getUniversalAdIds());
                                        Log.i(
                                                LOGTAG,
                                                "Discarding the current ad break with universal "
                                                        + "ad Ids: "
                                                        + universalAdIds);
                                        adsManager.discardAdBreak();
                                    }
                                });
                        adsManager.addAdEventListener(
                                new AdEvent.AdEventListener() {
                                    @Override
                                    public void onAdEvent(AdEvent adEvent) {
                                        if (adEvent.getType() != AdEvent.AdEventType.AD_PROGRESS) {
                                            Log.i(LOGTAG, "Event: " + adEvent.getType());
                                        }
                                        // These are the suggested event types to handle. For full list of
                                        // all ad event types, see AdEvent.AdEventType documentation.
                                        switch (adEvent.getType()) {
                                            case LOADED:
                                                // AdEventType.LOADED is fired when ads are ready to play.

                                                // This sample app uses the sample tag
                                                // single_preroll_skippable_ad_tag_url that requires calling
                                                // AdsManager.start() to start ad playback.
                                                // If you use a different ad tag URL that returns a VMAP or
                                                // an ad rules playlist, the adsManager.init() function will
                                                // trigger ad playback automatically and the IMA SDK will
                                                // ignore the adsManager.start().
                                                // It is safe to always call adsManager.start() in the
                                                // LOADED event.
                                                adsManager.start();
                                                break;
                                            case CONTENT_PAUSE_REQUESTED:
                                                // AdEventType.CONTENT_PAUSE_REQUESTED is fired when you
                                                // should pause your content and start playing an ad.
                                                pauseContentForAds();
                                                break;
                                            case CONTENT_RESUME_REQUESTED:
                                                // AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad
                                                // you should play your content.
                                                resumeContent();
                                                break;
                                            case ALL_ADS_COMPLETED:
                                                // Calling adsManager.destroy() triggers the function
                                                // VideoAdPlayer.release().
                                                adsManager.destroy();
                                                adsManager = null;
                                                break;
                                            case CLICKED:
                                                // When the user clicks on the Learn More button, the IMA SDK fires
                                                // this event, pauses the ad, and opens the ad's click-through URL.
                                                // When the user returns to the app, the IMA SDK calls the
                                                // VideoAdPlayer.playAd() function automatically.
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                });
                        AdsRenderingSettings adsRenderingSettings =
                                ImaSdkFactory.getInstance().createAdsRenderingSettings();
                        adsManager.init(adsRenderingSettings);
                    }
                });

        videoPlayer.setVideoPath(PreRoll_VIDEO_URL);
        requestAds(SAMPLE_VAST_TAG_URL);
    }
    // Pausing Content for Ads (We don't actually have content but it's mandatory)
    private void pauseContentForAds() {
        videoPlayer.stopPlayback();
        // Hide the buttons and seek bar controlling the video view.
        videoPlayer.setMediaController(null);
    }

    // Handle what happens after ad finish/fails
    private void resumeContent() {
        // Hide VideoPlayer
        videoPlayer.setVisibility(View.GONE);
        // Setting Focus on last selected button
        findViewById(lastSelectedButtonId).requestFocus();
    }

    // Request Ads Function
    private void requestAds(String adTagUrl) {
        // Create the ads request.
        AdsRequest request = sdkFactory.createAdsRequest();
        request.setAdTagUrl(adTagUrl);
        request.setContentProgressProvider(
                () -> {
                    if (videoPlayer.getDuration() <= 0) {
                        return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                    }
                    return new VideoProgressUpdate(
                            videoPlayer.getCurrentPosition(), videoPlayer.getDuration());
                });

        // Request the ad
        adsLoader.requestAds(request);
    }
}
