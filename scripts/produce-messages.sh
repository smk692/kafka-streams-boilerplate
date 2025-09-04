#!/bin/bash

# Kafka Producer 스크립트 - 토폴로지 테스트용

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Kafka 설정
KAFKA_CONTAINER="kafka"
BOOTSTRAP_SERVER="localhost:9092"

# 메시지 전송 함수
produce_message() {
    local topic=$1
    local message=$2
    local key=${3:-""}
    
    if [ -z "$key" ]; then
        # 키 없이 메시지 전송
        echo "$message" | docker exec -i $KAFKA_CONTAINER kafka-console-producer \
            --bootstrap-server $BOOTSTRAP_SERVER \
            --topic $topic
    else
        # 키와 함께 메시지 전송
        echo "$key:$message" | docker exec -i $KAFKA_CONTAINER kafka-console-producer \
            --bootstrap-server $BOOTSTRAP_SERVER \
            --topic $topic \
            --property "parse.key=true" \
            --property "key.separator=:"
    fi
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 메시지 전송 완료${NC}"
        echo -e "   Topic: ${BLUE}$topic${NC}"
        if [ ! -z "$key" ]; then
            echo -e "   Key: ${YELLOW}$key${NC}"
        fi
        echo -e "   Message: $message"
    else
        echo -e "${RED}❌ 메시지 전송 실패${NC}"
    fi
}

# JSON 메시지 전송 함수
produce_json() {
    local topic=$1
    local json_message=$2
    local key=${3:-""}
    
    echo -e "${BLUE}📤 JSON 메시지 전송 중...${NC}"
    produce_message "$topic" "$json_message" "$key"
}

# 배치 메시지 전송 함수
produce_batch() {
    local topic=$1
    local count=$2
    local prefix=${3:-"message"}
    
    echo -e "${YELLOW}📦 배치 메시지 전송 시작 (총 ${count}개)${NC}"
    for i in $(seq 1 $count); do
        local timestamp=$(($(date +%s) * 1000 + RANDOM % 1000))
        local message="{\"id\":$i,\"content\":\"${prefix}_${i}\",\"timestamp\":$timestamp}"
        produce_json "$topic" "$message" "key_$i"
        sleep 0.1
    done
    echo -e "${GREEN}✅ 배치 전송 완료${NC}"
}

# Order 메시지 생성 함수
create_order_message() {
    local order_id=$1
    local customer_id=$2
    local product_id=$3
    local quantity=$4
    local price=$5
    
    echo "{\"orderId\":\"$order_id\",\"customerId\":\"$customer_id\",\"productId\":\"$product_id\",\"quantity\":$quantity,\"price\":$price,\"timestamp\":$(($(date +%s) * 1000 + RANDOM % 1000)),\"status\":\"NEW\"}"
}

# Inventory 메시지 생성 함수
create_inventory_message() {
    local product_id=$1
    local warehouse_id=$2
    local quantity=$3
    local operation=$4
    
    echo "{\"productId\":\"$product_id\",\"warehouseId\":\"$warehouse_id\",\"quantity\":$quantity,\"operation\":\"$operation\",\"timestamp\":$(($(date +%s) * 1000 + RANDOM % 1000))}"
}

# 메인 메뉴
show_menu() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${GREEN}   Kafka Message Producer Script${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo -e "${YELLOW}토픽 선택:${NC}"
    echo "1) orders - Order 메시지 전송"
    echo "2) inventory-updates - Inventory 업데이트 전송"
    echo "3) 커스텀 토픽 메시지 전송"
    echo "4) 배치 메시지 전송"
    echo "5) 테스트 시나리오 실행"
    echo "6) 토픽 목록 확인"
    echo "7) 토픽 컨슈머 시작 (메시지 읽기)"
    echo "0) 종료"
    echo -e "${BLUE}========================================${NC}"
}

