param(
    [string] $JarDir = (Join-Path $PSScriptRoot "..\build\libs"),
    [string] $TokenFile = (Join-Path $env:APPDATA "SilentLanding\modrinth-token.sec"),
    [switch] $DryRun,
    [switch] $SubmitForReview
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Net.Http

$ApiBase = "https://api.modrinth.com/v2"
$ProjectId = "FqetKSGR"
$ProjectSlug = "bone-meal-sieve"
$UserAgent = "bonemeal-sieve-release-uploader/1.0"
$RepositoryUrl = "https://github.com/nekodon-dev/BoneMealSieve"
$JarFile = "bonemeal-sieve-mc26.1.2-1.0.0.jar"
$VersionNumber = "1.0.0"
$FabricApiVersionId = "E1mjhYMF"

$ProjectBody = @'
# Bone Meal Sieve

Bone Meal Sieve is a Fabric mod that converts normal bone meal into low-quality or high-quality bone meal.

## Features

- Process one bone meal per second with the sieve.
- Automate input and output with hoppers above and below the sieve.
- Configure the high-quality bone meal rate in `config/bonemeal_sieve.json`.
- Low-quality and high-quality bone meal have different effects on grass, nylium, moss, crops, dripleaf, flowers, vines, cactus, lily pads, and other plants.
- Low-Quality Bone Meal used on a grass block generates grass without flowers.
- High-Quality Bone Meal used on a grass block generates flowers without grass. In areas where flowers are naturally rare, you may need to use it several times.

For more details, see the [README](https://github.com/nekodon-dev/BoneMealSieve/blob/main/README.md).

---

# Bone Meal Sieve（日本語）

通常の骨粉を「質の悪い骨粉」または「上質な骨粉」に変換するFabric製MODです。

## 主な機能

- ふるいで骨粉を1個1秒で処理します。
- 上下のホッパーで投入と回収を自動化できます。
- `config/bonemeal_sieve.json` で上質な骨粉の排出率を設定できます。
- 草、ナイリウム、苔、作物、ドリップリーフ、花、ツタ、サボテン、スイレンなどに品質別の効果があります。
- 質の悪い骨粉は草ブロックに使うと、花は生成されず草だけが生成されます。
- 上質な骨粉を草ブロックに使うと、草は生成されず花だけが生成されます。もともと花の生成が少ないエリアでは何回か使用する必要があるかもしれません。

さらに詳細は[こちら](https://github.com/nekodon-dev/BoneMealSieve/blob/main/README.md)を参照してください。
'@

function Get-Token {
    if (-not [string]::IsNullOrWhiteSpace($env:MODRINTH_TOKEN)) {
        return $env:MODRINTH_TOKEN
    }

    if (Test-Path -LiteralPath $TokenFile) {
        $secure = Get-Content -LiteralPath $TokenFile | ConvertTo-SecureString
        $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try {
            return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
        }
    }

    $secure = Read-Host "Modrinth token" -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function New-ApiClient($token) {
    $client = [System.Net.Http.HttpClient]::new()
    $client.DefaultRequestHeaders.Add("Authorization", $token)
    $client.DefaultRequestHeaders.UserAgent.ParseAdd($UserAgent)
    return $client
}

function Get-Project($token) {
    try {
        return Invoke-RestMethod -Uri "$ApiBase/project/$ProjectId" -Headers @{
            "User-Agent" = $UserAgent
            "Authorization" = $token
        }
    } catch {
        if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 404) {
            return $null
        }
        throw
    }
}

function New-Project($token) {
    $client = New-ApiClient $token
    $multipart = $null
    $iconStream = $null
    try {
        $projectData = @{
            slug = $ProjectSlug
            title = "Bone Meal Sieve"
            description = "Sieve normal bone meal into poor or high-quality variants with different plant effects."
            body = $ProjectBody
            categories = @("technology", "utility")
            additional_categories = @()
            status = "draft"
            requested_status = "draft"
            issues_url = "$RepositoryUrl/issues"
            source_url = $RepositoryUrl
            wiki_url = $null
            discord_url = $null
            donation_urls = @()
            client_side = "required"
            server_side = "required"
            license_id = "MIT"
            license_url = "$RepositoryUrl/blob/main/LICENSE"
            project_type = "mod"
            initial_versions = @()
            is_draft = $true
            gallery_items = @()
        }

        $multipart = [System.Net.Http.MultipartFormDataContent]::new()
        $json = $projectData | ConvertTo-Json -Depth 20 -Compress
        $multipart.Add([System.Net.Http.StringContent]::new(
            $json, [System.Text.Encoding]::UTF8, "application/json"), "data")

        $iconPath = Join-Path $PSScriptRoot "..\src\main\resources\assets\bonemeal_sieve\icon.png"
        $iconStream = [System.IO.File]::OpenRead($iconPath)
        $iconContent = [System.Net.Http.StreamContent]::new($iconStream)
        $iconContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("image/png")
        $multipart.Add($iconContent, "icon", "icon.png")

        $response = $client.PostAsync("$ApiBase/project", $multipart).GetAwaiter().GetResult()
        $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            throw "Modrinth project creation failed: HTTP $([int]$response.StatusCode) $($response.ReasonPhrase)`n$body"
        }
        return $body | ConvertFrom-Json
    } finally {
        if ($multipart) { $multipart.Dispose() }
        if ($iconStream) { $iconStream.Dispose() }
        $client.Dispose()
    }
}

function New-VersionData($projectId) {
    return @{
        name = "Bone Meal Sieve 1.0.0"
        version_number = $VersionNumber
        changelog = "Initial release for Minecraft 26.1.2."
        dependencies = @(
            @{
                version_id = $FabricApiVersionId
                project_id = "P7dR8mSH"
                dependency_type = "required"
            }
        )
        game_versions = @("26.1.2")
        version_type = "release"
        loaders = @("fabric")
        featured = $true
        status = "listed"
        project_id = $projectId
        file_parts = @("file")
        primary_file = "file"
        environment = "client_and_server"
    }
}

function New-Version($token, $jarPath, $versionData) {
    $client = New-ApiClient $token
    $multipart = $null
    $fileStream = $null
    try {
        $multipart = [System.Net.Http.MultipartFormDataContent]::new()
        $json = $versionData | ConvertTo-Json -Depth 20 -Compress
        $multipart.Add([System.Net.Http.StringContent]::new(
            $json, [System.Text.Encoding]::UTF8, "application/json"), "data")

        $fileStream = [System.IO.File]::OpenRead($jarPath)
        $fileContent = [System.Net.Http.StreamContent]::new($fileStream)
        $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("application/java-archive")
        $multipart.Add($fileContent, "file", [System.IO.Path]::GetFileName($jarPath))

        $response = $client.PostAsync("$ApiBase/version", $multipart).GetAwaiter().GetResult()
        $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            throw "Modrinth upload failed: HTTP $([int]$response.StatusCode) $($response.ReasonPhrase)`n$body"
        }
        return $body | ConvertFrom-Json
    } finally {
        if ($multipart) { $multipart.Dispose() }
        if ($fileStream) { $fileStream.Dispose() }
        $client.Dispose()
    }
}

function Submit-Project($token, $projectId) {
    $client = New-ApiClient $token
    try {
        $json = @{ requested_status = "approved" } | ConvertTo-Json -Compress
        $content = [System.Net.Http.StringContent]::new(
            $json, [System.Text.Encoding]::UTF8, "application/json")
        $response = $client.PatchAsync("$ApiBase/project/$projectId", $content).GetAwaiter().GetResult()
        $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            throw "Modrinth review submission failed: HTTP $([int]$response.StatusCode) $($response.ReasonPhrase)`n$body"
        }
    } finally {
        $client.Dispose()
    }
}

