# PrimeNotify

Your phone already tells you when something arrives. PrimeNotify decides *how* — flash the camera LED, light up the whole screen, wake the display, or trigger always-on display. One rule, several actions, only for the notifications you actually care about.

## What a rule looks like

Pick the apps. Optionally narrow it with keywords (all text, or just the title, or just the body). Then choose what happens:

- **Flash** — camera LED, with a built-in pattern like heartbeat or one you draw yourself
- **Screen flash** — full-screen color, works over the lock screen, until it times out or you tap it
- **Wake up** — turn the screen on for a few seconds, with pocket mode so it stays dark in your pocket
- **AOD** — always-on display, optionally until the notification is dismissed or the phone is unlocked

A rule can also be told to only fire on vibrate, silent, or Do Not Disturb, and to stay quiet when the same app keeps buzzing.

There's also an ignore list for notifications you'd rather never see again, and a log so you can check what matched and what didn't.

## Building it

```bash
./gradlew assembleDebug
```

Kotlin, Jetpack Compose, Material 3. minSdk 24, targetSdk 36.

## First run

Open the app and work through the Permissions screen — notification access is the important one, plus camera for the LED, overlay for the screen flash, and a battery optimization exemption so Android doesn't put the listener to sleep.

Always-on display needs a permission the system won't hand out from inside the app, so grant it over adb once:

```bash
adb shell pm grant com.arslan.primenotify android.permission.WRITE_SECURE_SETTINGS
```

Then add your first rule from the Rules screen.
