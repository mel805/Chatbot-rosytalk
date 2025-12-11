# 🚀 Guide de Compilation et Release GitHub - v3.8.0

## ⚠️ Important
Ce guide vous explique comment **compiler l'APK** et **créer le release GitHub** sur **votre machine locale**.

---

## 📋 Prérequis

### 1. SDK Android
Vous devez avoir le SDK Android installé :

```bash
# Vérifier que ANDROID_HOME est défini
echo $ANDROID_HOME

# Si vide, installez Android Studio et définissez :
export ANDROID_HOME=/path/to/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

### 2. GitHub CLI (optionnel mais recommandé)
```bash
# Installation
# MacOS
brew install gh

# Linux
sudo apt install gh

# Windows
winget install --id GitHub.cli

# Authentification
gh auth login
```

### 3. Java JDK 17+
```bash
# Vérifier la version
java -version

# Devrait afficher Java 17 ou supérieur
```

---

## 🚀 Méthode 1 : Script Automatique (Recommandé)

### Étape 1 : Exécuter le script
```bash
cd /workspace

# Rendre le script exécutable (si pas déjà fait)
chmod +x BUILD_AND_RELEASE.sh

# Exécuter
./BUILD_AND_RELEASE.sh
```

Le script va automatiquement :
- ✅ Vérifier les prérequis
- ✅ Nettoyer le projet
- ✅ Compiler Debug et Release APK
- ✅ Créer le dossier `release-v3.8.0/` avec tous les fichiers
- ✅ Afficher les instructions pour GitHub

### Étape 2 : Créer le release GitHub

Après l'exécution du script, vous aurez un dossier `release-v3.8.0/` contenant :
```
release-v3.8.0/
├── RolePlayAI-v3.8.0-debug.apk
├── RolePlayAI-v3.8.0-release.apk (si signature OK)
├── RELEASE_NOTES_v3.8.0.md
├── QUICK_START_v3.8.0.md
├── AMELIORATIONS_IA_LOCALE_v3.8.0.md
└── README.md
```

**Avec GitHub CLI** :
```bash
# 1. Commiter
git add .
git commit -F COMMIT_MESSAGE_v3.8.0.txt

# 2. Pusher
git push origin main  # ou votre branche

# 3. Créer tag
git tag -a v3.8.0 -m "Release v3.8.0 - Cascade Intelligente"
git push origin v3.8.0

# 4. Créer release
gh release create v3.8.0 \
  --title "RolePlay AI v3.8.0 - Cascade Intelligente" \
  --notes-file RELEASE_NOTES_v3.8.0.md \
  release-v3.8.0/RolePlayAI-v3.8.0-*.apk
```

**Manuellement (sans GitHub CLI)** :
1. Aller sur : https://github.com/VOTRE_USERNAME/VOTRE_REPO/releases/new
2. Créer un nouveau tag : `v3.8.0`
3. Titre : `RolePlay AI v3.8.0 - Cascade Intelligente`
4. Description : Copier le contenu de `RELEASE_NOTES_v3.8.0.md`
5. Uploader les APK depuis `release-v3.8.0/`
6. Publier le release

---

## 🔧 Méthode 2 : Manuelle

### Étape 1 : Nettoyer le projet
```bash
cd /workspace
./gradlew clean
```

### Étape 2 : Compiler Debug APK
```bash
./gradlew assembleDebug

# APK sera dans :
# app/build/outputs/apk/debug/app-debug.apk
```

### Étape 3 : Compiler Release APK (signé)

**Option A : Avec keystore existant**
```bash
./gradlew assembleRelease

# Si vous avez configuré la signature dans build.gradle
# APK sera dans :
# app/build/outputs/apk/release/app-release.apk
```

**Option B : Créer un nouveau keystore**
```bash
# Créer le keystore
keytool -genkey -v -keystore my-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias my-key-alias

