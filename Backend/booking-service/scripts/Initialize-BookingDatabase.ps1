param(
    [string]$MysqlPath = 'C:\xampp\mysql\bin\mysql.exe',
    [string]$DatabaseHost = '127.0.0.1',
    [int]$DatabasePort = 3306,
    [string]$RootUser = 'root'
)
$ErrorActionPreference = 'Stop'
$taskModule = Split-Path -Parent $PSScriptRoot
$taskLocalConfig = Join-Path $taskModule 'application-local.properties'
if (Test-Path -LiteralPath $taskLocalConfig) {
    Write-Output 'application-local.properties existe deja. Configuration conservee.'
    return
}
if (-not (Test-Path -LiteralPath $MysqlPath)) { throw 'Client MySQL introuvable. Precisez -MysqlPath.' }
$taskPreviousPassword = $env:MYSQL_PWD
try {
    # Pour un compte root protege, definir BOOKING_SETUP_ROOT_PASSWORD dans la session.
    $env:MYSQL_PWD = $env:BOOKING_SETUP_ROOT_PASSWORD
    $taskArguments = @('--protocol=TCP', "--host=$DatabaseHost", "--port=$DatabasePort", "--user=$RootUser", '--batch', '--skip-column-names')
    $taskExisting = & $MysqlPath @taskArguments -e "SELECT COUNT(*) FROM mysql.user WHERE User='booking_svc' AND Host='localhost';"
    if ($LASTEXITCODE -ne 0) { throw 'Connexion administrateur impossible. Verifiez le serveur et BOOKING_SETUP_ROOT_PASSWORD.' }
    if ([int]$taskExisting -gt 0) { throw 'Le compte booking_svc existe deja. Configurez son mot de passe dans application-local.properties ; aucun compte existant n a ete modifie.' }
    $taskPassword = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
    # Conserver le secret avant les commandes SQL : un GRANT peut echouer apres CREATE USER.
    $taskConfiguration = "spring.datasource.url=jdbc:mysql://${DatabaseHost}:${DatabasePort}/booking_db`nspring.datasource.username=booking_svc`nspring.datasource.password=$taskPassword`n"
    [IO.File]::WriteAllText($taskLocalConfig, $taskConfiguration, [Text.UTF8Encoding]::new($false))
    $taskSql = @"
CREATE DATABASE IF NOT EXISTS booking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'booking_svc'@'localhost' IDENTIFIED BY '$taskPassword';
GRANT ALL PRIVILEGES ON booking_db.* TO 'booking_svc'@'localhost';
"@
    $taskSql | & $MysqlPath @taskArguments
    if ($LASTEXITCODE -ne 0) { throw 'Initialisation MySQL impossible.' }
    Write-Output 'booking_db et booking_svc crees. Mot de passe genere dans application-local.properties (ignore par Git).'
} finally {
    $env:MYSQL_PWD = $taskPreviousPassword
    $taskPassword = $null
    $taskSql = $null
    $taskConfiguration = $null
}
