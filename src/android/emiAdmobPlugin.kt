package emi.indo.cordova.plugin.admob
import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.preference.PreferenceManager
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.RequestConfiguration.PublisherPrivacyPersonalizationState
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaInterface
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CordovaWebView
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by EMI INDO So on Apr 2, 2023
 */
class emiAdmobPlugin : CordovaPlugin() {
    private var PUBLIC_CALLBACKS: CallbackContext? = null

    private var rewardedAd: RewardedAd? = null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var cWebView: CordovaWebView? = null
    private var isAppOpenAdShow = false
    private var isInterstitialLoad = false
    private var isRewardedInterstitialLoad = false
    private var isRewardedLoad = false
    private var isBannerPause = 0
    private var Position: String? = null
    private var Size: String? = null

    private var paddingInPx = 0
    private var marginsInPx = 0
    var ResponseInfo: Boolean = false
    private var isAdSkip = 0
    private var SetTagForChildDirectedTreatment: Boolean = false
    private var SetTagForUnderAgeOfConsent: Boolean = false
    private var SetMaxAdContentRating: String = "G"
    private var bannerAdUnitId: String? = null

    private var consentInformation: ConsentInformation? = null
    private var bannerViewLayout: RelativeLayout? = null
    private var bannerView: AdView? = null
    var isBannerLoad: Boolean = false
    var isBannerShow: Boolean = false

    var isBannerShows: Boolean = true
    private var bannerAutoShow = false
    private var isAutoResize: Boolean = false
    var appOpenAutoShow: Boolean = false
    var intAutoShow: Boolean = false
    var rewardedAutoShow: Boolean = false
    var rIntAutoShow: Boolean = false
    private var isCollapsible: Boolean = false
    var lock: Boolean = true
    private var setDebugGeography: Boolean = false
    private var mActivity: Activity? = null
    private var mContext: Context? = null
    private var mInterstitialAd: InterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var isOrientation: Int = 1

    private var mPreferences: SharedPreferences? = null
    var mBundleExtra: Bundle? = null
    private var collapsiblePos: String? = null

    // only isUsingAdManagerRequest = true
    private var customTargetingEnabled: Boolean = false
    private var customTargetingList: MutableList<String>? = null
    private var categoryExclusionsEnabled: Boolean = false
    private var cExclusionsValue: String = ""
    private var ppIdEnabled: Boolean = false
    private var ppIdVl: String = ""
    private var ppsEnabled: Boolean = false
    private var ppsVl: String = ""
    private var ppsArrayList: MutableList<Int>? = null
    private var contentURLEnabled: Boolean = false
    private var cURLVl: String = ""
    private var brandSafetyEnabled: Boolean = false
    private var brandSafetyUrls: MutableList<String>? = null


    private val isMobileAdsInitializeCalled = AtomicBoolean(false)

    private var isUsingAdManagerRequest = false


