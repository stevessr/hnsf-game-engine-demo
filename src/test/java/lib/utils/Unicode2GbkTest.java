package lib.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Unicode2GbkTest {
    @Test
    void convertShouldReturnOriginalTextInCurrentEncodingEnvironment() {
        String input = "测试";

        assertEquals(input, Unicode2Gbk.convert(input));
    }

    @Test
    void isGbkEnvironmentShouldMatchTheCurrentJvmEncoding() {
        boolean isGbk = Unicode2Gbk.isGbkEnvironment();
        String encoding = System.getProperty("file.encoding");
        if ("GBK".equalsIgnoreCase(encoding) || "GB2312".equalsIgnoreCase(encoding)) {
            assertTrue(isGbk);
        } else {
            assertFalse(isGbk);
        }
    }
}
