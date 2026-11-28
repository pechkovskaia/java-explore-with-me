#!/bin/bash
BASE=http://localhost:8080/admin/users
EMAIL="user$RANDOM@mail.ru"   # случайный email, чтобы можно было запускать много раз

check() {
  if [ "$2" == "$3" ]; then
    echo "OK    $1 ($3)"
  else
    echo "FAIL  $1: ожидали $2, получили $3"
  fi
}

# 1. Создание пользователя
code=$(curl -s -o /tmp/user.json -w "%{http_code}" -X POST $BASE \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Ivan\",\"email\":\"$EMAIL\"}")
check "Создание пользователя" 201 $code
ID=$(grep -o '"id":[0-9]*' /tmp/user.json | cut -d: -f2)
echo "      создан пользователь с id=$ID"

# 2. Повторный email
code=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Petr\",\"email\":\"$EMAIL\"}")
check "Дубликат email" 409 $code

# 3. Пустое имя
code=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE \
  -H "Content-Type: application/json" \
  -d '{"name":"   ","email":"blank@mail.ru"}')
check "Пустое имя" 400 $code

# 4. Некорректный email
code=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE \
  -H "Content-Type: application/json" \
  -d '{"name":"Anna","email":"not-an-email"}')
check "Некорректный email" 400 $code

# 5. Получение по ids
code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE?ids=$ID")
check "Получение по ids" 200 $code

# 6. Получение с пагинацией
code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE?from=0&size=10")
check "Получение from/size" 200 $code

# 7. Удаление
code=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE/$ID")
check "Удаление" 204 $code

# 8. Повторное удаление
code=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE/$ID")
check "Удаление несуществующего" 404 $code