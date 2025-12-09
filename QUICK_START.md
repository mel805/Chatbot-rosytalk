# 🚀 Quick Start - RolePlay AI

## ⚡ Démarrage Ultra-Rapide (5 minutes)

### Pour les Utilisateurs

**Vous voulez juste utiliser l'application ?**

1. **Télécharger** l'APK
2. **Installer** sur votre téléphone Android
3. **Lancer** l'application
4. **Choisir** un personnage
5. **Commencer** à discuter !

📖 **Guide détaillé** : [INSTALLATION.md](INSTALLATION.md)

---

### Pour les Développeurs

**Vous voulez compiler l'application ?**

```bash
# 1. Cloner le projet
git clone <repo-url>
cd RolePlayAI

# 2. Ouvrir dans Android Studio
# File > Open > Sélectionner le dossier

# 3. Attendre la synchronisation Gradle

# 4. Lancer
# Cliquer sur Run (▶️) ou Shift+F10
```

📖 **Guide détaillé** : [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md)

---

## 📱 Fonctionnalités Principales

### 🎭 15+ Personnages Uniques
- **Anime** : Sakura, Yuki, Akane, Hinata, Misaki
- **Fantasy** : Elara (Elfe), Isabella (Vampire), Lyra (Guerrière)
- **Réaliste** : Marie, Sophie, Camille, Emma, Chloé, Valérie

### 💬 Chat IA Intelligent
- Conversations naturelles et contextuelles
- Personnalités uniques pour chaque personnage
- Réponses en temps réel (3-10 secondes)

### 🎨 Interface Moderne
- Material Design 3
- Thème clair/sombre automatique
- Animations fluides
- UI intuitive

---

## 🔧 Configuration Rapide

### Option 1 : Utilisation Standard (Gratuit)

**Aucune configuration nécessaire !**

L'application utilise l'API gratuite HuggingFace par défaut.

### Option 2 : Avec Token HuggingFace (Recommandé)

Pour de meilleures performances :

1. Créer un compte sur [HuggingFace](https://huggingface.co)
2. Obtenir un token : Settings > Access Tokens
3. Modifier `AIEngine.kt` ligne 19 :
   ```kotlin
   private var apiKey = "hf_votre_token_ici"
   ```

### Option 3 : API Locale (Avancé)

Pour une confidentialité totale :

1. Installer [LM Studio](https://lmstudio.ai/)
2. Télécharger Mistral-7B
3. Démarrer le serveur local
4. Configurer l'app avec votre IP

📖 **Guide complet** : [API_CONFIGURATION.md](API_CONFIGURATION.md)

---

## 🎯 Premiers Pas

### 1. Choisir un Personnage

**Recommandations pour débuter** :

- **Débutant** → **Emma** (Amie d'enfance, douce)
- **Roleplay** → **Sakura** (Timide, anime)
- **Fantasy** → **Elara** (Elfe mage, aventure)
- **Mature** → **Marie** (Voisine, séductrice)

### 2. Commencer une Conversation

**Exemples de premiers messages** :

```
"Bonjour ! Comment vas-tu aujourd'hui ?"

"Salut ! Raconte-moi ta journée."

"Hey ! Qu'est-ce que tu aimes faire ?"
```

### 3. Profiter du Chat

**Conseils** :
- ✅ Être naturel et conversationnel
- ✅ Poser des questions ouvertes
- ✅ Rester dans le contexte du personnage
- ❌ Éviter les messages trop courts ("ok", "oui")

📖 **Guide d'utilisation** : [USAGE_GUIDE.md](USAGE_GUIDE.md)

---

## 🛠️ Dépannage Express

### L'IA ne répond pas
→ Vérifier Internet, attendre 30s, réessayer

### L'app crash au démarrage
→ Redémarrer l'app, vérifier Android 7.0+

### Images ne chargent pas
→ Vérifier Internet, attendre quelques secondes

### Build échoue (développeurs)
→ `./gradlew clean build --refresh-dependencies`

📖 **Guide complet** : Section Dépannage dans [README.md](README.md)

---

## 📚 Documentation Complète

| Document | Description |
|----------|-------------|
| [README.md](README.md) | Vue d'ensemble du projet |
| [INSTALLATION.md](INSTALLATION.md) | Installation détaillée |
| [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md) | Compilation du projet |
| [USAGE_GUIDE.md](USAGE_GUIDE.md) | Guide d'utilisation |
| [FEATURES.md](FEATURES.md) | Fonctionnalités détaillées |
| [API_CONFIGURATION.md](API_CONFIGURATION.md) | Configuration API IA |
| [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) | Résumé technique |

---

## 🎮 Essayez Maintenant !

### Scénarios à Essayer

**🌸 Romance avec Sakura**
```
"Bonjour Sakura, tu veux qu'on aille voir les cerisiers ensemble ?"
```

**⚔️ Aventure avec Elara**
```
"Elara, j'ai entendu parler d'un dragon dans les montagnes. On part à l'aventure ?"
```

**👩‍🏫 Étude avec Camille**
```
"Bonjour Madame, je voudrais améliorer mes notes. Pouvez-vous m'aider ?"
```

**🏘️ Voisinage avec Marie**
```
"Bonjour Marie, je vous ai vue déménager. Besoin d'aide ?"
```

---

## 💡 Astuces Pro

### Pour de Meilleures Conversations

1. **Contexte** : Donner des détails dans vos messages
2. **Émotions** : L'IA répond à l'émotion de vos messages
3. **Questions** : Encourager le personnage à développer
4. **Patience** : Laisser l'IA générer (5-10 secondes)
5. **Immersion** : Jouer le jeu du roleplay

### Raccourcis Clavier (Développeurs)

```
Shift + F10  : Lancer l'app
Ctrl + F9    : Build
Ctrl + Shift + A : Recherche d'action
Alt + Enter  : Quick fix
Ctrl + B     : Go to definition
```

---

## 🤝 Besoin d'Aide ?

1. **Consulter la documentation** (liens ci-dessus)
2. **Vérifier les issues GitHub** (si disponible)
3. **Ouvrir une nouvelle issue** avec :
   - Version Android
   - Modèle d'appareil
   - Description du problème
   - Logs (si possible)

---

## ⚡ Commandes Rapides

```bash
# Build APK Debug
./gradlew assembleDebug

# Installer sur appareil
./gradlew installDebug

# Lancer l'app
adb shell am start -n com.roleplayai.chatbot/.MainActivity

# Voir les logs
adb logcat | grep -i "roleplay\|aiengine"

# Tout en un
./gradlew installDebug && adb shell am start -n com.roleplayai.chatbot/.MainActivity
```

---

## 📊 Informations Techniques Rapides

- **Langage** : Kotlin
- **UI** : Jetpack Compose + Material 3
- **Architecture** : MVVM
- **IA** : HuggingFace (Mistral-7B)
- **Min Android** : 7.0 (API 24)
- **Target Android** : 14 (API 34)
- **Taille APK** : ~10-15 MB

---

## 🎉 C'est Parti !

Vous êtes maintenant prêt à :
- ✅ Utiliser l'application
- ✅ Compiler depuis les sources
- ✅ Modifier et personnaliser
- ✅ Contribuer au projet

**Amusez-vous bien avec RolePlay AI ! 🎭✨**

---

*Pour plus de détails, consultez la documentation complète.*
