#!/bin/bash
# Script de compilation et release automatique - v3.8.0
# Usage: ./BUILD_AND_RELEASE.sh

set -e  # Arrêter en cas d'erreur

echo "🚀 ===== BUILD AND RELEASE v3.8.0 ====="
echo ""

# Couleurs pour les logs
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Variables
VERSION="3.8.0"
APP_NAME="RolePlayAI"
BRANCH=$(git branch --show-current)

echo -e "${BLUE}📋 Configuration${NC}"
echo "Version: $VERSION"
echo "App: $APP_NAME"
echo "Branche: $BRANCH"
echo ""

# ===== ÉTAPE 1 : VÉRIFICATIONS =====
echo -e "${BLUE}🔍 Étape 1/5 : Vérifications préliminaires${NC}"

# Vérifier que nous sommes sur la bonne branche
if [ "$BRANCH" != "main" ] && [ "$BRANCH" != "master" ]; then
    echo -e "${YELLOW}⚠️  Attention : Vous n'êtes pas sur main/master (branche actuelle: $BRANCH)${NC}"
    read -p "Continuer quand même ? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Vérifier que le SDK Android est installé
if [ -z "$ANDROID_HOME" ]; then
    echo -e "${RED}❌ ANDROID_HOME n'est pas défini !${NC}"
    echo "Installez Android SDK et définissez ANDROID_HOME"
    echo "Exemple: export ANDROID_HOME=/path/to/Android/Sdk"
    exit 1
fi

echo -e "${GREEN}✅ Vérifications OK${NC}"
echo ""

# ===== ÉTAPE 2 : NETTOYAGE =====
echo -e "${BLUE}🧹 Étape 2/5 : Nettoyage${NC}"
./gradlew clean
echo -e "${GREEN}✅ Nettoyage terminé${NC}"
echo ""

# ===== ÉTAPE 3 : COMPILATION =====
echo -e "${BLUE}🔨 Étape 3/5 : Compilation de l'APK${NC}"

# Debug APK
echo "Compilation Debug APK..."
./gradlew assembleDebug

# Release APK (signé)
echo "Compilation Release APK..."
./gradlew assembleRelease

# Vérifier que les APK ont été créés
DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"
RELEASE_APK="app/build/outputs/apk/release/app-release.apk"

if [ ! -f "$DEBUG_APK" ]; then
    echo -e "${RED}❌ Erreur : Debug APK non trouvé${NC}"
    exit 1
fi

if [ ! -f "$RELEASE_APK" ]; then
    echo -e "${YELLOW}⚠️  Release APK non trouvé (signature manquante ?)${NC}"
    echo "Continuant avec Debug APK seulement..."
    RELEASE_APK=""
fi

echo -e "${GREEN}✅ Compilation terminée${NC}"
echo ""

# ===== ÉTAPE 4 : RENOMMAGE =====
echo -e "${BLUE}📦 Étape 4/5 : Préparation des fichiers${NC}"

# Créer dossier release
RELEASE_DIR="release-v${VERSION}"
mkdir -p "$RELEASE_DIR"

# Copier et renommer les APK
cp "$DEBUG_APK" "$RELEASE_DIR/${APP_NAME}-v${VERSION}-debug.apk"
echo "✓ Debug APK copié: ${APP_NAME}-v${VERSION}-debug.apk"

if [ -n "$RELEASE_APK" ]; then
    cp "$RELEASE_APK" "$RELEASE_DIR/${APP_NAME}-v${VERSION}-release.apk"
    echo "✓ Release APK copié: ${APP_NAME}-v${VERSION}-release.apk"
fi

# Copier la documentation
cp RELEASE_NOTES_v3.8.0.md "$RELEASE_DIR/"
cp QUICK_START_v3.8.0.md "$RELEASE_DIR/"
cp AMELIORATIONS_IA_LOCALE_v3.8.0.md "$RELEASE_DIR/"
echo "✓ Documentation copiée"

