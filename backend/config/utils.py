import os
import shutil
from pathlib import Path

import environ


def get_or_create_env_file():
    base_dir = Path(__file__).resolve().parent.parent

    env_file = os.path.join(base_dir, ".env")
    if not os.path.isfile(env_file):
        env_example_file = os.path.join(base_dir, ".env_example")
        shutil.copyfile(env_example_file, env_file)

    return env_file


def get_env():
    env_file = get_or_create_env_file()

    env = environ.Env(DEBUG=(bool, True), overwrite=True)
    environ.Env.read_env(env_file=env_file)

    return env


def set_environment():
    env = get_env()

    match env("ENVIRONMENT"):
        case "PROD":
            os.environ.setdefault("DJANGO_SETTINGS_MODULE", "config.settings.production")
        case "DEV":
            os.environ.setdefault("DJANGO_SETTINGS_MODULE", "config.settings.development")
        case _:
            os.environ.setdefault("DJANGO_SETTINGS_MODULE", "config.settings.local")
