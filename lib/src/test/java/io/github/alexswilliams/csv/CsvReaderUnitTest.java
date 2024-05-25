package io.github.alexswilliams.csv;

import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

/**
 * @author Alex Williams
 * @since 25-Nov-2016
 */
public class CsvReaderUnitTest {

    private static class Assertion {
        String displayName;
        String input;
        List<List<String>> expectedOutput;

        Assertion(String displayName, String input, List<List<String>> expectedOutput) {
            this.displayName = displayName;
            this.input = input;
            this.expectedOutput = expectedOutput;
        }
    }

    @SafeVarargs
    private final <T> List<T> list(T... elements) {
        return asList(elements);
    }

    @TestFactory
    public Stream<DynamicTest> regressions() {
        return Stream.of(
                new Assertion("A name can have a quoted substring", "\"bob \"\"wonderful\"\" smith\"", list(list("bob \"wonderful\" smith"))),
                new Assertion("A mix of quotes and commas", "0,0,N,\"\"\"AAAAAAAAAA\"\": BB BB BB 'ABCDEFG QWEIR', 1234-5678\"\n",
                        list(list("0", "0", "N", "\"AAAAAAAAAA\": BB BB BB 'ABCDEFG QWEIR', 1234-5678"))),
                new Assertion("Long input", "333333333,xxxxx,xxxxx@yyy,AAAA,XXXXX,XXX,false,false,false,X,W,XXX,N,aaaaaaaaa,11-XXX-XX,11-XXX-XX,1,1,XXX,1,11/11/1111,11/11/1111,11/11/1111,11/11/1111,0,0,N,\"\"\"NNNNNNNNNNNN AMBNDJ\"\": AMAMAMMAMAMA 'QPQPQPQPQP', 1234-5678\"\n",
                        list(list("333333333", "xxxxx", "xxxxx@yyy", "AAAA", "XXXXX", "XXX", "false", "false", "false", "X", "W", "XXX", "N", "aaaaaaaaa", "11-XXX-XX", "11-XXX-XX", "1", "1", "XXX", "1", "11/11/1111", "11/11/1111", "11/11/1111", "11/11/1111", "0", "0", "N", "\"NNNNNNNNNNNN AMBNDJ\": AMAMAMMAMAMA 'QPQPQPQPQP', 1234-5678")))


        ).map(it -> DynamicTest.dynamicTest(it.displayName, () -> {
            final List<List<String>> output;
            try (StringReader stringReader = new StringReader(it.input); CsvReader csvReader = new CsvReader(stringReader)) {
                output = csvReader.readFile(false);
            }
            Assertions.assertIterableEquals(it.expectedOutput, output);
        }));
    }

