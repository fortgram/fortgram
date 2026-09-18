/*
 * This file is a part of FortGram (Telegram-X fork).
 *
 * Allowed groups editor — lists the groups the child is a member of and lets
 * the parent switch on the ones the child may read and write in.
 * Gated by ParentalPin via the caller.
 */
package org.thunderdog.challegram.fortgram;

import android.content.Context;
import android.view.View;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.component.base.SettingView;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.ui.ListItem;
import org.thunderdog.challegram.ui.RecyclerViewController;
import org.thunderdog.challegram.ui.SettingsAdapter;
import org.thunderdog.challegram.v.CustomRecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SettingsGroupWhitelistController extends RecyclerViewController<Void> implements View.OnClickListener {
  private static final int CHATS_LIMIT = 1000;

  private SettingsAdapter adapter;
  private final Set<Long> memberGroups = new LinkedHashSet<>();
  private int pendingLists;

  public SettingsGroupWhitelistController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.ParentalGroups);
  }

  @Override
  public int getId () {
    return R.id.controller_parentalGroups;
  }

  @Override
  protected void onCreateView (Context context, CustomRecyclerView recyclerView) {
    adapter = new SettingsAdapter(this) {
      @Override
      public void setValuedSetting (ListItem item, SettingView v, boolean isUpdate) {
        if (item.getId() == R.id.btn_parentalGroupItem) {
          long chatId = item.getLongId();
          TdApi.Chat chat = tdlib.chat(chatId);
          if (chat != null) {
            v.setName(chat.title);
            v.setData(R.string.Group);
          } else {
            v.setName(Long.toString(chatId));
            v.setData(R.string.ParentalGroupUnavailable);
          }
          v.getToggler().setRadioEnabled(ParentalPolicy.instance().isGroupWhitelisted(chatId), isUpdate);
        }
      }
    };
    recyclerView.setAdapter(adapter);
    loadGroups();
  }

  private void loadGroups () {
    pendingLists = 2;
    loadGroups(new TdApi.ChatListMain());
    loadGroups(new TdApi.ChatListArchive());
  }

  private void loadGroups (TdApi.ChatList chatList) {
    tdlib.client().send(new TdApi.GetChats(chatList, CHATS_LIMIT), result -> runOnUiThreadOptional(() -> {
      if (result instanceof TdApi.Chats) {
        for (long chatId : ((TdApi.Chats) result).chatIds) {
          if (ParentalPolicy.isWhitelistableGroup(chatId, tdlib)) {
            memberGroups.add(chatId);
          }
        }
      }
      if (--pendingLists == 0) {
        rebuildItems();
      }
    }));
  }

  private void rebuildItems () {
    // Whitelisted groups that are no longer in the chat list stay visible so they can be switched off.
    Set<Long> chatIds = new LinkedHashSet<>(memberGroups);
    for (long chatId : ParentalPolicy.instance().getAllowedGroups()) {
      chatIds.add(chatId);
    }

    List<ListItem> items = new ArrayList<>();
    items.add(new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL));
    if (chatIds.isEmpty()) {
      items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.ParentalGroupsEmpty));
    } else {
      items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.ParentalGroupsHeader));
      items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
      boolean first = true;
      for (long chatId : chatIds) {
        if (first) {
          first = false;
        } else {
          items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
        }
        items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_parentalGroupItem, 0, 0).setLongId(chatId));
      }
      items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
    }
    items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.ParentalGroupsHint));
    adapter.setItems(items, false);
  }

  @Override
  public void onClick (View v) {
    if (v.getId() == R.id.btn_parentalGroupItem) {
      Object tag = v.getTag();
      if (!(tag instanceof ListItem)) return;
      long chatId = ((ListItem) tag).getLongId();
      if (ParentalPolicy.instance().isGroupWhitelisted(chatId)) {
        ParentalPolicy.instance().removeAllowedGroup(chatId);
      } else {
        ParentalPolicy.instance().addAllowedGroup(chatId);
      }
      adapter.updateValuedSettingByLongId(chatId);
    }
  }
}
