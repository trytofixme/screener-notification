#!/bin/bash

CONFIG_FILE="topics.conf"
BROKER="kafka:9092"
PARTITIONS=1
REPLICATION_FACTOR=1

echo "Ожидаем доступность брокера Kafka ($BROKER)..."
for i in {1..30}; do
  if kafka-topics.sh --bootstrap-server "$BROKER" --list >/dev/null 2>&1; then
    echo "Kafka доступна!"
    break
  fi
  echo "Ожидание $i..."
  sleep 2
done

while IFS= read -r TOPIC || [[ -n "$TOPIC" ]]; do
  TOPIC=$(echo "$TOPIC" | tr -d '\r' | xargs)
  if [[ -z "$TOPIC" ]]; then continue; fi

  echo "Создаём топик: $TOPIC"
  kafka-topics.sh --bootstrap-server "$BROKER" \
                  --create \
                  --if-not-exists \
                  --topic "$TOPIC" \
                  --partitions "$PARTITIONS" \
                  --replication-factor "$REPLICATION_FACTOR"
done < "$CONFIG_FILE"
