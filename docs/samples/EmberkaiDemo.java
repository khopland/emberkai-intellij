import java.time.Duration;
import java.util.List;

/** A small fixture for syntax, comments, selections, and console colors. */
public class EmberkaiDemo {
    private static final int RETRY_LIMIT = 3;

    record Job(String name, boolean ready) {}

    public static void main(String[] args) {
        // Check italics here, then select a matching No Italics scheme.
        var jobs = List.of(new Job("compile", true), new Job("package", false));
        Duration timeout = Duration.ofSeconds(30);

        for (Job job : jobs) {
            String status = job.ready() ? "ready" : "waiting";
            System.out.printf("%-12s %s (timeout: %ss)%n", job.name(), status, timeout.toSeconds());
        }

        printAnsiColors();
        System.err.println("Example diagnostic: retry limit is " + RETRY_LIMIT);
    }

    private static void printAnsiColors() {
        String[] names = {"black", "red", "green", "yellow", "blue", "magenta", "cyan", "gray"};
        for (int i = 0; i < names.length; i++) {
            System.out.printf("\u001b[%dm%-10s\u001b[0m \u001b[%dmbright %s\u001b[0m%n",
                    30 + i, names[i], 90 + i, names[i]);
        }
    }
}
