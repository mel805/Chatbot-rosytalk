# Guide de Compilation et Release GitHub

## 🚀 Étape 1 : Récupérer les modifications

Sur votre machine locale, dans le dossier du projet :

```bash
# Récupérer les dernières modifications
git pull origin cursor/ajouter-m-moire-conversation-et-inscription-5382

# Ou si vous n'êtes pas sur cette branche
git checkout cursor/ajouter-m-moire-conversation-et-inscription-5382
git pull
```

## 🔧 Étape 2 : Préparer l'environnement

### Vérifier que vous avez :
- ✅ Android Studio installé
- ✅ SDK Android (niveau 24 minimum, 34 recommandé)
- ✅ NDK Android (version 26.1.10909125)
- ✅ Java JDK 8 ou supérieur

### Créer le fichier local.properties

Dans la racine du projet, créez `local.properties` :

```properties
sdk.dir=/chemin/vers/votre/Android/Sdk
```

Exemple Windows :
```properties
sdk.dir=C\:\\Users\\VotreNom\\AppData\\Local\\Android\\Sdk
```

Exemple macOS/Linux :
```properties
sdk.dir=/Users/VotreNom/Library/Android/sdk
```

## 📦 Étape 3 : Compiler l'APK

### Option A : APK Debug (pour tests)

```bash
# Dans le dossier du projet
./gradlew assembleDebug

# L'APK sera dans :
# app/build/outputs/apk/debug/app-debug.apk
```

### Option B : APK Release (pour distribution)

