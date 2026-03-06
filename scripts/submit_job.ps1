# Submit Flink Job (PowerShell)
# Usage: .\submit_job.ps1 [-JarFile "path"] [-MainClass "class"]

param(
    [string]$JarFile = "target\flink-streaming-1.0-SNAPSHOT.jar",
    [string]$MainClass = "com.eventengine.flink.EventAggregationJob",
    [string]$FlinkHost = "localhost",
    [int]$FlinkPort = 8081
)

$ErrorActionPreference = "Stop"

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Submitting Flink Job" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "JAR: $JarFile" -ForegroundColor Yellow
Write-Host "Main Class: $MainClass" -ForegroundColor Yellow
Write-Host "Flink: ${FlinkHost}:${FlinkPort}" -ForegroundColor Yellow
Write-Host "======================================" -ForegroundColor Cyan

# Check if JAR exists
if (-not (Test-Path $JarFile)) {
    Write-Host "Error: JAR file not found: $JarFile" -ForegroundColor Red
    Write-Host "Please build the project first: mvn clean package" -ForegroundColor Yellow
    exit 1
}

# Wait for Flink to be ready
Write-Host "`nWaiting for Flink to be ready..." -ForegroundColor Yellow
$ready = $false
for ($i = 1; $i -le 30; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "http://${FlinkHost}:${FlinkPort}" -UseBasicParsing -TimeoutSec 2
        if ($response.StatusCode -eq 200) {
            Write-Host "✓ Flink is ready" -ForegroundColor Green
            $ready = $true
            break
        }
    }
    catch {
        Write-Host "Waiting... ($i/30)" -ForegroundColor Gray
        Start-Sleep -Seconds 2
    }
}

if (-not $ready) {
    Write-Host "✗ Flink is not ready after 60 seconds" -ForegroundColor Red
    exit 1
}

# Upload JAR
Write-Host "`nUploading JAR..." -ForegroundColor Yellow

$boundary = [System.Guid]::NewGuid().ToString()
$jarBytes = [System.IO.File]::ReadAllBytes((Resolve-Path $JarFile))
$jarFileName = Split-Path $JarFile -Leaf

$bodyLines = @(
    "--$boundary",
    "Content-Disposition: form-data; name=`"jarfile`"; filename=`"$jarFileName`"",
    "Content-Type: application/java-archive",
    "",
    [System.Text.Encoding]::Latin1.GetString($jarBytes),
    "--$boundary--"
) -join "`r`n"

$uploadResponse = Invoke-RestMethod -Uri "http://${FlinkHost}:${FlinkPort}/jars/upload" `
    -Method Post `
    -ContentType "multipart/form-data; boundary=$boundary" `
    -Body $bodyLines

$jarId = $uploadResponse.filename

if (-not $jarId) {
    Write-Host "✗ Failed to upload JAR" -ForegroundColor Red
    Write-Host ($uploadResponse | ConvertTo-Json)
    exit 1
}

Write-Host "✓ JAR uploaded: $jarId" -ForegroundColor Green

# Run the job
Write-Host "`nRunning job..." -ForegroundColor Yellow

$runBody = @{
    entryClass = $MainClass
} | ConvertTo-Json

$runResponse = Invoke-RestMethod -Uri "http://${FlinkHost}:${FlinkPort}/jars/$jarId/run" `
    -Method Post `
    -ContentType "application/json" `
    -Body $runBody

$jobId = $runResponse.jobid

if (-not $jobId) {
    Write-Host "✗ Failed to run job" -ForegroundColor Red
    Write-Host ($runResponse | ConvertTo-Json)
    exit 1
}

Write-Host "✓ Job submitted successfully!" -ForegroundColor Green
Write-Host "Job ID: $jobId" -ForegroundColor Cyan
Write-Host "Dashboard: http://${FlinkHost}:${FlinkPort}/#/job/$jobId/overview" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
