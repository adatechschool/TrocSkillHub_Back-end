# Audit sécurité (OWASP) : TrocSkillHub Back-end

> Branche : `audit-cyber-rgpd`
> Périmètre : API REST Spring Boot (controllers, config sécurité JWT, services, repositories), configuration (`application.yaml`), migrations Flyway.
> Méthode : audit statique selon OWASP API Security Top 10 (2023) et OWASP Top 10 web. Aucune modification de code. Chaque finding est vérifié dans le code et assorti d'un scénario d'exploitation.

## Verdict

**Bloquant exploitable.** Une prise de contrôle de compte à distance est possible sans authentification via le flux de réinitialisation de mot de passe. Corrections obligatoires avant toute mise en production.

Récapitulatif : **1 bloquant, 5 majeurs, 3 mineurs.**

---

## Findings

### [bloquant] Réinitialisation de mot de passe contournable : jeton = identifiant séquentiel, le code n'est jamais vérifié à /reset (OWASP API1 — BOLA / API2 — Broken Authentication)

- **Fichier** : `TrocSkillHub/src/main/java/RNCP/TrocSkillHub/Services/ImplServices/PasswordResetServiceImpl.java:104` (`return prr.getId().toString();`) et `:107-145` (`resetPassword`)
- **Faille** : le « reset token » renvoyé par `/verify` est simplement la clé primaire auto-incrémentée (`Long`, `GenerationType.IDENTITY`) de la ligne `password_reset_request`. À l'étape `/reset`, `resetPassword` fait `Long.parseLong(resetToken)` puis `findById(prrId)` et se contente de vérifier `!used && !expired` avant de changer le mot de passe de `prr.getUser()`. Aucune revérification du code à 4 chiffres, ni de l'email, ni de propriété du jeton. Le secret réel (le code envoyé par email à la victime) n'intervient pas dans `/reset`.
- **Scénario d'exploitation** : un attaquant non authentifié appelle `POST /auth/password-reset/request {"email":"victime@x.com"}` (réponse générique, mais une ligne est créée pour la victime, `used=false`, valable 15 min). Les ids étant séquentiels, il appelle ensuite `POST /auth/password-reset/reset {"resetToken":"<N>","newPassword":"Pirate1!","confirmPassword":"Pirate1!"}` en énumérant quelques valeurs proches de l'id courant. Dès qu'il touche la ligne non utilisée et non expirée, le mot de passe de la victime est remplacé. Prise de contrôle de n'importe quel compte (y compris un futur admin), sans jamais connaître le code à 4 chiffres.
- **Correction attendue** : le jeton remis après `/verify` doit être un secret aléatoire fort (≥128 bits, `SecureRandom`, stocké haché), non devinable, distinct de l'id. `/reset` doit exiger ce jeton opaque, vérifier qu'il correspond à une demande en état « code validé », à usage unique, lié à l'utilisateur. Ne jamais exposer la PK comme jeton.

### [majeur] Rôle admin totalement non appliqué + fonctions privilégiées ouvertes (catégories/knowledges) (OWASP API5 — Broken Function Level Authorization / API3)

- **Fichier** : `SecurityConfig.java:46-52` ; `CustomUserDetailsService.java:28-29` ; `CategoryController.java:32` ; `KnowledgeController.java:37-56` ; `Models/User.java` (aucun champ `role` mappé)
- **Faille** : trois problèmes cumulés. (1) `CustomUserDetailsService.loadUserByUsername` appelle `getGrantedAuthorities(null)` en dur : tout utilisateur, admin compris, reçoit une liste d'autorités VIDE. (2) L'entité `User` ne mappe pas la colonne `role` ajoutée en V4 : le rôle n'est jamais chargé. (3) `SecurityConfig` ne contient aucun `hasRole`/`@PreAuthorize`. Conséquence : `POST /categories` est en `permitAll()` (création sans authentification) et `POST/PUT/DELETE /knowledges` sont en `authenticated()` seulement. Catégories et knowledges sont des données de référence PARTAGÉES par tous les profils.
- **Scénario d'exploitation** : un utilisateur lambda authentifié (ou anonyme pour les catégories) appelle `DELETE /knowledges/12` et supprime une compétence de référence utilisée dans les profils d'autres utilisateurs, ou pollue le référentiel via `POST /categories` sans compte. Le rôle `ADMIN` prévu (V4) n'a aucun effet. Corruption/altération de données partagées, déni de service fonctionnel.
- **Correction attendue** : mapper `role` dans l'entité, charger les vraies autorités dans `CustomUserDetailsService`, réserver `POST/PUT/DELETE /knowledges` et `POST /categories` à `hasRole("ADMIN")`.

