# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Xend** is an Android email client application with AI-powered email drafting assistance. Built with Kotlin and Jetpack Compose, following modern Android architecture patterns (MVVM + Repository pattern).

## Development Commands

### Building and Running
```bash
./gradlew build                    # Build the entire project
./gradlew :app:assembleDebug       # Build debug APK
./gradlew :app:compileDebugKotlin  # Compile Kotlin sources only
```

### Testing
```bash
./gradlew test                           # Run all unit tests
./gradlew testDebugUnitTest             # Run debug unit tests only
./gradlew :app:testDebugUnitTest        # Run app module unit tests
./gradlew connectedDebugAndroidTest     # Run instrumentation tests on connected device
```

### Code Quality
```bash
./gradlew ktlintCheck              # Check Kotlin code style
./gradlew ktlintFormat             # Auto-format Kotlin code
./gradlew lint                     # Run Android lint
./gradlew lintFix                  # Apply safe lint fixes
./gradlew check                    # Run all checks (tests + lint + ktlint)
```

### Pre-commit Hooks
This project uses pre-commit hooks that automatically run ktlint formatting. If hooks fail:
1. Check the error message for specific violations
2. Ktlint enforces 120-character line limit - break long lines into multiple statements
3. Modified files will be auto-formatted if possible; review changes before committing

## Architecture

### Overall Pattern: MVVM + Repository + Unidirectional Data Flow

```
UI (Compose) → ViewModel (StateFlow) → Repository → Data Sources (API/DB)
```

### Key Layers

1. **UI Layer** (`ui/` package)
   - Jetpack Compose screens and components
   - ViewModels manage UI state using `StateFlow` and `MutableStateFlow`
   - Unidirectional data flow: UI emits events → ViewModel updates state → UI reacts to state changes

2. **Data Layer** (`data/` package)
   - **Repository Pattern**: Single source of truth, coordinates between network and local storage
   - **Model**: Data classes for domain objects
   - **Source**: API services and local data sources (Room, DataStore)

3. **Network Layer** (`network/` package)
   - `RetrofitClient`: Centralized HTTP client configuration with custom DNS fallback (Google DNS)
   - **Two OkHttpClient configurations**:
     - `getClient()`: Standard REST API with automatic Bearer token injection via interceptor
     - `getWebSocketClient()`: WebSocket-specific client WITHOUT interceptor (tokens manually added)
   - `TokenRefreshAuthenticator`: Automatic JWT token refresh on 401 responses
   - **Real-time Communication**:
     - `MailReplySseClient`: Server-Sent Events for AI email reply suggestions
     - `MailComposeWebSocketClient`: WebSocket for AI-powered email composition with streaming responses

### Authentication & Token Management

- **OAuth2 + JWT**: Google Sign-In with JWT access/refresh tokens
- **TokenManager** (`data/source/TokenManager.kt`):
  - Stores tokens in `EncryptedSharedPreferences` (secure)
  - Provides token retrieval for API calls
  - Handles token lifecycle
- **Automatic Token Refresh**:
  - `TokenRefreshAuthenticator` intercepts 401 responses
  - Automatically refreshes access token using refresh token
  - Retries original request with new token
  - WebSocket clients handle token refresh independently with retry logic

### Database (Room)

- **Local Email Caching**: `MailDatabase` stores emails for offline access and pagination
- **Entities**: `EmailEntity`, `ContactEntity`, `GroupEntity`
- **DAOs**: Type-safe database access with Flow-based queries for reactive updates
- Location: `data/source/local/`

### Real-time AI Features

#### SSE (Server-Sent Events) - Email Reply Suggestions
- **File**: `network/MailReplySseClient.kt`
- **Purpose**: Stream AI-generated email reply suggestions in real-time
- **Pattern**:
  - Opens SSE connection with Bearer token
  - Sends original email context
  - Receives streamed response chunks
  - Handles token expiration with automatic reconnection

#### WebSocket - Email Composition Assistant
- **File**: `network/MailComposeWebSocketClient.kt`
- **Purpose**: Interactive AI assistance during email composition
- **Pattern**:
  - Bidirectional communication (send draft text, receive AI suggestions)
  - Custom JWT middleware authentication (Bearer token in Authorization header)
  - State management with `AtomicBoolean` for connection lifecycle
  - Automatic token refresh and reconnection on auth failures (401, 1008 close codes)
  - JSON message format: `{"system_prompt": "...", "text": "...", "max_tokens": 50}`

### Testing Strategy

- **Unit Tests**:
  - MockK for mocking dependencies
  - Turbine for testing Flow emissions
  - Kotlinx Coroutines Test for coroutine testing
  - Robolectric for Android framework simulation
  - Location: `app/src/test/`

- **Instrumentation Tests**:
  - Espresso for UI testing
  - MockK Android for Android-specific mocking
  - Location: `app/src/androidTest/`

### Configuration

**Required Setup**: `local.properties` file must contain:
```properties
base_url=https://your-api-server.com/
sse.url=https://your-api-server.com/sse/endpoint
send.url=https://your-api-server.com/send/endpoint
ws.url=wss://your-api-server.com/ws/endpoint
```

These are injected as `BuildConfig` fields at compile time.

## Important Implementation Notes

### Network Layer Patterns

1. **Token Authentication**:
   - REST API calls: Token automatically added by interceptor in `getClient()`
   - WebSocket connections: Token manually added in request builder (no interceptor)
   - Always use `"Bearer $accessToken"` format for Authorization header

2. **Error Handling**:
   - 401 Unauthorized → Automatic token refresh via `TokenRefreshAuthenticator`
   - Network failures → Custom DNS fallback to Google DNS (8.8.8.8)
   - WebSocket errors → Automatic retry with token refresh on auth failures

3. **WebSocket Connection Lifecycle**:
   - Use `AtomicBoolean` flags to prevent race conditions (`isConnecting`, `isConnected`, `isRetrying`)
   - Always check connection state before sending messages
   - Clean up callbacks on disconnect to prevent memory leaks
   - Handle multiple failure scenarios: onFailure (network), onClosing (policy violation 1008), onMessage (token_invalid)

### ViewModel State Management

- Use `StateFlow` for exposing state to UI (read-only)
- Use `MutableStateFlow` internally for state updates
- Emit loading/success/error states through sealed classes or data class with state fields
- Launch coroutines in `viewModelScope` for automatic cancellation

### Code Style

- **Ktlint enforced**: 120-character line limit strictly enforced
- **Long strings**: Break into variables or use multi-line string templates
- **Example fix pattern**:
  ```kotlin
  // ❌ Bad - exceeds 120 chars
  Log.d(TAG, "Access token retrieved: ${if (token.isNullOrEmpty()) "EMPTY" else token.take(30) + "..."}")

  // ✅ Good - use intermediate variable
  val tokenPreview = if (token.isNullOrEmpty()) "EMPTY" else token.take(30) + "..."
  Log.d(TAG, "Access token retrieved: $tokenPreview")
  ```
  주석은 간단하고 담백하게 작성하기.

## Project Structure

- `app/src/main/java/com/fiveis/xend/`
  - `ui/` - Compose UI, screens, ViewModels
  - `data/` - Models, repositories, data sources
  - `network/` - Retrofit, OkHttp, SSE, WebSocket clients
  - `MainActivity.kt` - App entry point with navigation setup

- `app/src/test/` - Unit tests
- `app/src/androidTest/` - Instrumentation tests
