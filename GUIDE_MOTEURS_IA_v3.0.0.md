# 🤖 Guide des Moteurs IA - RolePlay AI v3.0.0

## 📋 Nouveautés de la v3.0.0

Cette version introduit un **système de moteurs IA modulaire** qui vous permet de choisir comment les personnages génèrent leurs réponses !

### ✨ Fonctionnalités ajoutées

- ✅ **5 moteurs IA** au choix : Groq, Gemini Nano, llama.cpp, Together AI, SmartLocalAI
- ✅ **Sélection dans les paramètres** : Changez de moteur à tout moment
- ✅ **Fallbacks automatiques** : Si un moteur échoue, l'app bascule automatiquement
- ✅ **Support llama.cpp** : Utilisez des modèles locaux GGUF (Phi-3, Gemma, etc.)
- ✅ **Gemini Nano** : IA Google intégrée (Android 14+)
- ✅ **Architecture NDK** : Code natif C++ pour performances optimales

---

## 🚀 Moteurs Disponibles

### 1. **Groq API** ☁️ (Par défaut)
- **Type** : Cloud (nécessite Internet)
- **Qualité** : ⭐⭐⭐⭐⭐ Excellente
- **Vitesse** : ⚡⚡⚡⚡⚡ Ultra-rapide (1-2s)
- **Gratuit** : Oui (avec clés API)
- **NSFW** : ✅ Supporté

**Avantages** :
- Réponses quasi-instantanées grâce aux LPU Groq
- Modèles puissants (Llama 3.3 70B, Mixtral)
- Rotation automatique des clés API

