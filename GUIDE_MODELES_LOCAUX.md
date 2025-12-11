# 📱 Guide des Modèles Locaux - RolePlay AI v5.0.0

## 🎯 Pourquoi des Modèles Locaux ?

Les modèles locaux offrent :
- ✅ **0 latence** - Réponse instantanée
- ✅ **0 coût** - Pas d'API payante
- ✅ **Privacy totale** - Tout reste sur votre téléphone
- ✅ **Offline** - Fonctionne sans internet
- ✅ **Illimité** - Pas de rate limits

---

## 🧠 Modèles Recommandés

### 1. Gemini Nano (Recommandé #1)

**Le meilleur choix pour Android !**

- **Type** : IA on-device de Google
- **Taille** : Intégré dans Android 14+
- **Qualité** : ⭐⭐⭐⭐⭐ Excellente
- **Vitesse** : ⚡⚡⚡⚡⚡ 2-5 secondes
- **Installation** : Automatique

**Prérequis** :
- Android 14 ou supérieur
- Google Play Services à jour
- Appareil compatible (Pixel 8+, certains Samsung/OnePlus récents)

**Activation** :
1. Aller dans Paramètres Android
2. Apps > Google > AI Core
3. Activer "Gemini Nano"

**Avantages** :
- Qualité équivalente à Groq
- Très rapide
- Pas de téléchargement manuel
- Support NSFW

---

### 2. Phi-3 Mini 4K (Q4) - Recommandé #2

**Excellent compromis qualité/taille**

- **Taille** : 2.2 GB
- **Qualité** : ⭐⭐⭐⭐⭐ Excellente
- **Vitesse** : ⚡⚡⚡⚡ 3-8 secondes
- **Mémoire** : 4096 tokens de contexte

**Téléchargement** :
```
https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf
```

**Installation** :
1. Télécharger le fichier `.gguf`
2. Copier dans : `/sdcard/RolePlayAI/models/`
3. Dans l'app : Paramètres > IA Locale > Sélectionner Phi-3

**Pourquoi Phi-3 ?**
- Créé par Microsoft
- Optimisé pour conversationsroleplay
- Excellente compréhension du contexte
- Support multilingue (français parfait)
- Très bon en NSFW (naturel)

---

### 3. Gemma 2B (Q4) - Léger et Rapide

**Pour les appareils avec moins de RAM**

- **Taille** : 1.5 GB
- **Qualité** : ⭐⭐⭐⭐ Très bonne
- **Vitesse** : ⚡⚡⚡⚡⚡ 2-5 secondes
- **Mémoire** : 2048 tokens de contexte

**Téléchargement** :
```
https://huggingface.co/google/gemma-2b-it-gguf/resolve/main/gemma-2b-it-q4_k_m.gguf
```

**Installation** :
1. Télécharger le fichier `.gguf`
2. Copier dans : `/sdcard/RolePlayAI/models/`
3. Dans l'app : Paramètres > IA Locale > Sélectionner Gemma

**Pourquoi Gemma ?**
- Créé par Google
- Très léger (fonctionne sur 4GB RAM)
- Rapide
- Bon pour conversations courtes

---

### 4. TinyLlama 1.1B (Q4) - Ultra-Léger

**Pour les appareils bas de gamme**

- **Taille** : 630 MB
- **Qualité** : ⭐⭐⭐ Bonne
- **Vitesse** : ⚡⚡⚡⚡⚡ 1-3 secondes
- **Mémoire** : 2048 tokens de contexte

**Téléchargement** :
```
https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.q4_k_m.gguf
```

**Installation** :
1. Télécharger le fichier `.gguf`
2. Copier dans : `/sdcard/RolePlayAI/models/`
3. Dans l'app : Paramètres > IA Locale > Sélectionner TinyLlama

**Pourquoi TinyLlama ?**
- Très petit (< 1GB)
- Fonctionne sur 2GB RAM
- Ultra-rapide
- Bon pour texte simple

---

## 📊 Comparaison

| Modèle | Taille | RAM Min | Vitesse | Qualité | NSFW | Contexte |
|--------|--------|---------|---------|---------|------|----------|
| **Gemini Nano** | 0 GB | 4 GB | ⚡⚡⚡⚡⚡ | ⭐⭐⭐⭐⭐ | ✅ | 8K |
| **Phi-3 Mini** | 2.2 GB | 6 GB | ⚡⚡⚡⚡ | ⭐⭐⭐⭐⭐ | ✅ | 4K |
| **Gemma 2B** | 1.5 GB | 4 GB | ⚡⚡⚡⚡⚡ | ⭐⭐⭐⭐ | ✅ | 2K |
| **TinyLlama** | 630 MB | 2 GB | ⚡⚡⚡⚡⚡ | ⭐⭐⭐ | ⚠️  | 2K |

---

## 🎯 Quel Modèle Choisir ?

### Si vous avez Android 14+ (Pixel 8, Samsung S24, etc.)
➡️ **Gemini Nano** - Pas de téléchargement, qualité maximale

### Si vous avez un flagship récent (8GB+ RAM)
➡️ **Phi-3 Mini** - Meilleur compromis qualité/vitesse

