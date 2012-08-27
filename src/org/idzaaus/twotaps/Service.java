package org.idzaaus.twotaps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import org.idzaaus.twotaps.R; 
import android.content.res.Resources;
import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;

public class Service extends InputMethodService {
  private ArrayList<LetterSet> letterSets   = new ArrayList<LetterSet>();
  private ArrayList<String> allLetters      = new ArrayList<String>(); //TODO: only chosen letters 
  
  public ArrayList<String> getAllLetters() { return allLetters; }
  private KeyboardView view;
  
  private void readLetterSets() {
    Resources res = getResources();
    InputStream is = res.openRawResource(R.raw.layout);
    BufferedReader input = new BufferedReader(new InputStreamReader(is), 1024 * 8);
    String line = null;
    try {
      while (( line = input.readLine()) != null) {
        int i = line.indexOf(':');
        if (i < 0) {
          //Log.e("readLetterSets", "Missing ':' character in line");
          //return;
          continue;
        }
        LetterSet s = new LetterSet();
        s.name = line.substring(0, i);
        s.letters = new ArrayList<String>(Arrays.asList(line.substring(i + 2).split(" ")));
        letterSets.add(s);
        Log.i("readLetterSets", "Added letter set: " + s.name);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override 
  public View onCreateInputView() {   
    if (letterSets.isEmpty()) {
      readLetterSets();
      for(int i = 0; i < letterSets.size(); i++) {
        allLetters.addAll(letterSets.get(i).letters);
      }
    }

    view = new KeyboardView(this);
    return view;
  }

  
  @Override 
  public void onStartInputView (EditorInfo info, boolean restarting) {
    if (view != null) {
      int bit =info.inputType & InputType.TYPE_MASK_CLASS;
      view.setNumericMode(bit == InputType.TYPE_CLASS_NUMBER || bit == InputType.TYPE_CLASS_PHONE);
    }    
  }
  
  
  public void typeLetter(String letter) {
    if (letter.equals("↵")) letter = "\n";
    for(int i = 0; i < letter.length(); i++) {
      sendKeyChar(letter.charAt(i));
    }
  }
  
} 
