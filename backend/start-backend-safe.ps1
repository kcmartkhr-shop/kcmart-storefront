param(
    [switch]$ConfirmStart
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not $ConfirmStart) {
    Write-Host "SAFE MODE: Preview only. Backend not started."
    Write-Host "Flyway will be disabled."
    Write-Host "Email sending will be disabled."
    exit 0
}

$listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue

if ($listener) {
    throw "Port 8080 is occupied. Existing backend will not be stopped."
}

$envFile = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path -LiteralPath $envFile)) {
    throw ".env file not found."
}

foreach ($line in Get-Content -LiteralPath $envFile) {
    if ($line -match '^\s*(#|$)') {
        continue
    }

    if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        $name = $Matches[1]
        $value = $Matches[2].Trim().Trim('"', "'")

        [Environment]::SetEnvironmentVariable(
            $name,
            $value,
            "Process"
        )
    }
}

foreach ($name in @(
    "KCMART_DB_PASSWORD",
    "KCMART_RAZORPAY_KEY_ID",
    "KCMART_RAZORPAY_KEY_SECRET",
    "KCMART_RAZORPAY_WEBHOOK_SECRET"
)) {
    $value = [Environment]::GetEnvironmentVariable($name, "Process")

    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Required environment variable missing: $name"
    }
}

$env:SPRING_FLYWAY_ENABLED = "false"
$env:KCMART_MAIL_ENABLED = "false"

Write-Host "Environment loaded successfully."
Write-Host "Flyway disabled."
Write-Host "SMTP disabled."
Write-Host "No credentials displayed."

throw "START BLOCKED: Launch command has not yet been verified. No backend started."
