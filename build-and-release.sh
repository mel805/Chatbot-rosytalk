#!/bin/bash
# Script de build et release automatique pour RolePlay AI v1.4.0

set -e  # Arrêter en cas d'erreur

echo "🚀 RolePlay AI - Build et Release v1.4.0"
echo "========================================"
echo ""

# Couleurs pour les messages
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Variables
VERSION="1.4.0"
VERSION_CODE="4"
TAG="v${VERSION}"
APK_NAME="RolePlayAI-v${VERSION}.apk"

# Fonction pour afficher un message de succès
success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Fonction pour afficher un avertissement
warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Fonction pour afficher une erreur
error() {
    echo -e "${RED}❌ $1${NC}"
    exit 1
}

# Étape 1 : Vérifications préliminaires
echo "📋 Étape 1/6 : Vérifications préliminaires"
echo "-------------------------------------------"

# Vérifier que nous sommes dans le bon dossier
if [ ! -f "build.gradle.kts" ]; then
    error "Ce script doit être exécuté depuis la racine du projet"
fi

# Vérifier Java
if ! command -v java &> /dev/null; then
    error "Java n'est pas installé"
fi
success "Java installé"

# Vérifier que le wrapper Gradle existe
if [ ! -f "./gradlew" ]; then
    error "gradlew non trouvé"
fi
success "Gradle wrapper trouvé"

# Vérifier ANDROID_HOME ou local.properties
if [ -z "$ANDROID_HOME" ] && [ ! -f "local.properties" ]; then
    warning "ANDROID_HOME non défini et local.properties manquant"
    echo "Créez local.properties avec : sdk.dir=/chemin/vers/android/sdk"
    read -p "Voulez-vous continuer quand même ? (o/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Oo]$ ]]; then
        exit 1
    fi
else
    success "SDK Android configuré"
fi

echo ""

# Étape 2 : Nettoyage
echo "🧹 Étape 2/6 : Nettoyage du build précédent"
echo "-------------------------------------------"
./gradlew clean
success "Build précédent nettoyé"
echo ""

# Étape 3 : Build Debug pour tests rapides
echo "🔨 Étape 3/6 : Compilation Debug (pour test)"
echo "-------------------------------------------"
./gradlew assembleDebug

if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    success "APK Debug compilé : app/build/outputs/apk/debug/app-debug.apk"
    
    # Proposer d'installer pour test
    if command -v adb &> /dev/null; then
        echo ""
        read -p "Voulez-vous installer l'APK Debug sur un appareil connecté ? (o/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Oo]$ ]]; then
            adb install -r app/build/outputs/apk/debug/app-debug.apk
            success "APK Debug installé"
            echo ""
            read -p "Testez l'application et appuyez sur Entrée pour continuer vers la Release..."
        fi
    fi
else
    error "Échec de la compilation Debug"
fi
echo ""

# Étape 4 : Build Release
echo "🏗️  Étape 4/6 : Compilation Release"
echo "-------------------------------------------"

# Vérifier si keystore existe
if [ ! -f "keystore.properties" ]; then
    warning "keystore.properties non trouvé"
    echo "La release sera non signée (pour tests seulement)"
    echo ""
fi

./gradlew assembleRelease

if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    success "APK Release compilé"
    
    # Copier et renommer l'APK
    cp app/build/outputs/apk/release/app-release.apk "./${APK_NAME}"
    success "APK copié vers : ./${APK_NAME}"
    
    # Afficher la taille
    SIZE=$(du -h "./${APK_NAME}" | cut -f1)
    echo "📦 Taille de l'APK : ${SIZE}"
else
    error "Échec de la compilation Release"
fi
echo ""

# Étape 5 : Git commit et tag
echo "📝 Étape 5/6 : Git commit et tag"
echo "-------------------------------------------"

# Vérifier s'il y a des modifications
if [ -z "$(git status --porcelain)" ]; then
    warning "Aucune modification Git détectée"
else
    echo "Modifications détectées :"
    git status --short
    echo ""
    
    read -p "Voulez-vous commiter ces changements ? (o/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Oo]$ ]]; then
        git add .
        git commit -m "feat: v${VERSION} - mémoire conversations, profil utilisateur et personnalisation

- Ajout système de persistence des conversations avec DataStore
- Nouvelle page de profil utilisateur avec pseudo, bio, âge
- Les personnages utilisent le pseudo de l'utilisateur dans les conversations
- Amélioration du système d'inscription
- Mise à jour des moteurs AI (Groq et Local) pour intégration du pseudo"
        
        success "Commit créé"
    fi
fi

# Créer le tag
if git rev-parse "${TAG}" >/dev/null 2>&1; then
    warning "Le tag ${TAG} existe déjà"
else
    git tag -a "${TAG}" -m "Version ${VERSION} - Mémoire, Profil et Personnalisation"
    success "Tag ${TAG} créé"
fi

echo ""
read -p "Voulez-vous pousser vers GitHub ? (o/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Oo]$ ]]; then
    CURRENT_BRANCH=$(git branch --show-current)
    git push origin "${CURRENT_BRANCH}"
    git push origin "${TAG}"
    success "Code et tag poussés vers GitHub"
fi

echo ""

# Étape 6 : Créer la release GitHub
echo "🎉 Étape 6/6 : Création de la release GitHub"
echo "-------------------------------------------"

if command -v gh &> /dev/null; then
    echo "GitHub CLI détecté"
    echo ""
    read -p "Voulez-vous créer la release automatiquement ? (o/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Oo]$ ]]; then
        gh release create "${TAG}" \
            "./${APK_NAME}" \
            --title "Version ${VERSION} - Mémoire, Profil et Personnalisation" \
            --notes-file RELEASE_NOTES_v${VERSION}.md
        
        success "Release GitHub créée !"
        
        # Obtenir l'URL de la release
        REPO_URL=$(git config --get remote.origin.url | sed 's/\.git$//')
        RELEASE_URL="${REPO_URL}/releases/tag/${TAG}"
        
        echo ""
        echo "🎊 Release publiée avec succès !"
        echo "📎 URL de la release : ${RELEASE_URL}"
        echo "📥 Téléchargement direct : ${REPO_URL}/releases/download/${TAG}/${APK_NAME}"
    fi
else
    warning "GitHub CLI non installé"
    echo ""
    echo "Pour créer la release manuellement :"
    echo "1. Allez sur : https://github.com/VOTRE_USERNAME/VOTRE_REPO/releases/new"
    echo "2. Choisissez le tag : ${TAG}"
    echo "3. Titre : Version ${VERSION} - Mémoire, Profil et Personnalisation"
    echo "4. Copiez le contenu de RELEASE_NOTES_v${VERSION}.md"
    echo "5. Uploadez : ./${APK_NAME}"
    echo "6. Publiez la release"
fi

echo ""
echo "========================================"
echo "✨ Build et release terminés !"
echo "========================================"
echo ""
echo "📱 APK disponible : ./${APK_NAME}"
echo "📋 Notes de release : RELEASE_NOTES_v${VERSION}.md"
echo "📚 Guide complet : GUIDE_COMPILATION_RELEASE.md"
echo ""
echo "Merci d'utiliser RolePlay AI ! 🎭"
