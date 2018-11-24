package com.paddlebike.kenandrews.riverwatch

import android.content.*
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.content.ContextCompat
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationData.TYPE_SHORT_TEXT
import android.support.wearable.complications.SystemProviders.DATE
import android.support.wearable.complications.SystemProviders.WATCH_BATTERY
import android.support.wearable.complications.rendering.ComplicationDrawable
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.WindowInsets
import com.paddlebike.kenandrews.riverwatch.config.ComplicationConfigRecyclerViewAdapter
import com.paddlebike.kenandrews.riverwatch.config.ComplicationConfigRecyclerViewAdapter.ComplicationLocation.*
import net.danlew.android.joda.JodaTimeAndroid
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.lang.ref.WeakReference
import java.util.*
import android.support.wearable.complications.ComplicationHelperActivity
import android.content.Intent
import android.content.ComponentName
import android.app.PendingIntent
import android.support.wearable.complications.ComplicationData.TYPE_LONG_TEXT
import com.paddlebike.kenandrews.riverwatch.complicationProvider.ComplicationSiteModel
import com.paddlebike.kenandrews.riverwatch.complicationProvider.USGSStreamLevelComplication
import com.paddlebike.kenandrews.riverwatch.complicationProvider.USGSStreamSummaryComplication


/**
 * Digital watch face
 *
 *
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
class RiverWatchFace : CanvasWatchFaceService() {


    companion object {
        private val NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        /**
         * Updates rate in milliseconds for interactive mode. We update once a second since seconds
         * are displayed in interactive mode.
         */
        private const val INTERACTIVE_UPDATE_RATE_MS = 60000

        /**
         * Handler message id for updating the time periodically in interactive mode.
         */
        private const val MSG_UPDATE_TIME = 0

        private const val LEFT_COMPLICATION_ID = 100
        private const val CENTER_COMPLICATION_ID = 101
        private const val RIGHT_COMPLICATION_ID = 102
        private const val BOTTOM_COMPLICATION_ID = 103

        // Background, Left and right complication IDs as array for Complication API.
        private val COMPLICATION_IDS = intArrayOf(LEFT_COMPLICATION_ID, CENTER_COMPLICATION_ID, RIGHT_COMPLICATION_ID, BOTTOM_COMPLICATION_ID)

        // supported types.
        private val COMPLICATION_SUPPORTED_TYPES = arrayOf(
                intArrayOf(ComplicationData.TYPE_RANGED_VALUE,
                        ComplicationData.TYPE_ICON,
                        ComplicationData.TYPE_SHORT_TEXT,
                        ComplicationData.TYPE_SMALL_IMAGE),
                intArrayOf(ComplicationData.TYPE_RANGED_VALUE,
                        ComplicationData.TYPE_ICON,
                        ComplicationData.TYPE_SHORT_TEXT,
                        ComplicationData.TYPE_SMALL_IMAGE),
                intArrayOf(ComplicationData.TYPE_RANGED_VALUE,
                        ComplicationData.TYPE_ICON,
                        ComplicationData.TYPE_SHORT_TEXT,
                        ComplicationData.TYPE_SMALL_IMAGE),
                intArrayOf(ComplicationData.TYPE_LONG_TEXT))

        // Used by {@link ComplicationConfigRecyclerViewAdapter} to check if complication location
        // is supported in settings config activity.
        fun getComplicationId(
                complicationLocation: ComplicationConfigRecyclerViewAdapter.ComplicationLocation): Int {
            // Add any other supported locations here.
            return when (complicationLocation) {
                LEFT -> LEFT_COMPLICATION_ID
                CENTER -> CENTER_COMPLICATION_ID
                RIGHT -> RIGHT_COMPLICATION_ID
                BOTTOM -> BOTTOM_COMPLICATION_ID
                else -> -1
            }
        }

        // Used by {@link ComplicationConfigRecyclerViewAdapter} to retrieve all complication ids.
        fun getComplicationIds(): IntArray {
            return COMPLICATION_IDS
        }

        // Used by {@link ComplicationConfigRecyclerViewAdapter} to see which complication types
        // are supported in the settings config activity.
        fun getSupportedComplicationTypes(
                complicationLocation: ComplicationConfigRecyclerViewAdapter.ComplicationLocation): IntArray {
            // Add any other supported locations here.
            return when (complicationLocation) {
                LEFT -> COMPLICATION_SUPPORTED_TYPES[0]
                CENTER -> COMPLICATION_SUPPORTED_TYPES[1]
                RIGHT -> COMPLICATION_SUPPORTED_TYPES[2]
                BOTTOM -> COMPLICATION_SUPPORTED_TYPES[3]
                else -> intArrayOf()
            }
        }
    }

    override fun onCreateEngine(): Engine {
        JodaTimeAndroid.init(this)
        return Engine()
    }



    private class EngineHandler(reference: RiverWatchFace.Engine) : Handler() {
        private val mWeakReference: WeakReference<RiverWatchFace.Engine> = WeakReference(reference)

        override fun handleMessage(msg: Message) {
            val engine = mWeakReference.get()
            when (msg.what) {
                MSG_UPDATE_TIME -> engine?.handleUpdateTimeMessage()
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        private val TAG ="CanvasWatchFaceService"

        private lateinit var mCalendar: Calendar

        private var mRegisteredTimeZoneReceiver = false

        private var mXOffset: Float = 0F
        private var mYOffset: Float = 0F

        private lateinit var mBackgroundPaint: Paint
        private lateinit var mTimeTextPaint: Paint
        private lateinit var mDateTextPaint: Paint

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private var mLowBitAmbient: Boolean = false
        private var mBurnInProtection: Boolean = false
        private var mAmbient: Boolean = false

        /* Maps active complication ids to the data for that complication. Note: Data will only be
         * present if the user has chosen a provider via the settings activity for the watch face.
         */
        private val mComplicationDataArray: SparseArray<ComplicationData> = SparseArray(COMPLICATION_IDS.size)

        /* Maps complication ids to corresponding ComplicationDrawable that renders the
         * the complication data on the watch face.
         */
        private val mComplicationDrawableArray: SparseArray<ComplicationDrawable> = SparseArray(COMPLICATION_IDS.size)

        private var mFahrenheitDisplayPreference: Boolean = false

        private val mUpdateTimeHandler: Handler = EngineHandler(this)

        private val mTimeZoneReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }


        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(WatchFaceStyle.Builder(this@RiverWatchFace)
                    .setAcceptsTapEvents(true)
                    .build())

            initializeComplications()

            loadSavedPreferences()

            mCalendar = Calendar.getInstance()

            val resources = this@RiverWatchFace.resources
            mYOffset = resources.getDimension(R.dimen.digital_y_offset)

            // Initializes background.
            mBackgroundPaint = Paint().apply {
                color = ContextCompat.getColor(applicationContext, R.color.background)
            }

            // Initializes date part of the watch Face.
            mDateTextPaint = Paint().apply {
                typeface = NORMAL_TYPEFACE
                isAntiAlias = true
                color = ContextCompat.getColor(applicationContext, R.color.digital_text)
                textSize = 24f
            }
            // Initializes time part of the watch Face.
            mTimeTextPaint = Paint().apply {
                typeface = NORMAL_TYPEFACE
                isAntiAlias = true
                color = ContextCompat.getColor(applicationContext, R.color.digital_text)
            }
        }

        // Pulls all user's preferences for watch face appearance.
        private fun loadSavedPreferences() {

            val sharedPref = applicationContext.getSharedPreferences(
                    getString(R.string.watchface_prefs),
                    Context.MODE_PRIVATE)
            //val gaugeId = applicationContext.getString(R.string.prefs_site_id)
            //Log.d(TAG, "Got Site ID from prefs: " + gaugeId)


            if (sharedPref != null) {
                val fahrenheitDisplayPreferenceResourceName = applicationContext.getString(R.string.fahrenheit_display_pref)
                mFahrenheitDisplayPreference = sharedPref.getBoolean(fahrenheitDisplayPreferenceResourceName, true)
            }

        }

        private fun initializeComplications() {
            Log.d(TAG, "initializeComplications()")

            val riverLevelProvider = ComponentName(applicationContext, USGSStreamLevelComplication::class.java)
            val riverSummaryProvider = ComponentName(applicationContext, USGSStreamSummaryComplication::class.java)


            val left = ComplicationDrawable(applicationContext)
            val center = ComplicationDrawable(applicationContext)
            val right = ComplicationDrawable(applicationContext)
            val bottom = ComplicationDrawable(applicationContext)

            setDefaultComplicationProvider(LEFT_COMPLICATION_ID, riverLevelProvider, TYPE_SHORT_TEXT)
            setDefaultSystemComplicationProvider(CENTER_COMPLICATION_ID, DATE, TYPE_SHORT_TEXT)
            setDefaultSystemComplicationProvider(RIGHT_COMPLICATION_ID, WATCH_BATTERY, TYPE_SHORT_TEXT)
            setDefaultComplicationProvider(BOTTOM_COMPLICATION_ID, riverSummaryProvider, TYPE_LONG_TEXT)


            // Adds new complications to a SparseArray to simplify setting styles and ambient
            // properties for all complications, i.e., iterate over them all.
            mComplicationDrawableArray.put(LEFT_COMPLICATION_ID, left)
            mComplicationDrawableArray.put(CENTER_COMPLICATION_ID, center)
            mComplicationDrawableArray.put(RIGHT_COMPLICATION_ID, right)
            mComplicationDrawableArray.put(BOTTOM_COMPLICATION_ID, bottom)

            setActiveComplications(*COMPLICATION_IDS)

        }


        private fun drawComplications(canvas: Canvas, currentTimeMillis: Long) {
            var complicationId: Int
            var complicationDrawable: ComplicationDrawable

            for (i in COMPLICATION_IDS.indices) {
                complicationId = COMPLICATION_IDS[i]
                complicationDrawable = mComplicationDrawableArray.get(complicationId)

                complicationDrawable.setVisible(true, false)
                complicationDrawable.draw(canvas, currentTimeMillis)

            }
        }

        override fun onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)

            // Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient)

            mLowBitAmbient = properties.getBoolean(WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false)
            mBurnInProtection = properties.getBoolean(WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false)

            // Updates complications to properly render in ambient mode based on the
            // screen's capabilities.
            var complicationDrawable: ComplicationDrawable

            for (i in COMPLICATION_IDS.indices) {
                complicationDrawable = mComplicationDrawableArray.get(COMPLICATION_IDS[i])

                complicationDrawable.setLowBitAmbient(mLowBitAmbient)
                complicationDrawable.setBurnInProtection(mBurnInProtection)
            }
        }

        /*
         * Called when there is updated data for a complication id.
         */
        override fun onComplicationDataUpdate(complicationId: Int, complicationData: ComplicationData?) {
            // Log.d(TAG, "onComplicationDataUpdate() id: " + complicationId)

            // Adds/updates active complication data in the array.
            mComplicationDataArray.put(complicationId, complicationData)

            // Updates correct ComplicationDrawable with updated data.
            val complicationDrawable = mComplicationDrawableArray.get(complicationId)
            complicationDrawable.setComplicationData(complicationData)

            invalidate()
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            mAmbient = inAmbientMode

            if (mLowBitAmbient) {
                mTimeTextPaint.isAntiAlias = !inAmbientMode
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer()
        }

        /*
         * Determines if tap inside a complication area or returns -1.
         */
        private fun getTappedComplicationId(x: Int, y: Int): Int {
            var complicationData: ComplicationData?
            var complicationDrawable: ComplicationDrawable

            val currentTimeMillis = System.currentTimeMillis()

            for (complicationId in COMPLICATION_IDS) {
                complicationData = mComplicationDataArray.get(complicationId)

                if (complicationData != null
                        && complicationData.isActive(currentTimeMillis)
                        && complicationData.type != ComplicationData.TYPE_NOT_CONFIGURED
                        && complicationData.type != ComplicationData.TYPE_EMPTY) {

                    complicationDrawable = mComplicationDrawableArray.get(complicationId)
                    val complicationBoundingRect = complicationDrawable.bounds

                    if (complicationBoundingRect.width() > 0) {
                        if (complicationBoundingRect.contains(x, y)) {
                            return complicationId
                        }
                    } else {
                        Log.e(TAG, "Not a recognized complication id.")
                    }
                }
            }
            return -1
        }

        /**
         * Captures tap event (and tap type)
         */
        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TOUCH -> {
                    // The user has started touching the screen.
                }
                WatchFaceService.TAP_TYPE_TOUCH_CANCEL -> {
                    // The user has started a different gesture or otherwise cancelled the tap.
                }
                WatchFaceService.TAP_TYPE_TAP -> {
                    val tappedComplicationId = getTappedComplicationId(x, y)
                    if (tappedComplicationId != -1) {
                        onComplicationTap(tappedComplicationId)
                    }
                }
            }
            invalidate()
        }

        private fun onComplicationTap(complicationId: Int) {
            Log.d(TAG, "onComplicationTap()")

            val complicationData = mComplicationDataArray.get(complicationId)

            if (complicationData != null) {

                if (complicationData.tapAction != null) {
                    try {
                        complicationData.tapAction.send()
                    } catch (e: PendingIntent.CanceledException) {
                        Log.e(TAG, "onComplicationTap() tap action error: $e")
                    }

                } else if (complicationData.type == ComplicationData.TYPE_NO_PERMISSION) {

                    // Watch face does not have permission to receive complication data, so launch
                    // permission request.
                    val componentName = ComponentName(applicationContext, ComplicationSiteModel::class.java)

                    val permissionRequestIntent = ComplicationHelperActivity.createPermissionRequestHelperIntent(
                            applicationContext, componentName)

                    startActivity(permissionRequestIntent)
                }

            } else {
                Log.d(TAG, "No PendingIntent for complication $complicationId.")
            }
        }


        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)


            // For most Wear devices, width and height are the same, so we just chose one (width).
            val sizeOfComplication = width / 4
            val midpointOfScreen = width / 2

            val padding = 16
            var horizontalOffset = padding
            var verticalOffset = midpointOfScreen - sizeOfComplication / 2

            val leftBounds =
                    // Left, Top, Right, Bottom
                    Rect(horizontalOffset, verticalOffset,
                            horizontalOffset + sizeOfComplication, verticalOffset + sizeOfComplication)

            horizontalOffset += sizeOfComplication + padding

            val leftComplicationDrawable = mComplicationDrawableArray.get(LEFT_COMPLICATION_ID)
            leftComplicationDrawable.bounds = leftBounds

            val centerBound =
                    // Left, Top, Right, Bottom
                    Rect(horizontalOffset, verticalOffset,
                            horizontalOffset + sizeOfComplication,
                            verticalOffset + sizeOfComplication)

            horizontalOffset += sizeOfComplication + padding
            val centerComplicationDrawable = mComplicationDrawableArray.get(CENTER_COMPLICATION_ID)
            centerComplicationDrawable.bounds = centerBound

            val rightBounds =
                    // Left, Top, Right, Bottom
                    Rect(horizontalOffset, verticalOffset,
                             horizontalOffset + sizeOfComplication,
                            verticalOffset + sizeOfComplication)

            val rightComplicationDrawable = mComplicationDrawableArray.get(RIGHT_COMPLICATION_ID)
            rightComplicationDrawable.bounds = rightBounds

            verticalOffset += padding + sizeOfComplication
            val bottomBounds = Rect(padding, verticalOffset, width - padding, width - padding)
            val bottomComplicationDrawable = mComplicationDrawableArray.get(BOTTOM_COMPLICATION_ID)
            bottomComplicationDrawable.bounds = bottomBounds
        }


        override fun onDraw(canvas: Canvas, bounds: Rect) {
            // Draw the background.
            if (mAmbient) {
                canvas.drawColor(Color.BLACK)
            } else {
                canvas.drawRect(
                        0f, 0f, bounds.width().toFloat(), bounds.height().toFloat(), mBackgroundPaint)
            }

            val now = DateTime.now()
            mCalendar.timeInMillis = now.millis

            drawComplications(canvas, now.millis)

            val dateString =  now.toLocalDate().toString(DateTimeFormat.forPattern("MM/dd"))
            canvas.drawText(dateString, mXOffset , mYOffset, mDateTextPaint)

            val timeString = now.toLocalTime().toString(DateTimeFormat.forPattern("HH:mm"))
           canvas.drawText(timeString, mXOffset + 70, mYOffset, mTimeTextPaint)

        }


        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()

                // Update time zone in case it changed while we weren't visible.
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer()
        }


        private fun registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@RiverWatchFace.registerReceiver(mTimeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = false
            this@RiverWatchFace.unregisterReceiver(mTimeZoneReceiver)
        }

        override fun onApplyWindowInsets(insets: WindowInsets) {
            super.onApplyWindowInsets(insets)

            // Load resources that have alternate values for round watches.
            val resources = this@RiverWatchFace.resources
            val isRound = insets.isRound
            mXOffset = resources.getDimension(
                    if (isRound)
                        R.dimen.digital_x_offset_round
                    else
                        R.dimen.digital_x_offset
            )

            val textSize = resources.getDimension(
                    if (isRound)
                        R.dimen.digital_text_size_round
                    else
                        R.dimen.digital_text_size
            )

            mTimeTextPaint.textSize = textSize
        }

        /**
         * Starts the [.mUpdateTimeHandler] timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private fun updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        /**
         * Returns whether the [.mUpdateTimeHandler] timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !isInAmbientMode
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()
                val delayMs = INTERACTIVE_UPDATE_RATE_MS - timeMs % INTERACTIVE_UPDATE_RATE_MS
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }
    }


}