# 테스트 시나리오
run_test_scenario() {
    echo -e "\n${GREEN}🧪 테스트 시나리오 실행${NC}"
    echo -e "${YELLOW}시나리오: E-commerce Order Processing${NC}\n"
    
    # Step 1: 주문 생성
    echo -e "${BLUE}Step 1: 주문 생성${NC}"
    for i in {1..5}; do
        order_msg=$(create_order_message "ORDER_$(date +%s)_$i" "CUST_$((RANDOM % 100))" "PROD_$((RANDOM % 50))" "$((RANDOM % 10 + 1))" "$((RANDOM % 1000 + 100))")
        produce_json "orders" "$order_msg" "order_key_$i"
        sleep 0.5
    done
    
    # Step 2: 재고 업데이트
    echo -e "\n${BLUE}Step 2: 재고 업데이트${NC}"
    for i in {1..3}; do
        inv_msg=$(create_inventory_message "PROD_$((RANDOM % 50))" "WH_$((RANDOM % 5 + 1))" "$((RANDOM % 100 + 10))" "ADD")
        produce_json "inventory-updates" "$inv_msg" "inv_key_$i"
        sleep 0.5
    done
    
    echo -e "\n${GREEN}✅ 테스트 시나리오 완료${NC}"
}

# 컨슈머 시작 함수
start_consumer() {
    local topic=$1
    echo -e "${GREEN}📨 토픽 '$topic' 컨슈머 시작${NC}"
    echo -e "${YELLOW}종료하려면 Ctrl+C를 누르세요${NC}\n"
    
    docker exec -it $KAFKA_CONTAINER kafka-console-consumer \
        --bootstrap-server $BOOTSTRAP_SERVER \
        --topic $topic \
        --from-beginning \
        --property print.key=true \
        --property key.separator=" => " \
        --formatter kafka.tools.DefaultMessageFormatter
}

# 메인 루프
while true; do
    show_menu
    read -p "선택: " choice
    
    case $choice in
        1)
            echo -e "\n${YELLOW}Order 메시지 전송${NC}"
            read -p "Customer ID (예: CUST_001): " cust_id
            read -p "Product ID (예: PROD_001): " prod_id
            read -p "Quantity: " qty
            read -p "Price: " price
            
            order_id="ORDER_$(date +%s)"
            order_msg=$(create_order_message "$order_id" "$cust_id" "$prod_id" "$qty" "$price")
            produce_json "orders" "$order_msg" "$order_id"
            ;;
            
        2)
            echo -e "\n${YELLOW}Inventory 업데이트 전송${NC}"
            read -p "Product ID (예: PROD_001): " prod_id
            read -p "Warehouse ID (예: WH_001): " wh_id
            read -p "Quantity: " qty
            read -p "Operation (ADD/REMOVE): " op
            
            inv_msg=$(create_inventory_message "$prod_id" "$wh_id" "$qty" "$op")
            produce_json "inventory-updates" "$inv_msg" "$prod_id"
            ;;
            
        3)
            echo -e "\n${YELLOW}커스텀 메시지 전송${NC}"
            read -p "Topic 이름: " topic
            read -p "메시지 내용: " message
            read -p "키 (선택사항, Enter로 스킵): " key
            
            produce_message "$topic" "$message" "$key"
            ;;
            
        4)
            echo -e "\n${YELLOW}배치 메시지 전송${NC}"
            read -p "Topic 이름: " topic
            read -p "메시지 개수: " count
            read -p "메시지 prefix (기본: message): " prefix
            prefix=${prefix:-"message"}
            
            produce_batch "$topic" "$count" "$prefix"
            ;;
            
        5)
            run_test_scenario
            ;;
            
        6)
            echo -e "\n${YELLOW}📋 토픽 목록:${NC}"
            docker exec $KAFKA_CONTAINER kafka-topics --list --bootstrap-server $BOOTSTRAP_SERVER | sort
            ;;
            
        7)
            echo -e "\n${YELLOW}토픽 선택${NC}"
            docker exec $KAFKA_CONTAINER kafka-topics --list --bootstrap-server $BOOTSTRAP_SERVER | sort | nl
            read -p "토픽 이름 입력: " topic
            start_consumer "$topic"
            ;;
            
        0)
            echo -e "\n${GREEN}👋 종료합니다.${NC}"
            exit 0
            ;;
            
        *)
            echo -e "${RED}잘못된 선택입니다.${NC}"
            ;;
    esac
done