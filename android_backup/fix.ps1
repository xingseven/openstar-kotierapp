
$files = Get-ChildItem -Path "d:\code\code\qt_easytier_mobile\android_app\app\src\main\java\com\easytier\ui\pages" -Filter "*.kt"
foreach ($file in $files) {
    if ($file.Name -match "HomePage|NetworkConfigPage|OneClickPage|ServersPage|SettingsPage|LogPage") {
        $content = Get-Content $file.FullName -Raw -Encoding UTF8
        $content = $content -replace "TopAppBar\(modifier = Modifier\.height\(52\.dp\),", "TopAppBar("
        Set-Content $file.FullName $content -Encoding UTF8 -NoNewline
    }
}

