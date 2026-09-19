# Daily Macros — Privacy Policy

_Last updated: 2026-09-19_

Daily Macros is a nutrition diary. This policy explains what data the app
handles, where it goes, and the choices you have. It describes the app's current
behaviour; if it changes materially, this document will be updated.

## Summary

- Your food diary (meal photos, descriptions, and the nutrition figures derived
  from them) is stored **on your device**.
- To recognise meals and estimate nutrition, the photos and text you add are
  sent to **OpenAI** for analysis. The developer does **not** keep a copy of
  your meal photos or diary entries.
- Optional cloud backup goes to **your own Google Drive**, not to the developer.
- If you turn on **Health Connect sync** (off by default), your logged meals'
  names and nutrition figures are also written to Android Health Connect, where
  other apps you've connected to it (e.g. Samsung Health) can read them.
- The app sends **anonymous** usage and crash telemetry to Google Firebase, tied
  only to a randomly generated identifier — not to your name or email.

## Data stored on your device

The following stays on your device (in the app's private storage) unless you
explicitly export or back it up:

- Meal photos and thumbnails.
- Meal descriptions and the nutrition breakdown (calories, protein, carbs,
  sugar, fat, saturated fat, salt, fibre) produced from them.
- Your nutritional targets and app settings.

Uninstalling the app removes all of this on-device data.

## Data sent to OpenAI (meal analysis)

When you add a meal, the app sends the relevant content to OpenAI's API so it
can recognise the food and estimate nutrition:

- The **photos** you attach to a meal.
- The **title and description** you type.
- Your device language, so results come back in your language.

Please avoid including information in photos or text that you do not want
analysed by a third party. OpenAI's handling of API data is governed by
OpenAI's own policies.

These requests are routed through a small server function operated by the
developer. That function's only role is to attach the app's OpenAI credential
and enforce usage limits — it passes your content straight to OpenAI and does
**not** store your photos, descriptions, or diary. It records only a per-device
counter and a random identifier so usage limits can be enforced and support
requests answered (see "Identifiers" below).

## Optional cloud backup (your Google Drive)

If you enable cloud backup, the app writes a backup of your diary to a private
folder in **your own** Google Drive account. This data is under your control in
your Google account; the developer cannot access it.

## Optional Health Connect sync

If you turn on Health Connect sync in Settings > Connected services (off by
default), the app writes your logged meals — name, timestamp, and nutrition
figures (calories, protein, fat, carbs, sugar, fibre, sodium) — to Android
Health Connect on your device. Meal photos and descriptions are never sent to
Health Connect, only the derived nutrition figures.

Health Connect is a system component that mediates data between apps you've
explicitly connected — the developer has no access to it. Any app you've
granted read access to (for example Samsung Health) can read what's written
there; managing that access is done in the Health Connect app itself, not in
Daily Macros. Turning the sync off stops future writes but does not delete
what was already written — remove it from Health Connect's own app if you want
that.

## Analytics and crash reporting

The app uses Google Firebase Analytics and Crashlytics to understand usage and
diagnose crashes. These send:

- Anonymous usage events and screen views.
- Crash diagnostics (stack traces and technical context).

This telemetry is associated only with a **randomly generated identifier**
created on your device, not with your name, email, or account. No meal photos or
diary content are sent to analytics or crash reporting.

## Identifiers

On first run the app generates a random identifier (a set of three words). It is
used to enforce fair-use limits on meal analysis and as the reference you can
quote to the developer for support. It is not derived from any personal
information. If you uninstall the app, this identifier is discarded and any
server-side usage counter tied to it is no longer associated with any device or
person.

## Permissions

- **Photos / media (`READ_MEDIA_IMAGES`)** — to read the meal photos you add to
  your food diary.
- **Notifications** — to show reminders and status notifications.
- **Health Connect - write nutrition (`health.WRITE_NUTRITION`)** — only
  requested if you turn on Health Connect sync in Settings; lets the app write
  your logged meals' nutrition figures to Health Connect.

## Data retention and deletion

- On-device data: removed when you uninstall the app.
- Cloud backup: remove it from your Google Drive at any time.
- Health Connect sync: turning the toggle off stops new writes; previously
  synced entries stay in Health Connect until removed there.
- Server-side usage counter: not tied to your identity and detaches from any
  device once the app is uninstalled.

## Children

Daily Macros is a general-audience app and is not directed at children.

## Contact

Questions about this policy or your data: nomadworkz@gmail.com.
