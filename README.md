# BBC Надеждност — SRGM Tool

**BBC Надеждност** е Android приложение за анализ на надеждността на софтуер чрез **Software Reliability Growth Models (SRGM)**. Инструментът зарежда исторически данни за грешки, прилага пет математически модела чрез MLE оптимизация и генерира визуални отчети за прогнозиране на бъдещи откази.

---

## Характеристики

- **5 SRGM модела** — едновременно изчисляване и сравнение:
  - Goel-Okumoto (GO)
  - Jelinski-Moranda (JM)
  - Yamada Exponential
  - Delayed S-Shaped
  - Schick-Wolverton (SW)
- **MLE параметрична оценка** чрез Levenberg-Marquardt нелинеен оптимизатор
- **Метрики за качество**: R², AIC, BIC за обективен избор на модел
- **Интерактивна графика** — наблюдавани vs прогнозирани откази
- **Времеви филтър** — slider и текстово поле за прецизен анализ на период
- **Scorecard за сравнение** на всички модели с визуален ранкинг
- **Детайли за параметрите** — α (очакван брой откази), β (скорост на откриване)
- **Автоматизирано резюме** — текстов анализ генериран от модела
- **PDF експорт** — структуриран отчет, готов за споделяне
- **Responsive layout** — адаптиран за телефони и таблети (Compact / Medium / Large / XL)
- **Анимиран фон** с математически символи (λ, Σ, ∫, μ, σ, π…)

---

## Поддържани SRGM модели

| Модел | Формула μ(t) | Параметри |
|-------|-------------|-----------|
| Goel-Okumoto | `α(1 - e^{-βt})` | α, β |
| Jelinski-Moranda | `α(1 - e^{-βt})` | α, β |
| Yamada Exponential | `α(1 - (1 + βt)e^{-βt})` | α, β |
| Delayed S-Shaped | `α(1 - (1 + βt)e^{-βt})` | α, β |
| Schick-Wolverton | `α(1 - e^{-βt^2})` | α, β |

---

## Технически стек

| Компонент | Технология |
|-----------|-----------|
| Език | Kotlin 2.2 |
| UI Framework | Jetpack Compose + Material3 |
| Архитектура | Custom MVVM с `StateFlow` |
| Математика | Apache Commons Math3 3.6.1 (LM оптимизатор) |
| CSV парсинг | Apache Commons CSV 1.11 |
| PDF генерация | Android `PdfDocument` API |
| Min SDK | Android 7.0 (API 24) |
| Target SDK | Android 15 (API 35) |
| Build система | Gradle 9.3 (Kotlin DSL) |

---

## Формат на входните данни

Приложението приема CSV файл с два задължителни колони:

```csv
Time,Failures
1,2
2,5
3,9
...
```

- `Time` — единица за измерване на времето (CPU hours, тест цикли, дни и др.)
- `Failures` — кумулативен брой открити грешки до момента

По подразбиране е наличен бутон за зареждане на публичния **T39 dataset** от Derek Jones' Reliability-data хранилище.

---

## Инсталация и стартиране

### Изисквания

- Android Studio Ladybug или по-нова версия
- JDK 11+
- Android устройство или емулатор с API 24+

### Стъпки

```bash
git clone https://github.com/tankovatanas/SRGM-Tool.git
cd SRGM-Tool
```

Отворете проекта в Android Studio и стартирайте конфигурацията `app`.

---

## Как се използва

1. **Зареди данни** — натиснете "Зареди T39 от GitHub" или подайте собствен CSV
2. **Стартирай анализ** — приложението изчислява всичките 5 модела едновременно
3. **Сравни моделите** — прегледайте scorecard-а с AIC/BIC ранкинг
4. **Филтрирай периода** — използвайте slider за анализ на конкретен времеви прозорец
5. **Избери модел** — натиснете желания модел от scorecard-а за детайлна визуализация
6. **Експортирай** — генерирайте PDF отчет готов за споделяне

---

## Структура на проекта

```
src/main/kotlin/com/example/bbc/
├── domain/
│   └── Models.kt           # Домейн модели: ReliabilityTestRecord, ModelParameters, SRGMType
├── logic/
│   ├── SRGMEngines.kt      # 5 SRGM имплементации + BaseMLEEngine
│   ├── ReportGenerator.kt  # Генериране на текстово резюме
│   └── PdfExporter.kt      # PDF експорт чрез Android PdfDocument
├── ui/
│   ├── components/         # Compose компоненти (BentoBox, Scorecard, ReliabilityGraph…)
│   ├── theme/              # Material3 тема
│   └── viewmodel/
│       └── BBCViewModel.kt # Централен ViewModel с StateFlow
├── App.kt                  # Главен Composable + responsive layout логика
└── MainActivity.kt
```

---

## Автор

**Atanas Tankov** — [tankovatanas](https://github.com/tankovatanas)
