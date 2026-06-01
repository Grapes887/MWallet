# Mwallet — Android-клиент

Jetpack Compose + Firebase Auth + REST API к серверу **Ktor**.

Сервер: https://github.com/Grapes887/MWallet-server
(порт **8080**, PostgreSQL, Firebase Admin — см. `application.conf` на сервере).

## Запуск сервера

Из папки сервера:

```bash
cd "..\server-mwallet"
.\gradlew.bat run
```

Убедитесь, что PostgreSQL запущен с параметрами из `application.conf` (или переменных `DATABASE_*`), и в каталоге сервера лежит `firebase-service-account.json` из того же Firebase-проекта, что и `app/google-services.json`.

## Запуск Android

1. Скопируйте `local.properties.example` → `local.properties` (если ещё нет).
2. Укажите **`api.base.url`** — адрес машины, где крутится сервер:

| Где запускаете приложение | `api.base.url` |
|---------------------------|----------------|
| Эмулятор Android Studio | `http://10.0.2.2:8080` |
| Телефон в той же Wi‑Fi | `http://<IP_вашего_ПК>:8080` (узнать: `ipconfig`) |

3. **File → Sync Project with Gradle Files**, соберите и запустите `:app`.

## API (совпадает с сервером на рабочем столе)

| Метод | Путь | JWT |
|-------|------|-----|
| POST | `/api/auth/resolve-login` | нет |
| POST | `/api/auth/reset-lookup` | нет |
| POST | `/api/auth/firebase` | нет (в ответе выдаётся JWT) |
| GET | `/api/wallet` | да |
| POST | `/api/wallet/deposit` | да |
| POST | `/api/wallet/transfer` | да |
| GET | `/api/wallet/transactions` | да |
| GET | `/api/users/search?q=` | да |

После входа клиент сохраняет JWT и отправляет заголовок `Authorization: Bearer <token>`.
