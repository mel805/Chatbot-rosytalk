# Configuration de l'API IA - RolePlay AI

## 🤖 Options d'IA Disponibles

RolePlay AI supporte plusieurs backends d'IA :

1. **HuggingFace Inference API** (Par défaut - Gratuit)
2. **API Locale** (LM Studio, Ollama, etc.)
3. **OpenAI Compatible APIs**

## 1️⃣ HuggingFace (Recommandé pour débuter)

### Avantages
- ✅ Gratuit
- ✅ Aucune installation requise
- ✅ Fonctionne immédiatement
- ✅ Modèle puissant (Mistral-7B)

### Limitations
- ⚠️ Nécessite Internet
- ⚠️ Rate limiting possible
- ⚠️ Peut être lent aux heures de pointe

### Configuration

#### Sans Token (Limite de débit)

L'application fonctionne directement sans configuration !

#### Avec Token (Recommandé)

Pour de meilleures performances :

1. **Créer un compte HuggingFace**
   - Aller sur https://huggingface.co/join
   - Créer un compte gratuit

2. **Obtenir un Access Token**
   - Aller dans Settings > Access Tokens
   - Créer un nouveau token (Read only suffit)
   - Copier le token

3. **Configurer dans l'application**

Modifier `AIEngine.kt` ligne ~19 :

```kotlin
private var apiKey = "hf_xxxxxxxxxxxxxxxxxxxxx" // Votre token ici
```

Ou créer un écran de paramètres dans l'app (à venir).

### Modèles Disponibles

**Actuellement utilisé** : `mistralai/Mistral-7B-Instruct-v0.2`

**Alternatives possibles** :
```kotlin
// Dans AIEngine.kt, changer apiEndpoint

// Mistral (actuel)
"https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.2"

// Llama 2
"https://api-inference.huggingface.co/models/meta-llama/Llama-2-7b-chat-hf"

// Falcon
"https://api-inference.huggingface.co/models/tiiuae/falcon-7b-instruct"

// Zephyr
"https://api-inference.huggingface.co/models/HuggingFaceH4/zephyr-7b-beta"
```

### Exemple d'Intégration

```kotlin
val aiEngine = AIEngine(context)

// Utiliser le token HuggingFace
aiEngine.setAPIKey("hf_xxxxxxxxxxxxx")

// Optionnel : Changer de modèle
aiEngine.setAPIEndpoint(
    "https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.2"
)
```

## 2️⃣ API Locale (Pour utilisateurs avancés)

### Avantages
- ✅ Pas de limite de débit
- ✅ Confidentialité totale
- ✅ Pas de connexion Internet nécessaire
- ✅ Personnalisation complète

### Limitations
- ⚠️ Nécessite un serveur local
- ⚠️ Configuration technique
- ⚠️ Ressources machine importantes

### Option A : LM Studio

**LM Studio** est un client desktop pour exécuter des LLM localement.

#### Installation

1. **Télécharger LM Studio**
   - Site : https://lmstudio.ai/
   - Télécharger pour Windows/Mac/Linux
   - Installer l'application

2. **Télécharger un Modèle**
   - Ouvrir LM Studio
   - Aller dans l'onglet "Discover"
   - Chercher "Mistral-7B-Instruct"
   - Télécharger une version GGUF (ex: Q4_K_M)
   - Attendre le téléchargement (4-8 GB)

3. **Démarrer le Serveur**
   - Aller dans l'onglet "Local Server"
   - Sélectionner le modèle téléchargé
   - Cliquer sur "Start Server"
   - Le serveur démarre sur `http://localhost:1234`

4. **Configurer l'Application Android**

   **Important** : Votre téléphone doit être sur le même réseau WiFi que votre PC.

   Trouver l'IP de votre PC :
   ```bash
   # Windows
   ipconfig
   
   # Mac/Linux
   ifconfig
   ```

   Modifier `ChatViewModel.kt` ou `AIEngine.kt` :

   ```kotlin
   val aiEngine = AIEngine(application)
   
   // Remplacer localhost par l'IP de votre PC
   aiEngine.setUseLocalAPI(true, "http://192.168.1.XXX:1234/v1/chat/completions")
   ```

