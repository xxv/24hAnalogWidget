# Privacy Policy

**24h Analog** watch face, **24h Analog Sun** companion app, and **24h Analog** home screen widget

Effective September 19, 2026

These apps are made by Steve Pomeroy, an independent developer. This policy explains what
information they use and what happens to it. In short: the only personal information any of them
uses is your approximate location, it's used only to work out sunrise and sunset times, and it
never leaves your device.

## At a glance

| App | Package | Personal data used | Leaves your device? |
| --- | --- | --- | --- |
| 24h Analog (watch face) | `info.staticfree.android.twentyfourhour.wear` | None | — |
| 24h Analog Sun (Wear OS) | `info.staticfree.android.twentyfourhour.sun` | Approximate location | No |
| 24h Analog (widget) | `info.staticfree.android.twentyfourhour` | Approximate location | No |

None of these apps show ads or include analytics, tracking or crash-reporting tools. None of them
ask you to create an account. None of them sell, share or send your information to the developer
or to anyone else. Neither the 24h Analog Sun app nor the widget has permission to use the internet at all.

## 24h Analog watch face

The watch face is built with Wear OS's Watch Face Format. It contains no program code, only a
description of how the face looks, and it requests no permissions. It shows the time and date from
your watch. The accent color you pick in the watch face editor is saved by Wear OS on your watch.

The sunrise and sunset shading on the dial comes from the 24h Analog Sun app, described next. The
watch face gets only the sun times, never your location.

## 24h Analog Sun

This companion app works out when the sun rises and sets, and when each stage of twilight begins
and ends, so the watch face can shade the dial.

**What it uses.** Your approximate location (the "approximate location" permission, accurate to
about a few kilometers). Sun times only depend on roughly where you are, so the app never asks for
your precise location.

**Why it uses it.** Only to calculate sunrise, sunset and twilight times. The calculation runs
entirely on your watch.

**Background access.** If you allow location access "all the time", the app checks your
location when it updates the sun times (at most once every 15 minutes, usually about once an
hour), so the dial stays right when you travel. If you allow it only while the app is in use, it updates your location only when
you open the app.

**Where your location comes from.** Your watch provides it through Android's location services
(Google Play services on most watches). Google handles that part under the
[Google Privacy Policy](https://policies.google.com/privacy).

**What it stores.** The most recent location it received (latitude and longitude) and the time it
last asked for one. These are stored only in the app's private storage on your watch and are
excluded from Android backups, so they aren't copied to your Google account or to a new device. A
new location replaces the old one; no history is kept.

**Who can see the sun times.** The app sends the calculated times, not your location, to the
watch face through Wear OS's complication system. Like any complication, you could choose to show
it on another watch face, which would then get the same sun times. The app's own screen shows your
stored location (rounded to two decimal places) and today's times.

**Your choices.** You can turn off location access at any time in your watch's settings. The app
then stops updating your location but keeps using the last one it stored. To delete the stored
location, clear the app's storage in your watch's settings or uninstall the app; the watch face
then shows no sun shading.

## 24h Analog widget

The home screen widget shows a 24-hour clock on your phone, shaded with today's sunrise, sunset
and twilight times.

**What it uses.** Your approximate location (the "approximate location" permission), only to
calculate those times. The calculation runs entirely on your phone. It doesn't access any other
personal information.

**When it checks your location.** When you open the app, it asks Android for a fresh location. If
you allow location access "all the time", the widget also picks up the location your phone already
has, at most once every 15 minutes; this doesn't turn on any location hardware. If you allow it
only while the app is in use, the location updates only when you open the app.

**Where your location comes from.** Android's location services on your phone. On most phones
these are provided by Google under the [Google Privacy Policy](https://policies.google.com/privacy).

**What it stores.** The most recent location (latitude and longitude) and when it last checked, in
the app's private storage on your phone. It's excluded from Android backups, and a new location
replaces the old one; no history is kept.

**Your choices.** You can turn off location access at any time in your phone's settings; the
widget then keeps using the last location it stored. To delete it, clear the app's storage or
uninstall the app; the widget then shows a striped placeholder instead of the sun shading.

## Children

These apps are not directed at children, and they don't collect personal information from anyone,
including children.

## Open source

All three apps are free software under the GNU General Public License. You can read exactly what
they do in the [source code](https://github.com/xxv/24hAnalogWidget).

## Changes to this policy

If these apps start using information differently, this policy will be updated and the effective
date above will change.

## Contact

Questions about this policy or about your information: Steve Pomeroy,
[steve@staticfree.info](mailto:steve@staticfree.info).
