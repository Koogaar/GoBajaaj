Add-Type -AssemblyName System.Drawing
$srcPath = "E:\go\GoBajaaj_DriverPass_Fixed_Source\SHIRI_BRAND_ASSETS\shiri_app_icon_playstore.png"
$src = [System.Drawing.Image]::FromFile($srcPath)

$sizes = [ordered]@{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

foreach ($folder in $sizes.Keys) {
    $size = $sizes[$folder]
    $destFolder = "E:\go\GoBajaaj_DriverPass_Fixed_Source\app\src\main\res\$folder"
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.Clear([System.Drawing.Color]::White)
    $g.DrawImage($src, 0, 0, $size, $size)
    $g.Dispose()
    
    $p1 = Join-Path $destFolder "ic_launcher.png"
    $p2 = Join-Path $destFolder "ic_launcher_round.png"
    if (Test-Path $p1) { Remove-Item $p1 -Force }
    if (Test-Path $p2) { Remove-Item $p2 -Force }
    $bmp.Save($p1, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Save($p2, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output "Saved $size x $size to $destFolder"
}
$src.Dispose()
Write-Output "Done updating all mipmap icons!"

