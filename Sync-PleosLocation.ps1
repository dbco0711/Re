param(
    [ValidateRange(5, 300)]
    [int]$IntervalSeconds = 15
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$null = [Windows.Devices.Geolocation.Geolocator, Windows, ContentType=WindowsRuntime]
$positionType = [Windows.Devices.Geolocation.Geoposition, Windows, ContentType=WindowsRuntime]
$asTask = [System.WindowsRuntimeSystemExtensions].GetMethods() |
    Where-Object {
        $_.Name -eq 'AsTask' -and $_.IsGenericMethodDefinition -and
        $_.GetParameters().Length -eq 1 -and
        $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
    } | Select-Object -First 1
$awaitPosition = $asTask.MakeGenericMethod($positionType)
$locator = New-Object Windows.Devices.Geolocation.Geolocator
$adb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
$culture = [Globalization.CultureInfo]::InvariantCulture

$devices = @(& $adb devices | Where-Object { $_ -match '^emulator-\d+\s+device$' })
if ($devices.Count -ne 1) {
    throw 'Connect one Android emulator before starting location sync.'
}
$serial = ($devices[0] -split '\s+')[0]
$lastFix = $null

while ((& $adb -s $serial get-state 2>$null) -eq 'device') {
    try {
        $request = $locator.GetGeopositionAsync(
            [TimeSpan]::FromSeconds(10), [TimeSpan]::FromSeconds(15)
        )
        $task = $awaitPosition.Invoke($null, @($request))
        $coordinate = $task.GetAwaiter().GetResult().Coordinate
        $longitude = $coordinate.Point.Position.Longitude.ToString($culture)
        $latitude = $coordinate.Point.Position.Latitude.ToString($culture)
        $fix = "$longitude $latitude"
        if ($fix -ne $lastFix) {
            $result = & $adb -s $serial emu geo fix $longitude $latitude
            if ($LASTEXITCODE -ne 0 -or $result -notcontains 'OK') {
                throw 'The emulator rejected the location update.'
            }
            $lastFix = $fix
            Write-Output "Updated $serial (Windows accuracy: $($coordinate.Accuracy) m)."
        }
    } catch {
        Write-Warning "Location update failed: $($_.Exception.Message)"
    }
    Start-Sleep -Seconds $IntervalSeconds
}
