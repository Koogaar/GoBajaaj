$src = "e:\go\GoBajaaj_DriverPass_Fixed_Source\app\build\outputs\apk\release\app-release.apk"
$aab = "e:\go\GoBajaaj_DriverPass_Fixed_Source\app\build\outputs\bundle\release\app-release.aab"
$dst = "e:\go\GoBajaaj_DriverPass_Fixed_Source\GoBajaaj_APK"
$ver = "4.0.1"
if (Test-Path $src) {
    Copy-Item $src "$dst\SHIRI_v${ver}_release.apk" -Force
    Write-Host "APK => SHIRI_v${ver}_release.apk"
}
if (Test-Path $aab) {
    Copy-Item $aab "$dst\SHIRI_v${ver}_release.aab" -Force
    Write-Host "AAB => SHIRI_v${ver}_release.aab"
}
Get-ChildItem $dst -Filter "SHIRI_v${ver}*" | Format-Table Name, LastWriteTime, @{N='MB';E={[math]::Round($_.Length/1MB,1)}}

