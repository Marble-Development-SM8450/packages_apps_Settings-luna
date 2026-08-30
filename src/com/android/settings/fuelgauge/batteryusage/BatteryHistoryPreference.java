/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.fuelgauge.batteryusage;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

/** A preference that hosts the single continuous battery level chart graph. */
public class BatteryHistoryPreference extends Preference {

    private HyperBatteryChartView mHyperChartView;
    private TextView mChartSummaryTextView;

    private List<Integer> mPendingLevels;
    private List<Long> mPendingTimestamps;

    public BatteryHistoryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.battery_chart_graph);
        setSelectable(false);
    }

    /** Sets the chart data. Safe to call before the view is bound. */
    void setChartData(List<Integer> levels, List<Long> timestamps) {
        mPendingLevels = levels;
        mPendingTimestamps = timestamps;
        if (mHyperChartView != null) {
            mHyperChartView.setData(levels, timestamps, new ArrayList<>());
        }
    }

    TextView getChartSummaryTextView() {
        return mChartSummaryTextView;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mChartSummaryTextView = (TextView) view.findViewById(R.id.chart_summary);
        mHyperChartView = (HyperBatteryChartView) view.findViewById(R.id.hyper_battery_chart);
        if (mPendingLevels != null && mPendingTimestamps != null) {
            mHyperChartView.setData(mPendingLevels, mPendingTimestamps, new ArrayList<>());
        }
    }
}
