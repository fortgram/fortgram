/*
 * This file is a part of FortGram (Telegram-X fork).
 *
 * Helper for parental PIN dialogs: setup, unlock, change.
 * Built on top of ViewController.openInputAlert (numeric, 4 digits).
 */
package org.thunderdog.challegram.fortgram;

import android.text.InputFilter;
import android.text.InputType;
import android.widget.Toast;

import org.thunderdog.challegram.R;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.widget.MaterialEditTextGroup;

public final class ParentalPinGate {
  private ParentalPinGate () {}

  public interface OnSuccess {
    void run ();
  }

  /** If PIN is set, prompt to enter it; otherwise prompt to create one. */
  public static void promptEnterOrSetup (ViewController<?> controller, OnSuccess onSuccess) {
    if (ParentalPin.instance().isSet()) {
      promptUnlock(controller, onSuccess);
    } else {
      promptSetup(controller, onSuccess);
    }
  }

  /** First-time PIN setup. Asks twice for confirmation. */
  public static void promptSetup (ViewController<?> controller, OnSuccess onSuccess) {
    MaterialEditTextGroup input = controller.openInputAlert(
      Lang.getString(R.string.ParentalPinSetupTitle),
      Lang.getString(R.string.ParentalPinSetupHint),
      R.string.Done, R.string.Cancel, null,
      (inputView, firstPin) -> {
        if (!ParentalPin.isValidPin(firstPin)) {
          return false;
        }
        promptConfirmNewPin(controller, firstPin, onSuccess);
        return true;
      }, true);
    configureAsPinInput(input);
  }

  /** Verify existing PIN, then run onSuccess. */
  public static void promptUnlock (ViewController<?> controller, OnSuccess onSuccess) {
    MaterialEditTextGroup input = controller.openInputAlert(
      Lang.getString(R.string.ParentalControls),
      Lang.getString(R.string.ParentalPinUnlockHint),
      R.string.OK, R.string.Cancel, null,
      (inputView, pin) -> {
        if (ParentalPin.instance().check(pin)) {
          onSuccess.run();
          return true;
        }
        return false;
      }, true);
    configureAsPinInput(input);
  }

  /** Change PIN from inside parental settings (no need to verify old — already inside). */
  public static void promptChangePin (ViewController<?> controller) {
    MaterialEditTextGroup input = controller.openInputAlert(
      Lang.getString(R.string.ParentalPinChange),
      Lang.getString(R.string.ParentalPinSetupHint),
      R.string.Done, R.string.Cancel, null,
      (inputView, firstPin) -> {
        if (!ParentalPin.isValidPin(firstPin)) {
          return false;
        }
        promptConfirmNewPin(controller, firstPin, null);
        return true;
      }, true);
    configureAsPinInput(input);
  }

  private static void promptConfirmNewPin (ViewController<?> controller, String firstPin, OnSuccess onSuccess) {
    MaterialEditTextGroup input = controller.openInputAlert(
      Lang.getString(R.string.ParentalPinSetupTitle),
      Lang.getString(R.string.ParentalPinConfirmHint),
      R.string.OK, R.string.Cancel, null,
      (inputView, secondPin) -> {
        if (!firstPin.equals(secondPin)) {
          UI.showToast(R.string.ParentalPinMismatch, Toast.LENGTH_SHORT);
          return false;
        }
        ParentalPin.instance().setPin(firstPin);
        if (onSuccess != null) {
          onSuccess.run();
        }
        return true;
      }, true);
    configureAsPinInput(input);
  }

  private static void configureAsPinInput (MaterialEditTextGroup input) {
    if (input == null) return;
    input.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
    input.getEditText().setFilters(new InputFilter[] {new InputFilter.LengthFilter(ParentalPin.PIN_SIZE)});
    input.setMaxLength(ParentalPin.PIN_SIZE);
  }
}
