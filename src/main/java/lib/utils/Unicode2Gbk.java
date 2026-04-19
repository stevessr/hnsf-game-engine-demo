package lib.utils;

import java.nio.charset.StandardCharsets;

/**
 * 字符编码转换辅助类。
 * 用于在检测到系统环境为 GBK 时，将 Unicode (UTF-8) 编码的字符串进行转换以避免乱码。
 */
public final class Unicode2Gbk {
    private static final String FILE_ENCODING = System.getProperty("file.encoding", "UTF-8");
    private static final boolean IS_GBK = FILE_ENCODING.equalsIgnoreCase("GBK") || FILE_ENCODING.equalsIgnoreCase("GB2312");

    private Unicode2Gbk() {
        // 工具类私有构造函数
    }

    /**
     * 将输入字符串转换为适合当前环境的编码。
     * 如果检测到运行环境为 GBK，则自动尝试进行转换以避免乱码。
     * 
     * @param input 输入字符串
     * @return 转换后的字符串
     */
    public static String convert(String input) {
        if (input == null || !IS_GBK) {
            return input;
        }
        try {
            // 常见的解决乱码技巧：将原本被错误解释为 Unicode 的 UTF-8 字节流按 GBK 重新解码
            return new String(input.getBytes(StandardCharsets.UTF_8), "GBK");
        } catch (Exception e) {
            return input;
        }
    }

    /**
     * 检测当前环境是否为 GBK。
     * 
     * @return 如果是 GBK 环境则返回 true
     */
    public static boolean isGbkEnvironment() {
        return IS_GBK;
    }
}
