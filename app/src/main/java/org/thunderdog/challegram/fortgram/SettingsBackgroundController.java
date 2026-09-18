/*
 * This file is a part of FortGram (Telegram-X fork).
 *
 * "Background work & calls" — shortcuts to the vendor settings that decide whether
 * the app may wake up for pushes and show the incoming-call screen while in the
 * background (MIUI / ColorOS and friends hide these behind several menus).
 */
package org.thunderdog.challegram.fortgram;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import org.thunderdog.challegram.BuildConfig;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.component.base.SettingView;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.ui.ListItem;
import org.thunderdog.challegram.ui.RecyclerViewController;
import org.thunderdog.challegram.ui.SettingsAdapter;
import org.thunderdog.challegram.v.CustomRecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SettingsBackgroundController extends RecyclerViewController<Void> implements View.OnClickListener {
  private SettingsAdapter adapter;

  public SettingsBackgroundController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.BackgroundCalls);
  }

  @Override
  public int getId () {
    return R.id.controller_backgroundCalls;
  }

  private static boolean isMiui () {
    try {
      @SuppressWarnings("PrivateApi")
      Class<?> c = Class.forName("android.os.SystemProperties");
      String v = (String) c.getMethod("get", String.class).invoke(null, "ro.miui.ui.version.name");
      return v != null && !v.isEmpty();
    } catch (Throwable t) {
      return false;
    }
  }

  private boolean isBatteryUnrestricted () {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    return pm != null && pm.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID);
  }

  private boolean canDrawOverlays () {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
  }

  @Override
  protected void onCreateView (Context context, CustomRecyclerView recyclerView) {
    adapter = new SettingsAdapter(this) {
      @Override
      public void setValuedSetting (ListItem item, SettingView v, boolean isUpdate) {
        int id = item.getId();
        if (id == R.id.btn_backgroundBattery) {
          v.setData(isBatteryUnrestricted() ? R.string.BackgroundStatusOk : R.string.BackgroundStatusAction);
        } else if (id == R.id.btn_backgroundOverlay) {
          v.setData(canDrawOverlays() ? R.string.BackgroundStatusOk : R.string.BackgroundStatusAction);
        } else if (id == R.id.btn_backgroundMiuiPermissions || id == R.id.btn_backgroundAutostart) {
          v.setData(R.string.BackgroundStatusCheck);
        }
      }
    };
    List<ListItem> items = new ArrayList<>();
    items.add(new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL));
    items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.BackgroundCallsHint));
    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_VALUED_SETTING, R.id.btn_backgroundBattery, R.drawable.baseline_settings_24, R.string.BackgroundBattery));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    items.add(new ListItem(ListItem.TYPE_VALUED_SETTING, R.id.btn_backgroundOverlay, R.drawable.baseline_layers_24, R.string.BackgroundOverlay));
    if (isMiui()) {
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
      items.add(new ListItem(ListItem.TYPE_VALUED_SETTING, R.id.btn_backgroundMiuiPermissions, R.drawable.baseline_security_24, R.string.BackgroundMiuiPermissions));
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
      items.add(new ListItem(ListItem.TYPE_VALUED_SETTING, R.id.btn_backgroundAutostart, R.drawable.baseline_replay_24, R.string.BackgroundAutostart));
    }
    items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_backgroundAppInfo, R.drawable.baseline_settings_24, R.string.BackgroundAppInfo));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
    items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, isMiui() ? R.string.BackgroundMiuiHint : R.string.BackgroundGenericHint));
    adapter.setItems(items, false);
    recyclerView.setAdapter(adapter);
  }

  @Override
  public void onFocus () {
    super.onFocus();
    if (adapter != null) adapter.updateAllValuedSettings();
  }

  private void launch (Intent... candidates) {
    for (Intent intent : candidates) {
      try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return;
      } catch (ActivityNotFoundException | SecurityException ignored) {
      }
    }
    UI.showToast(R.string.BackgroundOpenFailed, Toast.LENGTH_SHORT);
  }

  private Intent appDetails () {
    return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
  }

  @Override
  public void onClick (View v) {
    int id = v.getId();
    String pkg = BuildConfig.APPLICATION_ID;
    if (id == R.id.btn_backgroundBattery) {
      Intent request = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + pkg));
      launch(request, new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), appDetails());
    } else if (id == R.id.btn_backgroundOverlay) {
      launch(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + pkg)), appDetails());
    } else if (id == R.id.btn_backgroundMiuiPermissions) {
      Intent editor = new Intent("miui.intent.action.APP_PERM_EDITOR");
      editor.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity");
      editor.putExtra("extra_pkgname", pkg);
      Intent editorOld = new Intent("miui.intent.action.APP_PERM_EDITOR");
      editorOld.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity");
      editorOld.putExtra("extra_pkgname", pkg);
      launch(editor, editorOld, appDetails());
    } else if (id == R.id.btn_backgroundAutostart) {
      Intent autostart = new Intent();
      autostart.setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
      launch(autostart, appDetails());
    } else if (id == R.id.btn_backgroundAppInfo) {
      launch(appDetails());
    }
  }
}
