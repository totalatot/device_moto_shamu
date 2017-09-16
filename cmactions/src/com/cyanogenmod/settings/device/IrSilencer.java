/*
 * Copyright (c) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.settings.device;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.UserHandle;
import android.telephony.PhoneStateListener;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import static com.cyanogenmod.settings.device.IrGestureManager.*;
import static android.telephony.TelephonyManager.*;

public class IrSilencer extends PhoneStateListener implements SensorEventListener, UpdatedStateNotifier {
    private static final String TAG = "CMActions-IRSilencer";

    private static final int IR_GESTURES_FOR_RINGING = (1 << IR_GESTURE_SWIPE) | (1 << IR_GESTURE_APPROACH);
    private static final int SILENCE_DELAY_MS = 500;

    private final Context mContext;
    private final TelecomManager mTelecomManager;
    private final TelephonyManager mTelephonyManager;
    private final CMActionsSettings mCMActionsSettings;
    private final SensorHelper mSensorHelper;
    private final Sensor mSensor;
    private final IrGestureVote mIrGestureVote;

    private boolean irEnable;

    private boolean mPhoneRinging;
    private long mPhoneRingStartedMs;

    private boolean mAlarmRinging;
    private long mAlarmRingStartedMs;

    private int mAlarmAction;

    // Alarm snoozing
    private static final String ALARM_ALERT_ACTION = "com.android.deskclock.ALARM_ALERT";
    private static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";
    private static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";
    private static final String ALARM_DONE_ACTION = "com.android.deskclock.ALARM_DONE";
    private static final String GOOGLE_ALARM_ALERT_ACTION = "android.intent.action.ALARM_ALERT";
    private static final String GOOGLE_ALARM_DISMISS_ACTION = "android.intent.action.ALARM_DISMISS";
    private static final String GOOGLE_ALARM_SNOOZE_ACTION = "android.intent.action.ALARM_SNOOZE";
    private static final String GOOGLE_ALARM_DONE_ACTION = "android.intent.action.ALARM_DONE";

    // Alarm action
    private static final int ACTION_NONE = 0;
    private static final int ACTION_DISMISS = 1;
    private static final int ACTION_SNOOZE = 2;

    public IrSilencer(CMActionsSettings cmActionsSettings, Context context,
                SensorHelper sensorHelper, IrGestureManager irGestureManager) {
        mContext = context;
        mTelecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        mCMActionsSettings = cmActionsSettings;
        mSensorHelper = sensorHelper;
        mSensor = sensorHelper.getIrGestureSensor();
        mIrGestureVote = new IrGestureVote(irGestureManager);
        mIrGestureVote.voteForSensors(0);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ALARM_ALERT_ACTION);
        intentFilter.addAction(ALARM_DISMISS_ACTION);
        intentFilter.addAction(ALARM_SNOOZE_ACTION);
        intentFilter.addAction(ALARM_DONE_ACTION);
        intentFilter.addAction(GOOGLE_ALARM_ALERT_ACTION);
        intentFilter.addAction(GOOGLE_ALARM_DISMISS_ACTION);
        intentFilter.addAction(GOOGLE_ALARM_SNOOZE_ACTION);
        intentFilter.addAction(GOOGLE_ALARM_DONE_ACTION);
        mContext.registerReceiver(mAlarmStateReceiver, intentFilter);
    }

    @Override
    public void updateState() {
        if (mCMActionsSettings.isIrSilencerEnabled()) {
            mTelephonyManager.listen(this, LISTEN_CALL_STATE);
            mAlarmAction = mCMActionsSettings.getWaveAlarmAction();
        } else {
            mTelephonyManager.listen(this, 0);
            // make sure we're disabled
            mPhoneRinging = false;
            mAlarmRinging = false;
            //mAlarmAction = 0;
        }
    }

    @Override
    public synchronized void onSensorChanged(SensorEvent event) {
        int gesture = (int) event.values[1];
        if (gesture == IR_GESTURE_SWIPE || gesture == IR_GESTURE_APPROACH) {
            if (mPhoneRinging) {
                long now = System.currentTimeMillis();
                if (now - mPhoneRingStartedMs >= SILENCE_DELAY_MS) {
                    Log.d(TAG, "Silencing ringer");
                    mTelecomManager.silenceRinger();
                } else {
                    Log.d(TAG, "Ignoring silence gesture: " + now + " is too close to " +
                            mPhoneRingStartedMs + ", delay=" + SILENCE_DELAY_MS);
                }
            } else if (mAlarmRinging) {
                long now = System.currentTimeMillis();
                if (now - mAlarmRingStartedMs >= SILENCE_DELAY_MS) {
                    if (mAlarmAction == ACTION_DISMISS) {
                        Log.d(TAG, "Dismissing alarm in case of ACTION_DISMISS");
                        // for DeskClock 5.0.1-
                        mContext.sendBroadcastAsUser(new Intent(ALARM_DISMISS_ACTION),
                            new UserHandle(UserHandle.USER_CURRENT));
                        // for DeskClock 5.1+
                        // mContext.sendBroadcastAsUser(new Intent(GOOGLE_ALARM_DISMISS_ACTION), new UserHandle(UserHandle.USER_CURRENT));
                    } else if (mAlarmAction == ACTION_SNOOZE) {
                        Log.d(TAG, "Snoozing alarm in case of ACTION_SNOOZE");
                        // for DeskClock 5.0.1-
                        mContext.sendBroadcastAsUser(new Intent(ALARM_SNOOZE_ACTION),
                            new UserHandle(UserHandle.USER_CURRENT));
                        // for DeskClock 5.1+
                        // mContext.sendBroadcastAsUser(new Intent(GOOGLE_ALARM_SNOOZE_ACTION), new UserHandle(UserHandle.USER_CURRENT));
                    } else {
                        Log.d(TAG, "Alarm Snoozing is set ACTION_NONE so do nothing");
                    }
                } else {
                    Log.d(TAG, "Ignoring silence gesture: " + now + " is too close to " +
                            mAlarmRingStartedMs + ", delay=" + SILENCE_DELAY_MS);
                }
            }
        }
    }

    @Override
    public synchronized void onCallStateChanged(int state, String incomingNumber) {
        if (state == CALL_STATE_RINGING && !mPhoneRinging) {
            Log.d(TAG, "Phone ringing started");
            irEnabler(true);
            mPhoneRinging = true;
            mPhoneRingStartedMs = System.currentTimeMillis();
        } else if (state != CALL_STATE_RINGING && mPhoneRinging) {
            Log.d(TAG, "Phone ringing stopped");
            irEnabler(false);
            mPhoneRinging = false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor mSensor, int accuracy) {
    }

    private BroadcastReceiver mAlarmStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
           if (!mCMActionsSettings.isIrSilencerEnabled())
              return;

           String action = intent.getAction();
           if (ALARM_ALERT_ACTION.equals(action)) {
               Log.d(TAG, "Alarm ringing started");
               irEnabler(true);
               mAlarmRinging = true;
               mAlarmRingStartedMs = System.currentTimeMillis();
           } else {
               Log.d(TAG, "Alarm ringing stopped");
               irEnabler(false);
               mAlarmRinging = false;
           }
        }
    };

    public void irEnabler(boolean enable) {
        if (enable && !irEnable) {
            mSensorHelper.registerListener(mSensor, this);
            mIrGestureVote.voteForSensors(IR_GESTURES_FOR_RINGING);
            irEnable = true;
        } else if (!enable && irEnable) {
            mSensorHelper.unregisterListener(this);
            mIrGestureVote.voteForSensors(0);
            irEnable = false;
        }
    }
}
