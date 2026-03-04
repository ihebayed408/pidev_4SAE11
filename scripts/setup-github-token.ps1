# Setup GitHub token for Planning microservice (gitignored - never committed)
# Usage: .\scripts\setup-github-token.ps1
# Or:   .\scripts\setup-github-token.ps1 -Token "ghp_your_token"

param(
    [string]$Token
)

$tokenFile = Join-Path $PSScriptRoot ".." "githubToken.txt"
$tokenFile = [System.IO.Path]::GetFullPath($tokenFile)

if (-not $Token) {
    $Token = Read-Host "Paste your GitHub Personal Access Token" -AsSecureString
    $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($Token)
    $Token = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
    [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR)
}

if ([string]::IsNullOrWhiteSpace($Token)) {
    Write-Host "No token provided. Exiting." -ForegroundColor Yellow
    exit 1
}

$Token = $Token.Trim()
Set-Content -Path $tokenFile -Value $Token -NoNewline

Write-Host "Token saved to githubToken.txt (gitignored)" -ForegroundColor Green
Write-Host ""
Write-Host "To use it when running the Planning microservice:" -ForegroundColor Cyan
Write-Host "  `$env:GITHUB_TOKEN = Get-Content githubToken.txt" -ForegroundColor White
Write-Host "  cd backEnd/Microservices/planning; mvn spring-boot:run" -ForegroundColor White
Write-Host ""
Write-Host "Or add to application-local.properties (gitignored):" -ForegroundColor Cyan
Write-Host "  github.token=your_token_here" -ForegroundColor White
Write-Host "  (Copy from backEnd/Microservices/planning/src/main/resources/application-local.properties.example)" -ForegroundColor Gray
