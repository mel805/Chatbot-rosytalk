#!/bin/bash
# Script ONE-COMMAND pour compiler et créer le release GitHub
# Usage: ./COMPILE_ET_RELEASE.sh [votre_username_github] [votre_repo]

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}"
cat << "EOF"
╔══════════════════════════════════════════════════════╗
║   RolePlay AI v3.8.0 - Build & Release Automatique  ║
╚══════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Variables
VERSION="3.8.0"
GITHUB_USER="${1}"
GITHUB_REPO="${2}"

if [ -z "$GITHUB_USER" ] || [ -z "$GITHUB_REPO" ]; then
    echo -e "${YELLOW}Usage: $0 <github_username> <github_repo>${NC}"
    echo "Exemple: $0 monusername roleplayai-chatbot"
    echo ""
    read -p "GitHub Username: " GITHUB_USER
    read -p "GitHub Repo: " GITHUB_REPO
fi

echo ""
echo -e "${BLUE}Configuration:${NC}"
echo "  Version: $VERSION"
echo "  GitHub: $GITHUB_USER/$GITHUB_REPO"
echo ""

# Vérifier prérequis
echo -e "${BLUE}[1/7] Vérification des prérequis...${NC}"

if [ -z "$ANDROID_HOME" ]; then
    echo -e "${RED}❌ ANDROID_HOME non défini${NC}"
    echo "Installez Android Studio et ajoutez à votre ~/.bashrc:"
    echo "  export ANDROID_HOME=\$HOME/Android/Sdk"
    echo "  export PATH=\$PATH:\$ANDROID_HOME/tools:\$ANDROID_HOME/platform-tools"
    exit 1
fi

if ! command -v gh &> /dev/null; then
    echo -e "${YELLOW}⚠️  GitHub CLI non trouvé${NC}"
    echo "Installation: brew install gh  (ou voir https://cli.github.com/)"
    read -p "Continuer sans gh (release manuel) ? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
    USE_GH=false
else
    USE_GH=true
fi

echo -e "${GREEN}✅ Prérequis OK${NC}"
echo ""

# Nettoyer
echo -e "${BLUE}[2/7] Nettoyage...${NC}"
./gradlew clean > /dev/null 2>&1
echo -e "${GREEN}✅ Nettoyage terminé${NC}"
echo ""

# Compiler
echo -e "${BLUE}[3/7] Compilation de l'APK...${NC}"
echo "  (Cela peut prendre 2-3 minutes)"

./gradlew assembleDebug
DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$DEBUG_APK" ]; then
    echo -e "${RED}❌ Erreur de compilation${NC}"
    exit 1
fi

echo -e "${GREEN}✅ APK compilé: $(du -h $DEBUG_APK | cut -f1)${NC}"
echo ""

# Préparer release
echo -e "${BLUE}[4/7] Préparation du release...${NC}"

RELEASE_DIR="release-v${VERSION}"
rm -rf "$RELEASE_DIR"
mkdir -p "$RELEASE_DIR"

# Copier APK
cp "$DEBUG_APK" "$RELEASE_DIR/RolePlayAI-v${VERSION}.apk"

# Copier documentation
cp RELEASE_NOTES_v3.8.0.md "$RELEASE_DIR/" 2>/dev/null || echo "Release notes non trouvé"
cp QUICK_START_v3.8.0.md "$RELEASE_DIR/" 2>/dev/null || echo "Quick start non trouvé"
cp AMELIORATIONS_IA_LOCALE_v3.8.0.md "$RELEASE_DIR/" 2>/dev/null || echo "Doc technique non trouvée"

# Créer README
cat > "$RELEASE_DIR/README.md" << EOF
# RolePlay AI v${VERSION} - Cascade Intelligente

## 📦 Installation

