# Внутренние инструменты Zephyra

## ZephyrCLI

ZephyrCLI — это консольный инструмент для разработчиков Zephyra. Устанавливается автоматически при онбординге. Текущая версия: 5.2.1.

### Основные команды

```
zephyr-cli nimbus start --local        # Запуск Нимбус локально в Docker
zephyr-cli nimbus stop                  # Остановка
zephyr-cli nimbus logs --service=NAME   # Логи конкретного микросервиса
zephyr-cli nimbus status                # Статус всех сервисов

zephyr-cli sensor add --virtual --type=temperature  # Добавить виртуальный датчик
zephyr-cli sensor list                              # Список подключённых датчиков
zephyr-cli sensor stream SENSOR_ID                  # Стрим данных с датчика

zephyr-cli drone simulate --count=10   # Запустить 10 виртуальных дронов
zephyr-cli drone status                # Статус дронов

zephyr-cli db migrate                  # Запуск миграций CrystalDB
zephyr-cli db seed --dataset=demo      # Заполнить БД тестовыми данными
zephyr-cli db backup                   # Бэкап базы

zephyr-cli deploy staging              # Деплой на staging-окружение «Туман»
zephyr-cli deploy production           # Деплой на прод (требует подтверждение от Z6+)
```

### Конфигурация

Файл конфигурации: `~/.zephyra/config.yaml`

```yaml
cluster: crystallograd-1
environment: development
vpn:
  server: vpn.zephyra.internal
  port: 7777
  protocol: ZephyrVPN
registry:
  url: registry.zephyra.internal
  auth: token
```

## Система тикетов «Ветерок»

«Ветерок» — внутренняя система управления IT-запросами, разработанная на базе Jira с кастомными плагинами.

### Категории тикетов

- **Порыв** (P1) — критическая проблема, блокирующая работу. SLA: 1 час
- **Шквал** (P2) — серьёзная проблема, есть workaround. SLA: 4 часа
- **Ветерок** (P3) — обычный запрос. SLA: 1 рабочий день
- **Штиль** (P4) — улучшение, не срочно. SLA: 5 рабочих дней

### Как создать тикет

1. Зайти на tickets.zephyra.internal
2. Нажать «Новый порыв ветра»
3. Выбрать категорию и приоритет
4. Описать проблему (шаблон: Что случилось → Что ожидалось → Шаги воспроизведения)
5. Прикрепить скриншоты или логи
6. Назначить на нужную команду (или оставить «Авторазнос» — система сама определит)

## VPN «Туннель»

Для удалённой работы используется VPN «Туннель» на базе протокола ZephyrVPN (форк WireGuard с двойным шифрованием).

### Подключение

```bash
zephyr-cli vpn connect
# или
zephyr-cli vpn connect --server=crystallograd-2  # резервный сервер
```

### Доступные серверы

| Сервер | Расположение | Для чего |
|--------|-------------|----------|
| crystallograd-1 | Кристаллоград, основной ДЦ | Повседневная работа |
| crystallograd-2 | Кристаллоград, резервный ДЦ | При падении основного |
| aurora-1 | Аврорабург (партнёрский город) | Для команды робототехники |

### Правила VPN

- VPN обязателен для доступа к внутренним сервисам (.zephyra.internal)
- Сессия живёт 12 часов, потом нужно переподключиться
- Максимум 2 активных сессии на одного сотрудника
- При 3 неудачных попытках подключения аккаунт блокируется на 30 минут

## Мониторинг — «Маяк»

«Маяк» — система мониторинга на базе Grafana + Prometheus с кастомными дашбордами.

### Основные дашборды

- **Прогноз погоды** — общее здоровье системы Нимбус (latency, error rate, throughput)
- **Карта ветров** — трафик между микросервисами
- **Барометр** — нагрузка на CrystalDB
- **Роза ветров** — статус парка дронов Пчёлка-7
- **Атмосферное давление** — бизнес-метрики (активные теплицы, доход)

### Алерты

Алерты настраиваются через YAML-файлы в репозитории `infra/alerts`:

```yaml
- name: high_sensor_latency
  condition: avg(sensor_response_time) > 500ms for 5m
  severity: storm
  notify:
    - slack: #incidents
    - pagerduty: on-call-nimbus
  runbook: wiki.zephyra.internal/runbooks/sensor-latency
```

### Дежурства

Дежурство (on-call) длится 1 неделю. Ротация — по расписанию в системе «Маяк». За неделю дежурства начисляется бонус 15 000 зефиров. Если за время дежурства произошёл инцидент уровня «Торнадо» — бонус удваивается.

## Внутренние библиотеки

### zephyr-commons

Общая библиотека для всех микросервисов Нимбус:
- `ZephyrLogger` — логгер с автоматической отправкой в систему «Маяк»
- `ZephyrConfig` — управление конфигурацией
- `ZephyrAuth` — аутентификация через ZephyraToken
- `ZephyrMetrics` — сбор метрик для Prometheus

### zephyr-testing

Библиотека для тестирования:
- `GreenhouseSimulator` — эмулятор теплицы с виртуальными датчиками
- `DroneTestHarness` — эмулятор парка дронов
- `TimeWarp` — управление временем в тестах (ускорение/замедление)
- `CrystalDBTestContainer` — тестовый контейнер CrystalDB

### zephyr-drone-sdk

SDK для разработки прошивки дронов Пчёлка-7:
- `FlightController` — управление полётом
- `PollinationModule` — алгоритм опыления
- `CollisionAvoidance` — избежание столкновений (алгоритм «Термик»)
- `HiveNavigator` — навигация к зарядной станции «Улей»
