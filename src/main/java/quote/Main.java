package quote;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Main {
    private static final String USAGE = "Usage: quote --prices PATH --requests PATH";

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 1 && args[0].equals("--help")) {
            out.println(USAGE);
            return 0;
        }
        try {
            Map<String, Path> paths = options(args);
            List<PriceSchedule> prices = Tables.prices(Tables.read(paths.get("--prices"), Tables.PRICE_HEADER));
            List<Tables.Row> requests = Tables.read(paths.get("--requests"), Tables.REQUEST_HEADER);
            Batch batch = new QuoteResolver().resolve(prices, requests);
            out.println(JsonOutput.render(batch));
            return batch.errors().isEmpty() ? 0 : 1;
        } catch (InputException exception) {
            err.println(exception.getMessage());
            return 2;
        } catch (IOException exception) {
            err.println("Cannot read input files: " + exception.getClass().getSimpleName());
            return 2;
        } catch (InvalidPathException exception) {
            err.println("Invalid input path");
            return 2;
        }
    }

    private static Map<String, Path> options(String[] args) throws InputException {
        if (args.length != 4) {
            throw new InputException(USAGE);
        }
        var paths = new HashMap<String, Path>();
        for (int index = 0; index < args.length; index += 2) {
            String option = args[index];
            if ((!option.equals("--prices") && !option.equals("--requests")) || paths.containsKey(option)) {
                throw new InputException(USAGE);
            }
            paths.put(option, Path.of(args[index + 1]));
        }
        return Map.copyOf(paths);
    }
}
