# King of Tokyo — Wickedness et référence back-end

Document pour Nadir Koudri, chargé du back-end et de la logique de jeu de The Culling Games. Aucun changement du front ni du moteur n'est réalisé ici.

## Sources et périmètre

- Photo des tuiles Wickedness fournie initialement : référence des 10 tuiles rouges transcrites ci-dessous ; image supprimée du dépôt après transcription.
- [Règles françaises](Regles_KingOfTokyo_FR.pdf), King of Tokyo — Dark Edition, particulièrement pages 3 à 8.
- [Rules-Cards](Rules-Cards) : 20 cartes Énergie personnalisées, thème Jujutsu Kaisen, utilisées à la place du catalogue Énergie original.
- [README](../README.md) : répartition de l'équipe.
- Les deux supports Android de `docs` concernent les exercices et la navigation. La grille IPAN2 demande notamment Kotlin, une architecture appropriée et une répartition des tâches visible ; ces documents n'ajoutent aucune règle de jeu.

Le catalogue prévoit quatre personnages. Le format retenu permet deux à quatre participants : un humain et une à trois IA. Un seul emplacement Tokyo City est utilisé.

**Périmètre des spécifications du professeur :** les jetons liés aux cartes (notamment Poison / radioactivité, ainsi que les jetons présentés avec les dés en page 3 du livret) et les dés supplémentaires ne font pas partie du périmètre. Les effets des cartes originales qui nécessitent ces éléments ne sont pas à intégrer. Les dés de base restent nécessaires. Cybercerveau double l’effet d’un dé selon les spécifications du professeur ; ses précisions restent dans [A_CLARIFIER.md](A_CLARIFIER.md).

## Transcription des tuiles Wickedness

Le tableau ci-dessous présente les tuiles retenues selon les spécifications du professeur. Seuls les pictogrammes sont représentés par une notation textuelle : ⚡ = Énergie, ♥ = Points de Vie, ☆ = Points de Victoire, [1] = face 1, [⚡] = face Énergie, [♥] = face Cœur, [Griffe] = face d'attaque. Les retours à la ligne et la mise en gras de la photo ne sont pas reproduits.

| Niveau de Méchanceté | Nom | Texte de la tuile |
| --- | --- | --- |
| 3 | INFATIGABLE | Au début de votre tour, gagnez 1⚡. |
| 3 | ÉTERNEL | Au début de votre tour, gagnez 1♥. |
| 3 | FURTIF | Gagnez 1☆ supplémentaire lorsque vous obtenez au moins [1] [1] [1]. |
| 3 | SOURNOIS | Gagnez un Lancer supplémentaire à chaque tour. |
| 6 | RÉGÉNÉRATION TOTALE | Vous pouvez avoir jusqu’à 12♥. Récupérez tous vos ♥ quand vous gagnez cette tuile. |
| 6 | PANIQUE GÉNÉRALE | Tous les autres Monstres perdent 4☆, puis défaussez cette tuile. |
| 6 | REPAIRE MALÉFIQUE | Le prix des cartes Énergie que vous achetez est diminué de 1⚡. |
| 6 | CYBERCERVEAU | À chacun de vos tours, jusqu’à la fin de la partie, comptez la face d’un dé de votre choix deux fois, y compris pour former les brelans. Aucun dé supplémentaire n’est lancé. |
| 10 | RAYON STELLAIRE | Gagnez 1⚡ supplémentaire pour chaque [⚡] et 1♥ supplémentaire pour chaque [♥]. |
| 10 | RAYON ANTIMATIÈRE | Doublez vos [Griffe]. |

La photo ne fournit que ces dix faces rouges. Les faces vertes ne sont pas transcrites. Les valeurs 3, 6 et 10 sont des seuils de Méchanceté, pas des coûts en Énergie.

## Acquisition et conservation des Wickedness

