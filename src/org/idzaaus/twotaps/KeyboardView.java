package org.idzaaus.twotaps;

import java.util.ArrayList;

import org.idzaaus.twotaps.KeyboardButton.System_command;
import org.idzaaus.twotaps.KeyboardButton.Type;

import org.idzaaus.twotaps.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class KeyboardView extends LinearLayout  implements OnClickListener, KeyboardButton.OnHoldListener, OnSharedPreferenceChangeListener {
  private Service service;
  private Point buttonsCount                = new Point(5, 3);
  private ArrayList<KeyboardButton> buttons = new ArrayList<KeyboardButton>();
  private ArrayList<String> layout = null;
  
  private final int regularButtonsCount = 12;
  private boolean shiftPressed = false;
  private boolean capsEnabled = false;
  private boolean inSystemMenu = false;
  private KeyboardButton firstButton = null; // pressed button (or null if none was pressed)
  private boolean numericMode = false;
  private boolean vibrate = false; 
  private Vibrator vibrator;
  private int screenWidth, screenHeight;
  
  enum Align { CENTER, LEFT, RIGHT };
  private Align align = Align.CENTER;
    
  
  public KeyboardView(Service service) {
    super(service);
    Log.i("", "KeyboardView construct!");
    this.service = service;
    //service.getLayoutInflater().inflate(resource, root)
    inflate(service, R.layout.keyboard, this);
    
    WindowManager wm = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
    Display display = wm.getDefaultDisplay();
    screenWidth = display.getWidth();
    screenHeight = display.getHeight();
    
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(service);
    preferences.registerOnSharedPreferenceChangeListener(this);
    vibrator = (Vibrator) service.getSystemService(Context.VIBRATOR_SERVICE);
        
    Resources r = service.getResources();
    for(int y = 0; y < buttonsCount.y; y++) {
      for(int x = 0; x < buttonsCount.x; x++) {
        int res_id = r.getIdentifier("button" + (y+1) + "_" + (x+1), "id", "org.idzaaus.twotaps");
        View view = findViewById(res_id);
        KeyboardButton b = new KeyboardButton(service, (ImageButton) view.findViewById(R.id.button), (TextView) view.findViewById(R.id.text));
        b.number = x + y * buttonsCount.x;
        b.x = x;
        b.y = y;
        if (x == buttonsCount.x - 1) {
          if (y == 0) {
            b.type = Type.BACKSPACE;
          } else if (y == 1) {
            b.type = Type.SHIFT;
          } else if (y == 2) {
            b.type = Type.SYSTEM_MENU;
          }
        } else {
          b.type = Type.REGULAR;
          b.regularNumber = x + y * (buttonsCount.x - 1);
        }
        
        
        b.imageButton.setOnClickListener(this);
        b.addOnHoldListener(this);
        buttons.add(b);
      }
    } 
    
    loadSettings(preferences);

    buttons.get(0).system_command = System_command.CAPS_LOCK;    
    buttons.get(1).system_command = System_command.NUMERIC;    
    buttons.get(2).system_command = System_command.SETTINGS;    
    String[] numericLayout = {"1", "2", "3", "*", null, "4", "5", "6", "#", "+", "7", "8", "9", "0" };
    for(int i = 0; i < numericLayout.length; i++) {
      if (numericLayout[i] != null) {
        buttons.get(i).numericModeLetter = numericLayout[i];        
      }
    }
    
    /*int singlePressButtonsCount = regularButtonsCount - 
        (int) Math.ceil(layout.size() / (double) regularButtonsCount);
    Log.i("", "rbc" + regularButtonsCount + " count=" + layout.size() + " r=" + singlePressButtonsCount);
    for(int i = regularButtonsCount - singlePressButtonsCount; i < regularButtonsCount; i++) {        
      int best_key_id = i - regularButtonsCount + singlePressButtonsCount;
      if (best_key_id < best_keys.length) {
        getButtonByRegularNumber(i).singlePressLetter = best_keys[best_key_id];
      } 
    }*/

    //TODO: most used letters
    getButtonByRegularNumber(regularButtonsCount - 1).singlePressLetter = " "; // space key
    
    updateButtonsText();
  }
  
  private KeyboardButton getButtonByRegularNumber(int number) {
    for(int i = 0; i < buttons.size(); i++) {
      if (buttons.get(i).type == Type.REGULAR && buttons.get(i).regularNumber == number) {
        return buttons.get(i);
      }
    }
    return null;
  }
  
  private void updateButtonsText() {    
    if (inSystemMenu) {
      for(KeyboardButton button: buttons) {
        button.setLargeText(false);
        button.textView.setText("");
        button.imageButton.setImageResource(R.drawable.key_background);
        if (button.type == Type.BACKSPACE) {
          button.imageButton.setImageResource(R.drawable.key_backspace);
        } else {
          if (button.system_command == null) {
          } else { 
            switch(button.system_command) {
            case SETTINGS: 
              button.imageButton.setImageResource(R.drawable.key_settings2);
              break;
            case CAPS_LOCK: 
              button.textView.setText("CAPS");
              break;
            case NUMERIC:
              button.textView.setText("123");              
            }
          }
        }
      }      
    } else if (numericMode) {
      for(KeyboardButton button: buttons) {
        button.imageButton.setImageResource(R.drawable.key_background);
        if (button.type == Type.BACKSPACE) {
          button.setLargeText(false);
          button.imageButton.setImageResource(R.drawable.key_backspace);
          button.textView.setText("");
        } else if (button.type == Type.SYSTEM_MENU) {
          button.setLargeText(false);
          button.textView.setText("ABC");          
        } else if (button.numericModeLetter != null) {
          button.setLargeText(true);
          button.textView.setText(button.numericModeLetter);
          
        }
      }
    
    } else {     
      for(KeyboardButton button: buttons) {
        button.imageButton.setImageResource(R.drawable.key_background);
        boolean candidate = false;
        String text;
        //KeyboardButton button = buttons.get(i); 
        switch(button.type) {
        case BACKSPACE:
          button.imageButton.setImageResource(R.drawable.key_backspace);
          if (firstButton == null) {
            text = "";
          } else {
            text = "";
          }
          break; 
        case SYSTEM_MENU: 
          button.imageButton.setImageResource(R.drawable.key_settings);
          text = "";
          break; 
        case SHIFT: 
          text = "";
          button.imageButton.setImageResource(R.drawable.key_shift);
          break;
        case REGULAR:
          if (firstButton == null) { 
            if (button.singlePressLetter != null) {
              if (button.singlePressLetter.equals(" ")) {
                button.imageButton.setImageResource(R.drawable.key_space);
                text = "";
              //} else if (button.singlePressLetter.equals("\n")) {
              //  text = "enter";
              } else {
                candidate = true;
                text = button.singlePressLetter;
              }
            } else {
              text = "";
              for(int j = 0; j < regularButtonsCount; j ++) {
                if (j % 4 == 0 && j > 0) text += "\n"; 
                text += letterAt(button, getButtonByRegularNumber(j));
              }
              /*text = letterAt(button, getButtonByRegularNumber(0)) + " " + 
                  letterAt(button, getButtonByRegularNumber(regularButtonsCount / 2)) + " " + 
                  letterAt(button, getButtonByRegularNumber(regularButtonsCount - 1)); */ 
            }
          } else {
            candidate = true;
            text = letterAt(firstButton, button);
          }
          break;
        default:
          text = "";  
        }
        button.textView.setText(text);
        button.setLargeText(candidate);
      }
    }
  }
  
  @Override
  public void onClick(View v) {
    //TODO: optimize
    for(KeyboardButton button: buttons) {
      if (button.imageButton == v) {
        onClickOrHold(button, false);
        return;
      }
    }
  }

  @Override
  public void onKeyboardButtonHold(KeyboardButton target) {
    onClickOrHold(target, true);
  }

  
  private void onClickOrHold(KeyboardButton button, boolean hold) {
    if (vibrate) {
      vibrator.vibrate(80);
    }
    if (inSystemMenu) {
      if (!hold) {
        if (button.type == Type.BACKSPACE) {
          inSystemMenu = false;          
        } else if (button.system_command == System_command.CAPS_LOCK) {
          capsEnabled = !capsEnabled;
          inSystemMenu = false;          
        } else if (button.system_command == System_command.NUMERIC) {
          numericMode  = true;
          inSystemMenu = false;
        } else if (button.system_command == System_command.SETTINGS) {        
          inSystemMenu = false;
          Intent intent = new Intent(service, KeyboardPreferences.class);
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          service.startActivity(intent);
        }
      }
    } else if (numericMode) {
      if (button.type == Type.SYSTEM_MENU) {
        numericMode = false;
      } else if (button.type == Type.BACKSPACE) {
        service.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);        
      } else if (button.numericModeLetter != null) {
        service.typeLetter(button.numericModeLetter);
      }
    } else {    
      if (button.type == Type.SHIFT) {
        if (!hold) shiftPressed = !shiftPressed;
      } else if (button.type == Type.SYSTEM_MENU) {
        inSystemMenu = true;
      } else if (button.type == Type.BACKSPACE) {
        if (firstButton != null) {
          firstButton = null;
        } else {
          service.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
        }
      } else if (button.type == Type.REGULAR) {
        if (firstButton == null) {
          if (button.singlePressLetter != null) {
            service.typeLetter(button.singlePressLetter);          
            if (!hold) shiftPressed = false;
          } else {
            if (!hold) firstButton = button;
          }
        } else {
          service.typeLetter(letterAt(firstButton, button));
          if (!hold) {
            shiftPressed = false;
            firstButton = null;        
          }
        }
      }
    }
    updateButtonsText();           
  }
  
  private String letterAt(KeyboardButton firstButton, KeyboardButton secondButton) {
    try {
      String s = layout.get(firstButton.regularNumber * regularButtonsCount + secondButton.regularNumber);
      if (capsEnabled ^ shiftPressed) s = s.toUpperCase();      
      return s;
    } catch (IndexOutOfBoundsException e) {
      return "";
    }
  }


  @Override
  public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
    Log.i("", "settings changed! " + key);
    loadSettings(preferences);
    updateButtonsText();
  }
  
  public void setNumericMode(boolean enabled) {
    numericMode = enabled;
    updateButtonsText();
  }
  
  public void loadSettings(SharedPreferences preferences) {
    vibrate = preferences.getBoolean("vibration", false); 
    align = Align.valueOf(preferences.getString("align", "center").toUpperCase());
//    LinearLayout.LayoutParams l = (LinearLayout.LayoutParams) (findViewById(R.id.layout1).getLayoutParams());
//    l.gravity = LinearLayout.LayoutParams.
    int gravity;
    //align = Align.LEFT; //DEBUG!
    switch(align) {
    case LEFT:
      gravity = Gravity.LEFT;
      break;
    case RIGHT:
      gravity = Gravity.RIGHT;
      break;
    default:
      gravity = Gravity.CENTER_HORIZONTAL;      
    }
    Log.i("", "align " + align);
    ArrayList<LinearLayout> lines = new ArrayList<LinearLayout>();
    lines.add((LinearLayout) findViewById(R.id.layout1));
    lines.add((LinearLayout) findViewById(R.id.layout2));
    lines.add((LinearLayout) findViewById(R.id.layout3));
    for(LinearLayout line: lines) {
      LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) line.getLayoutParams();
      params.gravity = gravity;
      line.setLayoutParams(params);
    }
    layout = service.getLayout(preferences.getString("layout", "ru"));
    
    float buttonSize = (float) preferences.getInt("button_size", -1);
    if (buttonSize == -1) {
      //auto
      buttonSize = 70;  
    }
    
    /*WindowManager wm = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
    Display display = wm.getDefaultDisplay();
    float w, h;
    int rotation = display.getRotation();
    if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
      Log.i("", "ori1");
      h = screenWidth;
      w = screenHeight;        
    } else {
      Log.i("", "ori2");
      w = screenWidth;
      h = screenHeight;
    }*/
    final float scale = getResources().getDisplayMetrics().density;
    float w = screenWidth, h = screenHeight;
    w /= scale; //convert to dp
    h /= scale;
    float maxWidth = w / 5;
    if (buttonSize > maxWidth) buttonSize = maxWidth;
    Log.i("", "w="+w+" h="+h+ " rot=");
    float maxHeight = (float) (0.7 * h / 3);
    if (buttonSize > maxHeight) buttonSize = maxHeight;
        
    int buttonSizeInPx = (int) (buttonSize * scale + 0.5f);
    

//    Log.i("", "set button size " + button_size);
    float fontSize = (float) (buttonSize / 4.0);
    for(KeyboardButton b: buttons) {
//      Log.i("", "set button size for " + b);
      b.imageButton.setMaxHeight(buttonSizeInPx);
      b.imageButton.setMaxWidth(buttonSizeInPx);
      b.setFontSize(fontSize);
    }
    
  }

}