# Ajouter dans app/build.gradle.kts :
android {
    signingConfigs {
        create("release") {
            storeFile = file("../my-release-key.jks")
            storePassword = "votre_password"
            keyAlias = "my-key-alias"
            keyPassword = "votre_password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

# Puis compiler
./gradlew assembleRelease
```

### Étape 4 : Vérifier l'APK
```bash
# Informations APK
aapt dump badging app/build/outputs/apk/debug/app-debug.apk | head -5

# Taille
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

### Étape 5 : Préparer le release
```bash
# Créer dossier
mkdir release-v3.8.0

# Copier APK
cp app/build/outputs/apk/debug/app-debug.apk \
   release-v3.8.0/RolePlayAI-v3.8.0-debug.apk

# Copier documentation
cp RELEASE_NOTES_v3.8.0.md release-v3.8.0/
cp QUICK_START_v3.8.0.md release-v3.8.0/
cp AMELIORATIONS_IA_LOCALE_v3.8.0.md release-v3.8.0/
```

### Étape 6 : GitHub Release (voir Méthode 1, Étape 2)

---

## 🐛 Dépannage

### Problème : SDK not found
```bash
# Solution : Définir ANDROID_HOME
export ANDROID_HOME=/path/to/Android/Sdk

# Ajouter à ~/.bashrc ou ~/.zshrc pour persistance
echo 'export ANDROID_HOME=/path/to/Android/Sdk' >> ~/.bashrc
source ~/.bashrc
```

### Problème : Build failed
```bash
# Nettoyer et réessayer
./gradlew clean
./gradlew assembleDebug --stacktrace
```

### Problème : Signature manquante (Release APK)
```bash
# Utiliser Debug APK pour le moment
# Ou configurer le keystore (voir Méthode 2, Étape 3, Option B)
```

### Problème : Permission denied sur script
```bash
chmod +x BUILD_AND_RELEASE.sh
```

### Problème : gh command not found
```bash
# Installer GitHub CLI
# Voir Prérequis section 2

# Ou utiliser la méthode manuelle
```

---

## 📦 Contenu du Release

### Fichiers à inclure :
1. ✅ **RolePlayAI-v3.8.0-debug.apk** (ou release.apk)
2. ✅ **RELEASE_NOTES_v3.8.0.md**
3. ✅ **QUICK_START_v3.8.0.md**
4. ✅ **AMELIORATIONS_IA_LOCALE_v3.8.0.md**

### Informations du release :
- **Tag** : `v3.8.0`
- **Titre** : `RolePlay AI v3.8.0 - Cascade Intelligente`
- **Description** : Contenu de `RELEASE_NOTES_v3.8.0.md`

---

## 📊 Vérifications Avant Release

### Checklist :
- [ ] APK compile sans erreur
- [ ] APK installable sur appareil Android
- [ ] Application démarre correctement
- [ ] Tests basiques fonctionnent (envoyer un message)
- [ ] Mode NSFW fonctionne (si activé)
- [ ] Documentation est à jour
- [ ] Version dans `build.gradle.kts` = 3.8.0
- [ ] Changelog dans `RELEASE_NOTES_v3.8.0.md` est correct

### Test rapide de l'APK :
```bash
# Installer sur appareil connecté
adb install release-v3.8.0/RolePlayAI-v3.8.0-debug.apk

# Lancer l'app
adb shell am start -n com.roleplayai.chatbot/.MainActivity

# Voir les logs
adb logcat | grep -E "(ChatViewModel|GroqAIEngine|HuggingFaceAIEngine|LocalAIEngine)"
```

---

## 🔗 Obtenir le Lien du Release

### Après création du release :

**Option 1 : GitHub CLI**
```bash
gh release view v3.8.0 --web
# Ouvre le release dans le navigateur

# Obtenir l'URL
gh release view v3.8.0 --json url -q .url
```

**Option 2 : Manuellement**
L'URL sera du format :
```
https://github.com/VOTRE_USERNAME/VOTRE_REPO/releases/tag/v3.8.0
```

**Lien de téléchargement direct de l'APK** :
```
https://github.com/VOTRE_USERNAME/VOTRE_REPO/releases/download/v3.8.0/RolePlayAI-v3.8.0-debug.apk
```

---

## 📝 Exemple Complet

### Workflow complet :

```bash
# 1. Aller dans le projet
cd /workspace

# 2. Vérifier prérequis
echo $ANDROID_HOME  # Doit afficher le chemin
java -version       # Doit afficher Java 17+
gh --version        # Optionnel

# 3. Exécuter le script de build
./BUILD_AND_RELEASE.sh

# 4. Commiter et pusher
git add .
git commit -F COMMIT_MESSAGE_v3.8.0.txt
git push origin main

# 5. Créer tag
git tag -a v3.8.0 -m "Release v3.8.0 - Cascade Intelligente"
git push origin v3.8.0

# 6. Créer release GitHub
gh release create v3.8.0 \
  --title "RolePlay AI v3.8.0 - Cascade Intelligente" \
  --notes-file RELEASE_NOTES_v3.8.0.md \
  release-v3.8.0/RolePlayAI-v3.8.0-*.apk

# 7. Obtenir le lien
gh release view v3.8.0 --json url -q .url

# ✅ Terminé !
```

---

## 🎉 Résultat Final

Vous obtiendrez :
1. ✅ APK compilé et prêt
2. ✅ Release GitHub créé
3. ✅ Lien de téléchargement public
4. ✅ Documentation incluse

**Exemple de lien final** :
```
https://github.com/VOTRE_USERNAME/roleplayai-chatbot/releases/tag/v3.8.0
```

---

## 📞 Besoin d'Aide ?

### Si vous bloquez :
1. Vérifiez les logs d'erreur
2. Consultez la section Dépannage ci-dessus
3. Vérifiez que tous les prérequis sont installés
4. Essayez la compilation Debug d'abord (plus simple)

### Commandes utiles :
```bash
# Voir les tâches Gradle disponibles
./gradlew tasks

# Build avec logs détaillés
./gradlew assembleDebug --info

# Build avec trace complète
./gradlew assembleDebug --stacktrace

# Nettoyer complètement
./gradlew clean
rm -rf .gradle build app/build
```

---

**Bonne chance pour la compilation et le release ! 🚀**
