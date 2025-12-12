# 📱 Migration vers v4.0.2 - Keystore Stable

## ⚠️ Pourquoi désinstaller une dernière fois ?

Les versions précédentes (v3.x, v4.0.0, v4.0.1) utilisaient des keystores **temporaires** différents à chaque build.

La v4.0.2 utilise un **keystore permanent stable** qui permettra toutes les futures mises à jour sans désinstallation.

## 🔄 Procédure de migration (1 seule fois)

### Étape 1 : Sauvegarder vos données (optionnel)

Si vous voulez garder vos conversations et paramètres :

1. Ouvrez l'app actuelle
2. Allez dans **Paramètres** → **À propos**
3. Notez vos **clés API** (Groq, Gemini) si vous en avez configurées
4. Prenez des captures d'écran de vos personnages favoris

### Étape 2 : Désinstaller l'ancienne version

```
Paramètres Android → Applications → RolePlay AI → Désinstaller
```

### Étape 3 : Installer v4.0.2

1. Téléchargez `app-debug.apk` depuis :
   https://github.com/mel805/Chatbot-rosytalk/releases/tag/v4.0.2

2. Installez l'APK

### Étape 4 : Reconfigurer (rapide)

1. Entrez vos clés API si vous en aviez
2. Sélectionnez votre moteur IA préféré
3. Activez le mode NSFW si besoin

## ✅ À partir de maintenant

**Toutes les futures mises à jour** (v4.0.3, v4.1.0, etc.) s'installeront **directement par-dessus** sans désinstallation !

Le keystore stable (`debug.keystore`) est maintenant dans le repository et utilisé pour tous les builds.

## 🎯 Pourquoi ça vaut le coup ?

- ✅ Plus jamais besoin de désinstaller
- ✅ Mises à jour en 1 clic
- ✅ llama.cpp fonctionne toujours (pas de "tous les moteurs indisponibles")
- ✅ Gemini 1.5 intégré
- ✅ 3 moteurs IA stables

## 💡 Astuce pour garder vos données à l'avenir

Après avoir installé v4.0.2, vos données seront préservées lors des futures mises à jour car le `applicationId` reste le même et le keystore est stable.
