package lib.object;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

import lib.game.GameWorld;

/**
 * 刷怪笼：在配置间隔内生成指定种类的怪物。
 */
public final class SpawnerObject extends BaseObject {
    private MonsterKind monsterKind;
    private double spawnIntervalSeconds;
    private int maxAlive;
    private int spawnWaveSize;
    private int spawnRadius;
    private int spawnOffsetX;
    private int spawnOffsetY;
    private String spawnGroupId;
    private int spawnCounter;
    private double spawnTimer;

    public SpawnerObject(String name, int x, int y, int width, int height) {
        super(GameObjectType.SPAWNER, name, x, y, width, height, new Color(150, 95, 240, 140), true);
        this.monsterKind = MonsterKind.DEFAULT;
        this.spawnIntervalSeconds = 4.0;
        this.maxAlive = 2;
        this.spawnWaveSize = 1;
        this.spawnRadius = 24;
        this.spawnOffsetX = 0;
        this.spawnOffsetY = 0;
        this.spawnGroupId = generateSpawnGroupId(name);
        this.spawnCounter = 0;
        this.spawnTimer = 0.0;
    }

    public MonsterKind getMonsterKind() {
        return monsterKind;
    }

    public void setMonsterKind(MonsterKind monsterKind) {
        this.monsterKind = monsterKind == null ? MonsterKind.DEFAULT : monsterKind;
    }

    public double getSpawnIntervalSeconds() {
        return spawnIntervalSeconds;
    }

    public void setSpawnIntervalSeconds(double spawnIntervalSeconds) {
        this.spawnIntervalSeconds = Math.max(0.1, spawnIntervalSeconds);
    }

    public int getMaxAlive() {
        return maxAlive;
    }

    public void setMaxAlive(int maxAlive) {
        this.maxAlive = Math.max(1, maxAlive);
    }

    public int getSpawnWaveSize() {
        return spawnWaveSize;
    }

    public void setSpawnWaveSize(int spawnWaveSize) {
        this.spawnWaveSize = Math.max(1, spawnWaveSize);
    }

    public int getSpawnRadius() {
        return spawnRadius;
    }

    public void setSpawnRadius(int spawnRadius) {
        this.spawnRadius = Math.max(0, spawnRadius);
    }

    public int getSpawnOffsetX() {
        return spawnOffsetX;
    }

    public void setSpawnOffsetX(int spawnOffsetX) {
        this.spawnOffsetX = spawnOffsetX;
    }

    public int getSpawnOffsetY() {
        return spawnOffsetY;
    }

    public void setSpawnOffsetY(int spawnOffsetY) {
        this.spawnOffsetY = spawnOffsetY;
    }

    public String getSpawnGroupId() {
        return spawnGroupId;
    }

    public int getSpawnCounter() {
        return spawnCounter;
    }

    public void setSpawnCounter(int spawnCounter) {
        this.spawnCounter = Math.max(0, spawnCounter);
    }

    public void setSpawnGroupId(String spawnGroupId) {
        if (spawnGroupId == null || spawnGroupId.isBlank()) {
            this.spawnGroupId = generateSpawnGroupId(getName());
            return;
        }
        this.spawnGroupId = spawnGroupId.trim();
    }

    public void regenerateSpawnGroupId() {
        this.spawnGroupId = generateSpawnGroupId(getName());
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        if (world == null || !isActive() || deltaSeconds <= 0.0) {
            return;
        }
        spawnTimer += deltaSeconds;
        while (spawnTimer >= spawnIntervalSeconds) {
            spawnTimer -= spawnIntervalSeconds;
            int alive = (int) countAliveSpawnedMonsters(world);
            int spawnCount = Math.min(spawnWaveSize, Math.max(0, maxAlive - alive));
            for (int index = 0; index < spawnCount; index++) {
                spawnMonster(world);
            }
        }
    }

