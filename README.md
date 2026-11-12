# Ganich's Lanterns (Fabric 1.21.1)

Ванильный фонарь (и фонарь душ) можно носить в отдельном слоте Trinkets `legs/lantern`:
руки свободны, фонарь качается и даёт динамический свет (LambDynamicLights).

- **Слот**: `src/main/resources/data/trinkets` (слот, привязка к игроку, тег подходящих предметов).
- **Надевание**: перетащить фонарь в слот на экране Trinkets (ПКМ в воздух тоже должен работать).
- **Рендер**: `client/BeltLanternRenderer` (маятник на крючке, привязан к телу модели игрока).
- **Свет**: `client/compat/BeltLanternDynamicLights`.

## Физика

Фонарь реагирует на: разгон/торможение, повороты (крючок не на оси, его "раскручивает"), прыжок и падение
(меняется эффективная гравитация), приземление, удар по игроку, взмах руки,
приседание, ритм шагов. Фонарь прибит к крючку: он только качается, вверх-вниз не прыгает. В покое слегка дышит. Углы ограничены, чтобы он упирался в тело, а не проходил сквозь него.

## Ресурспаки

Всё настраивается из ресурспака, ничего перекомпилировать не надо:

| Файл в паке | Что меняет |
| --- | --- |
| `assets/beltlantern/pose/default.json` | положение (`hook`), размер, физика, лимиты для всех фонарей |
| `assets/beltlantern/pose/lantern.json`, `soul_lantern.json` | то же, но для одного предмета |
| `assets/beltlantern/models/item/lantern_worn.json`, `soul_lantern_worn.json` | 3D-модель на поясе (можно заменить целиком, `model_top_px` задаёт высоту крепления) |
| `assets/minecraft/textures/block/lantern.png` | текстура, подхватывается как у обычного блока |

Файлы всех паков склеиваются по полям (верхний пак перекрывает нижний), достаточно указать только нужные поля.
Полный список полей и значения по умолчанию: `src/main/resources/assets/beltlantern/pose/default.json`.
`"physics": false` делает фонарь жёстким (без качания).

Анимационные моды и паки, которые меняют позу торса игрока, учитываются автоматически: фонарь цепляется к
части модели `body` уже после её позирования.

## Запуск

```bash
JAVA_HOME=<jdk 21> ./gradlew runClient
JAVA_HOME=<jdk 21> ./gradlew build     # jar в build/libs
```

`libs/lambdynamiclights-api-*.jar` (LGPL) лежит в репозитории: API LambDynamicLights нигде не опубликован отдельно,
он вложен в jar с Modrinth, а для компиляции он нужен.

Для `runClient` с динамическим светом положи в `dev-libs/` jar-ы из вложенных jar-ов LambDynamicLights
(`lambdynamiclights-runtime`, `spruceui`, `pridelib`, `yumi-*`), папка в git не хранится. Без них клиент запустится
только если убрать LambDynamicLights из зависимостей.

## Лицензия

Код: Apache License 2.0 (файл `LICENSE`).
Иконка мода (`assets/beltlantern/icon.png`) нарисована по текстуре фонаря из пака Bare Bones (RobotPants,
All Rights Reserved) и лицензией этого репозитория не покрывается.
