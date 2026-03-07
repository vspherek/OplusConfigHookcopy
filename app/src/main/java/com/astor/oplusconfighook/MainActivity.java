package com.astor.oplusconfighook;

import android.os.Bundle;
import android.os.UserManager;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * 主界面容器 Activity，承载底部导航与页面切换。
 */
public class MainActivity extends AppCompatActivity {
    private static final String FLOW_TAG = "AppListFlow";
    private static final String TAG_TOMBSTONE = "tab:tombstone";
    private static final String TAG_AUTOSTART = "tab:autostart";
    private static final String TAG_SETTINGS = "tab:settings";
    private static final String STATE_NAV_ID = "state_nav_id";
    private BottomNavigationView bottomNav;
    private BroadcastReceiver onboardingUnlockReceiver;
    private final Handler onboardingHandler = new Handler(Looper.getMainLooper());
    private int onboardingAttempt = 0;
    private boolean onboardingCheckScheduled = false;
    private int currentNavId = View.NO_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PolicyStore.migrateUserDirCeToDpIfNeeded(this);
        AppLogger.init(this);
        AppLogger.installCrashHandler();
        AppLogger.i(FLOW_TAG, "MainActivity onCreate");
        Prefs.logStartupDiagnostics(this, "MainActivity.onCreate");
        UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        boolean unlocked = (um != null && um.isUserUnlocked());
        AppLogger.i(FLOW_TAG, "[ONBOARDING_GATE] userUnlocked=" + unlocked + " action=" + (unlocked ? "immediate" : "wait_broadcast"));
        if (unlocked) {
            scheduleOnboardingCheck("unlocked_immediate");
        } else if (onboardingUnlockReceiver == null) {
            onboardingUnlockReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        if (onboardingUnlockReceiver != null) {
                            unregisterReceiver(onboardingUnlockReceiver);
                            onboardingUnlockReceiver = null;
                        }
                    } catch (Throwable ignored) {
                    }
                    try {
                        AppLogger.i(FLOW_TAG, "[ONBOARDING_GATE] ACTION_USER_UNLOCKED received, run onboarding check");
                        scheduleOnboardingCheck("user_unlocked_broadcast");
                    } catch (Throwable t) {
                        AppLogger.i(FLOW_TAG, "[ONBOARDING_GATE] onboarding check failed err=" + t.getClass().getSimpleName() + ":" + t.getMessage());
                    }
                }
            };
            registerReceiver(onboardingUnlockReceiver, new IntentFilter(Intent.ACTION_USER_UNLOCKED));
        }
        Prefs.ensureDefaults(this, "MainActivity.onCreate");
        logConfigState();
        PolicyStore.refresh(this);
        AppListRepository.ensurePreloaded(getApplicationContext());

        bottomNav = findViewById(R.id.bottomNav);
        disableBottomNavActiveIndicator(bottomNav);
        View fragmentContainer = findViewById(R.id.fragmentContainer);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            int baseBottom = getResources().getDimensionPixelSize(R.dimen.space_3);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), navBars.bottom + baseBottom);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id != R.id.nav_tombstone && id != R.id.nav_autostart && id != R.id.nav_settings) {
                return false;
            }
            switchTo(id, false);
            return true;
        });

        bottomNav.setOnItemReselectedListener(item -> {
        });

        if (savedInstanceState == null) {
            switchTo(R.id.nav_tombstone, true);
            forceBottomNavChecked(R.id.nav_tombstone);
        } else {
            int restored = savedInstanceState.getInt(STATE_NAV_ID, R.id.nav_tombstone);
            switchTo(restored, true);
            forceBottomNavChecked(restored);
        }
    }


    private void scheduleOnboardingCheck(String reason) {
        if (onboardingCheckScheduled) return;
        onboardingAttempt = 0;
        onboardingCheckScheduled = true;
        runOnboardingAttempt(reason);
    }

    private void runOnboardingAttempt(String reason) {
        onboardingAttempt++;
        AppLogger.i(FLOW_TAG, "[ONBOARDING_RETRY] attempt=" + onboardingAttempt + " reason=" + reason);
        boolean shown = Prefs.onboardingShownStable(this);
        if (shown) {
            AppLogger.i(FLOW_TAG, "[ONBOARDING_RETRY] attempt=" + onboardingAttempt + " result=skip shown=true reason=" + reason);
            onboardingCheckScheduled = false;
            return;
        }
        if (onboardingAttempt < 3) {
            long delayMs = onboardingAttempt == 1 ? 300L : 800L;
            onboardingHandler.postDelayed(() -> runOnboardingAttempt(reason), delayMs);
            return;
        }
        AppLogger.i(FLOW_TAG, "[ONBOARDING_RETRY] attempt=" + onboardingAttempt + " result=final_show_check reason=" + reason);
        showFirstLaunchFreeNoticeIfNeeded();
        onboardingCheckScheduled = false;
    }

    private void showFirstLaunchFreeNoticeIfNeeded() {
        boolean shown = Prefs.onboardingShownStable(this);
        Prefs.logOnboardingRead(this, "MainActivity.showFirstLaunch", shown ? "skip" : "show", "prefs_only");
        AppLogger.i(FLOW_TAG, "[ONBOARDING_DECIDE] shownStable=" + shown
                + " decision=" + (shown ? "skip" : "show"));
        if (shown) return;

        TextView content = new TextView(this);
        content.setText(R.string.free_notice_message);
        content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        content.setTextColor(Color.parseColor("#D32F2F"));
        int horizontal = getResources().getDimensionPixelSize(R.dimen.space_3);
        int vertical = getResources().getDimensionPixelSize(R.dimen.space_3);
        content.setPadding(horizontal, vertical, horizontal, vertical);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.free_notice_title)
                .setView(content, horizontal, vertical, horizontal, vertical)
                .setCancelable(false)
                .setPositiveButton(R.string.confirm_stage, (d, w) ->
                        Prefs.markOnboardingShownStable(this, "MainActivity.free_notice_dialog"))
                .show();
    }


    private void logConfigState() {
        AppLogger.i(FLOW_TAG, "[CONFIG_STATE] master=" + Prefs.enabledMaster(this)
                + " autostart=" + Prefs.enabledAutostart(this)
                + " tombstone=" + Prefs.enabledTombstone(this)
                + " allowAndroidFsHook=" + Prefs.allowAndroidFsHook(this)
                + " init=" + Prefs.configInitialized(this)
                + " onboardingShownStable=" + Prefs.onboardingShownStable(this)
                + " hasUserFact=" + Prefs.hasUserConfigurationFact(this)
                + " masterSource=" + Prefs.enabledMasterSource(this)
                + " onboardingStableSource=" + Prefs.onboardingShownStableSource(this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        AppLogger.i(FLOW_TAG, "MainActivity onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppLogger.i(FLOW_TAG, "MainActivity onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppLogger.i(FLOW_TAG, "MainActivity onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        AppLogger.i(FLOW_TAG, "MainActivity onStop");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        int navId = (currentNavId != View.NO_ID) ? currentNavId : bottomNav.getSelectedItemId();
        if (navId == View.NO_ID) navId = R.id.nav_tombstone;
        outState.putInt(STATE_NAV_ID, navId);
    }

    @Override
    protected void onDestroy() {
        onboardingHandler.removeCallbacksAndMessages(null);
        onboardingCheckScheduled = false;
        if (onboardingUnlockReceiver != null) {
            try {
                unregisterReceiver(onboardingUnlockReceiver);
            } catch (Throwable ignored) {
            }
            onboardingUnlockReceiver = null;
        }
        super.onDestroy();
        AppLogger.i(FLOW_TAG, "MainActivity onDestroy");
    }

    private void switchTo(int navId, boolean immediate) {
        if (currentNavId == navId) return;

        String targetTag = tagForNav(navId);
        Fragment target = getSupportFragmentManager().findFragmentByTag(targetTag);
        if (target == null) {
            target = createFragment(navId);
        }

        androidx.fragment.app.FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment != null && fragment.isAdded() && !fragment.isHidden() && fragment != target) {
                tx.hide(fragment);
            }
        }

        if (target.isAdded()) {
            tx.show(target);
        } else {
            tx.add(R.id.fragmentContainer, target, targetTag);
        }

        currentNavId = navId;
        if (immediate) {
            tx.commitNow();
        } else {
            tx.commit();
        }
        forceBottomNavChecked(navId);
    }

    private void forceBottomNavChecked(int navId) {
        if (bottomNav == null) return;
        try {
            if (bottomNav.getMenu().findItem(navId) != null) {
                bottomNav.getMenu().findItem(navId).setChecked(true);
            }
        } catch (Throwable ignored) {
        }
    }

    private String tagForNav(int navId) {
        if (navId == R.id.nav_tombstone) return TAG_TOMBSTONE;
        if (navId == R.id.nav_autostart) return TAG_AUTOSTART;
        return TAG_SETTINGS;
    }

    private Fragment createFragment(int navId) {
        if (navId == R.id.nav_tombstone) return new TombstoneFragment();
        if (navId == R.id.nav_autostart) return new AutostartFragment();
        return new SettingsFragment();
    }

    private void disableBottomNavActiveIndicator(BottomNavigationView nav) {
        try {
            nav.getClass().getMethod("setItemActiveIndicatorEnabled", boolean.class).invoke(nav, false);
            nav.getClass().getMethod("setItemActiveIndicatorColor", ColorStateList.class)
                    .invoke(nav, ColorStateList.valueOf(Color.TRANSPARENT));
        } catch (Exception ignored) {
        }
    }
}
