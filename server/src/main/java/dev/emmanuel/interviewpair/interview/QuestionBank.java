package dev.emmanuel.interviewpair.interview;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** The curated questions, written for this app. Every topic has at least two per level. */
@Component
public class QuestionBank {

    private final List<Question> questions = List.of(
            // ---------------------------------------------------------------- algorithms, beginner
            new Question(
                    "alg-duplicate-check", Topic.ALGORITHMS, Level.BEGINNER, "Duplicate check",
                    """
                    Given a list of integers, return True if any value appears at least twice, \
                    and False if every value is different.

                    Example: [3, 1, 4, 1] returns True. [3, 1, 4, 2] returns False.""",
                    """
                    def has_duplicate(nums):
                        pass

                    print(has_duplicate([3, 1, 4, 1]))
                    """,
                    List.of(
                            "Ask for the brute force first. What does it cost?",
                            "What data structure answers \"have I seen this before?\" in constant time?",
                            "Can the answer be known before the whole list is scanned?"),
                    """
                    Scan the list once and keep a set of values seen so far. If the current value is \
                    already in the set, return True right away. Time O(n), extra space O(n). \
                    A good alternative is to sort first and compare neighbours: O(n log n) time and \
                    no extra set. Listen for the trade-off between the two, and for the empty list."""),
            new Question(
                    "alg-balanced-brackets", Topic.ALGORITHMS, Level.BEGINNER, "Balanced brackets",
                    """
                    A string contains only the characters ( ) [ ] { }. Return True if every \
                    opening bracket is closed by the same kind of bracket in the right order.

                    Example: "([]{})" returns True. "(]" returns False. "((" returns False.""",
                    """
                    def is_balanced(s):
                        pass

                    print(is_balanced("([]{})"))
                    """,
                    List.of(
                            "What should happen when a closing bracket arrives?",
                            "Which structure gives you the most recent unclosed opener?",
                            "Think about a closer with nothing open, and openers left over at the end."),
                    """
                    Use a stack. Push each opener. For a closer, the stack must be non-empty and its \
                    top must be the matching opener, otherwise return False. At the end the stack must \
                    be empty. A dictionary from closer to opener keeps the code short. Time O(n), \
                    space O(n). Check that the candidate tests the odd-length and empty-stack cases."""),
            new Question(
                    "alg-common-word", Topic.ALGORITHMS, Level.BEGINNER, "Most common word",
                    """
                    Given a sentence, return the word that appears most often. Ignore upper and lower \
                    case and ignore punctuation. If two words tie, return the one that comes first \
                    alphabetically.

                    Example: "The cat and the hat." returns "the".""",
                    """
                    def most_common_word(text):
                        pass

                    print(most_common_word("The cat and the hat."))
                    """,
                    List.of(
                            "Clean the text before counting. What needs to be normalised?",
                            "A dictionary of counts is the core. How do you pick the winner?",
                            "How would you express the tie rule as a sort or min key?"),
                    """
                    Lower-case the text and pull out words with a regular expression or by stripping \
                    punctuation. Count with a dictionary (or collections.Counter). Choose the word \
                    that minimises (-count, word), which handles the tie rule. Time O(n) to count, plus \
                    O(k) to pick the winner over k distinct words. Ask what happens with an empty string."""),

            // ------------------------------------------------------------ algorithms, intermediate
            new Question(
                    "alg-merge-intervals", Topic.ALGORITHMS, Level.INTERMEDIATE, "Merge intervals",
                    """
                    You get a list of intervals [start, end]. Merge every group of intervals that \
                    overlap and return the result sorted by start. Intervals that only touch, like \
                    [1, 2] and [2, 3], count as overlapping.

                    Example: [[1, 3], [8, 10], [2, 6]] returns [[1, 6], [8, 10]].""",
                    """
                    def merge_intervals(intervals):
                        pass

                    print(merge_intervals([[1, 3], [8, 10], [2, 6]]))
                    """,
                    List.of(
                            "Is the input sorted? What changes if you sort it?",
                            "After sorting, which single comparison tells you whether to merge?",
                            "Watch the case where one interval completely contains the next."),
                    """
                    Sort by start. Keep a result list. For each interval, if its start is at most the \
                    end of the last merged interval, extend that end with max(old end, new end); \
                    otherwise append it. Time O(n log n) for the sort, space O(n). Common bug: setting \
                    the end to the new end instead of the maximum, which breaks containment."""),
            new Question(
                    "alg-unique-substring", Topic.ALGORITHMS, Level.INTERMEDIATE, "Longest run without repeats",
                    """
                    Given a string, return the length of the longest substring that has no repeated \
                    characters.

                    Example: "abcabcbb" returns 3 (the substring "abc"). "bbbb" returns 1.""",
                    """
                    def longest_unique(s):
                        pass

                    print(longest_unique("abcabcbb"))
                    """,
                    List.of(
                            "What is the brute force and why is it too slow?",
                            "Can a window of characters grow from the right and shrink from the left?",
                            "When a repeat appears, how far must the left edge move?"),
                    """
                    Sliding window with a dictionary of the last index of each character. Move the right \
                    edge one step at a time. If the character was seen at an index inside the current \
                    window, jump the left edge to one past that index. Track the best window length. \
                    Time O(n), space O(alphabet). The common mistake is moving the left edge backward."""),
            new Question(
                    "alg-count-islands", Topic.ALGORITHMS, Level.INTERMEDIATE, "Count the islands",
                    """
                    A grid contains "1" for land and "0" for water. An island is a group of land cells \
                    connected up, down, left or right. Return how many islands the grid has.

                    Example: ["110", "010", "001"] has 2 islands.""",
                    """
                    def count_islands(grid):
                        pass

                    print(count_islands(["110", "010", "001"]))
                    """,
                    List.of(
                            "Can you see the grid as a graph? What are the nodes and edges?",
                            "How do you avoid counting the same island twice?",
                            "What are the risks of recursion on a large grid?"),
                    """
                    Scan every cell. When you find unvisited land, count one island and flood-fill all of \
                    its connected land with DFS or BFS, marking cells visited (a set, or overwrite a copy \
                    of the grid). Use an explicit stack or queue to avoid hitting the recursion limit. \
                    Time O(rows x cols), space O(rows x cols) in the worst case."""),

            // ---------------------------------------------------------------- algorithms, advanced
            new Question(
                    "alg-lru-cache", Topic.ALGORITHMS, Level.ADVANCED, "Least recently used cache",
                    """
                    Design a cache with a fixed capacity. get(key) returns the value or -1 when the key \
                    is missing. put(key, value) stores a value. When the cache is full, adding a new key \
                    removes the key that was used least recently. Both operations must run in O(1) time. \
                    Reading a key counts as using it.""",
                    """
                    class LRUCache:
                        def __init__(self, capacity):
                            pass

                        def get(self, key):
                            pass

                        def put(self, key, value):
                            pass
                    """,
                    List.of(
                            "A dictionary alone cannot tell you which key is the oldest. What else do you need?",
                            "Which structure lets you remove from the middle and move to the front in O(1)?",
                            "Ask what happens when put is called with a key that already exists."),
                    """
                    A hash map from key to node plus a doubly linked list ordered by recency. get moves \
                    the node to the front. put updates or inserts at the front and, if over capacity, \
                    removes the tail node and its map entry. Sentinel head and tail nodes remove the edge \
                    cases. Python's OrderedDict with move_to_end is a valid shortcut once the candidate \
                    explains how it works. Probe the update-existing-key case."""),
            new Question(
                    "alg-course-order", Topic.ALGORITHMS, Level.ADVANCED, "Course order",
                    """
                    There are n courses numbered 0 to n - 1. Each pair [a, b] means you must finish b \
                    before a. Return one valid order to take all courses, or an empty list if it is \
                    impossible.

                    Example: n = 4, pairs [[1, 0], [2, 0], [3, 1], [3, 2]] can return [0, 1, 2, 3].""",
                    """
                    def course_order(n, prerequisites):
                        pass

                    print(course_order(4, [[1, 0], [2, 0], [3, 1], [3, 2]]))
                    """,
                    List.of(
                            "Draw the pairs as a directed graph. What makes an order impossible?",
                            "Which courses can safely be taken first?",
                            "How can you detect the impossible case without a separate search?"),
                    """
                    Topological sort with Kahn's algorithm. Build adjacency lists and in-degree counts, \
                    start a queue with every in-degree-0 course, pop one, append it to the order, and \
                    decrement its neighbours. If the order ends up shorter than n there is a cycle, so \
                    return []. Time O(n + edges). A DFS with three colours also works."""),
            new Question(
                    "alg-edit-distance", Topic.ALGORITHMS, Level.ADVANCED, "Edit distance",
                    """
                    Given two words, return the smallest number of single-character edits that turn the \
                    first into the second. An edit is an insert, a delete or a replace.

                    Example: "kitten" and "sitting" returns 3.""",
                    """
                    def edit_distance(a, b):
                        pass

                    print(edit_distance("kitten", "sitting"))
                    """,
                    List.of(
                            "Try the last characters of both words. What are your choices?",
                            "What overlaps if you solve this recursively without caching?",
                            "What do the first row and first column of a table mean?"),
                    """
                    Dynamic programming. Let dp[i][j] be the distance between the first i characters of a \
                    and the first j of b. If the characters match, dp[i][j] = dp[i-1][j-1]. Otherwise it \
                    is 1 + min(dp[i-1][j] (delete), dp[i][j-1] (insert), dp[i-1][j-1] (replace)). The \
                    base cases are dp[i][0] = i and dp[0][j] = j. Time and space O(len(a) x len(b)), and \
                    two rows are enough to cut space to O(len(b))."""),

            // ------------------------------------------------------------------ behavioral
            new Question(
                    "beh-about-you", Topic.BEHAVIORAL, Level.BEGINNER, "Tell me about yourself",
                    """
                    Tell me about yourself in about two minutes. Cover who you are, what you have built \
                    or learned, and what you want to do next.""",
                    "",
                    List.of(
                            "Listen for a clear shape: past, present, then future.",
                            "Did they mention one concrete project instead of listing everything?",
                            "Did they end with why this kind of role?"),
                    """
                    A strong answer runs about two minutes, names one or two specific projects with a \
                    real detail or number, and ends by linking the story to the role. Weak answers recite \
                    the resume in order, run past four minutes, or never say what they want next. Give \
                    feedback on clarity and length, not on how impressive the background is."""),
            new Question(
                    "beh-proud-project", Topic.BEHAVIORAL, Level.BEGINNER, "A project you are proud of",
                    """
                    Tell me about a project you are proud of. What was the problem, what did you \
                    personally do, and what was the result?""",
                    "",
                    List.of(
                            "Ask which parts they built themselves, not the team.",
                            "Ask what was hardest and how they got past it.",
                            "Ask how they know the project worked."),
                    """
                    Look for the STAR shape (situation, task, action, result), a clear personal \
                    contribution, one real obstacle, and a measurable or observable result. Push gently \
                    when the answer says "we" for everything or has no outcome."""),
            new Question(
                    "beh-disagreement", Topic.BEHAVIORAL, Level.INTERMEDIATE, "Disagreeing with a teammate",
                    """
                    Describe a time you disagreed with a teammate about how to do something. How did \
                    you handle it, and how did it end?""",
                    "",
                    List.of(
                            "Did they listen to the other view, or only defend theirs?",
                            "Did they use evidence, such as a test or a quick experiment?",
                            "Ask what they would do differently now."),
                    """
                    Strong answers show respect for the other person, a way of settling the question \
                    with facts, and a result that moved the project forward even if they lost the \
                    argument. Red flags are blaming, a story where they were simply right, or no \
                    reflection."""),
            new Question(
                    "beh-failure", Topic.BEHAVIORAL, Level.INTERMEDIATE, "A time something went wrong",
                    """
                    Tell me about a time something you built or planned did not work. What happened, \
                    and what did you do about it?""",
                    "",
                    List.of(
                            "Is it a real failure, not a hidden success story?",
                            "Did they take ownership of their part?",
                            "What changed in how they work afterwards?"),
                    """
                    Look for honesty about what went wrong, ownership, a concrete fix, and a lasting \
                    change in habit. Weak answers pick a fake weakness or end with no lesson."""),
            new Question(
                    "beh-too-much", Topic.BEHAVIORAL, Level.ADVANCED, "Too much to do",
                    """
                    Tell me about a time you had more work than time. How did you decide what to do, \
                    what to drop, and how did you tell the people affected?""",
                    "",
                    List.of(
                            "Did they explain a method for prioritising, not just working harder?",
                            "Did they communicate early to the people affected?",
                            "What was the cost of the trade-off?"),
                    """
                    Strong answers show a clear priority rule, early and honest communication, and an \
                    acknowledged cost. Be wary of answers that say they simply stayed up late and did \
                    everything."""),
            new Question(
                    "beh-unclear", Topic.BEHAVIORAL, Level.ADVANCED, "Deciding with missing information",
                    """
                    Describe a time you had to make a decision without all the information you wanted. \
                    How did you decide, and how did you limit the risk of being wrong?""",
                    "",
                    List.of(
                            "How did they decide which information mattered most?",
                            "Did they make the decision easy to undo?",
                            "How did they find out whether it was right?"),
                    """
                    Look for a clear choice, a stated assumption, a plan to check the assumption early, \
                    and a way to reverse the decision. Weak answers wait for certainty or decide by \
                    gut feeling without any check."""),

            // ---------------------------------------------------------------- system design
            new Question(
                    "sys-url-shortener", Topic.SYSTEM_DESIGN, Level.BEGINNER, "Design a URL shortener",
                    """
                    Design a service that turns a long web address into a short one and redirects \
                    visitors from the short address to the original. Start with requirements, then the \
                    data and the main steps.""",
                    "",
                    List.of(
                            "Did they clarify scale: reads per second, writes per second?",
                            "How are short codes generated, and what stops two long links colliding?",
                            "Ask what they would cache, and why."),
                    """
                    A good design states requirements and rough numbers (reads far outnumber writes), \
                    stores a mapping from a short code to the long URL, generates codes with a counter \
                    encoded in base 62 or random codes with a uniqueness check, and serves redirects \
                    from a cache in front of the database. Extras: expiry, custom aliases, abuse \
                    protection, analytics counts off the hot path."""),
            new Question(
                    "sys-pastebin", Topic.SYSTEM_DESIGN, Level.BEGINNER, "Design a text sharing site",
                    """
                    Design a site where people paste text and get a link to share it. Pastes can \
                    expire. Describe the API, the storage and what happens when a link is opened.""",
                    "",
                    List.of(
                            "What does the API look like for creating and reading a paste?",
                            "Where does the text live, and where does the metadata live?",
                            "How do expired pastes get cleaned up?"),
                    """
                    Expect two endpoints (create, read), small metadata rows in a database and the text \
                    in the same table or in object storage when large, unguessable ids, and a cleanup \
                    job or a lazy check on read for expiry. Extras: size limits, rate limits, a cache \
                    for popular links."""),
            new Question(
                    "sys-chat", Topic.SYSTEM_DESIGN, Level.INTERMEDIATE, "Design one-to-one chat",
                    """
                    Design a chat service where two people exchange messages in real time. Messages \
                    must not be lost and must arrive in order. Cover how a message travels from sender \
                    to receiver and how history is stored.""",
                    "",
                    List.of(
                            "How does the receiver learn about a new message right away?",
                            "What happens when the receiver is offline?",
                            "How do they keep messages in order?"),
                    """
                    Persistent connections (WebSocket) to a chat server, a message store keyed by \
                    conversation with a per-conversation sequence number for ordering, delivery \
                    acknowledgements and retries, a push notification path for offline users, and a way \
                    to route a message to the server that holds the receiver's connection. Extras: \
                    read receipts, group chats, history pagination."""),
            new Question(
                    "sys-feed", Topic.SYSTEM_DESIGN, Level.INTERMEDIATE, "Design a news feed",
                    """
                    Design the home feed of a social app: people follow others and see recent posts \
                    from them. Explain how a feed is built and how it stays fast.""",
                    "",
                    List.of(
                            "Compare building the feed when someone posts and when someone reads.",
                            "What goes wrong for accounts with millions of followers?",
                            "How is the feed ranked, even simply?"),
                    """
                    Discuss fan-out on write (push posts into followers' feed lists) versus fan-out on \
                    read (merge at read time), and a hybrid for very popular accounts. Store feeds in a \
                    cache with a size limit, page with cursors, rank with a simple recency plus \
                    engagement score. Extras: deletes, blocked users, backfill for new follows."""),
            new Question(
                    "sys-doc-editor", Topic.SYSTEM_DESIGN, Level.ADVANCED, "Design a shared document editor",
                    """
                    Design an editor where several people type in the same document at the same time \
                    and all see the same result. Explain how conflicting edits are handled.""",
                    "",
                    List.of(
                            "What happens if two people edit the same spot at once?",
                            "Where does the single source of truth live?",
                            "How does a client that goes offline catch up?"),
                    """
                    Expect operational transformation or a CRDT, explained at a high level, with a \
                    server that orders operations per document, persistent connections for live edits, \
                    periodic snapshots plus an operation log, and catch-up by replaying operations \
                    from a version number. Extras: cursors and presence, permissions, undo."""),
            new Question(
                    "sys-rate-limiter", Topic.SYSTEM_DESIGN, Level.ADVANCED, "Design a shared rate limiter",
                    """
                    Design a rate limiter that many servers share, for example 100 requests per minute \
                    per user across a whole fleet. Explain the algorithm, where the counters live and \
                    what happens when the counter store is slow or down.""",
                    "",
                    List.of(
                            "Which algorithm: fixed window, sliding window or token bucket?",
                            "How do you avoid races when many servers update one counter?",
                            "Should the limiter fail open or closed, and why?"),
                    """
                    A token bucket or sliding window counter kept in a fast shared store such as Redis, \
                    updated atomically (a script or atomic increment with expiry). Discuss local \
                    pre-counting to cut round trips, sharding by user id, and the fail-open versus \
                    fail-closed decision for the store being down. Extras: different limits per plan, \
                    response headers that tell clients when to retry."""));

    private final Map<String, Question> byId =
            questions.stream().collect(Collectors.toUnmodifiableMap(Question::id, Function.identity()));

    public Optional<Question> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Question get(String id) {
        return find(id).orElseThrow(() -> new IllegalStateException("Unknown question " + id));
    }

    public List<Question> all() {
        return questions;
    }

    /** Two different questions for one session, in random order. */
    public List<Question> pickTwo(Topic topic, Level level, RandomGenerator random) {
        List<Question> pool = new ArrayList<>(questions.stream()
                .filter(q -> q.topic() == topic && q.level() == level)
                .toList());
        if (pool.size() < 2) {
            throw new IllegalStateException("Need at least two questions for " + topic + " " + level);
        }
        int first = random.nextInt(pool.size());
        Question one = pool.remove(first);
        Question two = pool.get(random.nextInt(pool.size()));
        return List.of(one, two);
    }
}
