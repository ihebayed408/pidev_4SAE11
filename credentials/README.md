# Credentials (DO NOT COMMIT)

This folder is for **local** API keys and tokens. All credential files are gitignored.

## Google Calendar (Planning microservice)

1. Create a **service account** in [Google Cloud Console](https://console.cloud.google.com/) → IAM & Admin → Service accounts → Create key (JSON).
2. Enable **Google Calendar API** in APIs & Services → Library.
3. Share your target Google Calendar with the service account email (e.g. `xxx@yyy.iam.gserviceaccount.com`).

**Configure one of:**

- **Environment variable:**
  ```
  GOOGLE_APPLICATION_CREDENTIALS=C:\path\to\credentials\calendar-service-account.json
  ```

- **application-local.properties** (Planning module, not committed):
  ```properties
  google.calendar.enabled=true
  google.calendar.credentials-path=C:/path/to/credentials/calendar-service-account.json
  ```

## GitHub token (Planning microservice)

**All token storage locations are gitignored – never committed.**

1. GitHub → Settings → Developer settings → Personal access tokens → Generate new token.
2. Scopes: `repo` (private repos) or `public_repo` (public only).

### Option B: Token file (recommended)

Put your token in `githubToken.txt` at the repo root (gitignored). The Planning service reads it automatically when you run from the planning module:

```powershell
cd backEnd\Microservices\planning
mvn spring-boot:run
```

The service looks for `../../../../githubToken.txt` relative to the planning module (repo root). No env var needed.

### Option B2: Environment variable

```powershell
$env:GITHUB_TOKEN = "ghp_your_token_here"
cd backEnd\Microservices\planning; mvn spring-boot:run
```

Or: `$env:GITHUB_TOKEN = Get-Content githubToken.txt`

### Option A: Setup script

From the repo root:
```powershell
.\scripts\setup-github-token.ps1
```
Paste your token when prompted. It is saved to `githubToken.txt` (gitignored). Then use the env var commands above.

### Option C: application-local.properties

1. Copy `backEnd/Microservices/planning/src/main/resources/application-local.properties.example`
   to `application-local.properties` in the same folder.
2. Edit and set: `github.token=ghp_your_token_here`
3. The file is gitignored.

Never commit tokens or credential JSON files.
