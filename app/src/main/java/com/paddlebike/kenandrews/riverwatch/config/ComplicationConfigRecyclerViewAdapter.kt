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
     * Used by associated watch face ([ComplicationWatchFaceService]) to let this
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder? {
        Log.d(TAG, "onCreateViewHolder(): viewType: " + viewType)

        var viewHolder: RecyclerView.ViewHolder? = null

        when (viewType) {
            TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG -> {
                // Need direct reference to watch face preview view holder to update watch face
                // preview based on selections from the user.
                mPreviewAndComplicationsViewHolder = PreviewAndComplicationsViewHolder(
                        LayoutInflater.from(parent.context)
                                .inflate(
                                        R.layout.config_list_preview_and_complications_item,
                                        parent,
                                        false))
                viewHolder = mPreviewAndComplicationsViewHolder
            }

            TYPE_MORE_OPTIONS -> viewHolder = MoreOptionsViewHolder(
                    LayoutInflater.from(parent.context)
                            .inflate(
                                    R.layout.config_list_more_options_item,
                                    parent,
                                    false))

            TYPE_GAUGE_ID_CONFIG -> viewHolder = GaugeIdViewHolder(
                    LayoutInflater.from(parent.context)
                            .inflate(
                                    R.layout.config_list_gauge_id,
                                    parent,
                                    false))

            TYPE_UNREAD_NOTIFICATION_CONFIG -> viewHolder = UnreadNotificationViewHolder(
                    LayoutInflater.from(parent.context)
                            .inflate(
                                    R.layout.config_list_unread_notif_item,
                                    parent,
                                    false))

        }

        return viewHolder
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        Log.d(TAG, "Element $position set.")

        // Pulls all data required for creating the UX for the specific setting option.
        val configItemType = mSettingsDataSet[position]

        when (viewHolder.itemViewType) {
            TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG -> {
                val previewAndComplicationsViewHolder = viewHolder as PreviewAndComplicationsViewHolder

                val previewAndComplicationsConfigItem = configItemType as ComplicationConfigData.PreviewAndComplicationsConfigItem

                val defaultComplicationResourceId = previewAndComplicationsConfigItem.defaultComplicationResourceId
                previewAndComplicationsViewHolder.setDefaultComplicationDrawable(defaultComplicationResourceId)

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


            TYPE_UNREAD_NOTIFICATION_CONFIG -> {
                val unreadViewHolder = viewHolder as UnreadNotificationViewHolder

                val unreadConfigItem = configItemType as ComplicationConfigData.UnreadNotificationConfigItem

                val unreadEnabledIconResourceId = unreadConfigItem.iconEnabledResourceId
                val unreadDisabledIconResourceId = unreadConfigItem.iconDisabledResourceId

                val unreadName = unreadConfigItem.name
                val unreadSharedPrefId = unreadConfigItem.sharedPrefId

                unreadViewHolder.setIcons(
                        unreadEnabledIconResourceId, unreadDisabledIconResourceId)
                unreadViewHolder.setName(unreadName)
                unreadViewHolder.setSharedPrefId(unreadSharedPrefId)
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

        Log.d(TAG, "updateSelectedComplication: " + mPreviewAndComplicationsViewHolder!!)

        // Checks if view is inflated and complication id is valid.
        if (mPreviewAndComplicationsViewHolder != null && mSelectedComplicationId >= 0) {
            mPreviewAndComplicationsViewHolder!!.updateComplicationViews(
                    mSelectedComplicationId, complicationProviderInfo)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        super.onDetachedFromRecyclerView(recyclerView)
        // Required to release retriever for active complication data on detach.
        mProviderInfoRetriever.release()
    }


    /**
     * Displays watch face preview along with complication locations. Allows user to tap on the
     * complication they want to change and preview updates dynamically.
     */
    inner class PreviewAndComplicationsViewHolder(view: View) : RecyclerView.ViewHolder(view), OnClickListener {

        private val mLeftComplicationBackground: ImageView =
                view.findViewById(R.id.left_complication_background) as ImageView

        private val mCenterComplicationBackground: ImageView =
                view.findViewById(R.id.center_complication_background) as ImageView

        private val mRightComplicationBackground: ImageView =
                view.findViewById(R.id.right_complication_background) as ImageView

        private val mBottomComplicationBackground: ImageView =
                view.findViewById(R.id.bottom_complication_background) as ImageView

        private val mLeftComplication: ImageButton =
                view.findViewById(R.id.left_complication) as ImageButton

        private val mCenterComplication: ImageButton =
                view.findViewById(R.id.center_complication) as ImageButton

        private val mRightComplication: ImageButton =
                view.findViewById(R.id.right_complication) as ImageButton

        private val mBottomComplication: ImageButton =
                view.findViewById(R.id.bottom_complication) as ImageButton


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
            if (view == mLeftComplication) {
                Log.d(TAG, "Left Complication click()")
                val currentActivity = view.context as Activity
                launchComplicationHelperActivity(currentActivity, ComplicationLocation.LEFT)

            } else if (view == mCenterComplication) {
                Log.d(TAG, "Center Complication click()")
                val currentActivity = view.context as Activity
                launchComplicationHelperActivity(currentActivity, ComplicationLocation.CENTER)

            } else if (view == mRightComplication) {
                Log.d(TAG, "Right Complication click()")
                val currentActivity = view.context as Activity
                launchComplicationHelperActivity(currentActivity, ComplicationLocation.RIGHT)

            } else if (view == mBottomComplication) {
                Log.d(TAG, "Bottom Complication click()")
                val currentActivity = view.context as Activity
                launchComplicationHelperActivity(currentActivity, ComplicationLocation.BOTTOM)
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

        fun setDefaultComplicationDrawable(resourceId: Int) {

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
            Log.d(TAG, "updateComplicationViews(): id: " + watchFaceComplicationId)
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
                background.visibility = View.INVISIBLE
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

        private val mMoreOptionsImageView: ImageView

        init {
            mMoreOptionsImageView = view.findViewById(R.id.more_options_image_view) as ImageView
        }

        fun setIcon(resourceId: Int) {
            val context = mMoreOptionsImageView.context
            mMoreOptionsImageView.setImageDrawable(context.getDrawable(resourceId))
        }
    }


    /** Numeric text box for entering the gauge ID.  */
    inner class GaugeIdViewHolder(view: View) : RecyclerView.ViewHolder(view), TextView.OnEditorActionListener  {

        private var mGaugeIdTextView: TextView?? = view.findViewById(R.id.stationIdEditBox) as TextView

        init {
            mGaugeIdTextView!!.setOnEditorActionListener(this)
        }

        fun setDefault(resourceId: Int) {
            Log.d(TAG, "Setting the default gauge ID")
            val context = mGaugeIdTextView!!.context
            val defaultSite = context.getString(resourceId)
            mGaugeIdTextView!!.text = mSharedPref.getString("saved_gauge_id", defaultSite)

        }

        override fun onEditorAction(textView: TextView?, actionId: Int, p2: KeyEvent?): Boolean {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                val newSite = mGaugeIdTextView!!.text.toString()
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
    inner class UnreadNotificationViewHolder(view: View) : RecyclerView.ViewHolder(view), OnClickListener {

        private val mUnreadNotificationSwitch: Switch?

        private var mEnabledIconResourceId: Int = 0
        private var mDisabledIconResourceId: Int = 0

        private var mSharedPrefResourceId: Int = 0

        init {

            mUnreadNotificationSwitch = view.findViewById(R.id.unread_notification_switch) as Switch
            view.setOnClickListener(this)
        }

        fun setName(name: String) {
            mUnreadNotificationSwitch!!.text = name
        }

        fun setIcons(enabledIconResourceId: Int, disabledIconResourceId: Int) {

            mEnabledIconResourceId = enabledIconResourceId
            mDisabledIconResourceId = disabledIconResourceId

            val context = mUnreadNotificationSwitch!!.context

            // Set default to enabled.
            mUnreadNotificationSwitch.setCompoundDrawablesWithIntrinsicBounds(
                    context.getDrawable(mEnabledIconResourceId), null, null, null)
        }

        fun setSharedPrefId(sharedPrefId: Int) {
            mSharedPrefResourceId = sharedPrefId

            if (mUnreadNotificationSwitch != null) {

                val context = mUnreadNotificationSwitch.context
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

            mUnreadNotificationSwitch!!.isChecked = currentState
            mUnreadNotificationSwitch.setCompoundDrawablesWithIntrinsicBounds(
                    context.getDrawable(currentIconResourceId), null, null, null)
        }

        override fun onClick(view: View) {
            val position = adapterPosition
            Log.d(TAG, "Complication onClick() position: " + position)

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
        const val TYPE_UNREAD_NOTIFICATION_CONFIG = 3
    }
}