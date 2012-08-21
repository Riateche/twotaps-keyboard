package riateche;

import java.util.ArrayList;

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
  private KeyboardButton firstButton = null; // pressed button (or null if none was pressed)
  

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
        

    
    int singlePressButtonsCount = regularButtonsCount - 
        (int) Math.ceil(service.getAllLetters().size() / (double) regularButtonsCount);
    Log.i("", "rbc" + regularButtonsCount + " count=" + service.getAllLetters().size() + " r=" + singlePressButtonsCount);
    //TODO: most used letters here
    String[] best_keys = { " ", ".", ",", "a", "e", "o" }; 
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
    for(KeyboardButton button: buttons) {
      boolean candidate = false;
      String text;
      //KeyboardButton button = buttons.get(i); 
      switch(button.type) {
      case BACKSPACE: 
        text = "\nbksp\n";
        break;
      case SYSTEM_MENU: 
        text = "\nsys\n";
        break;
      case SHIFT: 
        text = "\nshift\n";
        break;
      case REGULAR:
        if (firstButton == null) {
          if (button.singlePressLetter != null) {
            if (button.singlePressLetter.equals(" ")) {
              text = "\nspace\n";
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
      if (candidate) {
        button.setTextAppearance(service, R.style.Button_candidate);
      } else {
        button.setTextAppearance(service, R.style.Button_regular);
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
    if (button.type == Type.SHIFT) {
      if (!hold) shiftPressed = !shiftPressed;
    } else if (button.type == Type.BACKSPACE) {
      if (firstButton == null) {
        service.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
      } else {
        firstButton = null;
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
    updateButtonsText();           
  }
  
  private String letterAt(KeyboardButton firstButton, KeyboardButton secondButton) {
    try {
      String s = service.getAllLetters().get(firstButton.regularNumber * regularButtonsCount + secondButton.regularNumber);
      if (shiftPressed) s = s.toUpperCase();      
      return s;
    } catch (IndexOutOfBoundsException e) {
      return "";
    }
  }
  

}

