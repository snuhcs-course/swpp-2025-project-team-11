FROM python:3.12-slim AS base

# 필수 OS 패키지 설치
RUN apt-get update && apt-get install -y \
    curl \
    vim \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# pip, setuptools, wheel 설치
RUN python -m ensurepip
RUN pip install --upgrade pip setuptools wheel

# poetry 설치
RUN pip install poetry

COPY . /app
WORKDIR /app
EXPOSE 8000

# Local
FROM base AS local
RUN poetry config virtualenvs.create false
RUN poetry install --no-root