    @Override
    public void render(Graphics2D graphics) {
        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color base = getColor() == null ? new Color(150, 95, 240, 140) : getColor();
            Color fill = new Color(base.getRed(), base.getGreen(), base.getBlue(), isActive() ? 125 : 70);
            Color stroke = new Color(255, 255, 255, isActive() ? 170 : 80);
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();

            g2d.setColor(fill);
            g2d.fillRoundRect(x, y, w, h, 12, 12);

            Stroke oldStroke = g2d.getStroke();
            g2d.setStroke(new BasicStroke(2f));
            g2d.setColor(stroke);
            g2d.drawRoundRect(x, y, w, h, 12, 12);

            int barCount = Math.max(2, Math.min(5, Math.max(2, w / 12)));
            int barSpacing = Math.max(8, w / (barCount + 1));
            for (int i = 1; i <= barCount; i++) {
                int barX = x + i * barSpacing;
                g2d.drawLine(barX, y + 4, barX, y + h - 4);
            }
            g2d.setStroke(oldStroke);

            FontMetrics metrics = g2d.getFontMetrics();
            String label = "SPAWNER";
            int labelWidth = metrics.stringWidth(label);
            g2d.setColor(new Color(255, 240, 255, 230));
            g2d.drawString(label, x + Math.max(6, (w - labelWidth) / 2), y + metrics.getAscent() + 3);

            String kindText = monsterKind == null ? MonsterKind.DEFAULT.toString() : monsterKind.toString();
            int kindWidth = metrics.stringWidth(kindText);
            g2d.setColor(new Color(235, 225, 255, 220));
            g2d.drawString(kindText, x + Math.max(6, (w - kindWidth) / 2), y + h - 8);

            String info = String.format(Locale.ROOT, "%.1fs x%d / %d", spawnIntervalSeconds, spawnWaveSize, maxAlive);
            int infoWidth = metrics.stringWidth(info);
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.drawString(info, x + Math.max(6, (w - infoWidth) / 2), y + h / 2 + 5);

            String radiusText = spawnRadius <= 0
                ? "offset " + spawnOffsetX + "," + spawnOffsetY
                : "offset " + spawnOffsetX + "," + spawnOffsetY + " r" + spawnRadius;
            int radiusWidth = metrics.stringWidth(radiusText);
            g2d.setColor(new Color(235, 225, 255, 210));
            g2d.drawString(radiusText, x + Math.max(6, (w - radiusWidth) / 2), y + 26);
        } finally {
            g2d.dispose();
        }
    }

    private long countAliveSpawnedMonsters(GameWorld world) {
        return world.getObjectsByType(GameObjectType.MONSTER).stream()
            .filter(MonsterObject.class::isInstance)
            .map(MonsterObject.class::cast)
            .filter(MonsterObject::isActive)
            .filter(monster -> spawnGroupId.equals(monster.getMaterial()))
            .count();
    }

    private void spawnMonster(GameWorld world) {
        MonsterObject monster = new MonsterObject(
            getName() + "-spawn-" + (++spawnCounter),
            getX() + getWidth() / 2 - 22,
            getY() + getHeight() - 44,
            60
        );
        monster.setMonsterKind(monsterKind);
        monster.setMaterial(spawnGroupId);
        monster.setActive(true);
        int spawnX = getX() + getWidth() / 2 - monster.getWidth() / 2 + spawnOffsetX;
        int spawnY = getY() + getHeight() / 2 - monster.getHeight() / 2 + spawnOffsetY;
        if (spawnRadius > 0) {
            double angle = ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2.0);
            double distance = ThreadLocalRandom.current().nextDouble(0.0, spawnRadius);
            spawnX += (int) Math.round(Math.cos(angle) * distance);
            spawnY += (int) Math.round(Math.sin(angle) * distance);
        }
        int clampedX = Math.max(0, Math.min(Math.max(0, world.getWidth() - monster.getWidth()), spawnX));
        int clampedY = Math.max(0, Math.min(Math.max(0, world.getHeight() - monster.getHeight()), spawnY));
        monster.setPosition(clampedX, clampedY);
        world.addObject(monster);
    }

    private static String generateSpawnGroupId(String name) {
        String base = normalizeGroupId(name);
        return base + "#" + UUID.randomUUID();
    }

    private static String normalizeGroupId(String value) {
        if (value == null || value.isBlank()) {
            return "spawner-group";
        }
        return value.trim();
    }
}
