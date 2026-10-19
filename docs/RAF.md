# RAF global — The Culling Games

Travail restant pour toute l’équipe, sur le périmètre actuel : adaptation de King of Tokyo, un humain et une à trois IA, 20 cartes Énergie personnalisées et tuiles Méchanceté. Architecture retenue : MVVM avec couches View, Business et Data.

État constaté dans le dépôt : squelette Android avec écran « Hello World! », documentation des règles et des cartes. Les tâches ci-dessous restent à réaliser ou à vérifier dans le projet ; aucun développement non constaté n’est considéré comme terminé.

Les décisions encore ouvertes sont dans [A_CLARIFIER.md](A_CLARIFIER.md). Les détails des dégâts y disposent de conventions provisoires et ne bloquent pas le développement. La sauvegarde persistante est exclue ; les capacités propres aux personnages sont reportées.

## Répartition de référence

D’après le README, complété par la précision de Nadir sur son périmètre :

| Domaine | Référents |
| --- | --- |
| Design et interface | Yicheng ZHENG et Arthur DRIEUX |
| Back, Business et Data | Nadir KOUDRI pour le périmètre de développement actuel |
| Règles et effets des cartes | Yahya Hijazi, avec validation de l’équipe |
| ViewModels | Équipe front |
| Intégration et recette | Front et back |

Pour le plan back, Nadir prend en charge l’ensemble des couches Business et Data ; la répartition initiale du README reste un historique.

## 1. Architecture et contrats — front et back

- [ ] Organiser les packages View, Business et Data selon MVVM.
- [ ] Garder le moteur Business indépendant des classes Android ; définir les interfaces de repositories côté Business et leurs implémentations côté Data.
- [ ] Définir les états exposés au front, les commandes du joueur, les événements de jeu et les erreurs métier.
- [ ] Définir les choix en attente : dés, ordre de résolution, cibles, tuile, défense, fuite de Tokyo.
- [ ] Convenir des responsabilités des ViewModels et brancher leur exposition d’état observable.

## 2. Business : moteur de base — back

- [ ] Modéliser partie, joueurs, ressources, dés, positions, cartes, tuiles et phases.
- [ ] Initialiser une partie de deux à quatre participants, dont un humain et une à trois IA ; déterminer le premier joueur.
- [ ] Gérer six dés, leur conservation et reprise après chaque lancer, les relances partielles et l’arrêt anticipé ; trois lancers de base, cinq maximum avec Sournois et Incantation (1 Énergie).
- [ ] Résoudre les chiffres, l’Énergie, les Cœurs et les Griffes, avec ordre de résolution choisi.
- [ ] Gérer Tokyo City : entrée obligatoire si libre, bonus, attaques et fuite.
- [ ] Gérer PV maximum, dépenses, élimination et passage au prochain joueur vivant.
- [ ] Appliquer les effets de début et de fin de tour avec soins prioritaires, puis vérifier les conditions de victoire ; Game Over immédiat à l’élimination humaine.
- [ ] Valider les commandes selon la phase, les ressources et les choix légaux.
- [ ] Fournir un aléatoire injectable pour reproduire les parties lors des tests.

## 3. Business : cartes et Méchanceté — back et règles

- [ ] Intégrer les 20 cartes de [Rules-Cards](Rules-Cards), leurs prix et leurs types.
- [ ] Gérer les trois cartes du marché, achats, remplacement, renouvellement payant, pioche et défausse.
- [ ] Mettre en place les cartes conservées pour utilisation ultérieure, les Pouvoirs permanents après activation, leurs conditions réactivables (dont Veil), les effets différés et limites d’utilisation.
- [ ] Implémenter les 20 effets personnalisés avec les conventions provisoires de dégâts.
- [ ] Gérer la jauge de Méchanceté, les seuils 3 / 6 / 10, les réserves communes et le choix des tuiles après résolution de tous les dés ; une acquisition par niveau et par joueur, sans dépenser la Méchanceté.
- [ ] Implémenter les 10 tuiles décrites dans [KOT_WICKEDNESS.md](KOT_WICKEDNESS.md).
- [ ] Appliquer Cybercerveau une fois à chaque tour du propriétaire : compter une face deux fois, y compris pour les brelans, sans lancer de dé supplémentaire ; première utilisation au tour suivant son acquisition.
- [ ] Conserver les effets récurrents des tuiles pendant toute la partie, et respecter les effets explicitement ponctuels.
- [ ] Centraliser les paramètres de dégâts pour permettre un équilibrage ultérieur.
- [ ] Exclure les jetons spécifiques et les dés supplémentaires du modèle et des effets.

