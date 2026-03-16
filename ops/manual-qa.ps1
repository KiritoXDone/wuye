$ErrorActionPreference = 'Stop'

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

$adminLogin = Invoke-JsonPost -Uri 'http://localhost:8080/api/v1/admin/auth/login/password' -Body @{
    username = 'admin'
    password = '123456'
}

$residentLogin = Invoke-JsonPost -Uri 'http://localhost:8080/api/v1/auth/login/wechat' -Body @{
    code = 'resident-zhangsan'
    nickname = 'resident-zhangsan'
}

$adminToken = $adminLogin.data.accessToken
$residentToken = $residentLogin.data.accessToken

$existingBills = Invoke-JsonGet -Uri 'http://localhost:8080/api/v1/me/bills?pageNo=1&pageSize=100' -Token $residentToken
$periodInfo = Get-NextPeriod -Bills $existingBills.data.list
$selectedYear = [int]$periodInfo.Year
$selectedMonth = [int]$periodInfo.Month
$selectedPeriod = [string]$periodInfo.Period
$monthText = $selectedMonth.ToString('00')
$effectiveFrom = '{0}-01-01' -f $selectedYear
$effectiveTo = '{0}-12-31' -f $selectedYear
$readAt = '{0}-{1}-28T09:00:00' -f $selectedYear, $monthText
$idempotencyKey = 'manual-qa-{0:yyyyMMddHHmmss}' -f (Get-Date)
$outTradeNo = 'WX-MANUAL-{0:yyyyMMddHHmmss}' -f (Get-Date)

$propertyRule = Invoke-JsonPost -Uri 'http://localhost:8080/api/v1/admin/fee-rules' -Token $adminToken -Body @{
    communityId = 100
    feeType = 'PROPERTY'
    unitPrice = 2.5000
    cycleType = 'MONTH'
    effectiveFrom = $effectiveFrom
    effectiveTo = $effectiveTo
    remark = 'manual property fee rule'
}

$waterRule = Invoke-JsonPost -Uri 'http://localhost:8080/api/v1/admin/fee-rules' -Token $adminToken -Body @{
    communityId = 100
    feeType = 'WATER'
    unitPrice = 3.2000
    cycleType = 'MONTH'
    effectiveFrom = $effectiveFrom
    effectiveTo = $effectiveTo
    remark = 'manual water fee rule'
}

$null = Invoke-JsonPost -Uri 'http://localhost:8080/api/v1/admin/bills/generate/property' -Token $adminToken -Body @{
    communityId = 100
    year = $selectedYear
    month = $selectedMonth
    overwriteStrategy = 'SKIP'
}

$null = Invoke-JsonPost -Uri 'http://localhost:8080/api/v1/admin/water-meters' -Token $adminToken -Body @{
    roomId = 1001
    meterNo = 'WM-1001'
}

$null = Invoke-JsonPost -Uri 'http://localhost:8080/api/v1/admin/water-readings' -Token $adminToken -Body @{
    roomId = 1001
    year = $selectedYear
    month = $selectedMonth
    prevReading = 1023.5
    currReading = 1034.2
    readAt = $readAt
}

$null = Invoke-JsonPost -Uri 'http://localhost:8080/api/v1/admin/bills/generate/water' -Token $adminToken -Body @{
    communityId = 100
    year = $selectedYear
    month = $selectedMonth
    overwriteStrategy = 'SKIP'
}

$residentBills = Invoke-JsonGet -Uri 'http://localhost:8080/api/v1/me/bills?pageNo=1&pageSize=100' -Token $residentToken
$selectedBills = @($residentBills.data.list | Where-Object { $_.period -eq $selectedPeriod })
$propertyBill = $selectedBills | Where-Object { $_.feeType -eq 'PROPERTY' } | Select-Object -First 1

if (-not $propertyBill) {
    throw "Property bill not found for period $selectedPeriod"
}

$payment = Invoke-JsonPost -Uri 'http://localhost:8080/api/v1/payments' -Token $residentToken -Body @{
    billId = $propertyBill.billId
    channel = 'WECHAT'
    idempotencyKey = $idempotencyKey
}

$null = Invoke-JsonPost -Uri 'http://localhost:8080/api/v1/callbacks/wechatpay' -Body @{
    payOrderNo = $payment.data.payOrderNo
    outTradeNo = $outTradeNo
}

$paymentStatus = Invoke-JsonGet -Uri ("http://localhost:8080/api/v1/payments/{0}" -f $payment.data.payOrderNo) -Token $residentToken
$report = Invoke-JsonGet -Uri ("http://localhost:8080/api/v1/admin/reports/monthly?periodYear={0}&periodMonth={1}" -f $selectedYear, $selectedMonth) -Token $adminToken

[PSCustomObject]@{
    period = $selectedPeriod
    adminAccountId = $adminLogin.data.accountId
    residentAccountId = $residentLogin.data.accountId
    propertyRuleId = $propertyRule.data.id
    waterRuleId = $waterRule.data.id
    selectedPeriodBillCount = $selectedBills.Count
    payOrderNo = $payment.data.payOrderNo
    paymentStatus = $paymentStatus.data.status
    paidCount = $report.data.paidCount
    totalCount = $report.data.totalCount
    payRate = $report.data.payRate
    paidAmount = $report.data.paidAmount
    unpaidAmount = $report.data.unpaidAmount
} | ConvertTo-Json -Depth 4
