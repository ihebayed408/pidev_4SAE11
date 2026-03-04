# Firebase Credentials (DO NOT COMMIT)

This folder is for **local** Firebase service account keys. The JSON files are gitignored.

## Setup

1. Go to [Firebase Console](https://console.firebase.google.com/) → Project Settings → Service accounts
2. Click **Generate new private key**
3. Save the JSON file here as `notificationsystem-3bd2f-firebase-adminsdk-*.json` (or any name)

## How the Notification service finds credentials

Set **one** of:

- **Environment variable** (recommended):
  ```
  GOOGLE_APPLICATION_CREDENTIALS=C:\path\to\your\firebase-credentials\notificationsystem-3bd2f-firebase-adminsdk-xxx.json
  ```

- **application-local.properties** (local dev only, not committed):
  ```properties
  notification.firebase.credentials-path=C:/path/to/your/firebase-key.json
  ```

Never commit the JSON file or add it to version control.
