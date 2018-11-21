package com.paddlebike.kenandrews.riverwatch.config

/**
 *
 */

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.support.wearable.complications.ComplicationHelperActivity
import android.support.wearable.complications.ComplicationProviderInfo
import android.support.wearable.complications.ProviderInfoRetriever
import android.support.wearable.complications.ProviderInfoRetriever.OnProviderInfoReceivedCallback
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.paddlebike.kenandrews.riverwatch.R
import com.paddlebike.kenandrews.riverwatch.RiverWatchFace
import kotlinx.android.synthetic.main.config_list_gauge_id.view.*
import kotlinx.android.synthetic.main.config_list_more_options_item.view.*
import kotlinx.android.synthetic.main.config_list_preview_and_complications_item.view.*


import java.util.ArrayList
import java.util.concurrent.Executors

/**
 * Displays different layouts for configuring watch face's complications and appearance settings
 * (highlight color [second arm], background color, unread notifications, etc.).
 *
 *
 * All appearance settings are saved via [SharedPreferences].
 *
 *
 * Layouts provided by this adapter are split into 5 main view types.
 *
 *
 * A watch face preview including complications. Allows user to tap on the complications to
 * change the complication data and see a live preview of the watch face.
 *
 *
 * Simple arrow to indicate there are more options below the fold.
 *
 *
 * Color configuration options for both highlight (seconds hand) and background color.
 *
 *
 * Toggle for unread notifications.
 *
 *
 * Background image complication configuration for changing background image of watch face.
 */
