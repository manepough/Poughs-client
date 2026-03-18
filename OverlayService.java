package com.bglauncher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private View overlayView;
    private View panelView;

    // Drag state
    private float startX, startY, initX, initY;
    private WindowManager.LayoutParams params;

    // Panel state
    private String selectedPart = "HumanoidRootPart";
    private boolean hitboxActive = false;

    private static final String CHANNEL_ID = "overlay_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification());

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        setupOverlay();
    }

    // ─── Notification (required for foreground service) ─────────────────────
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Overlay Panel", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Hitbox Expander overlay is active");
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = new Notification.Builder(this, CHANNEL_ID);
        } else {
            b = new Notification.Builder(this);
        }
        return b.setContentTitle("BG Toolkit")
                .setContentText("Hitbox panel is running")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build();
    }

    // ─── Overlay Setup ───────────────────────────────────────────────────────
    private void setupOverlay() {
        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;

        // ── Collapsed bubble (always visible) ──
        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 200;

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_bubble, null);
        windowManager.addView(overlayView, params);
        setupBubble();

        // ── Full panel (hidden initially) ──
        WindowManager.LayoutParams panelParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        );
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = 30;
        panelParams.y = 200;

        panelView = LayoutInflater.from(this).inflate(R.layout.overlay_panel, null);
        windowManager.addView(panelView, panelParams);
        panelView.setVisibility(View.GONE);
        setupPanel();
    }

    // ─── Bubble (drag + tap to open panel) ──────────────────────────────────
    private void setupBubble() {
        View bubble = overlayView.findViewById(R.id.bubbleBtn);

        bubble.setOnTouchListener(new View.OnTouchListener() {
            boolean moved = false;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        moved = false;
                        startX = e.getRawX(); startY = e.getRawY();
                        initX = params.x;    initY = params.y;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - startX;
                        float dy = e.getRawY() - startY;
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) moved = true;
                        params.x = (int)(initX + dx);
                        params.y = (int)(initY + dy);
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) togglePanel();
                        return true;
                }
                return false;
            }
        });
    }

    // ─── Panel UI ────────────────────────────────────────────────────────────
    private void setupPanel() {
        // Close button
        ImageButton closeBtn = panelView.findViewById(R.id.btnClosePanel);
        closeBtn.setOnClickListener(v -> togglePanel());

        // Stop service button
        Button stopBtn = panelView.findViewById(R.id.btnStop);
        stopBtn.setOnClickListener(v -> stopSelf());

        // Part spinner
        Spinner spinner = panelView.findViewById(R.id.spinnerPart);
        String[] parts = {"HumanoidRootPart", "Head"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, parts);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
                selectedPart = parts[pos];
                addLog("Part selected: " + selectedPart);
            }
            @Override public void onNothingSelected(AdapterView<?> a) {}
        });

        // Activate button
        Button activateBtn = panelView.findViewById(R.id.btnActivate);
        activateBtn.setOnClickListener(v -> {
            EditText factorInput = panelView.findViewById(R.id.inputFactor);
            String raw = factorInput.getText().toString().trim();
            try {
                float factor = Float.parseFloat(raw);
                if (factor <= 0) throw new NumberFormatException();
                hitboxActive = true;
                activateBtn.setText("■ Deactivate");
                addLog("ACTIVE: " + selectedPart + " x" + factor);
                Toast.makeText(this, "Hitbox x" + factor + " ON", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException ex) {
                addLog("ERROR: Invalid factor value");
            }
        });

        // Reset button
        Button resetBtn = panelView.findViewById(R.id.btnReset);
        resetBtn.setOnClickListener(v -> {
            hitboxActive = false;
            Button ab = panelView.findViewById(R.id.btnActivate);
            ab.setText("▶ Activate Hitbox");
            addLog("Hitboxes reset to original.");
            Toast.makeText(this, "Hitboxes Reset", Toast.LENGTH_SHORT).show();
        });

        addLog("Hitbox Expander BASIC ready.");
    }

    private void togglePanel() {
        boolean showing = panelView.getVisibility() == View.VISIBLE;
        panelView.setVisibility(showing ? View.GONE : View.VISIBLE);
    }

    private void addLog(String msg) {
        TextView log = panelView.findViewById(R.id.txtLog);
        String ts = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String current = log.getText().toString();
        String newLine = "[" + ts + "] " + msg;
        log.setText(newLine + (current.isEmpty() ? "" : "\n" + current));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) windowManager.removeView(overlayView);
        if (panelView   != null) windowManager.removeView(panelView);
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
