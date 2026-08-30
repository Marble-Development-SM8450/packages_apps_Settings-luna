/*
 * Copyright (C) 2024 The Lunaris Project
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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HyperBatteryChartView extends View {

    private static final int[] GRID_PERCENTAGES = {100, 75, 50, 25, 0};

    private final Paint mLinePaint = new Paint();
    private final Paint mFillPaint = new Paint();
    private final Paint mGridPaint = new Paint();
    private final Paint mLabelPaint = new Paint();
    private final Paint mYAxisLabelPaint = new Paint();
    private final Paint mDotPaint = new Paint();
    private final Paint mDotRingPaint = new Paint();
    private final Path mLinePath = new Path();
    private final Path mFillPath = new Path();

    private List<Integer> mLevels = new ArrayList<>();
    private List<Long> mTimestamps = new ArrayList<>();
    private List<Boolean> mCharging = new ArrayList<>();

    private int mLineColor = Color.parseColor("#4285F4");

    public HyperBatteryChartView(Context context) {
        this(context, null);
    }

    public HyperBatteryChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(dp(2.5f));
        mLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);
        mLinePaint.setColor(mLineColor);

        mFillPaint.setAntiAlias(true);
        mFillPaint.setStyle(Paint.Style.FILL);

        mGridPaint.setAntiAlias(true);
        mGridPaint.setStyle(Paint.Style.STROKE);
        mGridPaint.setStrokeWidth(dp(1f));
        mGridPaint.setColor(Color.argb(35, 255, 255, 255));
        mGridPaint.setPathEffect(new DashPathEffect(new float[]{dp(3f), dp(4f)}, 0));

        mLabelPaint.setAntiAlias(true);
        mLabelPaint.setTextSize(sp(12f));
        mLabelPaint.setColor(Color.argb(150, 255, 255, 255));

        mYAxisLabelPaint.setAntiAlias(true);
        mYAxisLabelPaint.setTextSize(sp(11f));
        mYAxisLabelPaint.setColor(Color.argb(130, 255, 255, 255));
        mYAxisLabelPaint.setTextAlign(Paint.Align.RIGHT);

        mDotPaint.setAntiAlias(true);
        mDotPaint.setStyle(Paint.Style.FILL);

        mDotRingPaint.setAntiAlias(true);
        mDotRingPaint.setStyle(Paint.Style.FILL);
        mDotRingPaint.setColor(Color.WHITE);
    }

    /** Sets the chart data. levels: 0-100 battery percentage per sample. */
    public void setData(List<Integer> levels, List<Long> timestamps, List<Boolean> charging) {
        mLevels = levels != null ? levels : new ArrayList<>();
        mTimestamps = timestamps != null ? timestamps : new ArrayList<>();
        mCharging = charging != null ? charging : new ArrayList<>();
        invalidate();
    }

    public void setLineColor(int color) {
        mLineColor = color;
        mLinePaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mLevels.size() < 2) {
            return;
        }

        final float yAxisLabelWidth = mYAxisLabelPaint.measureText("100%") + dp(8f);
        final int chartWidth = (int) (getWidth() - yAxisLabelWidth);
        final int chartHeight = getHeight() - (int) dp(24);

        drawGrid(canvas, chartWidth, chartHeight, yAxisLabelWidth);
        drawChart(canvas, chartWidth, chartHeight);
        drawXAxisLabels(canvas, chartWidth, chartHeight);
    }

    private void drawGrid(Canvas canvas, int chartWidth, int chartHeight, float yAxisLabelWidth) {
        final int count = GRID_PERCENTAGES.length;
        for (int i = 0; i < count; i++) {
            float y = chartHeight * i / (float) (count - 1);
            canvas.drawLine(0, y, chartWidth, y, mGridPaint);
            float textY = y + (i == 0 ? mYAxisLabelPaint.getTextSize() * 0.35f
                    : (i == count - 1 ? -dp(2f) : mYAxisLabelPaint.getTextSize() * 0.35f));
            canvas.drawText(GRID_PERCENTAGES[i] + "%",
                    chartWidth + yAxisLabelWidth - dp(8f), textY, mYAxisLabelPaint);
        }
    }

    private void drawChart(Canvas canvas, int chartWidth, int chartHeight) {
        mLinePath.reset();
        mFillPath.reset();

        final int count = mLevels.size();
        final float stepX = chartWidth / (float) (count - 1);

        float[] xs = new float[count];
        float[] ys = new float[count];
        for (int i = 0; i < count; i++) {
            xs[i] = i * stepX;
            float fraction = mLevels.get(i) / 100f;
            ys[i] = chartHeight - (chartHeight * fraction);
        }

        mLinePath.moveTo(xs[0], ys[0]);
        mFillPath.moveTo(xs[0], chartHeight);
        mFillPath.lineTo(xs[0], ys[0]);

        for (int i = 1; i < count; i++) {
            float midX = (xs[i - 1] + xs[i]) / 2f;
            mLinePath.cubicTo(midX, ys[i - 1], midX, ys[i], xs[i], ys[i]);
            mFillPath.cubicTo(midX, ys[i - 1], midX, ys[i], xs[i], ys[i]);
        }
        mFillPath.lineTo(xs[count - 1], chartHeight);
        mFillPath.close();

        mFillPaint.setShader(new LinearGradient(
                0, 0, 0, chartHeight,
                Color.argb(110, Color.red(mLineColor), Color.green(mLineColor), Color.blue(mLineColor)),
                Color.argb(20, Color.red(mLineColor), Color.green(mLineColor), Color.blue(mLineColor)),
                Shader.TileMode.CLAMP));

        canvas.drawPath(mFillPath, mFillPaint);
        canvas.drawPath(mLinePath, mLinePaint);

        // Draw a white-ringed dot at the last (current) point.
        canvas.drawCircle(xs[count - 1], ys[count - 1], dp(5f), mDotRingPaint);
        mDotPaint.setColor(mLineColor);
        canvas.drawCircle(xs[count - 1], ys[count - 1], dp(3.5f), mDotPaint);
    }

    private void drawXAxisLabels(Canvas canvas, int chartWidth, int chartHeight) {
        if (mTimestamps.size() < 2) {
            return;
        }
        final float baselineY = chartHeight + dp(18f);

        mLabelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(formatHour(mTimestamps.get(0)), 0, baselineY, mLabelPaint);

        mLabelPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(getContext().getString(
                com.android.settings.R.string.battery_usage_chart_now_label),
                chartWidth, baselineY, mLabelPaint);
    }

    private String formatHour(long timestampMs) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestampMs);
        return DateFormat.format(
                DateFormat.is24HourFormat(getContext()) ? "HH:mm" : "h a", calendar).toString();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
