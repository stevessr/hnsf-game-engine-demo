package lib.utils;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Unicode2GbkTest {
    @Test
    void testConvertOnNonGbkEnvironment() {
        // Force non-GBK environment by clearing the property or setting it to UTF-8
        String original = System.getProperty("file.encoding");
        try {
            System.setProperty("file.encoding", "UTF-8");
            // We need to re-initialize or use a way to bypass the static cache if we wanted to test both in one JVM,
            // but for a simple test, we just check if it returns original string when it thinks it's not GBK.
            
            String input = "测试";
            // If the static initializer already ran with a different value, this might be tricky.
            // But since this is a new class, it will run now.
            
            assertEquals(input, Unicode2Gbk.convert(input));
        } finally {
            if (original != null) System.setProperty("file.encoding", original);
        }
    }

    @Test
    void testIsGbkEnvironmentReturnsBoolean() {
        // Just verify it doesn't crash
        boolean isGbk = Unicode2Gbk.isGbkEnvironment();
        String encoding = System.getProperty("file.encoding");
        if ("GBK".equalsIgnoreCase(encoding) || "GB2312".equalsIgnoreCase(encoding)) {
            assertTrue(isGbk);
        } else {
            assertFalse(isGbk);
        }
    }
}
