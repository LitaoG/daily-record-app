# Private Calendar Privacy Notice

Effective date: 2026-07-26

Last reviewed: 2026-11-06

This is an English translation of the Chinese [privacy notice](PRIVACY.md). If the two versions differ, the Chinese version prevails.

Private Calendar is an Android calendar journal for the developer and a small group of friends. It keeps two separate private journals of daily counts that the user enters: a solo journal and an intimate journal.

## Data on this device

- Calendar entries, counts and sync status are stored first in a Room database on the device.
- Without an account, recording and statistics run entirely on the device and need no network.
- The app turns off Android system cloud backup, so the system does not copy its database to another device.
- Uninstalling the app normally deletes the data on that device.

## Optional app lock

- The app lock is off by default. The user can turn it on under "Data & privacy" in Settings; the device must already have a screen lock.
- Android itself verifies the user (fingerprint, face or screen lock). The app never reads or stores biometric data or the screen-lock secret. It keeps only two settings on the device, whether the lock is on and when to lock again, and never uploads them.
- The app requests the `USE_BIOMETRIC` permission only to show the system verification prompt; this permission gives no access to biometric data.
- While the lock is on, recent apps do not show the app's content. Below Android 13 the only way to do this also blocks screenshots.
- The app lock only covers the app's screens. It does not encrypt the database on the device; the Android app sandbox still protects that data.

## Optional home screen disguise

- The home screen disguise is off by default. The user can turn it on under "Data & privacy" in Settings. While it is on, the home screen shows the app with the neutral name "Journal" and a plain notebook icon; opening it still opens the same app.
- This choice is stored only on the device and is never uploaded.
- The disguise only changes the home screen icon and name. It does not hide the app in system settings, the list of installed apps or Google Play.

## Optional account and cloud sync

The user can choose a Firebase email-and-password account to restore records on another device. When the user signs in, the app processes:

- the email address the user provides;
- the sign-in credentials and account ID managed by Firebase Authentication;
- the dates, counts, deletion markers (tombstones) and sync revision data of both journals;
- the optional start and end times and short feeling notes (up to 100 characters) the user adds for each occurrence after opening the details section.

The app does not collect partner names or accounts, places, positions, photos, location or medical information, and it does not record duration as a separate field. Per-occurrence times and feelings are saved only when the user fills them in, and every one of them can stay empty.

Firebase Authentication handles passwords; the app never reads or stores a password in plain text. Password reset uses Firebase's own email and hosted page.

Cloud records live under the signed-in user's own Firestore path, and security rules stop every other account from reading or writing them. Clearing a day writes a deletion marker, so an old offline device cannot bring the record back.

When the user signs in, syncs, resets a password or deletes the account, Firebase services also receive the technical data they need to run the service and prevent abuse, such as the IP address, the SDK version and the device platform. This project does not use that data for analytics, advertising or location. Google lists what each Firebase SDK collects in the [Firebase data disclosure](https://firebase.google.com/docs/android/play-data-disclosure).

## Problem reports

The current version includes no analytics or crash-reporting SDK, and the app itself never uploads logs, stack traces, device details or usage behavior. To report a problem, the user can open a GitHub Issue in this repository with the app version, phone model, Android version, network conditions, steps to reproduce and optional screenshots. A report should not include an email address, password, token, the dates and counts of private records, or other personal information.

Per-occurrence time ranges and feelings are private content the user chooses to enter. They are saved to the journal's own Room detail table only after the user opens the details section, and they are written to the journal-specific `details` field of that day's cloud document only while the user is signed in and syncing. They never go into logs, problem reports or statistics. Clearing a whole day also clears that journal's details, and deleting the account covers both detail tables and both cloud collections.

## How data is used and third parties

Data is used only to:

- show the calendar and statistics;
- save the records the user enters;
- provide sync, restore on a new device, password reset and account deletion after the user chooses to sign in.

The project does not sell data, show ads or build social profiles. Cloud features use Google Firebase Authentication, Cloud Firestore and Cloud Functions for Firebase (server-side write checks and account data deletion). Network requests and Firebase's processing are governed by Firebase's terms of service and privacy policy.

## Retention and deletion

- The user can clear the record of a single day; clearing syncs a deletion marker.
- The account dialog offers to delete the account and its cloud data. This needs a second confirmation and the current password.
- Deletion first removes the user's own records of both journals from Firestore, then deletes the Firebase Authentication account.
- Records on the device are kept as offline records by default; the user can explicitly choose to delete them as well.
- If the network or a permission fails, the app never reports the deletion as successful; the records on the device stay and the user can retry.
- If the app is uninstalled or device access is lost, users can request permanent deletion of their account and all cloud data by following the [Account and Data Deletion Guide](docs/ACCOUNT_DELETION.md).

## Network limits

Firebase may be unreachable on ordinary networks in mainland China. The app keeps working fully offline; signing in, password reset, sync and cloud deletion may need a network or VPN that can reach Firebase.

## Contact

Report problems through a GitHub Issue in this repository. Describe the steps to reproduce, the expected result and the actual result, and do not post an email address, password, token, the dates and counts of private records, or other personal information.
