
### Слоеве:
```
├── Presentation Layer (UI)
│   ├── MainActivity
│   ├── Adapters (Brewery, LoyaltyCard, Badge)
│   └── Layouts (activity_main, item_brewery, item_loyalty_card, item_badge)
│
├── Business Logic Layer
│   ├── Repositories (BreweryRepository, LoyaltyCardRepository)
│   └── Services (QRCodeService, LocationService, SyncService, GamificationService)
│
├── Data Layer
│   ├── Room Database (6 entities)
│   ├── DAOs (6 DAOs)
│   └── API Clients (Retrofit - Brewery API, Cartes API)
│
└── Models
    └── Entities (BreweryEntity, LoyaltyCardEntity, UserEntity, etc.)
```

## 🗄️ Database Schema

### Entities:
1. **BreweryEntity** - Информация за пивоварни (от Open Brewery DB API)
2. **LoyaltyCardEntity** - Виртуални карти с печати
3. **UserEntity** - Потребителски профили и статистики
4. **VisitEntity** - История на посещения
5. **BadgeEntity** - Gamification badges
6. **BeerRatingEntity** - Рейтинги на бири

## 🔌 API Интеграции

### 1. Open Brewery DB API
- `GET /breweries` - Списък с пивоварни
- `GET /breweries?by_city={city}` - Търсене по град
- `GET /breweries?by_dist={lat},{lon}` - Близки пивоварни

### 2. Cartes.io API
- Споделяне на виртуални карти
- Създаване на custom maps

### 3. Google Play Services
- Location API за GPS координати
- Distance calculations (Haversine formula)

## 🎨 UI Design

### Модерен Dark Theme с Amber Accent
- **Primary Color**: `#FFB300` (Amber - бирена тема)
- **Background**: Градиенти с кафяви тонове (`#1A0F00`, `#2D1F0A`)
- **Cards**: Material Design с 16-20dp радиус
- **Animations**: Smooth transitions между tabs

### Компоненти:
- 📊 Stats Card - Показва общи статистики
- 🔍 Search Bar - Търсене на пивоварни
- 📍 Location Button - GPS търсене
- 📷 QR Scanner - Сканиране на QR кодове
- 🎯 Tabs - Breweries / My Cards / Badges
- ♻️ RecyclerView - Динамичен списък с cards

## 🚀 Технологии

### Core
- **Language**: Java 11
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)

### Libraries
```gradle
// UI & Material Design
implementation 'com.google.android.material:material:1.x'
implementation 'androidx.constraintlayout:constraintlayout:2.x'
implementation 'androidx.recyclerview:recyclerview:1.3.2'

// Database
implementation 'androidx.room:room-runtime:2.7.1'
annotationProcessor 'androidx.room:room-compiler:2.7.1'

// Networking
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'

// QR Code
implementation 'com.google.zxing:core:3.5.2'
implementation 'com.journeyapps:zxing-android-embedded:4.3.0'

// Location
implementation 'com.google.android.gms:play-services-location:21.0.1'

// Image Loading
implementation 'com.github.bumptech.glide:glide:4.16.0'

// Background Tasks
implementation 'androidx.work:work-runtime:2.9.0'
```

## 📱 Функционални възможности

### Tabs:
1. **Breweries Tab**
   - Списък с всички пивоварни
   - Търсене по име
   - Добавяне към любими
   - Създаване на loyalty card

2. **My Cards Tab**
   - Всички активни loyalty cards
   - Прогрес на печати (progress bar + емоджита)
   - QR код бутон за показване
   - Share и History бутони

3. **Badges Tab**
   - Earned badges (пълен цвят)
   - Unlocked badges (полупрозрачни с прогрес)
   - 8 типа badges за отключване

## 🎮 Gamification Система

### Badge Types:
- 🎉 **First Visit** - Първо посещение
- 🥉 **Bronze Collector** - 10 печата
- 🥈 **Silver Collector** - 25 печата
- 🥇 **Gold Collector** - 50 печата
- 💎 **Platinum Collector** - 100 печата
- 🗺️ **Explorer** - 10 различни пивоварни
- 🦋 **Social Butterfly** - 5 споделяния
- 🍺 **Beer Connoisseur** - 20 рейтинга

