# 0. Set Java Version to 21 (Required for Spring Boot 3.4+)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Write-Host "☕ Using Java: $($env:JAVA_HOME)" -ForegroundColor Cyan
java -version

# 1. Build JAR (Skip tests for speed)
Write-Host "🚧 Building Backend JAR..." -ForegroundColor Yellow
./gradlew bootJar -x test

if ($LASTEXITCODE -ne 0) {
    Write-Error "Build Failed!"
    exit 1
}

# 2. Run Docker Compose with Local Override
Write-Host "🐳 Starting Docker Containers..." -ForegroundColor Cyan
docker compose -f docker-compose.yml -f docker-compose.local.yml up --build -d

Write-Host "✅ Server is running at http://localhost:8080" -ForegroundColor Green
Write-Host "📜 Logs: docker compose logs -f backend"