## 4. Business : IA — back

- [ ] Faire utiliser aux IA les mêmes commandes légales qu’au joueur humain.
- [ ] Définir une stratégie initiale simple de conservation et relance des dés.
- [ ] Automatiser achats, choix de tuiles, activation des pouvoirs et choix de cibles.
- [ ] Automatiser les réactions : protection, fuite de Tokyo et choix imposés par les effets.
- [ ] Enchaîner les tours des IA et rendre la main au joueur sans bloquer l’interface.

## 5. Data — back

- [ ] Fournir les catalogues des personnages, cartes et tuiles via des repositories.
- [ ] Assurer des identifiants stables pour les données utilisées par Business et View.
- [ ] Fournir un repository de partie en mémoire pour la première version jouable.

## 6. View et ViewModels — front, avec intégration back

- [ ] Définir le design des quatre personnages et préparer les ressources graphiques.
- [ ] Réaliser l’accueil et la sélection du personnage.
- [ ] Réaliser l’écran de partie : joueurs, ressources, Tokyo, dés, phase et joueur actif.
- [ ] Permettre sélection des dés, relance et validation.
- [ ] Afficher le marché et permettre achats, renouvellement et consultation des cartes possédées.
- [ ] Afficher la jauge de Méchanceté, le choix des tuiles et les tuiles acquises.
- [ ] Réaliser les interactions de ciblage, répartition des dégâts, défense et fuite.
- [ ] Afficher les actions des IA et les résultats des effets de façon compréhensible.
- [ ] Réaliser l’écran de fin de partie et le lancement d’une nouvelle partie.
- [ ] Brancher les ViewModels sur les actions et états Business, sans dupliquer les règles dans les écrans.
- [ ] Préserver l’état lors des recréations d’écran ; aucune reprise après arrêt du processus, conformément à l’absence de sauvegarde.
- [ ] Vérifier la lisibilité et l’utilisation sur plusieurs tailles d’écran.

## 7. Règles et équilibrage — règles et équipe

- [ ] Traiter progressivement les décisions de [A_CLARIFIER.md](A_CLARIFIER.md), notamment l’option à deux participants, le soin à l’achat de Moharaga et le recyclage des Actions.
- [ ] Documenter les conventions retenues lorsque l’implémentation nécessite un choix provisoire.
- [ ] Jouer des parties complètes pour vérifier durées, rythme, puissance des cartes et comportement des IA.
- [ ] Ajuster ensuite les dégâts et interactions, sans bloquer la première version jouable.
- [ ] Mettre à jour les descriptions affichées et les documents après chaque règle validée.

## 8. Intégration, tests et livraison — toute l’équipe

- [ ] Tester le moteur : phases, ressources, Tokyo, cartes, seuils et effets récurrents.
- [ ] Tester les cas limites : défenses cumulées, soins, actions différées, éliminations et victoire.
- [ ] Vérifier que les IA ne produisent pas de commandes illégales et qu’une partie peut se terminer.
- [ ] Vérifier les parcours complets : choix du personnage → partie → victoire ou défaite → nouvelle partie.
- [ ] Tester les interactions front/back et les changements de configuration Android.
- [ ] Compiler et exécuter l’application sur émulateur ou appareil.
- [ ] Documenter le lancement, l’architecture, les limites connues et la répartition des contributions.
- [ ] Préparer une version de démonstration et vérifier les critères de la grille IPAN2 fournie dans `docs/`.

## Ordre de réalisation proposé

1. Architecture et contrats, puis moteur de base avec repository en mémoire.
2. Écran de partie minimal branché aux ViewModels et première partie sans effets avancés.
3. Cartes, Méchanceté et IA pour obtenir une partie complète.
4. Recette, équilibrage, finition visuelle sans sauvegarde persistante.
