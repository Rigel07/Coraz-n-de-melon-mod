# Corazón de Melon

Mod de **Minecraft Forge 1.20.1** (Forge 47.3.0+, Java 17).

Encuentra **corazones mágicos**, sube tu nivel de **Inocencia** (de *Bruto* a *Santo*) y los mobs te tratarán cada vez con más cariño.
Conoce a **Mao**, un pequeño ángel que puede ser tu compañero y asistente personal con IA.

## Cómo conseguir el .jar (GitHub)

1. Crea un repositorio en GitHub y sube todo este proyecto (`git init`, `git add .`, `git commit`, `git push`).
2. GitHub Actions compilará el mod automáticamente (pestaña **Actions** → último *Build* → artefacto `corazon-de-melon` → contiene el `.jar`).
3. Para publicar una *release* con el `.jar` adjunto: `git tag v1.0.0 && git push origin v1.0.0`.
4. Mete el `.jar` en la carpeta `mods/` de Minecraft Forge 1.20.1 (cliente y servidor).

### Compilar en tu PC
Necesitas JDK 17 y Gradle 8.x: `gradle wrapper` (una vez) y luego `./gradlew build`. El `.jar` queda en `build/libs/`.

## Corazones mágicos

| Corazón | Color | Inocencia | Rareza |
|---|---|---|---|
| Corazón mágico | Rosa | +1 | Común |
| Corazón mágico azul | Azul | +3 | Poco común |
| Corazón mágico dorado | Dorado | +8 | Raro |
| Corazón mágico violeta | Violeta | +20 | Épico |

- Aparecen **tirados en el suelo del Overworld** cerca de los jugadores, con un brillo para poder localizarlos (configurable).
- También pueden aparecer en **cofres** de estructuras (30 % de los cofres de vanilla).
- Al **recogerlos del suelo** se absorben al instante. Los que salen de cofres se absorben con **clic derecho**.
- **Buenas acciones** también suben la barra: cosechar cultivos maduros (+0.12), comerciar con aldeanos (+0.4), criar animales (+0.3), domesticar (+1.5).

## Niveles de Inocencia

La barra aparece unos segundos al ganar experiencia, o mientras mantienes **I** (configurable en Controles). Cada nivel cuesta más que el anterior.

| Nv | Nombre | XP total |
|---|---|---|
| 1 | Bruto | 0 |
| 2 | Salvaje | 8 |
| 3 | Arisco | 25 |
| 4 | Tranquilo | 55 |
| 5 | Amable | 105 |
| 6 | Gentil | 180 |
| 7 | Bondadoso | 290 |
| 8 | Puro | 450 |
| 9 | Angelical | 700 |
| 10 | Santo | 1100 |

Efectos:

- **Nv 5+**: los mobs hostiles que te persiguen pueden retirarse (probabilidad creciente; si los atacas, no se calman). Los animales pasivos te regalan objetos y efectos (vaca → cuero + regeneración, abeja → panal + velocidad, etc.).
- **Nv 7+**: aura de Suerte (Suerte II en nivel 10). Puedes domesticar a Mao.
- **Nv 8+**: lobos, gatos, loros y caballos salvajes **se domestican solos** (los lobos sueltan un hueso) y los hostiles calmados **te defienden** de otros hostiles durante unos segundos. Los aldeanos te dan esmeraldas con *Héroe de la aldea*.
- **Nv 9+**: los hostiles calmados a veces te dejan un objeto (pólvora, hueso, hilo...).
- Los jefes (Wither, Ender Dragon) y el Warden son inmunes a la calma.

## Mao, el pequeño ángel

- Aparece ocasionalmente de día en el Overworld. Si tienes Inocencia **6+** se acerca y te da objetos; si eres *Bruto/Salvaje/Arisco* (1-3) huye de ti.
- Con Inocencia **7+**, dale una **zanahoria dorada** para domesticarlo. Huevo de aparición en el creativo.
- Domesticado: te sigue, te defiende, te cura cuando estás por debajo del 50 % de vida, y con clic derecho (mano vacía) lo sientas o lo haces seguirte. Una zanahoria dorada lo cura.
- **Asistente con IA**: escribe en el chat `Mao, <mensaje>` (o `/mao <mensaje>`). Conoce tu bioma, salud, hora, objeto en mano... y puede sentarse, seguirte o curarte si se lo pides.

### Configurar la IA

Edita `config/corazondemelon-common.toml` (se crea al arrancar):

```toml
[ai]
provider = "anthropic"            # o "openai" (OpenAI, Ollama local, LM Studio, OpenRouter...)
apiKey = "TU_CLAVE"               # o variable de entorno MAO_API_KEY
model = "claude-haiku-4-5-20251001"
```

Ejemplo con Ollama (gratis y local): `provider = "openai"`, `apiUrl = "http://localhost:11434/v1/chat/completions"`, `model = "llama3.1"`.
Sin clave, Mao responde con frases sencillas (y entiende "siéntate", "sígueme", "cúrame").
**No subas tu clave a GitHub.** En multijugador la IA corre en el servidor.

## Comandos

- `/inocencia` — muestra tu nivel.
- `/inocencia nivel <1-10>` y `/inocencia dar <xp>` — para probar (requieren OP).
- `/mao <mensaje>` — hablar con tu Mao.

## Ajustes de dificultad

En el mismo `.toml`: `xpMultiplier`, `heartSpawnIntervalSeconds`, `heartSpawnChance`, `maxHeartsNearby`, `heartsGlow`, `maoSpawnChance`.

## Texturas

Las texturas se generaron con `tools_gen_textures.py` (Pillow). Puedes reemplazar los PNG de `src/main/resources/assets/corazondemelon/textures/` por tu propio arte.