class ComplicationConfigRecyclerViewAdapter(
        private val mContext: Context,
        watchFaceServiceClass: Class<*>,
        private val mSettingsDataSet: ArrayList<ComplicationConfigData.Companion.ConfigItemType>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    /**
    ComponentName associated with watch face service (service that renders watch face). Used
     to retrieve complication information.
    */

    private val mWatchFaceComponentName: ComponentName = ComponentName(mContext, watchFaceServiceClass)

    internal var mSharedPref: SharedPreferences

    // Selected complication id by user.
    private var mSelectedComplicationId: Int = 0

    private val mLeftComplicationId: Int
    private val mCenterComplicationId: Int
    private val mRightComplicationId: Int
    private val mBottomComplicationId: Int

    // Required to retrieve complication data from watch face for preview.
    private val mProviderInfoRetriever: ProviderInfoRetriever

    // Maintains reference view holder to dynamically update watch face preview. Used instead of
    // notifyItemChanged(int position) to avoid flicker and re-inflating the view.
    private var mPreviewAndComplicationsViewHolder: PreviewAndComplicationsViewHolder? = null

    /**
     * Used by associated watch face to let this
     * adapter know which complication locations are supported, their ids, and supported
     * complication data types.
     */
    enum class ComplicationLocation {
        BACKGROUND,
        LEFT,
        CENTER,
        RIGHT,
        TOP,
        BOTTOM
    }

    init {

        // Default value is invalid (only changed when user taps to change complication).
        mSelectedComplicationId = -1

        mLeftComplicationId = RiverWatchFace.getComplicationId(ComplicationLocation.LEFT)
        mCenterComplicationId = RiverWatchFace.getComplicationId(ComplicationLocation.CENTER)
        mRightComplicationId = RiverWatchFace.getComplicationId(ComplicationLocation.RIGHT)
        mBottomComplicationId = RiverWatchFace.getComplicationId(ComplicationLocation.BOTTOM)

        mSharedPref = mContext.getSharedPreferences(mContext.getString(R.string.watchface_prefs), 0)

        // Initialization of code to retrieve active complication data for the watch face.
        mProviderInfoRetriever = ProviderInfoRetriever(mContext, Executors.newCachedThreadPool())
        mProviderInfoRetriever.init()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Log.d(TAG, "onCreateViewHolder(): viewType: $viewType")


        return when (viewType) {
            TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG -> {
                // Need direct reference to watch face preview view holder to update watch face
                // preview based on selections from the user.
                PreviewAndComplicationsViewHolder(
                        LayoutInflater.from(parent.context)
                                .inflate(
                                        R.layout.config_list_preview_and_complications_item,
                                        parent,
                                        false))
            }

            TYPE_MORE_OPTIONS -> MoreOptionsViewHolder(
                    LayoutInflater.from(parent.context)
                            .inflate(
                                    R.layout.config_list_more_options_item,
                                    parent,
                                    false))

            TYPE_GAUGE_ID_CONFIG ->  GaugeIdViewHolder(
                    LayoutInflater.from(parent.context)
                            .inflate(
                                    R.layout.config_list_gauge_id,
                                    parent,
                                    false))

            TYPE_FAHRENHEIT_CONFIG ->  FahrenheitDisplayViewHolder(
                    LayoutInflater.from(parent.context)
                            .inflate(
                                    R.layout.config_list_fahrenheit,
                                    parent,
                                    false))
            else -> MoreOptionsViewHolder(
                    LayoutInflater.from(parent.context)
                            .inflate(
                                    R.layout.config_list_more_options_item,
                                    parent,
                                    false))
        }

    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        Log.d(TAG, "Element $position set.")

        // Pulls all data required for creating the UX for the specific setting option.
        val configItemType = mSettingsDataSet[position]

        when (viewHolder.itemViewType) {
            TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG -> {
                val previewAndComplicationsViewHolder = viewHolder as PreviewAndComplicationsViewHolder

                //val previewAndComplicationsConfigItem = configItemType as ComplicationConfigData.PreviewAndComplicationsConfigItem

                //val defaultComplicationResourceId = previewAndComplicationsConfigItem.defaultComplicationResourceId
                previewAndComplicationsViewHolder.setDefaultComplicationDrawable()

                previewAndComplicationsViewHolder.initializeComplications()
            }

            TYPE_MORE_OPTIONS -> {
                val moreOptionsViewHolder = viewHolder as MoreOptionsViewHolder
                val moreOptionsConfigItem = configItemType as ComplicationConfigData.MoreOptionsConfigItem

                moreOptionsViewHolder.setIcon(moreOptionsConfigItem.iconResourceId)
            }


            TYPE_GAUGE_ID_CONFIG -> {
                val gaugeIdViewHolder = viewHolder as GaugeIdViewHolder
                val gaugeIdConfigItem = configItemType as ComplicationConfigData.GaugeIdConfigItem

                gaugeIdViewHolder.setDefault(gaugeIdConfigItem.defaultGaugeId)
            }


            TYPE_FAHRENHEIT_CONFIG -> {
                val fahrenheitViewHolder = viewHolder as FahrenheitDisplayViewHolder

                val fahrenheitConfigItem = configItemType as ComplicationConfigData.FahrenheitDisplayConfigItem

                val fahrenheitEnabledIconResourceId = fahrenheitConfigItem.iconEnabledResourceId
                val fahrenheitDisabledIconResourceId = fahrenheitConfigItem.iconDisabledResourceId

                val unreadName = fahrenheitConfigItem.name
                val unreadSharedPrefId = fahrenheitConfigItem.sharedPrefId

                fahrenheitViewHolder.setIcons(
                        fahrenheitDisabledIconResourceId, fahrenheitEnabledIconResourceId)
                fahrenheitViewHolder.setName(unreadName)
                fahrenheitViewHolder.setSharedPrefId(unreadSharedPrefId)
            }


        }
    }

    override fun getItemViewType(position: Int): Int {
        val configItemType = mSettingsDataSet[position]
        return configItemType.configType
    }

    override fun getItemCount(): Int {
        return mSettingsDataSet.size
    }

    /** Updates the selected complication id saved earlier with the new information.  */
    fun updateSelectedComplication(complicationProviderInfo: ComplicationProviderInfo) {

        Log.d(TAG, "updateSelectedComplication: ")

        // Checks if view is inflated and complication id is valid.
        if (mSelectedComplicationId >= 0) {
            mPreviewAndComplicationsViewHolder?.updateComplicationViews(
                    mSelectedComplicationId, complicationProviderInfo)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        // Required to release retriever for active complication data on detach.
        mProviderInfoRetriever.release()
    }


    /**
     * Displays watch face preview along with complication locations. Allows user to tap on the
     * complication they want to change and preview updates dynamically.
     */
    inner class PreviewAndComplicationsViewHolder(view: View) : RecyclerView.ViewHolder(view), OnClickListener {

        private val mLeftComplicationBackground = view.left_complication_background

        private val mCenterComplicationBackground = view.center_complication_background

        private val mRightComplicationBackground: ImageView = view.right_complication_background

        private val mBottomComplicationBackground = view.bottom_complication_background

        private val mLeftComplication = view.left_complication

        private val mCenterComplication = view.center_complication

        private val mRightComplication = view.right_complication

        private val mBottomComplication = view.bottom_complication


        private var mDefaultComplicationDrawable: Drawable? = null

        private var mBackgroundComplicationEnabled: Boolean = false

        init {
            // Sets up left complication preview.
            mLeftComplication.setOnClickListener(this)
            mCenterComplication.setOnClickListener(this)
            mRightComplication.setOnClickListener(this)
            mBottomComplication.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            when (view) {
                mLeftComplication -> {
                    Log.d(TAG, "Left Complication click()")
                    val currentActivity = view.context as Activity
                    launchComplicationHelperActivity(currentActivity, ComplicationLocation.LEFT)

                }
                mCenterComplication -> {
                    Log.d(TAG, "Center Complication click()")
                    val currentActivity = view.context as Activity
                    launchComplicationHelperActivity(currentActivity, ComplicationLocation.CENTER)

                }
                mRightComplication -> {
                    Log.d(TAG, "Right Complication click()")
                    val currentActivity = view.context as Activity
                    launchComplicationHelperActivity(currentActivity, ComplicationLocation.RIGHT)

                }
                mBottomComplication -> {
                    Log.d(TAG, "Bottom Complication click()")
                    val currentActivity = view.context as Activity
                    launchComplicationHelperActivity(currentActivity, ComplicationLocation.BOTTOM)
                }
            }
        }



        // Verifies the watch face supports the complication location, then launches the helper
        // class, so user can choose their complication data provider.
        private fun launchComplicationHelperActivity(
                currentActivity: Activity, complicationLocation: ComplicationLocation) {

            mSelectedComplicationId = RiverWatchFace.getComplicationId(complicationLocation)

            mBackgroundComplicationEnabled = false

            if (mSelectedComplicationId >= 0) {

                val supportedTypes = RiverWatchFace.getSupportedComplicationTypes(
                        complicationLocation)

                val watchFace = ComponentName(currentActivity, RiverWatchFace::class.java)

                currentActivity.startActivityForResult(
                        ComplicationHelperActivity.createProviderChooserHelperIntent(
                                currentActivity,
                                watchFace,
                                mSelectedComplicationId,
                                *supportedTypes),
                        ComplicationConfigActivity.COMPLICATION_CONFIG_REQUEST_CODE)

            } else {
                Log.d(TAG, "Complication not supported by watch face.")
            }
        }

        fun setDefaultComplicationDrawable() {

            mLeftComplication.setImageDrawable(mDefaultComplicationDrawable)
            mLeftComplicationBackground.visibility = View.INVISIBLE

            mCenterComplication.setImageDrawable(mDefaultComplicationDrawable)
            mCenterComplicationBackground.visibility = View.INVISIBLE

            mRightComplication.setImageDrawable(mDefaultComplicationDrawable)
            mRightComplicationBackground.visibility = View.INVISIBLE

            mBottomComplication.setImageDrawable(mDefaultComplicationDrawable)
            mBottomComplicationBackground.visibility = View.INVISIBLE
        }

        fun updateComplicationViews(
                watchFaceComplicationId: Int, complicationProviderInfo: ComplicationProviderInfo?) {
            Log.d(TAG, "updateComplicationViews(): id: $watchFaceComplicationId")
            Log.d(TAG, "\tinfo: " + complicationProviderInfo!!)

            when (watchFaceComplicationId) {
                mLeftComplicationId -> updateComplicationView(complicationProviderInfo, mLeftComplication,
                        mLeftComplicationBackground)

                mCenterComplicationId -> updateComplicationView(complicationProviderInfo, mCenterComplication,
                        mCenterComplicationBackground)

                mRightComplicationId -> updateComplicationView(complicationProviderInfo, mRightComplication,
                        mRightComplicationBackground)

                mBottomComplicationId -> updateComplicationView(complicationProviderInfo, mBottomComplication,
                        mBottomComplicationBackground)
            }
        }

        private fun updateComplicationView(complicationProviderInfo: ComplicationProviderInfo?,
                                           button: ImageButton, background: ImageView) {
            if (complicationProviderInfo != null) {
                button.setImageIcon(complicationProviderInfo.providerIcon)
                button.contentDescription = mContext.getString(R.string.edit_complication,
                        complicationProviderInfo.appName + " " +
                                complicationProviderInfo.providerName)
                background.visibility = View.VISIBLE
            } else {
                button.setImageDrawable(mDefaultComplicationDrawable)
                button.contentDescription = mContext.getString(R.string.add_complication)
                background.visibility = View.VISIBLE
            }
        }

        fun initializeComplications() {

            val complicationIds = RiverWatchFace.getComplicationIds()

            mProviderInfoRetriever.retrieveProviderInfo(
                    object : OnProviderInfoReceivedCallback() {
                        override fun onProviderInfoReceived(
                                watchFaceComplicationId: Int,
                                complicationProviderInfo: ComplicationProviderInfo?) {

                            if (complicationProviderInfo != null) {
                                Log.d(TAG, "onProviderInfoReceived: " + complicationProviderInfo.toString())

                                updateComplicationViews(watchFaceComplicationId, complicationProviderInfo)
                            }
                        }
                    },
                    mWatchFaceComponentName,
                    *complicationIds)
        }
    }

    /** Displays icon to indicate there are more options below the fold.  */
    inner class MoreOptionsViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val mMoreOptionsImageView = view.more_options_image_view

        fun setIcon(resourceId: Int) {
            val context = mMoreOptionsImageView.context
            mMoreOptionsImageView.setImageDrawable(context.getDrawable(resourceId))
        }
    }


    /** Numeric text box for entering the gauge ID.  */
    inner class GaugeIdViewHolder(view: View) : RecyclerView.ViewHolder(view), TextView.OnEditorActionListener  {

        private var mGaugeIdTextView = view.stationIdEditBox

        init {
            mGaugeIdTextView.setOnEditorActionListener(this)
        }

        fun setDefault(resourceId: Int) {
            Log.d(TAG, "Setting the default gauge ID")
            val context = mGaugeIdTextView.context
            val defaultSite = context.getString(resourceId)
            val editable = mSharedPref.getString("saved_gauge_id", defaultSite)
            mGaugeIdTextView.setText(editable)

        }

        override fun onEditorAction(textView: TextView?, actionId: Int, p2: KeyEvent?): Boolean {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                val newSite = mGaugeIdTextView.text.toString()
                Log.v("New site ID", newSite)

                val editor = mSharedPref.edit()
                editor.putString("saved_gauge_id", newSite)
                editor.apply()
            }
            return true
        }

    }


    /**
     * Displays switch to indicate whether or not icon appears for unread notifications. User can
     * toggle on/off.
     */
    inner class FahrenheitDisplayViewHolder(view: View) : RecyclerView.ViewHolder(view), OnClickListener {

        private val mFahrenheitSwitch: Switch?

        private var mEnabledIconResourceId: Int = 0
        private var mDisabledIconResourceId: Int = 0

        private var mSharedPrefResourceId: Int = 0

        init {

            mFahrenheitSwitch = view.findViewById(R.id.fahrenheit_display_switch) as Switch
            view.setOnClickListener(this)
        }

        fun setName(name: String) {
            mFahrenheitSwitch!!.text = name
        }

        fun setIcons(enabledIconResourceId: Int, disabledIconResourceId: Int) {

            mEnabledIconResourceId = enabledIconResourceId
            mDisabledIconResourceId = disabledIconResourceId

            val context = mFahrenheitSwitch!!.context

            // Set default to enabled.
            mFahrenheitSwitch.setCompoundDrawablesWithIntrinsicBounds(
                    context.getDrawable(mEnabledIconResourceId), null, null, null)
        }

        fun setSharedPrefId(sharedPrefId: Int) {
            mSharedPrefResourceId = sharedPrefId

            if (mFahrenheitSwitch != null) {

                val context = mFahrenheitSwitch.context
                val sharedPreferenceString = context.getString(mSharedPrefResourceId)
                val currentState = mSharedPref.getBoolean(sharedPreferenceString, true)

                updateIcon(context, currentState)
            }
        }

        private fun updateIcon(context: Context, currentState: Boolean?) {
            val currentIconResourceId: Int

            if (currentState!!) {
                currentIconResourceId = mEnabledIconResourceId
            } else {
                currentIconResourceId = mDisabledIconResourceId
            }

            this.mFahrenheitSwitch!!.isChecked = currentState
            this.mFahrenheitSwitch.setCompoundDrawablesWithIntrinsicBounds(
                    context.getDrawable(currentIconResourceId), null, null, null)
        }

        override fun onClick(view: View) {
            val position = adapterPosition
            Log.d(TAG, "Complication onClick() position: $position")

            val context = view.context
            val sharedPreferenceString = context.getString(mSharedPrefResourceId)

            // Since user clicked on a switch, new state should be opposite of current state.
            val newState = !mSharedPref.getBoolean(sharedPreferenceString, true)

            val editor = mSharedPref.edit()
            editor.putBoolean(sharedPreferenceString, newState)
            editor.apply()

            updateIcon(context, newState)
        }
    }


    companion object {

        private const val TAG = "CompConfigAdapter"

        const val TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG = 0
        const val TYPE_MORE_OPTIONS = 1
        const val TYPE_GAUGE_ID_CONFIG = 2
        const val TYPE_FAHRENHEIT_CONFIG = 3
    }
}