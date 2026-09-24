$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $repositoryRoot

Write-Host "Removing generated files from the Git index..."
git rm -r --cached --ignore-unmatch -- `
    .gradle `
    .idea `
    .intellijPlatform `
    .kotlin `
    .run `
    build `
    out `
    work `
    outputs

git add -A

Write-Host "Creating a clean root commit from the current source tree..."
$tree = git write-tree
$message = "Initial release of Vertical Tabs"
$newCommit = $message | git commit-tree $tree
git update-ref refs/heads/master $newCommit

Write-Host "Removing unreachable large objects..."
git reflog expire --expire=now --all
git gc --prune=now --aggressive

Write-Host "Checking the cleaned repository..."
git status --short
git count-objects -vH

$trackedFiles = git ls-files
$largestFiles = foreach ($trackedFile in $trackedFiles) {
    if (Test-Path -LiteralPath $trackedFile -PathType Leaf) {
        $item = Get-Item -LiteralPath $trackedFile
        [pscustomobject]@{
            SizeKB = [math]::Round($item.Length / 1KB, 1)
            Path = $trackedFile
        }
    }
}

Write-Host "Largest tracked files:"
$largestFiles | Sort-Object SizeKB -Descending | Select-Object -First 15 | Format-Table -AutoSize

Write-Host ""
Write-Host "History cleanup completed. Push with:"
Write-Host "git push --set-upstream origin master --force-with-lease"
