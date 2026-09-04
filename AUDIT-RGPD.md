# Audit RGPD : TrocSkillHub Back-end

> Branche : `audit-cyber-rgpd`
> Périmètre : entités JPA (`Models/`), migrations Flyway, controllers/services, configuration (`application.yaml`), points de log, sous-traitants.
> Rôle du projet : l'exploitant de TrocSkillHub est **responsable de traitement** ; l'équipe technique fournit les **moyens techniques**. L'audit vérifie que les moyens techniques des obligations existent dans le code. Aucune modification de code.
> Ce document est un audit technique de conformité, **pas un avis juridique**. Les décisions marquées « responsable / DPO » relèvent du responsable de traitement.

## Verdict

**Bloquant.** Un défaut du flux de réinitialisation de mot de passe permet la prise de contrôle d'un compte sans connaître le code envoyé par email, donc l'accès non autorisé à l'ensemble des données personnelles d'un utilisateur (art. 32). À corriger avant toute mise en production. S'ajoutent plusieurs écarts majeurs (journalisation de données personnelles en clair, absence de purge des comptes, code de reset faible).

Récapitulatif : **1 bloquant, 5 majeurs, 7 mineurs.**

---

## Findings

### [bloquant] Réinitialisation de mot de passe : le « token » est l'id séquentiel de la demande, le code n'est jamais revérifié à l'étape reset

