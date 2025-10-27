#!/bin/sh

RELATIVE_DIR=`dirname "$0"`

cd $RELATIVE_DIR
cd ../../

docker compose down
echo "y" | docker system prune -a --filter "until=24h"
docker compose -f docker-compose_dev.yml up --build -d