param(
    [string]$BaseUrl = 'http://localhost:8083',
    [string]$Username = '900001',
    [string]$Password = '123456',
    [int]$TotalRequests = 40,
    [int]$DelayMilliseconds = 100,
    [string]$RemoteIp = '127.0.0.1'
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

if ($TotalRequests -le 0) {
    throw 'TotalRequests 必须大于 0。'
}

$endpoint = "$BaseUrl/api/auth/login"
$headers = @{
    'Content-Type' = 'application/json'
    'X-Device-Fingerprint' = 'load-test-device'
    'X-Device-Info' = "UA=PowerShell LoadTest | RemoteIp=$RemoteIp"
    'X-Forwarded-For' = $RemoteIp
}

$durations = New-Object System.Collections.Generic.List[double]
$successCount = 0
$failureCount = 0
$messages = @{}

Write-Host "[load-test-login] 开始压测：$endpoint"
Write-Host "[load-test-login] 请求数：$TotalRequests，用户名：$Username"

for ($index = 1; $index -le $TotalRequests; $index++) {
    $payload = @{ username = $Username; password = $Password } | ConvertTo-Json
    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-RestMethod -Method Post -Uri $endpoint -Headers $headers -Body $payload -TimeoutSec 15
        if ($response.code -eq 0) {
            $successCount++
        }
        else {
            $failureCount++
            $message = $response.message
            if (-not $messages.ContainsKey($message)) { $messages[$message] = 0 }
            $messages[$message]++
        }
    }
    catch {
        $failureCount++
        $message = $_.Exception.Message
        if (-not $messages.ContainsKey($message)) { $messages[$message] = 0 }
        $messages[$message]++
    }
    finally {
        $watch.Stop()
        $durations.Add($watch.Elapsed.TotalMilliseconds)
    }

    if ($DelayMilliseconds -gt 0 -and $index -lt $TotalRequests) {
        Start-Sleep -Milliseconds $DelayMilliseconds
    }
}

$orderedDurations = $durations | Sort-Object
$avg = [Math]::Round((($durations | Measure-Object -Average).Average), 2)
$p95Index = [Math]::Max([Math]::Ceiling($orderedDurations.Count * 0.95) - 1, 0)
$p95 = [Math]::Round($orderedDurations[$p95Index], 2)
$max = [Math]::Round(($durations | Measure-Object -Maximum).Maximum, 2)

Write-Host "[load-test-login] 完成。"
Write-Host "  成功请求：$successCount"
Write-Host "  失败请求：$failureCount"
Write-Host "  平均耗时：$avg ms"
Write-Host "  P95 耗时：$p95 ms"
Write-Host "  最大耗时：$max ms"

if ($messages.Count -gt 0) {
    Write-Host "[load-test-login] 失败分布："
    foreach ($item in $messages.GetEnumerator() | Sort-Object Name) {
        Write-Host "  $($item.Name) -> $($item.Value)"
    }
}
