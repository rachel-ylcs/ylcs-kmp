package love.yinlin.repository;

import java.io.PrintStream;
import java.util.regex.Pattern;

public final class CommonizerWarningFilter extends PrintStream {
    private static final Pattern MISSING_APPLE_LIBRARIES = Pattern.compile(
        "Warning: No libraries found for target (?:macos|ios|tvos|watchos)_[a-z0-9_]+\\. This target will be excluded from commonization\\."
    );

    private CommonizerWarningFilter(PrintStream stdout) {
        super(stdout, true, stdout.charset());
    }

    public static void premain(String arguments) {
        System.setOut(new CommonizerWarningFilter(System.out));
    }

    @Override
    public void println(Object value) {
        println(String.valueOf(value));
    }

    @Override
    public void println(String value) {
        if (value == null || !MISSING_APPLE_LIBRARIES.matcher(value.strip()).matches()) {
            super.println(value);
        }
    }
}
