# Project Plan

Native Android Weather Application (MacóMet Weather App) in Kotlin with Jetpack Compose.
Key features:
1. Hungarian UI & Localization (Magyar nyelvű kezelőfelület és adatok).
2. Location-based weather tracking using GPS and network location (Fused Location Provider).
3. Open-Meteo API integration for weather models and forecasts.
4. Real-time current weather conditions ("Jelenidő") based on weighted calculations from nearby official and amateur weather stations.
5. 7-day detailed weather forecast (temperature, precipitation, wind, weather conditions).
6. Modular architecture (MVVM, Clean Architecture, Hilt) allowing easy expansion for future modules.

## Project Brief

# MacóMet Weather App - Projekt Összefoglaló (Project Brief)

## Features

1. **Magyar nyelvű kezelőfelület és lokalizáció (Hungarian UI & Localization)**
   - Teljesen magyar nyelvű felhasználói felület, magyar időjárási szakkifejezésekkel, szabványos hazai mértékegységekkel (°C, km/h, mm) és magyar nyelvű kódkommentekkel/címkékkel.
2. **Helymeghatározás alapú időjárás-követés (Location-based Weather Tracking)**
   - Helyalapú időjárás-adatok automatikus frissítése GPS és hálózati pozíció alapján (Fused Location Provider).
3. **Valós idejű "Jelenidő" kijelzés (Real-time Current Weather)**
   - Az aktuális hőmérséklet, hőérzet, szélsebesség, páratartalom és csapadékviszonyok megjelenítése a legközelebbi mérőállomások és az Open-Meteo modell adatai alapján.
4. **7 napos előrejelzés (7-Day Forecast)**
   - Napi és órás bontású részletes előrejelzés hőmérsékleti csúcsokkal, csapadékvalószínűséggel, széladatokkal és vizuális időjárás-ikonokkal.

## High-Level Tech Stack

- **Nyelv (Language):** Kotlin
- **UI Keretrendszer:** Jetpack Compose (Material 3)
- **Navigáció:** Jetpack Navigation 3 (állapotvezérelt / state-driven navigáció)
- **Adaptív elrendezés:** Compose Material Adaptive library
- **Architektúra:** Clean Architecture + MVVM, Hilt dependency injection
- **Aszinkron modulok:** Kotlin Coroutines & Flow
- **Hálózati kommunikáció:** Retrofit (Open-Meteo API integráció)
- **Helymeghatározás:** Fused Location Provider API (Google Play Services)

## Implementation Steps
**Total Duration:** 1h 7m 26s

### Task_1_DataLayerAndLocation: Setup Data Layer, Open-Meteo API service, weather data models/mappers, and FusedLocationProvider for location tracking.
- **Status:** COMPLETED
- **Updates:** Task_1_DataLayerAndLocation completed successfully:
- **Acceptance Criteria:**
  - Open-Meteo Retrofit API service and repository implemented
  - FusedLocationProvider integration ready with Budapest fallback location
  - Data classes for Current Weather ('Jelenidő') and 7-Day Forecast defined
  - build pass
- **Duration:** 3m 38s

### Task_2_ViewModelAndDomain: Implement WeatherViewModel, Hungarian weather code mappings, metric unit formatting (°C, km/h, mm), and UI state handling.
- **Status:** COMPLETED
- **Updates:** Task_2_ViewModelAndDomain completed successfully:
- **Acceptance Criteria:**
  - WeatherViewModel manages UI state for current weather, forecast, loading, and error states
  - Weather codes mapped to Hungarian descriptions (e.g. Napos, Esős, Zivatar)
  - Location updates correctly trigger weather data refresh
  - build pass
- **Duration:** 2m 29s

### Task_3_ComposeUI: Create Jetpack Compose UI screens with Material Design 3 in full Hungarian localization.
- **Status:** COMPLETED
- **Updates:** Task_3_ComposeUI completed successfully:
- **Acceptance Criteria:**
  - Hungarian UI implemented with Jetpack Compose Material 3
  - 'Jelenidő' current weather card displaying temperature, feels-like, wind, humidity, precipitation
  - 7-day and hourly forecast UI components displayed with Hungarian labels and icons
  - Location permission request handling and retry functionality
  - build pass
- **Duration:** 5m 5s

### Task_4_RunAndVerify: Run and verify the complete MacóMet Weather application stability, Hungarian localization, and UI functionality.
- **Status:** COMPLETED
- **Updates:** Task_4_RunAndVerify completed successfully:
- **Acceptance Criteria:**
  - make sure all existing tests pass
  - build pass
  - app does not crash
  - critic_agent verifies application stability, confirms alignment with Hungarian weather app requirements, and reports critical UI issues
- **Duration:** 2m 54s

### Task_5_StationAPIsAndWeightedJelenido: Integrate OpenSenseMap API, NOAA/WMO SYNOP API, and Netatmo API with API_KEY support. Implement nearby station fetching, distance calculation, and weighted 'Jelenidő' algorithm in domain/repository layers.
- **Status:** COMPLETED
- **Updates:** Task_5_StationAPIsAndWeightedJelenido completed successfully:
- **Acceptance Criteria:**
  - OpenSenseMap API, NOAA/WMO SYNOP API, and Netatmo API integrated
  - Netatmo API_KEY integration configured and functional
  - Nearby station fetching and distance calculation based on current location
  - Weighted Jelenidő calculation algorithm combining station measurements and Open-Meteo model
  - build pass
