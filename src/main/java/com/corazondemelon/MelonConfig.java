package com.corazondemelon;

import net.minecraftforge.common.ForgeConfigSpec;

/** Configuración en config/corazondemelon-common.toml */
public final class MelonConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue XP_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue HEART_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.DoubleValue HEART_CHANCE;
    public static final ForgeConfigSpec.IntValue MAX_HEARTS_NEARBY;
    public static final ForgeConfigSpec.BooleanValue HEARTS_GLOW;
    public static final ForgeConfigSpec.DoubleValue MAO_SPAWN_CHANCE;

    public static final ForgeConfigSpec.BooleanValue AI_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> AI_PROVIDER;
    public static final ForgeConfigSpec.ConfigValue<String> AI_URL;
    public static final ForgeConfigSpec.ConfigValue<String> AI_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> AI_MODEL;
    public static final ForgeConfigSpec.IntValue AI_MAX_TOKENS;
    public static final ForgeConfigSpec.IntValue AI_COOLDOWN;
    public static final ForgeConfigSpec.IntValue AI_TIMEOUT;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("gameplay");
        XP_MULTIPLIER = b.comment("Multiplicador de toda la experiencia de Inocencia (1.0 = normal).")
                .defineInRange("xpMultiplier", 1.0, 0.0, 100.0);
        HEART_INTERVAL_SECONDS = b.comment("Cada cuántos segundos se intenta generar un corazón cerca de cada jugador.")
                .defineInRange("heartSpawnIntervalSeconds", 45, 5, 3600);
        HEART_CHANCE = b.comment("Probabilidad (0-1) de que el intento genere un corazón.")
                .defineInRange("heartSpawnChance", 0.40, 0.0, 1.0);
        MAX_HEARTS_NEARBY = b.comment("Máximo de corazones en el suelo en 80 bloques alrededor del jugador.")
                .defineInRange("maxHeartsNearby", 2, 0, 50);
        HEARTS_GLOW = b.comment("Los corazones del suelo brillan (se ven a través de bloques). Ponlo en false para hacerlo más difícil.")
                .define("heartsGlow", true);
        MAO_SPAWN_CHANCE = b.comment("Probabilidad (0-1) por intento de que aparezca un Mao salvaje cerca de un jugador (de día, en el Overworld).")
                .defineInRange("maoSpawnChance", 0.04, 0.0, 1.0);
        b.pop();

        b.push("ai");
        AI_ENABLED = b.comment("Activa la conversación con Mao mediante IA. Si es false o no hay clave, Mao usa respuestas sencillas sin IA.")
                .define("enabled", true);
        AI_PROVIDER = b.comment("'anthropic' (API de Claude) u 'openai' (cualquier API compatible con OpenAI: OpenAI, Ollama local, LM Studio, OpenRouter...).")
                .define("provider", "anthropic");
        AI_URL = b.comment("URL de la API. Vacío = por defecto (anthropic: https://api.anthropic.com/v1/messages, openai: http://localhost:11434/v1/chat/completions).")
                .define("apiUrl", "");
        AI_KEY = b.comment("Clave de la API. También puede ponerse en la variable de entorno MAO_API_KEY. NO subas este archivo a GitHub con tu clave.")
                .define("apiKey", "");
        AI_MODEL = b.comment("Modelo a usar.")
                .define("model", "claude-haiku-4-5-20251001");
        AI_MAX_TOKENS = b.defineInRange("maxTokens", 300, 50, 2000);
        AI_COOLDOWN = b.comment("Segundos mínimos entre mensajes de un mismo jugador a Mao.")
                .defineInRange("cooldownSeconds", 3, 0, 300);
        AI_TIMEOUT = b.defineInRange("timeoutSeconds", 30, 5, 120);
        b.pop();

        SPEC = b.build();
    }

    private MelonConfig() {}
}
