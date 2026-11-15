# 🦉 École Fun — gestion d'école primaire (mono-école)

Application web **Java 21 / Spring Boot 3** avec une interface colorée et ludique
(mascotte, étoiles, badges, confettis). Elle implémente le noyau MVP du cahier des charges.

## Lancer en local

Prérequis : JDK 21 et Maven 3.9+.

```bash
mvn spring-boot:run
```

Puis ouvrir http://localhost:8080 (base H2 locale dans `./data`, jeu de données de démonstration créé au premier lancement).

| Rôle | Identifiant | Mot de passe |
|---|---|---|
| Direction | `admin` | `admin123` |
| Enseignant | `enseignant` | `ens123` |
| Parent | `parent` | `parent123` |
| Comptable / Secrétariat | `compta` | `compta123` |
| Élève | `eleve` | `eleve123` |

Pour repartir de zéro : arrêter l'application et supprimer le dossier `data/`.

## Ce qui est fait (MVP)

- **Rôles (RBAC)** : Direction, Enseignant, Parent, Élève, Comptable ; mots de passe chiffrés (BCrypt), HTTPS à activer en production.
- **CRUD + journalisation** (table `audit_log`) : comptes (enseignants, parents, élèves…), élèves, classes, matières, notes, paiements.
- **Suppressions contrôlées + suppression logique** : classe avec élèves, matière avec notes, parent avec enfants → refus explicite ; le reste est archivé (`deleted`).
- **Effectifs** : une classe pleine refuse de nouvelles affectations.
- **Notes et moyennes** : moyenne par matière puis moyenne générale pondérée par les coefficients, bulletin imprimable (Imprimer → Enregistrer en PDF).
- **Paiements** : solde par élève, impayés, relance en un clic, reçu numéroté (`R-AAAA-00001`), et **message automatique au parent** (montant, date, mode, nouveau solde, lien du reçu) après chaque paiement.
- **Messagerie** : reçus / envoyés / brouillons, droits par rôle, messages envoyés **immuables**, masquage côté destinataire uniquement, envoi groupé aux parents (Direction), journal des consultations.
- **Parents / élèves** : voient uniquement leurs propres enfants (fiche, bulletin, paiements, reçus).
- **Fun** : étoiles selon la moyenne, badges, avatars animaux, confettis après chaque enregistrement, clic sur le hibou 🦉.
- **PWA** : manifeste + service worker (installable sur mobile).
- `annee_scolaire` présente sur classes, notes et paiements.

## Pas encore fait (V2 / Should)

Inscription en ligne par les parents, import/export CSV, emploi du temps avec détection de conflits, salles, devoirs (homework), annonces générales, notifications push/e-mail (FCM / SendGrid), reçus et bulletins en vrai PDF côté serveur, stockage S3, interface en anglais, conformité RGPD détaillée (consentement, effacement, portabilité).

## Déploiement (Partie IV du cahier des charges)

Un `Dockerfile` est fourni (option B Railway/Render, ou D VPS). Variables à définir :

```
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://HOST:5432/DB
DATABASE_USER=...
DATABASE_PASSWORD=...
APP_ADMIN_PASSWORD=un-mot-de-passe-solide
```

En profil `prod`, aucune donnée de démonstration n'est créée : seul le compte `admin` existe, avec le mot de passe `APP_ADMIN_PASSWORD`. Pensez aux sauvegardes quotidiennes de la base (rétention 30 jours).

## Structure

```
src/main/java/tn/ecole/fun
  model/      entités JPA (AppUser, Student, SchoolClass, Subject, Grade, Payment, Message, AuditLog)
  repo/       dépôts Spring Data
  service/    moyennes & badges, paiements + message auto, messagerie, audit
  web/        contrôleurs MVC
  security/   Spring Security (formulaire, BCrypt, rôles)
  config/     DataLoader (admin + données de démo)
src/main/resources
  templates/  pages Thymeleaf
  static/     CSS, JS, manifeste PWA
```
