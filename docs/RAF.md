# RAF global — The Culling Games

Travail restant pour toute l’équipe, sur le périmètre actuel : adaptation de King of Tokyo, un humain et une à trois IA, 20 cartes Énergie personnalisées et tuiles Méchanceté. Architecture retenue : MVVM avec couches View, Business et Data.

État constaté dans le dépôt au 19/10/2026 : application jouable de bout en bout (moteur, 20 cartes, 10 tuiles, IA, écrans, tests). Les cases cochées correspondent à des développements présents dans le dépôt et couverts par les tests (`./gradlew testDebugUnitTest`) ; les cases restantes demandent encore une action de l’équipe.

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

- [x] Organiser les packages View, Business et Data selon MVVM.
- [x] Garder le moteur Business indépendant des classes Android ; définir les interfaces de repositories côté Business et leurs implémentations côté Data.
- [x] Définir les états exposés au front, les commandes du joueur, les événements de jeu et les erreurs métier.
- [x] Définir les choix en attente : dés, ordre de résolution, cibles, tuile, défense, fuite de Tokyo.
- [x] Convenir des responsabilités des ViewModels et brancher leur exposition d’état observable.

## 2. Business : moteur de base — back

- [x] Modéliser partie, joueurs, ressources, dés, positions, cartes, tuiles et phases.
- [x] Initialiser une partie de deux à quatre participants, dont un humain et une à trois IA ; déterminer le premier joueur.
- [x] Gérer six dés, leur conservation et reprise après chaque lancer, les relances partielles et l’arrêt anticipé ; trois lancers de base, cinq maximum avec Sournois et Incantation (1 Énergie).
- [x] Résoudre les chiffres, l’Énergie, les Cœurs et les Griffes, avec ordre de résolution choisi.
- [x] Gérer Tokyo City : entrée obligatoire si libre, attaques et fuite ; bonus selon le nombre initial de participants (à deux : +1 Énergie à l’entrée et au début du tour ; à trois ou quatre : respectivement +1 et +2 points de victoire).
- [x] Gérer PV maximum, dépenses, élimination et passage au prochain joueur vivant.
- [x] Appliquer les effets de début et de fin de tour avec soins prioritaires, puis vérifier les conditions de victoire ; Game Over immédiat à l’élimination humaine.
- [x] Valider les commandes selon la phase, les ressources et les choix légaux.
- [x] Fournir un aléatoire injectable pour reproduire les parties lors des tests.

## 3. Business : cartes et Méchanceté — back et règles

- [x] Intégrer les 20 cartes de [Rules-Cards](Rules-Cards), leurs prix et leurs types.
- [x] Gérer les trois cartes du marché, achats, remplacement, renouvellement payant, pioche et défausse.
- [x] Mettre en place les cartes conservées pour utilisation ultérieure, les Pouvoirs permanents après activation, leurs conditions réactivables (dont Veil), les effets différés et limites d’utilisation.
- [x] Implémenter les 20 effets personnalisés avec les conventions provisoires de dégâts.
- [x] Gérer la jauge de Méchanceté, les seuils 3 / 6 / 10, les réserves communes et le choix des tuiles après résolution de tous les dés ; une acquisition par niveau et par joueur, sans dépenser la Méchanceté.
- [x] Implémenter les 10 tuiles décrites dans [KOT_WICKEDNESS.md](KOT_WICKEDNESS.md).
- [x] Appliquer Cybercerveau une fois à chaque tour du propriétaire : compter une face deux fois, y compris pour les brelans, sans lancer de dé supplémentaire ; première utilisation au tour suivant son acquisition.
- [x] Conserver les effets récurrents des tuiles pendant toute la partie, et respecter les effets explicitement ponctuels.
- [x] Centraliser les paramètres de dégâts pour permettre un équilibrage ultérieur.
- [x] Exclure les jetons spécifiques et les dés supplémentaires du modèle et des effets.

## 4. Business : IA — back

- [x] Faire utiliser aux IA les mêmes commandes légales qu’au joueur humain.
- [x] Définir une stratégie initiale simple de conservation et relance des dés.
- [x] Automatiser achats, choix de tuiles, activation des pouvoirs et choix de cibles.
- [x] Automatiser les réactions : protection, fuite de Tokyo et choix imposés par les effets.
- [x] Enchaîner les tours des IA et rendre la main au joueur sans bloquer l’interface.

## 5. Data — back

- [x] Fournir les catalogues des personnages, cartes et tuiles via des repositories.
- [x] Assurer des identifiants stables pour les données utilisées par Business et View.
- [x] Fournir un repository de partie en mémoire pour la première version jouable.

## 6. View et ViewModels — front, avec intégration back

