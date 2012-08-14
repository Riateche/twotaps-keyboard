package riateche;

import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class Service extends InputMethodService {
  /**
   * Called by the framework when your view for creating input needs to
   * be generated.  This will be called the first time your input method
   * is displayed, and every time it needs to be re-created such as due to
   * a configuration change.
   */
  @Override public View onCreateInputView() {   
    LinearLayout v = new LinearLayout(this);
    v.addView(new Button(this));
    v.addView(new Button(this));
    v.addView(new Button(this));    
    return v;
  }
} 
