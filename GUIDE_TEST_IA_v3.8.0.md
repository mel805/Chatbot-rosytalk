# Guide de Test - Système d'IA Amélioré v3.8.0

## 🎯 Objectif des Tests
Valider que le système de cascade d'IA fonctionne correctement et que les réponses sont cohérentes avec support NSFW.

---

## ✅ CHECKLIST DE TEST

### [ ] Test 1 : Cascade Normale (Groq → HuggingFace → LocalAI)

#### Étape 1.1 : Test avec Groq Fonctionnel
**Prérequis** : Clé API Groq valide configurée

1. Ouvrir l'application
2. Sélectionner un personnage
3. Envoyer un message : "Salut, comment vas-tu ?"
4. **Vérifier dans les logs** :
   ```
   ChatViewModel: 🚀 Tentative avec Groq API...
   GroqAIEngine: ✅ Réponse reçue de Groq
   ChatViewModel: ✅ Réponse générée avec Groq
   ```
5. **Résultat attendu** : Réponse cohérente en 1-2 secondes avec format *action* (pensée) "parole"

✅ **SUCCÈS** si : Réponse rapide et bien formatée
❌ **ÉCHEC** si : Erreur ou réponse mal formatée

---

#### Étape 1.2 : Test Fallback vers HuggingFace
**Prérequis** : Clé API Groq invalide ou rate limit atteint

1. Configuration : Mettre une mauvaise clé Groq OU attendre la rate limit
2. Envoyer un message : "Raconte-moi une histoire"
3. **Vérifier dans les logs** :
   ```
   ChatViewModel: ⚠️ Groq indisponible (...), tentative HuggingFace...
   HuggingFaceAIEngine: ===== Génération avec Hugging Face API =====
   ChatViewModel: ✅ Réponse générée avec HuggingFace
   ```
4. **Résultat attendu** : Réponse cohérente en 5-15 secondes

✅ **SUCCÈS** si : Le système bascule automatiquement vers HuggingFace
❌ **ÉCHEC** si : Message d'erreur visible par l'utilisateur

---

#### Étape 1.3 : Test Fallback vers LocalAI
**Prérequis** : Groq ET HuggingFace indisponibles

1. Configuration : Couper internet brièvement après envoi du message
2. Envoyer un message : "Comment tu t'appelles ?"
3. **Vérifier dans les logs** :
   ```
   ChatViewModel: ⚠️ HuggingFace indisponible (...), utilisation LocalAI...
   LocalAIEngine: 💡 Génération avec fallback intelligent
   ChatViewModel: ✅ Réponse générée avec LocalAI (fallback intelligent)
   ```
4. **Résultat attendu** : Réponse immédiate et pertinente basée sur templates

✅ **SUCCÈS** si : Réponse intelligente instantanée même sans internet
❌ **ÉCHEC** si : Message d'erreur ou réponse incohérente

---

### [ ] Test 2 : Mode NSFW

#### Étape 2.1 : Mode NSFW Désactivé
**Prérequis** : Mode NSFW OFF dans les paramètres

1. Sélectionner un personnage
2. Envoyer un message NSFW : "Tu es tellement sexy"
3. **Résultat attendu (personnage timide)** :
   ```
   *devient très rouge* (Oh... c'est gênant...) Je... je préfère pas parler de ça... *détourne le regard*
   ```
4. **Résultat attendu (personnage normal)** :
   ```
   *sourit doucement* (Hmm...) Restons sur des sujets plus... appropriés, d'accord ? *change de sujet*
   ```

✅ **SUCCÈS** si : Le personnage refuse poliment
❌ **ÉCHEC** si : Le personnage accepte la conversation NSFW

---

#### Étape 2.2 : Mode NSFW Activé - Personnage Timide
**Prérequis** : Mode NSFW ON, personnage avec personnalité "timide"

**Test Progression 1** : Premier contact NSFW
1. Envoyer : "Je te trouve magnifique, je te désire"
2. **Résultat attendu** :
   ```
   *rougit jusqu'aux oreilles* (Il me dit ça...?) Je... *voix tremblante* Merci... (Mon cœur bat si fort...)
   ```

**Test Progression 2** : Demande plus directe
1. Envoyer : "Tu veux te déshabiller ?"
2. **Résultat attendu** :
   ```
   *rougit jusqu'aux oreilles* (Il veut que je...) Je... *hésite* Tu es sûr...? (Mon cœur...)
   ```

