/*
 * This file is a part of MiniGram (Telegram-X fork).
 *
 * Parental controls settings screen. Gated by ParentalPin —
 * caller must verify PIN before navigating here.
 */
package org.thunderdog.challegram.minigram;

import android.content.Context;
import android.view.View;

import org.thunderdog.challegram.R;
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
    SettingsAdapter adapter = new SettingsAdapter(this);

    List<ListItem> items = new ArrayList<>();
    items.add(new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL));

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
    if (viewId == R.id.btn_parentalPinChange) {
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
