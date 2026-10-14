/*
 * This file is a part of FortGram (Telegram-X fork).
 *
 * Parental PIN — independent from app passcode. Gates access to
 * parental control settings only; does not lock the whole app.
 *
 * Wrong attempts are rate-limited: after FREE_ATTEMPTS failures every
 * further failure locks the PIN for a growing interval (30 s … 1 h). The
 * lock is measured with the monotonic clock, so moving the wall clock
 * does not shorten it; a reboot restarts the current lock in full.
 */
package org.thunderdog.challegram.fortgram;

import android.content.SharedPreferences;
import android.os.SystemClock;

import org.thunderdog.challegram.U;
import org.thunderdog.challegram.unsorted.AppContext;
import org.thunderdog.challegram.unsorted.Settings;

public final class ParentalPin {
  public static final int PIN_SIZE = 4;

  private static final String SALT = "Rm9ydEdyYW0gcGFyZW50YWwgUElOIHNhbHQ=";

  private static final String KEY_HASH = "fg_par_pin_hash";
  private static final String KEY_FAILS = "fg_par_pin_fails";
  private static final String KEY_LOCK_UNTIL = "fg_par_pin_lock_until";   // SystemClock.elapsedRealtime() at which the lock ends
  private static final String KEY_LOCK_DURATION = "fg_par_pin_lock_ms";   // length of the current lock
  private static final String KEY_LOCK_BOOT = "fg_par_pin_lock_boot";     // Settings.Global.BOOT_COUNT when the lock was set

  /** Failures that cost nothing but a "wrong PIN" message. */
  private static final int FREE_ATTEMPTS = 3;
  /** Lock after the 4th, 5th, 6th … failure; the last value repeats. */
  private static final long[] LOCK_STEPS_MS = {
    30_000L, 60_000L, 5 * 60_000L, 15 * 60_000L, 30 * 60_000L, 60 * 60_000L
  };

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

  /**
   * Verifies the PIN and updates the attempt counter. Always false while
   * locked — callers should consult {@link #lockRemainingMs()} first to
   * tell "locked" from "wrong".
   */
  public boolean check (String pin) {
    if (hash == null) {
      return false;
    }
    if (lockRemainingMs() > 0) {
      return false;
    }
    boolean ok = isValidPin(pin) && hash.equals(hashPin(pin));
    if (ok) {
      resetFailures();
    } else {
      registerFailure();
    }
    return ok;
  }

  public void setPin (String pin) {
    if (!isValidPin(pin)) {
      throw new IllegalArgumentException("Invalid PIN");
    }
    hash = hashPin(pin);
    SharedPreferences.Editor edit = Settings.instance().edit();
    edit.putString(KEY_HASH, hash);
    edit.apply();
    resetFailures();
  }

  public void clear () {
    hash = null;
    SharedPreferences.Editor edit = Settings.instance().edit();
    edit.remove(KEY_HASH);
    edit.apply();
    resetFailures();
  }

  /** Milliseconds until the next attempt is accepted; 0 when not locked. */
  public long lockRemainingMs () {
    Settings s = Settings.instance();
    long duration = s.getLong(KEY_LOCK_DURATION, 0);
    if (duration <= 0) {
      return 0;
    }
    long now = SystemClock.elapsedRealtime();
    long until = s.getLong(KEY_LOCK_UNTIL, 0);
    int boot = bootCount();
    if (boot != s.getInt(KEY_LOCK_BOOT, boot) || until - now > duration) {
      // The device rebooted (monotonic clock restarted) or the stored value is inconsistent:
      // the current lock applies in full from now.
      until = now + duration;
      storeLock(until, duration, boot);
    }
    long remaining = until - now;
    if (remaining <= 0) {
      // Lock expired; the failure count is kept so the next failure locks for longer.
      s.remove(KEY_LOCK_UNTIL);
      s.remove(KEY_LOCK_DURATION);
      s.remove(KEY_LOCK_BOOT);
      return 0;
    }
    return remaining;
  }

  private void registerFailure () {
    Settings s = Settings.instance();
    int fails = s.getInt(KEY_FAILS, 0) + 1;
    s.putInt(KEY_FAILS, fails);
    int step = fails - FREE_ATTEMPTS - 1;
    if (step < 0) {
      return;
    }
    long duration = LOCK_STEPS_MS[Math.min(step, LOCK_STEPS_MS.length - 1)];
    storeLock(SystemClock.elapsedRealtime() + duration, duration, bootCount());
  }

  private void resetFailures () {
    Settings s = Settings.instance();
    s.remove(KEY_FAILS);
    s.remove(KEY_LOCK_UNTIL);
    s.remove(KEY_LOCK_DURATION);
    s.remove(KEY_LOCK_BOOT);
  }

  private static void storeLock (long until, long duration, int boot) {
    Settings s = Settings.instance();
    s.putLong(KEY_LOCK_UNTIL, until);
    s.putLong(KEY_LOCK_DURATION, duration);
    s.putInt(KEY_LOCK_BOOT, boot);
  }

  private static int bootCount () {
    try {
      return android.provider.Settings.Global.getInt(AppContext.get().getContentResolver(), android.provider.Settings.Global.BOOT_COUNT);
    } catch (Throwable t) {
      return 0;
    }
  }

  private static String hashPin (String pin) {
    return U.sha256(U.sha256(pin + SALT));
  }
}
