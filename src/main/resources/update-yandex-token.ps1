# Выполнение команды yc для создания нового токена
$token = yc iam create-token

# Проверяем, начинается ли токен с ">>" и убираем его
if ($token.StartsWith(">>")) {
    $token = $token.Substring(2) # Убираем первые два символа ">>"
}

# Выводим токен без префикса
Write-Output $token