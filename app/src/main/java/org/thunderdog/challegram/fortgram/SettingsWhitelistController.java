/*
 * This file is a part of FortGram (Telegram-X fork).
 *
 * Whitelist editor — shows the manual list of additionally allowed users
 * (those NOT already in phone contacts). Add by @username.
 * Gated by ParentalPin via the caller.
 */
package org.thunderdog.challegram.fortgram;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import org.drinkless.tdlib.TdApi;
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

public class SettingsWhitelistController extends RecyclerViewController<Void> implements View.OnClickListener {
  private SettingsAdapter adapter;

  public SettingsWhitelistController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.ParentalWhitelistContacts);
  }

  @Override
  public int getId () {
    return R.id.controller_parentalWhitelist;
  }

  @Override
  protected void onCreateView (Context context, CustomRecyclerView recyclerView) {
    adapter = new SettingsAdapter(this) {
      @Override
      public void setValuedSetting (ListItem item, SettingView v, boolean isUpdate) {
        if (item.getId() == R.id.btn_parentalWhitelistUser) {
          long userId = item.getLongId();
          TdApi.User user = tdlib.cache().user(userId);
          v.setName(TD.getUserName(userId, user));
          v.setData(R.string.ParentalWhitelistRemove);
        }
      }
    };

    recyclerView.setAdapter(adapter);
    rebuildItems();
  }

  private void rebuildItems () {
    List<ListItem> items = new ArrayList<>();
    items.add(new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL));
    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_parentalWhitelistAdd, R.drawable.baseline_person_add_24, R.string.ParentalWhitelistAddUser));

    long[] users = ParentalPolicy.instance().getManualUsers();
    if (users.length == 0) {
      items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
      items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.ParentalWhitelistEmpty));
    } else {
      for (long userId : users) {
        items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
        items.add(new ListItem(ListItem.TYPE_VALUED_SETTING, R.id.btn_parentalWhitelistUser, R.drawable.baseline_person_24, 0).setLongId(userId));
      }
      items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
    }

    adapter.setItems(items, false);
  }

  @Override
  public void onClick (View v) {
    int id = v.getId();
    if (id == R.id.btn_parentalWhitelistAdd) {
      promptAddByUsername();
    } else if (id == R.id.btn_parentalWhitelistUser) {
      Object tag = v.getTag();
      if (tag instanceof ListItem) {
        long userId = ((ListItem) tag).getLongId();
        showConfirm(Lang.getString(R.string.ParentalWhitelistRemove),
          Lang.getString(R.string.ParentalWhitelistRemove),
          R.drawable.baseline_remove_circle_24,
          OptionColor.RED,
          () -> {
            ParentalPolicy.instance().removeManualUser(userId);
            rebuildItems();
          });
      }
    }
  }

  private void promptAddByUsername () {
    openInputAlert(
      Lang.getString(R.string.ParentalWhitelistAddUser),
      Lang.getString(R.string.Username),
      R.string.Done, R.string.Cancel, null,
      (input, query) -> {
        String trimmed = query != null ? query.trim() : "";
        if (trimmed.startsWith("@")) trimmed = trimmed.substring(1);
        if (trimmed.isEmpty()) return false;
        resolveAndAdd(trimmed);
        return true;
      }, true);
  }

  private void resolveAndAdd (String username) {
    tdlib.send(new TdApi.SearchPublicChat(username), (chat, error) -> runOnUiThreadOptional(() -> {
      if (chat == null || error != null) {
        UI.showToast(R.string.UsernameNotOccupied, Toast.LENGTH_SHORT);
        return;
      }
      long userId = tgx.td.ChatId.toUserId(chat.id);
      if (userId == 0) {
        UI.showToast(R.string.UsernameNotOccupied, Toast.LENGTH_SHORT);
        return;
      }
      ParentalPolicy.instance().addManualUser(userId);
      rebuildItems();
    }));
  }
}
