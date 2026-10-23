# android-projet-s9 — The Culling Games

Yicheng ZHENG
Arthur DRIEUX
Nadir KOUDRI
Yahya Hijazi
Alexandre Leclaire

Adaptation de **King of Tokyo** (Dark Edition, avec les tuiles Méchanceté) dans l’univers des **Culling Games de Jujutsu Kaisen** : un joueur humain affronte une à trois IA, avec 4 sorciers, 20 cartes Énergie personnalisées et 10 tuiles Méchanceté. Application Android en Kotlin, architecture MVVM.

## Lancer le projet

1. Ouvrir le dossier `TheCullingGames/` dans Android Studio et laisser Gradle se synchroniser.
2. Lancer la configuration `app` sur un émulateur ou un appareil (Android 10 / API 29 minimum).
3. Tests (moteur, IA et parcours d’interface, sans appareil) : `./gradlew testDebugUnitTest` depuis `TheCullingGames/`.
   Le test d’interface enregistre des captures d’écran dans `app/build/ui-screenshots/`.

## Déroulement d’une partie

Accueil → choix du sorcier et du nombre d’IA (1 à 3) → partie → écran de victoire ou de défaite → nouvelle partie.
Pendant la partie : lancer les dés (3 lancers, dés conservables et reprenables), les résoudre dans l’ordre choisi, entrer dans la colonie de Tokyo si elle est vide, acheter et jouer des techniques, choisir les tuiles de Méchanceté aux seuils 3 / 6 / 10. Les réactions (Infinity, fuite de Tokyo) sont demandées au joueur pendant les tours adverses. Les règles et toutes les cartes sont consultables depuis l’accueil et pendant la partie (menu « Règles »).

## Architecture

MVVM en couches, avec un moteur de règles indépendant d’Android :

```
View (Fragments + layouts XML, Navigation + SafeArgs)
   │  observe StateFlow<GameUiState>, envoie des GameCommand
ViewModel (GameViewModel, CharacterSelectViewModel)
   │  fait jouer les IA dans une coroutine, construit un état immuable pour la vue
Business (Kotlin pur, testable sur JVM)
   │  GameEngine · DamageCalculator · RulesConfig · SimpleAi · interfaces des repositories
Data
      catalogues (personnages, cartes, tuiles) · InMemoryGameRepository · ServiceLocator
```

Choix principaux :

- **Un seul point d’entrée pour les règles** : `GameEngine.legalCommands(joueur)` liste les commandes autorisées, et `submit(commande)` refuse toute commande absente de cette liste. L’interface active ses boutons d’après cette liste, et l’IA choisit uniquement parmi elle : les règles ne sont jamais dupliquées dans les écrans et l’IA ne peut pas tricher.
- **Choix en attente** (`PendingChoice`) : Infinity, fuite de Tokyo et choix de tuile peuvent appartenir à un joueur autre que le joueur actif ; le moteur s’interrompt jusqu’à la réponse.
- **Paramètres centralisés** : tous les nombres des règles et des dégâts sont dans `RulesConfig`, pour l’équilibrage.
- **Aléatoire injectable** (`RandomSource`) : parties reproductibles dans les tests.
- **Pas de sauvegarde persistante** (spécification) : la partie vit dans le ViewModel et le repository en mémoire ; elle survit aux rotations, pas à l’arrêt du processus.
- **Une activité, plusieurs fragments** reliés par `res/navigation/nav_graph.xml`, arguments passés avec SafeArgs ; vues personnalisées `DieView` (dé) et `WickednessGaugeView` (jauge) ; mises en page dédiées au paysage et aux tablettes (`layout-land`, `layout-sw600dp`).

## Correspondance spécifications → code

Chemins relatifs à `TheCullingGames/app/src/main/java/fr/epita/thecullinggames/`.

| Spécification | Code |
| --- | --- |
| Tour de jeu, dés, résolution, Tokyo, victoire (`docs/KOT_WICKEDNESS.md`) | `business/engine/GameEngine.kt` |
| 20 cartes Énergie (`docs/Rules-Cards`) | prix et types : `data/Catalogs.kt` (`CardCatalog`) ; effets : `GameEngine.playAction`, `activatePower`, `DamageCalculator.kt` ; textes et images : `res/values/strings.xml`, `ui/common/Visuals.kt` |
| 10 tuiles Méchanceté | `TileId` (`business/model/Ids.kt`), `GameEngine.acquireTile` et effets de début de tour / résolution |
| Conventions de dégâts (`docs/A_CLARIFIER.md`) | `business/engine/DamageCalculator.kt`, `GameEngine.applyDamage`, `RulesConfig.kt` |
| Un humain contre 1 à 3 IA | `business/ai/SimpleAi.kt`, boucle des IA dans `ui/game/GameViewModel.kt` |
| Personnages | `data/Catalogs.kt` (`CharacterCatalog`), portraits dans `res/drawable-nodpi/` |
| Écrans | `ui/home`, `ui/select`, `ui/game`, `ui/gameover`, `res/navigation/nav_graph.xml` |

## Tests

- `app/src/test/.../business/GameEngineTest.kt` : 25 tests de règles (brelans, Méchanceté et tuiles, Tokyo à 2 et à 3-4 joueurs, soins, Binding Vow, Infinity, Domain Expansion, Veil + Special Grade, Cybercerveau, Sournois + Incantation, marché maintenu à trois cartes, Mahoraga, recyclage des Actions, Boogie Woogie dans les deux sens, Divergent Fist, Panique générale, élimination du joueur humain, victoire en fin de tour, commandes illégales).
- `AiSimulationTest.kt` : 600 parties entre IA (2 à 4 joueurs) doivent toutes se terminer sans commande illégale.
- `ui/GameFlowUiTest.kt` (Robolectric) : parcours complet accueil → choix du sorcier → partie jouée par les vrais boutons → rotation pendant le tour → écran de fin.

Sur 400 parties simulées par format, une partie dure environ 8 à 9 tours par joueur.

## Limites connues

- Les règles encore ouvertes ou provisoires sont listées dans `docs/A_CLARIFIER.md` (section « Conventions retenues dans l’implémentation »).
- Les capacités propres aux personnages sont reportées (spécification) : les quatre sorciers ont les mêmes règles.
- L’IA est volontairement simple (heuristiques) ; elle n’achète pas Maki’s glasses.
- Pas de sauvegarde : quitter la partie (retour, avec confirmation) ou arrêter l’application la perd.

## Organisation

Suivi des tâches : [`docs/RAF.md`](docs/RAF.md). Décisions de règles : [`docs/A_CLARIFIER.md`](docs/A_CLARIFIER.md). Référence back-end : [`docs/KOT_WICKEDNESS.md`](docs/KOT_WICKEDNESS.md).

### Première séance

* **Yicheng ZHENG & Arthur DRIEUX** : travail sur le **design** des personnages et l’interface de l’application.
* **Nadir KOUDRI & Alexandre Leclaire** : travail sur le **back-end** et la logique de l’application.
* **Yahya Hijazi** : travail sur les **règles du jeu** ainsi que sur la **réflexion autour des effets programmables des cartes**.

L’objectif de cette première séance est de poser les bases du projet et de permettre à chacun d’avancer sur sa partie.
