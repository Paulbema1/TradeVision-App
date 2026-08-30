# ⚠️ Configuration Firebase requise avant build de production

Le fichier `app/google-services.json` présent dans ce dépôt est un **placeholder**
(valeurs factices) uniquement destiné à permettre au projet de compiler sans
erreur Gradle. **Les notifications push FCM ne fonctionneront pas** tant qu'il
n'est pas remplacé par le vrai fichier.

## Comment obtenir le vrai fichier

1. Allez sur [Firebase Console](https://console.firebase.google.com/)
2. Sélectionnez (ou créez) le projet Firebase utilisé par le **backend**
   (celui dont `FIREBASE_PROJECT_ID` est configuré côté serveur — ils doivent
   appartenir au même projet, voir `FIREBASE_SETUP.md` du backend).
3. Paramètres du projet → Vos applications → Application Android
   (`com.tradevision.ai`)
4. Téléchargez `google-services.json`
5. Remplacez `app/google-services.json` dans ce dépôt par le fichier téléchargé
6. **Ne committez jamais** ce fichier réel dans un dépôt public — préférez un
   dépôt privé, ou injectez-le via votre pipeline CI/CD (secret).

## Vérification rapide

Après remplacement, `./gradlew assembleDebug` doit continuer à réussir, et
un vrai token FCM doit être visible dans les logs (`FCMService` / `onNewToken`)
au premier lancement de l'app.
