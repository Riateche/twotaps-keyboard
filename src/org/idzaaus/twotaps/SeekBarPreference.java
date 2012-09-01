package org.idzaaus.twotaps;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.preference.DialogPreference;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout;


public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
  private static final String androidns="http://schemas.android.com/apk/res/android";

  private SeekBar mSeekBar;
  private TextView mSplashText, mValueText;
  private Context mContext;

  private String mDialogMessage, mSuffix, mDefaultText;
  private int mDefault, mMax, mMin, mValue = 0;

  public SeekBarPreference(Context context, AttributeSet attrs) { 
    super(context,attrs); 
    mContext = context;

    mDialogMessage = attrs.getAttributeValue(androidns, "dialogMessage");
    mSuffix = attrs.getAttributeValue(androidns, "text");
    mDefault = attrs.getAttributeIntValue(androidns, "defaultValue", 0);
    for (int i = 0; i < attrs.getAttributeCount(); i++) {
      String name = attrs.getAttributeName(i);
      String value = attrs.getAttributeValue(i);
      if (name.equalsIgnoreCase("min")) {
        mMin = Integer.parseInt(value);
      }
      if (name.equalsIgnoreCase("max")) {
        mMax = Integer.parseInt(value);
      }
    }
    mDefaultText = context.getResources().getString(R.string.default_string);

  }
  @Override 
  protected View onCreateDialogView() {
    LinearLayout.LayoutParams params;
    LinearLayout layout = new LinearLayout(mContext);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(6,6,6,6);

    mSplashText = new TextView(mContext);
    if (mDialogMessage != null)
      mSplashText.setText(mDialogMessage);
    layout.addView(mSplashText);

    mValueText = new TextView(mContext);
    mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
    mValueText.setTextSize(32);
    params = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.FILL_PARENT, 
        LinearLayout.LayoutParams.WRAP_CONTENT);
    layout.addView(mValueText, params);

    mSeekBar = new SeekBar(mContext);
    mSeekBar.setOnSeekBarChangeListener(this);
    layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

    if (shouldPersist())
      mValue = getPersistedInt(mDefault);

    mSeekBar.setMax(mMax - mMin);
    mSeekBar.setProgress(mValue - mMin);
    return layout;
  }
  @Override 
  protected void onBindDialogView(View v) {
    super.onBindDialogView(v);
    mSeekBar.setMax(mMax - mMin);
    mSeekBar.setProgress(mValue - mMin);
    updateText();
  }
  @Override
  protected void onSetInitialValue(boolean restore, Object defaultValue) {
    super.onSetInitialValue(restore, defaultValue);
    if (restore) 
      mValue = shouldPersist() ? getPersistedInt(mDefault) : 0;
    else 
      mValue = (Integer) defaultValue;
    updateText();
  }
  
  private void updateText() {
    if (mSeekBar == null) return;
    int value = mSeekBar.getProgress();
    int result = value == 0 ? -1 : value + mMin;
    if (value == 0 && mDefaultText != null) {
      mValueText.setText(mDefaultText);
    } else {
      String t = String.valueOf(result);
      mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
    }
    
  }

  public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
    int result = value == 0 ? -1 : value + mMin;
    if (shouldPersist())
      persistInt(result);
    updateText();
    callChangeListener(Integer.valueOf(result));
  }
  //public void onStartTrackingTouch(SeekBar seek) {}
  //public void onStopTrackingTouch(SeekBar seek) {}

  @Override
  public void onStartTrackingTouch(SeekBar arg0) {  }
  @Override
  public void onStopTrackingTouch(SeekBar arg0) { }

  //public void setMax(int max) { mMax = max; }
  //public int getMax() { return mMax; }

  /*public void setProgress(int progress) { 
    mValue = progress;
    if (mSeekBar != null)
      mSeekBar.setProgress(progress); 
  }
  public int getProgress() { return mValue; } */
}