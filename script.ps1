
$projectPath = "d:\code\code\qt_easytier_mobile\android_app\app\src\main\java\com\easytier\ui"
$files = Get-ChildItem -Path $projectPath -Recurse -Filter "*.kt"

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $original = $content
    
    $content = $content -replace "padding\(16\.dp\)", "padding(12.dp)"
    $content = $content -replace "padding\(horizontal = 16\.dp\)", "padding(horizontal = 12.dp)"
    $content = $content -replace "padding\(vertical = 16\.dp\)", "padding(vertical = 12.dp)"
    $content = $content -replace "Arrangement\.spacedBy\(16\.dp\)", "Arrangement.spacedBy(10.dp)"
    $content = $content -replace "Arrangement\.spacedBy\(8\.dp\)", "Arrangement.spacedBy(6.dp)"
    $content = $content -replace "Spacer\(Modifier\.height\(12\.dp\)\)", "Spacer(Modifier.height(8.dp))"
    $content = $content -replace "Spacer\(Modifier\.height\(10\.dp\)\)", "Spacer(Modifier.height(6.dp))"
    $content = $content -replace "Spacer\(Modifier\.width\(8\.dp\)\)", "Spacer(Modifier.width(6.dp))"

    $content = $content -replace "fontSize = 14\.sp", "fontSize = 13.sp"
    $content = $content -replace "fontSize = 16\.sp", "fontSize = 14.sp"

    $content = $content -replace "TopAppBar\(", "TopAppBar(modifier = Modifier.height(52.dp)," 

    if ($content -cne $original) {
        if ($content -match "Modifier\.height" -and $content -notmatch "import androidx\.compose\.foundation\.layout\.height") {
            $content = $content -replace "import androidx\.compose\.ui\.Modifier", "import androidx.compose.ui.Modifier`r`nimport androidx.compose.foundation.layout.height"
        }
        Set-Content $file.FullName $content -Encoding UTF8 -NoNewline
        Write-Host "Updated $($file.Name)"
    }
}

