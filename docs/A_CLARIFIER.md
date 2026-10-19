# À clarifier — règles et décisions

Les tâches de réalisation de toute l’équipe sont suivies dans [RAF.md](RAF.md). Ce document rassemble les décisions ouvertes et les conventions provisoires.

Points relevés dans `Rules-Cards`, la photo initiale des tuiles Wickedness (supprimée du dépôt après transcription) et le livret français Dark Edition. Ce fichier recense des décisions à prendre ; il ne modifie pas les textes sources. Des valeurs et conventions provisoires sont utilisées pour les dégâts afin d'avancer ; leur équilibrage et leurs détails seront revus plus tard.

## Décisions confirmées

- [x] Deux à quatre participants, un humain et une à trois IA simples ; un emplacement Tokyo.
- [x] Back Business/Data complet ; ViewModels pris en charge côté front. Pas de sauvegarde persistante ; pouvoirs propres aux personnages reportés.
- [x] Un exemplaire par carte Énergie, marché de trois cartes, défausse remélangée à épuisement de la pioche.
- [x] Cartes conservables pour plus tard et utilisation pendant son propre tour.
- [x] Pouvoirs permanents après activation ; les conditions telles que la présence dans Tokyo se réappliquent à chaque retour.
- [x] Veil est un Pouvoir : sa protection se réapplique à chaque retour dans Tokyo. Cette décision remplace l’expiration définitive envisagée précédemment.
- [x] Wickedness actifs dès l’acquisition pour toute la partie, sous réserve des effets explicitement ponctuels.
- [x] Cybercerveau compte une face choisie deux fois, y compris pour les brelans, une fois par tour du propriétaire ; aucun dé supplémentaire lancé.
- [x] Soins prioritaires au début du tour, avant les pertes de PV : Éternel avant Sukuna’s finger.
- [x] Divergent Fist : prochain tour seulement, une perte d’Énergie maximum par cible.
- [x] Boogie Woogie : inutilisable quand Tokyo est vide ; le sortant gagne toujours un point.
- [x] Élimination du joueur humain : Game Over immédiat.
- [x] Livret page 5 vérifié : trois lancers maximum de base, soit un premier lancer et deux relances.

- [x] Six dés physiques ; après chaque lancer, conservation libre des dés et relance des seuls dés choisis. Un dé conservé peut être repris à une relance ultérieure ; arrêt anticipé possible.
- [x] Sournois et Incantation se cumulent : cinq lancers maximum (premier lancer compris), dont un supplémentaire accordé en payant 1 Énergie pour Incantation.

- [x] **Acquisition des Wickedness** : après résolution de tous les dés, choisir une tuile disponible pour chaque seuil atteint ou dépassé (3, 6, 10), avec une seule acquisition par niveau et par joueur. Le pion reste à sa place ; la Méchanceté n’est pas dépensée. La tuile devient active immédiatement. Cybercerveau ne modifie pas les dés déjà résolus : première utilisation au prochain tour du propriétaire.

- [x] **Partie initialement à deux participants** : appliquer l’option recommandée du livret page 7 : +1 Énergie à l’entrée dans Tokyo et +1 Énergie au début du tour dans Tokyo, à la place des bonus de points de victoire. Une partie commencée à trois ou quatre conserve ses bonus de points même après réduction à deux survivants.

## Points encore ouverts

La base du moteur peut être développée. Les décisions ci-dessous restent à confirmer avant de considérer toutes les règles comme figées.

- [ ] **Moharaga’s adaptation et activation différée** : la carte prévoit explicitement le soin de 2 PV à l’achat, alors que les Pouvoirs peuvent être activés plus tard. Proposition : conserver le soin à l’achat et porter le maximum à 12 lors de l’activation ; appliquer le maximum actuel au soin reçu.
- [ ] **Recyclage des Actions** : le livret prévoit leur défausse après utilisation. Proposition pour les cartes conservées : une Action quitte la main à son utilisation et entre dans la défausse après résolution ; pour Divergent Fist, suivre son effet différé indépendamment de la carte. Confirmer ce recyclage avec la pioche remélangée.

- [x] **Infinity** : une fois le Pouvoir activé pendant son tour, son propriétaire peut payer 2 Énergies pour annuler une attaque pendant le tour adverse, dans la limite d’une utilisation par tour de table.

Les autres modalités de résolution ci-dessous sont des choix techniques provisoires, pas des questions bloquantes. Elles restent des conventions proposées tant qu’elles ne sont pas validées : compteurs réinitialisés au début du tour du propriétaire, 10 Shadows avant résolution, achat via Maki’s glasses pendant les achats, ressources et prix au minimum à zéro, maximum de PV fixé à 12 sans cumul, soin issu des dés bloqué par Binding Vow même avec Rayon stellaire, bonus de Méchanceté unique par valeur et seuil vide sans attribution différée, victoire en fin de tour selon le livret.

## Calcul des dégâts et interactions — différé, non bloquant

Pour avancer, utiliser provisoirement les nombres déjà présents dans les cartes et les tuiles. Pour les interactions non définies, les conventions arbitraires suivantes permettent d'avancer. Elles ne réécrivent pas les textes des cartes et seront révisables ; les futurs paramètres de dégâts devront être centralisés dans le moteur.