**Spécifications du professeur :** les tuiles acquises restent actives pendant toute la partie ; leurs effets récurrents s’appliquent à chaque tour concerné. Cybercerveau permet donc de compter la face d’un dé deux fois, y compris pour les brelans, une fois à chacun des tours de son propriétaire. Les déclenchements explicitement ponctuels restent ceux du texte : soin complet à l’acquisition de Régénération totale ; perte de points puis défausse pour Panique générale.

Base du livret, pages 4 et 5, avec le report d’acquisition confirmé pour la variante :

- Les tuiles sont réparties en réserves communes : quatre au niveau 3, quatre au niveau 6, deux au niveau 10.
- Un triple de 1 rapporte 2 points de Méchanceté ; un triple de 2 en rapporte 1 ; les 3 n'en rapportent pas. Les dés identiques au-delà du triple ajoutent des points de victoire, pas un bonus de Méchanceté par dé.
- Dans la variante retenue, après résolution de tous les dés, atteindre ou dépasser un seuil permet de choisir une tuile encore disponible de ce niveau. La jauge ne redescend pas après acquisition.
- Le livret limite l'acquisition à une tuile de chaque niveau par joueur. Il faut mémoriser les niveaux déjà utilisés, y compris après la défausse de Panique générale.
- L'effet d'une tuile devient actif à son acquisition ; elle est conservée sauf indication contraire. Panique générale est immédiatement défaussée après son effet. Cybercerveau ne modifie pas rétroactivement les dés résolus : il est utilisable à partir du prochain tour du propriétaire.
- Les tuiles acquises par un joueur éliminé sont défaussées.

## Règles de base utiles au moteur

Référence : livret fourni, pages 4 à 8. Il s'agit de la base documentaire, avant les adaptations personnalisées.

| Sujet | Règle |
| --- | --- |
| Départ | 10 PV, 0 point de victoire ; Tokyo vide ; marché de trois cartes Énergie visibles. |
| Premier joueur | Chaque joueur lance les six dés ; celui ayant le plus de Griffes commence ; les joueurs à égalité relancent. |
| Tour | Lancer les dés → résoudre les dés → entrer dans Tokyo → acheter des cartes → finir son tour. Les effets de début de tour précèdent les lancers. |
| Lancers | Six dés de base ; trois lancers maximum, soit un premier lancer et deux relances. Il est possible de conserver des dés, puis de les reprendre lors d'une relance ultérieure. |
| Résolution | Tous les dés sont résolus, dans l'ordre choisi par le joueur. Le moteur doit permettre cet ordre, qui peut influer sur les effets et les éliminations. |
| Chiffres | Un triple de valeur n rapporte n points de victoire ; chaque dé de même valeur au-delà des trois premiers ajoute un point. |
| Énergie | Chaque face Énergie rapporte une unité conservée jusqu'à utilisation. |
| Griffes | Depuis Tokyo : attaque de tous les adversaires dehors. Depuis l'extérieur : attaque des occupants de Tokyo. Chaque Griffe inflige un dégât à chaque cible concernée, avant modificateurs. |
| Soin | Chaque face Cœur soigne d'un PV hors Tokyo, dans la limite du maximum. Les dés ne soignent pas dans Tokyo ; cartes et tuiles peuvent soigner dans Tokyo. |
| Tokyo | Entrée obligatoire si vide pendant la phase correspondante : +1 point de victoire. Commencer son tour dans Tokyo : +2 points de victoire. |
| Fuite | Un occupant blessé par les Griffes peut choisir de sortir après avoir subi les dégâts. Les pertes de PV causées par une carte ne permettent pas cette fuite selon le livret. |
| Achats | Une ou plusieurs cartes achetables si l'Énergie suffit ; chaque achat révèle immédiatement une carte de remplacement. Dépenser 2 Énergies permet de renouveler les trois cartes visibles. |
| Types | Pouvoir : conservé et actif selon son texte. Action : résolue immédiatement puis défaussée dans la règle de base. |
| Élimination | À zéro PV, le monstre est éliminé et ses cartes Pouvoir, tuiles et Énergies sont défaussées. |
| Victoire | Vérification à la fin du tour selon ce livret : un monstre vivant ayant atteint 20 points de victoire, ou le dernier survivant, gagne. Un monstre mort ne gagne pas ; si tous sont éliminés simultanément, tous perdent. |

