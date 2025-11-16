# Xend

<img width="244" height="111" alt="xend" src="https://github.com/user-attachments/assets/7773cde3-c7bd-4d20-ad35-f7b91de14016" />

An AI-powered email drafting assistant that generates **polite, relationship-aware** emails with **streaming drafts**, **saved templates**, and **inline reply options**. Designed to reduce editing time while keeping tone appropriate for each recipient.


<img width="360" height="800" alt="Xend Login Screen" src="https://github.com/user-attachments/assets/c10b6b50-188d-4e7e-8863-18bc2bc34250" />


<img width="360" height="800" alt="Xend Mail List Screen" src="https://github.com/user-attachments/assets/00d9acb1-83f5-434d-b3ce-bd09d726a4bc" />

<img width="360" height="800" alt="Xend Compose with Keyboard" src="https://github.com/user-attachments/assets/1b4a1d89-be1a-4b67-9008-3c77b23afacb" />

<img width="360" height="800" alt="Xend Contacts Screen" src="https://github.com/user-attachments/assets/67107285-70b0-4632-b1ac-ca94cf1a368c" />

<img width="360" height="800" alt="Xend Improved Reply Options" src="https://github.com/user-attachments/assets/11e4ce38-7a7e-416e-b4e6-8321fff47fec" />


## Features

### What's New in Iteration 4
- **Addition of tone analysis module**: Analyze and apply the user's writing style based on emails previously sent to the same recipient during email generation
- **Email composition prompt preview**: Review the prompt currently used by the AI to generate emails
- **Attachment preview and AI analysis**: In-app attachment preview and AI-driven analysis of attachment contents
- **Profile information management**: Enter details such as name and affiliation to be utilized in email generation
- **Email draft saving**
- **Redo/undo for AI-generated content**
- **Contact search**
- **Overall user interface enhancements**

### Previous Iterations
- **Real-time AI Suggestions**: Live text completion as you type with WebSocket streaming
- **Smart Reply Options**: Generate multiple reply styles (positive/neutral/firm) with SSE streaming
- **Contact & Group Management**: Organize contacts by groups with personalized AI prompts
- **Offline-First Architecture**: Local database caching with incremental sync
- **Rich Text Editing**: Full formatting support (bold, italic, underline, colors, font sizes)
- **Template System**: Save and reuse frequently used email templates

- **Streaming Draft Generation**: First sentence appears quickly; content streams until completion
- **Relationship-Aware Tone**: Select **Group** (e.g., Manager/Clients) and **Tone** (e.g., Polite/Formal) and see rules applied
- **Inline Reply Options**: Positive / Neutral / Firm candidates shown inline; insert with a tap

---

## Getting Started

### Prerequisites
- **Docker** & **Docker Compose**
- **Android Studio**: Iguana or newer, **JDK 17**
- **Android SDK**: minSdk 24+

### Installation

#### For Iteration 4 Demo

> **Branch**: `iteration-4-demo`

```bash
git clone https://github.com/snuhcs-course/swpp-2025-project-team-11.git
cd swpp-2025-project-team-11
git switch iteration-4-demo
```

**Backend Setup**
```bash
cd backend
cp .env.example .env   # Configure environment variables
poetry install
python manage.py migrate
python manage.py runserver
```

**GPU Server Setup** (Optional, for AI features)
```bash
cd gpu-server
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8001
```

**Android App Setup**
1. Open `/frontend` in Android Studio
2. Create `local.properties` with:
   ```properties
   sdk.dir=/path/to/Android/sdk
   base.url=https://your-backend-url.com
   ws.url=wss://your-backend-url.com/ws/ai/mail/
   ```
3. Sync Gradle
4. Select an emulator/device (e.g., Pixel 7, API 34)
5. Run the app (Debug)

#### For Iteration 1 Demo (Legacy)

> **Branch**: `iteration-1-demo`

```bash
git switch iteration-1-demo
cp .env.example .env
docker compose up -d --build
```

Default URL: http://localhost/

## What Iteration 4 Demonstrates

### Core Features
- **Gmail Integration**: OAuth 2.0 authentication and inbox synchronization
- **Personalized email generation**: Create tailored emails that reflect the user's writing style and the relationship between the user and the recipient
- **Real-time AI Composition**: Live text suggestions as you type via WebSocket
- **Smart Reply Generation**: Multiple reply options (positive/neutral/firm) via SSE streaming
- **Contact Management**: Organize contacts by groups with custom AI prompts
- **Offline-First**: Local database caching with background sync
- **Rich Text Editor**: Full formatting toolbar with HTML support

### Technical Highlights
- **Incremental Sync**: Only fetch new emails since last update
- **Streaming Responses**: SSE/WebSocket for real-time AI feedback
- **Database Migrations**: Safe schema updates without data loss
- **JSON Parsing**: Clean extraction of AI responses from markdown code blocks
- **Attachment AI analysis**: Real-time analysis of Gmail attachments and uploaded files to provide summaries, insights, and guidance for composing emails

## Demo Videos

- **Iteration 1 Demo**: https://drive.google.com/file/d/1to4deCD6jBssx2V_oZgZU5E5Os2PlfP1/view?usp=sharing
- **Iteration 3 Demo**: https://drive.google.com/file/d/1UmfNOg-AQAKq9O-n2GzkcnLeweSbvZ1W/view?usp=sharing
- **Iteration 4 Demo**: https://drive.google.com/file/d/1neQrvApVfsdF6zUwdBuIUTQQ30xQDPFO/view?usp=sharing
