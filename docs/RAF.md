# Reste à faire — arbitrages des règles

Points relevés dans `Rules-Cards`, la photo initiale des tuiles Wickedness (supprimée du dépôt après transcription) et le livret français Dark Edition. Ce fichier recense des décisions à prendre ; il ne modifie pas les textes sources. Nadir autorise des valeurs et conventions provisoires pour les dégâts afin d'avancer ; leur équilibrage et leurs détails seront revus plus tard.

## Périmètre et matériel exclu

- [x] **Cybercerveau — durée et effet validés par Nadir** : à chacun des tours du propriétaire, pendant toute la partie après acquisition, doubler l’effet d’un dé choisi une fois, sans compter une deuxième face pour les brelans. Aucun dé supplémentaire. Cette précision remplace la limitation précédente au seul tour d’acquisition.
- [ ] **Cybercerveau — résolution** : préciser le moment du choix (la tuile peut être acquise pendant la résolution des dés), l’effet sur une face chiffrée et le cumul avec Rayon antimatière / Rayon stellaire.
- [ ] **Catalogue et quantités** : préciser le nombre d'exemplaires de chacune des 20 cartes personnalisées, et le traitement de la pioche épuisée (marché partiel ou recyclage de la défausse). Confirmer si les dix faces rouges de Wickedness constituent tout le catalogue retenu.
- [ ] **Distribution des cartes** : les sources décrivent un marché d'achat, pas une distribution automatique à chaque tour. Si « cartes de chaque tour » désigne une mécanique différente, la définir.

## Durée et fenêtre de jeu

- [ ] **Veil** : déclaré Action, mais effet continu sans échéance. Définir sa durée et son déclenchement, ou décider explicitement d'un changement de type.
- [ ] **Black Flash** : préciser s'il agit immédiatement à l'achat sur les dés déjà résolus, ou s'il peut être conservé et joué plus tard. Définir ses cibles : tous les adversaires ou seulement les cibles habituelles des Griffes ; faut-il avoir infligé des dégâts auparavant ?
- [ ] **Divergent Fist** : préciser si la perte d'Énergie concerne la première blessure ou chaque blessure du prochain tour, si elle touche chaque cible blessée et si les dégâts de cartes comptent. Définir son expiration à la fin du prochain tour même sans blessure.
- [ ] **Tour de table** pour Inifnity et Special Grade : définir le moment de réinitialisation des compteurs, notamment lors de l'achat et après élimination d'un joueur.
- [ ] **10 Shadows** : préciser quand transformer le dé, avant verrouillage du résultat final ou à une autre étape, et si « par tour » concerne uniquement le tour du propriétaire.
- [ ] **Maki's glasses** : préciser la fenêtre d'achat de la carte du dessus ; « à tout moment » peut désigner uniquement sa consultation. Préciser comment rendre l'information connue à l'IA et au joueur sans révéler toute la pioche.

## Calcul des dégâts et interactions — différé, non bloquant

À la demande de Nadir, utiliser provisoirement les nombres déjà présents dans les cartes et les tuiles. Pour les interactions non définies, les conventions arbitraires suivantes permettent d'avancer. Elles ne réécrivent pas les textes des cartes et seront révisables ; les futurs paramètres de dégâts devront être centralisés dans le moteur.

| Sujet | Convention provisoire |
| --- | --- |
| Base | Une Griffe inflige 1 dégât par cible légale. Les dégâts finaux sont au minimum de 0. |
| Bonus | Domain Expension : +1 depuis Tokyo ; Binding Vow : +2. Ces bonus concernent uniquement une attaque de dés avec au moins une Griffe. |
| Doublement | Cybercerveau double l'effet d'une Griffe choisie ; Rayon antimatière multiplie ensuite les dégâts des Griffes par 2 ; les bonus fixes sont ajoutés après. Aucun doublement ne crée de face pour les brelans. |
| Défenses | Inifnity annule d'abord l'attaque si activée. Sinon, Veil retire 1 si les dégâts avant réduction sont au moins de 3, puis Special Grade retire 2 si disponible. |
| Domain Expension | Provisoirement, depuis Tokyo et avec au moins trois faces Griffe finales, l'attaque de dés ignore ces trois protections, y compris Inifnity. |
| Special Grade | Consommé à sa première réduction d'une attaque positive, même si le résultat devient nul. Une attaque annulée par Inifnity ne le consomme pas. |
| Dégâts de cartes | Gardent leurs valeurs écrites, sans bonus offensifs de Domain Expension ou Binding Vow ; défenses appliquées par cible. Les coûts et pertes directes de PV ne déclenchent pas ces défenses. |
| Violet | Depuis Tokyo, répartir 3 dégâts entiers entre les adversaires dehors, au choix ; zéro dégât peut être attribué à une cible. |
| Nanami's overtime | Provisoirement déclenché si aucune face Griffe n'a été résolue, conformément à la parenthèse du texte. |
| Fuite | Seulement après des dégâts effectifs de Griffes, conformément au livret. |

