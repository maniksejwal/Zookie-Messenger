package com.zookiemessenger.zookiemessenger.poll;

import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by manik on 5/1/18.
 */

public class Poll {
    String title, details;
    Map<String, String> options = new HashMap<>();

    Poll(RadioGroup radioGroup, EditText editText1, EditText editText2) {
        title = editText1.getText().toString();
        details = editText2.getText().toString();
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            RadioButton radioButton = (RadioButton) radioGroup.getChildAt(i);
            options.put(radioButton.getText().toString(), "");
        }
    }
}
