# Environment Check Script for SellerX Backend
# Bu script hangi environment'ta çalıştığınızı gösterir

Write-Host "SellerX Backend Environment Check" -ForegroundColor Green
Write-Host "=================================" -ForegroundColor Green

# Check current profile
$profileCheck = $env:SPRING_PROFILES_ACTIVE
if ($profileCheck) {
    Write-Host "Active Profile: $profileCheck" -ForegroundColor Cyan
} else {
    Write-Host "Active Profile: development (default)" -ForegroundColor Yellow
}

# Check JWT Secret
$jwtSecret = $env:JWT_SECRET
if ($jwtSecret) {
    $secretLength = $jwtSecret.Length
    Write-Host "JWT Secret: Set ($secretLength characters)" -ForegroundColor Green
} else {
    Write-Host "JWT Secret: Not set (using default)" -ForegroundColor Yellow
}

# Check Database URL
$dbUrl = $env:DATABASE_URL
if ($dbUrl) {
    Write-Host "Database URL: $dbUrl" -ForegroundColor Green
} else {
    Write-Host "Database URL: Using profile default" -ForegroundColor Yellow
}

# Check Port
$port = $env:PORT
if ($port) {
    Write-Host "Port: $port" -ForegroundColor Green
} else {
    Write-Host "Port: 8080 (default)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Cookie Security Settings:" -ForegroundColor Cyan
switch ($profileCheck) {
    "production" {
        Write-Host "- Secure: true (HTTPS required)" -ForegroundColor Green
        Write-Host "- HttpOnly: true" -ForegroundColor Green
    }
    "docker" {
        Write-Host "- Secure: false (HTTP OK)" -ForegroundColor Yellow
        Write-Host "- HttpOnly: true" -ForegroundColor Green
    }
    default {
        Write-Host "- Secure: false (HTTP OK)" -ForegroundColor Yellow  
        Write-Host "- HttpOnly: true" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "To change profile:" -ForegroundColor Cyan
Write-Host "Set-Item -Path env:SPRING_PROFILES_ACTIVE -Value 'production'" -ForegroundColor Gray
