# Настройка Firebase для Mwallet

## 1. Создать проект в Firebase Console

1. Откройте [Firebase Console](https://console.firebase.google.com/).
2. **Add project** → укажите имя (например `Mwallet`).
3. При необходимости отключите Google Analytics (не обязательно для Auth).

## 2. Добавить Android-приложение

1. В проекте Firebase: **Project settings** → **Your apps** → иконка **Android**.
2. **Android package name:** `com.example.mwallet` (как в `applicationId`).
3. Скачайте **`google-services.json`**.
4. Положите файл в папку **`app/`** (рядом с `app/build.gradle.kts`).

После этого пересоберите Android-проект — подключится плагин `google-services`.

## 3. Включить методы входа (Authentication)

1. **Build** → **Authentication** → **Get started**.
2. Вкладка **Sign-in method**:
   - **Email/Password** → Enable → Save.
   - **Phone** → Enable → Save.

### Телефон (важно для разработки)

- Для тестов добавьте номера в **Phone numbers for testing** (код фиксированный, SMS не уходит).
- Для реальных SMS на Android может понадобиться **SHA-1** и **SHA-256** отладочного ключа:
  ```bash
  keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
  ```
  Добавьте отпечатки в Firebase: **Project settings** → ваше Android-приложение → **Add fingerprint**.

## 4. Сервисный аккаунт для сервера (Ktor)

Сервер проверяет Firebase ID token через **Firebase Admin SDK**.

1. **Project settings** → **Service accounts**.
2. **Generate new private key** → скачается JSON-файл.
3. Переименуйте, например, в `firebase-service-account.json`.
4. Положите в **`server-mwallet/`** (рядом с `build.gradle.kts`).

### Подключение на сервере

В `server-mwallet/src/main/resources/application.conf`:

```hocon
firebase {
    credentials = "firebase-service-account.json"
}
```

Или переменная окружения:

```
FIREBASE_CREDENTIALS=C:\path\to\firebase-service-account.json
```

**Не коммитьте** JSON с ключом в git (добавьте в `.gitignore`).

## 5. Схема работы

```
[Android] Firebase Auth (email / phone)
       ↓ idToken
[Android] POST /api/auth/firebase { idToken, nickname?, phone? }
       ↓
[Server] Firebase Admin verifyIdToken
       ↓
[Server] пользователь + кошелёк в PostgreSQL → JWT для API
```

Поиск получателя при переводе:

```
GET /api/users/search?q=никнейм_или_телефон
Authorization: Bearer <JWT>
```

## 6. Проверка

1. Запустите PostgreSQL и сервер (`.\gradlew.bat run` в `server-mwallet`).
2. Убедитесь, что `firebase-service-account.json` на месте.
3. Запустите Android-приложение с `google-services.json` в `app/`.
4. Зарегистрируйтесь по email или телефону → откройте перевод → введите никнейм или номер в поисковой строке.
