mvn test "-Dtest=CustomerServiceTest,CustomerApiIntegrationTest" -q
Write-Output "EXITCODE=$LASTEXITCODE"
