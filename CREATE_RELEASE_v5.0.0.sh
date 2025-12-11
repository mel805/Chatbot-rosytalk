#!/bin/bash

echo "🚀 Création du Release GitHub v5.0.0"
echo "===================================="
echo ""

# Vérifier que gh est installé
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI (gh) n'est pas installé"
    echo "   Installez-le: https://cli.github.com/"
    exit 1
fi

# Vérifier authentification
echo "🔐 Vérification authentification GitHub..."
if ! gh auth status &> /dev/null; then
    echo "❌ Non authentifié. Lancez: gh auth login"
    exit 1
fi

echo "✅ Authentifié"
echo ""

# Récupérer le dépôt
REPO="mel805/Chatbot-rosytalk"
TAG="v5.0.0"
APK_PATH="release-v5.0.0/RolePlayAI-v5.0.0.apk"

# Vérifier que l'APK existe
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK non trouvé: $APK_PATH"
    exit 1
fi

echo "📦 APK trouvé: $APK_PATH ($(du -h "$APK_PATH" | cut -f1))"
echo ""

# Vérifier si le tag existe
echo "🏷️  Vérification du tag v5.0.0..."
if ! git rev-parse "$TAG" >/dev/null 2>&1; then
    echo "❌ Tag $TAG n'existe pas localement"
    echo "   Le créer avec: git tag -a $TAG -m 'Release v5.0.0'"
    exit 1
fi

echo "✅ Tag trouvé"
echo ""

# Vérifier si le tag est pushé
echo "☁️  Vérification que le tag est pushé..."
if ! git ls-remote --tags origin | grep -q "refs/tags/$TAG"; then
    echo "⚠️  Tag pas encore pushé sur GitHub"
    echo "   Push en cours..."
    git push origin "$TAG"
fi

echo "✅ Tag pushé"
echo ""

# Créer le release
echo "🎉 Création du release GitHub..."
echo ""

gh release create "$TAG" \
  --repo "$REPO" \
  --title "v5.0.0 - Mémoire Long Terme & Cohérence Maximale" \
  --notes-file "RELEASE_NOTES_v5.0.0.md" \
  "$APK_PATH"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Release créé avec succès !"
    echo ""
    echo "📍 URL du release:"
    gh release view "$TAG" --repo "$REPO" --json url --jq .url
    echo ""
    echo "📥 URL de téléchargement APK:"
    gh release view "$TAG" --repo "$REPO" --json assets --jq '.assets[0].url'
    echo ""
    echo "🎊 Release v5.0.0 publié avec succès !"
else
    echo ""
    echo "❌ Erreur lors de la création du release"
    exit 1
fi
