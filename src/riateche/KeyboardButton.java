package riateche;

import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

public class KeyboardButton extends Button {
  enum Type {
    BACKSPACE,
    SHIFT,
    SYSTEM_MENU,
    REGULAR
  }

  public int number; //! General number of the button
  public int x, y;   //! Axis number of the button
  public Type type;   
  public int regularNumber; //! Only for type == REGULAR; number of the button over all regular buttons  
  public String singlePressLetter = null; 
  
  
  private final int repeatSpeed = 50;
  private final int repeatDelay = 500;


  public KeyboardButton(Context context) {
    super(context);

    setOnTouchListener(new View.OnTouchListener() {
      private Handler handler;

      @Override public boolean onTouch(View v, MotionEvent event) {
        switch(event.getAction()) {
        case MotionEvent.ACTION_DOWN:
          if (handler != null) return true;
          handler = new Handler();
          handler.postDelayed(action, repeatDelay);
          break;
        case MotionEvent.ACTION_UP:
          if (handler == null) return true;
          handler.removeCallbacks(action);
          handler = null;
          break;
        }
        return false;
      }

      Runnable action = new Runnable() {
        @Override public void run() {
          performClick();
          handler.postDelayed(this, repeatSpeed);
        }
      };

    });


  }

}
