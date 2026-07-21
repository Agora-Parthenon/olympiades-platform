# olympiades-platform
Back-end de la partie plateforme de **Olympiades**, une application web de jeux de société en ligne en temps réel.

Cette application fournit :
- des services REST pour la gestion de la plateforme de jeu
- un point de terminaison de santé (`/health`)
- un point de terminaison de contexte de jeu (`/game/info`)
- une intégration avec Keycloak pour l'authentification et l'autorisation
- une connexion à PostgreSQL et Kafka pour la persistance et les événements en temps réel
