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
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;

public class Service extends InputMethodService {
  private ArrayList<LetterSet> letterSets   = new ArrayList<LetterSet>();
//  private ArrayList<String> allLetters      = new ArrayList<String>(); //TODO: only chosen letters 
  
  private KeyboardView view;
  
  private void readLetterSets() {
    Resources res = getResources();
    InputStream is = res.openRawResource(R.raw.layout);
    BufferedReader input = new BufferedReader(new InputStreamReader(is), 1024 * 8);
    String line = null;
    LetterSet letterSet = null;
    try {
      while (( line = input.readLine()) != null) {
        line = line.trim();
        if (line.length() == 0) continue; 
        if (line.startsWith("* ")) {
          if (letterSet != null) {
            letterSets.add(letterSet);
          }
          letterSet = new LetterSet();
          letterSet.name = line.substring(2);
          letterSet.letters = new ArrayList<String>();
        } else {
          if (letterSet != null) {
            letterSet.letters.addAll(Arrays.asList(line.split("\\s+")));
          }
        }
        if (letterSet != null) {
          letterSets.add(letterSet);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override 
  public View onCreateInputView() {   
    PreferenceManager.setDefaultValues(this, R.xml.keyboard_preferences, false);
    
    if (letterSets.size() == 0) {
      readLetterSets();
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
  
  public ArrayList<String> getLayout(String code) {
    ArrayList<String> r = new ArrayList<String>();
    r.addAll(getLetterSet("latin").letters);
    if (!code.equals("en")) {
      r.addAll(getLetterSet(code).letters);
    }
    return r;
  }
  
  private LetterSet getLetterSet(String name) {
    for(LetterSet s: letterSets) {
      if (s.name.equals(name)) return s;
    }
    return null;
  }
  
} 
