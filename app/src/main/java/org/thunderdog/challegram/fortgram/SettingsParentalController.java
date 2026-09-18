/*
 * This file is a part of FortGram (Telegram-X fork).
 *
 * Parental controls settings screen. Gated by ParentalPin —
 * caller must verify PIN before navigating here.
 */
package org.thunderdog.challegram.fortgram;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.thunderdog.challegram.R;
import org.thunderdog.challegram.component.base.SettingView;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.tool.Intents;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.ui.camera.CameraController;
import org.thunderdog.challegram.ui.ListItem;
import org.thunderdog.challegram.ui.RecyclerViewController;
import org.thunderdog.challegram.ui.SettingsAdapter;
import org.thunderdog.challegram.v.CustomRecyclerView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class SettingsParentalController extends RecyclerViewController<Void> implements View.OnClickListener {
  private static final int REQUEST_EXPORT_FILE = 9101;
  private static final int REQUEST_IMPORT_FILE = 9102;

  private SettingsAdapter adapter;
  private @Nullable String pendingExportJson;

  public SettingsParentalController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.ParentalControls);
  }

  @Override
  public int getId () {
    return R.id.controller_parentalControls;
  }

  @Override
  protected void onCreateView (Context context, CustomRecyclerView recyclerView) {
    adapter = new SettingsAdapter(this) {
      @Override
      public void setValuedSetting (ListItem item, SettingView v, boolean isUpdate) {
        final int itemId = item.getId();
        if (itemId == R.id.btn_parentalWhitelistToggle) {
          v.getToggler().setRadioEnabled(ParentalPolicy.instance().isEnabled(), isUpdate);
        } else if (itemId == R.id.btn_parentalAllowAddContact) {
          v.getToggler().setRadioEnabled(ParentalPolicy.instance().isAddContactAllowed(), isUpdate);
        } else if (itemId == R.id.btn_parentalWhitelist) {
          int n = ParentalPolicy.instance().getManualUsers().length;
          v.setData(n > 0 ? Lang.plural(R.string.xUsers, n) : Lang.getString(R.string.ParentalWhitelistEmpty));
        } else if (itemId == R.id.btn_parentalGroups) {
          int n = ParentalPolicy.instance().getAllowedGroups().length;
          v.setData(n > 0 ? Lang.getString(R.string.ParentalGroupsCount, n) : Lang.getString(R.string.ParentalGroupsNone));
        } else if (itemId == R.id.btn_parentalStickersRestrict) {
          v.getToggler().setRadioEnabled(ParentalPolicy.instance().isStickerRestrictionEnabled(), isUpdate);
        } else if (itemId == R.id.btn_parentalStickerWhitelist) {
          int n = ParentalPolicy.instance().getStickerWhitelist().length;
          v.setData(n > 0 ? Lang.getString(R.string.ParentalStickerWhitelistCount, n) : Lang.getString(R.string.ParentalStickerWhitelistEmpty));
        }
      }
    };

    List<ListItem> items = new ArrayList<>();
    items.add(new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL));

    items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.ParentalWhitelistMode));
    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_parentalWhitelistToggle, 0, R.string.ParentalWhitelistMode));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_parentalAllowAddContact, 0, R.string.ParentalAllowAddContact));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    items.add(new ListItem(ListItem.TYPE_VALUED_SETTING, R.id.btn_parentalWhitelist, R.drawable.baseline_person_add_24, R.string.ParentalWhitelistContacts));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    items.add(new ListItem(ListItem.TYPE_VALUED_SETTING, R.id.btn_parentalGroups, R.drawable.baseline_group_24, R.string.ParentalGroups));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
    items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.ParentalWhitelistModeHint));
    items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.ParentalAllowAddContactHint));

    items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.ParentalStickers));
    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_parentalStickersRestrict, 0, R.string.ParentalStickersRestrict));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    items.add(new ListItem(ListItem.TYPE_VALUED_SETTING, R.id.btn_parentalStickerWhitelist, R.drawable.deproko_baseline_stickers_24, R.string.ParentalStickerWhitelist));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
    items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.ParentalStickersRestrictHint));

    items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.ParentalBackup));
    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_parentalExport, R.drawable.baseline_share_24, R.string.ParentalExport));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_parentalImport, R.drawable.baseline_content_paste_24, R.string.ParentalImport));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
    items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.ParentalExportHint));

    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_parentalPinChange, R.drawable.baseline_lock_24, R.string.ParentalPinChange));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_parentalPinDisable, R.drawable.baseline_block_24, R.string.ParentalPinDisable).setTextColorId(ColorId.textNegative));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

    adapter.setItems(items, false);
    recyclerView.setAdapter(adapter);
  }

  @Override
  public void onFocus () {
    super.onFocus();
    // Counters may have changed on the child screens (whitelist / groups / sticker packs).
    if (adapter != null) {
      adapter.updateValuedSettingById(R.id.btn_parentalWhitelist);
      adapter.updateValuedSettingById(R.id.btn_parentalGroups);
      adapter.updateValuedSettingById(R.id.btn_parentalStickerWhitelist);
    }
  }

  private void exportConfig (int targetId) {
    ParentalConfig.export(tdlib, json -> runOnUiThreadOptional(() -> {
      if (json == null) {
        UI.showToast(R.string.ParentalImportInvalid, Toast.LENGTH_SHORT);
      } else if (targetId == R.id.btn_parentalExportQr) {
        showQr(json);
      } else if (targetId == R.id.btn_parentalExportFile) {
        openExportFilePicker(json);
      } else if (targetId == R.id.btn_parentalExportShare) {
        Intents.shareText(json);
      } else {
        UI.copyText(json, R.string.ParentalExportCopied);
      }
    }));
  }

  private void showQr (String json) {
    String compact = ParentalConfig.toCompact(json);
    if (compact == null) {
      UI.showToast(R.string.ParentalExportQrTooLarge, Toast.LENGTH_LONG);
      return;
    }
    Bitmap bitmap;
    try {
      int size = Math.min(Screen.currentWidth(), Screen.currentHeight()) - Screen.dp(48f);
      java.util.Map<com.google.zxing.EncodeHintType, Object> hints = new java.util.HashMap<>();
      hints.put(com.google.zxing.EncodeHintType.MARGIN, 1);
      hints.put(com.google.zxing.EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L);
      com.google.zxing.common.BitMatrix matrix = new com.google.zxing.qrcode.QRCodeWriter().encode(compact, com.google.zxing.BarcodeFormat.QR_CODE, size, size, hints);
      bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
      int[] row = new int[size];
      for (int y = 0; y < size; y++) {
        for (int x = 0; x < size; x++) {
          row[x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
        }
        bitmap.setPixels(row, 0, size, 0, y, size, 1);
      }
    } catch (Throwable t) {
      UI.showToast(R.string.ParentalExportQrTooLarge, Toast.LENGTH_LONG);
      return;
    }
    ImageView view = new ImageView(context);
    view.setImageBitmap(bitmap);
    view.setBackgroundColor(Color.WHITE);
    int pad = Screen.dp(16f);
    view.setPadding(pad, pad, pad, pad);
    view.setAdjustViewBounds(true);
    android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(context);
    b.setTitle(Lang.getString(R.string.ParentalExportQr));
    b.setView(view);
    b.setPositiveButton(Lang.getString(R.string.Done), (dialog, which) -> dialog.dismiss());
    showAlert(b);
  }

  // File export / import via the system document picker (no storage permission needed).

  private void openExportFilePicker (String json) {
    pendingExportJson = json;
    String name = "fortgram-parental-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date()) + ".json";
    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("application/json");
    intent.putExtra(Intent.EXTRA_TITLE, name);
    context.putActivityResultHandler(REQUEST_EXPORT_FILE, (requestCode, resultCode, data) -> {
      Uri uri = resultCode == android.app.Activity.RESULT_OK && data != null ? data.getData() : null;
      String text = pendingExportJson;
      pendingExportJson = null;
      if (uri == null || text == null) return;
      try (OutputStream out = context.getContentResolver().openOutputStream(uri, "wt")) {
        if (out == null) throw new java.io.IOException("null stream");
        out.write(text.getBytes(StandardCharsets.UTF_8));
        UI.showToast(R.string.ParentalExportSaved, Toast.LENGTH_SHORT);
      } catch (Throwable t) {
        UI.showToast(R.string.ParentalFileError, Toast.LENGTH_SHORT);
      }
    });
    try {
      UI.startActivityForResult(intent, REQUEST_EXPORT_FILE);
    } catch (Throwable t) {
      pendingExportJson = null;
      UI.showToast(R.string.ParentalFileError, Toast.LENGTH_SHORT);
    }
  }

  private void openImportFilePicker () {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");
    intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"application/json", "text/plain", "application/octet-stream"});
    context.putActivityResultHandler(REQUEST_IMPORT_FILE, (requestCode, resultCode, data) -> {
      Uri uri = resultCode == android.app.Activity.RESULT_OK && data != null ? data.getData() : null;
      if (uri == null) return;
      StringBuilder sb = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getContentResolver().openInputStream(uri), StandardCharsets.UTF_8))) {
        char[] buf = new char[8192];
        int n;
        while ((n = reader.read(buf)) > 0 && sb.length() < 4_000_000) sb.append(buf, 0, n);
      } catch (Throwable t) {
        UI.showToast(R.string.ParentalFileError, Toast.LENGTH_SHORT);
        return;
      }
      importConfig(sb.toString());
    });
    try {
      UI.startActivityForResult(intent, REQUEST_IMPORT_FILE);
    } catch (Throwable t) {
      UI.showToast(R.string.ParentalFileError, Toast.LENGTH_SHORT);
    }
  }

  private @Nullable String readClipboard () {
    ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    ClipData clip = cm != null ? cm.getPrimaryClip() : null;
    if (clip == null || clip.getItemCount() == 0) return null;
    CharSequence text = clip.getItemAt(0).coerceToText(context);
    return text != null ? text.toString() : null;
  }

  private void importConfig (@Nullable String text) {
    org.json.JSONObject root = ParentalConfig.parse(ParentalConfig.fromCompact(text));
    if (root == null) {
      UI.showToast(R.string.ParentalImportInvalid, Toast.LENGTH_SHORT);
      return;
    }
    // Show what the file contains and let the parent untick entries before applying.
    SettingsImportPreviewController c = new SettingsImportPreviewController(context, tdlib);
    c.setArguments(root);
    navigateTo(c);
  }

  @Override
  public void onClick (View v) {
    final int viewId = v.getId();
    if (viewId == R.id.btn_parentalWhitelistToggle) {
      boolean next = !ParentalPolicy.instance().isEnabled();
      ParentalPolicy.instance().setEnabled(next);
      adapter.updateValuedSettingById(R.id.btn_parentalWhitelistToggle);
      showConfirm(Lang.getString(R.string.ParentalRestartRequired),
        Lang.getString(R.string.ParentalRestartNow),
        R.drawable.baseline_replay_24,
        OptionColor.NORMAL,
        () -> {
          android.os.Process.killProcess(android.os.Process.myPid());
          System.exit(0);
        });
    } else if (viewId == R.id.btn_parentalAllowAddContact) {
      ParentalPolicy.instance().setAddContactAllowed(!ParentalPolicy.instance().isAddContactAllowed());
      adapter.updateValuedSettingById(R.id.btn_parentalAllowAddContact);
    } else if (viewId == R.id.btn_parentalWhitelist) {
      SettingsWhitelistController c = new SettingsWhitelistController(context, tdlib);
      navigateTo(c);
    } else if (viewId == R.id.btn_parentalGroups) {
      navigateTo(new SettingsGroupWhitelistController(context, tdlib));
    } else if (viewId == R.id.btn_parentalStickersRestrict) {
      ParentalPolicy.instance().setStickerRestrictionEnabled(!ParentalPolicy.instance().isStickerRestrictionEnabled());
      adapter.updateValuedSettingById(R.id.btn_parentalStickersRestrict);
    } else if (viewId == R.id.btn_parentalStickerWhitelist) {
      SettingsStickerWhitelistController c = new SettingsStickerWhitelistController(context, tdlib);
      navigateTo(c);
    } else if (viewId == R.id.btn_parentalExport) {
      showOptions(null,
        new int[] {R.id.btn_parentalExportQr, R.id.btn_parentalExportFile, R.id.btn_parentalExportCopy, R.id.btn_parentalExportShare},
        new String[] {Lang.getString(R.string.ParentalExportQr), Lang.getString(R.string.ParentalExportFile), Lang.getString(R.string.ParentalExportCopy), Lang.getString(R.string.ParentalExportShare)},
        (itemView, id) -> {
          exportConfig(id);
          return true;
        });
    } else if (viewId == R.id.btn_parentalImport) {
      showOptions(null,
        new int[] {R.id.btn_parentalImportQr, R.id.btn_parentalImportFile, R.id.btn_parentalImportClipboard, R.id.btn_parentalImportPaste},
        new String[] {Lang.getString(R.string.ParentalImportQr), Lang.getString(R.string.ParentalImportFile), Lang.getString(R.string.ParentalImportClipboard), Lang.getString(R.string.ParentalImportPaste)},
        (itemView, id) -> {
          if (id == R.id.btn_parentalImportQr) {
            openInAppCamera(new CameraOpenOptions().anchor(itemView).noTrace(true).allowSystem(false).optionalMicrophone(true).mode(CameraController.MODE_QR).qrModeSubtitle(R.string.ParentalImportQrHint).qrCodeListener(qrCode -> {
              // Let the camera popup close first: opening our confirmation synchronously made the
              // camera's own back-press dismiss the confirmation instead and left the scanner running.
              UI.post(() -> runOnUiThreadOptional(() -> importConfig(qrCode)), 600L);
            }));
          } else if (id == R.id.btn_parentalImportFile) {
            openImportFilePicker();
          } else if (id == R.id.btn_parentalImportClipboard) {
            importConfig(readClipboard());
          } else {
            openInputAlert(Lang.getString(R.string.ParentalImport), "{…}", R.string.Done, R.string.Cancel, null, (input, text) -> {
              importConfig(text);
              return true;
            }, true);
          }
          return true;
        });
    } else if (viewId == R.id.btn_parentalPinChange) {
      ParentalPinGate.promptChangePin(this);
    } else if (viewId == R.id.btn_parentalPinDisable) {
      showConfirm(Lang.getString(R.string.ParentalPinDisableConfirm),
        Lang.getString(R.string.ParentalPinDisable),
        R.drawable.baseline_block_24,
        OptionColor.RED,
        () -> {
          ParentalPin.instance().clear();
          navigateBack();
        });
    }
  }
}