    @TestFactory
    public Stream<DynamicTest> scenarios() {
        return Stream.of(
                // Single-line Base cases
                new Assertion("Empty string returns []", "", list()),
                new Assertion("Single value returned as-is", "Cell 1", list(list("Cell 1"))),
                new Assertion("Multiple values returned as-is", "Cell 1,Cell 2", list(list("Cell 1", "Cell 2"))),

                // UTF-8
                new Assertion("UTF-8 value is returned as-is", "Юнікод", list(list("Юнікод"))),
                new Assertion("UTF-8 runes containing 0x2c (comma) do not need quoting", "x\u042cx", list(list("xЬx"))),
                new Assertion("UTF-8 runes containing 0x22 (\") do not need quoting", "x\u0422x", list(list("xТx"))),
                new Assertion("UTF-8 runes containing 0x0a (\\n) do not need quoting", "x\u040ax", list(list("xЊx"))),
                new Assertion("UTF-8 runes containing 0x0d (\\r) do not need quoting", "x\u040dx", list(list("xЍx"))),
                new Assertion("UTF-8 rounded quotation marks (“”) do not act as 0x22", "“Cell 1,Cell 2”", list(list("“Cell 1", "Cell 2”"))),

                // Spaces
                new Assertion("No space trimming is applied after comma", "Cell 1, Cell 2", list(list("Cell 1", " Cell 2"))),
                new Assertion("No space trimming is applied before comma", "Cell 1 ,Cell 2", list(list("Cell 1 ", "Cell 2"))),
                new Assertion("No space trimming is applied at start", " Cell 1,Cell 2", list(list(" Cell 1", "Cell 2"))),
                new Assertion("No space trimming is applied at end", "Cell 1,Cell 2 ", list(list("Cell 1", "Cell 2 "))),

                // Blank cells
                new Assertion("Empty cell at start is included in line", ",Cell 2", list(list("", "Cell 2"))),
                new Assertion("Empty cell at end is included in line", "Cell 1,", list(list("Cell 1", ""))),
                new Assertion("Empty cell in middle is included in line", "Cell 1,,Cell 3", list(list("Cell 1", "", "Cell 3"))),

                // Blank lines
                new Assertion("Just '\\n' returns [[]]", "\n", list(list())),
                new Assertion("Just '\\r\\n' returns [[]]", "\r\n", list(list())),
                new Assertion("Blank trailing line is omitted (LF)", "Cell 1\n", list(list("Cell 1"))),
                new Assertion("Blank trailing line is omitted (CRLF)", "Cell 1\r\n", list(list("Cell 1"))),
                new Assertion("Blank leading line is included (LF)", "\nCell 1", list(list(), list("Cell 1"))),
                new Assertion("Blank leading line is included (CRLF)", "\r\nCell 1", list(list(), list("Cell 1"))),
                new Assertion("Blank middle line is an empty list (LF)", "Cell 1\n\nCell 2", list(list("Cell 1"), list(), list("Cell 2"))),
                new Assertion("Blank middle line is an empty list (CRLF)", "Cell 1\r\n\r\nCell 2", list(list("Cell 1"), list(), list("Cell 2"))),

                // Simple Quoting
                new Assertion("Quoted values are unquoted (single)", "\"Cell 1\"", list(list("Cell 1"))),
                new Assertion("Quoted values are unquoted (multiple)", "\"Cell 1\",\"Cell 2\"", list(list("Cell 1", "Cell 2"))),
                new Assertion("Quoted values are unquoted (mixed 1)", "\"Cell 1\",Cell 2", list(list("Cell 1", "Cell 2"))),
                new Assertion("Quoted values are unquoted (mixed 2)", "Cell 1,\"Cell 2\"", list(list("Cell 1", "Cell 2"))),

                // Single-line Comma Quoting
                new Assertion("Values with commas are de-quoted (Alone)", "\"Cell 1A,1B\"", list(list("Cell 1A,1B"))),
                new Assertion("Values with commas are de-quoted (Leading)", "\"Cell 1A,1B\",Cell 2", list(list("Cell 1A,1B", "Cell 2"))),
                new Assertion("Values with commas are de-quoted (Trailing)", "Cell 0,\"Cell 1A,1B\"", list(list("Cell 0", "Cell 1A,1B"))),
                new Assertion("Values with commas are de-quoted (Mixes)", "Cell 0,\"Cell 1A,1B\",Cell 2", list(list("Cell 0", "Cell 1A,1B", "Cell 2"))),
                new Assertion("Values starting with commas are de-quoted", "\",Cell\"", list(list(",Cell"))),
                new Assertion("Values ending with commas are de-quoted", "\"Cell,\"", list(list("Cell,"))),

                // Single-line " Escaping
                new Assertion("Quoted values escaping quotes are unescaped", "\"A \"\" B\"", list(list("A \" B"))),
                new Assertion("Quoted values starting with escaped quotes are unescaped", "\"\"\" B\"", list(list("\" B"))),
                new Assertion("Quoted values ending with escaped quotes are unescaped", "\"A \"\"\"", list(list("A \""))),
                new Assertion("A value can contain quoted text", "\"Bob \"\"The Cheese\"\" Windsor\"", list(list("Bob \"The Cheese\" Windsor"))),
                new Assertion("A value can be just one escaped quote", "\"\"\"\"", list(list("\"")))
                // bug: new Assertion("A value can be just two escaped quotes", "\"\"\"\"\"\"", list(list("\"\""))),
                // bug: new Assertion("Quotes values that wrap their values in quotes are unescaped", "\"\"\"Cell\"\"\"", list(list("\"Cell\""))),

                // Multi-line Escaping
                /* all bugs:
                new Assertion("Values with LF are unescaped", "\"A\nB\"", list(list("A\nB"))),
                new Assertion("Values with CRLF are unescaped", "\"A\r\nB\"", list(list("A\r\nB"))),
                new Assertion("Quoted LF can be the first field", "\"A\nB\",C", list(list("A\nB", "C"))),
                new Assertion("Quoted LF can be the last field", "X,\"A\nB\"", list(list("X", "A\nB"))),
                new Assertion("Quoted LF can be a middle field", "X,\"A\nB\",C", list(list("X", "A\nB", "C"))),
                new Assertion("Quoted CRLF can be the first field", "\"A\r\nB\",C", list(list("A\r\nB", "C"))),
                new Assertion("Quoted CRLF can be the last field", "X,\"A\r\nB\"", list(list("X", "A\r\nB"))),
                new Assertion("Quoted CRLF can be a middle field", "X,\"A\r\nB\",C", list(list("X", "A\r\nB", "C"))),
                new Assertion("LF can start the field", "\"\r\nB\"", list(list("\r\nB"))),
                new Assertion("LF can end the field", "\"A\r\n\"", list(list("A\r\n"))), */

        ).map(it -> DynamicTest.dynamicTest(it.displayName, () -> {
            final List<List<String>> output;
            try (StringReader stringReader = new StringReader(it.input); CsvReader csvReader = new CsvReader(stringReader)) {
                output = csvReader.readFile(false);
            }
            Assertions.assertIterableEquals(it.expectedOutput, output);
        }));
    }

