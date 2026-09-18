/*
 * This file is a part of FortGram (Telegram-X fork).
 *
 * ParentalPolicy — single source of truth for the parental whitelist mode.
 * All UI filters and TDLib send/open chokepoints consult this class.
 */
package org.thunderdog.challegram.fortgram;

import android.util.LongSparseArray;

import androidx.annotation.AnyThread;
import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.telegram.TdlibCache;
import org.thunderdog.challegram.unsorted.Settings;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import tgx.td.ChatId;

public final class ParentalPolicy {
  private static final String KEY_ENABLED = "fg_par_enabled";
  private static final String KEY_WHITELIST_USERS = "fg_par_whitelist_users";
  private static final String KEY_WHITELIST_GROUPS = "fg_par_whitelist_groups";
  private static final String KEY_ALLOW_ADD_CONTACT = "fg_par_allow_add_contact";
  private static final String KEY_STICKERS_RESTRICT = "fg_par_stickers_restrict";
  private static final String KEY_STICKERS_WHITELIST = "fg_par_stickers_whitelist";

  public interface Listener {
    void onParentalPolicyChanged ();
  }

  private static volatile ParentalPolicy instance;

  public static ParentalPolicy instance () {
    if (instance == null) {
      synchronized (ParentalPolicy.class) {
        if (instance == null) {
          instance = new ParentalPolicy();
        }
      }
    }
    return instance;
  }

  private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
  private boolean enabled;
  private boolean allowAddContact;
  private long[] manualUsers;
  private long[] allowedGroups;
  private boolean stickersRestrict;
  private long[] stickerWhitelist;
  private final LongSparseArray<Boolean> officialSetCache = new LongSparseArray<>();
  private final Set<Long> resolvingSets = new java.util.HashSet<>();

  private ParentalPolicy () {
    this.enabled = Settings.instance().getBoolean(KEY_ENABLED, false);
    this.allowAddContact = Settings.instance().getBoolean(KEY_ALLOW_ADD_CONTACT, true);
    long[] stored = Settings.instance().getLongArray(KEY_WHITELIST_USERS);
    this.manualUsers = stored != null ? stored : new long[0];
    long[] storedGroups = Settings.instance().getLongArray(KEY_WHITELIST_GROUPS);
    this.allowedGroups = storedGroups != null ? storedGroups : new long[0];
    this.stickersRestrict = Settings.instance().getBoolean(KEY_STICKERS_RESTRICT, false);
    long[] storedSets = Settings.instance().getLongArray(KEY_STICKERS_WHITELIST);
    this.stickerWhitelist = storedSets != null ? storedSets : new long[0];
  }

  public boolean isAddContactAllowed () {
    return allowAddContact;
  }

  public void setAddContactAllowed (boolean value) {
    if (this.allowAddContact != value) {
      this.allowAddContact = value;
      Settings.instance().putBoolean(KEY_ALLOW_ADD_CONTACT, value);
      notifyChanged();
    }
  }

  public void addListener (Listener l) {
    if (l != null) listeners.addIfAbsent(l);
  }

  public void removeListener (Listener l) {
    if (l != null) listeners.remove(l);
  }

  private void notifyChanged () {
    for (Listener l : listeners) {
      l.onParentalPolicyChanged();
    }
  }

  @AnyThread
  public boolean isEnabled () {
    return enabled;
  }

  public void setEnabled (boolean value) {
    if (this.enabled != value) {
      this.enabled = value;
      Settings.instance().putBoolean(KEY_ENABLED, value);
      notifyChanged();
    }
  }

  public long[] getManualUsers () {
    return manualUsers.clone();
  }

  public boolean isManuallyAllowed (long userId) {
    for (long id : manualUsers) {
      if (id == userId) return true;
    }
    return false;
  }

  public void addManualUser (long userId) {
    if (userId == 0 || isManuallyAllowed(userId)) return;
    long[] next = Arrays.copyOf(manualUsers, manualUsers.length + 1);
    next[manualUsers.length] = userId;
    manualUsers = next;
    Settings.instance().putLongArray(KEY_WHITELIST_USERS, next);
    notifyChanged();
  }

  public void removeManualUser (long userId) {
    if (!isManuallyAllowed(userId)) return;
    Set<Long> set = new LinkedHashSet<>(manualUsers.length);
    for (long id : manualUsers) {
      if (id != userId) set.add(id);
    }
    long[] next = new long[set.size()];
    int i = 0;
    for (Long id : set) next[i++] = id;
    manualUsers = next;
    Settings.instance().putLongArray(KEY_WHITELIST_USERS, next);
    notifyChanged();
  }

