# Horror Mod — Snatcher (Forge 1.20.1)

Mob novo: **Snatcher**, criatura esquelética que caça jogadores no escuro.
Forge 47.3.0 / Minecraft 1.20.1 / Java 17 / mappings oficiais (Mojang).

## Como compilar

1. Baixe o **MDK 1.20.1** em https://files.minecraftforge.net e copie `gradlew`, `gradlew.bat` e a pasta
   `gradle/` (com o `gradle-wrapper.jar`) para dentro desta pasta. (Ou abra o projeto no IntelliJ, que já se vira.)
2. `./gradlew build` → o jar sai em `build/libs/horrormod-0.1.0.jar` (jogue na pasta `mods`).
3. Para testar direto: `./gradlew runClient`.

Teste no jogo: aba **Spawn Eggs** do criativo → *Snatcher Spawn Egg*, ou `/summon horrormod:snatcher`.
Ele também nasce naturalmente no Overworld (arquivo `data/horrormod/forge/biome_modifier/snatcher_spawns.json`).

## Como funciona o modelo

O `.glb` original é um mesh com esqueleto Mixamo (skinning), formato que o Minecraft não lê.
O script `tools/bake_glb.py` faz o skinning e "assa" a animação em 104 quadros de vértices, gravados em
`assets/horrormod/mesh/snatcher.bin`. O `SnatcherRenderer` lê esse arquivo e interpola entre os quadros.
Sem dependências externas (não precisa de GeckoLib).

Pra trocar o modelo/animação: `python3 tools/bake_glb.py novo.glb snatcher.bin snatcher.png` (precisa de numpy + Pillow)
e substitua os arquivos em `src/main/resources/assets/horrormod/`.

Arquivos principais:
- `entity/SnatcherEntity.java` — vida, dano, IA, sons
- `client/SnatcherRenderer.java` — escala (`SCALE`), rotação, animação
- `registry/ModEntities.java` — hitbox (`.sized(0.6F, 1.8F)`)

## Créditos (obrigatório, licença CC-BY 4.0)

Modelo **"PSX Snatcher - Low Poly Horror"** por **PolyGoreStudio**
https://sketchfab.com/3d-models/psx-snatcher-low-poly-horror-d21dc0fbad3146db97fc490a74e29188
Licença: CC-BY-4.0 — https://creativecommons.org/licenses/by/4.0/
Se publicar o mod (CurseForge/Modrinth), mantenha esse crédito na página.
