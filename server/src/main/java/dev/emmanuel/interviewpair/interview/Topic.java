package dev.emmanuel.interviewpair.interview;

/** What a mock interview is about. Only algorithms uses the code editor, the others use a shared notepad. */
public enum Topic {
    ALGORITHMS("Data Structures & Algorithms", "python"),
    BEHAVIORAL("Behavioral", "text"),
    SYSTEM_DESIGN("System Design", "text");

    private final String label;
    private final String language;

    Topic(String label, String language) {
        this.label = label;
        this.language = language;
    }

    public String label() { return label; }

    public String language() { return language; }
}