1. **Créer ou utiliser votre keystore** (si vous n'en avez pas) :

```bash
keytool -genkey -v -keystore roleplay-ai-release.keystore -alias roleplay-ai -keyalg RSA -keysize 2048 -validity 10000
```

2. **Créer `keystore.properties`** à la racine :

```properties
storeFile=roleplay-ai-release.keystore
storePassword=VOTRE_MOT_DE_PASSE
keyAlias=roleplay-ai
keyPassword=VOTRE_MOT_DE_PASSE
```

⚠️ **IMPORTANT** : Ajoutez `keystore.properties` à `.gitignore` !

3. **Modifier `app/build.gradle.kts`** pour signer l'APK :

```kotlin
android {
    // ... existing config ...
    
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = java.util.Properties()
                keystoreProperties.load(java.io.FileInputStream(keystorePropertiesFile))
                
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

4. **Compiler la release** :

```bash
./gradlew assembleRelease

# L'APK sera dans :
# app/build/outputs/apk/release/app-release.apk
```

## 🏷️ Étape 4 : Préparer la version

### Mettre à jour le numéro de version

Dans `app/build.gradle.kts` :

```kotlin
defaultConfig {
    applicationId = "com.roleplayai.chatbot"
    minSdk = 24
    targetSdk = 34
    versionCode = 4  // Incrémenter à chaque release
    versionName = "1.4.0"  // Nouvelle version avec les fonctionnalités
}
```

### Tester l'APK

```bash
# Installer sur un appareil/émulateur connecté
adb install app/build/outputs/apk/release/app-release.apk

# Vérifier les logs
adb logcat | grep RolePlayAI
```

## 🎉 Étape 5 : Créer la Release GitHub

### 1. Créer un tag de version

```bash
git add .
git commit -m "feat: ajout mémoire conversations, profil utilisateur et pseudo dans conversations

- Ajout système de persistence des conversations avec DataStore
- Nouvelle page de profil utilisateur avec pseudo, bio, âge
- Les personnages utilisent le pseudo de l'utilisateur dans les conversations
- Amélioration du système d'inscription
- Mise à jour des moteurs AI (Groq et Local) pour intégration du pseudo"

git tag -a v1.4.0 -m "Version 1.4.0 - Mémoire, Profil et Personnalisation"
git push origin cursor/ajouter-m-moire-conversation-et-inscription-5382
git push origin v1.4.0
```

### 2. Créer la release sur GitHub

#### Option A : Via l'interface GitHub

1. Allez sur : https://github.com/VOTRE_USERNAME/VOTRE_REPO/releases
2. Cliquez sur "Draft a new release"
3. Choisissez le tag `v1.4.0`
4. Titre : `Version 1.4.0 - Mémoire, Profil et Personnalisation`
5. Description :

```markdown
## 🎉 Version 1.4.0 - Mémoire, Profil et Personnalisation

### ✨ Nouvelles fonctionnalités

#### 💾 Système de mémoire des conversations
- Les conversations sont maintenant sauvegardées automatiquement
- Reprenez vos conversations là où vous les avez laissées
- Aucune perte de données au redémarrage de l'app

#### 👤 Profil utilisateur complet
- Nouvelle page "Mon Profil" dans les paramètres
- Définissez votre pseudo, bio, âge
- Gestion complète de vos informations personnelles

#### 🗣️ Personnages qui vous appellent par votre pseudo
- Les personnages AI utilisent maintenant votre pseudo dans les conversations
- Conversations plus immersives et personnelles
- Compatible avec tous les moteurs (Groq API et Local)

### 🔧 Améliorations techniques
- Persistence avec DataStore
- Modèles User étendus (username, bio, age)
- Prompts AI optimisés avec informations utilisateur
- Navigation améliorée avec nouvelle page de profil

### 📦 Installation
Téléchargez `RolePlayAI-v1.4.0.apk` ci-dessous

### 📝 Notes
- Version minimale Android : 7.0 (API 24)
- Taille : ~XX MB
- Données stockées localement (privé et sécurisé)

### 🐛 Corrections de bugs
- Amélioration de la stabilité générale
- Optimisation de la mémoire

---

**Contributeurs** : [Vos noms]
**Date** : $(date +%Y-%m-%d)
```

6. Uploadez `app-release.apk` (renommez-le en `RolePlayAI-v1.4.0.apk`)
7. Cochez "Set as the latest release"
8. Cliquez sur "Publish release"

#### Option B : Via GitHub CLI

```bash
# Installer GitHub CLI si nécessaire : https://cli.github.com/

# Se connecter
gh auth login

# Créer la release avec l'APK
gh release create v1.4.0 \
  app/build/outputs/apk/release/app-release.apk#RolePlayAI-v1.4.0.apk \
  --title "Version 1.4.0 - Mémoire, Profil et Personnalisation" \
  --notes-file RELEASE_NOTES.md
```

Où `RELEASE_NOTES.md` contient la description ci-dessus.

## 📱 Étape 6 : Partager le lien

Après la création de la release, le lien sera :

```
https://github.com/VOTRE_USERNAME/VOTRE_REPO/releases/tag/v1.4.0
```

Ou pour téléchargement direct :

```
https://github.com/VOTRE_USERNAME/VOTRE_REPO/releases/download/v1.4.0/RolePlayAI-v1.4.0.apk
```

## ✅ Checklist finale

Avant de publier la release, vérifiez :

- [ ] APK compile sans erreurs
- [ ] APK testé sur un appareil/émulateur
- [ ] Toutes les nouvelles fonctionnalités fonctionnent
- [ ] Pas de crash au démarrage
- [ ] Persistence des conversations fonctionne
- [ ] Page de profil accessible et fonctionnelle
- [ ] Pseudo utilisé dans les conversations
- [ ] Version incrémentée dans build.gradle.kts
- [ ] Tag Git créé et poussé
- [ ] APK uploadé sur GitHub Release
- [ ] Notes de release complètes

## 🎯 Résumé rapide

```bash
# 1. Compiler
./gradlew assembleRelease

# 2. Tester
adb install app/build/outputs/apk/release/app-release.apk

# 3. Commit et Tag
git add .
git commit -m "feat: v1.4.0 - mémoire, profil, personnalisation"
git tag -a v1.4.0 -m "Version 1.4.0"
git push origin cursor/ajouter-m-moire-conversation-et-inscription-5382
git push origin v1.4.0

# 4. Créer la release sur GitHub avec l'APK
```

## 📞 Support

Si vous rencontrez des problèmes :
1. Vérifiez que le SDK Android est bien configuré
2. Nettoyez le build : `./gradlew clean`
3. Vérifiez les logs : `./gradlew assembleRelease --stacktrace`

---

**Bon release ! 🚀**