### Option B : Ollama

**Ollama** est une alternative légère à LM Studio.

#### Installation

1. **Installer Ollama**
   ```bash
   # Mac/Linux
   curl -fsSL https://ollama.com/install.sh | sh
   
   # Ou télécharger depuis https://ollama.com/download
   ```

2. **Télécharger un Modèle**
   ```bash
   ollama pull mistral
   # ou
   ollama pull llama2
   ```

3. **Démarrer le Serveur**
   ```bash
   ollama serve
   ```
   
   Le serveur démarre sur `http://localhost:11434`

4. **Configurer l'Application**
   ```kotlin
   aiEngine.setUseLocalAPI(true, "http://192.168.1.XXX:11434/v1/chat/completions")
   ```

### Option C : Text Generation WebUI

#### Installation

1. **Cloner le Repository**
   ```bash
   git clone https://github.com/oobabooga/text-generation-webui
   cd text-generation-webui
   ```

2. **Installer les Dépendances**
   ```bash
   # Suivre les instructions du README
   pip install -r requirements.txt
   ```

3. **Télécharger un Modèle**
   - Via l'interface web
   - Ou manuellement dans le dossier `models/`

4. **Lancer avec API**
   ```bash
   python server.py --api --listen
   ```

5. **Configurer l'Application**
   ```kotlin
   aiEngine.setUseLocalAPI(true, "http://192.168.1.XXX:5000/v1/chat/completions")
   ```

## 3️⃣ APIs Compatibles OpenAI

### OpenAI API (Payant)

Si vous avez une clé API OpenAI :

```kotlin
val aiEngine = AIEngine(context)

// Utiliser OpenAI
aiEngine.setAPIEndpoint("https://api.openai.com/v1/chat/completions")
aiEngine.setAPIKey("sk-xxxxxxxxxxxxx")

// Note: Nécessite une modification du code pour le format OpenAI
```

**⚠️ Attention** : OpenAI est payant et peut coûter cher avec une utilisation intensive.

### Autres APIs

**APIs compatibles OpenAI** :
- Groq (gratuit, très rapide)
- Together AI
- Anyscale
- Replicate

**Exemple avec Groq** :

1. Créer un compte sur https://console.groq.com/
2. Obtenir une clé API
3. Configurer :

```kotlin
aiEngine.setAPIEndpoint("https://api.groq.com/openai/v1/chat/completions")
aiEngine.setAPIKey("gsk_xxxxxxxxxxxxx")
```

## 🔧 Configuration Avancée

### Modifier les Paramètres de Génération

Dans `AIEngine.kt`, ajuster les paramètres :

```kotlin
data class HFParameters(
    @SerializedName("max_new_tokens")
    val maxNewTokens: Int = 500,  // Longueur max de réponse
    
    @SerializedName("temperature")
    val temperature: Float = 0.9,  // Créativité (0.1-1.0)
    
    @SerializedName("top_p")
    val topP: Float = 0.95,  // Diversité
    
    @SerializedName("return_full_text")
    val returnFullText: Boolean = false
)
```

**Paramètres expliqués** :

- **max_new_tokens** (100-1000)
  - Plus haut = réponses plus longues
  - Recommandé : 500

- **temperature** (0.1-2.0)
  - Plus bas = plus prévisible
  - Plus haut = plus créatif
  - Recommandé : 0.8-1.0

- **top_p** (0.1-1.0)
  - Contrôle la diversité
  - Recommandé : 0.9-0.95

### Personnaliser le System Prompt

Dans `AIEngine.kt`, fonction `buildPrompt()` :

