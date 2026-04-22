package lib.object;

import java.awt.Color;
import java.util.Locale;

public enum ProjectileType {
    STANDARD(
        "普通弹",
        new Color(250, 220, 120),
        1.00,
        1.00,
        false,
        false,
        0,
        1.00,
        0.0,
        0.45,
        3.0,
        0,
        0.0f
    ),
    FLARE(
        "照明弹",
        new Color(255, 198, 96),
        0.82,
        0.55,
        false,
        false,
        0,
        1.00,
        0.0,
        0.45,
        5.5,
        180,
        1.0f
    ),
    BOMB(
        "爆破弹",
        new Color(70, 68, 76),
        0.72,
        0.85,
        true,
        true,
        96,
        1.65,
        1.0,
        0.75,
        4.0,
        0,
        0.0f
    );

    private final String displayName;
    private final Color baseColor;
    private final double speedMultiplier;
    private final double damageMultiplier;
    private final boolean gravityAffected;
    private final boolean explosive;
    private final int explosionRadius;
    private final double explosionDamageMultiplier;
    private final double fuseSeconds;
    private final double explosionDuration;
    private final double lifetimeSeconds;
    private final int lightRadius;
    private final float lightIntensity;

    ProjectileType(
        String displayName,
        Color baseColor,
        double speedMultiplier,
        double damageMultiplier,
        boolean gravityAffected,
        boolean explosive,
        int explosionRadius,
        double explosionDamageMultiplier,
        double fuseSeconds,
        double explosionDuration,
        double lifetimeSeconds,
        int lightRadius,
        float lightIntensity
    ) {
        this.displayName = displayName;
        this.baseColor = baseColor;
        this.speedMultiplier = speedMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.gravityAffected = gravityAffected;
        this.explosive = explosive;
        this.explosionRadius = Math.max(0, explosionRadius);
        this.explosionDamageMultiplier = explosionDamageMultiplier;
        this.fuseSeconds = Math.max(0.1, fuseSeconds);
        this.explosionDuration = Math.max(0.1, explosionDuration);
        this.lifetimeSeconds = Math.max(0.2, lifetimeSeconds);
        this.lightRadius = Math.max(0, lightRadius);
        this.lightIntensity = Math.max(0.0f, Math.min(1.0f, lightIntensity));
    }

    public Color getBaseColor() {
        return baseColor;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public boolean isGravityAffected() {
        return gravityAffected;
    }

    public boolean isExplosive() {
        return explosive;
    }

    public int getExplosionRadius() {
        return explosionRadius;
    }

    public double getExplosionDamageMultiplier() {
        return explosionDamageMultiplier;
    }

    public double getFuseSeconds() {
        return fuseSeconds;
    }

    public double getExplosionDuration() {
        return explosionDuration;
    }

    public double getLifetimeSeconds() {
        return lifetimeSeconds;
    }

    public int getLightRadius() {
        return lightRadius;
    }

    public float getLightIntensity() {
        return lightIntensity;
    }

    public int computeDamage(int attack) {
        return Math.max(1, (int) Math.round(Math.max(0, attack) * damageMultiplier));
    }

    public int computeExplosionDamage(int directDamage) {
        if (!explosive) {
            return Math.max(1, directDamage);
        }
        return Math.max(1, (int) Math.round(Math.max(1, directDamage) * explosionDamageMultiplier));
    }

    public ProjectileType next() {
        ProjectileType[] types = values();
        return types[(ordinal() + 1) % types.length];
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static ProjectileType fromSerialized(String value) {
        if (value == null || value.isBlank()) {
            return STANDARD;
        }
        String normalized = value.trim();
        for (ProjectileType type : values()) {
            if (type.name().equalsIgnoreCase(normalized) || type.displayName.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (lowered.contains("flare") || lowered.contains("light") || lowered.contains("照明") || lowered.contains("信号")) {
            return FLARE;
        }
        if (lowered.contains("bomb") || lowered.contains("爆破") || lowered.contains("炸弹") || lowered.contains("grenade")) {
            return BOMB;
        }
        return STANDARD;
    }
}
