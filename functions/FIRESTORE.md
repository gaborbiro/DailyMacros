# What's in Firestore

Everything in the database is written by the Cloud Functions in this folder.
The app and the website never touch Firestore directly (`firestore.rules`
denies all client access). Paths below are `collection/document`; `{x}` means
one document per x.

| Path | Purpose | Written by |
|---|---|---|
| `config/limits` | Your knobs for the app's AI usage | You, by hand |
| `config/demo` | Your knobs for the website demo | You, by hand |
| `meters/app` | App AI calls this month (the monthly budget) | `openaiProxy` |
| `meters/demo` | Website demo calls today (the demo's daily budget) | `demoAnalyze` |
| `meters/demo/visitors/{hash}` | Demo tries per visitor per day | `demoAnalyze` |
| `users/{uid}` | One per app install: AI usage and subscription | `openaiProxy`, subscription functions |
| `clientIds/{clientId}` | Support lookup: three-word client ID → `uid` | `openaiProxy` |
| `purchaseTokens/{token}` | Play purchase token → `uid` | Subscription functions |
| `jobs/checkVoidedPurchases` | Bookmark for the refund checker | `checkVoidedPurchases` |
| `stats/counters` | Website visit and click totals | `trackEvent` |
| `waitlist/{hash}` | Launch waitlist signups | `joinWaitlist` |
| `testers/{hash}` | "Join the test" signups | `joinTest` |

The naming rule: **`config`** is what you set, **`meters`** is what the code
checks against those settings on every request (delete one and its limit
starts counting again from 0), **`stats`** is analytics that nothing reads
(delete it and you only lose numbers), and the rest are records.

Before October 2026 the meters lived at `usage/global`, `usage/demo` and
`demoIps/{hash}`. Nothing reads those any more; delete them if they're still
there.

## Settings you edit

### `config/limits`: the app's AI

Read by `openaiProxy` on every request. Missing fields fall back to the
defaults in `index.js`.

| Field | Default | Meaning |
|---|---|---|
| `perUserDailyCap` | 15 | AI calls per install per UTC day |
| `monthlyRequestBudget` | 3000 | AI calls across all installs per month (see `meters/app`) |
| `killSwitch` | false | `true` stops all app AI calls |
| `unlimitedClientIds` | `[]` | Client IDs that skip the daily cap and the subscription check |
| `preSubTotalCap` | 9 | Free AI attempts before a subscription is needed |
| `preSubSuccessCap` | 6 | Free successful analyses before a subscription is needed |

### `config/demo`: the website demo

Read by `demoAnalyze` on every request. Missing fields fall back to the
defaults in `demo.js`.

| Field | Default | Meaning |
|---|---|---|
| `enabled` | true | `false` turns the demo off |
| `dailyBudget` | 60 | Demo calls across all visitors per UTC day (see `meters/demo`) |
| `perIpDailyCap` | 3 | Demo calls per visitor per UTC day (see `meters/demo/visitors`) |
| `requireAppCheck` | true | Reject requests without a valid App Check token |
| `model` | `gpt-5.4-mini` | OpenAI model |
| `reasoningEffort` | `low` | OpenAI reasoning effort |

## Meters

What the code counts against your `config` limits. Each holds a counter and
the period it belongs to. Nothing resets them on a timer: the first request of
a new period sees a stale `month`/`day`, treats the count as 0 and overwrites
both.

### `meters/app`

`{ month: "2026-10", count }`: app AI calls this month, across every install.
When `count` reaches `config/limits.monthlyRequestBudget`, the app's AI stops
until next month. (Per-install daily counts live on each `users/{uid}`.)

### `meters/demo`

`{ day: "2026-10-22", count }`: website demo calls today, across every
visitor. When `count` reaches `config/demo.dailyBudget`, the demo answers "fully
booked" until tomorrow. Kept apart from `meters/app` so abuse of the demo
can't use up the app's budget.

### `meters/demo/visitors/{hash}`

`{ count, expireAt }`: the same demo calls, counted per visitor for
`config/demo.perIpDailyCap` (the "3 tries a day"). The ID is a hash of the date
and the visitor's IP, so each visitor gets a new document every day and no IP
address is stored. Today's documents add up to roughly `meters/demo`'s count.
Older ones aren't read by anything; the TTL policy on `expireAt` deletes them
after about two days (see the demo setup in `README.md`).

## The app's users and subscriptions

### `users/{uid}`

One per app install (`uid` is the anonymous Firebase sign-in, so reinstalling
gives a new one). Holds:

- **AI usage:** `dailyCapCount` and `dailyCapUtcDate` (today's calls),
  `preSubTotal` and `preSubSuccess` (free tries used), `lastSeen`, `clientId`.
- **Subscription** (written after Play confirms a purchase):
  `subscriptionState`, `subscriptionProductId`,
  `subscriptionExpiryTimeMillis`, `subscriptionPurchaseToken`,
  `subscriptionUpdatedAt`, plus `subscriptionVoidedAt` if it was refunded.

### `clientIds/{clientId}`

`{ uid }`. The three-word ID a user sees in Settings and quotes in a support
email, mapped to their `uid`. A separate collection so it survives even if
`users/{uid}` is deleted.

### `purchaseTokens/{token}`

`{ uid }`. Which install a Play purchase belongs to. Used when Play reports a
renewal, cancellation or refund for a token, and to repair a lost
`users/{uid}`.

### `jobs/checkVoidedPurchases`

`{ catchUpCompletedAt }`. Play doesn't push a notification for refunds, so
`checkVoidedPurchases` asks Play for them every 6 hours and revokes the
matching subscriptions. This document only records that its one-off first run
(which looks back the full 30 days Play allows) has finished, so later runs
only check the recent window. Deleting it is harmless: the next run just does
the 30-day look-back again.

## The website

### `stats/counters`

Running totals, one field per event, nothing per visitor:

| Field | Counts |
|---|---|
| `page_view` | Landing page loads |
| `beta_cta_click` | Clicks on "Join the test" |
| `demo_start` | Visitors who started the demo (first tap on it) |

Any other field in this document is left over from an older version of the
page and can be deleted.

### `waitlist/{hash}` and `testers/{hash}`

One per signup: `email`, `status`, `source`, `createdAt`, and on `testers` a
`platform` (`android` or `ios`). The document ID is a hash of the email, which
is how duplicate signups are spotted. Set an Android tester's `status` to
`added` once they're in Play Console.
