package org.idzaaus.twotaps;

import java.util.ArrayList;

import android.graphics.Point;
import android.graphics.PointF;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class KeyboardButton implements View.OnTouchListener {
  private Handler handler;
  private PointF startPoint = new PointF();
  private PointF currentPoint = new PointF();
  private boolean moveFired = false;
  
  Runnable action = new Runnable() {
    @Override public void run() {
      fireHoldEvent(false);
      handler.postDelayed(this, repeatSpeed);
    }
  };
    
  enum Type {
    BACKSPACE,
    SHIFT,
    SYSTEM_MENU,
    REGULAR,
    SPACE
  }
  
  enum System_command {
    CAPS_LOCK,
    SETTINGS,
    NUMERIC
  }
  
  public interface OnHoldListener {
    void onKeyboardButtonHold(KeyboardButton target, boolean hold);
    void onKeyboardButtonMove(KeyboardButton target);
  } 
    
  public int number; //! General number of the button
  public int x, y;   //! Axis number of the button
  public Type type;   
  public int regularNumber; //! Only for type == REGULAR; number of the button over all regular buttons  
  public String singlePressLetter = null; 
  public System_command system_command = null;
  public String numericModeLetter = null;
  private boolean largeText = false;
  public TextView textView = null;
  public TextView candidateTextView = null;
  public ImageButton imageButton;
  private float fontSize;


  
  public void setFontSize(float s) { 
    fontSize = s;
    setLargeText(largeText);
  }
  
/*  public void setTextView(TextView v) { 
    textView = v;
    largeText = false;
  }*/
  
  private ArrayList<OnHoldListener> onHoldListeners = new ArrayList<OnHoldListener>();
  
  
  private final int repeatSpeed = 50;
  private final int repeatDelay = 500;


  public KeyboardButton(Service service, ImageButton imageButton, TextView textView, TextView candidateTextView) {
    this.textView = textView;
    this.imageButton = imageButton;
    this.candidateTextView = candidateTextView;
    //this.service = service;
    
    
    imageButton.setOnTouchListener(this);
    
    

  }
  
  public void addOnHoldListener(OnHoldListener listener) {
    onHoldListeners.add(listener);
  }
  
  private void fireHoldEvent(boolean hold) {
    for(OnHoldListener listener : onHoldListeners) {
      listener.onKeyboardButtonHold(this, hold);
    }
  }
  
  private void fireMoveEvent() {
    for(OnHoldListener listener : onHoldListeners) {
      listener.onKeyboardButtonMove(this);
    }
  }

  
  public void setLargeText(boolean enabled) {
    textView.setTextSize(fontSize);
    candidateTextView.setTextSize((float) (fontSize * 1.3));
    //largeText = enabled;
    //textView.setTextSize(largeText? fontSize * 2 : fontSize);
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    switch(event.getAction()) {
    case MotionEvent.ACTION_DOWN:
      if (handler != null) return true;
      handler = new Handler();
      handler.postDelayed(action, repeatDelay);
      startPoint.set(event.getX(), event.getY());
      break;
    case MotionEvent.ACTION_UP:
      if (handler == null) return true;
      handler.removeCallbacks(action);
      handler = null;
      if (moveFired) {
        moveFired = false;
      } else {
        fireHoldEvent(false);
      }
      break;
    case MotionEvent.ACTION_MOVE:
      if (!moveFired) {
        currentPoint.set(event.getX(), event.getY());
        double d = Math.sqrt(
            Math.pow(startPoint.x - currentPoint.x, 2) + 
            Math.pow(startPoint.y - currentPoint.y, 2));
        double ratio = d / imageButton.getWidth();
        if (ratio > 0.3) {
          fireMoveEvent();
          moveFired = true;
        }
      }
      break;
    }
    return false;
  }
}

