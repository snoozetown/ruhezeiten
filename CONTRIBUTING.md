# Contributing to Ruhezeiten

Thanks for considering it. This is a small project; you don't need to file an issue
before sending a small PR.

## Reporting bugs

Open an [Issue](../../issues) with:

- What you did, what you expected, what actually happened.
- Your Kompakt's MuditaOS K version (Settings → About).
- `adb logcat` output around the time of the bug, if you can get it -- most issues in
  an app this small turn out to be visible there.

## Translations

All UI text lives in `app/src/main/res/values/strings.xml`. To add a language:

1. Copy that file to `app/src/main/res/values-<lang-code>/strings.xml` (e.g.
   `values-de` for German).
2. Translate the string *values*, not the `name` attributes.
3. Test on-device or in an emulator set to that locale -- some strings (like the
   save preview) are built from multiple string resources concatenated, so check
   that translated fragments still read naturally together.

No code changes are needed for a translation-only contribution.

## Code contributions

- Match the existing style: no unnecessary comments, prefer MMD's own components
  (`ButtonMMD`, `TextMMD`, `SwitchMMD`, `RadioButtonMMD`, `TimeInputMMD`,
  `TopAppBarMMD`) over bare Material3 equivalents where MMD provides one.
- Accessibility is not optional -- see the Philosophy section in
  [README.md](README.md). If you add an interactive element, give it a single,
  correctly-labeled touch target of at least 48dp, and verify with a real
  accessibility-tree dump (`adb shell uiautomator dump`) rather than just
  eyeballing it -- several real bugs in this app (nested clickables, unlabeled
  controls) only showed up that way, not from visual inspection.
- Test on a real device before opening a PR if at all possible. This app has
  already hit at least one real Compose bug (two `TimeInputMMD` instances racing
  on autofocus) that a simulator/preview wouldn't have found.
- Keep it small. This app deliberately does one thing -- if you're adding
  something that feels like a second feature, consider whether it should be its
  own app instead.

## Building

See [README.md § Building from source](README.md#building-from-source).