# Créer un README pour le release
cat > "$RELEASE_DIR/README.md" << 'EOF'
# RolePlay AI v3.8.0 - Cascade Intelligente

## 📦 Fichiers de Release

- `RolePlayAI-v3.8.0-debug.apk` - Version debug (développement)
- `RolePlayAI-v3.8.0-release.apk` - Version release (production) *si disponible*
- `RELEASE_NOTES_v3.8.0.md` - Notes de version complètes
- `QUICK_START_v3.8.0.md` - Guide de démarrage rapide
- `AMELIORATIONS_IA_LOCALE_v3.8.0.md` - Documentation technique

## 🚀 Installation

1. Télécharger `RolePlayAI-v3.8.0-release.apk` (ou debug)
2. Activer "Sources inconnues" sur votre appareil Android
3. Installer l'APK
4. Profiter ! 🎉

## ✨ Nouveautés v3.8.0

- 🆕 Nouveau moteur HuggingFace (gratuit)
- 🔧 Système de cascade Groq → HuggingFace → LocalAI
- 🔐 Support NSFW complet et naturel
- ✅ Disponibilité 99.9%
- 🚀 Plus jamais d'erreur visible

Consultez `RELEASE_NOTES_v3.8.0.md` pour plus de détails.

## 📞 Support

- Documentation : `AMELIORATIONS_IA_LOCALE_v3.8.0.md`
- Quick Start : `QUICK_START_v3.8.0.md`
- GitHub Issues : [Créer une issue]

---

**Version** : 3.8.0 | **Date** : Décembre 2024
EOF

echo -e "${GREEN}✅ Fichiers préparés dans: $RELEASE_DIR/${NC}"
echo ""

# ===== ÉTAPE 5 : INFORMATIONS APK =====
echo -e "${BLUE}📊 Étape 5/5 : Informations des APK${NC}"

if command -v aapt &> /dev/null; then
    echo ""
    echo "Debug APK:"
    aapt dump badging "$RELEASE_DIR/${APP_NAME}-v${VERSION}-debug.apk" | grep -E "(package|sdkVersion|targetSdkVersion)"
    
    if [ -n "$RELEASE_APK" ]; then
        echo ""
        echo "Release APK:"
        aapt dump badging "$RELEASE_DIR/${APP_NAME}-v${VERSION}-release.apk" | grep -E "(package|sdkVersion|targetSdkVersion)"
    fi
else
    echo "aapt non trouvé, informations APK non disponibles"
fi

# Taille des fichiers
echo ""
echo "Tailles des fichiers:"
ls -lh "$RELEASE_DIR"/*.apk | awk '{print $9 ": " $5}'

echo ""
echo -e "${GREEN}✅ Build terminé avec succès !${NC}"
echo ""

# ===== INSTRUCTIONS GITHUB =====
echo -e "${BLUE}📤 Pour créer le release GitHub :${NC}"
echo ""
echo "1. Commiter les changements :"
echo "   git add ."
echo "   git commit -F COMMIT_MESSAGE_v3.8.0.txt"
echo ""
echo "2. Pusher vers GitHub :"
echo "   git push origin $BRANCH"
echo ""
echo "3. Créer un tag :"
echo "   git tag -a v${VERSION} -m \"Release v${VERSION} - Cascade Intelligente\""
echo "   git push origin v${VERSION}"
echo ""
echo "4. Créer le release sur GitHub :"
echo "   gh release create v${VERSION} \\"
echo "     --title \"RolePlay AI v${VERSION} - Cascade Intelligente\" \\"
echo "     --notes-file RELEASE_NOTES_v3.8.0.md \\"
echo "     $RELEASE_DIR/${APP_NAME}-v${VERSION}-*.apk"
echo ""
echo "   OU manuellement sur https://github.com/VOTRE_USERNAME/VOTRE_REPO/releases/new"
echo ""

echo -e "${GREEN}🎉 Tout est prêt pour le release !${NC}"
echo ""
echo "Fichiers dans: $RELEASE_DIR/"
ls -1 "$RELEASE_DIR/"
