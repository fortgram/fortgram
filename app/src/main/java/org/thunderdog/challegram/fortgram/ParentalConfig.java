/*
 * This file is a part of FortGram (Telegram-X fork).
 *
 * ParentalConfig — export / import of the parental control settings as JSON.
 * The PIN is never exported. Entries carry both numeric ids and human-readable
 * handles (username / sticker short name), so a config edited by hand or produced
 * elsewhere can reference users and packs by name; import resolves them on device.
 */
package org.thunderdog.challegram.fortgram;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thunderdog.challegram.data.TD;
import org.thunderdog.challegram.telegram.Tdlib;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import me.vkryl.core.lambda.RunnableData;

public final class ParentalConfig {
  public static final int FORMAT_VERSION = 1;
  private static final String KEY_MAGIC = "fortgram_parental";

  private ParentalConfig () { }

  public static final class ImportResult {
    public int users, groups, stickerSets, unresolved;
    public boolean enabledChanged;
  }

  // Export -------------------------------------------------------------------

  /**
   * Builds the JSON export. Sticker pack names are looked up asynchronously
   * (installed sets only; unknown sets are exported by id alone).
   */
  public static void export (@NonNull Tdlib tdlib, @NonNull RunnableData<String> callback) {
    ParentalPolicy policy = ParentalPolicy.instance();
    long[] setIds = policy.getStickerWhitelist();
    tdlib.client().send(new TdApi.GetInstalledStickerSets(new TdApi.StickerTypeRegular()), result -> {
      TdApi.StickerSetInfo[] installed = result instanceof TdApi.StickerSets ? ((TdApi.StickerSets) result).sets : new TdApi.StickerSetInfo[0];
      String json;
      try {
        json = buildJson(tdlib, policy, setIds, installed);
      } catch (JSONException e) {
        json = null;
      }
      callback.runWithData(json);
    });
  }

  private static String buildJson (Tdlib tdlib, ParentalPolicy policy, long[] setIds, TdApi.StickerSetInfo[] installed) throws JSONException {
    JSONObject root = new JSONObject();
    root.put(KEY_MAGIC, FORMAT_VERSION);
    root.put("exported_at", System.currentTimeMillis() / 1000L);

    JSONObject whitelist = new JSONObject();
    whitelist.put("enabled", policy.isEnabled());
    whitelist.put("allow_add_contact", policy.isAddContactAllowed());
    JSONArray users = new JSONArray();
    for (long userId : policy.getManualUsers()) {
      JSONObject u = new JSONObject();
      u.put("id", userId);
      TdApi.User user = tdlib.cache().user(userId);
      if (user != null) {
        String username = user.usernames != null && user.usernames.activeUsernames.length > 0 ? user.usernames.activeUsernames[0] : null;
        if (username != null && !username.isEmpty()) u.put("username", username);
        u.put("name", TD.getUserName(userId, user));
      }
      users.put(u);
    }
    whitelist.put("users", users);
    JSONArray groups = new JSONArray();
    for (long chatId : policy.getAllowedGroups()) {
      JSONObject g = new JSONObject();
      g.put("id", chatId);
      TdApi.Chat chat = tdlib.chat(chatId);
      if (chat != null) g.put("title", chat.title);
      groups.put(g);
    }
    whitelist.put("groups", groups);
    root.put("whitelist", whitelist);

    JSONObject stickers = new JSONObject();
    stickers.put("official_only", policy.isStickerRestrictionEnabled());
    JSONArray sets = new JSONArray();
    for (long setId : setIds) {
      JSONObject s = new JSONObject();
      s.put("id", setId);
      for (TdApi.StickerSetInfo info : installed) {
        if (info.id == setId) {
          s.put("name", info.name);
          s.put("title", info.title);
          break;
        }
      }
      sets.put(s);
    }
    stickers.put("sets", sets);
    root.put("stickers", stickers);
    return root.toString(2);
  }

  // Compact (QR) form --------------------------------------------------------

  /**
   * Looks like a tg:// link on purpose: the in-app QR scanner only reports codes
   * that start with tg:// or t.me, and a stock Telegram client simply ignores an
   * unknown tg:// path.
   */
  private static final String COMPACT_PREFIX = "tg://fgram/parental?p=";
  private static final String COMPACT_PREFIX_V0 = "FGP1:";
  /** Byte-mode QR capacity at version 40 / level L is 2953 bytes; keep a margin. */
  public static final int COMPACT_MAX_LENGTH = 2800;

