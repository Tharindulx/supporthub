param(
  [ValidateSet('run','build','test','frontend','install')][string]$Action='run',
  [string]$JavaTools='D:\DeveloperTools\GamingPlatform',
  [string]$ToolsRoot='D:\DeveloperTools\SupportHub'
)
$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot -Parent
$env:JAVA_HOME=Join-Path $JavaTools 'jdk'
$maven=Join-Path $JavaTools 'apache-maven-3.9.16\bin\mvn.cmd'
if (!(Test-Path -LiteralPath $maven)) { throw 'Java 21 and Maven are required. Set -JavaTools to the existing tools directory, or use standard mvn commands from the README.' }
$mavenArgs=@("-Dmaven.repo.local=$JavaTools/repository",'--batch-mode','--no-transfer-progress')
function Check-Exit { if($LASTEXITCODE -ne 0) { throw "Command failed with exit code $LASTEXITCODE" } }
function Install-Frontend {
  $dependencyRoot=Join-Path $ToolsRoot 'frontend'
  New-Item -ItemType Directory -Force $dependencyRoot | Out-Null
  Copy-Item -LiteralPath "$project\frontend\package.json" -Destination $dependencyRoot
  Copy-Item -LiteralPath "$project\frontend\package-lock.json" -Destination $dependencyRoot
  & npm.cmd ci --prefix $dependencyRoot --cache "$ToolsRoot\npm-cache" --no-fund
  Check-Exit
  $modules="$project\frontend\node_modules"
  if(!(Test-Path -LiteralPath $modules)){ New-Item -ItemType Junction -Path $modules -Target "$dependencyRoot\node_modules" | Out-Null }
}
function Build-Frontend {
  if(!(Test-Path -LiteralPath "$project\frontend\node_modules")){Install-Frontend}
  Push-Location "$project\frontend"
  try{ & npm.cmd run build; Check-Exit }finally{Pop-Location}
  $static=[IO.Path]::GetFullPath("$project\backend\src\main\resources\static")
  $expected=[IO.Path]::GetFullPath("$project\backend\src\main\resources")+[IO.Path]::DirectorySeparatorChar
  if(!$static.StartsWith($expected,[StringComparison]::OrdinalIgnoreCase)){throw 'Unsafe build destination'}
  if(Test-Path -LiteralPath $static){Remove-Item -LiteralPath $static -Recurse -Force}
  New-Item -ItemType Directory -Force $static | Out-Null
  Copy-Item -Path "$project\frontend\dist\supporthub\browser\*" -Destination $static -Recurse
}
Push-Location "$project\backend"
try {
  switch($Action){
    'install' {Install-Frontend}
    'frontend' {Build-Frontend}
    'test' {& $maven @mavenArgs verify; Check-Exit}
    'build' {Build-Frontend; & $maven @mavenArgs clean verify; Check-Exit}
    'run' {
      $jar="$project\backend\target\supporthub-1.0.0.jar"
      if(!(Test-Path -LiteralPath $jar)){Build-Frontend; & $maven @mavenArgs clean verify; Check-Exit}
      Write-Host 'SupportHub: http://localhost:8082 (Ctrl+C to stop)'
      & "$env:JAVA_HOME\bin\java.exe" -jar $jar --spring.profiles.active=demo
      Check-Exit
    }
  }
} finally {Pop-Location}
