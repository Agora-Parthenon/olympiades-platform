# olympiades-platform
Back-end de la partie plateforme de **Olympiades**, une application web de jeux de société en ligne en temps réel.

Cette application fournit :
- des services REST pour la gestion de la plateforme de jeu
- un point de terminaison de santé (`/health`)
- un point de terminaison de contexte de jeu (`/game/info`)
- la consultation et la modification du profil (`GET` / `PUT /profiles/me`, liste des avatars `GET /profiles/avatars`) ; un changement de pseudo est publié sur le topic Kafka `platform.user.events`
- la création automatique du profil à la première connexion (`POST /profiles/me`, idempotent : 201 à la création, 200 ensuite). L'identité vient des en-têtes `X-User-Id` et `X-User-Name` posés par la gateway
- une intégration avec Keycloak pour l'authentification et l'autorisation
- une connexion à PostgreSQL (schéma `platform`, migrations Flyway dans `src/main/resources/db/migration`) et Kafka pour la persistance et les événements en temps réel
