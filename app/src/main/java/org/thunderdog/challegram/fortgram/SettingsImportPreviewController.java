/*
 * This file is a part of FortGram (Telegram-X fork).
 *
 * Import preview — shows what a parental settings export contains and lets the
 * parent untick entries or flip the flags before anything is applied.
 */
package org.thunderdog.challegram.fortgram;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.component.base.SettingView;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.data.TD;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.ui.ListItem;
import org.thunderdog.challegram.ui.RecyclerViewController;
import org.thunderdog.challegram.ui.SettingsAdapter;
import org.thunderdog.challegram.v.CustomRecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SettingsImportPreviewController extends RecyclerViewController<JSONObject> implements View.OnClickListener {
  private SettingsAdapter adapter;
  private boolean enabled, allowAddContact, officialOnly;
  private final List<ListItem> userItems = new ArrayList<>();
  private final List<ListItem> groupItems = new ArrayList<>();
  private final List<ListItem> setItems = new ArrayList<>();

  public SettingsImportPreviewController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.ParentalImport);
  }

  @Override
  public int getId () {
    return R.id.controller_parentalImportPreview;
  }

  @Override
  protected void onCreateView (Context context, CustomRecyclerView recyclerView) {
    JSONObject root = getArgumentsStrict();
    JSONObject whitelist = root.optJSONObject("whitelist");
    JSONObject stickers = root.optJSONObject("stickers");
    ParentalPolicy policy = ParentalPolicy.instance();
    enabled = whitelist != null ? whitelist.optBoolean("enabled", policy.isEnabled()) : policy.isEnabled();
    allowAddContact = whitelist != null ? whitelist.optBoolean("allow_add_contact", policy.isAddContactAllowed()) : policy.isAddContactAllowed();
    officialOnly = stickers != null ? stickers.optBoolean("official_only", policy.isStickerRestrictionEnabled()) : policy.isStickerRestrictionEnabled();

    fillEntries(whitelist != null ? whitelist.optJSONArray("users") : null, userItems, R.id.btn_parentalImportUser);
    fillEntries(whitelist != null ? whitelist.optJSONArray("groups") : null, groupItems, R.id.btn_parentalImportGroup);
    fillEntries(stickers != null ? stickers.optJSONArray("sets") : null, setItems, R.id.btn_parentalImportSet);

    adapter = new SettingsAdapter(this) {
      @Override
      public void setValuedSetting (ListItem item, SettingView v, boolean isUpdate) {
        final int id = item.getId();
        if (id == R.id.btn_parentalWhitelistToggle) {
          v.getToggler().setRadioEnabled(enabled, isUpdate);
        } else if (id == R.id.btn_parentalAllowAddContact) {
          v.getToggler().setRadioEnabled(allowAddContact, isUpdate);
        } else if (id == R.id.btn_parentalStickersRestrict) {
          v.getToggler().setRadioEnabled(officialOnly, isUpdate);
        }
      }
    };

    List<ListItem> items = new ArrayList<>();
    items.add(new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL));
    items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.ParentalImportPreviewHint));

    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_parentalWhitelistToggle, 0, R.string.ParentalWhitelistMode));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_parentalAllowAddContact, 0, R.string.ParentalAllowAddContact));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_parentalStickersRestrict, 0, R.string.ParentalStickersRestrict));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

    addSection(items, R.string.ParentalWhitelistContacts, userItems);
    addSection(items, R.string.ParentalGroups, groupItems);
    addSection(items, R.string.ParentalStickerWhitelist, setItems);

    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_parentalImportApply, R.drawable.baseline_check_24, R.string.ParentalImportApply).setTextColorId(ColorId.textNeutral));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

    adapter.setItems(items, false);
    recyclerView.setAdapter(adapter);
  }

  private void fillEntries (@Nullable JSONArray array, List<ListItem> out, int itemId) {
    if (array == null) return;
    for (int i = 0; i < array.length(); i++) {
      JSONObject entry = array.optJSONObject(i);
      if (entry == null) continue;
      out.add(new ListItem(ListItem.TYPE_CHECKBOX_OPTION, itemId, 0, describe(entry, itemId), true).setData(entry));
    }
  }

  private CharSequence describe (JSONObject entry, int itemId) {
    long id = entry.optLong("id", 0);
    String name = entry.optString("name", "");
    String title = entry.optString("title", "");
    String username = entry.optString("username", "");
    if (itemId == R.id.btn_parentalImportUser) {
      if (id > 0) {
        TdApi.User user = tdlib.cache().user(id);
        if (user != null) return TD.getUserName(id, user);
      }
      if (!name.isEmpty()) return username.isEmpty() ? name : name + " (@" + username + ")";
      if (!username.isEmpty()) return "@" + username;
      return id != 0 ? Long.toString(id) : "?";
    } else if (itemId == R.id.btn_parentalImportGroup) {
      TdApi.Chat chat = id != 0 ? tdlib.chat(id) : null;
      if (chat != null) return chat.title;
      return !title.isEmpty() ? title : Long.toString(id);
    } else {
      if (!title.isEmpty()) return name.isEmpty() ? title : title + " (" + name + ")";
      if (!name.isEmpty()) return name;
      return id != 0 ? Long.toString(id) : "?";
    }
  }

  private static void addSection (List<ListItem> items, int headerRes, List<ListItem> entries) {
    if (entries.isEmpty()) return;
    items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, headerRes));
    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    for (int i = 0; i < entries.size(); i++) {
      if (i > 0) items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
      items.add(entries.get(i));
    }
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
  }

  @Override
  public void onClick (View v) {
    final int id = v.getId();
    if (id == R.id.btn_parentalWhitelistToggle) {
      enabled = !enabled;
      adapter.updateValuedSettingById(id);
    } else if (id == R.id.btn_parentalAllowAddContact) {
      allowAddContact = !allowAddContact;
      adapter.updateValuedSettingById(id);
    } else if (id == R.id.btn_parentalStickersRestrict) {
      officialOnly = !officialOnly;
      adapter.updateValuedSettingById(id);
    } else if (id == R.id.btn_parentalImportUser || id == R.id.btn_parentalImportGroup || id == R.id.btn_parentalImportSet) {
      adapter.toggleView(v);
    } else if (id == R.id.btn_parentalImportApply) {
      apply();
    }
  }

  private void apply () {
    JSONObject root;
    try {
      root = new JSONObject();
      root.put("fortgram_parental", ParentalConfig.FORMAT_VERSION);
      JSONObject whitelist = new JSONObject();
      whitelist.put("enabled", enabled);
      whitelist.put("allow_add_contact", allowAddContact);
      whitelist.put("users", selected(userItems));
      whitelist.put("groups", selected(groupItems));
      root.put("whitelist", whitelist);
      JSONObject stickers = new JSONObject();
      stickers.put("official_only", officialOnly);
      stickers.put("sets", selected(setItems));
      root.put("stickers", stickers);
    } catch (JSONException e) {
      UI.showToast(R.string.ParentalImportInvalid, Toast.LENGTH_SHORT);
      return;
    }
    ParentalConfig.apply(root, tdlib, result -> runOnUiThreadOptional(() -> {
      String msg = Lang.getString(R.string.ParentalImportDone, result.users, result.groups, result.stickerSets);
      if (result.unresolved > 0) {
        msg += "\n" + Lang.getString(R.string.ParentalImportUnresolved, result.unresolved);
      }
      UI.showToast(msg, Toast.LENGTH_LONG);
      navigateBack();
      if (result.enabledChanged) {
        showConfirm(Lang.getString(R.string.ParentalRestartRequired),
          Lang.getString(R.string.ParentalRestartNow),
          R.drawable.baseline_replay_24,
          OptionColor.NORMAL,
          () -> {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
          });
      }
    }));
  }

  private static JSONArray selected (List<ListItem> items) {
    JSONArray array = new JSONArray();
    for (ListItem item : items) {
      if (item.isSelected() && item.getData() instanceof JSONObject) {
        array.put(item.getData());
      }
    }
    return array;
  }
}
