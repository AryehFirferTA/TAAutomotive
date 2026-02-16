package com.example.taautomotive;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * Main activity for the automotive app.
 * Shows a simple UI indicating the media service is running.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Start the media service
        Intent serviceIntent = new Intent(this, com.example.taautomotive.shared.TAMediaLibraryService.class);
        startForegroundService(serviceIntent);
        
        // Create a simple text view to show the app is running
        TextView textView = new TextView(this);
        textView.setText("TAAutomotive Media Service\n\nMedia library service is running.\nUse the car's media system to browse and play music.");
        textView.setTextSize(18);
        textView.setPadding(50, 50, 50, 50);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        
        setContentView(textView);
    }
}