Les points ci-dessous sont conservés pour la révision ultérieure, sans bloquer le développement :

- [ ] **Ordre des modificateurs** : définir l'ordre entre Rayon antimatière (double les faces Griffe), bonus de Domain Expension / Binding Vow, seuil de Veil, réduction de Special Grade et annulation d'Inifnity. Préciser si les bonus de dégâts concernent uniquement les Griffes ou aussi les dégâts de cartes, et s'ils exigent au moins une Griffe.
- [ ] **Domain Expension** : préciser si l'ignorance des protections sur trois Griffes s'applique seulement depuis Tokyo ou partout, si elle bloque Inifnity, et si elle concerne uniquement l'attaque issue des dés.
- [ ] **Special Grade** : préciser si la réduction se déclenche sur une attaque entièrement annulée, si une attaque réduite à zéro consomme l'utilisation, et si les pertes de PV volontaires ou obligatoires sont concernées.
- [ ] **Nanami's overtime** : « aucun dégât » et « aucune face Griffe résolue » ne sont pas équivalents : Griffes sans cible, attaque annulée, ou dégâts de cartes sans Griffes. Choisir la condition exacte.
- [ ] **Violet** : préciser les répartitions autorisées des trois dégâts depuis Tokyo et l'application des protections par cible.
- [ ] **Blessure ou perte de PV** : distinguer les dégâts de Griffes des dégâts de cartes et des coûts en PV. Le livret ne permet la fuite de Tokyo qu'après blessure par les Griffes ; décider explicitement de toute exception personnalisée.

## Soin et effets simultanés

- [ ] **Binding Vow + Rayon stellaire** : préciser si le soin supplémentaire issu d'une face Cœur est lui aussi interdit. Binding Vow autorise explicitement les cartes mais ne précise pas les tuiles.
- [ ] **Maximum de PV** : préciser le cumul de Moharaga's adaptation et Régénération totale. Les deux textes fixent un maximum de 12, sans annoncer +2 cumulables ; éviter de coder un maximum de 14 sans décision explicite.
- [ ] **Début de tour** : définir l'ordre d'Éternel, Infatigable, Sukuna's finger et du bonus de Tokyo. À 1 PV, soigner avant la perte de Sukuna peut changer l'élimination.
- [ ] **Sournois + Incantation** : Sournois ajoute un lancer ; Incantation accorde une « troisième relance ». Préciser si les effets se cumulent jusqu'à cinq lancers ou si Incantation ne fait qu'autoriser la troisième relance déjà disponible avec Sournois.

## Tokyo, points et fin de partie

- [ ] **Boogie Woogie** : préciser la cible légale quand le propriétaire est dans Tokyo, et l'attribution du point de sortie dans chaque sens. La règle de base ne donne aucun point pour sortir : l'effet personnalisé doit être explicite. Définir le comportement si Tokyo est vide.
- [ ] **Bornes des ressources** : préciser le plancher des points de victoire après Panique générale et le plancher du prix après Repaire maléfique en présence d'éventuelles autres réductions.
- [ ] **Méchanceté** : préciser le gain pour six faces identiques (un ou deux triples), le traitement d'un seuil franchi lorsque sa réserve est vide et la possibilité de réclamer une tuile plus tard. Définir la gestion de plusieurs seuils franchis pendant une même résolution.
- [ ] **Fin de partie** : le PDF arrête la partie en fin de tour et exige un gagnant vivant. Confirmer cette base pour la variante et départager plusieurs joueurs vivants à 20 points si de futurs effets le permettent. Définir la clôture du tour si le joueur actif meurt pendant son propre tour.