### [majeur] Aucune limitation de débit / anti-bruteforce sur /auth/login (OWASP API2 — Broken Authentication)

- **Fichier** : `AuthController.java:90-119` ; `RateLimiter.java` (utilisé uniquement dans `PasswordResetController.java:30`)
- **Faille** : `login` n'a aucune protection anti-bruteforce. Le `RateLimiter` n'est branché que sur `/auth/password-reset/request`. De plus ce `RateLimiter` est une `HashMap` en mémoire non thread-safe (accès concurrents non synchronisés), non partagée entre instances et remise à zéro à chaque redémarrage ; ses constantes en dur (3 / 30 min) ignorent `password-reset.max-attempts` configuré.
- **Scénario d'exploitation** : un attaquant lance du credential stuffing ou du bruteforce sur `POST /auth/login` sans aucune borne (ni délai, ni verrouillage, ni captcha). Compromission de comptes à mots de passe faibles.
- **Correction attendue** : limiter les tentatives de login (par IP + par compte) avec un store partagé (Redis/bucket), verrouillage temporaire, et rendre le RateLimiter thread-safe/distribué.

### [majeur] Code de réinitialisation faible : 4 chiffres générés avec java.util.Random (OWASP API2 — Broken Authentication)

- **Fichier** : `PasswordResetServiceImpl.java:29` (`new Random()`), `:57` (`String.format("%04d", random.nextInt(10000))`)
- **Faille** : le code de reset n'a que 10 000 valeurs possibles et est produit par `java.util.Random`, un PRNG non cryptographique et prédictible (seed dérivable). Même hors du bloquant ci-dessus, l'espace est trop petit.
- **Scénario d'exploitation** : si le flux `/verify` était corrigé pour être la seule voie, un attaquant qui déclenche un reset sur un compte cible peut tenter les 10 000 codes ; la borne `maxAttempts=5` par demande ralentit mais reste contournable en enchaînant plusieurs demandes. Prédiction possible via l'observation d'autres sorties du `Random`.
- **Correction attendue** : utiliser `SecureRandom`, code d'au moins 6 chiffres (idéalement token alphanumérique long), durcir le comptage d'échecs global.

### [majeur] Journalisation verbeuse et fuite de données sensibles en logs (OWASP API8 / A09 — Security Logging)