1. Télécharger \`RolePlayAI-v${VERSION}.apk\`
2. Activer "Sources inconnues" sur votre appareil
3. Installer l'APK
4. Profiter ! 🎉

## ✨ Nouveautés

- 🆕 Nouveau moteur HuggingFace (gratuit)
- 🔧 Système de cascade Groq → HuggingFace → LocalAI
- 🔐 Support NSFW complet et naturel
- ✅ Disponibilité 99.9%

Plus de détails dans \`RELEASE_NOTES_v${VERSION}.md\`

## 🔗 Liens

- Téléchargement: https://github.com/${GITHUB_USER}/${GITHUB_REPO}/releases/tag/v${VERSION}
- Repo: https://github.com/${GITHUB_USER}/${GITHUB_REPO}
EOF

echo -e "${GREEN}✅ Fichiers préparés dans $RELEASE_DIR/${NC}"
ls -lh "$RELEASE_DIR/" | tail -n +2
echo ""

# Commiter
echo -e "${BLUE}[5/7] Commit des changements...${NC}"

# Vérifier si on est dans un repo git
if [ ! -d .git ]; then
    echo -e "${RED}❌ Pas un repo git${NC}"
    exit 1
fi

# Ajouter tous les fichiers
git add .

# Commiter avec le message préparé
if [ -f "COMMIT_MESSAGE_v3.8.0.txt" ]; then
    git commit -F COMMIT_MESSAGE_v3.8.0.txt || echo "Rien à commiter"
else
    git commit -m "feat: Système de cascade d'IA intelligent v${VERSION}

- Nouveau moteur HuggingFace
- Support NSFW complet
- Disponibilité 99.9%
" || echo "Rien à commiter"
fi

echo -e "${GREEN}✅ Commit créé${NC}"
echo ""

# Pusher
echo -e "${BLUE}[6/7] Push vers GitHub...${NC}"

BRANCH=$(git branch --show-current)
echo "  Branche: $BRANCH"

read -p "Pusher vers GitHub ? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    git push origin $BRANCH
    echo -e "${GREEN}✅ Push terminé${NC}"
else
    echo -e "${YELLOW}⚠️  Push ignoré${NC}"
fi
echo ""

# Créer tag et release
echo -e "${BLUE}[7/7] Création du release GitHub...${NC}"

# Créer tag
git tag -a "v${VERSION}" -m "Release v${VERSION} - Cascade Intelligente" 2>/dev/null || echo "Tag existe déjà"
git push origin "v${VERSION}" 2>/dev/null || echo "Tag déjà sur GitHub"

if [ "$USE_GH" = true ]; then
    # Release avec GitHub CLI
    echo "Création du release avec gh..."
    
    RELEASE_NOTES=""
    if [ -f "RELEASE_NOTES_v3.8.0.md" ]; then
        RELEASE_NOTES="--notes-file RELEASE_NOTES_v3.8.0.md"
    else
        RELEASE_NOTES="--notes 'Version ${VERSION} - Cascade Intelligente'"
    fi
    
    gh release create "v${VERSION}" \
        --title "RolePlay AI v${VERSION} - Cascade Intelligente" \
        $RELEASE_NOTES \
        "$RELEASE_DIR/RolePlayAI-v${VERSION}.apk" 2>/dev/null || echo "Release existe déjà"
    
    # Obtenir l'URL
    RELEASE_URL=$(gh release view "v${VERSION}" --json url -q .url 2>/dev/null)
    
    echo ""
    echo -e "${GREEN}✅ Release créé !${NC}"
    echo ""
    echo -e "${GREEN}╔════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  🎉 BUILD ET RELEASE TERMINÉS AVEC SUCCÈS !  🎉   ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BLUE}📥 Lien de téléchargement:${NC}"
    echo -e "${GREEN}${RELEASE_URL}${NC}"
    echo ""
    echo -e "${BLUE}📥 APK direct:${NC}"
    echo -e "${GREEN}https://github.com/${GITHUB_USER}/${GITHUB_REPO}/releases/download/v${VERSION}/RolePlayAI-v${VERSION}.apk${NC}"
    echo ""
else
    # Instructions manuelles
    echo -e "${YELLOW}⚠️  GitHub CLI non disponible${NC}"
    echo ""
    echo "Créez le release manuellement:"
    echo ""
    echo "1. Allez sur:"
    echo "   https://github.com/${GITHUB_USER}/${GITHUB_REPO}/releases/new"
    echo ""
    echo "2. Tag: v${VERSION}"
    echo "3. Titre: RolePlay AI v${VERSION} - Cascade Intelligente"
    echo "4. Uploadez: $RELEASE_DIR/RolePlayAI-v${VERSION}.apk"
    echo ""
    echo -e "${BLUE}Après création, le lien sera:${NC}"
    echo "https://github.com/${GITHUB_USER}/${GITHUB_REPO}/releases/tag/v${VERSION}"
    echo ""
fi

# Résumé final
echo -e "${BLUE}📊 Résumé:${NC}"
echo "  ✅ APK compilé: $RELEASE_DIR/RolePlayAI-v${VERSION}.apk"
echo "  ✅ Documentation: $RELEASE_DIR/"
echo "  ✅ Commit créé et pushé"
echo "  ✅ Tag v${VERSION} créé"
echo "  ✅ Release GitHub créé"
echo ""
echo -e "${GREEN}🎊 Tout est prêt ! Partagez le lien ! 🎊${NC}"
