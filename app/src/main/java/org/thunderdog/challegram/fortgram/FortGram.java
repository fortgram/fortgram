/*
 * This file is a part of FortGram (Telegram-X fork).
 *
 * Build-wide switches for behaviour that differs from upstream Telegram X.
 */
package org.thunderdog.challegram.fortgram;

public final class FortGram {
  private FortGram () { }

  /**
   * Upstream looks for newer APKs in the Telegram X update channel. FortGram is
   * a separate package with its own signing key, so such an update could never
   * install; keep the in-app updater off to avoid the misleading prompt.
   */
  public static final boolean IN_APP_UPDATES = false;
}