    @Test
    public void testReadFile() throws IOException, CsvException {
        final StringBuilder stringBuilder = initStrings();

        try (final CsvReader csvReader = new CsvReader(new StringReader(stringBuilder.toString()))) {

            final List<List<String>> result = csvReader.readFile(true);

            Assertions.assertNotNull(result, "ListList was null");
            Assertions.assertEquals(result.size(), 19, "Wrong number of elements");
            Assertions.assertEquals(result.get(2), Collections.emptyList(),
                    "'\\n' isn't represented as an empty list.");
            Assertions.assertEquals(result.stream().filter(Objects::isNull).count(), 0,
                    "Some results were null.");
            Assertions.assertEquals(result.stream().mapToLong(Collection::size).sum(), 64,
                    "Wrong number of total fields returned.");
        }
    }

    @NotNull
    private static StringBuilder initStrings() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("1,2,3,4,5\n");
        stringBuilder.append("single-value\n");
        stringBuilder.append('\n');
        stringBuilder.append("юникода\n");
        stringBuilder.append("aꜬbꜢd\uD801\uDF22e∛f\n");
        stringBuilder.append("hanging,\n");
        stringBuilder.append("#\n");
        stringBuilder.append("# comment line, with \" many # odd ,\"'\"\" characters in.\n");
        stringBuilder.append("# comment line, with \" many # odd ,\"'\"\" characters in.\n");
        stringBuilder.append(",leading\n");
        stringBuilder.append(" has spaces , everywhere \n");
        stringBuilder.append(StringEscapeUtils.escapeCsv("tricky'input")).append(",quotes\n");
        stringBuilder.append(StringEscapeUtils.escapeCsv("tricky\"input")).append(",quotes\n");
        stringBuilder.append(StringEscapeUtils.escapeCsv("tricky,input")).append(",quotes\n");
        stringBuilder.append(StringEscapeUtils.escapeCsv("tricky\"\"input")).append(",quotes\n");
        stringBuilder.append(StringEscapeUtils.escapeCsv("tricky\",\"input")).append(",quotes\n");
        stringBuilder.append(StringEscapeUtils.escapeCsv("\"aꜬbꜢd\uD801\uDF22e∛f\""))
                .append(",quotes\n");
        stringBuilder.append("# comment line, with \" many # odd ,\"'\"\" characters in.\n");
        stringBuilder.append("# comment line, with \" many # odd ,\"'\"\" characters in.\n");
        stringBuilder.append("\"\"\"\"\n");
        stringBuilder.append(StringEscapeUtils.escapeCsv("bob \"wonderful\" smith")).append('\n');
        stringBuilder.append("0,0,N,").append(StringEscapeUtils.escapeCsv("\"AAAAAAAAAA\": BB BB BB 'ABCDEFG QWEIR', 1234-5678")).append('\n');
        stringBuilder.append("333333333,xxxxx,xxxxx@yyy,AAAA,XXXXX,XXX,false,false,false,"
                + "X,W,XXX,N,aaaaaaaaa,11-XXX-XX,11-XXX-XX,1,1,XXX,1,11/11/1111,"
                + "11/11/1111,11/11/1111,11/11/1111,0,0,N,\"\"\"NNNNNNNNNNNN "
                + "AMBNDJ\"\": AMAMAMMAMAMA 'QPQPQPQPQP', 1234-5678\"\n");
        stringBuilder.append("\"XXX_XX\",\"Test\",\"P\",\"false\"");
        return stringBuilder;
    }


    @Test
    public void testParseLine() throws IOException, CsvException {
        final StringBuilder stringBuilder = initStrings();

        try (final CsvReader csvReader = new CsvReader(new StringReader(stringBuilder.toString()))) {

            final List<String> line1 = csvReader.parseLine();
            Assertions.assertNotNull(line1, "List was null");
            Assertions.assertEquals(line1.size(), 5, "Wrong number of elements");

            final List<String> line2 = csvReader.parseLine();
            Assertions.assertNotNull(line2, "List was null");
            Assertions.assertEquals(line2.size(), 1, "Wrong number of elements");

            final List<String> line3 = csvReader.parseLine();
            Assertions.assertNotNull(line3, "List was null");
            Assertions.assertEquals(line3.size(), 0, "Wrong number of elements");

            final List<String> line4 = csvReader.parseLine();
            Assertions.assertNotNull(line4, "List was null");
            Assertions.assertEquals(line4.size(), 1, "Wrong number of elements");
            Assertions.assertNotNull(line4.get(0), "String was null");
            Assertions.assertEquals(line4.get(0), "юникода", "Badly Translated");

            final List<String> line5 = csvReader.parseLine();
            Assertions.assertNotNull(line5, "List was null");
            Assertions.assertEquals(line5.size(), 1, "Wrong number of elements");
            Assertions.assertNotNull(line5.get(0), "String was null");
            Assertions.assertEquals(line5.get(0), "aꜬbꜢd\uD801\uDF22e∛f", "Badly Translated");

            final List<String> line6 = csvReader.parseLine();
            Assertions.assertNotNull(line6, "List was null");
            Assertions.assertEquals(line6.size(), 2, "Wrong number of elements");
            Assertions.assertNotNull(line6.get(1), "String was null");
            Assertions.assertEquals(line6.get(1), "", "Empty field didn't yield empty string.");

            final List<String> line7 = csvReader.parseLine();
            Assertions.assertNotNull(line7, "List was null");
            Assertions.assertEquals(line7.size(), 2, "Wrong number of elements");
            Assertions.assertNotNull(line7.get(0), "String was null");
            Assertions.assertEquals(line7.get(0), "", "Empty field didn't yield empty string.");

            final List<String> line8 = csvReader.parseLine();
            Assertions.assertNotNull(line8, "List was null");
            Assertions.assertEquals(line8.size(), 2, "Wrong number of elements");
            Assertions.assertNotNull(line8.get(0), "String was null");
            Assertions.assertEquals(line8.get(0), " has spaces ", "Space padding was mangled.");
            Assertions.assertNotNull(line8.get(1), "String was null");
            Assertions.assertEquals(line8.get(1), " everywhere ", "Space padding was mangled.");

            final List<String> line9 = csvReader.parseLine();
            Assertions.assertNotNull(line9, "List was null");
            Assertions.assertEquals(line9.size(), 2, "Wrong number of elements");
            Assertions.assertNotNull(line9.get(0), "String was null");
            Assertions.assertEquals(line9.get(0), "tricky'input",
                    "Apostrophe (') character was mis-interpreted.");

            final List<String> line10 = csvReader.parseLine();
            Assertions.assertNotNull(line10, "List was null");
            Assertions.assertEquals(line10.size(), 2, "Wrong number of elements");
            Assertions.assertNotNull(line10.get(0), "String was null");
            Assertions.assertEquals(line10.get(0), "tricky\"input",
                    "Interstitial quotation mark (\") was mis-interpreted.");

            final List<String> line11 = csvReader.parseLine();
            Assertions.assertNotNull(line11, "List was null");
            Assertions.assertEquals(line11.size(), 2, "Wrong number of elements");
            Assertions.assertNotNull(line11.get(0), "String was null");
            Assertions.assertEquals(line11.get(0), "tricky,input",
                    "Interstitial comma (,) was mis-interpreted.");

            final List<String> line12 = csvReader.parseLine();
            Assertions.assertNotNull(line12, "List was null");
            Assertions.assertEquals(line12.size(), 2, "Wrong number of elements");
            Assertions.assertNotNull(line12.get(0), "String was null");
            Assertions.assertEquals(line12.get(0), "tricky\"\"input",
                    "Pair of quotation marks (\"\") was mis-interpreted.");

            final List<String> line13 = csvReader.parseLine();
            Assertions.assertNotNull(line13, "List was null");
            Assertions.assertEquals(line13.size(), 2, "Wrong number of elements");
            Assertions.assertNotNull(line13.get(0), "String was null");
            Assertions.assertEquals(line13.get(0), "tricky\",\"input",
                    "Sequence \",\" was mis-interpreted.");

            final List<String> line14 = csvReader.parseLine();
            Assertions.assertNotNull(line14, "List was null");
            Assertions.assertEquals(line14.size(), 2, "Wrong number of elements");
            Assertions.assertNotNull(line14.get(0), "String was null");
            Assertions.assertEquals(line14.get(0), "aꜬbꜢd\uD801\uDF22e∛f",
                    "Quoted unicode characters caused problems.");

            final List<String> line15 = csvReader.parseLine();
            Assertions.assertNotNull(line15, "List was null");
            Assertions.assertEquals(line15.size(), 1, "Wrong number of elements");
            Assertions.assertNotNull(line15.get(0), "String was null");
            Assertions.assertEquals(line15.get(0), "\"",
                    "The raw field \"\"\"\" didn't get unescaped to a single \" character.");

            final List<String> line16 = csvReader.parseLine();
            Assertions.assertNotNull(line16, "List was null");
            Assertions.assertEquals(line16.size(), 1, "Wrong number of elements");
            Assertions.assertNotNull(line16.get(0), "String was null");
            Assertions.assertEquals(line16.get(0), "bob \"wonderful\" smith",
                    "An interstitially quoted string was mis-interpreted.");

            final List<String> line17 = csvReader.parseLine();
            Assertions.assertNotNull(line17, "List was null");
            Assertions.assertEquals(line17.size(), 4, "Wrong number of elements");
            Assertions.assertNotNull(line17.get(0), "String was null");
            Assertions.assertEquals(line17.get(0), "0",
                    "Normal single-character string was not pulled out successfully.");
            Assertions.assertNotNull(line17.get(3), "String with many quotations was null.");
            Assertions.assertEquals(line17.get(3), "\"AAAAAAAAAA\": BB BB BB 'ABCDEFG QWEIR', 1234-5678",
                    "Interstitial quotation marks at the beginning of the string caused an issue.");

            final List<String> line18 = csvReader.parseLine();
            Assertions.assertNotNull(line18, "List was null");
            Assertions.assertEquals(line18.size(), 28, "Wrong number of elements");
            Assertions.assertNotNull(line18.get(0), "String was null");
            Assertions.assertEquals(line18.get(0), "333333333",
                    "Made-up stucode was not pulled out successfully.");
            Assertions.assertNotNull(line18.get(27), "String with many quotations was null.");
            Assertions.assertEquals(line18.get(27), "\"NNNNNNNNNNNN AMBNDJ\": AMAMAMMAMAMA 'QPQPQPQPQP', 1234-5678",
                    "Interstitial quotation marks at the beginning of the string caused an issue.");

            final List<String> line19 = csvReader.parseLine();
            Assertions.assertNotNull(line19, "List was null");
            Assertions.assertEquals(line19.size(), 4, "Wrong number of elements");
            Assertions.assertNotNull(line19.get(0), "String was null");
            Assertions.assertEquals(line19.get(0), "XXX_XX", "Quoted value should have quotes removed.");

            final List<String> line20 = csvReader.parseLine();
            Assertions.assertNull(line20, "Reading past end of file didn't result in null");
        }
    }
}
