# RolePlay AI - Application Android de Chatbot Roleplay

Une application Android moderne permettant de discuter avec des personnages IA variés (anime, fantasy, réalistes, etc.) avec différents thèmes et personnalités.

## 🎯 Fonctionnalités

- **Multiple Personnages** : Plus de 15 personnages avec différentes personnalités
- **Catégories Variées** : Anime/Manga, Fantasy, Réel, Historique
- **Thèmes Diversifiés** : Maman, Sœur, Amie, Voisine, MILF, Professeur, Collègue, etc.
- **IA Intégrée** : Utilise une API d'IA gratuite (HuggingFace Inference API)
- **Interface Moderne** : UI Material Design 3 avec Jetpack Compose
- **Chat en Temps Réel** : Conversations fluides et naturelles
- **Compatible Xiaomi** : Optimisé pour tous les appareils Android, y compris Xiaomi

## 🏗️ Architecture

- **Langage** : Kotlin
- **UI** : Jetpack Compose avec Material Design 3
- **Architecture** : MVVM (Model-View-ViewModel)
- **IA** : HuggingFace Inference API (Mistral-7B)
- **Navigation** : Navigation Compose
- **Networking** : Retrofit + OkHttp
- **Images** : Coil

## 📱 Configuration Requise

- Android 7.0 (API 24) ou supérieur
- Connexion Internet pour l'IA
- 50 MB d'espace de stockage

## 🚀 Installation

### Option 1 : Build depuis les sources

1. **Cloner le repository**
```bash
git clone <votre-repo>
cd RolePlayAI
```

2. **Ouvrir dans Android Studio**
   - Ouvrir Android Studio
   - Fichier > Ouvrir > Sélectionner le dossier du projet
   - Attendre la synchronisation Gradle

3. **Builder l'APK**
   - Build > Build Bundle(s) / APK(s) > Build APK(s)
   - L'APK sera généré dans `app/build/outputs/apk/debug/`

4. **Installer sur votre appareil**
   - Transférer l'APK sur votre téléphone
   - Activer "Sources inconnues" dans les paramètres
   - Installer l'APK

### Option 2 : Via Android Studio (Pour développement)

```bash
# Connecter votre appareil en mode développeur
adb devices

# Lancer l'application
./gradlew installDebug
```

## 🎮 Utilisation

1. **Lancer l'application** : L'écran de démarrage charge les ressources
2. **Choisir un personnage** : Parcourir les personnages par catégorie ou thème
3. **Commencer à discuter** : Cliquer sur un personnage pour ouvrir le chat
4. **Converser** : Envoyer des messages et recevoir des réponses de l'IA

## 🔧 Configuration de l'IA

### Utiliser HuggingFace (Par défaut - Gratuit)

L'application utilise par défaut l'API gratuite de HuggingFace. Pour de meilleures performances :