- **Fichier** : `TrocSkillHub/src/main/java/RNCP/TrocSkillHub/Services/ImplServices/PasswordResetServiceImpl.java:104` et `:108-145` ; `PasswordResetRequest` id auto-incrément (`V12__create_password_reset_request_table.sql:4`)
- **Risque** : `verifyCode` renvoie `prr.getId().toString()` comme reset token, et `resetPassword` ne contrôle que `used`/`expires_at` avant de changer le mot de passe. Aucune liaison prouvant que le code à 4 chiffres a été validé. L'id étant un `BIGSERIAL` séquentiel donc devinable, un attaquant peut, dans la fenêtre de 15 min suivant n'importe quelle demande de reset (déclenchable par lui), appeler `/auth/password-reset/reset` avec un id deviné et définir un nouveau mot de passe sans jamais recevoir le code email. Résultat : prise de contrôle de compte et accès à toutes les données personnelles (identité, email, téléphone, adresse, parcours). Violation de la confidentialité du traitement, **art. 32**.
- **Qui agit** : responsable de traitement (via l'équipe technique).
- **Correction attendue** : lier l'étape reset à une vérification réussie du code (token secret aléatoire non devinable, généré uniquement après `verifyCode` OK, distinct de l'id ; marquer la demande « vérifiée » et n'autoriser reset que dans cet état) ; ne jamais utiliser l'id de ligne comme secret.

### [majeur] Adresses email des personnes journalisées en clair à chaque opération de reset

- **Fichier** : `Controllers/PasswordResetController.java:31,35,47,49,52` ; `Services/ImplServices/PasswordResetServiceImpl.java:52,66,74,80,85,91,99,103,140` ; `ImplServices/SimpleEmailService.java:36,38`
- **Risque** : l'email (donnée identifiante) est écrit en INFO/WARN à chaque request/verify/reset, y compris pour des emails inexistants (révèle une tentative sur un email donné). Ces journaux persistent et sont potentiellement transmis à un système de logs tiers. Défaut de minimisation appliqué aux logs, **art. 32 et art. 5-1-c**.
- **Qui agit** : responsable de traitement.
- **Correction attendue** : ne pas journaliser l'email en clair (identifiant interne ou valeur tronquée/hachée), abaisser le niveau, définir une durée de conservation des logs.

### [majeur] Mode debug SMTP activé : le corps de l'email, donc le code de reset, est écrit sur la sortie standard

- **Fichier** : `application.yaml:33` (`mail.properties.mail.debug: true`) ; corps contenant le code : `SimpleEmailService.java:32`
- **Risque** : `mail.debug=true` fait imprimer par JavaMail l'intégralité du dialogue SMTP, dont le texte du message qui contient le code de réinitialisation en clair et l'email du destinataire. Un secret d'authentification et une donnée personnelle se retrouvent dans les logs applicatifs. **art. 32**.
- **Qui agit** : responsable de traitement.
- **Correction attendue** : `mail.debug=false` en production ; s'assurer qu'aucun secret ne transite par les logs.

### [majeur] Journalisation verbeuse en production (Spring Security DEBUG, show-sql) exposant potentiellement des données

- **Fichier** : `application.yaml:42-44` (`org.springframework.security: DEBUG`) et `:14` (`show-sql: true`), `:18` (`format-sql: true`) ; idem `application-test.yml:24-26,12`
- **Risque** : le niveau DEBUG de Spring Security et `show-sql` augmentent fortement les traces liées à l'authentification et aux requêtes SQL sur `users`, `education`, `experience`, `project` (les valeurs restent en `?` par défaut, mais le paramétrage de trace Hibernate peut les révéler). Configuration non adaptée à un traitement de données personnelles en production, **art. 32**.
- **Qui agit** : responsable de traitement.
- **Correction attendue** : niveau INFO/WARN en prod, `show-sql: false`, séparer clairement config dev et prod.

### [majeur] Aucune durée de conservation ni purge des comptes utilisateurs

- **Fichier** : entité `User` (`Models/User.java`, champs `created_at`/`updated_at` présents mais non exploités pour une purge) ; seul scheduler existant : `Config/PasswordResetCleanupScheduler.java:27` (purge uniquement les demandes de reset expirées)
- **Risque** : les profils de type CV (identité, coordonnées, parcours détaillé) sont conservés sans limite de durée ni mécanisme d'inactivité. Aucun code ne supprime ou n'anonymise les comptes dormants. Manquement à la limitation de conservation, **art. 5-1-e**.
- **Qui agit** : responsable de traitement (fixer la durée) + technique (implémenter la purge/anonymisation).
- **Correction attendue** : définir une durée (ex X ans après dernière activité via `updated_at`), implémenter un job de purge/anonymisation, prévoir une information préalable de l'utilisateur.

### [majeur] Code de réinitialisation faible : 4 chiffres générés avec java.util.Random

- **Fichier** : `Services/ImplServices/PasswordResetServiceImpl.java:29,57` (`new Random()`, `String.format("%04d", random.nextInt(10000))`)
- **Risque** : espace de seulement 10 000 valeurs, générateur non cryptographique (`java.util.Random` prédictible). Le garde-fou est `max-attempts=5` (`application.yaml:58`) et un rate limiter en mémoire (3 requêtes/30 min par email, non partagé entre instances). Combiné au finding bloquant, la robustesse du reset est insuffisante, **art. 32**.
- **Qui agit** : responsable de traitement.
- **Correction attendue** : utiliser `SecureRandom`, code plus long (6 à 8 caractères), verrouillage après échecs, rate limiting persistant.

### [mineur] Données de personnes (fictives) et hash commités dans les migrations de seed

- **Fichier** : `db/migration/V2__seed_add_data_users.sql:6-8` ; `V3__add_ada_lovelace_user.sql:5`
- **Risque** : noms, emails `@example.fr` et hash bcrypt insérés en base à chaque déploiement. Les personnes sont manifestement fictives (Jean Martin, Sophie Lefebvre, Ada Lovelace) et les emails en `example.fr` sont réservés aux tests, donc pas de donnée réelle exposée. Reste une mauvaise pratique : ces comptes de démonstration seront créés en production et les hash sont versionnés.
- **Qui agit** : responsable de traitement.
- **Correction attendue** : ne pas charger de comptes de démonstration en production (profil de migration séparé), retirer les hash du dépôt.

### [mineur] Dump SQL commité à la racine

- **Fichier** : `TrocSkillHub/backup_avant_flyway.sql:80-81` (bloc `COPY ... FROM stdin` vide) ; `:26,46,60` (propriétaire `MoSa`)
- **Risque** : le dump ne contient aucune ligne de données personnelles (le `COPY` est vide), donc pas de fuite de données utilisateurs. Il révèle le nom du propriétaire de base `MoSa` et le schéma. Impact faible.
- **Qui agit** : responsable de traitement.
- **Correction attendue** : ne pas versionner de dumps de base ; si conservé, le sortir du dépôt applicatif.

### [mineur] JWT non révocable côté serveur, et token renvoyé dans le corps de réponse

- **Fichier** : `Controllers/AuthController.java:121-130` (logout efface seulement le cookie) ; `:111-113` (login renvoie `token` dans le body) ; `JwtService.java` (aucune denylist)
- **Risque** : à la déconnexion le JWT reste valide jusqu'à expiration (24 h), sans liste de révocation. Le token est aussi renvoyé dans le corps JSON alors que le cookie est `httpOnly`, ce qui invite le front à le stocker côté client et annule la protection XSS du cookie. Défaut de maîtrise des accès, **art. 32**.
- **Qui agit** : responsable de traitement.
- **Correction attendue** : mécanisme de révocation (denylist / rotation / durée courte + refresh), ne pas exposer le token dans le corps si le cookie httpOnly est utilisé.

### [mineur] /auth/register n'applique pas la politique de mot de passe et imprime la stacktrace

- **Fichier** : `Controllers/AuthController.java:63` (`encode(password)` sans `validatePasswordStrength`) ; `:85` (`e.printStackTrace()`)
- **Risque** : `/auth/register` encode le mot de passe sans contrôle de robustesse, alors que `POST /users` le valide (`UserServiceImpl.java:56`). Incohérence affaiblissant la sécurité des identifiants. `printStackTrace` écrit une trace technique sur stderr. **art. 32**, impact limité.
- **Qui agit** : responsable de traitement.
- **Correction attendue** : appliquer la même validation, remplacer `printStackTrace` par un log maîtrisé sans donnée personnelle.

### [mineur] Exposition du nom complet et de la localisation de tous les utilisateurs aux membres authentifiés

- **Fichier** : `Controllers/UserController.java:35-40` ; `DTOs/UserPublicResponseDTO.java:6-13` ; `SecurityConfig.java:54` (`GET /users` authenticated)
- **Risque** : `GET /users` renvoie `firstName`, `lastName`, `city`, `country`, compétences et besoins de tous les utilisateurs à tout membre connecté. Le scoping des données sensibles est correct (email, téléphone, adresse ne sortent que sur `/users/me`, avec `address` en WRITE_ONLY), mais l'exposition du nom de famille complet plus la ville pourrait être minimisée pour la seule finalité de mise en relation. **art. 5-1-c**.
- **Qui agit** : responsable de traitement.
- **Correction attendue** : évaluer si le nom de famille complet est nécessaire dans la liste interne ; sinon réduire (prénom + initiale, ou ville sans nom complet).

### [mineur] Champs libres pouvant capter des données sensibles (art. 9), sans encadrement

- **Fichier** : `Models/User.java:51-52` (`description` TEXT) ; `Models/Project.java:17` (`description` TEXT), `:20` (`links`) ; `Models/Experience.java:14-17` ; `Models/Education.java:14-17`
- **Risque** : les champs libres (bio, description de projet, employeur, établissement) peuvent révéler des opinions, engagements, appartenance syndicale, santé, etc. (**art. 9**). Rien dans le code ne les filtre ni n'avertit l'utilisateur. Risque inhérent à un profil de type CV.
- **Qui agit** : responsable de traitement / DPO.
- **Correction attendue** : mention d'information invitant à ne pas renseigner de données sensibles ; décision juridique sur la base légale et le traitement de ces champs.

### [mineur, à confirmer] Actuator présent

- **Fichier** : `pom.xml:38` (`spring-boot-starter-actuator`) ; aucune section `management` dans `application.yaml`
- **Risque** : la dépendance Actuator est incluse. Sans configuration `management.endpoints.web.exposure`, seuls `health` et `info` sont exposés en HTTP, et `anyRequest().authenticated()` (`SecurityConfig.java:60`) les protège. Pas d'exposition de config/env constatée.
- **Qui agit** : responsable de traitement.
- **Correction attendue** : garder l'exposition minimale, protéger explicitement `/actuator/**`.

---

## Points conformes constatés (à conserver)

- Mots de passe hachés (BCrypt via `PasswordConfig`ᵃ, code de reset stocké haché, colonne `code_hash`). *(ᵃ l'audit sécurité relève un hachage Argon2id sur les mots de passe utilisateur ; à réconcilier selon la configuration effective de `PasswordConfig`.)*
- `@JsonIgnore` sur `password` (`User.java:35`) : le hash n'est jamais sérialisé.
- Secrets externalisés en variables d'environnement (datasource, jwt, mail) ; `.env` gitignoré et non suivi par git ; aucun secret en dur dans docker-compose.
- **Droit à l'effacement techniquement présent** : `DELETE /users/me` scindé sur l'utilisateur authentifié, cascade JPA `CascadeType.ALL` + `orphanRemoval` sur education/experience/project/userKnowledge (`User.java:70-80`) et `ON DELETE CASCADE` sur password_reset_request (V12).
- Purge des demandes de reset expirées (`PasswordResetCleanupScheduler.java:27`, toutes les heures).
- Endpoints scindés sur l'utilisateur courant (`/me`) via `resolveOwnId` : pas d'accès aux données d'un autre par id dans l'URL.
- Réponses anti-énumération sur `/password-reset/request` (message générique).
- CORS en allowlist par variable d'environnement, pas de wildcard ; SMTP par défaut `smtp.mail.ovh.net` (OVH, UE probable).

---

## Cartographie des données personnelles touchées

| Donnée | Finalité | Base de conservation constatée dans le code | Destinataire |
|---|---|---|---|
| Nom, prénom (User.first_name/last_name) | Identité du profil, mise en relation | Illimitée (aucune purge) | Tous membres authentifiés (liste), hébergeur/BDD |
| Email (User.email) | Authentification, contact, reset | Illimitée | Propre profil (/me), serveur SMTP, **journaux applicatifs (en clair)** |
| Mot de passe haché (User.password) | Authentification | Illimitée | Base uniquement (non sérialisé) |
| Adresse, ville, pays | Localisation pour mise en relation | Illimitée | ville/pays exposés à tous ; adresse au propre profil |
| Téléphone (phone_number) | Contact | Illimitée | Propre profil (/me) |
| Photo (picture, bytea) | Illustration du profil | Illimitée | Propre profil |
| Bio / descriptions libres | Présentation, portfolio | Illimitée | Champ libre pouvant capter de l'art. 9 |
| Parcours : formations, expériences, projets | Profil de compétences type CV | Illimitée (cascade suppression avec le compte) | Propre profil ; potentielles données art. 9 |
| Compétences et besoins (UserKnowledge) | Matching d'échange de compétences | Illimitée | Tous membres authentifiés |
| Code de reset + tentatives (PasswordResetRequest) | Réinitialisation de mot de passe | Purge horaire après expiration (15 min) | Base + email + **logs (mode debug SMTP)** |
| Rôle (users.role, V4) | Contrôle d'accès admin | Illimitée | Base |

Sous-traitants ultérieurs (**art. 28**) : fournisseur SMTP (OVH par défaut, localisation UE à confirmer), hébergeur (Docker/PostgreSQL, localisation UE à déterminer). À contractualiser.

---

## À porter au registre / à la doc du responsable

- Finalités et base légale du traitement (mise en relation, gestion de profils/comptes) et des champs libres susceptibles de contenir de l'art. 9 ; mention d'information invitant à ne pas y saisir de données sensibles.
- Durées de conservation à définir : comptes utilisateurs (aucune actuellement, **art. 5-1-e**), logs applicatifs, sauvegardes ; mécanisme de purge/anonymisation des comptes inactifs à implémenter.
- Modalités d'exercice des droits : effacement (`DELETE /users/me` existe), rectification (`PUT/PATCH /users/me`), accès (`GET /users/me`), et portabilité **art. 20** : pas d'export structuré dédié, seul `/me` renvoie un JSON du profil ; prévoir un export réutilisable si nécessaire.
- Sous-traitants (**art. 28**) : fournisseur SMTP (OVH par défaut, localisation UE à confirmer et à contractualiser), hébergeur (Docker/PostgreSQL, localisation UE à déterminer), avec clauses art. 28 et vérification d'absence de transfert hors UE (**art. 44+**).
- Politique de journalisation : exclure email, code de reset et tout secret des logs ; désactiver `mail.debug`, `show-sql` et `security: DEBUG` en production ; durée de conservation des logs.
- Politique de gestion des secrets et de révocation des tokens (JWT), robustesse et cycle de vie du code de reset.

---

## Top 3 à traiter en priorité

1. **[bloquant] Reset de mot de passe contournable** (token = id séquentiel, code jamais revérifié) → prise de contrôle de compte (art. 32).
2. **Email et code de reset journalisés en clair**, aggravés par `mail.debug=true`, `show-sql=true` et Spring Security en DEBUG (art. 32).
3. **Aucune durée de conservation ni purge des comptes utilisateurs** (art. 5-1-e).

Donnée la plus à risque : l'ensemble du profil accessible via un compte pris en main par le contournement du reset ; l'email est l'élément le plus exposé (identifiant journalisé en clair et pivot de l'attaque).