| Sujet | Convention provisoire |
| --- | --- |
| Base | Une Griffe inflige 1 dégât par cible légale. Les dégâts finaux sont au minimum de 0. |
| Bonus | Domain Expension : +1 depuis Tokyo ; Binding Vow : +2. Ces bonus concernent uniquement une attaque de dés avec au moins une Griffe. |
| Doublement | Cybercerveau double l'effet d'une Griffe choisie ; Rayon antimatière multiplie ensuite les dégâts des Griffes par 2 ; les bonus fixes sont ajoutés après. Cybercerveau compte la face choisie deux fois pour les brelans ; Rayon antimatière multiplie les dégâts sans ajouter de face. |
| Défenses | Infinity annule d'abord l'attaque si activée. Sinon, Veil retire 1 si les dégâts avant réduction sont au moins de 3, puis Special Grade retire 2 si disponible. |
| Domain Expension | Provisoirement, depuis Tokyo et avec au moins trois faces Griffe finales, l'attaque de dés ignore ces trois protections, y compris Infinity. |
| Special Grade | Consommé à sa première réduction d'une attaque positive, même si le résultat devient nul. Une attaque annulée par Infinity ne le consomme pas. |
| Dégâts de cartes | Gardent leurs valeurs écrites, sans bonus offensifs de Domain Expension ou Binding Vow ; défenses appliquées par cible. Les coûts et pertes directes de PV ne déclenchent pas ces défenses. |
| Violet | Depuis Tokyo, répartir 3 dégâts entiers entre les adversaires dehors, au choix ; zéro dégât peut être attribué à une cible. |
| Nanami's overtime | Provisoirement déclenché si aucune face Griffe n'a été résolue, conformément à la parenthèse du texte. |
| Fuite | Seulement après des dégâts effectifs de Griffes, conformément au livret. |

Les points ci-dessous sont conservés pour la révision ultérieure, sans bloquer le développement :

- [ ] **Ordre des modificateurs** : définir l'ordre entre Rayon antimatière (double les faces Griffe), bonus de Domain Expension / Binding Vow, seuil de Veil, réduction de Special Grade et annulation d'Infinity. Préciser si les bonus de dégâts concernent uniquement les Griffes ou aussi les dégâts de cartes, et s'ils exigent au moins une Griffe.
- [ ] **Domain Expension** : préciser si l'ignorance des protections sur trois Griffes s'applique seulement depuis Tokyo ou partout, si elle bloque Infinity, et si elle concerne uniquement l'attaque issue des dés.
- [ ] **Special Grade** : préciser si la réduction se déclenche sur une attaque entièrement annulée, si une attaque réduite à zéro consomme l'utilisation, et si les pertes de PV volontaires ou obligatoires sont concernées.
- [ ] **Nanami's overtime** : « aucun dégât » et « aucune face Griffe résolue » ne sont pas équivalents : Griffes sans cible, attaque annulée, ou dégâts de cartes sans Griffes. Choisir la condition exacte.
- [ ] **Violet** : préciser les répartitions autorisées des trois dégâts depuis Tokyo et l'application des protections par cible.
- [ ] **Blessure ou perte de PV** : distinguer les dégâts de Griffes des dégâts de cartes et des coûts en PV. Le livret ne permet la fuite de Tokyo qu'après blessure par les Griffes ; décider explicitement de toute exception personnalisée.

## Soin et limites : lecture des textes

- Le maximum reste à 12 avec Moharaga’s adaptation et Régénération totale : les deux effets fixent un maximum, ils n’ajoutent pas chacun 2 PV.
- Binding Vow interdit les soins provenant des dés. Le bonus de Rayon stellaire dépendant d’une face Cœur sera traité provisoirement comme un soin issu des dés ; Éternel et Régénération totale restent des soins de tuiles indépendants des dés.

## Audit du livret et des adaptations

- **Conforme au livret** : six dés, trois lancers de base, dés conservables puis reprenables, une place Tokyo jusqu’à quatre participants, achats après la résolution et l’entrée dans Tokyo, choix des tuiles et non pioche aléatoire, une acquisition par niveau, jauge non dépensée, victoire vérifiée en fin de tour pour un monstre vivant.
- **Adaptations confirmées** : choix des Wickedness après toute la résolution ; Cybercerveau compte une face deux fois et devient utilisable au tour suivant ; cartes conservables et activation différée des Pouvoirs ; Veil en Pouvoir ; cumul Sournois / Incantation jusqu’à cinq lancers ; soins prioritaires au début du tour ; Game Over immédiat à l’élimination humaine.
- **Effets ponctuels à préserver** : Régénération totale soigne intégralement à l’acquisition uniquement ; Panique générale retire les points une seule fois puis est défaussée. Leur texte ne prévoit pas de répétition de ces effets à chaque tour.
- **Écarts documentaires corrigés** : exclusion injustifiée de l’option à deux participants ; ancien format fixe de quatre participants ; affirmation trop large « aucune question bloquante restante » malgré des conventions non validées.
- **Conventions non validées** : reset des compteurs au début du tour du propriétaire, 10 Shadows avant résolution, bornes de ressources, bonus de Méchanceté pour six chiffres identiques et gestion d’un seuil sans tuile. Ces conventions ne doivent pas être présentées comme imposées par le livret.