- **Duration:** 6m 17s

### Task_6_StationListUIAndRunVerify: Build Jetpack Compose UI for nearby station list displaying distance, type, and measurements, update main screen Jelenidő display, and verify overall app stability.
- **Status:** COMPLETED
- **Updates:** Task_6_StationListUIAndRunVerify completed successfully:
- **Acceptance Criteria:**
  - Nearby station list UI component displaying station details, distance, and live measurements in Hungarian
  - Weighted Jelenidő integrated seamlessly into the main UI
  - make sure all existing tests pass
  - build pass
  - app does not crash
  - critic_agent verifies application stability, confirms alignment with user requirements, and reports critical UI issues
- **Duration:** 9m 56s

### Task_7_CitySearchFeature: Integrate Open-Meteo Geocoding API (v1/search), implement city search repository and UI search bar/dialog with result list (city, country, region), and handle location switching to city or back to GPS.
- **Status:** COMPLETED
- **Updates:** Task_7_CitySearchFeature completed successfully:
- **Acceptance Criteria:**
  - Open-Meteo Geocoding Retrofit API service and repository implemented
  - Search UI component integrated in app header with city search bar/dialog and results list
  - Selecting a city updates weather forecast and nearby station data for chosen location
  - Option to switch back to GPS position ('GPS pozíció használata') functional
  - build pass
- **Duration:** 8m 37s

### Task_8_RunAndVerify: Run and verify application stability, city search feature, weather data update upon city selection, GPS fallback, and Hungarian localization.
- **Status:** COMPLETED
- **Updates:** Task_8_RunAndVerify completed successfully:
- **Acceptance Criteria:**
  - make sure all existing tests pass
  - build pass
  - app does not crash
  - critic_agent verifies application stability, confirms alignment with user requirements, and reports critical UI issues
- **Duration:** 5m 22s

### Task_9_WorldwideBoundingBoxAndStationNaming: Refactor station parsing to extract exact Netatmo station/module names, NOAA airport IDs, and street locations. Implement dynamic 50km bounding box scaling for worldwide station fetching.
- **Status:** COMPLETED
- **Updates:** Task_9_WorldwideBoundingBoxAndStationNaming completed successfully:
- **Acceptance Criteria:**
  - Dynamic 50km bounding box calculates correctly for any global coordinate
  - Netatmo parsing accurately retrieves module/station names instead of generic names
  - NOAA and OpenSenseMap parsing retrieves exact readable identifiers (e.g., airport IDs, street locations)
  - build pass
- **Duration:** 3m 15s

### Task_10_StationNameUIAndRunVerify: Update nearby station list UI to display exact station names instead of generic fallbacks. Run and verify application stability, global station fetching, and exact name display.
- **Status:** COMPLETED
- **Updates:** Task_10_StationNameUIAndRunVerify completed successfully:
- **Acceptance Criteria:**
  - Station list UI shows exact Netatmo module names, NOAA airport IDs, or street locations
  - Worldwide station fetching operates successfully without hardcoded city restrictions
  - make sure all existing tests pass
  - build pass
  - app does not crash
  - critic_agent verifies application stability, confirms alignment with user requirements, and reports critical UI issues
- **Duration:** 10m 53s

### Task_11_RemoveNetatmoBackend: Remove Netatmo API Retrofit services, token managers, data models, and exclude Netatmo from station fetching and weighted 'Jelenidő' calculations.
- **Status:** COMPLETED
- **Updates:** Task_11_RemoveNetatmoBackend completed successfully:
- **Acceptance Criteria:**
  - Netatmo API endpoints, token managers, and auth logic deleted
  - Domain and repository layers only fetch from OpenSenseMap and NOAA
  - build pass
- **Duration:** 4m 21s

### Task_12_RemoveNetatmoUIAndVerify: Remove all UI references to Netatmo stations. Run and verify that the application correctly displays weather data using only NOAA and OpenSenseMap.
- **Status:** COMPLETED
- **Updates:** Task_12_RemoveNetatmoUIAndVerify completed successfully:
- Netatmo has been completely removed from both UI and backend.
- Critic agent verified that NOAA (e.g., LHBP/Budapest) and OpenSenseMap stations are fetched and display correctly in 'Közeli mérőállomások'.
- Verified that the fallback generic station ('Körzeti PWS Észlelő / Fúziós Állomás') successfully appears when no stations are nearby (e.g. Békéscsaba) without crashing.
- Build and tests pass. No UI traces of Netatmo remain. App layout and edge-to-edge support verified as stable.
- **Acceptance Criteria:**
  - Netatmo specific UI elements or icons are completely removed
  - make sure all existing tests pass
  - build pass
  - app does not crash
  - critic_agent verifies application stability, confirms alignment with user requirements, and reports critical UI issues
- **Duration:** 4m 39s

