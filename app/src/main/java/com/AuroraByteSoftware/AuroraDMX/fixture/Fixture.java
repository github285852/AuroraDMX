package com.AuroraByteSoftware.AuroraDMX.fixture;

import android.widget.RelativeLayout;

import java.util.List;

/**
 * Abstract method used to represent a lighting fixture in the theatre
 * Created by furtchet on 11/14/15.
 */
//abstract：https://blog.csdn.net/qq_42077125/article/details/80725592
public abstract class Fixture {
    public static final int MAX_LEVEL = 255;
    public static final String PRESET_TEXT_COLOR = "#99ccff";
    static final String REGEX_255 = "([0-9]|[0-9][0-9]|[01][0-9][0-9]|2[0-4][0-9]|25[0-5])";

    public abstract RelativeLayout getViewGroup();

    public abstract void setChLevels(List<Integer> a_chLevel);

    public abstract List<Integer> getChLevels();

    public abstract void setupIncrementLevelFade(List<Integer> endVal, double steps);

    public abstract void incrementLevel();

    public abstract void setScrollColor(int scrollColor);

    public abstract void setColumnText(String text);

    public abstract String getChText();

    public abstract void setValuePresets(String text);

    public abstract String getValuePresets();

    public abstract boolean isRGB();

    public abstract void setFixtureNumber(int currentFixtureNum);

    public abstract void updateUi();

    public abstract void setParked(boolean checked);

    public abstract boolean isParked();
}