```kotlin
private fun buildPrompt(character: Character, messages: List<Message>): String {
    val systemPrompt = """
        Tu es ${character.name}.
        Description: ${character.description}
        Personnalité: ${character.personality}
        Scénario: ${character.scenario}
        
        Instructions supplémentaires:
        - Rester dans le personnage à tout moment
        - Être naturel et engageant
        - Adapter le ton à la personnalité
        - Utiliser *action* pour les actions physiques
        - Être cohérent avec l'historique
    """.trimIndent()
    
    // ... rest of the code
}
```

### Gérer le Contexte

**Nombre de messages dans le contexte** :

```kotlin
val conversationHistory = messages.takeLast(10)  // Ajuster ce nombre
```

- Plus petit (5) = Mémoire courte, plus rapide
- Plus grand (20) = Meilleure mémoire, plus lent

### Timeout et Retry

Configurer dans `AIEngine.kt` :

```kotlin
private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)  // Connexion
    .readTimeout(60, TimeUnit.SECONDS)     // Lecture
    .writeTimeout(30, TimeUnit.SECONDS)    // Écriture
    .build()
```

## 📊 Comparaison des Options

| Option | Coût | Vitesse | Qualité | Confidentialité | Difficulté |
|--------|------|---------|---------|-----------------|------------|
| HuggingFace Free | Gratuit | Moyenne | Bonne | Moyenne | Facile ✅ |
| HuggingFace + Token | Gratuit | Bonne | Bonne | Moyenne | Facile ✅ |
| LM Studio | Gratuit | Bonne* | Excellente | Totale | Moyenne |
| Ollama | Gratuit | Bonne* | Excellente | Totale | Moyenne |
| OpenAI API | Payant | Très rapide | Excellente | Faible | Facile |
| Groq | Gratuit | Très rapide | Bonne | Faible | Facile |

*Dépend de votre matériel

## 🛠️ Dépannage

### L'IA ne répond pas

1. **Vérifier la connexion**
   ```bash
   # Test HuggingFace
   curl https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.2
   
   # Test API Locale
   curl http://localhost:1234/v1/models
   ```

2. **Vérifier les logs**
   - Dans Android Studio : Logcat
   - Filtrer par "AIEngine"

3. **Tester l'endpoint directement**
   ```bash
   curl -X POST https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.2 \
     -H "Content-Type: application/json" \
     -d '{"inputs":"Hello, how are you?"}'
   ```

### Erreurs Fréquentes

**"Model is loading"** (HuggingFace)
- Le modèle se charge (première requête)
- Attendre 30 secondes et réessayer

**"Rate limit exceeded"** (HuggingFace)
- Trop de requêtes
- Attendre quelques minutes
- Utiliser un token API

**"Connection refused"** (Local)
- Vérifier que le serveur est démarré
- Vérifier l'IP et le port
- Vérifier le pare-feu

**"Timeout"**
- Augmenter le timeout dans le code
- Vérifier la connexion réseau
- Le modèle peut être trop lent

## 🎯 Recommandations

### Pour Débuter
→ **HuggingFace gratuit** : Aucune configuration, fonctionne immédiatement

### Pour Usage Régulier
→ **HuggingFace + Token** : Meilleures performances, toujours gratuit

### Pour Confidentialité
→ **LM Studio** : Local, privé, pas de connexion Internet

### Pour Performance
→ **Groq API** : Ultra rapide, gratuit avec limites

### Pour Production
→ **OpenAI API** ou **serveur dédié** : Fiable et puissant

## 📚 Ressources

- [HuggingFace Documentation](https://huggingface.co/docs/api-inference)
- [LM Studio](https://lmstudio.ai/)
- [Ollama](https://ollama.com/)
- [Groq](https://console.groq.com/)
- [Text Generation WebUI](https://github.com/oobabooga/text-generation-webui)

---

**Note** : La configuration par défaut (HuggingFace gratuit) fonctionne immédiatement sans aucune modification !
