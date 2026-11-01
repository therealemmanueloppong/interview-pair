# Practice dashboard

The signed-in home screen now offers a useful solo-practice workspace while the availability and partner-matching APIs are still being built.

## What works

- Light and dark themes; the first visit follows the system preference and an explicit choice is remembered.
- Overview with actual recorded sessions, minutes for the current Monday–Sunday week, topics explored, and a configurable goal of 1–7 sessions.
- Six original practice prompts covering arrays/strings, hash maps, trees, graphs, dynamic programming, and system design. Topic filters, bookmarks, and optional thinking hints are included.
- A 25/45/60-minute focus timer with pause, resume, reset, and finish-to-reflection. Countdown uses a wall-clock deadline to recover from background timer throttling.
- A notebook with topic, practiced minutes, self-assessed confidence, and optional reflection. Entries can be removed with undo and exported as JSON.
- Responsive layouts, keyboard focus indicators, a native modal dialog, reduced-motion support, and existing authentication behavior.

## Storage and boundaries

Practice data lives in localStorage under `interviewpair.practice.v1.<userId>`. Theme preference uses `interviewpair.theme`. Different accounts use different notebook keys, but this is not encryption: people with access to the browser profile can inspect its storage. Avoid recording sensitive information in notes.

There is no practice API or cross-device synchronization in this increment. Clearing browser storage removes the notebook. Export provides a readable JSON backup; importing backups is not yet implemented. Multiple simultaneous tabs do not merge edits. Storage errors are surfaced and in-memory practice remains usable. Malformed saved data is rejected rather than rendered.

Dates are displayed in the browser's local time zone. The weekly goal resets its calculation each Monday; historical entries remain. The active timer is not persisted across reloads or logout. Keep the tab open during a session; leaving dashboard sections within the app preserves it.

Partner matching is explicitly labeled as upcoming. No fake partners, interview schedules, ratings, or completion statistics are shown. Authentication and the existing backend/database schema are unchanged.

## Manual review

1. Register or log in, switch themes, reload, and verify the preference remains.
2. Set a weekly goal and explore the prompt library. Save and filter a prompt.
3. Start a timer, navigate to Overview and back, pause, and finish. Check the elapsed minutes in the reflection dialog.
4. Save a reflection, return to Overview, and check that progress updates immediately.
5. Reload and inspect the notebook. Remove an entry, undo, and export notes.
6. Log in as a different account; its notebook should be separate.
7. Check layouts at mobile and desktop widths and operate the dialog using the keyboard.

## Validation

Automated tests cover existing authentication flows, notebook persistence and account separation, malformed storage, local week boundaries, bookmarks and filtering, goal changes, timer pause/background timing, storage failure, theme persistence, and empty states.

Run the web tests, build, and lint using the repository scripts. The backend does not require a migration for this dashboard.