  /** JSON → deflate → base64url, prefixed so a scanner can tell it apart from other QR codes. */
  public static @Nullable String toCompact (@NonNull String json) {
    try {
      byte[] input;
      // Re-serialise without indentation to save space.
      try {
        input = new JSONObject(json).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
      } catch (JSONException e) {
        input = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      }
      Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
      deflater.setInput(input);
      deflater.finish();
      ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
      byte[] buf = new byte[4096];
      while (!deflater.finished()) {
        out.write(buf, 0, deflater.deflate(buf));
      }
      deflater.end();
      String encoded = android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING);
      String compact = COMPACT_PREFIX + encoded;
      return compact.length() <= COMPACT_MAX_LENGTH ? compact : null;
    } catch (Throwable t) {
      return null;
    }
  }

  /** Accepts either the compact QR form or plain JSON; returns JSON text or null. */
  public static @Nullable String fromCompact (@Nullable String text) {
    if (text == null) return null;
    String trimmed = text.trim();
    String prefix = trimmed.startsWith(COMPACT_PREFIX) ? COMPACT_PREFIX : trimmed.startsWith(COMPACT_PREFIX_V0) ? COMPACT_PREFIX_V0 : null;
    if (prefix == null) {
      return trimmed;
    }
    try {
      byte[] data = android.util.Base64.decode(trimmed.substring(prefix.length()), android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING);
      Inflater inflater = new Inflater(true);
      inflater.setInput(data);
      ByteArrayOutputStream out = new ByteArrayOutputStream(data.length * 4);
      byte[] buf = new byte[4096];
      while (!inflater.finished()) {
        int n = inflater.inflate(buf);
        if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break;
        out.write(buf, 0, n);
        if (out.size() > 4_000_000) return null;
      }
      inflater.end();
      return new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
    } catch (DataFormatException | IllegalArgumentException e) {
      return null;
    }
  }

  // Import -------------------------------------------------------------------

  /** @return parsed root object, or null when the text is not a FortGram export. */
  public static @Nullable JSONObject parse (@Nullable String text) {
    if (text == null) return null;
    String trimmed = text.trim();
    if (trimmed.isEmpty()) return null;
    try {
      JSONObject root = new JSONObject(trimmed);
      if (!root.has(KEY_MAGIC)) return null;
      if (root.getInt(KEY_MAGIC) > FORMAT_VERSION) return null;
      return root;
    } catch (JSONException e) {
      return null;
    }
  }

  /**
   * Applies a parsed export. Users and sticker sets without a numeric id are
   * resolved by username / short name first; the callback runs on the TDLib
   * thread once everything is applied.
   */
  public static void apply (@NonNull JSONObject root, @NonNull Tdlib tdlib, @NonNull RunnableData<ImportResult> callback) {
    JSONObject whitelist = root.optJSONObject("whitelist");
    JSONObject stickers = root.optJSONObject("stickers");
    JSONArray users = whitelist != null ? whitelist.optJSONArray("users") : null;
    JSONArray groups = whitelist != null ? whitelist.optJSONArray("groups") : null;
    JSONArray sets = stickers != null ? stickers.optJSONArray("sets") : null;

    final ImportResult result = new ImportResult();
    final Set<Long> userIds = new LinkedHashSet<>();
    final Set<Long> groupIds = new LinkedHashSet<>();
    final Set<Long> setIds = new LinkedHashSet<>();
    final List<String> usernamesToResolve = new ArrayList<>();
    final List<String> setNamesToResolve = new ArrayList<>();

    if (users != null) {
      for (int i = 0; i < users.length(); i++) {
        JSONObject u = users.optJSONObject(i);
        if (u == null) continue;
        long id = u.optLong("id", 0);
        String username = u.optString("username", "");
        if (id > 0) {
          userIds.add(id);
        } else if (!username.isEmpty()) {
          usernamesToResolve.add(username.startsWith("@") ? username.substring(1) : username);
        } else {
          result.unresolved++;
        }
      }
    }
    if (groups != null) {
      for (int i = 0; i < groups.length(); i++) {
        JSONObject g = groups.optJSONObject(i);
        long id = g != null ? g.optLong("id", 0) : 0;
        if (id != 0 && ParentalPolicy.isWhitelistableGroup(id, null)) {
          groupIds.add(id);
        } else {
          result.unresolved++;
        }
      }
    }
    if (sets != null) {
      for (int i = 0; i < sets.length(); i++) {
        JSONObject s = sets.optJSONObject(i);
        if (s == null) continue;
        long id = s.optLong("id", 0);
        String name = s.optString("name", "");
        if (id != 0) {
          setIds.add(id);
        } else if (!name.isEmpty()) {
          setNamesToResolve.add(name);
        } else {
          result.unresolved++;
        }
      }
    }

    final boolean enabled = whitelist != null && whitelist.optBoolean("enabled", ParentalPolicy.instance().isEnabled());
    final boolean allowAddContact = whitelist == null || whitelist.optBoolean("allow_add_contact", ParentalPolicy.instance().isAddContactAllowed());
    final boolean officialOnly = stickers != null && stickers.optBoolean("official_only", ParentalPolicy.instance().isStickerRestrictionEnabled());

    final AtomicInteger pending = new AtomicInteger(usernamesToResolve.size() + setNamesToResolve.size());
    final Runnable finish = () -> {
      ParentalPolicy policy = ParentalPolicy.instance();
      result.enabledChanged = policy.isEnabled() != enabled;
      policy.replaceAll(enabled, allowAddContact, toArray(userIds), toArray(groupIds), officialOnly, toArray(setIds));
      result.users = userIds.size();
      result.groups = groupIds.size();
      result.stickerSets = setIds.size();
      callback.runWithData(result);
    };
    if (pending.get() == 0) {
      finish.run();
      return;
    }
    for (String username : usernamesToResolve) {
      tdlib.client().send(new TdApi.SearchPublicChat(username), r -> {
        long userId = r instanceof TdApi.Chat ? tgx.td.ChatId.toUserId(((TdApi.Chat) r).id) : 0;
        synchronized (result) {
          if (userId != 0) userIds.add(userId); else result.unresolved++;
        }
        if (pending.decrementAndGet() == 0) finish.run();
      });
    }
    for (String name : setNamesToResolve) {
      tdlib.client().send(new TdApi.SearchStickerSet(name, false), r -> {
        synchronized (result) {
          if (r instanceof TdApi.StickerSet) {
            TdApi.StickerSet set = (TdApi.StickerSet) r;
            setIds.add(set.id);
            ParentalPolicy.instance().noteStickerSet(set.id, set.isOfficial);
          } else {
            result.unresolved++;
          }
        }
        if (pending.decrementAndGet() == 0) finish.run();
      });
    }
  }

  private static long[] toArray (Set<Long> set) {
    long[] out = new long[set.size()];
    int i = 0;
    for (Long v : set) out[i++] = v;
    return out;
  }
}