1. Créer un compte sur [HuggingFace](https://huggingface.co)
2. Obtenir une clé API dans les paramètres
3. Modifier `AIEngine.kt` pour ajouter votre clé :

```kotlin
private var apiKey = "votre_clé_api_ici"
```

### Utiliser une API Locale (Optionnel)

Pour utiliser un modèle LLM local (LM Studio, Ollama, etc.) :

1. Installer un serveur LLM local (ex: LM Studio)
2. Démarrer le serveur sur `http://localhost:8080`
3. Dans l'application, configurer :

```kotlin
aiEngine.setUseLocalAPI(true, "http://votre-ip:8080/v1/chat/completions")
```

## 📂 Structure du Projet

```
app/
├── src/main/
│   ├── java/com/roleplayai/chatbot/
│   │   ├── data/
│   │   │   ├── model/          # Modèles de données
│   │   │   ├── repository/     # Repositories
│   │   │   ├── api/            # Services API
│   │   │   └── ai/             # Moteur IA
│   │   ├── ui/
│   │   │   ├── screen/         # Écrans Compose
│   │   │   ├── viewmodel/      # ViewModels
│   │   │   ├── theme/          # Thème Material
│   │   │   └── navigation/     # Navigation
│   │   ├── MainActivity.kt
│   │   └── RolePlayAIApplication.kt
│   ├── res/                     # Ressources
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## 🎨 Personnalisation

### Ajouter un Nouveau Personnage

Modifier `CharacterRepository.kt` :

```kotlin
Character(
    id = "nouveau_1",
    name = "Nom du Personnage",
    description = "Description détaillée",
    personality = "Traits de personnalité",
    scenario = "Contexte de la rencontre",
    imageUrl = "URL de l'image",
    category = CharacterCategory.ANIME,
    themes = listOf(CharacterTheme.FRIEND_FEMALE),
    greeting = "Message de bienvenue"
)
```

### Modifier le Thème

Éditer `Color.kt` et `Theme.kt` dans `ui/theme/`

## 🔒 Confidentialité

- Les conversations ne sont PAS sauvegardées sur des serveurs
- Les données restent locales sur votre appareil
- L'API IA ne conserve pas l'historique des conversations

## 🛠️ Technologies Utilisées

- **Kotlin** 1.9.20
- **Jetpack Compose** 2023.10.01
- **Material Design 3**
- **Retrofit** 2.9.0
- **OkHttp** 4.12.0
- **Coil** 2.5.0
- **Coroutines** 1.7.3
- **Navigation Compose** 2.7.6

## 📱 Compatibilité Xiaomi

L'application est testée et compatible avec les appareils Xiaomi (MIUI). 

**Remarques MIUI** :
- Autoriser l'application à s'exécuter en arrière-plan dans les paramètres MIUI
- Désactiver l'optimisation batterie pour cette application si nécessaire
- Autoriser l'accès Internet dans les paramètres de sécurité

## 🐛 Dépannage

### L'IA ne répond pas
- Vérifier la connexion Internet
- L'API HuggingFace peut avoir des limites de débit (rate limiting)
- Essayer à nouveau après quelques secondes

### Crash au démarrage
- Vérifier que l'appareil est Android 7.0+
- Nettoyer et rebuilder le projet
- Vérifier les permissions dans AndroidManifest.xml

### Images ne chargent pas
- Vérifier la permission Internet
- Les URLs d'images sont des placeholders, remplacer par vos propres URLs

## 📝 TODO / Améliorations Futures

- [ ] Sauvegarde persistante des conversations (Room Database)
- [ ] Support multilingue
- [ ] Personnalisation des avatars
- [ ] Synthèse vocale (TTS)
- [ ] Reconnaissance vocale (STT)
- [ ] Mode hors-ligne avec modèle embarqué
- [ ] Export des conversations
- [ ] Thèmes personnalisés
- [ ] Plus de personnages

## 👨‍💻 Développement

### Prérequis

- Android Studio Hedgehog ou supérieur
- JDK 17
- SDK Android 34
- Gradle 8.2

### Commandes Utiles

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Installer sur appareil connecté
./gradlew installDebug

# Lancer les tests
./gradlew test

# Nettoyer le projet
./gradlew clean
```

## 📄 Licence

Ce projet est fourni à des fins éducatives. Utilisez-le de manière responsable.

## ⚠️ Avertissement

Cette application contient du contenu pour adultes. Utilisation réservée aux personnes majeures. Les créateurs ne sont pas responsables de l'utilisation qui en est faite.

## 🤝 Contribution

Les contributions sont les bienvenues ! N'hésitez pas à :
- Signaler des bugs
- Proposer de nouvelles fonctionnalités
- Soumettre des pull requests
- Ajouter de nouveaux personnages

## 📧 Contact

Pour toute question ou suggestion, n'hésitez pas à ouvrir une issue sur GitHub.

---

**Note** : Cette application utilise l'IA de manière responsable. Les personnages sont fictifs et ne représentent aucune personne réelle.
