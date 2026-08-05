# ScrapChef

ScrapChef is a native Android application built with Jetpack Compose that helps you reduce food waste by generating delicious recipes from the ingredients you already have in your kitchen.

## Features

*   **Pantry Management:** Keep track of the ingredients you have on hand. Add, edit, and organize your pantry easily.
*   **AI-Powered Recipe Generation:** Input your available ingredients and ScrapChef will generate creative, tailored recipes instantly.
*   **Streaming Recipe UI:** Watch your recipe build in real-time as it's generated, giving you immediate feedback.
*   **Kitchen Mode:** A distraction-free, step-by-step cooking interface designed specifically for use while cooking.
*   **Favorites:** Save the recipes you love for quick access later.
*   **Dietary Preferences:** Automatically filter generated recipes based on your specific dietary needs (e.g., Vegetarian, Vegan, Gluten-Free).

## Architecture & Tech Stack

ScrapChef is built using modern Android development practices and a robust MVVM architecture:

*   **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) for a fully declarative and responsive UI.
*   **Language:** 100% [Kotlin](https://kotlinlang.org/).
*   **Architecture:** MVVM (Model-View-ViewModel) promoting separation of concerns.
*   **Persistence:** 
    *   [Room Database](https://developer.android.com/training/data-storage/room) for local caching of ingredients, pantry items, and favorite recipes.
    *   [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) for user settings and preferences.
*   **Networking:** 
    *   [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/) for API communication.
    *   Server-Sent Events (SSE) via `okhttp-sse` for real-time recipe streaming.
*   **Asynchrony:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlin.lang.org/docs/flow.html).
*   **Testing:** Unit tests and robust Robolectric/Espresso configurations for UI testing.

## Project Structure

```
scrapchef/
├── app/
│   ├── src/main/java/com/scrapchef/android/
│   │   ├── data/       # Repositories, Room DAOs, API services, and Models
│   │   ├── ui/         # Jetpack Compose screens, components, and ViewModels
│   │   └── MainActivity.kt
│   └── src/test/       # Unit tests
└── docs/               # Engineering specifications, ADRs, and checklists
```

## Getting Started

### Prerequisites
*   Android Studio (latest version recommended)
*   An active API key for the underlying recipe generation engine.

### Setup Instructions

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/scrapchef.git
    cd scrapchef
    ```

2.  **Configure API Keys:**
    *   Create a `.env` file in the root project directory (use `.env.example` as a template).
    *   Add your API key:
        ```properties
        OPENROUTER_API_KEY="your_api_key_here"
        ```

3.  **Build and Run:**
    *   Open the project in Android Studio.
    *   Sync Gradle files.
    *   Run the app on a connected physical device or emulator (API 24+).

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions, issues, and feature requests are welcome! Please feel free to check the issues page.

---
*Cook smarter, waste less.*
