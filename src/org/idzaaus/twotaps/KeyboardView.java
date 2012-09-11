package org.idzaaus.twotaps;

import java.util.ArrayList;

import org.idzaaus.twotaps.KeyboardButton.System_command;
import org.idzaaus.twotaps.KeyboardButton.Type;

import org.idzaaus.twotaps.R;

import android.R.color;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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

public class KeyboardView extends LinearLayout  implements KeyboardButton.OnHoldListener, OnSharedPreferenceChangeListener {
  private Service service;
  private Point buttonsCount                = new Point(6, 3);
  private ArrayList<KeyboardButton> buttons = new ArrayList<KeyboardButton>();
  private ArrayList<LinearLayout> rowLayouts = new ArrayList<LinearLayout>();
  private ArrayList<String> layout = null;
  
  private int regularButtonsCount = 0;
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
    setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    setOrientation(LinearLayout.VERTICAL);
    setBackgroundResource(color.black); 
    
    
    WindowManager wm = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
    Display display = wm.getDefaultDisplay();
    screenWidth = display.getWidth();
    screenHeight = display.getHeight();
    
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(service);
    preferences.registerOnSharedPreferenceChangeListener(this);
    vibrator = (Vibrator) service.getSystemService(Context.VIBRATOR_SERVICE);
         
    //Log.i("", "start creating buttons");
    for(int y = 0; y < buttonsCount.y; y++) {
      LinearLayout row = new LinearLayout(service);
      rowLayouts.add(row);
      addView(row, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
      for(int x = 0; x < buttonsCount.x; x++) {
        View v = service.getLayoutInflater().inflate(R.layout.button, null);
        row.addView(v);
        KeyboardButton b = new KeyboardButton(
            service, 
            (ImageButton) v.findViewById(R.id.button), 
            (TextView) v.findViewById(R.id.text),
            (TextView) v.findViewById(R.id.candidate_text));
        b.number = x + y * buttonsCount.x;
        b.x = x;
        b.y = y;
        //Log.i("", "create button " + x + " " + y);
        if (x == buttonsCount.x - 1) {
          if (y == 0) {
            b.type = Type.BACKSPACE;
          } else if (y == 1) {
            b.type = Type.SHIFT;
          } else if (y == 2) {
            b.type = Type.SPACE;
          }
        } else {
          b.type = Type.REGULAR;
          regularButtonsCount++;
          b.regularNumber = x + y * (buttonsCount.x - 1);
        }
                
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
    
    getButtonByRegularNumber(regularButtonsCount - 1).singlePressLetter = " "; // space key
    firstButton = buttons.get(0);
    
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
        } else if (button.type == Type.BACKSPACE) {
          button.setLargeText(false);
          button.imageButton.setImageResource(R.drawable.key_space);
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
        String candidateText = "";
        //KeyboardButton button = buttons.get(i); 
        if (button.type == null) continue;
        switch(button.type) {
        case BACKSPACE:
          button.imageButton.setImageResource(R.drawable.key_backspace);
          if (firstButton == null) {
            text = "";
          } else {
            text = "";
          }
          break; 
        case SPACE:
          button.imageButton.setImageResource(R.drawable.key_space);
          text = "";
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
            }
          } else {
            //candidate = true;
            candidateText = letterAt(firstButton, button);
            text = "";
            for(int j = 0; j < regularButtonsCount; j ++) {
              if (j % 5 == 0 && j > 0) text += "\n"; 
              text += letterAt(button, getButtonByRegularNumber(j));
            }            
          }
          break;
        default:
          text = "";  
        }
        button.textView.setText(text);
        button.candidateTextView.setText(candidateText);
        button.setLargeText(candidate);
      }
    }
  }
  
  @Override
  public void onKeyboardButtonHold(KeyboardButton button, boolean hold) {
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
      } else if (button.type == Type.SPACE) {
        service.typeLetter(" ");
        return;
      } else if (button.type == Type.BACKSPACE) {
        //if (firstButton != null) {
        //  firstButton = null;
        //} else {
        service.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
        //}
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
            //firstButton = null;        
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
    int gravity;
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
    Log.i("", "setting gravity");
    for(LinearLayout line: rowLayouts) {
      Log.i("", "gravity " + gravity + " for" + line);
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
    
    final float scale = getResources().getDisplayMetrics().density;
    float w = screenWidth, h = screenHeight;
    w /= scale; //convert to dp
    h /= scale;
    float maxWidth = w / buttonsCount.x;
    if (buttonSize > maxWidth) buttonSize = maxWidth;
    float maxHeight = (float) (0.7 * h / buttonsCount.y);
    if (buttonSize > maxHeight) buttonSize = maxHeight;
        
    int buttonSizeInPx = (int) (buttonSize * scale + 0.5f);
    
    float fontSize = (float) (buttonSize / 4.0);
    for(KeyboardButton b: buttons) {
      b.imageButton.setMaxHeight(buttonSizeInPx);
      b.imageButton.setMaxWidth(buttonSizeInPx);
      b.setFontSize(fontSize);
    }
    
  }

  @Override
  public void onKeyboardButtonMove(KeyboardButton target) {
    //Log.i("", "move!");
    if (target.type == Type.REGULAR) {
      firstButton = target;
      updateButtonsText();
    }
  }

}

