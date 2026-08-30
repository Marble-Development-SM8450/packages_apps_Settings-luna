/*
 * Copyright (C) 2025-2026 The ASCP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.fuelgauge;

import static com.android.settings.fuelgauge.BatteryBroadcastReceiver.BatteryUpdateType.BATTERY_NOT_PRESENT;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.Utils;
import com.android.settingslib.widget.LayoutPreference;

public class HyperBatteryHeaderPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, LifecycleEventObserver {

    private static final int BATTERY_MAX_LEVEL = 100;
    private static final int LOW_BATTERY_THRESHOLD = 20;
    private static final long SHIMMER_DURATION_MS = 1800L;
    private static final int SHIMMER_WIDTH_DP = 120;

    @Nullable @VisibleForTesting BatteryBroadcastReceiver mBatteryBroadcastReceiver;
    @Nullable private LayoutPreference mLayoutPreference;
    @Nullable private View mCardRoot;
    @Nullable private View mShimmerView;
    @Nullable private TextView mBigNumber;
    @Nullable private TextView mSubtitle;
    @Nullable private ProgressBar mProgress;
    @Nullable private ValueAnimator mShimmerAnimator;
    private boolean mIsStarted;
    private boolean mWantsShimmer;

    public HyperBatteryHeaderPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                mBatteryBroadcastReceiver = new BatteryBroadcastReceiver(mContext);
                mBatteryBroadcastReceiver.setBatteryChangedListener(
                        type -> {
                            if (type != BATTERY_NOT_PRESENT) {
                                updateHeader();
                            }
                        });
                break;
            case ON_START:
                mIsStarted = true;
                if (mBatteryBroadcastReceiver != null) {
                    mBatteryBroadcastReceiver.register();
                }
                if (mWantsShimmer) {
                    startShimmer();
                }
                break;
            case ON_STOP:
                mIsStarted = false;
                if (mBatteryBroadcastReceiver != null) {
                    mBatteryBroadcastReceiver.unRegister();
                }
                stopShimmer();
                break;
            default:
                break;
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mLayoutPreference = screen.findPreference(getPreferenceKey());
        if (mLayoutPreference == null) {
            return;
        }
        mCardRoot = mLayoutPreference.findViewById(R.id.battery_hero_card_root);
        mShimmerView = mLayoutPreference.findViewById(R.id.battery_hero_shimmer);
        mBigNumber = mLayoutPreference.findViewById(R.id.battery_hero_big_number);
        mSubtitle = mLayoutPreference.findViewById(R.id.battery_hero_subtitle);
        mProgress = mLayoutPreference.findViewById(R.id.battery_hero_progress);

        if (com.android.settings.Utils.isBatteryPresent(mContext)) {
            updateHeader();
        } else if (mCardRoot != null) {
            mCardRoot.setVisibility(View.GONE);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    private void updateHeader() {
        if (mBigNumber == null || mSubtitle == null || mProgress == null || mCardRoot == null) {
            return;
        }

        Intent batteryBroadcast =
                com.android.settingslib.fuelgauge.BatteryUtils.getBatteryIntent(mContext);
        final int batteryLevel = Utils.getBatteryLevel(batteryBroadcast);
        final int pluggedState =
                batteryBroadcast.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        final boolean isCharging = pluggedState != 0;
        final int chargeCounterUah =
                batteryBroadcast.getIntExtra(BatteryManager.EXTRA_CHARGE_COUNTER, -1);

        mBigNumber.setText(String.valueOf(batteryLevel));
        mProgress.setProgress(batteryLevel);

        if (batteryLevel >= BATTERY_MAX_LEVEL) {
            mSubtitle.setText(mContext.getString(R.string.battery_hero_status_full));
        } else if (isCharging) {
            mSubtitle.setText(mContext.getString(R.string.battery_hero_status_charging));
        } else if (chargeCounterUah > 0) {
            mSubtitle.setText(mContext.getString(
                    R.string.battery_charge_counter_summary, chargeCounterUah / 1_000));
        } else {
            mSubtitle.setText(com.android.settings.Utils.formatPercentage(batteryLevel));
        }

        int cardColor;
        if (isCharging) {
            cardColor = mContext.getColor(R.color.battery_hero_card_charging_color);
        } else if (batteryLevel <= LOW_BATTERY_THRESHOLD) {
            cardColor = mContext.getColor(R.color.battery_hero_card_low_color);
        } else {
            cardColor = mContext.getColor(R.color.battery_hero_card_default_color);
        }

        if (mCardRoot.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) mCardRoot.getBackground().mutate()).setColor(cardColor);
        }

        mWantsShimmer = isCharging && batteryLevel < BATTERY_MAX_LEVEL;
        if (mWantsShimmer && mIsStarted) {
            startShimmer();
        } else {
            stopShimmer();
        }
    }

    private void startShimmer() {
        if (mShimmerView == null || mCardRoot == null || mShimmerAnimator != null) {
            return;
        }
        mShimmerView.setVisibility(View.VISIBLE);
        final float density = mContext.getResources().getDisplayMetrics().density;
        final float shimmerWidthPx = SHIMMER_WIDTH_DP * density;

        mShimmerAnimator = ValueAnimator.ofFloat(0f, 1f);
        mShimmerAnimator.setDuration(SHIMMER_DURATION_MS);
        mShimmerAnimator.setInterpolator(new LinearInterpolator());
        mShimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mShimmerAnimator.setRepeatMode(ValueAnimator.RESTART);
        mShimmerAnimator.addUpdateListener(animation -> {
            if (mShimmerView == null || mCardRoot == null) {
                return;
            }
            float fraction = (float) animation.getAnimatedValue();
            float cardWidth = mCardRoot.getWidth();
            float startX = -shimmerWidthPx;
            float endX = cardWidth + shimmerWidthPx;
            mShimmerView.setTranslationX(startX + fraction * (endX - startX));
        });
        mShimmerAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                if (mShimmerView != null) {
                    mShimmerView.setVisibility(View.GONE);
                }
            }
        });
        mShimmerAnimator.start();
    }

    private void stopShimmer() {
        if (mShimmerAnimator != null) {
            mShimmerAnimator.cancel();
            mShimmerAnimator = null;
        }
        if (mShimmerView != null) {
            mShimmerView.setVisibility(View.GONE);
        }
    }
}
