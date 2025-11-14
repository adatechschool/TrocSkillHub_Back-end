# --- Étape 1: Utilise une image Maven avec Java 21 pour le build ---
FROM maven:3.9-eclipse-temurin-21 AS build

# Définit le répertoire de travail dans le conteneur
WORKDIR /app

# Copie le fichier pom.xml pour télécharger les dépendances en cache
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# Copie le code source
COPY src ./src

# Construit le projet (sans exécuter les tests)
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests


# --- Étape 2 : Image finale légère pour l'exécution ---

# Utilise une image JRE Java 21 (sans Maven)
FROM eclipse-temurin:21-jre

# Définit le répertoire de travail
WORKDIR /app

# Copie le JAR construit depuis l'étape 'build'
COPY --from=build /app/target/*.jar app.jar

# Déclare le port sur lequel l'application écoute
EXPOSE 8080

# Commande de démarrage de l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
