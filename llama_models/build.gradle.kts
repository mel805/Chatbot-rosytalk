plugins {
    id("com.android.asset-pack")
}

assetPack {
    // Nom utilisé par Play Asset Delivery (et par l'app pour localiser les assets).
    packName = "llama_models"

    // Pour le Play Store, tu pourras changer en "fast-follow" ou "on-demand".
    dynamicDelivery {
        deliveryType = "install-time"
    }
}