### Si vous avez 4-6GB RAM
➡️ **Gemma 2B** - Léger et rapide

### Si vous avez 2-4GB RAM
➡️ **TinyLlama** - Ultra-léger

---

## ⚙️ Installation Détaillée

### Étape 1 : Télécharger le Modèle

**Option A : Via navigateur**
1. Cliquer sur le lien de téléchargement
2. Attendre la fin (peut prendre 5-30 minutes)
3. Le fichier est dans `/sdcard/Download/`

**Option B : Via termux (plus rapide)**
```bash
pkg install wget
cd /sdcard/RolePlayAI/models/
wget https://huggingface.co/.../model.gguf
```

### Étape 2 : Placer le Modèle

1. Ouvrir un gestionnaire de fichiers
2. Aller dans `/sdcard/Download/`
3. Créer le dossier `/sdcard/RolePlayAI/models/` si inexistant
4. Déplacer le fichier `.gguf` dedans

### Étape 3 : Configurer dans l'App

1. Ouvrir RolePlay AI
2. Menu > Paramètres
3. **IA Locale** section
4. "Sélectionner un modèle local"
5. Choisir le fichier `.gguf`
6. Activer "Préférer IA locale"
7. Sauvegarder

### Étape 4 : Tester

1. Créer un nouveau personnage
2. Désactiver Groq (pour forcer l'IA locale)
3. Envoyer un message
4. Observer les logs :
   ```
   OptimizedLocalLLM: ✅ Modèle chargé
   AIOrchestrator: ✅ Réponse générée par LLM Local
   ```

---

## 🔧 Optimisation

### Augmenter la Vitesse

**Dans les paramètres de l'app** :
- Réduire "Tokens max" à 200
- Augmenter "Threads" au nombre de cores CPU
- Activer "Utiliser GPU" si disponible

### Économiser la Batterie

- Utiliser TinyLlama pour conversations simples
- Désactiver GPU si non nécessaire
- Limiter le contexte à 2048 tokens

### Améliorer la Qualité

- Utiliser Phi-3 Mini ou Gemini Nano
- Augmenter le contexte à 4096 tokens
- Temperature à 0.85-0.9

---

## 🧠 Système de Mémoire

**Nouveau dans v5.0.0 !**

Tous les modèles utilisent maintenant **ConversationMemory** :

✅ **Mémoire Long Terme**
- Sauvegarde de l'historique complet
- Extraction automatique des faits (nom, préférences...)
- Résumés tous les 20 messages

✅ **Cohérence Maximale**
- Niveau de relation (0-100) qui évolue
- Moments clés sauvegardés
- Contexte pertinent récupéré automatiquement

✅ **Immersion Totale**
- Le personnage se souvient de TOUT
- Évolution progressive réaliste
- Pas de répétitions ni incohérences

**Exemple** :
```
Message 5 : "Je m'appelle Alex"
→ Mémoire : nom_utilisateur = Alex

Message 50 : "Tu te souviens de mon nom ?"
→ Réponse : "Bien sûr, Alex ! Comment pourrais-je oublier ?"
```

---

## ⚠️ Problèmes Courants

### "Modèle non chargé"
- Vérifier que le fichier existe dans `/sdcard/RolePlayAI/models/`
- Vérifier l'espace disque (besoin de 2x la taille du modèle)
- Redémarrer l'application

### "Out of memory"
- Votre appareil n'a pas assez de RAM
- Essayer un modèle plus petit (Gemma ou TinyLlama)
- Fermer les autres applications

### "Génération très lente"
- Normal au premier lancement (chargement du modèle)
- Vérifier le nombre de threads (2-4 optimal)
- Essayer un modèle plus petit

### "Réponses incohérentes"
- Le modèle est trop petit (essayer Phi-3)
- Augmenter le contexte dans les paramètres
- Vérifier que ConversationMemory est activée

---

## 📈 Performances Attendues

### Phi-3 Mini (2.2GB) sur Snapdragon 888

- **Chargement** : 3-5 secondes
- **Première génération** : 5-8 secondes
- **Générations suivantes** : 3-5 secondes
- **Qualité** : Équivalente à GPT-3.5

### Gemini Nano sur Pixel 8

- **Chargement** : Instantané
- **Génération** : 2-4 secondes
- **Qualité** : Équivalente à GPT-4

---

## 🎉 Avantages de v5.0.0

**Avant (v4.0.0)** :
- Templates fixes
- Pas de mémoire
- Incohérences

**Maintenant (v5.0.0)** :
- ✅ Vrais LLM locaux (Gemini Nano, Phi-3, etc.)
- ✅ Mémoire long terme (RAG)
- ✅ Cohérence maximale
- ✅ 6 niveaux de fallback
- ✅ Offline complet possible

---

## 📞 Support

**Problème avec un modèle ?**
1. Vérifier les logs dans l'app (Menu > Logs)
2. Partager le message d'erreur
3. Indiquer votre appareil et modèle choisi

**Besoin d'aide ?**
- GitHub Issues
- Discord communautaire
- Email support

---

**Profitez de vos conversations avec de vraies IA locales ! 🚀**
