<#  convert-to-usermanagement.ps1 — PowerShell script to convert pieq-api-template to custom service template
    Run from the pieq-api-template directory; converts the project in-place.
    Requires: PowerShell 5+ (7+ recommended), git (optional), JDK 21+.
#>

param(
  [Parameter(Mandatory=$false)][string]$ServiceName,
  [Parameter(Mandatory=$false)][string]$PackageSegment,
  [Parameter(Mandatory=$false)][string]$ClassPrefix
)

# ---------- logging functions ----------
function ts { (Get-Date).ToString("yyyy-MM-dd HH:mm:ss") }
function Log($m){ Write-Host "[$(ts)] $m" }
function Ok($m){ Write-Host "[$(ts)] $m" }
function Warn($m){ Write-Warning $m }
function Err($m){ Write-Error "[$(ts)] $m"; exit 1 }

# ---------- template constants (source template) ----------
$script:CurSubApi     = "pieq-api"
$script:CurSubApp     = "pieq-application"
$script:CurSubClient  = "pieq-client"
$script:CurBasePkg    = "com.pieq"
$script:CurAppClass   = "PieqApiApplication"
$script:CurCfgClass   = "PieqApiConfiguration"
$script:CurModelClass = "User"
$script:CurResClass   = "UserResource"
$script:CurSrvClass   = "UserService"
$script:CurDaoClass   = "UserDao"
$script:CurCliClass   = "UserClient"

# ---------- global variables ----------
$script:NewApi        = ""
$script:NewApp        = ""
$script:NewClient     = ""
$script:NewGroup      = ""
$script:TempFolder    = ""

# ---------- input validation and setup functions ----------
function Get-UserInput {
    if (-not $ServiceName)    { $script:ServiceName    = Read-Host "Service name (e.g. user-management)" }
    if (-not $PackageSegment) { $script:PackageSegment = Read-Host "Package segment after com.pieq. (e.g. usermanagement)" }
    if (-not $ClassPrefix)    { $script:ClassPrefix    = Read-Host "Class prefix (e.g. UserManagement)" }
}

function Initialize-TargetConstants {
    $script:NewApi        = "$ServiceName-api"
    $script:NewApp        = "$ServiceName-application"
    $script:NewClient     = "$ServiceName-client"
    $script:NewGroup      = "com.pieq.$PackageSegment"
    $script:TempFolder    = ".pieq-api-template"
    
    Log "Starting conversion from pieq-api-template to $ServiceName template"
    Log "Service name    : $ServiceName"
    Log "Package segment : $PackageSegment"
    Log "Class prefix    : $ClassPrefix"
    Log "New modules     : $NewApi, $NewApp, $NewClient"
    Log "New group       : $NewGroup"
}

function Test-TemplateStructure {
    foreach($m in @($CurSubApi,$CurSubApp,$CurSubClient)){
        if (-not (Test-Path (Join-Path (Get-Location) $m))) { 
            Err "Not in pieq-api-template root. Missing '$m' folder. Please run this script from the pieq-api-template directory." 
        }
    }
}

function Initialize-TempFolder {
    if (Test-Path $TempFolder) {
        $confirm = Read-Host "The folder '$TempFolder' already exists. Do you want to delete and recreate it? (y/n)"
        if ($confirm -eq 'y' -or $confirm -eq 'Y') {
            Remove-Item -Path $TempFolder -Recurse -Force
            New-Item -ItemType Directory -Path $TempFolder | Out-Null
        } else {
            Err "Aborted by user. Please remove or rename the folder '$TempFolder' and try again."
        }
    } else {
        New-Item -ItemType Directory -Path $TempFolder | Out-Null
    }
}

function Copy-TemplateFiles {
    Get-ChildItem -Path . -Exclude @($TempFolder, "bootstrap.ps1") | ForEach-Object {
        $destination = Join-Path $TempFolder $_.Name
        if ($_.PSIsContainer) {
            Move-Item -Path $_.FullName -Destination $destination -Force
        } else {
            Move-Item -Path $_.FullName -Destination $destination -Force
        }
    }
}

