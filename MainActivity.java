package com.bglauncher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int OVERLAY_PERMISSION_REQ = 1001;
    private static final String BLOCKMAN_PKG = "com.sandboxol.blockymods";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button launchBtn = findViewById(R.id.btnLaunchBlockman);
        launchBtn.setOnClickListener(v -> onLaunchClicked());
    }

    private void onLaunchClicked() {
        // 1. Check overlay permission first
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this,
                "Grant 'Display over other apps' permission to show the Hitbox Panel",
                Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ);
            return;
        }
        launchBlockmanWithOverlay();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ) {
            if (Settings.canDrawOverlays(this)) {
                launchBlockmanWithOverlay();
            } else {
                Toast.makeText(this,
                    "Permission denied – overlay panel won't show",
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchBlockmanWithOverlay() {
        // 2. Start the floating overlay service FIRST
        Intent serviceIntent = new Intent(this, OverlayService.class);
        startService(serviceIntent);

        // 3. Launch Blockman Go
        PackageManager pm = getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(BLOCKMAN_PKG);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
        } else {
            // Not installed → open Play Store
            Toast.makeText(this, "Blockman Go not installed. Opening Play Store...", Toast.LENGTH_SHORT).show();
            Intent store = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + BLOCKMAN_PKG));
            store.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(store);
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + BLOCKMAN_PKG)));
            }
        }
        // Minimize this activity so Blockman is in front
        finish();
    }
}
