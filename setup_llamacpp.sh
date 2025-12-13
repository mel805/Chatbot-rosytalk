#!/bin/bash

# Script d'intégration automatique de llama.cpp
# Version: 1.0.0

set -e

echo "🚀 Intégration de llama.cpp pour Android..."

# Créer le répertoire cpp s'il n'existe pas
mkdir -p app/src/main/cpp

# Aller dans le répertoire cpp
cd app/src/main/cpp

# Télécharger llama.cpp si absent OU si le dossier est vide / incomplet (ex: sous-module cassé)
NEEDED_FILE="llama.cpp/include/llama.h"
if [ ! -f "$NEEDED_FILE" ]; then
    echo "📥 Téléchargement (ou réparation) de llama.cpp..."
    rm -rf llama.cpp
    git clone --depth 1 https://github.com/ggerganov/llama.cpp.git llama.cpp
    echo "✅ llama.cpp prêt"
else
    echo "✅ llama.cpp déjà présent (OK)"
fi

# Retour à la racine
cd ../../..

echo "✅ llama.cpp intégré avec succès !"
echo ""
echo "📝 Prochaines étapes:"
echo "1. Activer NDK dans build.gradle.kts"
echo "2. Compiler le projet"
echo "3. Tester avec un modèle GGUF"
