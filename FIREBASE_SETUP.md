# 🔐 Configuration Firebase pour RoleplayAI

## IMPORTANT : Configuration Requise

Le fichier `google-services.json` actuel est un **template**. Vous devez le remplacer par votre propre configuration Firebase.

## Étapes de Configuration

### 1️⃣ Créer un Projet Firebase

1. Allez sur **https://console.firebase.google.com/**
2. Cliquez sur **"Ajouter un projet"**
3. Nom du projet : **roleplay-ai-chatbot** (ou autre)
4. Activez Google Analytics (optionnel)
5. Créez le projet

### 2️⃣ Ajouter une Application Android

1. Dans votre projet Firebase, cliquez sur **"Ajouter une application"**
2. Sélectionnez **Android** (icône robot)
3. Package Android : **`com.roleplayai.chatbot`** (IMPORTANT : exactement ce nom)
4. Nom de l'app : **RoleplayAI**
5. SHA-1 certificat (optionnel pour dev, mais requis pour Google Sign-In)

#### Obtenir SHA-1 (pour Google Sign-In)
```bash
cd /workspace
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

6. Cliquez sur **"Enregistrer l'application"**

### 3️⃣ Télécharger google-services.json

1. Firebase Console → Votre projet → Project Settings ⚙️
2. Onglet **"Général"**
3. Section **"Vos applications"**
4. Cliquez sur votre app Android
5. **Téléchargez** `google-services.json`
6. **Remplacez** le fichier dans `/workspace/app/google-services.json`

### 4️⃣ Activer Firebase Authentication

1. Firebase Console → **Authentication**
2. Cliquez sur **"Commencer"**
3. Onglet **"Sign-in method"**
4. Activez **"Google"** :
   - Cliquez sur Google
   - Activez le fournisseur
   - Email du projet : **douvdouv21@gmail.com**
   - Enregistrez

### 5️⃣ Activer Firestore Database

1. Firebase Console → **Firestore Database**
2. Cliquez sur **"Créer une base de données"**
3. Mode de sécurité : **Production** (recommandé)
4. Emplacement : Choisissez le plus proche (ex: europe-west)
5. Créez

#### Règles de Sécurité Firestore
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /admin/{document=**} {
      allow read, write: if request.auth.token.email == 'douvdouv21@gmail.com';
    }
  }
}
```

### 6️⃣ Configurer Admin (Votre Email)

Votre compte **douvdouv21@gmail.com** sera **automatiquement admin** dans l'app.

Droits admin :
- ✅ Contrôle total des paramètres
- ✅ Accès à tous les modèles Groq
- ✅ Peut activer/désactiver le mode NSFW pour tous
- ✅ Gestion des utilisateurs (futур)

Les autres utilisateurs :
- ✅ Peuvent se connecter avec Google
- ✅ Ont leurs propres préférences
- ✅ Peuvent activer leur propre mode NSFW

## Recompiler l'App

Une fois `google-services.json` configuré :

```bash
cd /workspace
unset JAVA_HOME
export ANDROID_HOME=$HOME/android-sdk
./gradlew assembleDebug
```

## Tester Google Sign-In

1. Installez l'APK
2. Écran de connexion s'affiche
3. **"Se connecter avec Google"**
4. Choisissez votre compte
5. Si vous êtes **douvdouv21@gmail.com** → **Admin** 👑
6. Autres comptes → Utilisateur normal

## Dépannage

### Erreur "SHA-1 non configuré"
➡️ Ajoutez le SHA-1 dans Firebase Console

### Erreur "API_KEY_NOT_FOUND"
➡️ Vérifiez que google-services.json est bien configuré

### Erreur "UNAUTHORIZED_DOMAIN"
➡️ Dans Firebase Console → Authentication → Settings → Authorized domains

## Structure Firestore

```
users/
  {userId}/
    email: string
    displayName: string
    photoURL: string
    isAdmin: boolean
    nsfwEnabled: boolean
    groqApiKey: string
    groqModelId: string
    createdAt: timestamp
    lastLogin: timestamp
```

## Résumé

✅ Firebase Authentication (Google Sign-In)  
✅ Firestore Database (données utilisateurs)  
✅ Admin : douvdouv21@gmail.com (contrôle total)  
✅ NSFW par utilisateur  
✅ Multi-utilisateurs support
