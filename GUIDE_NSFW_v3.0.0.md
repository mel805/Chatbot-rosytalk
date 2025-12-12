# Guide NSFW - RolePlay AI Chatbot v3.0.0 🔞

## ⚠️ Avertissement Important

Ce guide est destiné aux **utilisateurs adultes (18+)** uniquement. Le contenu NSFW (Not Safe For Work) inclut des conversations de nature adulte et explicite.

---

## 🔐 Activation du Mode NSFW

### Étape 1 : Vérification des Prérequis

Pour activer le mode NSFW, vous devez :
1. ✅ **Avoir 18 ans ou plus** (vérifié lors de l'inscription)
2. ✅ **Ne pas être bloqué** par un administrateur
3. ✅ **Avoir un compte actif**

### Étape 2 : Activation

1. Ouvrez l'application
2. Allez dans **Paramètres** ⚙️
3. Trouvez la section **Mode NSFW** 🔞
4. Activez le bouton

**Note** : Si vous ne pouvez pas activer le mode NSFW :
- Vérifiez votre âge dans les paramètres du compte
- Contactez un administrateur si vous pensez qu'il y a une erreur

---

## 🤖 Moteurs IA Compatibles NSFW

### 1. **Groq API** ⚡
- **Modèles recommandés** :
  - `llama-3.1-8b-instant` - Rapide et sans censure
  - `llama-3.3-70b-specdec` - Haute qualité
  - `mixtral-8x7b-32768` - Contexte étendu
- **Censure** : Légère (dépend du modèle)
- **Vitesse** : Ultra-rapide (1-2s)
- **Qualité NSFW** : ⭐⭐⭐⭐

### 2. **OpenRouter** 🔞 ⭐ RECOMMANDÉ
- **Modèles recommandés** :
  - `nousresearch/nous-hermes-2-mixtral-8x7b-dpo` - **Non censuré**, excellent pour NSFW
  - `gryphe/mythomax-l2-13b` - Spécialisé roleplay érotique
  - `koboldai/psyfighter-13b-2` - Creative writing NSFW
- **Censure** : **AUCUNE** (modèles uncensored)
- **Vitesse** : Rapide (2-4s)
- **Qualité NSFW** : ⭐⭐⭐⭐⭐
- **Prix** : Pay-as-you-go (0.05-0.80$ / 1M tokens)

### 3. **Together AI** 🌐
- **Modèles recommandés** :
  - `mistralai/Mistral-7B-Instruct-v0.2`
  - `meta-llama/Llama-3-8b-chat-hf`
- **Censure** : Modérée
- **Vitesse** : Rapide
- **Qualité NSFW** : ⭐⭐⭐

### 4. **Gemini Nano** 🤖
- **Censure** : Forte (Google)
- **Vitesse** : Instantanée (local)
- **Qualité NSFW** : ⭐⭐
- **Note** : Recommandé pour SFW uniquement

### 5. **llama.cpp** 🦙
- **Modèles recommandés NSFW** :
  - `Mistral-7B-OpenOrca-GGUF`
  - `TinyLlama-1.1B-Chat-v1.0-GGUF` (léger)
  - `Phi-3-mini-4k-instruct-GGUF`
- **Censure** : Dépend du modèle
- **Vitesse** : Moyenne (local)
- **Qualité NSFW** : ⭐⭐⭐⭐

---

## 📝 Configuration Recommandée pour NSFW

### Configuration Optimale

#### Pour Qualité Maximum (avec budget)
```
Moteur : OpenRouter
Modèle : nousresearch/nous-hermes-2-mixtral-8x7b-dpo
Fallbacks : Activés
Temperature : 0.9-1.0
```

#### Pour Gratuit
```
Moteur : Groq
Modèle : llama-3.1-8b-instant
Fallbacks : Together AI, Groq rotation
Temperature : 0.9
```

#### Pour 100% Local et Privé
```
Moteur : llama.cpp
Modèle : Mistral-7B-OpenOrca-GGUF (Q4_K_M)
Fallbacks : Désactivés
Temperature : 1.0
```

---

## 🎨 Conseils pour des Réponses NSFW de Qualité

### 1. **Instructions au Personnage**
Utilisez des descriptions de personnages explicites :
```
Personnalité : Audacieuse, sensuelle, directe, sans tabous
Scénario : Après une soirée passionnée, seuls dans l'appartement...
```

### 2. **Contexte et Progression**
- Commencez par de l'ambiance et de la tension
- Progressez graduellement vers le contenu explicite
- Laissez le personnage prendre l'initiative

### 3. **Format de Réponse**
Le format attendu est :
```
*action* (pensée) "dialogue"

Exemple :
*s'approche lentement, le regard intense* (Mon cœur bat la chamade...) "Tu sais ce que tu me fais ?"
```

### 4. **Mots-Clés NSFW**
Pour guider l'IA, utilisez :
- "sensuel", "passionné", "désir", "envie"
- "intime", "câlin", "caresse"
- Descriptions anatomiques (selon le niveau souhaité)

---

## 🔧 Résolution de Problèmes

### Le Mode NSFW ne s'active pas
**Solutions** :
1. Vérifiez votre âge dans les paramètres
2. Déconnectez-vous et reconnectez-vous
3. Contactez un administrateur

### Les Réponses Sont Trop Censurées
**Solutions** :
1. **Changez de moteur** → Passez à OpenRouter
2. **Changez de modèle** → Utilisez un modèle "uncensored"
3. **Augmentez la temperature** → 0.9 ou 1.0
4. **Reformulez** → Utilisez des tournures plus suggestives

### Les Réponses Sont Incohérentes
**Solutions** :
1. Activez les **fallbacks automatiques**
2. Vérifiez que votre **contexte est clair**
3. Donnez plus de détails dans la description du personnage

---

## 🛡️ Sécurité et Confidentialité

### Données Stockées
- ✅ **Messages** : Stockés localement sur votre appareil
- ✅ **Préférences** : Stockées localement
- ⚠️ **API Cloud** : Les messages sont envoyés aux APIs (Groq, OpenRouter, Together AI)
- ✅ **llama.cpp** : 100% local, aucune donnée envoyée

### Recommandations
1. **Pour confidentialité maximale** → Utilisez llama.cpp (local)
2. **Lisez les politiques** des APIs tierces (Groq, OpenRouter, etc.)
3. **N'envoyez jamais** d'informations personnelles sensibles
4. **Mode NSFW** est protégé par vérification d'âge

---

## 📊 Comparaison des Moteurs NSFW

| Moteur | Censure | Vitesse | Qualité | Prix | Local |
|--------|---------|---------|---------|------|-------|
| **OpenRouter** | ⭐⭐⭐⭐⭐ Aucune | ⭐⭐⭐⭐ Rapide | ⭐⭐⭐⭐⭐ Excellent | 💰 Payant | ❌ |
| **Groq** | ⭐⭐⭐⭐ Légère | ⭐⭐⭐⭐⭐ Ultra-rapide | ⭐⭐⭐⭐ Très bon | 🆓 Gratuit | ❌ |
| **llama.cpp** | ⭐⭐⭐⭐ Variable | ⭐⭐⭐ Moyen | ⭐⭐⭐⭐ Bon | 🆓 Gratuit | ✅ |
| **Together AI** | ⭐⭐⭐ Modérée | ⭐⭐⭐⭐ Rapide | ⭐⭐⭐ Correct | 🆓 Gratuit | ❌ |
| **Gemini Nano** | ⭐⭐ Forte | ⭐⭐⭐⭐⭐ Instantané | ⭐⭐ Limité | 🆓 Gratuit | ✅ |

---

## 🆘 Support

### Questions Fréquentes

**Q : Puis-je utiliser NSFW sans compte ?**
R : Non, un compte avec vérification d'âge est requis.

**Q : Mon modèle préféré est censuré, que faire ?**
R : Essayez OpenRouter avec les modèles "uncensored" mentionnés plus haut.

**Q : Les conversations NSFW sont-elles sauvegardées ?**
R : Localement sur votre appareil uniquement (selon vos paramètres).

**Q : Puis-je désactiver temporairement le NSFW ?**
R : Oui, dans Paramètres > Mode NSFW, désactivez le bouton.

### Contact
Pour toute question ou problème, contactez l'équipe de développement via GitHub.

---

## ⚖️ Mentions Légales

- ⚠️ **Réservé aux adultes** (18+)
- ⚠️ **Utilisez de manière responsable**
- ⚠️ **Respectez les lois** de votre pays
- ⚠️ **Consentement** est fondamental dans tout scénario

---

**Version** : 3.0.0  
**Dernière mise à jour** : Décembre 2024
