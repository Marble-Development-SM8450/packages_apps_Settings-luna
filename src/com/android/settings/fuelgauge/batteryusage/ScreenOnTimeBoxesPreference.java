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

package com.android.settings.fuelgauge.batteryusage;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.widget.GroupSectionDividerMixin;

import com.android.settings.R;

public class ScreenOnTimeBoxesPreference extends Preference implements GroupSectionDividerMixin {

    private CharSequence mUsedSinceChargeText;
    private CharSequence mScreenOnSinceChargeText;

    public ScreenOnTimeBoxesPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_screen_on_time_boxes);
    }

    void setTimes(CharSequence usedSinceChargeText, CharSequence screenOnSinceChargeText) {
        mUsedSinceChargeText = usedSinceChargeText;
        mScreenOnSinceChargeText = screenOnSinceChargeText;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        final TextView usedValue = (TextView) view.findViewById(R.id.used_since_charge_value);
        final TextView screenOnValue =
                (TextView) view.findViewById(R.id.screen_on_since_charge_value);
        usedValue.setText(mUsedSinceChargeText);
        screenOnValue.setText(mScreenOnSinceChargeText);
    }
}
