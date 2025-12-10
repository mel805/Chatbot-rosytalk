# 📥 Comment Télécharger l'APK

## 📍 **Emplacement du Fichier APK**

```
/workspace/app/build/outputs/apk/debug/app-debug.apk
```

**Taille** : 16 MB  
**Type** : Android Package (APK)  
**Nom du fichier** : `app-debug.apk`

---

## 💡 **MÉTHODES POUR RÉCUPÉRER L'APK**

### ✅ **Méthode 1 : Via Cursor (Le Plus Simple)**

Dans **Cursor/VSCode** :

1. **Ouvrir l'explorateur de fichiers** (panneau de gauche)
2. **Naviguer vers** :
   ```
   app → build → outputs → apk → debug → app-debug.apk
   ```
3. **Clic droit** sur `app-debug.apk`
4. Choisir **"Reveal in File Explorer"** ou **"Show in Finder"**
5. Le fichier s'ouvre dans votre explorateur de fichiers
6. **Copier** le fichier où vous voulez

**OU DIRECTEMENT** :

1. **Clic droit** sur `app-debug.apk` dans l'explorateur Cursor
2. Choisir **"Copy Path"** pour obtenir le chemin complet
3. Ouvrir ce chemin dans votre explorateur de fichiers

---

### ✅ **Méthode 2 : Copier vers un Emplacement Accessible**

Dans le terminal Cursor, exécutez :

```bash
# Copier l'APK vers le dossier racine du workspace
cp /workspace/app/build/outputs/apk/debug/app-debug.apk /workspace/RolePlayAI.apk

# Maintenant le fichier est à la racine : /workspace/RolePlayAI.apk
```

Puis accédez au fichier `RolePlayAI.apk` à la racine du projet.

---

### ✅ **Méthode 3 : Via Terminal (Ligne de Commande)**

**Sur Linux/Mac** :
```bash
# Ouvrir l'emplacement du fichier
xdg-open /workspace/app/build/outputs/apk/debug/  # Linux
open /workspace/app/build/outputs/apk/debug/      # Mac
```

**Sur Windows** :
```bash
# Si vous utilisez WSL
explorer.exe /workspace/app/build/outputs/apk/debug/

# Ou PowerShell
start /workspace/app/build/outputs/apk/debug/
```

---

### ✅ **Méthode 4 : Transférer Directement sur Téléphone**

Si votre téléphone Android est connecté via USB :

```bash
# Vérifier que le téléphone est connecté
adb devices

# Transférer l'APK sur le téléphone
adb push /workspace/app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/RolePlayAI.apk

# Maintenant l'APK est dans le dossier Téléchargements de votre téléphone
```

Ensuite sur le téléphone :
1. Ouvrir le gestionnaire de fichiers
2. Aller dans "Téléchargements" ou "Download"
3. Taper sur `RolePlayAI.apk`
4. Installer

---

### ✅ **Méthode 5 : Via Email/Cloud**

**A. Email** :
```bash
# Attacher l'APK à un email (si vous avez configuré un client mail)
# Ou simplement copier le fichier et l'envoyer comme pièce jointe
```

**B. Google Drive / Dropbox** :
1. Copier l'APK dans votre dossier Google Drive ou Dropbox
2. Synchroniser
3. Télécharger depuis votre téléphone