- **Fichier** : `application.yaml:14` (`show-sql: true`), `:18` (`format-sql: true`), `:33` (`mail.debug: true`), `:42-44` (`org.springframework.security: DEBUG`) ; `PasswordResetController.java:60,62` (log du reset token) ; `PasswordResetServiceImpl` et `SimpleEmailService` (emails loggés partout)
- **Faille** : en configuration de production `show-sql`/`format-sql` déversent les requêtes SQL, `spring.security` est en `DEBUG`, `mail.debug=true` imprime tout le dialogue SMTP (donc le code de reset et l'email en clair), et le controller logue le « reset token » et les adresses email à chaque étape.
- **Scénario d'exploitation** : quiconque a accès aux logs (exploitant, agrégateur, fuite de fichier log) récupère des jetons de reset et des données personnelles (emails), ce qui, combiné au bloquant, facilite encore la prise de compte.
- **Correction attendue** : `show-sql:false`, `mail.debug:false`, niveau `INFO`/`WARN` en prod, ne jamais logger jetons/codes/emails en clair.

### [majeur] Authentification par cookie JWT avec CSRF désactivé et SameSite=None (OWASP API8 — Security Misconfiguration / A01) — à confirmer dynamiquement

- **Fichier** : `SecurityConfig.java:38` (`csrf.disable()`) ; `JwtAuthFIlter.java:52-58` (token lu depuis le cookie `jwt`) ; `AuthController.java:101-107` (cookie `sameSite=None`, `secure`, `httpOnly`)
- **Faille** : le filtre accepte le JWT depuis un cookie envoyé automatiquement par le navigateur, la protection CSRF est désactivée et le cookie de login est en `SameSite=None`. Les endpoints d'écriture (`PUT/PATCH/DELETE /users/me`) deviennent des cibles CSRF potentielles.
- **Scénario d'exploitation** : un site malveillant visité par une victime connectée déclenche une requête cross-site vers l'API ; le navigateur joint le cookie `jwt`. L'exploitation réelle est atténuée par le préflight CORS sur les corps JSON et la restriction d'origines de `CorsConfig` : à confirmer selon la valeur réelle de `CORS_ALLOWED_ORIGINS`. Reste une faiblesse de conception.
- **Correction attendue** : soit n'utiliser que l'en-tête `Authorization: Bearer` (pas de cookie d'auth), soit activer une protection CSRF (double-submit token) et `SameSite=Strict/Lax`.

### [mineur] /auth/register contourne la politique de mot de passe et ne valide aucune entrée (OWASP API8 / A04 — Insecure Design)

- **Fichier** : `AuthController.java:44-89` (corps `Map<String,String>`, `save` direct, `encode(password)`), aucun `@Valid` ni jakarta-validation dans le projet
- **Faille** : `register` prend un `Map` brut, n'appelle pas `validatePasswordStrength` (contrairement à `POST /users` via `createUser`), ne valide ni le format email ni la présence des champs, et n'a pas de `@Valid`. Un mot de passe vide/faible ou un email invalide est accepté. `NullPointerException` possible si des champs manquent (capturée en 500 avec `printStackTrace()`).
- **Correction attendue** : DTO validé (`@NotBlank`, `@Email`, `@Size`), appliquer `validatePasswordStrength` sur register, supprimer `printStackTrace`.

### [mineur] Absence de révocation de JWT et jeton renvoyé dans le corps (OWASP API2)

- **Fichier** : `JwtService.java:23-30` (HS256, pas d'issuer/audience) ; `AuthController.java:111-113` (`token` dans le body) ; `AuthController.java:121-130` (`logout` efface seulement le cookie)
- **Faille** : `logout` ne fait qu'expirer le cookie ; aucune liste de révocation. Le token est aussi renvoyé dans le corps JSON, donc un jeton volé reste valable jusqu'à expiration et le logout ne l'invalide pas. Pas de rotation après reset de mot de passe.
- **Correction attendue** : liste de révocation/blacklist (jti) ou tokens courts + refresh, invalidation des sessions après changement de mot de passe.

### [mineur] Cookie de register non Secure / sans SameSite + @CrossOrigin en dur (OWASP API8)

- **Fichier** : `AuthController.java:71-77` (cookie `jwt` de register sans `secure`, sans `sameSite`) ; `UserController.java:25` (`@CrossOrigin(origins="http://localhost:5173")` en dur)
- **Faille** : le cookie posé par `register` n'est ni `Secure` ni `SameSite`, contrairement à celui de `login`. Le `@CrossOrigin` en dur fige une origine de dev dans le code, en doublon avec `CorsConfig`.
- **Correction attendue** : cookie `Secure`+`SameSite` cohérent, retirer l'origine en dur au profit de la config centralisée.

---

## Surface d'attaque (endpoints audités)

| Route | Méthode | Rôle/scope attendu | Statut protection |
|---|---|---|---|
| `/auth/register` | POST | public | Validation d'entrée et politique mot de passe absentes (mineur) |
| `/auth/login` | POST | public | Pas de rate limiting (majeur) |
| `/auth/logout` | POST | public | Pas de révocation réelle (mineur) |
| `/auth/me` | GET | token cookie | Signature vérifiée (OK) |
| `/auth/password-reset/request` | POST | public | Anti-énumération OK, rate limiter faible/en mémoire |
| `/auth/password-reset/verify` | POST | public | Renvoie la PK comme jeton (design cassé) |
| `/auth/password-reset/reset` | POST | public | **BLOQUANT** : ne vérifie pas le code, jeton = id séquentiel → ATO |
| `/users` | GET | authenticated | Renvoie `UserPublicResponseDTO` (pas de PII d'autrui) : sûr |
| `/users` | POST | authenticated | Passe par `createUser` (validation OK) : sûr |
| `/users/me` | GET/PUT/PATCH/DELETE | authenticated | Id résolu depuis le token, pas d'id dans l'URL : pas de BOLA, sûr |
| `/categories` | GET | public | OK ; `POST /categories` en permitAll → BFLA (majeur) |
| `/knowledges`, `/knowledges/{id}` | GET | public | OK ; `POST/PUT/DELETE` authenticated sans rôle → BFLA (majeur) |
| `/actuator/**` | — | authenticated | `anyRequest().authenticated()`, seul `/health` par défaut : risque faible |

---

## Points sûrs notables (à ne pas régresser)

- **Pas de BOLA sur les données de profil** : toutes les écritures utilisateur passent par `/users/me` avec l'id résolu depuis le token ; aucun endpoint d'écriture prenant un id pour user/education/experience/project. Éducation/expérience/projet synchronisés en collections liées au propriétaire.
- **Pas de mass assignment de privilège** : `UserRequestDTO` ne contient ni `role` ni `id` ; `UserMapper.toEntity` ignore explicitement `id` et les collections.
- **Hachage Argon2id** (paramètres 16/32/1/19456/2) via `PasswordConfig`. Code de reset stocké haché.
- **Aucune injection SQL/JPQL** : aucun `@Query`, uniquement des méthodes dérivées Spring Data paramétrées.
- `password` annoté `@JsonIgnore` ; `UserResponseDTO` (email/téléphone) renvoyé uniquement sur le profil propre ; `GET /users` public sans PII.
- **Secrets externalisés** en variables d'environnement (aucun secret en dur dans le code, docker-compose ni la CI) ; `.env` gitignoré.
- Anti-énumération sur `/password-reset/request` et sur `login` (messages génériques).
- `ddl-auto: validate` (pas de `update`/`create` en prod).
- `backup_avant_flyway.sql` versionné mais **sans aucune ligne de données** (COPY vide) : divulgation de structure uniquement. Seed V2/V3 = comptes fictifs avec hash bcrypt factices (l'app hache en Argon2, ces comptes ne peuvent pas se connecter).

---

## Non vérifiable statiquement (à traiter en DAST / pentest / revue infra)

- Force réelle du `JWT_SECRET` et valeur de `JWT_EXPIRATION` en prod (fournis par l'environnement).
- Valeur réelle de `CORS_ALLOWED_ORIGINS` en prod (détermine l'exploitabilité concrète du risque CSRF).
- Exposition réseau effective d'Actuator derrière le reverse proxy et endpoints Actuator éventuellement activés par variable d'environnement.
- Contenu réel de la base seedée en prod (les migrations V2/V3 s'exécutent-elles hors test ?).
- Exploitabilité CSRF end-to-end (dépend des content-types acceptés et du préflight CORS).

---

## Top 3 à traiter en priorité

1. **[bloquant] Reset de mot de passe contournable** (API1/API2) — jeton = id séquentiel, code jamais revérifié → account takeover à distance sans auth.
2. **Rôle admin jamais chargé/appliqué + `POST /categories` permitAll et `/knowledges` mutables par tout authentifié** (API5) — altération de données partagées.
3. **Aucun anti-bruteforce sur `/auth/login`** (API2).

Flux le plus exposé : `POST /auth/password-reset/reset`.
