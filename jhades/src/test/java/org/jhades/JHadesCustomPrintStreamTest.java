package org.jhades;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class JHadesCustomPrintStreamTest {

    @Test
    public void testCustomPrintStream() throws UnsupportedEncodingException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        // No auto-flush to test that jHades does it.
        // UTF-8 so we can decode the bytes (known encoding).
        PrintStream customOutput = new PrintStream(bytes, false, StandardCharsets.UTF_8.name());

        JHades j = new JHades(customOutput);
        j.findClassByName("org.jhades.JHadesCustomPrintStreamTest");

        String[] outputLines = new String(bytes.toByteArray(), StandardCharsets.UTF_8).split("\n");

        String expectedOutputFirstLine =
                ">> jHades printResourcePath >> searching for org/jhades/JHadesCustomPrintStreamTest.class";
        assertEquals("Expected number of lines", 9, outputLines.length);
        assertEquals("Prints heading", expectedOutputFirstLine, outputLines[0]);
    }
}