  /**
   * Replaces the whole policy at once (import). Persists every key and notifies
   * listeners a single time.
   */
  public void replaceAll (boolean enabled, boolean allowAddContact, long[] users, long[] groups, boolean stickersRestrict, long[] stickerSets) {
    this.enabled = enabled;
    this.allowAddContact = allowAddContact;
    this.manualUsers = users != null ? users.clone() : new long[0];
    this.allowedGroups = groups != null ? groups.clone() : new long[0];
    this.stickersRestrict = stickersRestrict;
    this.stickerWhitelist = stickerSets != null ? stickerSets.clone() : new long[0];
    Settings.instance().putBoolean(KEY_ENABLED, enabled);
    Settings.instance().putBoolean(KEY_ALLOW_ADD_CONTACT, allowAddContact);
    Settings.instance().putLongArray(KEY_WHITELIST_USERS, this.manualUsers);
    Settings.instance().putLongArray(KEY_WHITELIST_GROUPS, this.allowedGroups);
    Settings.instance().putBoolean(KEY_STICKERS_RESTRICT, stickersRestrict);
    Settings.instance().putLongArray(KEY_STICKERS_WHITELIST, this.stickerWhitelist);
    notifyChanged();
  }

  // Allowed groups -----------------------------------------------------------

  public long[] getAllowedGroups () {
    return allowedGroups.clone();
  }

  @AnyThread
  public boolean isGroupWhitelisted (long chatId) {
    for (long id : allowedGroups) {
      if (id == chatId) return true;
    }
    return false;
  }

  public void addAllowedGroup (long chatId) {
    if (chatId == 0 || isGroupWhitelisted(chatId)) return;
    long[] next = Arrays.copyOf(allowedGroups, allowedGroups.length + 1);
    next[allowedGroups.length] = chatId;
    allowedGroups = next;
    Settings.instance().putLongArray(KEY_WHITELIST_GROUPS, next);
    notifyChanged();
  }

  public void removeAllowedGroup (long chatId) {
    if (!isGroupWhitelisted(chatId)) return;
    long[] next = new long[allowedGroups.length - 1];
    int i = 0;
    for (long id : allowedGroups) {
      if (id != chatId) next[i++] = id;
    }
    allowedGroups = next;
    Settings.instance().putLongArray(KEY_WHITELIST_GROUPS, next);
    notifyChanged();
  }

  /**
   * Whether the chat is a group that may be put on the whitelist at all:
   * basic groups and supergroups, never broadcast channels.
   */
  public static boolean isWhitelistableGroup (long chatId, @Nullable Tdlib tdlib) {
    if (chatId == 0 || ChatId.isPrivate(chatId) || ChatId.isSecret(chatId)) return false;
    return tdlib == null || !tdlib.isChannel(chatId);
  }

  // Sticker restriction ------------------------------------------------------

  @AnyThread
  public boolean isStickerRestrictionEnabled () {
    return stickersRestrict;
  }

  public void setStickerRestrictionEnabled (boolean value) {
    if (this.stickersRestrict != value) {
      this.stickersRestrict = value;
      Settings.instance().putBoolean(KEY_STICKERS_RESTRICT, value);
      notifyChanged();
    }
  }

  public long[] getStickerWhitelist () {
    return stickerWhitelist.clone();
  }

  public boolean isStickerSetWhitelisted (long setId) {
    for (long id : stickerWhitelist) {
      if (id == setId) return true;
    }
    return false;
  }

  public void addStickerSet (long setId) {
    if (setId == 0 || isStickerSetWhitelisted(setId)) return;
    long[] next = Arrays.copyOf(stickerWhitelist, stickerWhitelist.length + 1);
    next[stickerWhitelist.length] = setId;
    stickerWhitelist = next;
    Settings.instance().putLongArray(KEY_STICKERS_WHITELIST, next);
    notifyChanged();
  }

  public void removeStickerSet (long setId) {
    if (!isStickerSetWhitelisted(setId)) return;
    Set<Long> set = new LinkedHashSet<>(stickerWhitelist.length);
    for (long id : stickerWhitelist) {
      if (id != setId) set.add(id);
    }
    long[] next = new long[set.size()];
    int i = 0;
    for (Long id : set) next[i++] = id;
    stickerWhitelist = next;
    Settings.instance().putLongArray(KEY_STICKERS_WHITELIST, next);
    notifyChanged();
  }

  /**
   * Records the official-ness of an observed sticker set so the send-time gate
   * (where only the set id is known) can decide without an extra network round-trip.
   */
  @AnyThread
  public void noteStickerSet (long setId, boolean isOfficial) {
    if (setId == 0) return;
    synchronized (officialSetCache) {
      officialSetCache.put(setId, isOfficial);
    }
  }

  /** @return TRUE/FALSE if known, null if the set has not been observed yet. */
  @AnyThread
  public @Nullable Boolean knownOfficial (long setId) {
    synchronized (officialSetCache) {
      return officialSetCache.get(setId);
    }
  }