# ---------- file and folder manipulation functions ----------
function Update-PackageReferences {
    param(
        [string]$FilePath,
        [string]$OldPackage,
        [string]$NewPackage
    )
    
    try {
        (Get-Content $FilePath) | 
            ForEach-Object { $_ -replace [regex]::Escape($OldPackage), $NewPackage } |
            Set-Content $FilePath
        Log "Updated package references in $FilePath"
    } catch {
        Warn "Failed to update package references in $FilePath : $_"
    }
}

function Update-ClassNames {
    param(
        [string]$FilePath,
        [string]$OldPrefix,
        [string]$NewPrefix
    )
    
    try {
        $content = Get-Content $FilePath -Raw
        $content = $content -replace "class\s+$OldPrefix", "class $NewPrefix"
        $content = $content -replace [regex]::Escape("com.pieq.application"), "com.pieq.$PackageSegment"
        $content = $content -replace [regex]::Escape("com.pieq.model"), "com.pieq.$PackageSegment"
        $content = $content -replace [regex]::Escape("com.pieq.client"), "com.pieq.$PackageSegment"
        Set-Content $FilePath $content
        Log "Updated class names in $FilePath"
    } catch {
        Warn "Failed to update class names in $FilePath : $_"
    }
}

function Rename-KotlinFile {
    param(
        [string]$FilePath,
        [string]$OldPrefix,
        [string]$NewPrefix
    )
    
    $fileName = [IO.Path]::GetFileName($FilePath)
    if ($fileName -match "^$OldPrefix(.+)") {
        $newFileName = $fileName -replace "^$OldPrefix", $NewPrefix
        $newFilePath = Join-Path ([IO.Path]::GetDirectoryName($FilePath)) $newFileName
        try {
            Rename-Item -Path $FilePath -NewName $newFileName -Force
            Log "Renamed file $fileName to $newFileName"
            return $newFilePath
        } catch {
            Warn "Failed to rename file $fileName : $_"
            return $FilePath
        }
    }
    return $FilePath
}

function Update-GradleFile {
    param(
        [string]$GradleFilePath
    )
    
    try {
        $content = Get-Content $GradleFilePath -Raw
        
        # Replace main class reference
        $oldMainClass = "com.pieq.application.PieqApiApplicationKt"
        $newMainClass = "com.pieq.$PackageSegment.${ClassPrefix}ApiApplicationKt"
        $content = $content -replace [regex]::Escape($oldMainClass), $newMainClass
        
        # Replace package references
        $content = $content -replace "com\.pieq\.application", "com.pieq.$PackageSegment"
        $content = $content -replace "com\.pieq\.client", "com.pieq.$PackageSegment"
        
        
        # # Replace class name prefixes
         $content = $content -replace ":pieq-api", ":$PackageSegment-api"
         $content = $content -replace ":pieq-application", ":$PackageSegment-application"
        
        Set-Content $GradleFilePath $content
        Log "Updated build.gradle.kts in $GradleFilePath"
    } catch {
        Warn "Failed to update build.gradle.kts in $GradleFilePath : $_"
    }
}