## 🔐 Permissions
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## 📂 Файлова структура

```
app/src/main/java/com/example/aletrail/
├── Activities
│   └── MainActivity.java
├── Entities (6 files)
│   ├── BreweryEntity.java
│   ├── LoyaltyCardEntity.java
│   ├── UserEntity.java
│   ├── VisitEntity.java
│   ├── BadgeEntity.java
│   └── BeerRatingEntity.java
├── DAOs (6 files)
│   ├── AleTrailDAO.java
│   ├── LoyaltyCardDAO.java
│   ├── UserDAO.java
│   ├── VisitDAO.java
│   ├── BadgeDAO.java
│   └── BeerRatingDAO.java
├── Repositories
│   ├── BreweryRepository.java
│   └── LoyaltyCardRepository.java
├── Services
│   ├── QRCodeService.java
│   ├── LocationService.java
│   ├── SyncService.java
│   └── GamificationService.java
├── Adapters
│   ├── BreweryAdapter.java
│   ├── LoyaltyCardAdapter.java
│   └── BadgeAdapter.java
├── API
│   ├── TheAleTrailAPI.java
│   ├── TheCartesAPI.java
│   ├── CartesModels.java
│   └── RetrofitClient.java
└── Database.java
```

## 🧪 Testing
- Unit тестове за Repository класове
- Integration тестове за Database операции
- UI тестове за основните flows

## 🚀 Build & Run

```bash
# Sync dependencies
./gradlew build --refresh-dependencies

# Run on emulator/device
./gradlew installDebug

# Run tests
./gradlew test
```

## 🔮 Бъдещи подобрения
- [ ] OAuth 2.0 интеграция (Google, Facebook)
- [ ] Push нотификации за нови пивоварни
- [ ] Social feed за споделяне на check-ins
- [ ] Google Maps интеграция
- [ ] Export на данни
- [ ] Dark/Light theme toggle
- [ ] Multi-language support

## 👨‍💻 Автор
Проект разработен за курс по Mobile Development

## 📄 Лиценз
MIT License
# 🍺 AleTrail - Система за управление на виртуални карти за пивоварни

## 📋 Описание
AleTrail е модерно Android приложение за създаване и управление на виртуални карти за лоялни клиенти на пивоварни. Приложението интегрира геолокация, QR код сканиране, gamification система с badges, и синхронизация на данни.

## ✨ Основни функционалности

### 1. Основни функции
- ✅ Регистрация и автентикация на потребители
- ✅ Търсене и преглед на пивоварни в близост (GPS)
- ✅ Създаване на виртуална карта за избрани пивоварни
- ✅ Визуализация на картата с брой печати/точки
- ✅ Генериране на уникален QR код за всяка карта

### 2. Система за печати
- ✅ Добавяне на печат при сканиране от страна на пивоварната
- ✅ Проследяване на историята на посещенията
- ✅ Прогрес бар с визуализация (⭐ емоджита)

### 3. Споделяне и комуникация
- ✅ Генериране на споделяем линк за картата
- ✅ Възможност за изпращане на картата чрез Share Intent
- ✅ Интеграция с Cartes.io за споделяне на карти

### 4. Съхранение на данни
- ✅ Локално съхранение чрез Room Database (офлайн достъп)
- ✅ Автоматична синхронизация при наличие на интернет
- ✅ 6 таблици: Breweries, LoyaltyCards, Users, Visits, Badges, BeerRatings

### 5. Допълнителни функции
- ✅ **Gamification**: 8 типа badges (Bronze, Silver, Gold, Platinum, Explorer, etc.)
- ✅ **Beer Rating система**: Рейтинг на бири с коментари
- ✅ **Геолокация**: Намиране на близки пивоварни
- ✅ **QR код**: Генериране и сканиране на QR кодове
- ✅ **Офлайн режим**: Пълна функционалност без интернет

## 🏗️ Архитектура

