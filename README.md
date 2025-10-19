# Xend

<img width="244" height="111" alt="xend" src="https://github.com/user-attachments/assets/7773cde3-c7bd-4d20-ad35-f7b91de14016" />

An AI-powered email drafting assistant that generates **polite, relationship-aware** emails with **streaming drafts**, **saved templates**, and **inline reply options**. Designed to reduce editing time while keeping tone appropriate for each recipient.


<img width="360" height="800" alt="Xend Login Screen" src="https://github.com/user-attachments/assets/c10b6b50-188d-4e7e-8863-18bc2bc34250" />


<img width="360" height="800" alt="Xend Mail List Screen" src="https://github.com/user-attachments/assets/00d9acb1-83f5-434d-b3ce-bd09d726a4bc" />

<img width="360" height="800" alt="Xend Compose with Keyboard" src="https://github.com/user-attachments/assets/1b4a1d89-be1a-4b67-9008-3c77b23afacb" />

<img width="360" height="800" alt="Xend Contacts Screen" src="https://github.com/user-attachments/assets/67107285-70b0-4632-b1ac-ca94cf1a368c" />

<img width="360" height="800" alt="Xend Improved Reply Options" src="https://github.com/user-attachments/assets/11e4ce38-7a7e-416e-b4e6-8321fff47fec" />


## Features

- **Streaming Draft Generation**: First sentence appears quickly; content streams until completion.
- **Relationship-Aware Tone**: Select **Group** (e.g., Manager/Clients) and **Tone** (e.g., Polite/Formal) and see rules applied.
- **Saved Templates**: Apply a frequently used template (e.g., “Weekly Report”) to prefill structure.
- **Inline Reply Options**: Positive / Neutral / Firm candidates shown inline; insert with a tap.
- **Attachment-Aware Reply (mocked)**: If a PDF is attached, the reply references summarized key points.

---

## Getting Started

### Prerequisites
- **Docker** & **Docker Compose**
- **Android Studio**: Iguana or newer, **JDK 17**
- **Android SDK**: minSdk 24+

### Installation

> **Branch**: `iteration-2-demo`

```
git clone https://github.com/snuhcs-course/swpp-2025-project-team-11.git
cd swpp-2025-project-team-11
git switch iteration-2-demo
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

- **Supports local email saving**  
  Pre-loads data from local storage.

- **Add Groups**  
  Assign people to group you want.

Demo Video
Link: https://drive.google.com/file/d/1D2pSNKzdieUSxCElThCU0ZZjq0TQ3GPn/view?usp=sharing
