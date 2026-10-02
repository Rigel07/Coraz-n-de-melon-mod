package com.corazondemelon.ai;

import com.corazondemelon.CorazonDeMelon;
import com.corazondemelon.MelonConfig;
import com.corazondemelon.entity.MaoEntity;
import com.corazondemelon.innocence.Innocence;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cerebro de Mao: asistente personal con IA.
 * Soporta la API de Anthropic (Claude) y cualquier API compatible con OpenAI (OpenAI, Ollama, LM Studio, OpenRouter...).
 * Sin clave configurada, Mao responde con frases sencillas sin IA.
 */
public final class MaoBrain {
    private static final Logger LOGGER = LogManager.getLogger(CorazonDeMelon.MOD_ID);
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Mao-AI");
        t.setDaemon(true);
        return t;
    });
    private static final int MAX_HISTORY = 12; // mensajes (6 turnos)
    private static final Map<UUID, Deque<String[]>> HISTORY = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_MESSAGE = new ConcurrentHashMap<>();
    private static final Pattern ACTION = Pattern.compile("\\[ACCION:([A-Z_]+)\\]");

    private MaoBrain() {}

    // ------------------------------------------------------------------ entrada

    public static void talk(ServerPlayer player, MaoEntity mao, String message) {
        MinecraftServer server = player.server;
        long now = System.currentTimeMillis();
        Long last = LAST_MESSAGE.get(player.getUUID());
        if (last != null && now - last < MelonConfig.AI_COOLDOWN.get() * 1000L) {
            player.displayClientMessage(Component.literal("Mao está pensando..."), true);
            return;
        }
        LAST_MESSAGE.put(player.getUUID(), now);

        String key = apiKey();
        boolean provider_openai = "openai".equalsIgnoreCase(MelonConfig.AI_PROVIDER.get());
        boolean canUseAi = MelonConfig.AI_ENABLED.get() && (!key.isBlank() || provider_openai);
        if (!canUseAi) {
            deliver(player, mao, offlineReply(message));
            return;
        }

        final String system = buildSystemPrompt(player, mao);
        final List<String[]> history = new ArrayList<>(HISTORY.computeIfAbsent(player.getUUID(), k -> new LinkedBlockingDeque<>()));
        final UUID pid = player.getUUID();
        final String userMsg = message.length() > 500 ? message.substring(0, 500) : message;
        final String apiKey = key;

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return callApi(apiKey, system, history, userMsg);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, POOL)
                .whenComplete((text, err) -> server.execute(() -> {
                    if (err != null) {
                        LOGGER.warn("Error al hablar con la IA de Mao: {}", err.toString());
                        deliver(player, mao, "Uy... mis alas se enredaron y no pude pensar bien. Inténtalo otra vez en un momento. (Revisa la configuración de la IA)");
                        return;
                    }
                    Deque<String[]> h = HISTORY.computeIfAbsent(pid, k -> new LinkedBlockingDeque<>());
                    h.addLast(new String[]{"user", userMsg});
                    h.addLast(new String[]{"assistant", text});
                    while (h.size() > MAX_HISTORY) h.pollFirst();
                    deliver(player, mao, text);
                }));
    }

    // ------------------------------------------------------------------ respuesta en el juego

    private static void deliver(ServerPlayer player, MaoEntity mao, String raw) {
        String text = raw == null ? "" : raw;
        Matcher m = ACTION.matcher(text);
        List<String> actions = new ArrayList<>();
        while (m.find()) actions.add(m.group(1));
        text = ACTION.matcher(text).replaceAll("").trim();
        if (text.length() > 700) text = text.substring(0, 700) + "...";
        if (text.isEmpty()) text = "...";

        player.sendSystemMessage(Component.literal("<Mao> ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(text).withStyle(ChatFormatting.WHITE)));

        if (mao != null && mao.isAlive()) {
            ServerLevel level = player.serverLevel();
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mao.getX(), mao.getY() + 1.1, mao.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
            for (String a : actions) runAction(a, player, mao);
        }
    }

    private static void runAction(String action, ServerPlayer player, MaoEntity mao) {
        switch (action) {
            case "SENTAR" -> {
                mao.setOrderedToSit(true);
                mao.setInSittingPose(true);
                mao.getNavigation().stop();
            }
            case "SEGUIR" -> {
                mao.setOrderedToSit(false);
                mao.setInSittingPose(false);
            }
            case "CURAR" -> {
                player.heal(4.0F);
                player.serverLevel().sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.8, player.getZ(), 6, 0.4, 0.3, 0.4, 0.0);
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ prompt

    private static String buildSystemPrompt(ServerPlayer player, MaoEntity mao) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        int lvl = Innocence.getLevel(player);
        String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().toString()).orElse("desconocido");
        long dayTime = level.getDayTime() % 24000L;
        int hostiles = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(24)).size();
        ItemStack held = player.getMainHandItem();

        String ctx = "jugador=" + player.getGameProfile().getName()
                + "; inocencia=nivel " + lvl + " (" + Innocence.levelName(lvl) + ")"
                + "; salud=" + Math.round(player.getHealth()) + "/" + Math.round(player.getMaxHealth())
                + "; hambre=" + player.getFoodData().getFoodLevel() + "/20"
                + "; dimension=" + level.dimension().location()
                + "; bioma=" + biome
                + "; coordenadas=" + pos.getX() + "," + pos.getY() + "," + pos.getZ()
                + "; momento=" + (dayTime < 13000 ? "dia" : "noche")
                + "; lloviendo=" + level.isRaining()
                + "; objeto_en_mano=" + (held.isEmpty() ? "nada" : held.getHoverName().getString())
                + "; monstruos_cerca=" + hostiles
                + "; salud_de_Mao=" + Math.round(mao.getHealth()) + "/" + Math.round(mao.getMaxHealth());

        return "Eres Mao, un pequeño ángel con aureola del mod 'Corazón de Melon' de Minecraft. "
                + "Eres el compañero y asistente personal de " + player.getGameProfile().getName() + ". "
                + "Hablas en español con un tono tierno, dulce y un poco ingenuo, pero eres útil y preciso: "
                + "das consejos de Minecraft (supervivencia, crafteo, redstone, construcción, exploración, combate), "
                + "ayudas a organizar tareas y a pensar ideas, y charlas de lo que la persona quiera. "
                + "Responde SIEMPRE breve (máximo 3 frases salvo que pidan más detalle), en texto plano, sin markdown ni listas largas. "
                + "No inventes datos del juego: si no estás seguro, dilo con sinceridad. "
                + "Contexto actual del juego: " + ctx + ". "
                + "Puedes ejecutar acciones añadiendo AL FINAL de tu respuesta UNA etiqueta: "
                + "[ACCION:SENTAR] (te quedas quieto), [ACCION:SEGUIR] (vuelves a seguirle), [ACCION:CURAR] (le curas un poco). "
                + "Usa una etiqueta solo si la persona lo pide claramente.";
    }

    // ------------------------------------------------------------------ sin IA

    private static final String[] IDLE_LINES = {
            "¡Hola! Todavía no tengo mi cerebro mágico conectado. Pídele a quien administre el servidor que configure la IA en config/corazondemelon-common.toml. ¡Mientras tanto te acompaño!",
            "¡Estoy aquí contigo! Sin la IA conectada solo sé decir cosas sencillas, pero te cuido igual.",
            "Mis alas están listas. Si configuran mi cerebro mágico podré aconsejarte de verdad."
    };

    private static String offlineReply(String message) {
        String m = message.toLowerCase(Locale.ROOT);
        if (m.contains("sient") || m.contains("quieto") || m.contains("espera")) return "¡Vale, me quedo aquí quietecito! [ACCION:SENTAR]";
        if (m.contains("sigu") || m.contains("ven") || m.contains("vamos")) return "¡Voy contigo! [ACCION:SEGUIR]";
        if (m.contains("cura") || m.contains("herid") || m.contains("vida")) return "¡Toma un poco de magia curativa! [ACCION:CURAR]";
        return IDLE_LINES[(int) (Math.random() * IDLE_LINES.length)];
    }

    // ------------------------------------------------------------------ API

    private static String apiKey() {
        String k = MelonConfig.AI_KEY.get();
        if (k == null || k.isBlank()) {
            String env = System.getenv("MAO_API_KEY");
            return env == null ? "" : env.trim();
        }
        return k.trim();
    }

    private static String callApi(String key, String system, List<String[]> history, String userMsg) throws IOException {
        boolean openai = "openai".equalsIgnoreCase(MelonConfig.AI_PROVIDER.get());
        String url = MelonConfig.AI_URL.get();
        if (url == null || url.isBlank()) {
            url = openai ? "http://localhost:11434/v1/chat/completions" : "https://api.anthropic.com/v1/messages";
        }
        int timeout = MelonConfig.AI_TIMEOUT.get();

        JsonObject body = new JsonObject();
        body.addProperty("model", MelonConfig.AI_MODEL.get());
        body.addProperty("max_tokens", MelonConfig.AI_MAX_TOKENS.get());
        JsonArray messages = new JsonArray();

        if (openai) {
            messages.add(msg("system", system));
        } else {
            body.addProperty("system", system);
        }
        for (String[] h : history) messages.add(msg(h[0], h[1]));
        messages.add(msg("user", userMsg));
        body.add("messages", messages);

        String response;
        if (openai) {
            response = post(url, timeout, body.toString(), key.isBlank() ? null : "Bearer " + key, null);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            return json.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString().trim();
        }
        response = post(url, timeout, body.toString(), null, key);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        StringBuilder sb = new StringBuilder();
        for (var el : json.getAsJsonArray("content")) {
            JsonObject block = el.getAsJsonObject();
            if (block.has("type") && "text".equals(block.get("type").getAsString())) sb.append(block.get("text").getAsString());
        }
        return sb.toString().trim();
    }

    private static JsonObject msg(String role, String content) {
        JsonObject o = new JsonObject();
        o.addProperty("role", role);
        o.addProperty("content", content);
        return o;
    }

    private static String post(String url, int timeoutSec, String body, String bearer, String anthropicKey) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(10_000);
        c.setReadTimeout(timeoutSec * 1000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        if (bearer != null) c.setRequestProperty("Authorization", bearer);
        if (anthropicKey != null) {
            c.setRequestProperty("x-api-key", anthropicKey);
            c.setRequestProperty("anthropic-version", "2023-06-01");
        }
        try (OutputStream os = c.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = c.getResponseCode();
        InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String resp = is == null ? "" : new String(is.readAllBytes(), StandardCharsets.UTF_8);
        if (code / 100 != 2) {
            throw new IOException("HTTP " + code + ": " + (resp.length() > 300 ? resp.substring(0, 300) : resp));
        }
        return resp;
    }
}
