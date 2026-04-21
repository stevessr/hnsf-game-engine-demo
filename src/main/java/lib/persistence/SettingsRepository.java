package lib.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONObject;

/**
 * 游戏设置持久化仓库。
 * 将设置保存到本地 JSON 文件。
 */
public final class SettingsRepository {
    private static final String SETTINGS_FILE_NAME = "settings.json";
    private final Path settingsPath;

    public SettingsRepository() {
        this(resolveDefaultPath());
    }

    public SettingsRepository(Path settingsPath) {
        this.settingsPath = settingsPath;
    }

    /**
     * 保存设置到文件。
     *
     * @param targetFPS 目标帧率
     * @param uiFontSize UI 字体大小
     * @param width 分辨率宽
     * @param height 分辨率高
     * @param throttlePower 油门功率
     * @param deceleration 减速度 (0-100)
     * @param gravityEnabled 重力是否开启
     * @param lightingEnabled 光照系统是否开启
     * @param keyBindings 按键映射
     */
    public void saveSettings(
        int targetFPS,
        int uiFontSize,
        int width,
        int height,
        int throttlePower,
        int deceleration,
        boolean gravityEnabled,
        boolean lightingEnabled,
        boolean debugEnabled,
        float ambientLight,
        float lightingIntensity,
        float volume,
        JSONObject keyBindings
    ) {
        JSONObject json = new JSONObject();
        json.put("targetFPS", targetFPS);
        json.put("uiFontSize", uiFontSize);
        json.put("width", width);
        json.put("height", height);
        json.put("throttlePower", throttlePower);
        json.put("deceleration", deceleration);
        json.put("gravityEnabled", gravityEnabled);
        json.put("lightingEnabled", lightingEnabled);
        json.put("debugEnabled", debugEnabled);
        json.put("ambientLight", ambientLight);
        json.put("lightingIntensity", lightingIntensity);
        json.put("volume", volume);
        json.put("keyBindings", keyBindings);

        try {
            Files.createDirectories(settingsPath.getParent());
            Files.writeString(settingsPath, json.toString(4));
        } catch (IOException ex) {
            System.err.println("保存设置失败：" + ex.getMessage());
        }
    }

    /**
     * 从文件加载设置。
     *
     * @return 包含设置的 JSONObject，如果文件不存在或读取失败则返回空对象。
     */
    public JSONObject loadSettings() {
        if (!Files.exists(settingsPath)) {
            return new JSONObject();
        }
        try {
            String content = Files.readString(settingsPath);
            return new JSONObject(content);
        } catch (Exception ex) {
            System.err.println("加载设置失败：" + ex.getMessage());
            return new JSONObject();
        }
    }

    private static Path resolveDefaultPath() {
        String home = System.getProperty("user.home");
        return Paths.get(home, ".hnsfgame", SETTINGS_FILE_NAME);
    }
}