  /**
   * Whether a sticker set may be shown / used. When the restriction is off this
   * always returns true (stock behaviour). Otherwise official sets and whitelisted
   * sets are allowed.
   */
  @AnyThread
  public boolean isStickerSetAllowed (long setId, boolean isOfficial) {
    if (!stickersRestrict) return true;
    return isOfficial || isStickerSetWhitelisted(setId);
  }

  /**
   * Render-time decision: should a sticker from this set be drawn as its fallback
   * emoji instead of the image? Safe default for an unknown set is {@code true}
   * (emoji), so unverified packs never display their imagery; callers should then
   * {@link #resolveStickerSet} to upgrade once the set is known to be allowed.
   */
  @AnyThread
  public boolean shouldRenderStickerAsEmoji (long setId) {
    if (!stickersRestrict) return false;
    if (setId == 0) return true;
    if (isStickerSetWhitelisted(setId)) return false;
    Boolean official = knownOfficial(setId);
    if (official != null) {
      return !official;
    }
    return true;
  }

  /**
   * Resolves the official-ness of a not-yet-observed set and caches it, invoking
   * {@code onResolved} on the result so the caller can invalidate / redraw. No-op
   * when the set is already known or a request is already in flight.
   */
  @AnyThread
  public void resolveStickerSet (@Nullable Tdlib tdlib, long setId, @Nullable Runnable onResolved) {
    if (tdlib == null || setId == 0 || knownOfficial(setId) != null) {
      return;
    }
    synchronized (resolvingSets) {
      if (!resolvingSets.add(setId)) {
        return;
      }
    }
    tdlib.client().send(new TdApi.GetStickerSet(setId), result -> {
      boolean isOfficial = result instanceof TdApi.StickerSet && ((TdApi.StickerSet) result).isOfficial;
      noteStickerSet(setId, isOfficial);
      synchronized (resolvingSets) {
        resolvingSets.remove(setId);
      }
      if (onResolved != null) {
        onResolved.run();
      }
    });
  }

  // Policy decisions ---------------------------------------------------------

  public boolean canCreateSecret () {
    return !enabled;
  }

  public boolean canJoinChannels () {
    return !enabled;
  }

  public boolean canCreateGroups () {
    return !enabled;
  }

  public boolean canAddContact () {
    return !enabled || allowAddContact;
  }

  /**
   * Whether the given user is allowed to be communicated with.
   * In stage 1: user is allowed if they are a Telegram contact or in the manual whitelist.
   */
  public boolean isUserAllowed (long userId, @Nullable Tdlib tdlib) {
    if (!enabled) return true;
    if (userId == 0) return false;
    if (tdlib != null && tdlib.isSelfUserId(userId)) return true;
    if (isManuallyAllowed(userId)) return true;
    if (tdlib != null) {
      TdlibCache cache = tdlib.cache();
      TdApi.User user = cache != null ? cache.user(userId) : null;
      return user != null && user.isContact;
    }
    return false;
  }

  /**
   * Whether the chat itself is allowed to be visible / opened / written to.
   * Private chats with allowed users and groups whitelisted by the parent.
   * Channels, secret chats and every other group are not allowed.
   */
  public boolean isChatAllowed (long chatId, @Nullable Tdlib tdlib) {
    if (!enabled) return true;
    if (chatId == 0) return false;
    if (ChatId.isSecret(chatId)) return false;
    // Telegram service notifications (login codes etc.) must always reach the child.
    if (tdlib != null && tdlib.isServiceNotificationsChat(chatId)) return true;
    if (ChatId.isPrivate(chatId)) {
      return isUserAllowed(ChatId.toUserId(chatId), tdlib);
    }
    // Groups are allowed only when the parent whitelisted them; channels never.
    return isGroupWhitelisted(chatId) && isWhitelistableGroup(chatId, tdlib);
  }

  public boolean canSendTo (long chatId, @Nullable Tdlib tdlib) {
    return isChatAllowed(chatId, tdlib);
  }

  /**
   * Returns true when the supplied URL/internal link must be denied (channel/public chat resolution etc).
   * Called early from openTelegramUrl/openInternalLinkType to short-circuit network resolution for things
   * we will refuse anyway.
   */
  public boolean isLinkTypeAllowed (@Nullable TdApi.InternalLinkType type) {
    if (!enabled || type == null) return true;
    switch (type.getConstructor()) {
      case TdApi.InternalLinkTypeChatInvite.CONSTRUCTOR:
      case TdApi.InternalLinkTypePublicChat.CONSTRUCTOR:
      case TdApi.InternalLinkTypeBotStart.CONSTRUCTOR:
      case TdApi.InternalLinkTypeBotStartInGroup.CONSTRUCTOR:
      case TdApi.InternalLinkTypeBotAddToChannel.CONSTRUCTOR:
      case TdApi.InternalLinkTypeMessage.CONSTRUCTOR:
      case TdApi.InternalLinkTypeVideoChat.CONSTRUCTOR:
        return false;
      default:
        return true;
    }
  }
}
