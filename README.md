# SIGEON Calendar (Расписание колледжа & Умный органайзер)

Современное Android-приложение на **Kotlin** и **Jetpack Compose** с поддержкой расписания колледжа через **Planovo** (.ics), офлайн-хранилищем на **Room**, виджетом для рабочего стола и синхронизацией с **Google Календарем**.

---

## 🚀 Как открыть проект в Android Studio

1. **Скачайте или клонируйте проект** в любую удобную папку на компьютере.
2. Откройте **Android Studio** (рекомендуются версии Ladybug, Hedgehog, Iguana, Koala, Meerkat или новее).
3. В стартовом окне выберите **Open** (или в меню `File -> Open...`).
4. Укажите **корневую директорию** проекта (где расположены файлы `settings.gradle.kts` и `build.gradle.kts`).
5. Дождитесь автоматической синхронизации **Gradle Sync**:
   - В проект включен готовый **Gradle Wrapper** (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`).
   - Рекомендуемая версия **JDK**: Java 17 или Java 21 (проверьте в `Settings / Preferences -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JDK`).
6. Нажмите кнопку **Run (Shift+F10)** для запуска на эмуляторе или физическом устройстве (требуется Android 7.0+ / API 24+).

---

## 🛠️ Стек технологий и архитектура

- **UI / Язык:** Kotlin 2.2, Jetpack Compose, Material Design 3 (M3).
- **Архитектура:** Clean Architecture / MVVM (`MainViewModel`, `CalendarRepository`, Reactive Flow/StateFlow).
- **База данных:** Room 2.7 с локальным кэшированием занятий, событий и заметок (полный Offline-First).
- **Сеть:** OkHttp, парсер RFC 5545 iCalendar (`IcsCalendarParser`), интеграция с Planovo API (`https://planovo.pro/api/v1/public/...`).
- **Синхронизация:** Google Calendar ContentProvider API, JSON экспорт/импорт резервных копий.
- **Виджет:** Android AppWidgetProvider (`CollegeScheduleWidgetProvider`) с отображением текущей и следующей пары в реальном времени.
- **Анимации:** Spring-анимации, плавные переходы между вкладками (`AnimatedContent`), пульсирующий статус «Идет сейчас», тактильный отклик кнопок.

---

## 📁 Структура проекта

```
app/src/main/java/com/example/
├── MainActivity.kt                      # Корневой экран с AnimatedContent и BackHandler
├── data/
│   ├── api/                             # Парсинг ICS, каталог групп Planovo, сетевой клиент
│   │   ├── IcsCalendarParser.kt         # Парсер RFC 5545 iCalendar (.ics)
│   │   ├── PlanovoGroup.kt              # Модель и справочник 59 групп
│   │   ├── CollegeScheduleFetcher.kt    # Сетевой загрузчик расписания
│   │   └── CollegeScheduleModels.kt     # Модели пар и занятий
│   ├── local/                           # Room Database, DAO и сущности (Event, Note)
│   ├── repository/                      # CalendarRepository (источник истины)
│   └── sync/                            # Google Calendar Sync, Backup, ConnectivityObserver
├── ui/
│   ├── college/                         # Экран расписания, выбор группы, диалоги
│   │   ├── CollegeScheduleScreen.kt     # Экран расписания колледжа
│   │   ├── GroupSelectorBottomSheet.kt  # Нижняя шторка поиска и выбора групп
│   │   ├── EditCollegeClassDialog.kt    # Редактирование пары
│   │   └── CollegeApiConfigDialog.kt    # Настройка API и ссылок
│   ├── calendar/                        # Календарная сетка, события и расписание
│   ├── notes/                           # Интерактивные заметки и чеклисты
│   ├── settings/                        # Настройки синхронизации и бэкап
│   ├── components/                      # Нижняя панель навигации, анимации
│   └── theme/                           # Цветовая палитра M3, типографика
└── widget/                              # Виджет рабочего стола Android
```
