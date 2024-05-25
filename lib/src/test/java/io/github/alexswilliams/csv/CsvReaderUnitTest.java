package io.github.alexswilliams.csv;

import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Alex Williams
 * @since 25-Nov-2016
 */
public class CsvReaderUnitTest {

    @Test
    public void testStringUnQuoter() {
        Assertions.assertEquals(CsvReader.stripEnclosingQuotes("\"XXX_PO\""), "XXX_PO",
                "Couldn't unquote string correctly.");
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
        stringBuilder.append("0,0,N,").append(
                StringEscapeUtils.escapeCsv("\"AAAAAAAAAA\": BB BB BB 'ABCDEFG "
                        + "QWEIR', 1234-5678")).append('\n');
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
