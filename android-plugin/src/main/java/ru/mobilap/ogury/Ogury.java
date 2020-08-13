package ru.mobilap.ogury;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import io.presage.Presage;
import com.ogury.cm.OguryCmConfig;
import com.ogury.cm.OguryChoiceManager;
import com.ogury.cm.OguryConsentListener;
import com.ogury.core.OguryError;

import org.godotengine.godot.Godot;
import org.godotengine.godot.GodotLib;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;

public class Ogury extends GodotPlugin
{

    private final String TAG = Ogury.class.getName();
    private Activity activity = null; // The main activity of the game
    private boolean _inited = false;

    /*
    private HashMap<String, View> zombieBanners = new HashMap<>();
    private HashMap<String, FrameLayout.LayoutParams> bannerParams = new HashMap<>();
    private HashMap<String, InterstitialWrapper> interstitials = new HashMap<>();
    private HashMap<String, TMBannerAdView> banners = new HashMap<>();
    private HashMap<String, TMAdListener> rewardeds = new HashMap<>();
    */

    private boolean ProductionMode = true; // Store if is real or not

    private FrameLayout layout = null; // Store the layout
    private String assetKey = null;

    /* Init
     * ********************************************************************** */

    /**
     * Prepare for work with MoPub
     * @param boolean ProductionMode Tell if the enviroment is for real or test
     * @param int gdscript instance id
     */
    public void init(final String _assetKey, final boolean _productionMode) {
        this.ProductionMode = _productionMode;
        this.assetKey = _assetKey;
        layout = (FrameLayout)activity.getWindow().getDecorView().getRootView();
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    Log.w(TAG, "Init with " + _assetKey);
                    OguryChoiceManager.initialize(activity, _assetKey, new OguryCmConfig());
                    OguryChoiceManager.ask(activity, getConsentListener(0, ""));
                }
            });
    }

    public void initWithCallback(final String _assetKey, final boolean _productionMode, final int callback_id, final String callback_method) {
        this.ProductionMode = _productionMode;
        this.assetKey = _assetKey;
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    Log.w(TAG, "Init with " + _assetKey);
                    OguryChoiceManager.initialize(activity, _assetKey, new OguryCmConfig());
                    OguryChoiceManager.ask(activity, getConsentListener(callback_id, callback_method));
                }
            });
    }

    private void startSdk() {
        // start the Ogury SDK
        Presage.getInstance().start(assetKey, activity);
        _inited = true;
    }

    public void editConsent(final int callback_id, final String callback_method) {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(OguryChoiceManager.isEditAvailable()) {
                        OguryChoiceManager.edit(activity, getConsentListener(callback_id, callback_method));
                    } else if(callback_id > 0) {
                        GodotLib.calldeferred(callback_id, callback_method, new Object[] { false, false, "" });
                    }
                }
            });
    }

    public boolean gdprApplies() {
        return OguryChoiceManager.gdprApplies();
    }

    public OguryConsentListener getConsentListener(final int callback_id, final String callback_method)
    {
        return new OguryConsentListener() {
            @Override
            public void onComplete(OguryChoiceManager.Answer answer) {
                // will serve personalized ads when the user consents
                boolean applies = OguryChoiceManager.gdprApplies();
                String consent = OguryChoiceManager.TcfV2.getIabString();
                boolean approval = false;
                //Log.i(TAG, "Consent value: " + consent);
                if(answer == OguryChoiceManager.Answer.FULL_APPROVAL) {
                    Log.i(TAG, "Consent result: FULL_APPROVAL");
                    approval = true;
                } else if(answer == OguryChoiceManager.Answer.PARTIAL_APPROVAL) {
                    Log.i(TAG, "Consent result: PARTIAL_APPROVAL");
                    approval = true;
                } else if(answer == OguryChoiceManager.Answer.REFUSAL) {
                    Log.i(TAG, "Consent result: REFUSAL");
                } else if(answer == OguryChoiceManager.Answer.NO_ANSWER) {
                    Log.i(TAG, "Consent result: NO_ANSWER");
                }
                startSdk();
                if(callback_id > 0) {
                    GodotLib.calldeferred(callback_id, callback_method, new Object[] { applies, approval, consent });
                }
            }
            @Override
            public void onError(OguryError error) {
                // will serve non-personalized ads
                Log.e(TAG, "Consent manager error: " + error.getMessage());
                Log.e(TAG, "Consent error code: " + error.getErrorCode());
                boolean applies = OguryChoiceManager.gdprApplies();
                String consent = OguryChoiceManager.TcfV2.getIabString();
                startSdk();
                if(callback_id > 0) {
                    GodotLib.calldeferred(callback_id, callback_method, new Object[] { applies, false, consent });
                }
            }
        };
    }

    /* Rewarded Video
     * ********************************************************************** */
    /*
    private TMAdListener makeRewardedListener(final String id, final int callback_id)
    {
        return new TMAdListener() {
            @Override
            public void didLoad() {
                // Ready to display the interstitial
                Log.w(TAG, "Rewarded: didLoad");
                GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_loaded", new Object[] { id });
            }
            @Override
            public void didClose() {
                Log.w(TAG, "Rewarded: didClose");
                GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_closed", new Object[] { id });
            }
            @Override
            public void didFailToLoad(TMAdError error) {
                Log.w(TAG, "Rewarded: didFailToLoad " + error.toString());
                rewardeds.remove(id);
                GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_failed_to_load", new Object[] { id, error.toString() });
            }
            @Override
            public void didDisplay() {
                Log.w(TAG, "Rewarded: didDisplay");
                GodotLib.calldeferred(callback_id, "_on_rewarded_video_started", new Object[] { id });
            }
            @Override
            public void didClick() {
                Log.w(TAG, "Rewarded: didClick");
                GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_left_application", new Object[] { id });
            }
            @Override
            public void didVerify(TDReward reward) {
                Log.w(TAG, String.format(Locale.ENGLISH, "didVerify: %s, %d, %b", reward.getName(), reward.getValue(), reward.isValid()));
                if(reward.isValid())
                    GodotLib.calldeferred(callback_id, "_on_rewarded", new Object[] { id, reward.getName(), reward.getValue() });
            }
            @Override
            public void didRefresh() {
                Log.w(TAG, "Rewarded: didRefresh");
                GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_loaded", new Object[]{ id });
            }
        };
    }
    */

    /**
     * Load a Rewarded Video
     * @param String id AdMod Rewarded video ID
     */
    /*
    public void loadRewardedVideo(final String id, final int callback_id) {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(_inited) {
                        if(!rewardeds.containsKey(id)) {
                            TMAdListener listener = makeRewardedListener(id, callback_id);
                            rewardeds.put(id, listener);
                            sdk.loadRewardedVideo(activity,  id, listener);
                        } else {
                            Log.i(TAG, "Rewarded already created: "+id);
                        }
                    } else {
                        Log.e(TAG, "Tapdaq not inited");
                        GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_failed_to_load", new Object[]{ id, "SDK not initialized" });
                    }
                }
            });
    }
    */

    /**
     * Show a Rewarded Video
     */
    /*
    public void showRewardedVideo(final String id) {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(rewardeds.containsKey(id) && sdk.isRewardedVideoReady(activity, id)) {
                        TMAdListener listener = rewardeds.get(id);
                        sdk.showRewardedVideo(activity, id, listener);
                    } else {
                        Log.w(TAG, "Rewarded not found: " + id);
                    }
                }
            });
    }
    */

    /* Banner
     * ********************************************************************** */
    /*

    private TMBannerAdView initBanner(final String id, final boolean isOnTop, final int callback_id)
    {
        FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
                                                                         FrameLayout.LayoutParams.MATCH_PARENT,
                                                                         FrameLayout.LayoutParams.WRAP_CONTENT
                                                                         );
        if(isOnTop) adParams.gravity = Gravity.TOP;
        else adParams.gravity = Gravity.BOTTOM;
        bannerParams.put(id, adParams);
                
        TMBannerAdView banner = new TMBannerAdView(activity);
        banner.setBackgroundColor(Color.TRANSPARENT);

        TMAdListener listener = new TMAdListener() {
                @Override
                public void didLoad() {
                    // First banner loaded into view
                    Log.w(TAG, "Banner: didLoad");
                    GodotLib.calldeferred(callback_id, "_on_banner_loaded", new Object[]{ id });
                }
                @Override
                public void didFailToLoad(TMAdError error) {
                    // No banners available. View will stop refreshing
                    Log.w(TAG, "Banner: didFailToLoad");
                    GodotLib.calldeferred(callback_id, "_on_banner_failed_to_load", new Object[]{ id, error.toString() });
                }
                @Override
                public void didRefresh() {
                    // Subequent banner loaded, this view will refresh every 30 seconds
                    Log.w(TAG, "Banner: didRefresh");
                    GodotLib.calldeferred(callback_id, "_on_banner_loaded", new Object[]{ id });
                }
                @Override
                public void didFailToRefresh(TMAdError error) {
                    // Banner could not load, this view will attempt another refresh every 30 seconds
                    Log.w(TAG, "Banner: didFailToRefresh");
                }
                @Override
                public void didClick() {
                    // User clicked on banner
                    Log.w(TAG, "Banner: didClick");
                }
            };

        // Request
        banner.load(activity, id, TMBannerAdSizes.STANDARD, listener);
        return banner;
    }

    private void placeBannerOnScreen(final String id, final TMBannerAdView banner)
    {
        FrameLayout.LayoutParams adParams = bannerParams.get(id);
        layout.addView(banner, adParams);
    }
    */

    /**
     * Load a banner
     * @param String id AdMod Banner ID
     * @param boolean isOnTop To made the banner top or bottom
     */
    /*
    public void loadBanner(final String id, final boolean isOnTop, final int callback_id)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(_inited) {
                        if(!banners.containsKey(id)) {
                            TMBannerAdView b = initBanner(id, isOnTop, callback_id);
                            banners.put(id, b);
                        } else {
                            Log.w(TAG, "Banner already loaded: " + id);
                        }
                    } else {
                        Log.e(TAG, "Tapdaq not inited");
                        GodotLib.calldeferred(callback_id, "_on_banner_failed_to_load", new Object[]{ id, "SDK not initialized" });
                    }
                }
            });
    }
    */

    /**
     * Show the banner
     */
    /*
    public void showBanner(final String id)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(banners.containsKey(id)) {
                        TMBannerAdView b = banners.get(id);
                        if(b.getParent() == null) {
                            placeBannerOnScreen(id, b);
                        }
                        b.setVisibility(View.VISIBLE);
                        for (String key : banners.keySet()) {
                            if(!key.equals(id)) {
                                TMBannerAdView b2 = banners.get(key);
                                b2.setVisibility(View.GONE);
                            }
                        }
                        Log.d(TAG, "Show Banner");
                    } else {
                        Log.w(TAG, "Banner not found: "+id);
                    }
                }
            });
    }

    public void removeBanner(final String id)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(banners.containsKey(id)) {
                        TMBannerAdView b = banners.get(id);
                        banners.remove(id);
                        layout.removeView(b); // Remove the banner
                        Log.d(TAG, "Remove Banner");
                    } else {
                        Log.w(TAG, "Banner not found: "+id);
                    }
                }
            });
    }
    */

    /**
     * Hide the banner
     */
    /*
    public void hideBanner(final String id)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(banners.containsKey(id)) {
                        TMBannerAdView b = banners.get(id);
                        b.setVisibility(View.GONE);
                        Log.d(TAG, "Hide Banner");
                    } else {
                        Log.w(TAG, "Banner not found: "+id);
                    }
                }
            });
    }
    */
    
    /**
     * Get the banner width
     * @return int Banner width
     */
    /*
    public int getBannerWidth(final String id)
    {
        if(banners.containsKey(id)) {
            TMBannerAdView b = banners.get(id);
            int w = b.getWidth();
            if(w == 0) {
                Resources r = activity.getResources();
                w = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, b.getSize().width, r.getDisplayMetrics());
            }
            return w;
        } else {
            return 0;
        }
    }
    */
    
    /**
     * Get the banner height
     * @return int Banner height
     */
    /*
    public int getBannerHeight(final String id)
    {
        if(banners.containsKey(id)) {
            TMBannerAdView b = banners.get(id);
            int h = b.getHeight();
            if(h == 0) {
                Resources r = activity.getResources();
                h = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, b.getSize().height, r.getDisplayMetrics());
            }
            return h;
        } else {
            return 0;
        }
    }

    public String makeZombieBanner(final String id)
    {
        if (banners.containsKey(id)) {
            TMBannerAdView b = banners.get(id);
            String zid = java.util.UUID.randomUUID().toString();
            banners.remove(id);
            zombieBanners.put(zid, b);
            Log.i(TAG, "makeZombieBanner: OK");
            return zid;
        } else {
            Log.w(TAG, "makeZombieBanner: Banner not found: "+id);
            return "";
        }
    }

    public void killZombieBanner(final String zid)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if (zombieBanners.containsKey(zid)) {
                        View z = zombieBanners.get(zid);
                        zombieBanners.remove(zid);
                        layout.removeView(z); // Remove the zombie banner
                        Log.w(TAG, "killZombieBanner: OK");
                    } else {
                        Log.w(TAG, "killZombieBanner: Banner not found: "+zid);
                    }
                }
            });
    }
    */

    /* Interstitial
     * ********************************************************************** */
    /*

    private TMAdListener makeInterstitialListener(final String id, final int callback_id)
    {
        return new TMAdListener() {
            @Override
            public void didLoad() {
                // Ready to display the interstitial
                Log.w(TAG, "Interstitial: didLoad");
                GodotLib.calldeferred(callback_id, "_on_interstitial_loaded", new Object[] { id });
            }
            @Override
            public void didClose() {
                Log.w(TAG, "Interstitial: didClose");
                GodotLib.calldeferred(callback_id, "_on_interstitial_close", new Object[] { id });
            }
            @Override
            public void didFailToLoad(TMAdError error) {
                Log.w(TAG, "Interstitial: didFailToLoad " + error.toString());
                interstitials.remove(id);
                GodotLib.calldeferred(callback_id, "_on_interstitial_failed_to_load", new Object[] { id, error.toString() });
            }
            @Override
            public void didDisplay() {
                Log.w(TAG, "Interstitial: didDisplay");
            }
            @Override
            public void didRefresh() {
                Log.w(TAG, "Interstitial: didRefresh");
                GodotLib.calldeferred(callback_id, "_on_interstitial_loaded", new Object[]{ id });
            }
        };
    }
    */
    
    /**
     * Load a interstitial
     * @param String id AdMod Interstitial ID
     */
    /*
    public void loadInterstitial(final String id, final int callback_id)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(_inited) {
                        if(!interstitials.containsKey(id)) {
                            TMAdListener listener = makeInterstitialListener(id, callback_id);
                            interstitials.put(id, new InterstitialWrapper(id, false, listener));
                            sdk.loadInterstitial(activity,  id, listener);
                        } else {
                            Log.i(TAG, "Interstitial already created: "+id);
                        }
                    } else {
                        Log.e(TAG, "Tapdaq not inited");
                        GodotLib.calldeferred(callback_id, "_on_interstitial_failed_to_load", new Object[] { id, "SDK not initialized" });
                    }
                }
            });
    }

    public void loadVideoInterstitial(final String id, final int callback_id)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(_inited) {
                        TMAdListener listener = makeInterstitialListener(id, callback_id);
                        interstitials.put(id, new InterstitialWrapper(id, true, listener));
                        sdk.loadVideo(activity,  id, listener);
                    } else {
                        Log.e(TAG, "Tapdaq not inited");
                        GodotLib.calldeferred(callback_id, "_on_interstitial_failed_to_load", new Object[] { id, "SDK not initialized" });
                    }
                }
            });
    }
    */

    /**
     * Show the interstitial
     */
    /*
    public void showInterstitial(final String id)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if (interstitials.containsKey(id)) {
                        InterstitialWrapper interstitial = interstitials.get(id);
                        if(interstitial.video && sdk.isVideoReady(activity, id)) {
                            sdk.showVideo(activity, id, interstitial.listener);
                        } else if(!interstitial.video && sdk.isInterstitialReady(activity, id)) {
                            sdk.showInterstitial(activity, id, interstitial.listener);
                        } else {
                            Log.w(TAG, "showInterstitial: interstitial not loaded");
                        }
                    } else {
                        Log.w(TAG, "showInterstitial: interstitial not found");
                    }
                }
            });
    }
    */

    /* Utilities
     * ********************************************************************** */
    
    /* Definitions
     * ********************************************************************** */

    public Ogury(Godot godot) 
    {
        super(godot);
        activity = godot;
    }

    @Override
    public String getPluginName() {
        return "Ogury";
    }

    @Override
    public List<String> getPluginMethods() {
        return Arrays.asList(
            "init", "initWithCallback", "editConsent", "gdprApplies"
        );
    }

    /*
    @Override
    public Set<SignalInfo> getPluginSignals() {
        return Collections.singleton(loggedInSignal);
    }
    */

    @Override
    public View onMainCreate(Activity activity) {
        return null;
    }

}
