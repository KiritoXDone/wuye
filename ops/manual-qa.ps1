$ErrorActionPreference = 'Stop'

$apiBaseUrl = if ($env:API_BASE_URL) { $env:API_BASE_URL.TrimEnd('/') } else { 'http://localhost:8080' }

function Resolve-ApiUri {
    param([string]$Path)
    return "$apiBaseUrl$Path"
}

function Invoke-JsonPost {
    param(
        [string]$Uri,
        [object]$Body,
        [string]$Token
    )

    $headers = @{}
    if ($Token) {
        $headers['Authorization'] = "Bearer $Token"
    }

    $payload = $Body | ConvertTo-Json -Depth 8 -Compress

    try {
        Invoke-RestMethod -Method Post -Uri $Uri -ContentType 'application/json; charset=utf-8' -Headers $headers -Body $payload
    }
    catch {
        $message = $_.Exception.Message
        $responseBody = $null
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
        }
        throw "POST $Uri failed. message=$message body=$payload response=$responseBody"
    }
}

function Invoke-JsonGet {
    param(
        [string]$Uri,
        [string]$Token
    )

    $headers = @{}
    if ($Token) {
        $headers['Authorization'] = "Bearer $Token"
    }

    try {
        Invoke-RestMethod -Method Get -Uri $Uri -Headers $headers
    }
    catch {
        $message = $_.Exception.Message
        $responseBody = $null
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
        }
        throw "GET $Uri failed. message=$message response=$responseBody"
    }
}

function Get-NextPeriod {
    param(
        [object[]]$Bills
    )

    $existing = @{}
    foreach ($bill in $Bills) {
        if ($bill.period) {
            $existing[$bill.period] = $true
        }
    }

    $startYear = (Get-Date).Year
    for ($year = $startYear; $year -le $startYear + 5; $year++) {
        for ($month = 1; $month -le 12; $month++) {
            $period = '{0}-{1}' -f $year, $month.ToString('00')
            if (-not $existing.ContainsKey($period)) {
                return @{
                    Year = $year
                    Month = $month
                    Period = $period
                }
            }
        }
    }

    throw 'No available period found for manual QA'
}

$adminLogin = Invoke-JsonPost -Uri (Resolve-ApiUri '/api/v1/admin/auth/login/password') -Body @{
    username = 'admin'
    password = '123456'
}

$residentLogin = Invoke-JsonPost -Uri (Resolve-ApiUri '/api/v1/auth/login/wechat') -Body @{
    code = 'resident-zhangsan'
    nickname = 'resident-zhangsan'
}

$adminToken = $adminLogin.data.accessToken
$residentToken = $residentLogin.data.accessToken

$existingBills = Invoke-JsonGet -Uri (Resolve-ApiUri '/api/v1/me/bills?pageNo=1&pageSize=100') -Token $residentToken
$periodInfo = Get-NextPeriod -Bills $existingBills.data.list
$selectedYear = [int]$periodInfo.Year
$selectedMonth = [int]$periodInfo.Month
$selectedPeriod = [string]$periodInfo.Period
$monthText = $selectedMonth.ToString('00')
$importYear = if ($selectedMonth -eq 12) { $selectedYear + 1 } else { $selectedYear }
$importMonth = if ($selectedMonth -eq 12) { 1 } else { $selectedMonth + 1 }
$importMonthText = $importMonth.ToString('00')
$effectiveFrom = '{0}-01-01' -f $selectedYear
$effectiveTo = '{0}-12-31' -f $selectedYear
$readAt = '{0}-{1}-28T09:00:00' -f $selectedYear, $monthText
$idempotencyKey = 'manual-qa-{0:yyyyMMddHHmmss}' -f (Get-Date)
$outTradeNo = 'ALI-MANUAL-{0:yyyyMMddHHmmss}' -f (Get-Date)
$alipayMerchantId = if ($env:ALIPAY_MERCHANT_ID) { $env:ALIPAY_MERCHANT_ID } else { 'alipay-dev-mock-merchant' }
$alipaySecret = if ($env:ALIPAY_CALLBACK_SECRET) { $env:ALIPAY_CALLBACK_SECRET } else { 'alipay-dev-callback-secret' }

function Get-CallbackSign {
    param(
        [string]$PayOrderNo,
        [string]$OutTradeNo,
        [string]$MerchantId,
        [decimal]$TotalAmount,
        [string]$Secret
    )

    $payload = "$PayOrderNo|$OutTradeNo|$MerchantId|$([decimal]::Parse($TotalAmount).ToString('0.00'))|$Secret"
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($payload)
        $hash = $sha.ComputeHash($bytes)
        return ([System.BitConverter]::ToString($hash)).Replace('-', '').ToLowerInvariant()
    }
    finally {
        $sha.Dispose()
    }
}

$propertyRule = Invoke-JsonPost -Uri (Resolve-ApiUri '/api/v1/admin/fee-rules') -Token $adminToken -Body @{
    communityId = 100
    feeType = 'PROPERTY'
    unitPrice = 2.5000
    cycleType = 'MONTH'
    effectiveFrom = $effectiveFrom
    effectiveTo = $effectiveTo
    remark = 'manual property fee rule'
}

$waterRule = Invoke-JsonPost -Uri (Resolve-ApiUri '/api/v1/admin/fee-rules') -Token $adminToken -Body @{
    communityId = 100
    feeType = 'WATER'
    unitPrice = 3.2000
    cycleType = 'MONTH'
    effectiveFrom = $effectiveFrom
    effectiveTo = $effectiveTo
    remark = 'manual water fee rule'
}

