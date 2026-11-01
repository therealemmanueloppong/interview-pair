# Mock interviews

Two students are paired for a live mock interview. One answers a question in a shared editor while the
other interviews and watches the code appear. Halfway through they switch roles and get a new question.

## How a session works

1. **Schedule.** A student picks a topic (algorithms, behavioral or system design), a level and a
   two hour time slot. Slots sit on the UTC clock, so everyone sees the same instants whatever their
   time zone. The page shows them in local time.
2. **Match.** The server looks for a waiting request with the same topic, level and slot. If there is
   one, both students get a session. If not, the request waits. Matching reuses `MatchingService`,
   which already required at least 45 minutes of overlap, and a session is planned as one hour inside
   its slot. Whoever asked first answers first.
3. **Round one.** The person answering sees the question and edits the code. The interviewer sees the
   same code live but cannot change it, plus a guide with hints and a model answer.
4. **Switch roles.** Either person can switch. The roles swap, the second question loads with fresh
   starter code, and the guide moves to the other person.
5. **Finish and feedback.** Either person can finish. The editor locks, both people can now read the
   hints and solution, and each can leave a 1 to 5 rating and a comment. You only see what your
   partner wrote about you.

Behavioral and system design sessions work the same way, but the editor is a shared notepad.

## Why the editor has one writer

Only the person answering can type. That is how an interview works, and it removes a hard problem: with
a single writer there are no conflicting edits, so the server just stores the latest text and passes it
on. No operational transformation or CRDT is needed. The cost is that the interviewer cannot type into
the answer. If pair editing is ever wanted, this is the part to replace.

## What is secret, and how the server keeps it secret

The hints and model answer would spoil the question for the person answering, so the **server never
sends them to that person**. `InterviewService.view` removes them from the response, whether the data
goes out over REST or over the WebSocket. Hiding a tab in the browser would not be enough, because the
data would still be in the network traffic. After the session is finished both people can see them.

A session that is not yours looks the same as one that does not exist (404), so ids cannot be probed.

## Real time: the WebSocket

The browser connects to `/ws/sessions`. Messages are JSON.

| Direction | Message | Meaning |
| --- | --- | --- |
| to server | `{"type":"code","text":"..."}` | The full text of the editor. Accepted only from the person answering. |
| to browser | `{"type":"state","session":{...}}` | The session as this person may see it. Sent on connect, after a role switch and after a refused edit. |
| to browser | `{"type":"code","text":"..."}` | The other person's latest text. |
| to browser | `{"type":"presence","online":[ids]}` | Who is connected. |
| to browser | `{"type":"error","message":"..."}` | An edit was refused. |

The browser sends at most one `code` message per 200 ms pause in typing. It reconnects with a short
backoff after a dropped connection, and fetches the saved state again when it does.

### Authenticating a WebSocket

Browsers cannot attach an `Authorization` header to a WebSocket, and putting the login token in the URL
would copy it into server and proxy logs. So the app uses **one-time tickets**:

1. A logged-in student calls `POST /api/sessions/{id}/ws-ticket`. The server checks they belong to the
   session and returns a random 256-bit ticket.
2. The browser opens `/ws/sessions?ticket=...`.
3. The handshake redeems the ticket. It works once, only for that person and session, and expires after
   30 seconds. A reused, expired or made-up ticket gets a 401.

Other protections: only same-site pages may open the socket (origin check), text messages are limited
to 128 KB and code to 20,000 characters, and a second tab for the same person closes the first with
code 4001 so the two do not fight over the connection.

## Tests

* `QuestionBankTest` checks every topic and level has enough questions with hints and a solution.
* `WsTicketServiceTest` covers single use, expiry and randomness.
* `InterviewServiceTest` covers matching, slot rules, who can edit, role switching, secrecy and feedback.
* `InterviewApiTest` covers login requirements, validation and what each person receives as JSON.
* `SessionSocketIntegrationTest` opens two real WebSocket clients against a running server and checks
  live code, refused edits, role switches, ticket reuse, second tabs and a long message.
* The web tests use a fake WebSocket to check the room from both roles, reconnects and the feedback form.

## Known limits and next steps

* **No code execution.** "Run code" and "Run tests" are not built. Running other people's code safely
  needs a sandbox. A free option to try is running Python in the browser with Pyodide.
* **One server instance.** Matching runs one request at a time inside a single process, and open
  sockets live in that process's memory. Running several instances would need a database lock for
  matching and a shared message channel such as Redis.
* **No rate limit on messages** beyond the size limits. Add one before opening to the public.
* **Round one's code is not kept** after the roles switch, and finished sessions show only the second
  question. Storing both rounds is a small change.
* **Left for later.** Inviting a friend by link, reminders before a session, ratings feeding into
  matching, and an AI practice partner (which would cost money to run, so it is not started).
