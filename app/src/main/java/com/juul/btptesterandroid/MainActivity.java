package com.juul.btptesterandroid;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    TextView statusTextView = null;
    BTTester tester;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("TAG", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.statusText);
        statusTextView.setText("");

        tester = new BTTester();
        tester.init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tester.close();
    }
}
