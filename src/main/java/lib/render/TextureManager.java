package lib.render;

import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * 纹理管理器，负责加载和缓存自定义贴图。
 */
public final class TextureManager {
    private static final Map<String, BufferedImage> textureCache = new HashMap<>();

    public static BufferedImage getTexture(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (textureCache.containsKey(path)) {
            return textureCache.get(path);
        }
        try {
            BufferedImage image = ImageIO.read(new File(path));
            textureCache.put(path, image);
            return image;
        } catch (Exception ex) {
            System.err.println("加载纹理失败: " + path + " - " + ex.getMessage());
            return null;
        }
    }

    public static TexturePaint createTexturePaint(String path, int width, int height) {
        BufferedImage image = getTexture(path);
        if (image == null) {
            return null;
        }
        return new TexturePaint(image, new Rectangle2D.Double(0, 0, width, height));
    }
}
