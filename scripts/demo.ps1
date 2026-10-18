param([ValidateSet('start','stop')][string]$Action='start')
$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot -Parent
$jar=Join-Path $project 'backend\target\supporthub-1.0.0.jar'
$logDir=Join-Path $project 'logs'
$pidFile=Join-Path $logDir 'demo.pid'
if($Action -eq 'stop') {
  if(!(Test-Path -LiteralPath $pidFile)){Write-Host 'No managed demo is running.';return}
  $demoPid=[int](Get-Content -LiteralPath $pidFile -Raw)
  $process=Get-CimInstance Win32_Process -Filter "ProcessId=$demoPid"
  if($process -and $process.CommandLine.Contains($jar)){Stop-Process -Id $demoPid;Write-Host 'SupportHub stopped.'}
  elseif($process){throw 'PID belongs to another process; refusing to stop it.'}
  Remove-Item -LiteralPath $pidFile
  return
}
if(!(Test-Path -LiteralPath $jar)){throw 'Run scripts/dev.ps1 -Action build first.'}
if(Test-Path -LiteralPath $pidFile){
  $demoPid=[int](Get-Content -LiteralPath $pidFile -Raw)
  $existing=Get-CimInstance Win32_Process -Filter "ProcessId=$demoPid"
  if($existing -and $existing.CommandLine.Contains($jar)){Write-Host 'SupportHub is already running at http://localhost:8082';return}
}
New-Item -ItemType Directory -Force $logDir | Out-Null
$java='D:\DeveloperTools\GamingPlatform\jdk\bin\java.exe'
$process=Start-Process -FilePath $java -ArgumentList @('-jar',('"'+$jar+'"'),'--spring.profiles.active=demo','--server.port=8082') -WorkingDirectory "$project\backend" -WindowStyle Hidden -PassThru -RedirectStandardOutput "$logDir\demo.log" -RedirectStandardError "$logDir\demo-error.log"
Set-Content -LiteralPath $pidFile -Value $process.Id
for($attempt=0;$attempt -lt 30;$attempt++){
  if($process.HasExited){throw "Startup failed. Read $logDir\demo.log"}
  try{$response=Invoke-RestMethod 'http://127.0.0.1:8082/api/health' -TimeoutSec 1;if($response.status -eq 'UP'){Write-Host 'SupportHub is running at http://localhost:8082';return}}catch{}
  Start-Sleep -Seconds 1
}
throw "Startup is taking longer than expected. Read $logDir\demo.log"
