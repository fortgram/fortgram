/*
 * This file is a part of MiniGram (Telegram-X fork).
 *
 * Parental PIN — independent from app passcode. Gates access to
 * parental control settings only; does not lock the whole app.
 */
package org.thunderdog.challegram.minigram;

import android.content.SharedPreferences;

import org.thunderdog.challegram.U;
import org.thunderdog.challegram.unsorted.Settings;

public final class ParentalPin {
  public static final int PIN_SIZE = 4;

  private static final String SALT = "TWluaUdyYW0gcGFyZW50YWwgUElOIHNhbHQ=";

  private static final String KEY_HASH = "mg_par_pin_hash";

  private static ParentalPin instance;

  public static ParentalPin instance () {
    if (instance == null) {
      instance = new ParentalPin();
    }
    return instance;
  }

  private String hash;

  private ParentalPin () {
    hash = Settings.instance().getString(KEY_HASH, null);
  }

  public boolean isSet () {
    return hash != null;
  }

  public static boolean isValidPin (String pin) {
    if (pin == null || pin.length() != PIN_SIZE) {
      return false;
    }
    for (int i = 0; i < PIN_SIZE; i++) {
      if (!Character.isDigit(pin.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public boolean check (String pin) {
    return hash != null && isValidPin(pin) && hash.equals(hashPin(pin));
  }

  public void setPin (String pin) {
    if (!isValidPin(pin)) {
      throw new IllegalArgumentException("Invalid PIN");
    }
    hash = hashPin(pin);
    SharedPreferences.Editor edit = Settings.instance().edit();
    edit.putString(KEY_HASH, hash);
    edit.apply();
  }

  public void clear () {
    hash = null;
    SharedPreferences.Editor edit = Settings.instance().edit();
    edit.remove(KEY_HASH);
    edit.apply();
  }

  private static String hashPin (String pin) {
    return U.sha256(U.sha256(pin + SALT));
  }
}