function Update-SettingsGradle {
    param(
        [string]$SettingsGradlePath
    )
    
    try {
        $content = Get-Content $SettingsGradlePath -Raw
        # Replace project name from pieq-api-template to pieq-api-$PackageSegment
        $oldProjectName = "pieq-api-template"
        $newProjectName = "pieq-api-$PackageSegment"
        $content = $content -replace [regex]::Escape($oldProjectName), $newProjectName
                
        # Replace module references from pieq- to $PackageSegment-
        $content = $content -replace '"pieq-api"', "`"$PackageSegment-api`""
        $content = $content -replace '"pieq-application"', "`"$PackageSegment-application`""
        $content = $content -replace '"pieq-client"', "`"$PackageSegment-client`""
        Set-Content $SettingsGradlePath $content
        Log "Updated settings.gradle.kts with new project name and module references"
    } catch {
        Warn "Failed to update settings.gradle.kts in $SettingsGradlePath : $_"
    }
}

function Update-YamlFiles {
    param(
        [string]$BasePath
    )
    
    try {
        Log "Updating YAML files (module names, packages, name)"
        
        # Find all YAML files in the base path
        $yamlFiles = Get-ChildItem -Path $BasePath -Recurse -File -Include "*.yml","*.yaml" -ErrorAction SilentlyContinue
        
        foreach ($yamlFile in $yamlFiles) {
            try {
                $content = Get-Content $yamlFile.FullName -Raw
                
                # Replace module names
                $content = $content -replace "\b$CurSubApi\b", $NewApi
                $content = $content -replace "\b$CurSubApp\b", $NewApp
                $content = $content -replace "\b$CurSubClient\b", $NewClient
                
                # Replace package references (more sophisticated regex to avoid conflicts)
                $content = [Regex]::Replace($content, '\bcom\.pieq\b(?!\.(?:' + [Regex]::Escape($PackageSegment) + '|core))', $NewGroup)
                $content = [Regex]::Replace($content, 'com\.pieq\.(?!' + [Regex]::Escape($PackageSegment) + '\.|core\.)', "$NewGroup.")
                
                # Replace service name in YAML name fields
                $content = [Regex]::Replace($content, '^( *name *: *).*$', '${1}"' + $ServiceName + '"', 'Multiline')
                
                Set-Content $yamlFile.FullName $content
                Log "Updated YAML file: $($yamlFile.Name)"
            } catch {
                Warn "Failed to update YAML file $($yamlFile.FullName) : $_"
            }
        }
        
        Ok "YAML files updated"
    } catch {
        Warn "Failed to update YAML files in $BasePath : $_"
    }
}

function Update-DockerFiles {
    param(
        [string]$BasePath
    )
    
    try {
        Log "Updating Dockerfile(s) for module renames and jar paths"
        
        $dockerFiles = @("Dockerfile", "docker.local")
        
        foreach ($dockerFile in $dockerFiles) {
            $dockerPath = Join-Path $BasePath $dockerFile
            
            if (Test-Path $dockerPath) {
                try {
                    $content = Get-Content $dockerPath -Raw
                    
                    # Replace module names
                    $content = $content -replace "\b$CurSubApi\b", $NewApi
                    $content = $content -replace "\b$CurSubApp\b", $NewApp
                    $content = $content -replace "\b$CurSubClient\b", $NewClient
                    
                    # Normalize any versioned jar names like <module>-1.0.jar -> <module>.jar
                    $content = [Regex]::Replace($content, '([A-Za-z0-9_.-]+-application)-([0-9]+(?:\.[0-9]+)*)\.jar', '$1.jar')
                    
                    Set-Content $dockerPath $content
                    Log "Updated $dockerFile"
                } catch {
                    Warn "Failed to update $dockerFile : $_"
                }
            } else {
                Warn "$dockerFile not found (skipped)"
            }
        }
        
        Ok "Docker files updated"
    } catch {
        Warn "Failed to update Docker files in $BasePath : $_"
    }
}

# ---------- module-specific processing functions ----------
function Process-ApiModule {
    param(
        [string]$ModulePath
    )
    
    # Find and rename model folder - use the correct path structure
    $modelPath = Join-Path $ModulePath "src\main\kotlin\com\pieq\model"
    
    if (Test-Path $modelPath) {
        $newModelPath = Join-Path (Split-Path $modelPath -Parent) $PackageSegment
        try {
            Rename-Item -Path $modelPath -NewName $PackageSegment -Force
            
            Log "Renamed 'model' folder to '$PackageSegment' in API module"
            
            # Update package references in Kotlin files
            $ktFiles = Get-ChildItem -Path $newModelPath -Filter *.kt -Recurse -ErrorAction SilentlyContinue
            foreach ($file in $ktFiles) {
                Update-PackageReferences -FilePath $file.FullName -OldPackage "$CurBasePkg.model" -NewPackage "$CurBasePkg.$PackageSegment"
            }
        } catch {
            Warn "Failed to rename 'model' folder to '$PackageSegment' in API module: $_"
        }
    } else {
        Warn "'model' folder not found in API module, skipping model rename and package update."
    }
}

function Process-ApplicationModule {
    param(
        [string]$ModulePath
    )
    
    # Process both main and test application folders - use the correct path structure
    $appFolders = @(
        "src\main\kotlin\com\pieq\application",
        "src\test\kotlin\com\pieq\application"
    )
    
    foreach ($appFolderRel in $appFolders) {
        $appSrcPath = Join-Path $ModulePath $appFolderRel
        
        if (Test-Path $appSrcPath) {
            $newAppPath = Join-Path (Split-Path $appSrcPath -Parent) $PackageSegment
            try {
                Rename-Item -Path $appSrcPath -NewName $PackageSegment -Force
                Log "Renamed 'application' folder to '$PackageSegment' ($appFolderRel)"
                
                # Process Kotlin files in the renamed folder
                $ktFiles = Get-ChildItem -Path $newAppPath -Filter *.kt -Recurse -ErrorAction SilentlyContinue
                foreach ($file in $ktFiles) {
                    # Rename file if it starts with Pieq*
                    $newFilePath = Rename-KotlinFile -FilePath $file.FullName -OldPrefix "Pieq" -NewPrefix $ClassPrefix
                    
                    # Update class names and package references
                    Update-ClassNames -FilePath $newFilePath -OldPrefix "Pieq" -NewPrefix $ClassPrefix
                }
            } catch {
                Warn "Failed to rename 'application' folder to '$PackageSegment' ($appFolderRel): $_"
            }
        } else {
            Warn "'application' folder not found ($appFolderRel), skipping application rename and package update."
        }
    }
    
    # Update build.gradle.kts files
    $buildGradleFiles = Get-ChildItem -Path $ModulePath -Recurse -Filter "build.gradle.kts" -ErrorAction SilentlyContinue
    foreach ($gradleFile in $buildGradleFiles) {
        Update-GradleFile -GradleFilePath $gradleFile.FullName
    }
}

function Process-ClientModule {
    param(
        [string]$ModulePath
    )
    
    # Process both main and test client folders - use the correct path structure
    $clientFolders = @(
        "src\main\kotlin\com\pieq\client",
        "src\test\kotlin\com\pieq\client"
    )
    
    foreach ($clientFolderRel in $clientFolders) {
        $clientSrcPath = Join-Path $ModulePath $clientFolderRel
        
        if (Test-Path $clientSrcPath) {
            $newClientPath = Join-Path (Split-Path $clientSrcPath -Parent) $PackageSegment
            try {
                Rename-Item -Path $clientSrcPath -NewName $PackageSegment -Force
                Log "Renamed 'client' folder to '$PackageSegment' ($clientFolderRel)"
                
                # Process Kotlin files in the renamed folder
                $ktFiles = Get-ChildItem -Path $newClientPath -Filter *.kt -Recurse -ErrorAction SilentlyContinue
                foreach ($file in $ktFiles) {
                    # Rename file if it starts with Pieq*
                    $newFilePath = Rename-KotlinFile -FilePath $file.FullName -OldPrefix "Pieq" -NewPrefix $ClassPrefix
                    
                    # Update class names and package references
                    Update-ClassNames -FilePath $newFilePath -OldPrefix "Pieq" -NewPrefix $ClassPrefix
                }
            } catch {
                Warn "Failed to rename 'client' folder to '$PackageSegment' ($clientFolderRel): $_"
            }
        } else {
            Warn "'client' folder not found ($clientFolderRel), skipping client rename and package update."
        }
    }
    
    # Update build.gradle.kts files
    $buildGradleFiles = Get-ChildItem -Path $ModulePath -Recurse -Filter "build.gradle.kts" -ErrorAction SilentlyContinue
    foreach ($gradleFile in $buildGradleFiles) {
        Update-GradleFile -GradleFilePath $gradleFile.FullName
    }
}

function Process-Module {
    param(
        [hashtable]$FolderInfo
    )
    
    $oldPath = Join-Path $TempFolder $FolderInfo.Old
    $newPath = Join-Path $TempFolder $FolderInfo.New
    
    if (Test-Path $oldPath) {
        try {
            Rename-Item -Path $oldPath -NewName $FolderInfo.New -Force
            Log "Renamed folder '$($FolderInfo.Old)' to '$($FolderInfo.New)'"
            
            # Process module-specific operations
            if ($FolderInfo.New -like "*-api") {
                Process-ApiModule -ModulePath $newPath
            } elseif ($FolderInfo.New -like "*-application") {
                Process-ApplicationModule -ModulePath $newPath
            } elseif ($FolderInfo.New -like "*-client") {
                Process-ClientModule -ModulePath $newPath
            }
        } catch {
            Warn "Failed to rename folder '$($FolderInfo.Old)' to '$($FolderInfo.New)': $_"
        }
    } else {
        Warn "Folder '$($FolderInfo.Old)' not found in temp directory, skipping rename."
    }
}

# ---------- main conversion function ----------
function Convert-Template {
    $foldersToRename = @(
        @{ Old = $CurSubApi;    New = $NewApi },
        @{ Old = $CurSubApp;    New = $NewApp },
        @{ Old = $CurSubClient; New = $NewClient }
    )
    
    foreach ($folder in $foldersToRename) {
        Process-Module -FolderInfo $folder
    }
    
    # Update settings.gradle.kts in the temp folder
    $settingsGradlePath = Join-Path $TempFolder "settings.gradle.kts"
    Log "Updating settings.gradle.kts in $settingsGradlePath"
    if (Test-Path $settingsGradlePath) {
        Update-SettingsGradle -SettingsGradlePath $settingsGradlePath
    } else {
        Warn "settings.gradle.kts not found in temp directory"
    }
    
    # Update YAML files in the temp folder
    Update-YamlFiles -BasePath $TempFolder
    
    # Update Docker files in the temp folder
    Update-DockerFiles -BasePath $TempFolder
}

function Show-NextSteps {
    Write-Host ""
    Write-Host "Next steps:"
    Write-Host "  1. Test the build: .\gradlew clean build"
    Write-Host "  2. Run the application: .\gradlew :$NewApp:installDist"
    Write-Host "  3. Start the server: .\$NewApp\build\install\$NewApp\bin\$NewApp server $NewApp\src\main\resources\config_dev.yml"
    Write-Host ""
    Write-Host "The project has been successfully converted to $ServiceName structure."
}

function Overwrite-CurrentFolder {
    try {
        Log "Overwriting current folder with converted files from $TempFolder"
        
        # Get all items from temp folder
        $tempItems = Get-ChildItem -Path $TempFolder -Force
        
        foreach ($item in $tempItems) {
            $destinationPath = Join-Path (Get-Location) $item.Name
            
            if ($item.PSIsContainer) {
                # For directories, remove existing and copy new
                if (Test-Path $destinationPath) {
                    Remove-Item -Path $destinationPath -Recurse -Force
                }
                Copy-Item -Path $item.FullName -Destination (Split-Path $destinationPath -Parent) -Recurse -Force
                Log "Replaced directory: $($item.Name)"
            } else {
                # For files, copy with overwrite
                Copy-Item -Path $item.FullName -Destination $destinationPath -Force
                Log "Replaced file: $($item.Name)"
            }
        }
        
        # Clean up temp folder
        Remove-Item -Path $TempFolder -Recurse -Force
        Log "Cleaned up temporary folder"
        
        Ok "Successfully overwrote current folder with converted files"
    } catch {
        Err "Failed to overwrite current folder: $_"
    }
}

# ---------- main execution ----------
try {
    Get-UserInput
    Initialize-TargetConstants
    Test-TemplateStructure
    Initialize-TempFolder
    Copy-TemplateFiles
    Convert-Template
    Overwrite-CurrentFolder
    Ok "Conversion to $ServiceName template complete!"
    Show-NextSteps
} catch {
    Err "Conversion failed: $_"
}