Tokyo Bay est exclu puisque le format ne dépasse pas quatre participants. Pour une partie commençant à deux participants, le livret page 7 conseille une option : gagner 1 Énergie à l’entrée dans Tokyo au lieu de 1 point de victoire, et 1 Énergie au début du tour dans Tokyo au lieu de 2 points de victoire. Cette option reste à choisir pour notre version ; elle ne s’applique pas automatiquement lorsqu’une partie commencée à trois ou quatre se réduit à deux survivants.

## Cartes personnalisées : inventaire pour le back

Les noms ci-dessous conservent l'orthographe de `Rules-Cards`. Ce tableau résume leurs effets ; le fichier source reste la référence de leur texte intégral. Les questions de durée, de cumul et de priorité sont regroupées dans [A_CLARIFIER.md](A_CLARIFIER.md).

| Coût | Carte | Type déclaré | Effet à gérer |
| --- | --- | --- | --- |
| 5 | Domain Expension | Pouvoir | +1 dégât depuis Tokyo ; au moins trois Griffes finales ignorent immunités et réductions adverses. |
| 5 | Infinity | Pouvoir | Dépenser 2 Énergies pour annuler une attaque reçue ; une utilisation par tour de table. |
| 5 | Reversed Cursed Technique | Action | Soin immédiat de 4 PV, y compris dans Tokyo. |
| 5 | Binding Vow | Pouvoir | Interdit le soin par les dés ; +2 dégâts ; soins de cartes autorisés. |
| 5 | Moharaga's adaptation | Pouvoir | Maximum fixé à 12 PV ; soin immédiat de 2 PV à l'achat. |
| 4 | Special Grade | Pouvoir | Première fois que des dégâts sont subis par tour de table : réduction de 2. |
| 4 | Black Flash | Action | Après les dés, un brelan de 1, 2 ou 3 permet d'infliger 2 dégâts supplémentaires aux adversaires. |
| 4 | Violet | Action | 2 dégâts à une cible ; depuis Tokyo, 3 dégâts à répartir entre les joueurs extérieurs. |
| 4 | Veil | Pouvoir | Dans Tokyo, les attaques infligeant au moins 3 dégâts sont réduites de 1. |
| 4 | Resonance technique | Action | Une cible perd toute son Énergie ; gain de la moitié de cette quantité, arrondie au supérieur. |
| 3 | Nanami's overtime | Pouvoir | Fin de tour sans dégât, précisé comme aucune Griffe résolue : +1 Énergie et +1 point de victoire. |
| 3 | 10 Shadows | Pouvoir | Une fois par tour, transformer une face 1 ou 2 en Griffe. |
| 3 | Sukuna's finger | Pouvoir | Début de chaque tour : +1 point de victoire et perte obligatoire de 1 PV, potentiellement mortelle. |
| 3 | Overflowing Energy | Action | Soin immédiat de 3 PV. |
| 3 | Incantation | Pouvoir | Une fois par tour, payer 1 Énergie pour une troisième relance. |
| 2 | Divergent Fist | Action | Au prochain tour, une cible recevant au moins un dégât perd 1 Énergie si possible. |
| 2 | Boogie Woogie | Action | Échange de place avec un occupant de Tokyo, et inversement ; sortie récompensée de 1 point, entrée sans point. |
| 2 | Maki's glasses | Pouvoir | Consulter la première carte de la pioche et l'acheter comme une carte visible. |
| 2 | Blood Manipulation | Action | Sacrifier 1 PV pour infliger 2 dégâts ciblés hors Tokyo ; interdit à 1 PV. |
| 2 | Talisman | Action | Renouveler les trois cartes visibles et gagner 1 Énergie. |

Ces cartes ne sont pas distribuées automatiquement à chaque tour par les règles fournies : ce sont les cartes du marché achetées pendant les tours. Elles sont distinctes des tuiles Wickedness acquises par la jauge.