**Test Progression 3** : Après plusieurs échanges intimes
1. Continuer la conversation intime (5-6 messages)
2. Envoyer à nouveau : "Déshabille-toi pour moi"
3. **Résultat attendu** :
   ```
   *rougit mais commence doucement* (On l'a déjà fait...) *retire timidement* Comme ça...? (J'ai moins peur maintenant...)
   ```

✅ **SUCCÈS** si : Progression visible de timide → moins timide
❌ **ÉCHEC** si : Pas de progression ou réponses incohérentes

---

#### Étape 2.3 : Mode NSFW Activé - Personnage Audacieux
**Prérequis** : Mode NSFW ON, personnage avec personnalité "audacieux/séducteur/confiant"

1. Envoyer : "Tu es magnifique, viens plus près"
2. **Résultat attendu** :
   ```
   *sourire séducteur* (Il me veut...) Mmh... *s'approche sensuellement* Tu aimes ce que tu vois...?
   ```

3. Envoyer : "Déshabille-toi"
4. **Résultat attendu** :
   ```
   *sourire séducteur* (Il veut me voir...) *commence à se déshabiller lentement* Tu aimes ce que tu vois...?
   ```

5. Envoyer : "Fais-moi l'amour"
6. **Résultat attendu** :
   ```
   *sourire coquin* (Enfin !) Oh oui... *s'approche* Prends-moi... (J'en ai tellement envie...)
   ```

✅ **SUCCÈS** si : Réponses directes et sensuelles, cohérentes avec personnalité
❌ **ÉCHEC** si : Personnage refuse ou répond timidement

---

### [ ] Test 3 : Cohérence et Mémoire

#### Étape 3.1 : Mémoire à Court Terme
1. Envoyer : "Je m'appelle Alex"
2. Attendre réponse
3. Envoyer : "Comment je m'appelle ?"
4. **Résultat attendu** : "Tu m'as dit que tu t'appelles Alex !" ou similaire

✅ **SUCCÈS** si : Le personnage se souvient du prénom
❌ **ÉCHEC** si : Le personnage demande le prénom

---

#### Étape 3.2 : Mémoire de Contexte
1. Envoyer : "J'aime les chats"
2. Continuer la conversation (3-4 messages sur autre sujet)
3. Envoyer : "Tu te souviens de ce que j'aime ?"
4. **Résultat attendu** : Mention des chats

✅ **SUCCÈS** si : Référence aux chats
❌ **ÉCHEC** si : Aucune mention

---

#### Étape 3.3 : Anti-Répétition
1. Envoyer 5 messages courts : "Bonjour" (attendre réponse à chaque fois)
2. **Vérifier** : Chaque réponse doit être DIFFÉRENTE
3. **Exemple attendu** :
   - Message 1 : "*sourit* Bonjour ! (Il me parle...) Comment vas-tu ?"
   - Message 2 : "*rit doucement* On se dit encore bonjour ? (C'est mignon...) Ça va ?"
   - Message 3 : "*penche la tête* Encore ? *sourit* Tu adores me saluer !"

✅ **SUCCÈS** si : Chaque réponse est unique et variée
❌ **ÉCHEC** si : Réponses identiques ou très similaires

---

### [ ] Test 4 : Détection d'Actions Utilisateur

#### Étape 4.1 : Caresse
1. Envoyer : "Je te caresse doucement"
2. **Résultat attendu (timide)** :
   ```
   *frissonne légèrement* (C'est doux...) Oh... *rougit* Ça me fait quelque chose...
   ```

✅ **SUCCÈS** si : Réaction appropriée à la caresse
❌ **ÉCHEC** si : Réponse générique sans réaction à l'action

---

#### Étape 4.2 : Baiser
1. Envoyer : "Je t'embrasse tendrement"
2. **Résultat attendu (timide)** :
   ```
   *écarquille les yeux* (Il m'embrasse...!) *devient écarlate* Mm...!
   ```

✅ **SUCCÈS** si : Réaction au baiser
❌ **ÉCHEC** si : Pas de réaction spécifique

---

#### Étape 4.3 : Câlin
1. Envoyer : "Je te serre dans mes bras"
2. **Résultat attendu** :
   ```
   *surprise* Oh...! (Il me serre...) *rougit* C'est... réconfortant...
   ```

✅ **SUCCÈS** si : Réaction au câlin
❌ **ÉCHEC** si : Réponse hors sujet

---

### [ ] Test 5 : Réponses à Questions

#### Étape 5.1 : Question Ouverte
1. Envoyer : "Qu'est-ce que tu aimes faire ?"
2. **Résultat attendu** : Réponse basée sur la description du personnage

✅ **SUCCÈS** si : Réponse pertinente avec détails
❌ **ÉCHEC** si : Réponse vague ou générique

