# Instructions de Build - RolePlay AI

## 🎯 Guide Rapide de Build

Ce guide vous explique comment compiler l'application Android **RolePlay AI** depuis les sources.

## 📋 Prérequis

### Logiciels Nécessaires

1. **Android Studio** (Recommandé: Hedgehog 2023.1.1 ou supérieur)
   - Télécharger : https://developer.android.com/studio
   - Installer avec les composants par défaut

2. **JDK 17**
   - Généralement inclus avec Android Studio
   - Vérifier : `java -version`

3. **Android SDK**
   - SDK Platform 34 (Android 14)
   - Build Tools 34.0.0
   - Android SDK Platform-Tools
   - Android SDK Command-line Tools

### Configuration Système

**Minimale** :
- RAM : 8 GB
- Stockage : 10 GB libre
- OS : Windows 10+, macOS 10.14+, Linux Ubuntu 18.04+

**Recommandée** :
- RAM : 16 GB+
- Stockage : 20 GB+ libre
- SSD pour de meilleures performances

## 🚀 Étapes de Build

### Étape 1 : Cloner le Projet

```bash
# Cloner depuis Git (si disponible)
git clone <repository-url>
cd RolePlayAI

# Ou extraire depuis une archive
unzip RolePlayAI.zip
cd RolePlayAI
```

### Étape 2 : Ouvrir dans Android Studio

1. Lancer **Android Studio**
2. Cliquer sur **"Open"**
3. Naviguer vers le dossier du projet
4. Sélectionner le dossier racine contenant `build.gradle.kts`
5. Cliquer sur **"OK"**

### Étape 3 : Synchronisation Gradle

Android Studio va automatiquement :
- Télécharger Gradle 8.2
- Télécharger les dépendances
- Indexer le projet

**Attendre** que cette étape se termine (barre de progression en bas).

Si des erreurs apparaissent :
```bash
# Dans le terminal Android Studio
./gradlew clean
./gradlew build --refresh-dependencies
```

### Étape 4 : Configurer un Appareil

#### Option A : Émulateur Android

1. Ouvrir **Device Manager** (icône téléphone)
2. Cliquer sur **"Create Device"**
3. Choisir un appareil (ex: Pixel 6)
4. Sélectionner une image système :
   - **Recommandé** : Tiramisu (API 33) ou UpsideDownCake (API 34)
   - Télécharger si nécessaire
5. Cliquer sur **"Finish"**
6. Démarrer l'émulateur

#### Option B : Appareil Physique

1. **Activer le Mode Développeur** sur votre téléphone :
   - Paramètres > À propos du téléphone
   - Appuyer 7 fois sur "Numéro de build"

2. **Activer le Débogage USB** :
   - Paramètres > Options pour les développeurs
   - Activer "Débogage USB"

3. **Connecter via USB** :
   - Brancher le câble USB
   - Autoriser le débogage sur le téléphone

4. **Vérifier la connexion** :
   ```bash
   adb devices
   # Devrait afficher votre appareil
   ```

### Étape 5 : Build et Run

#### Via Android Studio (Méthode Simple)

1. Sélectionner l'appareil dans la barre d'outils
2. Cliquer sur le bouton **Run** (▶️) ou **Shift+F10**
3. L'application se compile et s'installe automatiquement
4. Elle se lance automatiquement sur l'appareil

#### Via Terminal (Méthode Avancée)

```bash
# Debug Build
./gradlew assembleDebug

# Installer sur appareil connecté
./gradlew installDebug

# Lancer l'application
adb shell am start -n com.roleplayai.chatbot/.MainActivity

# Tout en une commande
./gradlew installDebug && adb shell am start -n com.roleplayai.chatbot/.MainActivity
```

### Étape 6 : Vérifier l'Installation

L'application devrait :
- ✅ Se lancer sans erreur
- ✅ Afficher l'écran de chargement
- ✅ Afficher la liste des personnages
- ✅ Permettre d'ouvrir un chat
- ✅ Générer des réponses (si Internet disponible)

## 📦 Build APK pour Distribution

### Debug APK (Non Signé)

**Pour tester rapidement** :

```bash
./gradlew assembleDebug
```

L'APK sera généré dans :
```
app/build/outputs/apk/debug/app-debug.apk
```

**Taille** : ~10-15 MB

### Release APK (Signé)

**Pour distribution publique** :

#### 1. Créer un Keystore

```bash
keytool -genkey -v -keystore roleplayai.keystore \
  -alias roleplayai \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Remplir les informations demandées
# IMPORTANT : Noter le mot de passe !
```

#### 2. Configurer le Keystore

Créer `keystore.properties` à la racine :

```properties
storePassword=VotreMotDePasse
keyPassword=VotreMotDePasse
keyAlias=roleplayai
storeFile=../roleplayai.keystore
```

**⚠️ Ne jamais committer ce fichier !**

Ajouter à `.gitignore` :
```
keystore.properties
*.keystore
```

#### 3. Modifier `app/build.gradle.kts`

Ajouter avant `android {` :

```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
```

Dans `android { }`, ajouter :

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(keystoreProperties["storeFile"] as String)
        storePassword = keystoreProperties["storePassword"] as String
        keyAlias = keystoreProperties["keyAlias"] as String
        keyPassword = keystoreProperties["keyPassword"] as String
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

#### 4. Build Release APK

```bash
./gradlew assembleRelease
```

L'APK signé sera dans :
```
app/build/outputs/apk/release/app-release.apk
```

**Taille** : ~8-12 MB (optimisé)