$jarPath = Join-Path $JarDir $JarFile
if (-not (Test-Path -LiteralPath $jarPath)) {
    throw "JAR not found: $jarPath"
}

if ($DryRun) {
    Write-Host "Dry run: would create '$ProjectSlug' when absent and upload $JarFile as $VersionNumber"
    New-VersionData "PROJECT_ID" | ConvertTo-Json -Depth 20
    exit
}

$token = Get-Token
$project = Get-Project $token
if (-not $project) {
    $project = New-Project $token
    Write-Host "Created project: $($project.id)"
}

$existing = Invoke-RestMethod `
    -Uri "$ApiBase/project/$($project.id)/version" `
    -Headers @{
        "User-Agent" = $UserAgent
        "Authorization" = $token
    }

if (@($existing.version_number) -notcontains $VersionNumber) {
    $created = New-Version $token $jarPath (New-VersionData $project.id)
    Write-Host "Uploaded ${VersionNumber}: $($created.id)"
} else {
    Write-Host "Skip existing version: $VersionNumber"
}

if ($SubmitForReview -and ($project.status -eq "draft" -or $project.status -eq "private")) {
    Submit-Project $token $project.id
    Write-Host "Submitted project for review."
} elseif ($project.status -eq "draft" -or $project.status -eq "private") {
    Write-Host "Project remains a draft. Use -SubmitForReview only after final review."
}


