/*
 * This file is a part of FortGram (Telegram-X fork).
 *
 * Draws a single sticker-set cover thumbnail on the trailing edge of a SettingView,
 * used as a preview in the parental allowed-packs editor.
 */
package org.thunderdog.challegram.fortgram;

import android.graphics.Canvas;
import android.view.View;

import androidx.annotation.Nullable;

import org.thunderdog.challegram.component.sticker.TGStickerObj;
import org.thunderdog.challegram.loader.ComplexReceiver;
import org.thunderdog.challegram.loader.ComplexReceiverProvider;
import org.thunderdog.challegram.loader.ImageReceiver;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.util.DrawModifier;

public class StickerCoverModifier implements DrawModifier {
  // Cover is drawn in the leading icon slot; SettingView indents text to 73dp when
  // a row reserves the icon slot, so the preview sits before the name (not over the toggle).
  public static final int SIZE_DP = 32;
  private static final int LEFT_DP = 18;

  private final @Nullable TGStickerObj sticker;

  public StickerCoverModifier (@Nullable TGStickerObj sticker) {
    this.sticker = sticker;
  }

  public void requestFiles (ComplexReceiver complexReceiver) {
    complexReceiver.getImageReceiver(0).requestFile(sticker != null ? sticker.getImage() : null);
  }

  @Override
  public void afterDraw (View view, Canvas c) {
    if (sticker == null) return;
    ComplexReceiver complexReceiver = view instanceof ComplexReceiverProvider ? ((ComplexReceiverProvider) view).getComplexReceiver() : null;
    if (complexReceiver == null) return;
    int size = Screen.dp(SIZE_DP);
    int left = Screen.dp(LEFT_DP);
    int top = view.getMeasuredHeight() / 2 - size / 2;
    ImageReceiver receiver = complexReceiver.getImageReceiver(0);
    receiver.setBounds(left, top, left + size, top + size);
    receiver.draw(c);
  }

  @Override
  public int getWidth () {
    return 0;
  }
}
