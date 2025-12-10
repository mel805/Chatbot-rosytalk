# RolePlay AI v1.5.0 - Nouvelle Interface & Menu de Navigation

## 🎉 Nouveautés Majeures

### 📱 **Nouvelle Interface avec Menu de Navigation**
- **Bottom Navigation Bar** moderne avec 3 sections principales
- Navigation fluide et intuitive entre les écrans
- Design Material 3 amélioré

### 🔍 **Écran Explorer**
- **Grille de personnages** avec images
- **Recherche en temps réel** par nom ou description
- **Filtres par catégorie** : Anime, Fantasy, Réaliste
- **Vue rapide du profil** avec bouton info
- **15 personnages disponibles** avec 8 images chacun

### 💬 **Écran Conversations**
- **Liste des conversations en cours** avec preview
- **Timestamp intelligent** (il y a X min/h/j)
- **Badge du nombre de messages**
- **Menu d'options complet** :
  - ✅ Continuer la conversation
  - ➕ Nouvelle conversation avec le personnage
  - 👤 Voir le profil du personnage
  - 🗑️ Supprimer la conversation

### ⚙️ **Paramètres Améliorés**
Séparation claire entre **Membres** et **Administrateur** :

#### Pour TOUS les membres :
- 🔞 **Mode NSFW** (18+) - Activable par tous
- 👤 **Modifier le profil** (nom, pseudo, bio, âge)
- ℹ️ **Informations de compte**

#### Pour l'administrateur (douvdouv21@gmail.com) :
- ⚙️ **Configuration complète des modèles IA locaux**
- 🚀 **Configuration Groq API**
  - Activation/Désactivation
  - Gestion de la clé API
  - Sélection du modèle Groq
- 🎛️ **Contrôle total de l'application**
- 👑 **Badge "Mode Administrateur"**

### 🖼️ **Galerie d'Images pour Chaque Personnage**
- **8 images supplémentaires** par personnage (style anime + réaliste)
- Générées par IA (Pollinations AI)
- Visibles dans le profil du personnage
- Total : **120 images** pour 15 personnages

## 🔧 Améliorations Techniques

### Navigation
- Écran principal (`MainScreen`) avec navigation par onglets
- Gestion des états de navigation
- Retour automatique à l'écran principal

### UI/UX
- **Cards modernisées** avec élévation
- **Couleurs adaptatives** selon le rôle (admin/membre)
- **Icônes Material** cohérentes
- **Animations de transition**

### Performance
- Filtrage optimisé avec `remember`
- StateFlow pour la réactivité
- Chargement asynchrone des images

## 📋 Fonctionnalités Existantes (conservées)

### 💾 Système de Mémoire des Conversations
- Sauvegarde automatique avec DataStore
- Reprise de conversations à tout moment
- Historique complet des messages

### 👥 Système d'Inscription & Profils
- Inscription avec email, nom, pseudo, bio, âge
- Page de profil personnalisable
- Le personnage utilise votre pseudo dans les conversations

### 🤖 Moteurs IA
- **Groq API** (ultra-rapide, recommandé)
- **Modèles locaux** (llama.cpp - admin)
- **Fallback intelligent** si pas de connexion

## 🆕 Changements par rapport à v1.4.0

| Avant (v1.4.0) | Après (v1.5.0) |
|----------------|----------------|
| Liste simple de personnages | Grille visuelle avec recherche et filtres |
| Pas d'accès aux conversations | Écran dédié avec options avancées |
| Paramètres identiques pour tous | Séparation membre/admin |
| Navigation linéaire | Bottom Navigation Bar |
| Images basiques | 8 images IA par personnage |

## 📱 Compatibilité

- **Android 7.0+** (API 24+)
- **RAM recommandée** : 2 GB minimum
- **Stockage** : ~15 MB (sans modèles locaux)
- **Connexion Internet** : Recommandée pour Groq API

## 🔐 Accès Administrateur

**Email admin** : douvdouv21@gmail.com

L'administrateur a accès à :
- Configuration des modèles IA locaux
- Gestion complète de Groq API
- Toutes les fonctionnalités avancées

Les membres ont accès à :
- Mode NSFW
- Modification de profil
- Toutes les fonctionnalités de chat et personnages

## 📝 Notes Techniques

### Écrans créés/modifiés :
- ✨ **ExplorerScreen.kt** (nouveau)
- ✨ **ChatsScreen.kt** (nouveau)
- ✨ **MainScreen.kt** (nouveau)
- ♻️ **SettingsScreen.kt** (refonte complète)
- ♻️ **Navigation.kt** (mise à jour)

### Dépendances :
- AndroidX Compose Material 3
- Coil (chargement d'images)
- DataStore (persistance)
- Kotlinx Serialization

## 🐛 Corrections

- ✅ Conflit de noms de fonctions résolu
- ✅ Imports optimisés
- ✅ Structure de code améliorée
- ✅ Gestion d'état corrigée

## 🚀 Installation

1. Téléchargez `RolePlayAI-v1.5.0-signed.apk`
2. Activez "Sources inconnues" sur Android
3. Installez l'APK
4. Lancez et inscrivez-vous !

## 🙏 Remerciements

Merci à tous les utilisateurs pour leurs retours ! Cette version apporte une expérience utilisateur largement améliorée.

---

**Version** : 1.5.0  
**Date** : 10 décembre 2024  
**Taille APK** : ~11 MB  
**Personnages** : 15 (120 images)
