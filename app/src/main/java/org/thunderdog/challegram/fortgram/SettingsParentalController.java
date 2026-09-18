/*
 * This file is a part of FortGram (Telegram-X fork).
 *
 * Parental controls settings screen. Gated by ParentalPin —
 * caller must verify PIN before navigating here.
 */
package org.thunderdog.challegram.fortgram;

import android.content.Context;
import android.view.View;

import org.thunderdog.challegram.R;
import org.thunderdog.challegram.component.base.SettingView;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.ui.ListItem;
import org.thunderdog.challegram.ui.RecyclerViewController;
import org.thunderdog.challegram.ui.SettingsAdapter;
import org.thunderdog.challegram.v.CustomRecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SettingsParentalController extends RecyclerViewController<Void> implements View.OnClickListener {
  private SettingsAdapter adapter;

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

    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_parentalPinChange, R.drawable.baseline_lock_24, R.string.ParentalPinChange));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_parentalPinDisable, R.drawable.baseline_block_24, R.string.ParentalPinDisable).setTextColorId(ColorId.textNegative));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

    adapter.setItems(items, false);
    recyclerView.setAdapter(adapter);
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