$null = Invoke-JsonPost -Uri (Resolve-ApiUri '/api/v1/admin/bills/generate/property') -Token $adminToken -Body @{
    communityId = 100
    year = $selectedYear
    month = $selectedMonth
    overwriteStrategy = 'SKIP'
}

$null = Invoke-JsonPost -Uri (Resolve-ApiUri '/api/v1/admin/water-meters') -Token $adminToken -Body @{
    roomId = 1001
    meterNo = 'WM-1001'
}

$null = Invoke-JsonPost -Uri (Resolve-ApiUri '/api/v1/admin/water-readings') -Token $adminToken -Body @{
    roomId = 1001
    year = $selectedYear
    month = $selectedMonth
    prevReading = 1023.5
    currReading = 1034.2
    readAt = $readAt
}

$null = Invoke-JsonPost -Uri (Resolve-ApiUri '/api/v1/admin/bills/generate/water') -Token $adminToken -Body @{
    communityId = 100
    year = $selectedYear
    month = $selectedMonth
    overwriteStrategy = 'SKIP'
}

$residentBills = Invoke-JsonGet -Uri (Resolve-ApiUri '/api/v1/me/bills?pageNo=1&pageSize=100') -Token $residentToken
$selectedBills = @($residentBills.data.list | Where-Object { $_.period -eq $selectedPeriod })
$propertyBill = $selectedBills | Where-Object { $_.feeType -eq 'PROPERTY' } | Select-Object -First 1

if (-not $propertyBill) {
    throw "Property bill not found for period $selectedPeriod"
}

$payment = Invoke-JsonPost -Uri (Resolve-ApiUri '/api/v1/payments') -Token $residentToken -Body @{
    billId = $propertyBill.billId
    channel = 'ALIPAY'
    idempotencyKey = $idempotencyKey
}

$callbackSign = Get-CallbackSign -PayOrderNo $payment.data.payOrderNo -OutTradeNo $outTradeNo -MerchantId $alipayMerchantId -TotalAmount ([decimal]$payment.data.payAmount) -Secret $alipaySecret

$null = Invoke-JsonPost -Uri (Resolve-ApiUri '/api/v1/callbacks/alipay') -Body @{
    payOrderNo = $payment.data.payOrderNo
    outTradeNo = $outTradeNo
    merchantId = $alipayMerchantId
    totalAmount = $payment.data.payAmount
    sign = $callbackSign
}

$paymentStatus = Invoke-JsonGet -Uri (Resolve-ApiUri ("/api/v1/payments/{0}" -f $payment.data.payOrderNo)) -Token $residentToken
$report = Invoke-JsonGet -Uri (Resolve-ApiUri ("/api/v1/admin/reports/monthly?periodYear={0}&periodMonth={1}" -f $selectedYear, $selectedMonth)) -Token $adminToken

$importFile = Join-Path $env:TEMP ("bill-import-{0}.csv" -f (Get-Date -Format 'yyyyMMddHHmmss'))
@"
bill_no,fee_type,period_year,period_month,community_code,building_no,unit_no,room_no,group_code,amount_due,due_date,remark
B-MANUAL-001,PROPERTY,$importYear,$importMonth,COMM-001,1,2,302,G-COMM001-1-2,188.88,$importYear-$importMonthText-28,manual import success
B-MANUAL-002,PROPERTY,$importYear,$importMonth,COMM-001,1,2,302,INVALID,188.88,$importYear-$importMonthText-28,manual import fail
"@ | Set-Content -Path $importFile -Encoding UTF8

$importBatch = Invoke-JsonPost -Uri (Resolve-ApiUri '/api/v1/admin/imports/bills') -Token $adminToken -Body @{
    fileUrl = $importFile
}

$importErrors = Invoke-JsonGet -Uri (Resolve-ApiUri ("/api/v1/admin/imports/{0}/errors" -f $importBatch.data.id)) -Token $adminToken

$exportJob = Invoke-JsonPost -Uri (Resolve-ApiUri '/api/v1/admin/exports/bills') -Token $adminToken -Body @{
    periodYear = $selectedYear
    periodMonth = $selectedMonth
    feeType = 'PROPERTY'
    status = 'PAID'
}

$exportFileExists = if ($exportJob.data.fileUrl) { Test-Path $exportJob.data.fileUrl } else { $false }

[PSCustomObject]@{
    apiBaseUrl = $apiBaseUrl
    period = $selectedPeriod
    adminAccountId = $adminLogin.data.accountId
    residentAccountId = $residentLogin.data.accountId
    propertyRuleId = $propertyRule.data.id
    waterRuleId = $waterRule.data.id
    selectedPeriodBillCount = $selectedBills.Count
    payOrderNo = $payment.data.payOrderNo
    paymentChannel = $payment.data.channel
    paymentStatus = $paymentStatus.data.status
    rewardIssuedCount = $paymentStatus.data.rewardIssuedCount
    importBatchId = $importBatch.data.id
    importSuccessCount = $importBatch.data.successCount
    importFailCount = $importBatch.data.failCount
    importFirstErrorCode = if ($importErrors.data.Count -gt 0) { $importErrors.data[0].errorCode } else { $null }
    exportJobId = $exportJob.data.id
    exportStatus = $exportJob.data.status
    exportFileExists = $exportFileExists
    paidCount = $report.data.paidCount
    totalCount = $report.data.totalCount
    payRate = $report.data.payRate
    paidAmount = $report.data.paidAmount
    unpaidAmount = $report.data.unpaidAmount
} | ConvertTo-Json -Depth 4
