Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead("c:\Users\palla\Desktop\GeminiApiComposeStarter\Assignment-1.docx")
$entry = $zip.Entries | Where-Object { $_.FullName -eq 'word/document.xml' }
$stream = $entry.Open()
$reader = New-Object System.IO.StreamReader($stream)
$xmlStr = $reader.ReadToEnd()
$reader.Close()
$zip.Dispose()
$xml = [xml]$xmlStr
Write-Output $xml.DocumentElement.InnerText