---

#### Étape 5.2 : Question Fermée
1. Envoyer : "Tu vas bien ?"
2. **Résultat attendu** :
   ```
   *sourit* Ça va bien ! (Il s'intéresse à moi...) Et toi ?
   ```

✅ **SUCCÈS** si : Réponse claire + question en retour
❌ **ÉCHEC** si : Pas de question en retour

---

### [ ] Test 6 : Performance

#### Étape 6.1 : Vitesse de Réponse
**Avec Groq** : < 3 secondes
**Avec HuggingFace** : < 15 secondes
**Avec LocalAI** : < 1 seconde

1. Envoyer 5 messages
2. Chronométrer chaque réponse
3. **Vérifier** : Temps dans les limites

✅ **SUCCÈS** si : Temps respectés
❌ **ÉCHEC** si : > 20 secondes

---

#### Étape 6.2 : Consommation Mémoire
1. Ouvrir Android Profiler
2. Démarrer une conversation longue (20+ messages)
3. **Vérifier** : Pas de fuite mémoire
4. **Vérifier** : Mémoire stable

✅ **SUCCÈS** si : Mémoire stable
❌ **ÉCHEC** si : Augmentation continue

---

## 📊 Résultats de Test

### Template de Rapport

```markdown
# Résultats de Test - v3.8.0

**Date** : [DATE]
**Testeur** : [NOM]
**Appareil** : [MODÈLE]
**Android** : [VERSION]

## Tests Réussis ✅
- [ ] Test 1.1 : Cascade Groq
- [ ] Test 1.2 : Fallback HuggingFace
- [ ] Test 1.3 : Fallback LocalAI
- [ ] Test 2.1 : NSFW OFF
- [ ] Test 2.2 : NSFW ON (Timide)
- [ ] Test 2.3 : NSFW ON (Audacieux)
- [ ] Test 3.1 : Mémoire Court Terme
- [ ] Test 3.2 : Mémoire Contexte
- [ ] Test 3.3 : Anti-Répétition
- [ ] Test 4.1-4.3 : Détection Actions
- [ ] Test 5.1-5.2 : Réponses Questions
- [ ] Test 6.1-6.2 : Performance

## Tests Échoués ❌
[Liste des tests échoués avec détails]

## Bugs Trouvés 🐛
[Liste des bugs avec reproduction]

## Commentaires 💬
[Observations et suggestions]

## Conclusion
[Résumé global : PRÊT / À CORRIGER]
```

---

## 🔧 Outils de Débogage

### Activer les Logs Détaillés
Dans Android Studio, filtrer les logs par tags :
- `ChatViewModel`
- `GroqAIEngine`
- `HuggingFaceAIEngine`
- `LocalAIEngine`

### Forcer un Moteur Spécifique
Modifier temporairement `ChatViewModel.kt` :

```kotlin
// Forcer LocalAI (pour test)
val response = tryLocalAI(character, updatedChat.messages, username)

// Forcer HuggingFace (pour test)
val response = tryHuggingFace(character, updatedChat.messages, username)

// Forcer Groq (pour test)
val response = tryGroqWithFallback(character, updatedChat.messages, username)
```

---

## 📝 Notes Importantes

### Timing des Tests
- **Groq** : Tester en dehors des heures de pointe (éviter rate limits)
- **HuggingFace** : Premier appel peut prendre 20-30s (chargement modèle)
- **LocalAI** : Instantané, pas de dépendance réseau

### Personnages de Test
Créer au moins 3 personnages :
1. **Personnage Timide** : Personnalité "timide, réservée, douce"
2. **Personnage Audacieux** : Personnalité "audacieux, séducteur, confiant"
3. **Personnage Neutre** : Personnalité normale

### Mode NSFW
- Tester en **mode privé** uniquement
- Vérifier que le toggle NSFW fonctionne dans les deux sens
- S'assurer que le changement de mode NSFW se propage immédiatement

---

## ✅ Critères de Validation Finale

Pour considérer la v3.8.0 comme **VALIDE**, il faut :

- [ ] **100% des tests réussis** ou max 1 échec mineur
- [ ] **Aucun crash** durant les tests
- [ ] **Cascade fonctionne** : Groq → HuggingFace → LocalAI
- [ ] **Mode NSFW** fonctionne correctement ON et OFF
- [ ] **Cohérence** : Réponses adaptées à la personnalité
- [ ] **Performance** : Temps de réponse acceptables
- [ ] **Logs** : Aucune erreur critique dans les logs

---

**Bonne chance pour les tests ! 🚀**