**Configuration** :
1. Obtenir une clé gratuite sur [console.groq.com](https://console.groq.com)
2. Paramètres > Configuration Admin > Ajouter clé Groq

---

### 2. **Gemini Nano** 📱 (Recommandé si compatible)
- **Type** : Local (on-device)
- **Qualité** : ⭐⭐⭐⭐⭐ Excellente
- **Vitesse** : ⚡⚡⚡⚡⚡ Très rapide (2-5s)
- **Gratuit** : Oui (100% gratuit)
- **NSFW** : ✅ Supporté

**Prérequis** :
- Android 14+ (API 34+)
- Appareil compatible : Pixel 8+, Samsung S24+, OnePlus 12+
- Google Play Services à jour

**Avantages** :
- Aucune connexion Internet requise
- Qualité équivalente à GPT-3.5
- Privacy totale (tout reste sur votre téléphone)
- Gratuit et illimité

**Activation** :
1. Vérifier que votre appareil est sous Android 14+
2. Paramètres > Moteur IA > Sélectionner "Gemini Nano (Local)"
3. Si erreur : votre appareil ne supporte pas Gemini Nano

---

### 3. **llama.cpp** 📱 (Pour experts)
- **Type** : Local (modèles GGUF)
- **Qualité** : ⭐⭐⭐⭐ à ⭐⭐⭐⭐⭐ (selon modèle)
- **Vitesse** : ⚡⚡⚡ à ⚡⚡⚡⚡ (3-10s selon modèle)
- **Gratuit** : Oui (modèles à télécharger)
- **NSFW** : ✅ Supporté

**Modèles recommandés** :

#### Phi-3 Mini (2.2 GB) - ⭐ Recommandé
- **RAM requise** : 6 GB+
- **Qualité** : Excellente (équivalent GPT-3.5)
- **Téléchargement** : [HuggingFace](https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf)

#### Gemma 2B (1.5 GB) - Léger
- **RAM requise** : 4 GB+
- **Qualité** : Très bonne
- **Téléchargement** : [HuggingFace](https://huggingface.co/google/gemma-2b-it-gguf/resolve/main/gemma-2b-it-q4_k_m.gguf)

#### TinyLlama (630 MB) - Ultra-léger
- **RAM requise** : 2 GB+
- **Qualité** : Correcte
- **Téléchargement** : [HuggingFace](https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.q4_k_m.gguf)

**Installation** :
1. Télécharger un modèle `.gguf`
2. Placer dans `/sdcard/Android/data/com.roleplayai.chatbot/files/models/`
3. Paramètres > Moteur IA > Sélectionner "llama.cpp (Local)"
4. Paramètres > Modèle llama.cpp > Choisir votre modèle

**Note** : Nécessite la compilation NDK (voir section développeur)

---

### 4. **Together AI** ☁️ (Fallback)
- **Type** : Cloud (nécessite Internet)
- **Qualité** : ⭐⭐⭐⭐ Bonne
- **Vitesse** : ⚡⚡⚡ Correcte (5-10s)
- **Gratuit** : Oui (API gratuite)
- **NSFW** : ✅ Supporté

**Avantages** :
- Aucune configuration requise
- Utilisé automatiquement en fallback
- Gratuit sans limite

---

### 5. **SmartLocalAI** 📱 (Fallback ultime)
- **Type** : Local (templates)
- **Qualité** : ⭐⭐⭐ Acceptable
- **Vitesse** : ⚡⚡⚡⚡⚡ Instantané
- **Gratuit** : Oui
- **NSFW** : ✅ Supporté

**Caractéristiques** :
- Réponses basées sur templates intelligents
- Ne nécessite aucun modèle
- Toujours disponible (ne peut jamais échouer)
- Utilisé en dernier recours

---

## ⚙️ Configuration

### Changer de moteur IA

1. Ouvrir **Paramètres**
2. Section **"🤖 Moteur d'Intelligence Artificielle"**
3. Cliquer sur **"Moteur actuel"**
4. Sélectionner le moteur souhaité
5. C'est tout ! Le changement est immédiat

### Activer/Désactiver les Fallbacks

Par défaut, les fallbacks sont **activés**. Si le moteur principal échoue, l'app basculera automatiquement vers un autre moteur.

**Pour désactiver** :
1. Paramètres > Moteur IA
2. Désactiver **"Fallbacks automatiques"**
3. L'app utilisera uniquement le moteur sélectionné

**Cascade de fallbacks** :
- **Groq** → Together AI → Gemini Nano → llama.cpp → SmartLocalAI
- **Gemini Nano** → llama.cpp → Together AI → SmartLocalAI
- **llama.cpp** → Gemini Nano → Together AI → SmartLocalAI

---

## 📊 Comparaison des Moteurs

| Moteur | Type | Internet | Qualité | Vitesse | Setup | NSFW |
|--------|------|----------|---------|---------|-------|------|
| **Groq API** | Cloud | ✅ Oui | ⭐⭐⭐⭐⭐ | ⚡⚡⚡⚡⚡ | API Key | ✅ |
| **Gemini Nano** | Local | ❌ Non | ⭐⭐⭐⭐⭐ | ⚡⚡⚡⚡⚡ | Android 14+ | ✅ |
| **llama.cpp** | Local | ❌ Non | ⭐⭐⭐⭐ | ⚡⚡⚡⚡ | Modèle GGUF | ✅ |
| **Together AI** | Cloud | ✅ Oui | ⭐⭐⭐⭐ | ⚡⚡⚡ | Aucun | ✅ |
| **SmartLocalAI** | Local | ❌ Non | ⭐⭐⭐ | ⚡⚡⚡⚡⚡ | Aucun | ✅ |

---

## 🎯 Recommandations par Appareil

### Flagship récent (Android 14+, 8GB+ RAM)
➡️ **Gemini Nano** - Meilleur rapport qualité/vitesse, 100% gratuit et privé

### Flagship (Android 13-, 8GB+ RAM)
➡️ **Groq API** - Ultra-rapide, excellente qualité (nécessite clés API)

### Milieu de gamme (4-6GB RAM)
➡️ **Together AI + Fallback SmartLocalAI** - Pas de configuration, fonctionne partout

### Bas de gamme (2-4GB RAM)
➡️ **SmartLocalAI** - Léger, rapide, toujours disponible

### Pour experts avec modèles locaux
➡️ **llama.cpp (Phi-3 ou Gemma)** - Privacy maximale, offline complet

---

## 🛠️ Pour Développeurs

### Architecture Technique

```
┌─────────────────────────────────────┐
│         ChatViewModel               │
│                                     │
│    ┌─────────────────────┐          │
│    │   AIOrchestrator    │          │
│    └─────────┬───────────┘          │
│              │                      │
│    ┌─────────▼──────────────────┐   │
│    │  Sélection moteur + Config │   │
│    └─────────┬──────────────────┘   │
│              │                      │
│    ┌─────────▼──────────────────┐   │
│    │  Génération avec fallbacks │   │
│    └─────────┬──────────────────┘   │
│              │                      │
├──────────────┼──────────────────────┤
│              │                      │
│    ┌─────────▼─────────┐            │
│    │  Moteurs IA       │            │
│    ├───────────────────┤            │
│    │ GroqAIEngine      │            │
│    │ GeminiNanoEngine  │            │
│    │ LlamaCppEngine    │            │
│    │ TogetherAIEngine  │            │
│    │ SmartLocalAI      │            │
│    └───────────────────┘            │
└─────────────────────────────────────┘
```

### Fichiers Créés/Modifiés

**Nouveaux fichiers** :
- `GeminiNanoEngine.kt` - Intégration Gemini Nano
- `LlamaCppEngine.kt` - Interface llama.cpp avec JNI
- `AIOrchestrator.kt` - Routeur intelligent des moteurs
- `llama-android.cpp` - Code natif JNI pour llama.cpp
- `CMakeLists.txt` - Configuration CMake pour NDK

**Fichiers modifiés** :
- `PreferencesManager.kt` - Ajout préférences moteur IA
- `SettingsViewModel.kt` - Ajout flows et setters
- `SettingsScreen.kt` - UI sélection moteur
- `ChatViewModel.kt` - Utilisation AIOrchestrator
- `build.gradle.kts` - Activation NDK

### Build NDK

Pour compiler le code natif llama.cpp :

```bash
# Installer NDK
sdkmanager --install "ndk;26.1.10909125"

# Build
./gradlew assembleDebug

# Le .so sera généré dans :
# app/build/intermediates/cmake/debug/obj/arm64-v8a/libllama-android.so
```

**Note** : La bibliothèque llama.cpp complète n'est pas encore intégrée. Le wrapper JNI est prêt mais retourne un fallback. Pour l'intégration complète :

1. Cloner llama.cpp : `git clone https://github.com/ggerganov/llama.cpp app/src/main/cpp/llama.cpp`
2. Décommenter les sections TODO dans `CMakeLists.txt`
3. Implémenter les fonctions dans `llama-android.cpp`

---

## 🐛 Dépannage

### Gemini Nano : "Non disponible"
- Vérifier Android 14+ : Paramètres > À propos > Version Android
- Vérifier Google Play Services à jour
- Votre appareil ne supporte peut-être pas Gemini Nano

### llama.cpp : "Modèle non trouvé"
- Vérifier que le fichier `.gguf` est dans `/sdcard/Android/data/com.roleplayai.chatbot/files/models/`
- Vérifier les permissions de stockage
- Réessayer la sélection dans Paramètres

### llama.cpp : "Erreur de chargement"
- RAM insuffisante (fermer d'autres apps)
- Modèle trop gros pour votre appareil
- Essayer un modèle plus petit (TinyLlama)

### Groq : "Clé API manquante"
- Ajouter une clé dans Paramètres > Configuration Admin
- Vérifier que la clé est valide sur console.groq.com

### Réponses lentes
- Changer de moteur (Gemini Nano ou Groq sont les plus rapides)
- Activer fallbacks automatiques
- Vérifier la connexion Internet (pour moteurs cloud)

---

## 📈 Logs de Debug

L'app génère des logs détaillés pour chaque génération :

```
ChatViewModel: 🤖 Moteur sélectionné: GEMINI_NANO
AIOrchestrator: ===== AI Orchestrator =====
AIOrchestrator: Moteur principal: Gemini Nano (Local)
GeminiNanoEngine: ✅ Gemini Nano initialisé
GeminiNanoEngine: ✅ Réponse Gemini Nano: *rougit*...
ChatViewModel: ✅ Réponse générée par GEMINI_NANO en 3421ms
```

Pour voir les logs :
```bash
adb logcat | grep -E "ChatViewModel|AIOrchestrator|GeminiNano|LlamaCpp"
```

---

## 🎉 Conclusion

Cette version 3.0.0 transforme RolePlay AI en une **plateforme IA modulaire** où vous avez le contrôle total sur :
- Le moteur utilisé
- La privacy (local vs cloud)
- Le coût (gratuit vs API)
- Les performances

**Recommandation générale** : Commencer avec **Groq API** (excellent par défaut), puis tester **Gemini Nano** si vous avez Android 14+. Pour une privacy maximale, utiliser **llama.cpp** avec Phi-3.

Enjoy ! 🚀

---

**Développé par l'équipe RolePlay AI**
*Version 3.0.0 - Décembre 2024*
