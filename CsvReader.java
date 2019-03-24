package com.github.alexswilliams.csvreaderjava;

/*
 * Depends on:
 *  - org.apache.commons:commons-text
 *  - org.slf4j:slf4j-api
 */

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Conforms to https://tools.ietf.org/html/rfc4180
 * Transforms a CSV file into a list of rows, each being a list of field values.
 * No distinction is made between header and record rows.
 *
 * @author Alex Williams
 * @date 25-Nov-2016
 */
public class CsvReader extends BufferedReader {
    @Nonnull
    private static final Pattern QUOTE_MATCHER = Pattern.compile("\\A\"(.*)\"\\z");
    @Nonnull
    private static final Logger logger = LoggerFactory.getLogger(CsvReader.class);

    public CsvReader(@Nonnull final Reader reader) {
        super(reader);
    }

    static class CsvException extends Exception {
        CsvException(final String s) {
            super(s);
        }
    }


    private int lineNum = 1;

    @Nonnull
    public List<List<String>> readFile(final boolean skipBadLines) throws IOException, CsvException {
        final List<List<String>> fullFile = new LinkedList<>();

        List<String> thisLine = null;
        do {
            try {
                thisLine = this.parseLine();
            } catch (CsvException e) {
                if (skipBadLines) {
                    logger.warn("Skipping line {} due to parsing error: {}", lineNum, e.getMessage());
                    continue;
                } else {
                    logger.warn("Stopping at line {} due to parsing error: {}", lineNum, e.getMessage());
                    throw e;
                }
            }
            if (thisLine != null) {
                fullFile.add(thisLine);
            }
        } while (thisLine != null);

        return Collections.unmodifiableList(new ArrayList<>(fullFile));
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
                        lineNum++;
                        result.add(thisField.toString());
                        return startOfLine ? Collections.emptyList() : Collections.unmodifiableList(result);
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
                        lineNum++;
                        result.add(StringEscapeUtils.unescapeCsv(thisField.toString()));
                        return Collections.unmodifiableList(result);
                    }
                    if (nextChar == ',') {
                        stateMachine = CsvStates.START_OF_FIELD;
                        result.add(StringEscapeUtils.unescapeCsv(thisField.toString()));
                        thisField.setLength(0);
                        fieldNum++;
                    } else if (nextChar == '\"') {
                        logger.error("CSV (Line {}, Field {}): Unescaped quotation mark or leading characters "
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
                        logger.error("CSV (Line {}, Field {}): Unterminated quoted field.",
                                lineNum, fieldNum);
                        throw new CsvException("[ERROR] CSV (Line " + lineNum + ", Field " + fieldNum
                                + "): Unterminated quoted field.");
                    }
                    if (nextChar == '\n') {
                        lineNum++;
                        logger.info("CSV (Line {}, Field {}): Quoted field contains newline - was this "
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
                        lineNum++;
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
                        logger.error("CSV (Line {}, Field {}): Unescaped quotation mark or trailing character "
                                + "after quoted field.", lineNum, fieldNum);
                        throw new CsvException("[ERROR] CSV (Line " + lineNum + ", Field " + fieldNum
                                + "): Unescaped quotation mark or trailing character "
                                + "after quoted field.");
                    }
                    break;
            }
        } while (true);
    }

    static String stripEnclosingQuotes(@Nonnull final CharSequence quotedString) {
        return QUOTE_MATCHER.matcher(quotedString).replaceAll("$1");
    }

    private enum CsvStates {
        START_OF_FIELD,
        LEXING_UNQUOTED_FIELD,
        LEXING_QUOTED_FIELD,
        ENDING_QUOTED_FIELD
    }
}
