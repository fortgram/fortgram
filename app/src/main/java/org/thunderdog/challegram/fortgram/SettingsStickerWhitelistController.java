/*
 * This file is a part of FortGram (Telegram-X fork).
 *
 * Allowed sticker packs editor. Lets a parent toggle which installed packs are
 * allowed and add additional packs by link/short name. Official packs are always
 * allowed and shown as locked. Gated by ParentalPin via the caller.
 */
package org.thunderdog.challegram.fortgram;

import android.content.Context;
import android.util.LongSparseArray;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.component.base.SettingView;
import org.thunderdog.challegram.component.sticker.TGStickerObj;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.ui.ListItem;
import org.thunderdog.challegram.ui.RecyclerViewController;
import org.thunderdog.challegram.ui.SettingsAdapter;
import org.thunderdog.challegram.v.CustomRecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SettingsStickerWhitelistController extends RecyclerViewController<Void> implements View.OnClickListener {
  private SettingsAdapter adapter;
  private final LongSparseArray<TdApi.StickerSetInfo> installedSets = new LongSparseArray<>();
  private final LongSparseArray<String> resolvedTitles = new LongSparseArray<>();
  private final LongSparseArray<TdApi.Sticker> resolvedCovers = new LongSparseArray<>();
  private final LongSparseArray<TGStickerObj> coverCache = new LongSparseArray<>();
  private boolean loaded;

  public SettingsStickerWhitelistController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.ParentalStickerWhitelist);
  }

  @Override
  public int getId () {
    return R.id.controller_parentalStickerWhitelist;
  }

  @Override
  protected void onCreateView (Context context, CustomRecyclerView recyclerView) {
    adapter = new SettingsAdapter(this) {
      @Override
      public void setValuedSetting (ListItem item, SettingView v, boolean isUpdate) {
        if (item.getId() == R.id.btn_parentalStickerSetItem) {
          long setId = item.getLongId();
          TdApi.StickerSetInfo info = installedSets.get(setId);
          String title = info != null ? info.title : resolvedTitles.get(setId);
          v.setName(title != null ? title : Lang.getString(R.string.ParentalStickerWhitelist));
          boolean official = info != null && info.isOfficial;
          if (official) {
            v.setData(R.string.ParentalStickerOfficialAlways);
            v.getToggler().setRadioEnabled(true, isUpdate);
          } else {
            boolean allowed = ParentalPolicy.instance().isStickerSetWhitelisted(setId);
            v.setData(allowed ? R.string.ParentalStickerAllowed : R.string.ParentalStickerNotAllowed);
            v.getToggler().setRadioEnabled(allowed, isUpdate);
          }
          TGStickerObj cover = getCover(setId);
          if (cover != null) {
            v.forcePadding(org.thunderdog.challegram.tool.Screen.dp(73f), 0);
            StickerCoverModifier modifier = new StickerCoverModifier(cover);
            v.setDrawModifier(modifier);
            modifier.requestFiles(v.getComplexReceiver());
          } else {
            v.forcePadding(0, 0);
            v.setDrawModifier(null);
          }
        }
      }
    };
    recyclerView.setAdapter(adapter);
    loadInstalled();
  }

  private void loadInstalled () {
    tdlib.client().send(new TdApi.GetInstalledStickerSets(new TdApi.StickerTypeRegular()), result -> runOnUiThreadOptional(() -> {
      if (result instanceof TdApi.StickerSets) {
        installedSets.clear();
        for (TdApi.StickerSetInfo info : ((TdApi.StickerSets) result).sets) {
          installedSets.put(info.id, info);
          ParentalPolicy.instance().noteStickerSet(info.id, info.isOfficial);
        }
      }
      loaded = true;
      resolveMissingWhitelistTitles();
      rebuildItems();
    }));
  }

  private void resolveMissingWhitelistTitles () {
    for (long setId : ParentalPolicy.instance().getStickerWhitelist()) {
      if (installedSets.get(setId) == null && resolvedTitles.get(setId) == null) {
        tdlib.client().send(new TdApi.GetStickerSet(setId), result -> runOnUiThreadOptional(() -> {
          if (result instanceof TdApi.StickerSet) {
            TdApi.StickerSet set = (TdApi.StickerSet) result;
            resolvedTitles.put(set.id, set.title);
            if (set.stickers != null && set.stickers.length > 0) {
              resolvedCovers.put(set.id, set.stickers[0]);
            }
            ParentalPolicy.instance().noteStickerSet(set.id, set.isOfficial);
          } else {
            resolvedTitles.put(setId, Lang.getString(R.string.ParentalStickerWhitelist));
          }
          rebuildItems();
        }));
      }
    }
  }

  private @Nullable TGStickerObj getCover (long setId) {
    TGStickerObj cached = coverCache.get(setId);
    if (cached != null) {
      return cached;
    }
    TdApi.Sticker raw = null;
    TdApi.StickerType type = null;
    TdApi.StickerSetInfo info = installedSets.get(setId);
    if (info != null) {
      if (info.covers != null && info.covers.length > 0) {
        raw = info.covers[0];
      }
      type = info.stickerType;
    } else {
      raw = resolvedCovers.get(setId);
    }
    if (raw == null) {
      return null;
    }
    if (type == null) {
      type = new TdApi.StickerTypeRegular();
    }
    TGStickerObj cover = new TGStickerObj(tdlib, raw, null, type);
    coverCache.put(setId, cover);
    return cover;
  }

  private void rebuildItems () {
    if (!loaded) return;
    List<ListItem> items = new ArrayList<>();
    items.add(new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL));
    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_parentalStickerWhitelistAdd, R.drawable.baseline_link_24, R.string.ParentalStickerWhitelistAdd));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
    items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.ParentalStickerWhitelistAddHint));

    boolean headerAdded = false;
    for (int i = 0; i < installedSets.size(); i++) {
      TdApi.StickerSetInfo info = installedSets.valueAt(i);
      if (!headerAdded) {
        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.ParentalStickerInstalledHeader));
        items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
        headerAdded = true;
      } else {
        items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
      }
      items.add(new ListItem(ListItem.TYPE_VALUED_SETTING_COMPACT_WITH_TOGGLER, R.id.btn_parentalStickerSetItem, 0, 0).setLongId(info.id));
    }
    if (headerAdded) {
      items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
    }

    boolean extraHeaderAdded = false;
    for (long setId : ParentalPolicy.instance().getStickerWhitelist()) {
      if (installedSets.get(setId) != null) {
        continue;
      }
      if (!extraHeaderAdded) {
        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.ParentalStickerWhitelist));
        items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
        extraHeaderAdded = true;
      } else {
        items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
      }
      items.add(new ListItem(ListItem.TYPE_VALUED_SETTING_COMPACT_WITH_TOGGLER, R.id.btn_parentalStickerSetItem, 0, 0).setLongId(setId));
    }
    if (extraHeaderAdded) {
      items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
    }

    if (!headerAdded && !extraHeaderAdded) {
      items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.ParentalStickerWhitelistEmpty));
    }

    adapter.setItems(items, false);
  }

  @Override
  public void onClick (View v) {
    int id = v.getId();
    if (id == R.id.btn_parentalStickerWhitelistAdd) {
      promptAddByLink();
    } else if (id == R.id.btn_parentalStickerSetItem) {
      Object tag = v.getTag();
      if (!(tag instanceof ListItem)) return;
      long setId = ((ListItem) tag).getLongId();
      TdApi.StickerSetInfo info = installedSets.get(setId);
      if (info != null && info.isOfficial) {
        UI.showToast(R.string.ParentalStickerOfficialAlways, Toast.LENGTH_SHORT);
        return;
      }
      if (ParentalPolicy.instance().isStickerSetWhitelisted(setId)) {
        ParentalPolicy.instance().removeStickerSet(setId);
      } else {
        ParentalPolicy.instance().addStickerSet(setId);
      }
      if (info != null) {
        adapter.updateValuedSettingByLongId(setId);
      } else {
        rebuildItems();
      }
    }
  }

  private void promptAddByLink () {
    openInputAlert(
      Lang.getString(R.string.ParentalStickerWhitelistAdd),
      Lang.getString(R.string.ParentalStickerWhitelistAdd),
      R.string.Done, R.string.Cancel, null,
      (input, query) -> {
        String name = parseStickerSetName(query);
        if (name.isEmpty()) return false;
        resolveAndAdd(name);
        return true;
      }, true);
  }

  private static String parseStickerSetName (String query) {
    String trimmed = query != null ? query.trim() : "";
    int idx = trimmed.indexOf("addstickers/");
    if (idx >= 0) {
      trimmed = trimmed.substring(idx + "addstickers/".length());
    }
    int slash = trimmed.indexOf('/');
    if (slash >= 0) {
      trimmed = trimmed.substring(0, slash);
    }
    int q = trimmed.indexOf('?');
    if (q >= 0) {
      trimmed = trimmed.substring(0, q);
    }
    return trimmed.trim();
  }

  private void resolveAndAdd (String name) {
    tdlib.client().send(new TdApi.SearchStickerSet(name, false), result -> runOnUiThreadOptional(() -> {
      if (!(result instanceof TdApi.StickerSet)) {
        UI.showToast(R.string.ParentalStickerSetNotFound, Toast.LENGTH_SHORT);
        return;
      }
      TdApi.StickerSet set = (TdApi.StickerSet) result;
      ParentalPolicy.instance().addStickerSet(set.id);
      ParentalPolicy.instance().noteStickerSet(set.id, set.isOfficial);
      resolvedTitles.put(set.id, set.title);
      rebuildItems();
    }));
  }
}
