# Guide d'Installation - RolePlay AI

## 📱 Installation de l'APK sur Android

### Étape 1 : Activer les Sources Inconnues

Sur les appareils Android modernes (Android 8.0+) :

1. Ouvrir **Paramètres**
2. Aller dans **Sécurité** ou **Applications et notifications**
3. Chercher **Installer des applications inconnues** ou **Sources inconnues**
4. Sélectionner le navigateur ou gestionnaire de fichiers que vous utilisez
5. Activer **Autoriser cette source**

### Étape 2 : Télécharger et Installer l'APK

1. Télécharger le fichier APK sur votre appareil
2. Ouvrir le gestionnaire de fichiers
3. Naviguer vers le dossier **Téléchargements**
4. Appuyer sur le fichier **RolePlayAI.apk**
5. Appuyer sur **Installer**
6. Attendre la fin de l'installation
7. Appuyer sur **Ouvrir** pour lancer l'application

### Étape 3 : Permissions

Au premier lancement, l'application demandera les permissions suivantes :
- **Internet** : Pour communiquer avec l'API IA (obligatoire)
- **Stockage** : Pour sauvegarder les conversations (optionnel)

## 🔧 Configuration Spécifique Xiaomi/MIUI

Les appareils Xiaomi avec MIUI ont des restrictions supplémentaires :

### 1. Autoriser l'Installation

1. Ouvrir **Paramètres**
2. Aller dans **Confidentialité** > **Protection**
3. Désactiver temporairement **Analyser les applications**
4. Installer l'APK
5. Réactiver **Analyser les applications** si souhaité

### 2. Autoriser l'Exécution en Arrière-Plan

1. Ouvrir **Paramètres**
2. Aller dans **Applications** > **Gérer les applications**
3. Chercher **RolePlay AI**
4. Appuyer sur **Économie d'énergie**
5. Sélectionner **Aucune restriction**
6. Activer **Démarrage automatique**

### 3. Permissions Internet (MIUI Security)

1. Ouvrir **Sécurité**
2. Aller dans **Contrôle des données**
3. Chercher **RolePlay AI**
4. Activer **Données mobiles** et **Wi-Fi**

### 4. Désactiver l'Optimisation Batterie

1. Ouvrir **Paramètres**
2. Aller dans **Applications** > **RolePlay AI**
3. Aller dans **Économie d'énergie**
4. Sélectionner **Aucune restriction**

## 🏗️ Build depuis les Sources (Développeurs)

### Prérequis

- **Android Studio** Hedgehog (2023.1.1) ou supérieur
- **JDK 17**
- **Android SDK 34**
- **Git**

### Installation

1. **Cloner le Repository**

```bash
git clone <votre-repository-url>
cd RolePlayAI
```

2. **Ouvrir dans Android Studio**

- Lancer Android Studio
- Cliquer sur **Open**
- Sélectionner le dossier du projet
- Attendre la synchronisation Gradle

3. **Configurer le SDK**

- Aller dans **File** > **Project Structure**
- Dans **SDK Location**, vérifier que le chemin est correct
- Cliquer sur **Apply**

4. **Build Debug APK**

Option A - Via Android Studio :
- Cliquer sur **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**
- Attendre la compilation
- Cliquer sur **locate** dans la notification pour trouver l'APK

Option B - Via Terminal :
```bash
./gradlew assembleDebug
```

L'APK sera dans : `app/build/outputs/apk/debug/app-debug.apk`

5. **Build Release APK (Signé)**

Créer d'abord un keystore :

```bash
keytool -genkey -v -keystore roleplayai.keystore -alias roleplayai -keyalg RSA -keysize 2048 -validity 10000
```

Puis builder :

```bash
./gradlew assembleRelease
```

Ou via Android Studio :
- **Build** > **Generate Signed Bundle / APK**
- Sélectionner **APK**
- Créer ou sélectionner votre keystore
- Choisir **release** build variant
- Cliquer sur **Finish**

## 🧪 Test sur Émulateur

1. **Créer un AVD (Android Virtual Device)**

- Ouvrir **Device Manager** dans Android Studio
- Cliquer sur **Create Device**
- Choisir un appareil (ex: Pixel 6)
- Sélectionner une image système (Android 12+)
- Cliquer sur **Finish**

2. **Lancer l'Application**

- Sélectionner l'émulateur dans la barre d'outils
- Cliquer sur le bouton **Run** (▶️)

## 📱 Test sur Appareil Physique

1. **Activer le Mode Développeur**

- Aller dans **Paramètres** > **À propos du téléphone**
- Appuyer 7 fois sur **Numéro de build**
- Le mode développeur est activé

2. **Activer le Débogage USB**

- Aller dans **Paramètres** > **Options pour les développeurs**
- Activer **Débogage USB**

3. **Connecter l'Appareil**

- Connecter via USB
- Autoriser le débogage sur l'appareil
- Vérifier la connexion :

```bash
adb devices
```

4. **Installer et Lancer**

Via Android Studio : Cliquer sur Run
Via Terminal :
```bash
./gradlew installDebug
adb shell am start -n com.roleplayai.chatbot/.MainActivity
```

## 🔧 Dépannage

### Problème : "App not installed"

**Solutions :**
1. Désinstaller toute version précédente
2. Vérifier l'espace de stockage disponible
3. Redémarrer l'appareil
4. Utiliser un autre gestionnaire de fichiers

### Problème : "Parse Error"

**Solutions :**
1. Vérifier que le fichier APK n'est pas corrompu
2. Re-télécharger l'APK
3. Vérifier la compatibilité de la version Android

### Problème : Crash au Démarrage

**Solutions :**
1. Vider le cache de l'application
2. Réinstaller l'application
3. Vérifier les permissions
4. Vérifier la connexion Internet

### Problème : L'IA ne répond pas

**Solutions :**
1. Vérifier la connexion Internet
2. Attendre quelques secondes (rate limiting)
3. Redémarrer l'application
4. Vérifier que l'API HuggingFace fonctionne

### Xiaomi : "Installation Blocked"

**Solutions :**
1. Désactiver **MIUI Optimization** dans Options développeur
2. Utiliser **Mi Unlock** si nécessaire
3. Autoriser l'installation dans Sécurité > Permissions

## 📊 Vérification de l'Installation

Après l'installation, vérifier que :

✅ L'icône de l'application apparaît dans le lanceur
✅ L'application se lance sans erreur
✅ L'écran de chargement s'affiche
✅ La liste des personnages s'affiche
✅ Les images des personnages se chargent
✅ Le chat fonctionne et l'IA répond

## 🔒 Sécurité

- L'APK est non signé en mode debug (normal)
- Pour la production, utiliser un APK signé
- Vérifier la source de téléchargement
- Ne pas installer d'APK de sources non fiables

## 📞 Support

En cas de problème :

1. Consulter le fichier README.md
2. Vérifier les issues GitHub
3. Créer une nouvelle issue avec :
   - Version Android
   - Modèle d'appareil
   - Description du problème
   - Logs si possible

## 🚀 Mises à Jour

Pour mettre à jour l'application :

1. Désinstaller l'ancienne version (optionnel si la signature est identique)
2. Installer la nouvelle version
3. Les données peuvent être conservées entre versions

---

**Note** : Cette application est fournie telle quelle. Utilisez-la de manière responsable.
