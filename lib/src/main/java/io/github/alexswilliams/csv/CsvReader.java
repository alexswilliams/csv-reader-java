package io.github.alexswilliams.csv;

import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Conforms to <a href="https://tools.ietf.org/html/rfc4180">...</a>
 * Transforms a CSV file into a sequence of rows, each being a list of field values.
 * No distinction is made between header and record rows.
 *
 * @author Alex Williams
 * @since 25-Nov-2016
 */
public class CsvReader extends BufferedReader {
    private static final Pattern QUOTE_MATCHER = Pattern.compile("\\A\"(.*)\"\\z");
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvReader.class);

    private final AtomicInteger lineNum = new AtomicInteger(1);

    public CsvReader(@NotNull final Reader reader) {
        super(reader);
    }


    @NotNull
    public List<List<String>> readFile(final boolean skipBadLines) throws IOException, CsvException {
        return Collections.unmodifiableList(
                streamFile(skipBadLines).collect(Collectors.toList()));
    }

    class CsvReaderSpliterator extends AbstractCsvReaderSpliterator {
        @Override
        public boolean tryAdvance(Consumer<? super Result<List<String>>> action) {
            try {
                @Nullable List<String> line = parseLine();
                if (line != null) {
                    action.accept(new Result.OK<>(line));
                    return true;
                }
                return false;
            } catch (IOException e) {
                throw new CheckedLaterException(e);
            } catch (CsvException e) {
                action.accept(new Result.Error<>(e, lineNum.get()));
                return true;
            }
        }
    }

    @NotNull
    public Stream<List<String>> streamFile(final boolean skipBadLines) throws IOException, CsvException {
        final Stream<Result<List<String>>> items = StreamSupport.stream(new CsvReaderSpliterator(), false);

        try {
            return items.peek(it -> {
                        if (it instanceof Result.Error) {
                            Result.Error<List<String>> error = (Result.Error<List<String>>) it;
                            if (skipBadLines) {
                                LOGGER.warn("Skipping line {} due to parsing error: {}",
                                        error.line, error.exception.getMessage());
                            } else {
                                LOGGER.warn("Stopping at line {} due to parsing error: {}",
                                        error.line, error.exception.getMessage());
                                throw new CheckedLaterException(error.exception);
                            }
                        }
                    })
                    .filter(it -> it instanceof Result.OK)
                    .map(it -> ((Result.OK<List<String>>) it).data);

        } catch (CheckedLaterException e) {
            if (e.getCause() instanceof CsvException) {
                throw (CsvException) e.getCause();
            }
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new RuntimeException(e.getCause());
        }
    }

    @Nullable
    List<String> parseLine() throws IOException, CsvException {
        final List<String> result = new LinkedList<>();
        CsvStates stateMachine = CsvStates.START_OF_FIELD;

        int fieldNum = 1;
        boolean startOfLine = true;
        final StringBuilder thisField = new StringBuilder();
        do {
            int nextRead = this.read();
            char nextChar = (char) nextRead;
            if (nextChar == '\r') {
                continue;
            }
            while (nextChar == '#') {
                this.readLine();
                nextRead = this.read();
                nextChar = (char) nextRead;
            }
            if ((nextRead == -1) && startOfLine) {
                return null;
            }

            switch (stateMachine) {
                case START_OF_FIELD:
                    if ((nextRead == -1) || (nextChar == '\n')) {
                        lineNum.getAndIncrement();
                        result.add(thisField.toString());
                        return startOfLine ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(result));
                    }
                    startOfLine = false;

                    if (nextChar == ',') {
                        result.add("");
                    } else if (nextChar == '\"') {
                        thisField.append('\"');
                        stateMachine = CsvStates.LEXING_QUOTED_FIELD;
                    } else {
                        thisField.append(nextChar);
                        stateMachine = CsvStates.LEXING_UNQUOTED_FIELD;
                    }
                    break;

                case LEXING_UNQUOTED_FIELD:
                    if ((nextRead == -1) || (nextChar == '\n')) {
                        lineNum.getAndIncrement();
                        result.add(StringEscapeUtils.unescapeCsv(thisField.toString()));
                        return Collections.unmodifiableList(result);
                    }
                    if (nextChar == ',') {
                        stateMachine = CsvStates.START_OF_FIELD;
                        result.add(StringEscapeUtils.unescapeCsv(thisField.toString()));
                        thisField.setLength(0);
                        fieldNum++;
                    } else if (nextChar == '\"') {
                        LOGGER.error("CSV (Line {}, Field {}): Unescaped quotation mark or leading characters "
                                + "before quoted field.\n", lineNum, fieldNum);
                        throw new CsvException("[ERROR] CSV (Line " + lineNum + ", Field " + fieldNum
                                + "): Unescaped quotation mark or leading character "
                                + "before quoted field.");
                    } else {
                        thisField.append(nextChar);
                    }
                    break;

                case LEXING_QUOTED_FIELD:
                    if (nextRead == -1) {
                        LOGGER.error("CSV (Line {}, Field {}): Unterminated quoted field.",
                                lineNum, fieldNum);
                        throw new CsvException("[ERROR] CSV (Line " + lineNum + ", Field " + fieldNum
                                + "): Unterminated quoted field.");
                    }
                    if (nextChar == '\n') {
                        lineNum.getAndIncrement();
                        LOGGER.info("CSV (Line {}, Field {}): Quoted field contains newline - was this "
                                + "intentional?", lineNum, fieldNum);

                    } else if (nextChar == '\"') {
                        thisField.append('\"');
                        stateMachine = CsvStates.ENDING_QUOTED_FIELD;
                    } else {
                        thisField.append(nextChar);
                    }
                    break;

                case ENDING_QUOTED_FIELD:
                    if ((nextRead == -1) || (nextChar == '\n')) {
                        lineNum.getAndIncrement();
                        result.add(stripEnclosingQuotes(StringEscapeUtils.unescapeCsv(thisField.toString())));
                        return Collections.unmodifiableList(result);
                    }
                    if (nextChar == ',') {
                        stateMachine = CsvStates.START_OF_FIELD;
                        result.add(stripEnclosingQuotes(StringEscapeUtils.unescapeCsv(thisField.toString())));
                        thisField.setLength(0);
                        fieldNum++;
                    } else if (nextChar == '\"') {
                        thisField.append('\"');
                        stateMachine = CsvStates.LEXING_QUOTED_FIELD;
                    } else {
                        LOGGER.error("CSV (Line {}, Field {}): Unescaped quotation mark or trailing character "
                                + "after quoted field.", lineNum, fieldNum);
                        throw new CsvException("[ERROR] CSV (Line " + lineNum + ", Field " + fieldNum
                                + "): Unescaped quotation mark or trailing character "
                                + "after quoted field.");
                    }
                    break;
            }
        } while (true);
    }

    static String stripEnclosingQuotes(@NotNull final CharSequence quotedString) {
        return QUOTE_MATCHER.matcher(quotedString).replaceAll("$1");
    }

    private enum CsvStates {
        START_OF_FIELD,
        LEXING_UNQUOTED_FIELD,
        LEXING_QUOTED_FIELD,
        ENDING_QUOTED_FIELD
    }
}
