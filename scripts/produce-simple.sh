#!/bin/bash

# 간단한 Kafka Producer 스크립트 - 빠른 테스트용

KAFKA_CONTAINER="kafka"
BOOTSTRAP_SERVER="localhost:9092"

# 사용법 출력
usage() {
    echo "사용법: $0 <topic> <message> [key]"
    echo "예시:"
    echo "  $0 orders '{\"orderId\":\"123\",\"amount\":1000}'"
    echo "  $0 orders '{\"orderId\":\"456\",\"amount\":2000}' order_456"
    exit 1
}

# 인자 확인
if [ $# -lt 2 ]; then
    usage
fi

TOPIC=$1
MESSAGE=$2
KEY=${3:-""}

# 메시지 전송
if [ -z "$KEY" ]; then
    echo "📤 메시지 전송 중..."
    echo "$MESSAGE" | docker exec -i $KAFKA_CONTAINER kafka-console-producer \
        --bootstrap-server $BOOTSTRAP_SERVER \
        --topic $TOPIC
else
    echo "📤 키와 함께 메시지 전송 중..."
    echo "$KEY:$MESSAGE" | docker exec -i $KAFKA_CONTAINER kafka-console-producer \
        --bootstrap-server $BOOTSTRAP_SERVER \
        --topic $TOPIC \
        --property "parse.key=true" \
        --property "key.separator=:"
fi

if [ $? -eq 0 ]; then
    echo "✅ 전송 완료!"
    echo "   Topic: $TOPIC"
    [ ! -z "$KEY" ] && echo "   Key: $KEY"
    echo "   Message: $MESSAGE"
else
    echo "❌ 전송 실패"
    exit 1
fi