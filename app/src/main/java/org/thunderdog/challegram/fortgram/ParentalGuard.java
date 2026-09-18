/*
 * This file is a part of FortGram (Telegram-X fork).
 *
 * Helper utilities used at TDLib/UI chokepoints when enforcing ParentalPolicy.
 */
package org.thunderdog.challegram.fortgram;

import androidx.annotation.StringRes;

import org.thunderdog.challegram.R;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.tool.UI;

public final class ParentalGuard {
  private ParentalGuard () {}

  public static void denyToast () {
    denyToast(R.string.ParentalBlocked);
  }

  public static void denyToast (@StringRes int resId) {
    UI.post(() -> UI.showToast(Lang.getString(resId), android.widget.Toast.LENGTH_SHORT));
  }
}