#### 5. Vérifier la Signature

```bash
# Vérifier que l'APK est bien signé
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# Devrait afficher "jar verified"
```

## 🔧 Résolution de Problèmes

### Erreur : "SDK location not found"

**Solution** :
```bash
# Créer local.properties
echo "sdk.dir=/chemin/vers/Android/Sdk" > local.properties

# Chemins par défaut :
# Windows : C:\\Users\\VotreNom\\AppData\\Local\\Android\\Sdk
# Mac : /Users/VotreNom/Library/Android/sdk
# Linux : /home/VotreNom/Android/Sdk
```

### Erreur : "Gradle sync failed"

**Solutions** :
```bash
# 1. Nettoyer le projet
./gradlew clean

# 2. Supprimer le cache Gradle
rm -rf ~/.gradle/caches/

# 3. Re-sync
./gradlew build --refresh-dependencies

# 4. Dans Android Studio : File > Invalidate Caches / Restart
```

### Erreur : "Duplicate class found"

**Solution** :
```bash
# Nettoyer et rebuilder
./gradlew clean build
```

### Erreur : "Out of memory"

**Solution** :

Éditer `gradle.properties` :
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

### Build Très Lent

**Solutions** :
1. Activer le Gradle Daemon (déjà fait)
2. Utiliser un SSD
3. Fermer les applications inutiles
4. Augmenter la RAM allouée

```properties
# Dans gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.jvmargs=-Xmx4096m
```

### APK Trop Gros

**Solutions** :

Dans `app/build.gradle.kts` :

```kotlin
android {
    // Activer la séparation par ABI
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
    
    // Activer R8 (shrinking)
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
}
```

Cela créera plusieurs APK plus petits par architecture.

## 📊 Variantes de Build

### Types de Build

```bash
# Debug (développement)
./gradlew assembleDebug
# - Non obfusqué
# - Logs activés
# - Débogueur attachable
# - ~15 MB

# Release (production)
./gradlew assembleRelease
# - Code obfusqué (ProGuard)
# - Optimisé
# - Signé
# - ~10 MB
```

### Build Flavors (Optionnel)

Pour créer différentes versions (ex: Free/Pro), modifier `build.gradle.kts` :

```kotlin
android {
    flavorDimensions += "version"
    
    productFlavors {
        create("free") {
            dimension = "version"
            applicationIdSuffix = ".free"
            versionNameSuffix = "-free"
        }
        
        create("pro") {
            dimension = "version"
            applicationIdSuffix = ".pro"
            versionNameSuffix = "-pro"
        }
    }
}
```

Puis :
```bash
./gradlew assembleFreeDebug
./gradlew assembleProRelease
```

## 🧪 Tests

### Tests Unitaires

```bash
# Lancer tous les tests
./gradlew test

# Tests debug uniquement
./gradlew testDebugUnitTest

# Avec rapport HTML
./gradlew test
# Rapport dans : app/build/reports/tests/testDebugUnitTest/index.html
```

### Tests Instrumentés (sur appareil)

```bash
# Lancer les tests instrumentés
./gradlew connectedAndroidTest

# Nécessite un appareil/émulateur connecté
```

## 📈 Analyse de Build

### Analyser la Taille de l'APK

```bash
# Générer un rapport
./gradlew assembleRelease

# Ouvrir dans Android Studio
# Build > Analyze APK > Sélectionner l'APK

# Voir ce qui prend de la place
```

### Build Scan

```bash
./gradlew build --scan

# Génère un rapport détaillé du build
# URL fournie à la fin
```

## 🚀 CI/CD (Intégration Continue)

### GitHub Actions

Créer `.github/workflows/android.yml` :

```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Build with Gradle
      run: ./gradlew assembleDebug
      
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

## 📦 Distribution

### Google Play Store

1. Créer un compte développeur (25$ one-time)
2. Créer une application
3. Build un AAB (Android App Bundle) :
   ```bash
   ./gradlew bundleRelease
   ```
4. Upload le fichier : `app/build/outputs/bundle/release/app-release.aab`
5. Remplir les informations du store
6. Soumettre pour review

### Distribution Directe (APK)

1. Héberger l'APK sur un serveur
2. Partager le lien de téléchargement
3. Utilisateurs doivent activer "Sources inconnues"

## 📝 Checklist de Release

Avant de distribuer :

- [ ] Tester sur plusieurs appareils
- [ ] Vérifier toutes les fonctionnalités
- [ ] Tester avec/sans Internet
- [ ] Vérifier les permissions
- [ ] Mettre à jour versionCode et versionName
- [ ] Générer APK signé
- [ ] Tester l'APK final
- [ ] Créer des release notes
- [ ] Préparer les screenshots
- [ ] Mettre à jour la documentation

## 🎯 Commandes Utiles

```bash
# Nettoyer le projet
./gradlew clean

# Build debug
./gradlew assembleDebug

# Build release
./gradlew assembleRelease

# Installer sur appareil
./gradlew installDebug

# Désinstaller
./gradlew uninstallDebug

# Lister les tâches
./gradlew tasks

# Vérifier les dépendances
./gradlew dependencies

# Linter
./gradlew lint

# Tests
./gradlew test

# Tout en un
./gradlew clean assembleRelease
```

## 📚 Ressources

- [Documentation Android](https://developer.android.com)
- [Gradle User Manual](https://docs.gradle.org)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Documentation](https://kotlinlang.org/docs)

---

**Bonne compilation ! 🎉**

En cas de problème, consultez la documentation ou ouvrez une issue sur GitHub.
