# Timezone Detection Without Log Entries — Implementation Plan

## Problem

The jet-lag advisory/target-scaling feature (`OverviewUiMapper`) only ever sees
a day if it has at least one logged meal — the zone for a day comes entirely
from the zone stamped on its records. A user who doesn't log anything on a
travel day (common — people are more interested in logging steady-state days
than a chaotic flight day) gets no banner and no scaled targets for that day,
even though a genuine timezone shift happened.

Goal: detect a timezone change even on a day with zero log entries, and still
surface the informational banner + scaled targets for it.

## Feasibility

Android broadcasts `Intent.ACTION_TIMEZONE_CHANGED` whenever the system zone
actually changes (OS auto-detection or a manual change). It's one of the
broadcasts exempted from the Android 8+ implicit-broadcast background
restrictions, so a manifest-registered receiver catches it even with the app
process dead — the same mechanism this app already uses for
`BOOT_COMPLETED`/`MY_PACKAGE_REPLACED` in `features/widgets/.../WidgetBootReceiver`.
No new runtime permission required.

Limitation to keep in mind: this only fires if the user has "Automatic time
zone" enabled in Android settings, and it fires whenever the OS decides to
flip — it doesn't make zone detection any more precisely timed than it is
today, it just gives a signal on days with no meal logs instead of no signal
at all.

## Storage: no DB table needed

Checked how backups actually work in this repo before deciding:
- `BackupRepositoryImpl` (the app's own Drive cloud sync) only ever reads/writes
  the Room DB file (`databases/daily_macros_db`) — it doesn't touch
  SharedPreferences at all.
- `app/src/main/res/xml/backup_rules.xml` and `data_extraction_rules.xml`
  already exclude `settings2.xml` (the prefs file `SettingsRepositoryImpl`
  uses for every setting) from Android's own OS-level auto-backup and
  device-transfer.

So storing this data in the existing `settings2` prefs file keeps it out of
every backup path that exists today, with zero new exclusion rules. A Room
table would mean a schema bump (risky — the DB currently uses
`fallbackToDestructiveMigration()`), a new DAO, and re-litigating what the
export/backup code should do with it. None of that is needed: we only ever
need a short rolling list of `(instant, zoneId)` pairs, which fits as a small
JSON blob exactly like `getManuallyAddedMediaStoreIds()` already does for a
`Set<Long>` in the same file.

**Retention**: prune anything older than
`max(getTimezoneAdaptationHours(), 72h) + 1 day`, hard-capped at 14 days
regardless of the configured threshold. This is provably enough — the
adaptation baseline logic (`computeAdaptationBaselines`) never needs to look
back further than the configured threshold, and once a day has a real logged
record again, the existing record-driven zone chain takes back over anyway.

## Plan

### 1. Storage — `SettingsRepository` / `SettingsRepositoryImpl`
- New domain model: `TimezoneEvent(epochMs: Long, zoneId: String)`.
- `getRecentTimezoneEvents(): List<TimezoneEvent>`
- `recordTimezoneEvent(zoneId: String, epochMs: Long)` — appends, then prunes
  per the retention rule above. Skips the append entirely if the new zone
  equals the most recently stored one (the OS can refire redundantly).
- Persisted as Gson JSON in the existing `settings2` prefs, new key.

### 2. Capture — a new receiver
- `TimezoneChangeReceiver : BroadcastReceiver`, `@AndroidEntryPoint` with
  `@Inject lateinit var settingsRepository: SettingsRepository`.
- Listens for `Intent.ACTION_TIMEZONE_CHANGED`.
- Registered in `features/shared/src/main/AndroidManifest.xml` alongside the
  existing `PhotoRecognitionActionReceiver`.
- `onReceive` just calls
  `settingsRepository.recordTimezoneEvent(TimeZone.getDefault().id, System.currentTimeMillis())`.
  Cheap enough that no `goAsync()`/coroutine is needed.

### 3. Feeding it into the overview
- In `OverviewUiMapper.map()`, after grouping real records into `TravelDay`s,
  check the stored `TimezoneEvent`s and synthesize a **phantom `TravelDay`**
  (`records = emptyList()`, synthetic `firstLog`/`lastLog` built from the
  event's zone/instant) for any calendar day that has *both* zero records
  *and* a detected zone-change event landing in it. Merge these into the
  existing chronological day list.
- `computeAdaptationBaselines`, `effectiveTravelDelta`, `buildTimezoneInfo`,
  and target scaling already operate generically on `TravelDay` regardless of
  whether `records` is empty — no changes needed there.
- `computeDailyTotals` (weekly averages/adherence) already skips days with no
  records (`if (r.isEmpty()) return@mapNotNull null`), so phantom days won't
  skew weekly stats — no change needed there either.
- Ordinary no-record days with no detected event stay exactly as invisible as
  they are today. This only adds a card on days where something genuinely
  happened.

### 4. Settings toggle
- New opt-in setting, default **off** — matching the existing
  `getAutoPhotoRecognitionEnabled()` precedent for other passive background
  capture in this app. Surfaced plainly in Settings since it's a standing log
  of the phone's zone over time, even though it isn't backed up anywhere.

### 5. Tests
- `SettingsRepositoryImpl`: round-trip + pruning behavior for
  `recordTimezoneEvent`/`getRecentTimezoneEvents`.
- `OverviewUiMapper`: a zero-record day with a stored `TimezoneEvent` produces
  a `ListUiModelDailySummary` with a banner and no meal rows, and doesn't
  affect weekly adherence.

## Open questions for whoever picks this up
- Exact wording/placement of the opt-in toggle in the Settings screen.
- Whether the phantom day card should visually distinguish itself from a
  normal (fully-logged) day summary card, given it has no meal rows under it.
