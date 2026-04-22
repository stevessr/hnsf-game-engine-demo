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
        0.0f,
        true,
        false,
        0,
        0.0
    ),
    FLARE(
        "照明弹",
        new Color(255, 198, 96),
        2.20,
        0.35,
        false,
        false,
        0,
        1.00,
        0.0,
        0.45,
        6.5,
        250,
        1.0f,
        true,
        true,
        0,
        0.0
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
        0.0f,
        true,
        false,
        0,
        0.0
    ),
    PIERCE(
        "穿透弹",
        new Color(94, 224, 244),
        1.30,
        0.95,
        false,
        false,
        0,
        1.00,
        0.0,
        0.45,
        3.8,
        0,
        0.0f,
        true,
        false,
        2,
        0.0
    ),
    HOMING(
        "追踪弹",
        new Color(186, 126, 255),
        1.10,
        0.88,
        false,
        false,
        0,
        1.00,
        0.0,
        0.45,
        4.5,
        0,
        0.0f,
        true,
        false,
        0,
        0.18
    ),
    ICE(
        "冰霜弹",
        new Color(162, 232, 255),
        0.92,
        0.75,
        false,
        false,
        0,
        1.00,
        0.0,
        0.45,
        4.0,
        0,
        0.0f,
        true,
        false,
        0,
        0.0
    ),
    LASER(
        "激光弹",
        new Color(92, 244, 255),
        2.55,
        1.10,
        false,
        false,
        0,
        1.00,
        0.0,
        0.35,
        3.6,
        95,
        0.52f,
        true,
        false,
        4,
        0.0
    ),
    SEEKER(
        "追猎弹",
        new Color(186, 104, 255),
        1.18,
        0.92,
        false,
        false,
        0,
        1.00,
        0.0,
        0.45,
        4.2,
        28,
        0.28f,
        true,
        false,
        0,
        0.36
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
    private final boolean canDamageAllies;
    private final boolean revealsExplorationFog;
    private final int pierceTargets;
    private final double homingStrength;

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
        float lightIntensity,
        boolean canDamageAllies,
        boolean revealsExplorationFog,
        int pierceTargets,
        double homingStrength
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
        this.canDamageAllies = canDamageAllies;
        this.revealsExplorationFog = revealsExplorationFog;
        this.pierceTargets = Math.max(0, pierceTargets);
        this.homingStrength = Math.max(0.0, Math.min(1.0, homingStrength));
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

    public boolean canDamageAllies() {
        return canDamageAllies;
    }

    public boolean revealsExplorationFog() {
        return revealsExplorationFog;
    }

    public int getPierceTargets() {
        return pierceTargets;
    }

    public double getHomingStrength() {
        return homingStrength;
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
        if (lowered.contains("pierce") || lowered.contains("penetr") || lowered.contains("穿透") || lowered.contains("贯穿")) {
            return PIERCE;
        }
        if (lowered.contains("homing") || lowered.contains("tracking") || lowered.contains("追踪") || lowered.contains("导弹")) {
            return HOMING;
        }
        if (lowered.contains("ice") || lowered.contains("frost") || lowered.contains("冰霜") || lowered.contains("冰冻")) {
            return ICE;
        }
        if (lowered.contains("laser") || lowered.contains("beam") || lowered.contains("激光") || lowered.contains("光束")) {
            return LASER;
        }
        if (lowered.contains("seeker") || lowered.contains("追猎") || lowered.contains("寻的") || lowered.contains("导向")) {
            return SEEKER;
        }
        if (lowered.contains("bomb") || lowered.contains("爆破") || lowered.contains("炸弹") || lowered.contains("grenade")) {
            return BOMB;
        }
        return STANDARD;
    }
}