## Organisation proposée du back-end

Le dépôt contient actuellement une activité Android de démarrage, sans moteur métier dans les sources Kotlin consultées. Le back désigne ici la logique Kotlin de l'application ; les documents n'imposent pas de serveur distant.

- **État de partie** : joueurs vivants, joueur actif, phase, occupation de Tokyo, dés et dés conservés, nombre de lancers, pioche, marché et défausse, réserves Wickedness, choix en attente.
- **État du joueur** : PV, maximum de PV, Énergie, points de victoire, Méchanceté, cartes et tuiles possédées, seuils déjà utilisés, compteurs d'utilisation et effets différés.
- **Moteur de règles indépendant d'Android** : reçoit une commande validée, produit le nouvel état et des événements métier. L'interface et les IA utilisent les mêmes commandes et les mêmes actions légales.
- **Déclencheurs d'effets** : acquisition, début de tour, modification des dés, résolution des faces, calcul des dégâts par cible, réaction défensive, achats, fin de tour. Un effet différé peut survivre à la défausse de sa carte Action.
- **Choix explicites** : dés à relancer, ordre de résolution, tuile à prendre, cible, répartition de dégâts, activation d'Infinity et décision de fuite. Les IA doivent pouvoir répondre à ces mêmes choix.
- **Aléatoire injectable** : rendre les lancers et la pioche reproductibles pour vérifier les règles.

Les futurs tests métier devront couvrir les seuils franchis et épuisés, la résolution de plusieurs effets, les limites de soin, les réductions par cible, les actions différées, les éliminations et la victoire en fin de tour. Les arbitrages nécessaires avant ces implémentations sont dans `A_CLARIFIER.md`.

## Dégâts : conventions provisoires

Des valeurs provisoires sont utilisées pour avancer, avec détail et équilibrage ultérieurs. Les valeurs écrites des cartes servent de base ; les conventions provisoires de calcul, de cumul et de défense sont regroupées dans [A_CLARIFIER.md](A_CLARIFIER.md#calcul-des-dégâts-et-interactions--différé-non-bloquant). Ces points ne bloquent pas le développement et ne modifient pas la transcription des tuiles. Les paramètres de dégâts devront être centralisés pour faciliter leur ajustement.

## Précisions de la variante

- Deux, trois ou quatre participants : un humain et les autres contrôlés par une IA simple ; un seul emplacement Tokyo.
- Aucune sauvegarde persistante ; les capacités propres aux personnages seront étudiées plus tard.
- Un exemplaire de chaque carte Énergie ; marché de trois cartes ; défausse remélangée lorsque la pioche est épuisée.
- Les cartes peuvent être conservées et jouées plus tard, uniquement pendant le tour de leur propriétaire. Une fois Infinity activé pendant son tour, sa réaction peut être utilisée pendant le tour adverse, en payant 2 Énergies, une fois par tour de table.
- Les cartes Pouvoir sont permanentes après activation. Leurs conditions sont réévaluées : Domain Expension et Veil s’appliquent dans Tokyo, cessent de produire cet effet dehors et le retrouvent au retour dans Tokyo. Veil est un Pouvoir.
- Au début du tour, les soins sont prioritaires sur les pertes de PV, notamment Éternel avant Sukuna’s finger.
- Divergent Fist concerne uniquement le prochain tour après utilisation et retire au maximum une Énergie par cible pendant ce tour.
- Boogie Woogie est inutilisable si Tokyo est vide ; le sortant gagne toujours un point.
- L’élimination du joueur humain termine immédiatement sa partie : Game Over.
- Le livret, page 5, autorise trois lancers maximum de base : le premier lancer et deux relances. Sournois et Incantation se cumulent jusqu’à cinq lancers maximum, en payant 1 Énergie pour le lancer supplémentaire d’Incantation. Les six dés peuvent être conservés ou repris librement entre les lancers ; seules les faces des dés choisis sont relancées.
