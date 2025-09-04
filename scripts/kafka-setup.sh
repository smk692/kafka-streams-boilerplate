#!/bin/bash

# Kafka 토픽 생성 스크립트

echo "🚀 Kafka 토픽 생성 시작..."

# Kafka가 완전히 시작될 때까지 대기
echo "⏳ Kafka 서비스가 준비될 때까지 대기 중..."
sleep 30

# 토픽 생성 함수
create_topic() {
    local topic_name=$1
    local partitions=${2:-3}
    local replication_factor=${3:-1}
    
    echo "📝 토픽 생성: $topic_name (partitions: $partitions, replication-factor: $replication_factor)"
    
    docker exec kafka kafka-topics --create \
        --bootstrap-server localhost:9092 \
        --topic $topic_name \
        --partitions $partitions \
        --replication-factor $replication_factor \
        --if-not-exists
        
    if [ $? -eq 0 ]; then
        echo "✅ 토픽 '$topic_name' 생성 완료"
    else
        echo "❌ 토픽 '$topic_name' 생성 실패"
    fi
}

# Order Processing 토픽
create_topic "orders" 3 1
create_topic "processed-orders" 3 1

# Inventory Tracking 토픽
create_topic "inventory-updates" 3 1
create_topic "inventory-alerts" 3 1

# 추가 토픽 (필요시)
create_topic "error-topic" 1 1

echo ""
echo "📋 생성된 토픽 목록:"
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

echo ""
echo "🎉 Kafka 토픽 설정 완료!"
echo "💡 Kafka UI: http://localhost:8090"
echo "🔗 Kafka Broker: localhost:9092"