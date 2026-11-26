#!/bin/bash
URL=localhost:8080
PASS=0; FAIL=0

check() {
  local desc=$1 expected=$2; shift 2
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" "$@")
  if [ "$code" == "$expected" ]; then
    echo "OK   [$code] $desc"; PASS=$((PASS+1))
  else
    echo "FAIL [$code, ждали $expected] $desc"; FAIL=$((FAIL+1))
  fi
}

J=(-H "Content-Type: application/json")
LONG=$(printf 'a%.0s' {1..51})
MAX=$(printf 'b%.0s' {1..50})

check "создать категорию"            201 -X POST $URL/admin/categories "${J[@]}" -d '{"name":"Концерты"}'
check "создать вторую"               201 -X POST $URL/admin/categories "${J[@]}" -d '{"name":"Театр"}'
check "дубликат имени"               409 -X POST $URL/admin/categories "${J[@]}" -d '{"name":"Концерты"}'
check "пустое имя"                   400 -X POST $URL/admin/categories "${J[@]}" -d '{"name":""}'
check "имя из пробелов"              400 -X POST $URL/admin/categories "${J[@]}" -d '{"name":"   "}'
check "без поля name"                400 -X POST $URL/admin/categories "${J[@]}" -d '{}'
check "имя 51 символ"                400 -X POST $URL/admin/categories "${J[@]}" -d "{\"name\":\"$LONG\"}"
check "имя 50 символов"              201 -X POST $URL/admin/categories "${J[@]}" -d "{\"name\":\"$MAX\"}"
check "битый JSON"                   400 -X POST $URL/admin/categories "${J[@]}" -d '{name:'

check "PATCH на то же имя"           200 -X PATCH $URL/admin/categories/1 "${J[@]}" -d '{"name":"Концерты"}'
check "PATCH на новое имя"           200 -X PATCH $URL/admin/categories/1 "${J[@]}" -d '{"name":"Концерты 2"}'
check "PATCH на чужое имя"           409 -X PATCH $URL/admin/categories/1 "${J[@]}" -d '{"name":"Театр"}'
check "PATCH несуществующей"         404 -X PATCH $URL/admin/categories/999 "${J[@]}" -d '{"name":"X"}'
check "PATCH имя 51 символ"          400 -X PATCH $URL/admin/categories/1 "${J[@]}" -d "{\"name\":\"$LONG\"}"

check "GET по id"                    200 $URL/categories/1
check "GET несуществующей"           404 $URL/categories/999
check "GET id не число"              400 $URL/categories/abc
check "GET список"                   200 "$URL/categories?from=0&size=10"
check "GET список size=0"            400 "$URL/categories?from=0&size=0"
check "GET список from=-1"           400 "$URL/categories?from=-1&size=10"

check "DELETE"                       204 -X DELETE $URL/admin/categories/2
check "DELETE повторно"              404 -X DELETE $URL/admin/categories/2
check "можно занять имя удалённой"   201 -X POST $URL/admin/categories "${J[@]}" -d '{"name":"Театр"}'

echo "-----"; echo "Пройдено: $PASS, упало: $FAIL"
echo; echo "Тело ошибки (проверь формат):"
curl -s $URL/categories/999; echo