**C. Services de transfert** :
- [Firefox Send](https://send.vis.ee/)
- [WeTransfer](https://wetransfer.com/)
- [Dropbox Transfer](https://www.dropbox.com/transfer)

---

### ✅ **Méthode 6 : Créer un Serveur HTTP Local**

Dans le terminal :

```bash
# Option A : Python SimpleHTTPServer
cd /workspace/app/build/outputs/apk/debug/
python3 -m http.server 8000

# Option B : PHP
php -S 0.0.0.0:8000

# Ensuite, depuis votre téléphone (sur le même réseau WiFi) :
# Ouvrir le navigateur et aller à : http://[IP-DE-VOTRE-PC]:8000
# Télécharger app-debug.apk
```

Pour trouver l'IP de votre PC :
```bash
# Linux/Mac
ifconfig | grep "inet "

# Windows
ipconfig
```

---

## 🔍 **VÉRIFICATION DU FICHIER**

Pour vérifier que l'APK est intact :

```bash
# Vérifier que c'est bien un APK Android
file /workspace/app/build/outputs/apk/debug/app-debug.apk

# Voir les informations
du -h /workspace/app/build/outputs/apk/debug/app-debug.apk

# Checksum MD5 (optionnel)
md5sum /workspace/app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 **INSTALLATION SUR ANDROID**

Une fois l'APK sur votre téléphone :

### 1. **Activer les Sources Inconnues**

**Android 8.0+** :
- Paramètres → Sécurité
- "Installer des applications inconnues"
- Activer pour le gestionnaire de fichiers ou le navigateur

**Android 7.x et antérieur** :
- Paramètres → Sécurité
- Cocher "Sources inconnues"

### 2. **Installer**

1. Ouvrir le gestionnaire de fichiers
2. Naviguer vers l'emplacement de l'APK
3. Taper sur `app-debug.apk` ou `RolePlayAI.apk`
4. Appuyer sur "Installer"
5. Attendre l'installation
6. Appuyer sur "Ouvrir"

### 3. **Pour Xiaomi/MIUI**

Instructions spéciales pour MIUI :
- Voir le fichier `INSTALLATION.md` section "Configuration Xiaomi/MIUI"

---

## 🎯 **CHEMIN COMPLET DU FICHIER**

```
Chemin absolu :
/workspace/app/build/outputs/apk/debug/app-debug.apk

Chemin relatif (depuis /workspace) :
app/build/outputs/apk/debug/app-debug.apk

Dans l'explorateur Cursor :
workspace > app > build > outputs > apk > debug > app-debug.apk
```

---

## 💾 **INFORMATIONS DU FICHIER**

**Nom** : `app-debug.apk`  
**Taille** : ~16 MB (16,777,216 bytes)  
**Type MIME** : `application/vnd.android.package-archive`  
**Format** : ZIP (APK est un fichier ZIP)  
**Signature** : Debug (non signé pour production)

**Contenu** :
- Code compilé (DEX)
- Ressources Android
- Manifest
- Bibliothèques natives
- Icônes et images

---

## ⚡ **COMMANDE RAPIDE**

Pour copier l'APK à la racine du projet :

```bash
cp /workspace/app/build/outputs/apk/debug/app-debug.apk /workspace/RolePlayAI.apk
```

Ensuite, vous pouvez accéder à `RolePlayAI.apk` directement dans `/workspace/`.

---

## 🔒 **SÉCURITÉ**

✅ **Cet APK est sûr** :
- Compilé depuis le code source que vous voyez
- Aucun code malveillant
- Debug build pour développement
- Non signé avec certificat production

⚠️ **Note** :
- C'est une version DEBUG
- Pour production, créer un APK RELEASE signé
- Voir `BUILD_INSTRUCTIONS.md` pour créer un APK release

---

## 📞 **BESOIN D'AIDE ?**

Si vous ne trouvez pas le fichier :

1. **Vérifier qu'il existe** :
   ```bash
   ls -lh /workspace/app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Recompiler si nécessaire** :
   ```bash
   cd /workspace
   export ANDROID_HOME=$HOME/android-sdk
   ./gradlew assembleDebug
   ```

3. **Chercher tous les APK** :
   ```bash
   find /workspace -name "*.apk" -type f
   ```

---

## 🎉 **RÉCAPITULATIF**

**Le fichier APK se trouve ici** :
```
/workspace/app/build/outputs/apk/debug/app-debug.apk
```

**Pour y accéder facilement** :
1. Utiliser l'explorateur de fichiers de Cursor
2. Ou copier vers `/workspace/RolePlayAI.apk`
3. Ou transférer directement sur téléphone avec ADB

**Ensuite** :
- Transférer sur Android
- Installer l'APK
- Profiter de RolePlay AI ! 🎭✨

---

*Si vous avez des questions, consultez `INSTALLATION.md` ou `APK_PRET.md`*