- [x] Définir le design des quatre personnages et préparer les ressources graphiques.
- [x] Réaliser l’accueil et la sélection du personnage.
- [x] Réaliser l’écran de partie : joueurs, ressources, Tokyo, dés, phase et joueur actif.
- [x] Permettre sélection des dés, relance et validation.
- [x] Afficher le marché et permettre achats, renouvellement et consultation des cartes possédées.
- [x] Afficher la jauge de Méchanceté, le choix des tuiles et les tuiles acquises.
- [x] Réaliser les interactions de ciblage, répartition des dégâts, défense et fuite.
- [x] Afficher les actions des IA et les résultats des effets de façon compréhensible.
- [x] Réaliser l’écran de fin de partie et le lancement d’une nouvelle partie.
- [x] Brancher les ViewModels sur les actions et états Business, sans dupliquer les règles dans les écrans.
- [x] Préserver l’état lors des recréations d’écran ; aucune reprise après arrêt du processus, conformément à l’absence de sauvegarde.
- [ ] Vérifier la lisibilité et l’utilisation sur plusieurs tailles d’écran. *Mises en page téléphone, paysage et tablette (`sw600dp`) en place ; portrait et paysage vérifiés par captures Robolectric, tablette à vérifier sur appareil.*

## 7. Règles et équilibrage — règles et équipe

- [x] Traiter progressivement les décisions de [A_CLARIFIER.md](A_CLARIFIER.md), notamment le soin à l’achat de Moharaga et le recyclage des Actions.
- [x] Documenter les conventions retenues lorsque l’implémentation nécessite un choix provisoire.
- [ ] Jouer des parties complètes pour vérifier durées, rythme, puissance des cartes et comportement des IA. *Simulation IA contre IA : environ 8 à 9 tours par joueur, toutes les cartes achetées régulièrement ; parties humaines à faire.*
- [ ] Ajuster ensuite les dégâts et interactions, sans bloquer la première version jouable.
- [ ] Mettre à jour les descriptions affichées et les documents après chaque règle validée.

## 8. Intégration, tests et livraison — toute l’équipe

- [x] Tester le moteur : phases, ressources, Tokyo, cartes, seuils et effets récurrents.
- [x] Tester les cas limites : défenses cumulées, soins, actions différées, éliminations et victoire.
- [x] Vérifier que les IA ne produisent pas de commandes illégales et qu’une partie peut se terminer.
- [x] Vérifier les parcours complets : choix du personnage → partie → victoire ou défaite → nouvelle partie.
- [x] Tester les interactions front/back et les changements de configuration Android.
- [ ] Compiler et exécuter l’application sur émulateur ou appareil. *APK de debug compilé ; parcours complet vérifié avec Robolectric ; partie complète sur émulateur ou appareil à faire.*
- [ ] Documenter le lancement, l’architecture, les limites connues et la répartition des contributions. *Lancement, architecture, correspondance spécifications → code et limites dans le README ; répartition réelle des contributions à compléter par l’équipe.*
- [ ] Préparer une version de démonstration et vérifier les critères de la grille IPAN2 fournie dans `docs/`.

## 9. Itération retours visuels et règles — octobre 2026

- [x] Gain de points « 4 ★ avec quatre 2 » : moteur correct (2 + 1) ; le 4e point venait d’un autre gain du même moment (entrée dans Tokyo) ou de Cybercerveau. Le journal précise désormais « brelan de 2, 4 dés ». Tests ajoutés.
- [x] Victoire immédiate à 20 ★ à n’importe quel moment, et dernier survivant gagnant immédiatement.
- [x] Cartes utilisées dès l’achat : main supprimée, Pouvoirs actifs à l’achat, Actions résolues à l’achat avec choix de cible, Actions non achetables si leur condition n’est pas remplie (raison affichée).
- [x] Mahoraga’s adaptation (maximum à 12 puis soin de 2 à l’achat) et texte de Reversed Cursed Technique.
- [x] Nanami's overtime : condition « aucune face Griffe lors de votre dernier lancer », lue sur les dés validés (convention 10 Shadows / Cybercerveau dans `A_CLARIFIER.md`).
- [x] Dés : tous gardés après un lancer, on touche ceux à relancer ; bouton « Relancer (n) » désactivé sans sélection.
- [x] Entrées et sorties de Tokyo : bannière non bloquante, mise en évidence de la zone Tokyo et du panneau du joueur.
- [x] Achats : une seule notification par achat avec la carte, l’acheteur et l’effet appliqué ; file d’attente sans chevauchement, conservée à la rotation sans être rejouée.
- [x] Cartes et tuiles : carte ou tuile affichée en grand puis envoyée vers les joueurs touchés ; appui long sur une carte du marché, un Pouvoir ou une tuile pour voir son effet.
- [x] Profil d’un joueur (appui sur son panneau) : jauge de Méchanceté, tuiles, Pouvoirs actifs, effets en cours.
- [ ] Vérifier ces retours visuels sur appareil (durées, lisibilité pendant les tours des IA).
- [ ] Mettre à jour `docs/Rules-Cards` (texte source) pour Nanami's overtime, Reversed Cursed Technique et l’utilisation des cartes à l’achat.

## Ordre de réalisation proposé

1. Architecture et contrats, puis moteur de base avec repository en mémoire.
2. Écran de partie minimal branché aux ViewModels et première partie sans effets avancés.
3. Cartes, Méchanceté et IA pour obtenir une partie complète.
4. Recette, équilibrage, finition visuelle sans sauvegarde persistante.