    override fun initialize(cordova: CordovaInterface, webView: CordovaWebView) {
        super.initialize(cordova, webView)
        cWebView = webView
        mActivity = this.cordova.activity
        mActivity?.let {
            mContext = it.applicationContext
        }

        mPreferences = mContext?.let { PreferenceManager.getDefaultSharedPreferences(it) }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val orientation = newConfig.orientation
        if (orientation != isOrientation) {
            cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.screen.rotated');")
            isOrientation = orientation
            if (this.isAutoResize) {
                mActivity!!.runOnUiThread {
                    try {
                        if (bannerViewLayout != null && bannerView != null) {
                            val parentView = bannerViewLayout!!.parent as ViewGroup
                            parentView?.removeView(bannerViewLayout)
                            bannerViewLayout = RelativeLayout(mActivity)
                            val params = RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT
                            )
                            val rootView =
                                mActivity!!.window.decorView.findViewById<View>(R.id.content)
                            if (rootView is ViewGroup) {
                                rootView.addView(bannerViewLayout, params)
                            } else {
                                mActivity!!.addContentView(bannerViewLayout, params)
                            }
                            bannerView = AdView(mContext!!)
                            setBannerPosition(this.Position)
                            setBannerSiz(this.Size)
                            bannerView!!.adUnitId = bannerAdUnitId!!
                            bannerView!!.adListener = bannerAdListener
                            bannerView!!.loadAd(buildAdRequest())
                            bannerViewLayout!!.addView(bannerView)
                            bannerViewLayout!!.bringToFront()
                        }
                    } catch (e: Exception) {
                        PUBLIC_CALLBACKS!!.error("Error adjusting banner size: " + e.message)
                    }
                }
            }

            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.orientation.portrait');")
            } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.orientation.landscape');")
            } else if (orientation == Configuration.ORIENTATION_UNDEFINED) {
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.orientation.undefined');")
            }
        }
    }


    @Throws(JSONException::class)
    override fun execute(
        action: String,
        args: JSONArray,
        callbackContext: CallbackContext
    ): Boolean {
        PUBLIC_CALLBACKS = callbackContext

        if (action == "initialize") {
            val options = args.getJSONObject(0)
            mActivity!!.runOnUiThread {
                val setAdRequest = options.optBoolean("isUsingAdManagerRequest")
                val responseInfo = options.optBoolean("isResponseInfo")
                val setDebugGeography = options.optBoolean("isConsentDebug")
                setUsingAdManagerRequest(setAdRequest)
                this.ResponseInfo = responseInfo
                this.setDebugGeography = setDebugGeography
                val params: ConsentRequestParameters
                if (this.setDebugGeography) {
                    val debugSettings = mActivity?.let {
                        deviceId?.let { it1 ->
                            ConsentDebugSettings.Builder(it)
                                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                                .addTestDeviceHashedId(it1).build()
                        }
                    }
                    params = ConsentRequestParameters.Builder()
                        .setConsentDebugSettings(debugSettings).build()
                } else {
                    params = ConsentRequestParameters.Builder()
                        .setTagForUnderAgeOfConsent(this.SetTagForUnderAgeOfConsent).build()
                }

                consentInformation = mContext?.let { UserMessagingPlatform.getConsentInformation(it) }
                mActivity?.let {
                    consentInformation?.requestConsentInfoUpdate(
                        it,
                        params,
                        {
                            cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.consent.info.update');")
                            when (consentInformation!!.getConsentStatus()) {
                                ConsentInformation.ConsentStatus.NOT_REQUIRED -> cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.consent.status.not_required');")
                                ConsentInformation.ConsentStatus.OBTAINED -> cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.consent.status.obtained');")
                                ConsentInformation.ConsentStatus.REQUIRED -> {
                                    handleConsentForm()
                                    cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.consent.status.required');")
                                }

                                ConsentInformation.ConsentStatus.UNKNOWN -> cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.consent.status.unknown');")
                            }
                        },
                        { formError: FormError ->
                            if (consentInformation!!.canRequestAds()) {
                                initializeMobileAdsSdk()
                            }
                            cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.consent.info.update.failed', { message: '" + formError.message + "' });")
                        })
                }
                if (consentInformation?.canRequestAds()!!) {
                    initializeMobileAdsSdk()
                }
            }

            return true
        } else if (action == "targeting") {
            val options = args.getJSONObject(0)
            mActivity!!.runOnUiThread {
                try {
                    val childDirectedTreatment = options.optBoolean("childDirectedTreatment")
                    val underAgeOfConsent = options.optBoolean("underAgeOfConsent")
                    val contentRating = options.optString("contentRating")
                    this.SetTagForChildDirectedTreatment = childDirectedTreatment
                    this.SetTagForUnderAgeOfConsent = underAgeOfConsent
                    this.SetMaxAdContentRating = contentRating
                    _Targeting(childDirectedTreatment, underAgeOfConsent, contentRating)
                    //  callbackContext.success();
                } catch (e: Exception) {
                    callbackContext.error("Error: " + e.message)
                }
            }
            return true
        } else if (action == "targetingAdRequest") {
            val options = args.getJSONObject(0)
            mActivity!!.runOnUiThread {
                val customTargetingEnabled = options.optBoolean("customTargetingEnabled")
                val categoryExclusionsEnabled = options.optBoolean("categoryExclusionsEnabled")
                val ppIdEnabled = options.optBoolean("ppIdEnabled")
                val contentURLEnabled = options.optBoolean("contentURLEnabled")
                val brandSafetyEnabled = options.optBoolean("brandSafetyEnabled")

                val customTargeting = options.optJSONArray("customTargetingValue")
                val categoryExclusions = options.optString("categoryExclusionsValue")
                val ppId = options.optString("ppIdValue")
                val ctURL = options.optString("contentURLValue")
                val brandSafetyArr = options.optJSONArray("brandSafetyArr")
                try {
                    this.customTargetingEnabled = customTargetingEnabled
                    this.categoryExclusionsEnabled = categoryExclusionsEnabled
                    this.ppIdEnabled = ppIdEnabled
                    this.contentURLEnabled = contentURLEnabled
                    this.brandSafetyEnabled = brandSafetyEnabled
                    this.cExclusionsValue = categoryExclusions
                    this.ppIdVl = ppId
                    this.cURLVl = ctURL
                    targetingAdRequest(
                        customTargeting,
                        categoryExclusions,
                        ppId,
                        ctURL,
                        brandSafetyArr
                    )
                    callbackContext.success()
                } catch (e: Exception) {
                    callbackContext.error(e.toString())
                }
            }
            return true
        } else if (action == "setPersonalizationState") {
            val options = args.getJSONObject(0)
            mActivity!!.runOnUiThread {
                val setPPT = options.optString("setPersonalizationState")
                try {
                    setPersonalizationState(setPPT)
                    callbackContext.success()
                } catch (e: Exception) {
                    callbackContext.error(e.toString())
                }
            }
            return true
        } else if (action == "setPPS") {
            val options = args.getJSONObject(0)
            mActivity!!.runOnUiThread {
                val ppsEnabled = options.optBoolean("ppsEnabled")
                val iabContent = options.optString("iabContent")
                val ppsArrValue = options.optJSONArray("ppsArrValue")
                try {
                    this.ppsEnabled = ppsEnabled
                    this.ppsVl = iabContent
                    setPublisherProvidedSignals(ppsArrValue)
                    callbackContext.success()
                } catch (e: Exception) {
                    callbackContext.error(e.toString())
                }
            }
            return true
        } else if (action == "globalSettings") {
            val options = args.getJSONObject(0)
            mActivity!!.runOnUiThread {
                val setAppMuted = options.optBoolean("setAppMuted")
                val setAppVolume = options.optInt("setAppVolume").toFloat()
                val pubIdEnabled = options.optBoolean("pubIdEnabled")
                try {
                    _globalSettings(setAppMuted, setAppVolume, pubIdEnabled)
                    // callbackContext.success();
                } catch (e: Exception) {
                    callbackContext.error(e.toString())
                }
            }
            return true
        } else if (action == "loadAppOpenAd") {
            val options = args.getJSONObject(0)
            mActivity!!.runOnUiThread {
                val adUnitId = options.optString("adUnitId")
                val autoShow = options.optBoolean("autoShow")
                try {
                    this.appOpenAutoShow = autoShow
                    AppOpenAd.load(
                        mActivity!!, adUnitId, buildAdRequest(),
                        object : AppOpenAdLoadCallback() {
                            override fun onAdLoaded(ad: AppOpenAd) {
                                appOpenAd = ad
                                isAppOpenAdShow = true

                                if (appOpenAutoShow) {
                                    OpenAutoShow()
                                }

                                _appOpenAdLoadCallback(callbackContext)

                                appOpenAd!!.onPaidEventListener =
                                    OnPaidEventListener { adValue: AdValue ->
                                        val valueMicros = adValue.valueMicros
                                        val currencyCode = adValue.currencyCode
                                        val precision = adValue.precisionType
                                        val appOpenAdAdUnitId = appOpenAd!!.adUnitId
                                        val result = JSONObject()
                                        try {
                                            result.put("micros", valueMicros)
                                            result.put("currency", currencyCode)
                                            result.put("precision", precision)
                                            result.put("adUnitId", appOpenAdAdUnitId)
                                            callbackContext.success(result)
                                            cWebView!!.loadUrl(
                                                "javascript:cordova.fireDocumentEvent('on.appOpenAd.revenue');"
                                            )
                                        } catch (e: JSONException) {
                                            callbackContext.error(e.message)
                                        }
                                    }
                                cWebView!!.loadUrl(
                                    "javascript:cordova.fireDocumentEvent('on.appOpenAd.loaded');"
                                )
                                if (ResponseInfo) {
                                    val result = JSONObject()
                                    val responseInfo = ad.responseInfo
                                    try {
                                        result.put("getResponseId", responseInfo.responseId)
                                        result.put(
                                            "getAdapterResponses",
                                            responseInfo.adapterResponses
                                        )
                                        result.put("getResponseExtras", responseInfo.responseExtras)
                                        result.put(
                                            "getMediationAdapterClassName",
                                            responseInfo.mediationAdapterClassName
                                        )
                                        result.put("getBundleExtra", mBundleExtra.toString())
                                        callbackContext.success(result)
                                    } catch (e: JSONException) {
                                        callbackContext.error(e.message)
                                    }
                                }
                            }

                            private fun OpenAutoShow() {
                                try {
                                    if (isAppOpenAdShow && appOpenAd != null) {
                                        mActivity!!.runOnUiThread { appOpenAd!!.show(mActivity!!) }
                                    }
                                } catch (e: Exception) {
                                    PUBLIC_CALLBACKS!!.error(e.toString())
                                }
                            }

                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                isAppOpenAdShow = false
                                callbackContext.error(loadAdError.toString())
                                cWebView!!.loadUrl(
                                    "javascript:cordova.fireDocumentEvent('on.appOpenAd.failed.loaded');"
                                )
                            }
                        })
                } catch (e: Exception) {
                    callbackContext.error(e.toString())
                }
            }
            return true
        } else if (action == "showAppOpenAd") {
            try {
                if (isAppOpenAdShow && appOpenAd != null) {
                    mActivity!!.runOnUiThread { appOpenAd!!.show(mActivity!!) }
                    _appOpenAdLoadCallback(callbackContext)
                } else {
                    callbackContext.error("The App Open Ad wasn't ready yet")
                }
            } catch (e: Exception) {
                PUBLIC_CALLBACKS!!.error(e.toString())
            }

            return true
        } else if (action == "loadInterstitialAd") {
            val options = args.getJSONObject(0)
            mActivity!!.runOnUiThread {
                val adUnitId = options.optString("adUnitId")
                val autoShow = options.optBoolean("autoShow")
                try {
                    this.intAutoShow = autoShow
                    InterstitialAd.load(
                        mActivity!!, adUnitId, buildAdRequest(),
                        object : InterstitialAdLoadCallback() {
                            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                                isInterstitialLoad = true
                                mInterstitialAd = interstitialAd

                                if (intAutoShow) {
                                    isIntAutoShow
                                }

                                _interstitialAdLoadCallback(callbackContext)

                                cWebView!!.loadUrl(
                                    "javascript:cordova.fireDocumentEvent('on.interstitial.loaded');"
                                )
                                if (ResponseInfo) {
                                    val result = JSONObject()
                                    val responseInfo = mInterstitialAd!!.responseInfo
                                    try {
                                        result.put("getResponseId", responseInfo.responseId)
                                        result.put(
                                            "getAdapterResponses",
                                            responseInfo.adapterResponses
                                        )
                                        result.put("getResponseExtras", responseInfo.responseExtras)
                                        result.put(
                                            "getMediationAdapterClassName",
                                            responseInfo.mediationAdapterClassName
                                        )
                                        result.put("getBundleExtra", mBundleExtra.toString())
                                        callbackContext.success(result)
                                    } catch (e: JSONException) {
                                        callbackContext.error(e.message)
                                    }
                                }
                                mInterstitialAd!!.onPaidEventListener =
                                    OnPaidEventListener { adValue: AdValue ->
                                        val valueMicros = adValue.valueMicros
                                        val currencyCode = adValue.currencyCode
                                        val precision = adValue.precisionType
                                        val interstitialAdUnitId = mInterstitialAd!!.adUnitId
                                        val result = JSONObject()
                                        try {
                                            result.put("micros", valueMicros)
                                            result.put("currency", currencyCode)
                                            result.put("precision", precision)
                                            result.put("adUnitId", interstitialAdUnitId)
                                            callbackContext.success(result)
                                        } catch (e: JSONException) {
                                            callbackContext.error(e.message)
                                        }
                                        cWebView!!.loadUrl(
                                            "javascript:cordova.fireDocumentEvent('on.interstitial.revenue');"
                                        )
                                    }
                            }

                            private val isIntAutoShow: Unit
                                get() {
                                    if (isInterstitialLoad && mInterstitialAd != null) {
                                        mActivity!!.runOnUiThread { mInterstitialAd!!.show(mActivity!!) }
                                    }
                                }

                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                mInterstitialAd = null
                                isInterstitialLoad = false
                                cWebView!!.loadUrl(
                                    "javascript:cordova.fireDocumentEvent('on.interstitial.failed.load');"
                                )
                                callbackContext.error(loadAdError.toString())
                            }
                        })
                } catch (e: Exception) {
                    callbackContext.error(e.toString())
                }
            }
            return true
        } else if (action == "showInterstitialAd") {
            if (isInterstitialLoad && mInterstitialAd != null) {
                mActivity!!.runOnUiThread { mInterstitialAd!!.show(mActivity!!) }
                _interstitialAdLoadCallback(callbackContext)
            } else {
                callbackContext.error("The Interstitial ad wasn't ready yet")
            }
            return true
        } else if (action == "loadRewardedAd") {
            val options = args.getJSONObject(0)
            mActivity!!.runOnUiThread {
                val adUnitId = options.optString("adUnitId")
                val autoShow = options.optBoolean("autoShow")
                try {
                    this.rewardedAutoShow = autoShow
                    RewardedAd.load(
                        mActivity!!, adUnitId, buildAdRequest(),
                        object : RewardedAdLoadCallback() {
                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                rewardedAd = null
                                isRewardedLoad = false
                                cWebView!!.loadUrl(
                                    "javascript:cordova.fireDocumentEvent('on.rewarded.failed.load');"
                                )
                                callbackContext.error(loadAdError.toString())
                            }

                            override fun onAdLoaded(ad: RewardedAd) {
                                rewardedAd = ad
                                isRewardedLoad = true
                                isAdSkip = 0
                                if (rewardedAutoShow) {
                                    isRewardedAutoShow
                                }
                                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.rewarded.loaded');")
                                _rewardedAdLoadCallback(callbackContext)
                                if (ResponseInfo) {
                                    val result = JSONObject()
                                    val responseInfo = ad.responseInfo
                                    try {
                                        result.put("getResponseId", responseInfo.responseId)
                                        result.put(
                                            "getAdapterResponses",
                                            responseInfo.adapterResponses
                                        )
                                        result.put("getResponseExtras", responseInfo.responseExtras)
                                        result.put(
                                            "getMediationAdapterClassName",
                                            responseInfo.mediationAdapterClassName
                                        )
                                        result.put("getBundleExtra", mBundleExtra.toString())
                                        callbackContext.success(result)
                                    } catch (e: JSONException) {
                                        callbackContext.error(e.message)
                                    }
                                }

                                rewardedAd!!.onPaidEventListener =
                                    OnPaidEventListener { adValue: AdValue ->
                                        val valueMicros = adValue.valueMicros
                                        val currencyCode = adValue.currencyCode
                                        val precision = adValue.precisionType
                                        val rewardedAdAdUnitId = rewardedAd!!.adUnitId
                                        val result = JSONObject()
                                        try {
                                            result.put("micros", valueMicros)
                                            result.put("currency", currencyCode)
                                            result.put("precision", precision)
                                            result.put("adUnitId", rewardedAdAdUnitId)
                                            callbackContext.success(result)
                                        } catch (e: JSONException) {
                                            callbackContext.error(e.message)
                                        }
                                        cWebView!!.loadUrl(
                                            "javascript:cordova.fireDocumentEvent('on.rewarded.revenue');"
                                        )
                                    }
                            }

                            private val isRewardedAutoShow: Unit
                                get() {
                                    if (isRewardedLoad && rewardedAd != null) {
                                        isAdSkip = 1
                                        rewardedAd!!.show(mActivity!!) { rewardItem: RewardItem ->
                                            isAdSkip = 2
                                            val rewardAmount = rewardItem.amount
                                            val rewardType = rewardItem.type
                                            val result = JSONObject()
                                            try {
                                                result.put("rewardAmount", rewardAmount)
                                                result.put("rewardType", rewardType)
                                                callbackContext.success(result)
                                            } catch (e: JSONException) {
                                                callbackContext.error(e.message)
                                            }
                                            cWebView!!.loadUrl(
                                                "javascript:cordova.fireDocumentEvent('on.reward.userEarnedReward');"
                                            )
                                        }
                                    }
                                }
                        })
                } catch (e: Exception) {
                    callbackContext.error(e.toString())
                }
            }
            return true
        } else if (action == "showRewardedAd") {
            mActivity!!.runOnUiThread {
                if (isRewardedLoad && rewardedAd != null) {
                    isAdSkip = 1
                    rewardedAd!!.show(mActivity!!) { rewardItem: RewardItem ->
                        isAdSkip = 2
                        val rewardAmount = rewardItem.amount
                        val rewardType = rewardItem.type
                        val result = JSONObject()
                        try {
                            result.put("rewardAmount", rewardAmount)
                            result.put("rewardType", rewardType)
                            callbackContext.success(result)
                        } catch (e: JSONException) {
                            callbackContext.error(e.message)
                        }
                        cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.reward.userEarnedReward');")
                    }
                    _rewardedAdLoadCallback(callbackContext)
                } else {
                    callbackContext.error("The rewarded ad wasn't ready yet")
                }
            }
            return true
        } else if (action == "loadRewardedInterstitialAd") {
            val options = args.getJSONObject(0)
            mActivity!!.runOnUiThread {
                val adUnitId = options.optString("adUnitId")
                val autoShow = options.optBoolean("autoShow")
                try {
                    this.rIntAutoShow = autoShow
                    RewardedInterstitialAd.load(
                        mActivity!!, adUnitId, buildAdRequest(),
                        object : RewardedInterstitialAdLoadCallback() {
                            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                                rewardedInterstitialAd = ad
                                isRewardedInterstitialLoad = true
                                isAdSkip = 0

                                if (rIntAutoShow) {
                                    isRIntAutoShow
                                }
                                _rewardedInterstitialAdLoadCallback(callbackContext)
                                responseAdLoad()
                                revenueAd()


                                cWebView!!.loadUrl(
                                    "javascript:cordova.fireDocumentEvent('on.rewardedInt.loaded');"
                                )
                            }

                            private fun revenueAd() {
                                rewardedInterstitialAd!!.onPaidEventListener =
                                    OnPaidEventListener { adValue: AdValue ->
                                        val valueMicros = adValue.valueMicros
                                        val currencyCode = adValue.currencyCode
                                        val precision = adValue.precisionType
                                        val rewardedIntAdUnitId = rewardedInterstitialAd!!.adUnitId
                                        val result = JSONObject()
                                        try {
                                            result.put("micros", valueMicros)
                                            result.put("currency", currencyCode)
                                            result.put("precision", precision)
                                            result.put("adUnitId", rewardedIntAdUnitId)
                                            callbackContext.success(result)
                                        } catch (e: JSONException) {
                                            callbackContext.error(e.message)
                                        }
                                        cWebView!!.loadUrl(
                                            "javascript:cordova.fireDocumentEvent('on.rewardedInt.revenue');"
                                        )
                                    }
                            }

                            private fun responseAdLoad() {
                                if (ResponseInfo) {
                                    val result = JSONObject()
                                    val responseInfo = rewardedInterstitialAd!!.responseInfo
                                    try {
                                        result.put("getResponseId", responseInfo.responseId)
                                        result.put(
                                            "getAdapterResponses",
                                            responseInfo.adapterResponses
                                        )
                                        result.put("getResponseExtras", responseInfo.responseExtras)
                                        result.put(
                                            "getMediationAdapterClassName",
                                            responseInfo.mediationAdapterClassName
                                        )
                                        result.put("getBundleExtra", mBundleExtra.toString())
                                        callbackContext.success(result)
                                    } catch (e: JSONException) {
                                        callbackContext.error(e.message)
                                    }
                                }
                            }

                            private val isRIntAutoShow: Unit
                                get() {
                                    if (isRewardedInterstitialLoad && rewardedInterstitialAd != null) {
                                        isAdSkip = 1
                                        rewardedInterstitialAd!!.show(mActivity!!) { rewardItem: RewardItem ->
                                            isAdSkip = 2
                                            val rewardAmount = rewardItem.amount
                                            val rewardType = rewardItem.type
                                            val result = JSONObject()
                                            try {
                                                result.put("rewardAmount", rewardAmount)
                                                result.put("rewardType", rewardType)
                                                callbackContext.success(result)
                                            } catch (e: JSONException) {
                                                callbackContext.error(e.message)
                                            }
                                            cWebView!!.loadUrl(
                                                "javascript:cordova.fireDocumentEvent('on.rewardedInt.userEarnedReward');"
                                            )
                                        }
                                    }
                                }

                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                rewardedInterstitialAd = null
                                isRewardedInterstitialLoad = false
                                cWebView!!.loadUrl(
                                    "javascript:cordova.fireDocumentEvent('on.rewardedInt.failed.load');"
                                )
                                callbackContext.error(loadAdError.toString())
                            }
                        })
                } catch (e: Exception) {
                    callbackContext.error(e.toString())
                }
            }
            return true
        } else if (action == "showRewardedInterstitialAd") {
            mActivity!!.runOnUiThread {
                if (isRewardedInterstitialLoad && rewardedInterstitialAd != null) {
                    isAdSkip = 1
                    rewardedInterstitialAd!!.show(mActivity!!) { rewardItem: RewardItem ->
                        isAdSkip = 2
                        val rewardAmount = rewardItem.amount
                        val rewardType = rewardItem.type
                        val result = JSONObject()
                        try {
                            result.put("rewardAmount", rewardAmount)
                            result.put("rewardType", rewardType)
                            callbackContext.success(result)
                        } catch (e: JSONException) {
                            callbackContext.error(e.message)
                        }
                        cWebView!!.loadUrl(
                            "javascript:cordova.fireDocumentEvent('on.rewardedInt.userEarnedReward');"
                        )
                    }

                    _rewardedInterstitialAdLoadCallback(callbackContext)
                } else {
                    callbackContext.error("The rewarded ad wasn't ready yet")
                }
            }
            return true
        } else if (action == "showPrivacyOptionsForm") {
            mActivity!!.runOnUiThread {
                try {
                    val params: ConsentRequestParameters
                    if (this.setDebugGeography) {
                        val debugSettings = deviceId?.let {
                            mActivity?.let { it1 ->
                                ConsentDebugSettings.Builder(it1)
                                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                                    .addTestDeviceHashedId(it).build()
                            }
                        }
                        params = ConsentRequestParameters.Builder()
                            .setConsentDebugSettings(debugSettings).build()
                    } else {
                        params = ConsentRequestParameters.Builder()
                            .setTagForUnderAgeOfConsent(this.SetTagForUnderAgeOfConsent).build()
                    }
                    consentInformation = mContext?.let {
                        UserMessagingPlatform.getConsentInformation(
                            it
                        )
                    }
                    mActivity?.let {
                        consentInformation?.requestConsentInfoUpdate(
                            it,
                            params,
                            {
                                mActivity?.let { it1 ->
                                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                                        it1
                                    ) { loadAndShowError: FormError? ->
                                        if (loadAndShowError != null) {
                                            cordova.activity.runOnUiThread {
                                                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.consent.failed.show', { message: '" + loadAndShowError.message + "' });")
                                            }
                                        }
                                        if (isPrivacyOptionsRequired == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED) {
                                            mActivity?.let { it2 ->
                                                UserMessagingPlatform.showPrivacyOptionsForm(
                                                    it2
                                                ) { formError: FormError? ->
                                                    if (formError != null) {
                                                        cordova.activity.runOnUiThread {
                                                            cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.consent.failed.show.options', { message: '" + formError.message + "' });")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            { requestConsentError: FormError ->
                                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.consent.info.update.failed', { message: '" + requestConsentError.message + "' });")
                            })
                    }
                } catch (e: Exception) {
                    callbackContext.error(e.toString())
                }
            }
            return true
        } else if (action == "consentReset") {
            mActivity!!.runOnUiThread {
                try {
                    consentInformation!!.reset()
                } catch (e: Exception) {
                    callbackContext.error(e.toString())
                }
            }
            return true
        } else if (action == "getIabTfc") {
            mActivity!!.runOnUiThread {
                val gdprApplies = mPreferences!!.getInt("IABTCF_gdprApplies", 0)
                val purposeConsents = mPreferences!!.getString("IABTCF_PurposeConsents", "")
                val vendorConsents = mPreferences!!.getString("IABTCF_VendorConsents", "")
                val consentString = mPreferences!!.getString("IABTCF_TCString", "")
                val userInfoJson = JSONObject()
                try {
                    userInfoJson.put("IABTCF_gdprApplies", gdprApplies)
                    userInfoJson.put("IABTCF_PurposeConsents", purposeConsents)
                    userInfoJson.put("IABTCF_VendorConsents", vendorConsents)
                    userInfoJson.put("IABTCF_TCString", consentString)
                    val editor = mPreferences!!.edit()
                    editor.putString("IABTCF_TCString", consentString)
                    editor.putLong(LAST_ACCESS_SUFFIX, System.currentTimeMillis())
                    editor.apply()
                    val key = "IABTCF_TCString"
                    getString(key)
                    callbackContext.success(userInfoJson)
                    cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.getIabTfc');")
                } catch (e: Exception) {
                    callbackContext.error(e.toString())
                    cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.getIabTfc.error');")
                }
            }
            return true
        } else if (action == "loadBannerAd") {
            val options = args.getJSONObject(0)
            mActivity!!.runOnUiThread {
                val adUnitId = options.optString("adUnitId")
                val position = options.optString("position")
                val collapsible = options.optString("collapsible")
                val size = options.optString("size")
                val autoResize = options.optBoolean("autoResize")
                val autoShow = options.optBoolean("autoShow")
                this.bannerAdUnitId = adUnitId
                this.Position = position
                this.Size = size
                this.bannerAutoShow = autoShow
                this.isAutoResize = autoResize
                this.collapsiblePos = collapsible


                isCollapsible = if (collapsible.isEmpty()) {
                    false
                } else {
                    true
                }
                try {
                    _loadBannerAd(adUnitId, position, size)
                } catch (e: Exception) {
                    callbackContext.error(e.toString())
                }
            }
            return true
        } else if (action == "showBannerAd") {
            mActivity!!.runOnUiThread {
                if (isBannerPause == 0) {
                    isShowBannerAds
                } else if (isBannerPause == 1) {
                    try {
                        bannerView!!.visibility = View.VISIBLE
                        bannerView!!.resume()
                    } catch (e: Exception) {
                        callbackContext.error(e.toString())
                    }
                }
            }
            return true
        } else if (action == "styleBannerAd") {
            val options = args.getJSONObject(0)
            mActivity!!.runOnUiThread {
                val paddingPx = options.optInt("padding")
                val marginsPx = options.optInt("margins")
                // final boolean autoResize = options.optBoolean("autoResize");
                try {
                    this.paddingInPx = paddingPx
                    this.marginsInPx = marginsPx
                    // this.isAutoResize = autoResize;
                } catch (e: Exception) {
                    callbackContext.error(e.toString())
                }
            }
            return true
        } else if (action == "collapsibleBannerAd") {
            val options = args.getJSONObject(0)

            mActivity!!.runOnUiThread {
                val enableCollapsible = options.optBoolean("enabledBannerCollapsible")
                val collapsible = options.optString("collapsiblePosition")
                try {
                    this.isCollapsible = enableCollapsible
                    this.collapsiblePos = collapsible
                } catch (e: Exception) {
                    callbackContext.error(e.toString())
                }
            }
            return true
        } else if (action == "hideBannerAd") {
            cordova.activity.runOnUiThread {
                if (isBannerShow) {
                    try {
                        bannerView!!.visibility = View.GONE
                        bannerView!!.pause()
                        isBannerLoad = false
                        isBannerPause = 1
                    } catch (e: Exception) {
                        callbackContext.error(e.toString())
                    }
                }
            }
            return true
        } else if (action == "removeBannerAd") {
            mActivity!!.runOnUiThread {
                try {
                    if (bannerViewLayout != null && bannerView != null) {
                        bannerViewLayout!!.removeView(bannerView)
                        bannerView!!.destroy()
                        bannerView = null
                        bannerViewLayout = null
                        isBannerLoad = false
                        isBannerShow = false
                        isBannerPause = 2
                        lock = true
                    }
                } catch (e: Exception) {
                    PUBLIC_CALLBACKS!!.error("Error removing banner: " + e.message)
                }
            }

            return true
        }
        return false
    }


    private fun handleConsentForm() {
        if (consentInformation!!.isConsentFormAvailable) {
            mContext?.let {
                UserMessagingPlatform.loadConsentForm(it,
                    { consentForm: ConsentForm ->
                        mActivity?.let { it1 ->
                            consentForm.show(
                                it1
                            ) { formError: FormError? ->
                                if (formError != null) {
                                    cordova.activity.runOnUiThread {
                                        cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.consent.failed.show', { message: '" + formError.message + "' });")
                                    }
                                }
                            }
                        }
                    },
                    { formError: FormError ->
                        cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.consent.failed.load.from', { message: '" + formError.message + "' });")
                    }
                )
            }
        }
    }


    private fun _loadBannerAd(adUnitId: String, position: String, size: String) {
        try {
            if (bannerViewLayout == null) {
                bannerViewLayout = RelativeLayout(mActivity)

                val params = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )

                val rootView = mActivity!!.window.decorView.findViewById<View>(R.id.content)
                if (rootView is ViewGroup) {
                    rootView.addView(bannerViewLayout, params)
                } else {
                    mActivity!!.addContentView(bannerViewLayout, params)
                }

                bannerView = AdView(mContext!!)
                setBannerPosition(position)
                setBannerSiz(size)
                bannerView!!.adUnitId = adUnitId
                bannerView!!.adListener = bannerAdListener
                bannerView!!.loadAd(buildAdRequest())
            } else {
                Log.d(TAG, "Banner view layout already exists.")
            }
        } catch (e: Exception) {
            PUBLIC_CALLBACKS!!.error("Error showing banner: " + e.message)
            Log.d(TAG, "Error showing banner: " + e.message)
        }
    }


    private fun isBannerAutoShow() {
        try {
            if (bannerView != null && bannerViewLayout != null) {
                if (lock) {
                    bannerViewLayout!!.addView(bannerView)
                    bannerViewLayout!!.bringToFront()
                    lock = false
                }
                isBannerPause = 0
                isBannerLoad = true
            } else {
                val errorMessage = "Error showing banner: bannerView or bannerViewLayout is null."
                Log.e("isBannerAutoShow", errorMessage)
                PUBLIC_CALLBACKS!!.error(errorMessage)
            }
        } catch (e: Exception) {
            val errorMessage = "Error showing banner: " + e.message
            Log.e("isBannerAutoShow", errorMessage, e)
            PUBLIC_CALLBACKS!!.error(errorMessage)
        }
    }


    private val isShowBannerAds: Unit
        get() {
            if (isBannerLoad && bannerView != null) {
                try {
                    if (lock) {
                        bannerViewLayout!!.addView(bannerView)
                        bannerViewLayout!!.bringToFront()
                        lock = false
                    }
                    isBannerShow = true
                } catch (e: Exception) {
                    lock = true
                    PUBLIC_CALLBACKS!!.error(e.toString())
                }
            }
        }


    private val bannerAdListener: AdListener = object : AdListener() {
        override fun onAdClicked() {
            cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.banner.click');")
        }

        override fun onAdClosed() {
            cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.banner.close');")
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            mActivity!!.runOnUiThread {
                try {
                    if (bannerViewLayout != null && bannerView != null) {
                        bannerViewLayout!!.removeView(bannerView)
                        bannerView!!.destroy()
                        bannerView = null
                        bannerViewLayout = null
                        isBannerLoad = false
                        isBannerShow = false
                        isBannerPause = 2
                        lock = true
                    }
                } catch (e: Exception) {
                    PUBLIC_CALLBACKS!!.error("Error removing banner: " + e.message)
                }
            }

            PUBLIC_CALLBACKS!!.error(adError.toString())
            cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.banner.failed.load');")
        }

        override fun onAdImpression() {
            cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.banner.impression');")
        }


        override fun onAdLoaded() {
            // Log.d(TAG, "onAdLoaded: Ad finished loading successfully.");
            isBannerLoad = true
            isBannerPause = 0

            if (bannerAutoShow) {
                isBannerAutoShow()
            }
            cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.banner.load');")


            val eventData = String.format(
                "{\"collapsible\": \"%s\"}",
                if (bannerView!!.isCollapsible) "collapsible" else "not collapsible"
            )

            cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.is.collapsible', $eventData)")


            bannerView!!.onPaidEventListener = bannerPaidAdListener

            if (ResponseInfo) {
                val result = JSONObject()
                val responseInfo = bannerView!!.responseInfo
                try {
                    checkNotNull(responseInfo)
                    result.put("getResponseId", responseInfo.responseId)
                    result.put("getAdapterResponses", responseInfo.adapterResponses)
                    result.put("getResponseExtras", responseInfo.responseExtras)
                    result.put(
                        "getMediationAdapterClassName",
                        responseInfo.mediationAdapterClassName
                    )

                    if (mBundleExtra != null) {
                        result.put("getBundleExtra", mBundleExtra.toString())
                    } else {
                        result.put("getBundleExtra", JSONObject.NULL)
                    }

                    PUBLIC_CALLBACKS!!.success(result)
                } catch (e: JSONException) {
                    PUBLIC_CALLBACKS!!.error(e.toString())
                }
            }
        }


        override fun onAdOpened() {
            cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.banner.open');")
            isBannerShows = false
        }
    }


    private val bannerPaidAdListener = OnPaidEventListener { adValue ->
        val valueMicros = adValue.valueMicros
        val currencyCode = adValue.currencyCode
        val precision = adValue.precisionType
        val adUnitId = bannerView!!.adUnitId
        val result = JSONObject()
        try {
            result.put("micros", valueMicros)
            result.put("currency", currencyCode)
            result.put("precision", precision)
            result.put("adUnitId", adUnitId)
            isBannerLoad = false
            isBannerShow = true
            PUBLIC_CALLBACKS!!.success(result)
            cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.banner.revenue');")
        } catch (e: JSONException) {
            PUBLIC_CALLBACKS!!.error(e.message)
        }
    }


    private fun setBannerSiz(size: String?) {
        when (size) {
            "responsive_adaptive" -> bannerView!!.setAdSize(adSize)
            "anchored_adaptive" -> bannerView!!.setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    mContext!!, adWidth
                )
            )

            "full_width_adaptive" -> bannerView!!.setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    mContext!!, AdSize.FULL_WIDTH
                )
            )

            "in_line_adaptive" -> bannerView!!.setAdSize(
                AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(
                    mContext!!, adWidth
                )
            )

            "banner" -> bannerView!!.setAdSize(AdSize.BANNER)
            "large_banner" -> bannerView!!.setAdSize(AdSize.LARGE_BANNER)
            "medium_rectangle" -> bannerView!!.setAdSize(AdSize.MEDIUM_RECTANGLE)
            "full_banner" -> bannerView!!.setAdSize(AdSize.FULL_BANNER)
            "leaderboard" -> bannerView!!.setAdSize(AdSize.LEADERBOARD)
            "fluid" -> bannerView!!.setAdSize(AdSize.FLUID)
        }
    }


    private val adSize: AdSize
        get() {
            val display = mActivity!!.windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)
            val density = outMetrics.density
            val adWidthPixels =
                if (bannerViewLayout != null && bannerViewLayout!!.width > 0) bannerViewLayout!!.width else outMetrics.widthPixels
            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(mContext!!, adWidth)
        }

    private val adWidth: Int
        get() {
            val display = mActivity!!.windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)
            val density = outMetrics.density
            val adWidthPixels =
                (if (bannerViewLayout != null && bannerViewLayout!!.width > 0) bannerViewLayout!!.width else outMetrics.widthPixels).toFloat()
            return (adWidthPixels / density).toInt()
        }


    private fun setBannerPosition(position: String?) {
        val bannerParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        )

        when (position) {
            "top-right" -> {
                bannerParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                bannerParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                bannerParams.setMargins(0, marginsInPx, marginsInPx, 0)
                bannerViewLayout!!.setPadding(0, paddingInPx, paddingInPx, 0)
            }

            "top-center" -> {
                bannerParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                bannerParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                bannerParams.setMargins(0, marginsInPx, 0, 0)
                bannerViewLayout!!.setPadding(0, paddingInPx, 0, 0)
            }

            "left" -> {
                bannerParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                bannerParams.addRule(RelativeLayout.CENTER_VERTICAL)
                bannerParams.setMargins(marginsInPx, 0, 0, 0)
                bannerViewLayout!!.setPadding(paddingInPx, 0, 0, 0)
            }

            "center" -> {
                bannerParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                bannerParams.addRule(RelativeLayout.CENTER_VERTICAL)
            }

            "right" -> {
                bannerParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                bannerParams.addRule(RelativeLayout.CENTER_VERTICAL)
                bannerParams.setMargins(0, 0, marginsInPx, 0)
                bannerViewLayout!!.setPadding(0, 0, paddingInPx, 0)
            }

            "bottom-center" -> {
                bannerParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                bannerParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                bannerParams.setMargins(0, 0, 0, marginsInPx)
                bannerViewLayout!!.setPadding(0, 0, 0, paddingInPx)
            }

            "bottom-right" -> {
                bannerParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                bannerParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                bannerParams.setMargins(0, 0, marginsInPx, marginsInPx)
                bannerViewLayout!!.setPadding(0, 0, paddingInPx, paddingInPx)
            }

            else -> {
                bannerParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                bannerParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                bannerParams.setMargins(marginsInPx, 0, 0, marginsInPx)
                bannerViewLayout!!.setPadding(paddingInPx, 0, 0, paddingInPx)
            }
        }
        bannerView!!.layoutParams = bannerParams
    }


    fun setUsingAdManagerRequest(isUsingAdManagerRequest: Boolean) {
        this.isUsingAdManagerRequest = isUsingAdManagerRequest
    }


    private fun targetingAdRequest(
        customTargeting: JSONArray?,
        categoryExclusions: String,
        ppId: String,
        ctURL: String,
        brandSafetyArr: JSONArray?
    ) {
        try {
            customTargetingList = ArrayList()

            if (customTargeting != null) {
                for (i in 0 until customTargeting.length()) {
                    (customTargetingList as ArrayList<String>).add(customTargeting.getString(i))
                }
            }


            brandSafetyUrls = ArrayList()
            if (brandSafetyArr != null) {
                for (i in 0 until brandSafetyArr.length()) {
                    try {
                        (brandSafetyUrls as ArrayList<String>).add(brandSafetyArr.getString(i))
                    } catch (e: JSONException) {
                        // e.printStackTrace();
                    }
                }
            }

            this.cExclusionsValue = categoryExclusions
            this.ppIdVl = ppId
            this.cURLVl = ctURL
        } catch (e: JSONException) {
            //  e.printStackTrace();
        }
    }


    private fun setPublisherProvidedSignals(ppsArrValue: JSONArray?) {
        try {
            ppsArrayList = ArrayList()
            if (ppsArrValue != null) {
                for (i in 0 until ppsArrValue.length()) {
                    (ppsArrayList as ArrayList<Int>).add(ppsArrValue.getInt(i))
                }
            }
        } catch (e: JSONException) {
            // e.printStackTrace();
        }
    }


    @SuppressLint("DefaultLocale")
    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        MobileAds.initialize(mContext!!) { initializationStatus: InitializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                if (status != null) {
                    Log.d(
                        TAG, String.format(
                            "Adapter name:%s, Description:%s, Latency:%d", adapterClass,
                            status.description, status.latency
                        )
                    )
                } else {
                    PUBLIC_CALLBACKS!!.error(MobileAds.ERROR_DOMAIN)
                }
            }
            val sdkVersion = MobileAds.getVersion().toString()
            val mStatus = consentInformation!!.consentStatus.toString()

            val adapterInfo = StringBuilder()
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                if (status != null) {
                    adapterInfo.append(
                        String.format(
                            "Adapter name:%s, Description:%s, Latency:%d\n",
                            adapterClass, status.description, status.latency
                        )
                    )
                }
            }

            val gdprApplies = mPreferences!!.getInt("IABTCF_gdprApplies", 0)
            val purposeConsents = mPreferences!!.getString("IABTCF_PurposeConsents", "")
            val vendorConsents = mPreferences!!.getString("IABTCF_VendorConsents", "")
            val consentTCString = mPreferences!!.getString("IABTCF_TCString", "")
            val additionalConsent = mPreferences!!.getString("IABTCF_AddtlConsent", "")

            val eventData = String.format(
                "{ version: '%s', adapters: '%s', consentStatus: '%s', gdprApplies: '%d', purposeConsents: '%s', vendorConsents: '%s', consentTCString: '%s', additionalConsent: '%s' }",
                sdkVersion,
                adapterInfo,
                mStatus,
                gdprApplies,
                purposeConsents,
                vendorConsents,
                consentTCString,
                additionalConsent
            )
            Log.d(TAG, "Google Mobile Ads SDK: this.SetTagForUnderAgeOfConsent $eventData")
            cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.sdkInitialization', $eventData)")
        }
    }


    @SuppressLint("DefaultLocale")
    private fun buildAdRequest(): AdRequest {
        if (isUsingAdManagerRequest) {
            val builder = AdManagerAdRequest.Builder()

            if (this.customTargetingEnabled) {
                if (customTargetingList!!.isEmpty()) {
                    Log.d(TAG, "List is empty")
                    PUBLIC_CALLBACKS?.error("List is empty")
                } else {
                    builder.addCustomTargeting("age", customTargetingList!!)
                }
            }

            if (this.categoryExclusionsEnabled) {
                if (cExclusionsValue != "") {
                    builder.addCategoryExclusion(this.cExclusionsValue)
                }
            }

            if (this.ppIdEnabled) {
                if (ppIdVl != "") {
                    builder.setPublisherProvidedId(this.ppIdVl)
                }
            }


            if (this.contentURLEnabled) {
                if (cURLVl != "") {
                    builder.setPublisherProvidedId(this.cURLVl)
                }
            }


            if (this.brandSafetyEnabled) {
                if (brandSafetyUrls!!.isEmpty()) {
                    Log.d(TAG, "List is empty")
                    PUBLIC_CALLBACKS?.error("List is empty")
                } else {
                    builder.setNeighboringContentUrls(brandSafetyUrls!!)
                }
            }


            val bundleExtra = Bundle()
            // bundleExtra.putString("npa", this.Npa); DEPRECATED Beginning January 16, 2024
            if (isCollapsible) {
                bundleExtra.putString("collapsible", this.collapsiblePos)
            }
            bundleExtra.putBoolean("is_designed_for_families", this.SetTagForChildDirectedTreatment)
            bundleExtra.putBoolean("under_age_of_consent", this.SetTagForUnderAgeOfConsent)
            bundleExtra.putString("max_ad_content_rating", this.SetMaxAdContentRating)
            builder.addNetworkExtrasBundle(AdMobAdapter::class.java, bundleExtra)

            if (this.ppsEnabled) {
                when (this.ppsVl) {
                    "IAB_AUDIENCE_1_1" -> bundleExtra.putIntegerArrayList(
                        "IAB_AUDIENCE_1_1",
                        ppsArrayList as ArrayList<Int>?
                    )

                    "IAB_CONTENT_2_2" -> bundleExtra.putIntegerArrayList(
                        "IAB_CONTENT_2_2",
                        ppsArrayList as ArrayList<Int>?
                    )
                }
            }
            mBundleExtra = bundleExtra

            return builder.build()
        } else {
            val builder = AdRequest.Builder()
            val bundleExtra = Bundle()
            // bundleExtra.putString("npa", this.Npa); DEPRECATED Beginning January 16, 2024
            if (isCollapsible) {
                bundleExtra.putString("collapsible", this.collapsiblePos)
            }
            bundleExtra.putBoolean("is_designed_for_families", this.SetTagForChildDirectedTreatment)
            bundleExtra.putBoolean("under_age_of_consent", this.SetTagForUnderAgeOfConsent)
            bundleExtra.putString("max_ad_content_rating", this.SetMaxAdContentRating)
            builder.addNetworkExtrasBundle(AdMobAdapter::class.java, bundleExtra)
            mBundleExtra = bundleExtra

            return builder.build()
        }
    }


    val deviceId: String?
        get() {
            var algorithm = "SHA-256"
            try {
                val messageDigest = MessageDigest.getInstance(algorithm)
                val contentResolver = mContext!!.contentResolver
                @SuppressLint("HardwareIds") val androidId =
                    Settings.Secure.getString(contentResolver, "android_id")
                messageDigest.update(androidId.toByteArray())
                val by = messageDigest.digest()
                val sb = StringBuilder()
                for (b in by) {
                    val emi = StringBuilder(Integer.toHexString((255 and b.toInt())))
                    while (emi.length < 2) {
                        emi.insert(0, "0")
                    }
                    sb.append(emi)
                }
                return sb.toString().uppercase(Locale.getDefault())
            } catch (e: NoSuchAlgorithmException) {
                algorithm = "SHA-1"
                try {
                    val messageDigest = MessageDigest.getInstance(algorithm)
                    val contentResolver = mContext!!.contentResolver
                    @SuppressLint("HardwareIds") val androidId =
                        Settings.Secure.getString(contentResolver, "android_id")
                    messageDigest.update(androidId.toByteArray())
                    val by = messageDigest.digest()
                    val sb = StringBuilder()
                    for (b in by) {
                        val emi = StringBuilder(Integer.toHexString((255 and b.toInt())))
                        while (emi.length < 2) {
                            emi.insert(0, "0")
                        }
                        sb.append(emi)
                    }
                    return sb.toString().uppercase(Locale.getDefault())
                } catch (ex: NoSuchAlgorithmException) {
                    // ex.printStackTrace();
                    return null
                }
            }
        }


    fun getString(key: String) {
        val lastAccessTime = mPreferences!!.getLong(key + LAST_ACCESS_SUFFIX, 0)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAccessTime > EXPIRATION_TIME) {
            removeKey(key)
            cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.TCString.expired');")
        }
        val editor = mPreferences!!.edit()
        editor.putLong(key + LAST_ACCESS_SUFFIX, currentTime)
        editor.apply()
    }

    private fun removeKey(key: String) {
        val editor = mPreferences!!.edit()
        editor.remove(key)
        editor.remove(key + LAST_ACCESS_SUFFIX)
        editor.apply()
        cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.TCString.remove');")
    }






    private fun _appOpenAdLoadCallback(callbackContext: CallbackContext) {
        appOpenAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.appOpenAd.dismissed');")

                // Akses langsung ke properti view tanpa menggunakan 'this'
                val mainView: View? = view
                if (mainView != null) {
                    mainView.requestFocus()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.appOpenAd.failed.show');")
                callbackContext.error(adError.toString())
                appOpenAd = null
            }

            override fun onAdShowedFullScreenContent() {
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.appOpenAd.show');")
            }
        }
    }


    private fun _interstitialAdLoadCallback(callbackContext: CallbackContext) {
        mInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.interstitial.click');")
            }

            override fun onAdDismissedFullScreenContent() {
                mInterstitialAd = null
                isInterstitialLoad = false
                val mainView: View? = view
                mainView?.requestFocus()
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.interstitial.dismissed');")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                mInterstitialAd = null
                isInterstitialLoad = false
                callbackContext.error(adError.toString())
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.interstitial.failed.show');")
            }

            override fun onAdImpression() {
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.interstitial.impression');")
            }

            override fun onAdShowedFullScreenContent() {
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.interstitial.show');")
            }
        }
    }

    private fun _rewardedAdLoadCallback(callbackContext: CallbackContext) {
        rewardedAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.rewarded.click');")
            }

            override fun onAdDismissedFullScreenContent() {
                if (isAdSkip != 2) {
                    rewardedAd = null
                    isRewardedLoad = false
                    val mainView: View? = view
                    mainView?.requestFocus()
                    cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.rewarded.ad.skip');")
                }
                rewardedAd = null
                isRewardedLoad = false
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.rewarded.dismissed');")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                isRewardedLoad = false
                callbackContext.error(adError.toString())
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.rewarded.failed.show');")
            }

            override fun onAdImpression() {
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.rewarded.impression');")
            }

            override fun onAdShowedFullScreenContent() {
                isAdSkip = 1
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.rewarded.show');")
            }
        }
    }

    private fun _rewardedInterstitialAdLoadCallback(callbackContext: CallbackContext) {
        rewardedInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.rewardedInt.click');")
            }

            override fun onAdDismissedFullScreenContent() {
                if (isAdSkip != 2) {
                    rewardedInterstitialAd = null
                    isRewardedInterstitialLoad = false
                    cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.rewardedInt.ad.skip');")
                }
                rewardedInterstitialAd = null
                isRewardedInterstitialLoad = false
                val mainView: View? = view
                mainView?.requestFocus()
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.rewardedInt.dismissed');")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedInterstitialAd = null
                isRewardedInterstitialLoad = false
                callbackContext.error(adError.toString())
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.rewardedInt.failed.show');")
            }

            override fun onAdImpression() {
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.rewardedInt.impression');")
            }

            override fun onAdShowedFullScreenContent() {
                isAdSkip = 1
                Log.d(TAG, "Ad showed fullscreen content.")
                cWebView!!.loadUrl("javascript:cordova.fireDocumentEvent('on.rewardedInt.showed');")
            }
        }
    }

    fun _globalSettings(setAppMuted: Boolean, setAppVolume: Float, pubIdEnabled: Boolean) {
        MobileAds.setAppMuted(setAppMuted)
        MobileAds.setAppVolume(setAppVolume)
        MobileAds.putPublisherFirstPartyIdEnabled(pubIdEnabled)
    }

    fun _Targeting(
        childDirectedTreatment: Boolean,
        underAgeOfConsent: Boolean,
        contentRating: String?
    ) {
        val requestConfiguration = MobileAds.getRequestConfiguration().toBuilder()
        requestConfiguration.setTagForChildDirectedTreatment(if (childDirectedTreatment) RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE else RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
        requestConfiguration.setTagForUnderAgeOfConsent(if (underAgeOfConsent) RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE else RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        when (contentRating) {
            "T" -> requestConfiguration.setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
            "PG" -> requestConfiguration.setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG)
            "MA" -> requestConfiguration.setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_MA)
            "G" -> requestConfiguration.setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
            else -> requestConfiguration.setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_UNSPECIFIED)
        }
        MobileAds.setRequestConfiguration(requestConfiguration.build())
    }

    val isPrivacyOptionsRequired: ConsentInformation.PrivacyOptionsRequirementStatus
        get() = consentInformation
            ?.getPrivacyOptionsRequirementStatus() ?: ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED



    private fun setPersonalizationState(setPPT: String) {
        val state = when (setPPT) {
            "disabled" -> PublisherPrivacyPersonalizationState.DISABLED
            "enabled" -> PublisherPrivacyPersonalizationState.ENABLED
            else -> PublisherPrivacyPersonalizationState.DEFAULT
        }
        val requestConfiguration = MobileAds.getRequestConfiguration()
            .toBuilder()
            .setPublisherPrivacyPersonalizationState(state)
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)
    }


    private val view: View?
        get() {
            if (View::class.java.isAssignableFrom(CordovaWebView::class.java)) {
                return cWebView as View?
            }
            return mActivity!!.window.decorView.findViewById(R.id.content)
        }

    override fun onPause(multitasking: Boolean) {
        if (bannerView != null) {
            bannerView!!.pause()
        }

        super.onPause(multitasking)
    }

    override fun onResume(multitasking: Boolean) {
        super.onResume(multitasking)
        if (bannerView != null) {
            bannerView!!.resume() // ini berfungsi
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    override fun onDestroy() {
        if (bannerView != null) {
            bannerView!!.destroy()
            bannerView = null
        }
        if (bannerViewLayout != null) {
            val parentView = bannerViewLayout!!.parent as ViewGroup
            parentView?.removeView(bannerViewLayout)
            bannerViewLayout = null
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "emiAdmobPlugin"

        // Consent status will automatically reset after 12 months
        // https://support.google.com/admanager/answer/9999955?hl=en
        private const val LAST_ACCESS_SUFFIX = "_last_access"
        private const val EXPIRATION_TIME = 360L * 24 * 60 * 60 * 1000
    }
}