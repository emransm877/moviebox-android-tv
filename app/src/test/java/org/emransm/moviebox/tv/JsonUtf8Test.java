package org.emransm.moviebox.tv;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JsonUtf8Test {
    @Test public void preservesInternationalMetadataAndOpaqueValuesExactly() throws Exception {
        String json = "{\"title\":\"Dhindora · S2 E1 — Café, São Tomé, Ângela, বাংলা, हिन्दी, العربية, 日本語, 🎬\","
                + "\"id\":\"opaque-Â\",\"url\":\"https://example.invalid/Ã?q=%C3%A9\",\"description\":\"‘Quoted’ and “correct” – already Unicode\"}";
        assertEquals(json, JsonUtf8.decode(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals(json, JsonUtf8.decode(JsonUtf8.decode(json.getBytes(StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8)));
    }

    @Test public void preservesBomAndAsciiRatherThanGuessingAnotherEncoding() throws Exception {
        for (String value : new String[]{"{}", "\uFEFF{\"title\":\"Café\"}", "{\"title\":\"A & B\"}"}) {
            assertEquals(value, JsonUtf8.decode(value.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test public void rejectsMalformedUtf8BeforeJsonAndCacheCanAcceptReplacementCharacters() throws Exception {
        for (byte[] invalid : new byte[][]{{(byte)0xC3, 0x28}, {(byte)0xE2, (byte)0x82}, {(byte)0xC0, (byte)0xAF}, {(byte)0x80}, {(byte)0xE9}}) {
            try {
                JsonUtf8.decode(invalid);
                fail("Malformed UTF-8 must fail at the response boundary");
            } catch (CharacterCodingException expected) {
                // Existing service error/retry handling receives this failure.
            }
        }
    }
}
