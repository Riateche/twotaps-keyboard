package riateche;

import java.util.ArrayList;

import riateche.KeyboardButton.System_command;
import riateche.KeyboardButton.Type;
import riateche.twotaps.R;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

public class KeyboardView extends LinearLayout  implements OnClickListener, KeyboardButton.OnHoldListener {
  private Service service;
  private Point buttonsCount                = new Point(5, 3);
  private ArrayList<KeyboardButton> buttons = new ArrayList<KeyboardButton>();
  
  private final int regularButtonsCount = 12;
  private boolean shiftPressed = false;
  private boolean capsEnabled = false;
  private boolean inSystemMenu = false;
  private KeyboardButton firstButton = null; // pressed button (or null if none was pressed)
  private boolean numericMode = false;
  
  public void setNumericMode(boolean enabled) {
    numericMode = enabled;
    updateButtonsText();
  }
  

  public KeyboardView(Service service) {
    super(service);
    this.service = service;
    //service.getLayoutInflater().inflate(resource, root)
    inflate(service, R.layout.keyboard, this);
    
    Resources r = service.getResources();
    for(int y = 0; y < buttonsCount.y; y++) {
      for(int x = 0; x < buttonsCount.x; x++) {
        int res_id = r.getIdentifier("button" + (y+1) + "_" + (x+1), "id", "riateche.twotaps");
        Log.i("", "res_id " + res_id);
        KeyboardButton b = (KeyboardButton) findViewById(res_id);
        Log.i("", "b " + b);
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
        
        
        b.setOnClickListener(this);
        b.addOnHoldListener(this);
        buttons.add(b);
      }
    }
    buttons.get(0).system_command = System_command.CAPS_LOCK;    
    buttons.get(1).system_command = System_command.NUMERIC;    
    buttons.get(0).numericModeLetter = "1";
    buttons.get(1).numericModeLetter = "2";
    buttons.get(2).numericModeLetter = "3";
    buttons.get(3).numericModeLetter = "*";
    buttons.get(5).numericModeLetter = "4";
    buttons.get(6).numericModeLetter = "5";
    buttons.get(7).numericModeLetter = "6";
    buttons.get(8).numericModeLetter = "#";
    buttons.get(9).numericModeLetter = "+";
    buttons.get(10).numericModeLetter = "7";
    buttons.get(11).numericModeLetter = "8";
    buttons.get(12).numericModeLetter = "9";
    buttons.get(13).numericModeLetter = "0";
    
    int singlePressButtonsCount = regularButtonsCount - 
        (int) Math.ceil(service.getAllLetters().size() / (double) regularButtonsCount);
    Log.i("", "rbc" + regularButtonsCount + " count=" + service.getAllLetters().size() + " r=" + singlePressButtonsCount);
    //TODO: most used letters here
    String[] best_keys = { "\n", " ", "," }; 
    for(int i = regularButtonsCount - singlePressButtonsCount; i < regularButtonsCount; i++) {        
      int best_key_id = i - regularButtonsCount + singlePressButtonsCount;
      if (best_key_id < best_keys.length) {
        getButtonByRegularNumber(i).singlePressLetter = best_keys[best_key_id];
      }
    }
    
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
      }
      for(KeyboardButton button: buttons) {
        if (button.type == Type.BACKSPACE) {
          button.setText("back");
        } else {
          if (button.system_command == null) {
            button.setText("");
          } else {
            switch(button.system_command) {
            case CAPS_LOCK: 
              button.setText("caps");
              break;
            case NUMERIC:
              button.setText("123");              
            }
          }
        }
      }      
    } else if (numericMode) {
      for(KeyboardButton button: buttons) {
        if (button.type == Type.BACKSPACE) {
          button.setLargeText(false);
          button.setText("bksp");
        } else if (button.type == Type.SYSTEM_MENU) {
          button.setLargeText(false);
          button.setText("ABC");          
        } else if (button.numericModeLetter != null) {
          button.setLargeText(true);
          button.setText(button.numericModeLetter);
          
        }
      }
    
    } else {    
      for(KeyboardButton button: buttons) {
        boolean candidate = false;
        String text;
        //KeyboardButton button = buttons.get(i); 
        switch(button.type) {
        case BACKSPACE:
          if (firstButton == null) {
            text = "bksp";
          } else {
            text = "back";
          }
          break;
        case SYSTEM_MENU: 
          text = "sys";
          break;
        case SHIFT: 
          text = "shift";
          break;
        case REGULAR:
          if (firstButton == null) {
            if (button.singlePressLetter != null) {
              if (button.singlePressLetter.equals(" ")) {
                text = "space";
              } else if (button.singlePressLetter.equals("\n")) {
                text = "enter";
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
        button.setText(text);
        button.setLargeText(candidate);
      }
    }
  }
  
  @Override
  public void onClick(View v) {
    onClickOrHold( (KeyboardButton) v, false);
  }

  @Override
  public void onKeyboardButtonHold(KeyboardButton target) {
    onClickOrHold(target, true);
  }

  
  private void onClickOrHold(KeyboardButton button, boolean hold) {
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
      String s = service.getAllLetters().get(firstButton.regularNumber * regularButtonsCount + secondButton.regularNumber);
      if (capsEnabled ^ shiftPressed) s = s.toUpperCase();      
      return s;
    } catch (IndexOutOfBoundsException e) {
      return "";
    }
  }
  

}

