FROM python:3.13-slim AS base

RUN apt-get -y update
RUN apt-get -y install curl vim

RUN python -m pip install --upgrade pip
RUN pip install poetry

COPY . /app
WORKDIR /app

EXPOSE 8000

FROM base AS local
RUN poetry config virtualenvs.create false
RUN poetry install

FROM base AS dev
RUN poetry install

FROM base AS prod
RUN poetry install