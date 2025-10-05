# Xend

An AI-powered email drafting assistant that generates **polite, relationship-aware** emails with **streaming drafts**, **saved templates**, and **inline reply options**. Designed to reduce editing time while keeping tone appropriate for each recipient.



## Features

- **Streaming Draft Generation**: First sentence appears quickly; content streams until completion.
- **Relationship-Aware Tone**: Select **Group** (e.g., Manager/Clients) and **Tone** (e.g., Polite/Formal) and see rules applied.
- **Saved Templates**: Apply a frequently used template (e.g., “Weekly Report”) to prefill structure.
- **Inline Reply Options**: Positive / Neutral / Firm candidates shown inline; insert with a tap.
- **Attachment-Aware Reply (mocked)**: If a PDF is attached, the reply references summarized key points.

---

## Getting Started

### Prerequisites

- Android Studio [version, e.g., 4.2.1]
- Minimum Android SDK Version [e.g., 21]

### Installation

### Prerequisites
- **Docker** & **Docker Compose**
- **Android Studio**: Iguana or newer, **JDK 17**
- **Android SDK**: minSdk 24+

### Installation

> **Branch**: `iteration-1-demo`

```
git clone https://github.com/snuhcs-course/swpp-2025-project-team-11.git
cd swpp-2025-project-team-11
git switch iteration-1-demo
cp .env.example .env   # keep defaults to use mocked external services for the demo
```

Backend (Docker + Poetry inside containers)
Backend runs via Docker Compose (includes nginx).

# Build & start
```
docker compose up -d --build
```


# Stop
```
docker compose down
```

Default URL: http://localhost/

Android App
Open /frontend in Android Studio

Sync Gradle

Select an emulator/device (e.g., Pixel 7, API 34)

Run the app (Debug)

## What This Demo Demonstrates
- **Google Sign-In**  
  Sign in with Google to authenticate the user.

- **AI Email Generation**  
  Generate an email draft using the AI endpoint.

- **Send Email**  
  Send the composed AI draft through the backend to the mail provider.

Demo Video
Link: https://drive.google.com/file/d/1to4deCD6jBssx2V_oZgZU5E5Os2PlfP1/view?usp=sharing
