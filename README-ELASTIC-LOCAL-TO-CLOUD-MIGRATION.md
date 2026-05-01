# Elasticsearch Local -> Elastic Cloud Migration (Index + Mapping + Aliases + Data)

This guide migrates all non-system indices from local Elasticsearch to Elastic Cloud, including:

- index settings
- mappings (including analyzers)
- all documents
- aliases

It works with your current index definition (for example `products_v2` + `products` alias) and also supports multiple indices.

## 1) Prerequisites

- Local Elasticsearch is running (example: `http://localhost:9200`)
- Elastic Cloud deployment endpoint (example: `https://<cluster-id>.<region>.aws.elastic-cloud.com:443`)
- Elastic Cloud API key with index/admin privileges
- PowerShell 7+ (or Windows PowerShell 5.1)

## 2) Configure Variables

```powershell
$LOCAL_ES_URL = "http://localhost:9200"
$CLOUD_ES_URL = "https://<elastic-cloud-endpoint>:443"
$CLOUD_API_KEY = "<base64-api-key>"

$CloudHeaders = @{
  Authorization = "ApiKey $CLOUD_API_KEY"
}
```

## 3) Run Full Migration Script

Run this from PowerShell. It migrates all open non-system indices (`.`-prefixed indices are skipped).

```powershell
$ErrorActionPreference = "Stop"

$LOCAL_ES_URL = "http://localhost:9200"
$CLOUD_ES_URL = "https://<elastic-cloud-endpoint>:443"
$CLOUD_API_KEY = "<base64-api-key>"

$CloudHeaders = @{ Authorization = "ApiKey $CLOUD_API_KEY" }

function Invoke-EsJson {
  param(
    [string]$Method,
    [string]$Url,
    [hashtable]$Headers,
    [object]$Body = $null,
    [string]$ContentType = "application/json"
  )

  if ($null -eq $Body) {
    return Invoke-RestMethod -Method $Method -Uri $Url -Headers $Headers
  }

  if ($Body -is [string]) {
    return Invoke-RestMethod -Method $Method -Uri $Url -Headers $Headers -Body $Body -ContentType $ContentType
  }

  return Invoke-RestMethod -Method $Method -Uri $Url -Headers $Headers -Body ($Body | ConvertTo-Json -Depth 100) -ContentType $ContentType
}

function Remove-ReadOnlyIndexSettings {
  param([hashtable]$IndexSettings)

  $readOnlyKeys = @(
    "creation_date", "provided_name", "uuid", "version",
    "routing", "resize", "history_uuid", "verified_before_close"
  )

  foreach ($k in $readOnlyKeys) {
    if ($IndexSettings.ContainsKey($k)) {
      $IndexSettings.Remove($k)
    }
  }

  return $IndexSettings
}

Write-Host "1/5 Fetch local indices..."
$localIndicesResp = Invoke-RestMethod -Method GET -Uri "$LOCAL_ES_URL/_cat/indices?format=json&h=index,status"
$indices = @($localIndicesResp | Where-Object { $_.status -eq "open" -and $_.index -notlike ".*" } | ForEach-Object { $_.index })

if ($indices.Count -eq 0) {
  throw "No non-system open indices found in local cluster."
}

Write-Host "Indices to migrate: $($indices -join ', ')"

Write-Host "2/5 Create target indices in cloud (settings + mappings)..."
foreach ($index in $indices) {
  Write-Host "Preparing index: $index"

  $localDef = Invoke-RestMethod -Method GET -Uri "$LOCAL_ES_URL/$index"
  $src = $localDef.$index

  $settingsHash = $src.settings.index | ConvertTo-Json -Depth 100 | ConvertFrom-Json -AsHashtable
  $settingsHash = Remove-ReadOnlyIndexSettings -IndexSettings $settingsHash

  # Optional safety default for cloud (uncomment if needed)
  # if (-not $settingsHash.ContainsKey("number_of_replicas")) { $settingsHash["number_of_replicas"] = "1" }

  $createBody = @{
    settings = @{ index = $settingsHash }
    mappings = ($src.mappings | ConvertTo-Json -Depth 100 | ConvertFrom-Json -AsHashtable)
  }

  try {
    Invoke-EsJson -Method PUT -Url "$CLOUD_ES_URL/$index" -Headers $CloudHeaders -Body $createBody | Out-Null
    Write-Host "Created index in cloud: $index"
  }
  catch {
    if ($_.Exception.Message -match "resource_already_exists_exception") {
      Write-Host "Index already exists in cloud: $index (skipping create)"
    }
    else {
      throw
    }
  }
}

Write-Host "3/5 Copy documents via scroll + bulk..."
foreach ($index in $indices) {
  Write-Host "Copying docs for index: $index"

  $searchBody = @{
    size = 500
    sort = @("_doc")
    query = @{ match_all = @{} }
  }

  $firstPage = Invoke-EsJson -Method POST -Url "$LOCAL_ES_URL/$index/_search?scroll=2m" -Headers @{} -Body $searchBody
  $scrollId = $firstPage._scroll_id
  $hits = @($firstPage.hits.hits)
  $totalCopied = 0

  while ($hits.Count -gt 0) {
    $lines = New-Object System.Collections.Generic.List[string]

    foreach ($hit in $hits) {
      $action = @{ index = @{ _index = $index; _id = $hit._id } } | ConvertTo-Json -Compress
      $doc = $hit._source | ConvertTo-Json -Compress -Depth 100
      $lines.Add($action)
      $lines.Add($doc)
    }

    $ndjson = ($lines -join "`n") + "`n"
    $bulkResp = Invoke-EsJson -Method POST -Url "$CLOUD_ES_URL/_bulk?refresh=false" -Headers $CloudHeaders -Body $ndjson -ContentType "application/x-ndjson"

    if ($bulkResp.errors -eq $true) {
      throw "Bulk indexing returned errors for index: $index"
    }

    $totalCopied += $hits.Count
    Write-Host "Indexed $totalCopied docs into $index"

    $scrollResp = Invoke-EsJson -Method POST -Url "$LOCAL_ES_URL/_search/scroll" -Headers @{} -Body @{ scroll = "2m"; scroll_id = $scrollId }
    $scrollId = $scrollResp._scroll_id
    $hits = @($scrollResp.hits.hits)
  }

  if ($scrollId) {
    Invoke-EsJson -Method DELETE -Url "$LOCAL_ES_URL/_search/scroll" -Headers @{} -Body @{ scroll_id = @($scrollId) } | Out-Null
  }

  Write-Host "Completed doc copy for $index. Total copied: $totalCopied"
}

Write-Host "4/5 Recreate aliases in cloud..."
$localAliases = Invoke-RestMethod -Method GET -Uri "$LOCAL_ES_URL/_aliases"
$actions = New-Object System.Collections.Generic.List[object]

foreach ($idxProp in $localAliases.PSObject.Properties) {
  $indexName = $idxProp.Name
  $aliasesObj = $idxProp.Value.aliases

  if ($null -eq $aliasesObj) { continue }

  foreach ($aliasProp in $aliasesObj.PSObject.Properties) {
    $aliasName = $aliasProp.Name
    $aliasData = $aliasProp.Value

    $add = @{
      index = $indexName
      alias = $aliasName
    }

    if ($null -ne $aliasData.filter) { $add["filter"] = $aliasData.filter }
    if ($null -ne $aliasData.routing) { $add["routing"] = $aliasData.routing }
    if ($null -ne $aliasData.index_routing) { $add["index_routing"] = $aliasData.index_routing }
    if ($null -ne $aliasData.search_routing) { $add["search_routing"] = $aliasData.search_routing }
    if ($null -ne $aliasData.is_write_index) { $add["is_write_index"] = $aliasData.is_write_index }

    $actions.Add(@{ add = $add })
  }
}

if ($actions.Count -gt 0) {
  Invoke-EsJson -Method POST -Url "$CLOUD_ES_URL/_aliases" -Headers $CloudHeaders -Body @{ actions = $actions } | Out-Null
  Write-Host "Aliases created/updated in cloud."
}
else {
  Write-Host "No aliases found in local cluster."
}

Write-Host "5/5 Verify counts and aliases..."
$localCount = Invoke-RestMethod -Method GET -Uri "$LOCAL_ES_URL/_cat/indices?format=json&h=index,docs.count" | Where-Object { $_.index -notlike ".*" }
$cloudCount = Invoke-RestMethod -Method GET -Uri "$CLOUD_ES_URL/_cat/indices?format=json&h=index,docs.count" -Headers $CloudHeaders | Where-Object { $_.index -notlike ".*" }

Write-Host "Local counts:"
$localCount | Format-Table -AutoSize

Write-Host "Cloud counts:"
$cloudCount | Format-Table -AutoSize

Write-Host "Cloud aliases:"
Invoke-RestMethod -Method GET -Uri "$CLOUD_ES_URL/_cat/aliases?format=json" -Headers $CloudHeaders | Format-Table -AutoSize

Write-Host "Migration complete."
```

## 4) Quick Validation Commands

```powershell
# index health in cloud
Invoke-RestMethod -Method GET -Uri "$CLOUD_ES_URL/_cluster/health" -Headers $CloudHeaders

# list cloud indices
Invoke-RestMethod -Method GET -Uri "$CLOUD_ES_URL/_cat/indices?v" -Headers $CloudHeaders

# list cloud aliases
Invoke-RestMethod -Method GET -Uri "$CLOUD_ES_URL/_cat/aliases?v" -Headers $CloudHeaders
```

## 5) If You Only Need the Product Index

If you only want your product index + alias (example `products_v2` and `products`), keep only that index in `$indices`:

```powershell
$indices = @("products_v2")
```

The script will still copy mapping/settings/data and then recreate aliases from local to cloud.

## Notes

- Existing target indices are not deleted by this script.
- If a target index already exists with incompatible mapping, delete it in cloud first and rerun.
- Ensure your app config points to cloud after migration:
  - `ES_URL=https://<elastic-cloud-endpoint>:443`
  - `ES_BASE64_API_KEY=<base64-api-key>`
  - `ES_INDEX=products_v2`
  - `ES_INDEX_ALIAS=products`
