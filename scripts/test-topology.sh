#!/bin/bash

# Kafka Streams 토폴로지 테스트 스크립트

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

KAFKA_CONTAINER="kafka"
BOOTSTRAP_SERVER="localhost:9092"

# 타임스탬프 생성 함수 (macOS 호환)
get_timestamp() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS - 초 단위에 랜덤 밀리초 추가
        echo $(($(date +%s) * 1000 + RANDOM % 1000))
    else
        # Linux
        date +%s%3N
    fi
}

# JSON 메시지 전송 함수
send_json() {
    local topic=$1
    local message=$2
    local key=${3:-""}
    
    if [ -z "$key" ]; then
        echo "$message" | docker exec -i $KAFKA_CONTAINER kafka-console-producer \
            --bootstrap-server $BOOTSTRAP_SERVER \
            --topic $topic 2>/dev/null
    else
        echo "$key:$message" | docker exec -i $KAFKA_CONTAINER kafka-console-producer \
            --bootstrap-server $BOOTSTRAP_SERVER \
            --topic $topic \
            --property "parse.key=true" \
            --property "key.separator=:" 2>/dev/null
    fi
}

# Order Processing 테스트
test_order_processing() {
    echo -e "\n${BLUE}==== Order Processing Topology 테스트 ====${NC}"
    
    # 정상 주문
    echo -e "${GREEN}1. 정상 주문 전송${NC}"
    for i in {1..3}; do
        order_id="ORDER_$(get_timestamp)_$i"
        message="{
            \"orderId\": \"$order_id\",
            \"customerId\": \"CUST_00$i\",
            \"productId\": \"PROD_00$i\",
            \"quantity\": $((i * 2)),
            \"price\": $((i * 100)),
            \"timestamp\": $(get_timestamp),
            \"status\": \"NEW\"
        }"
        compressed_msg=$(echo $message | tr -d '\n' | tr -s ' ')
        send_json "orders" "$compressed_msg" "$order_id"
        echo "   ✅ Order $order_id 전송"
        sleep 0.5
    done
    
    # 대량 주문
    echo -e "\n${GREEN}2. 대량 주문 전송 (quantity > 10)${NC}"
    order_id="ORDER_BULK_$(get_timestamp)"
    bulk_order="{
        \"orderId\": \"$order_id\",
        \"customerId\": \"CUST_VIP\",
        \"productId\": \"PROD_BULK\",
        \"quantity\": 50,
        \"price\": 5000,
        \"timestamp\": $(get_timestamp),
        \"status\": \"NEW\"
    }"
    send_json "orders" "$(echo $bulk_order | tr -d '\n' | tr -s ' ')" "$order_id"
    echo "   ✅ Bulk order $order_id 전송"
    
    # 고가 주문
    echo -e "\n${GREEN}3. 고가 주문 전송 (price > 1000)${NC}"
    order_id="ORDER_HIGH_$(get_timestamp)"
    high_value="{
        \"orderId\": \"$order_id\",
        \"customerId\": \"CUST_PREMIUM\",
        \"productId\": \"PROD_LUXURY\",
        \"quantity\": 2,
        \"price\": 10000,
        \"timestamp\": $(get_timestamp),
        \"status\": \"NEW\"
    }"
    send_json "orders" "$(echo $high_value | tr -d '\n' | tr -s ' ')" "$order_id"
    echo "   ✅ High-value order $order_id 전송"
}

# Inventory Tracking 테스트
test_inventory_tracking() {
    echo -e "\n${BLUE}==== Inventory Tracking Topology 테스트 ====${NC}"
    
    # 재고 추가
    echo -e "${GREEN}1. 재고 추가 업데이트${NC}"
    for i in {1..3}; do
        product_id="PROD_00$i"
        message="{
            \"productId\": \"$product_id\",
            \"warehouseId\": \"WH_0$i\",
            \"quantity\": $((i * 10)),
            \"operation\": \"ADD\",
            \"timestamp\": $(get_timestamp)
        }"
        compressed_msg=$(echo $message | tr -d '\n' | tr -s ' ')
        send_json "inventory-updates" "$compressed_msg" "$product_id"
        echo "   ✅ 재고 추가: $product_id (+$((i * 10)))"
        sleep 0.3
    done
    
    # 재고 감소
    echo -e "\n${GREEN}2. 재고 감소 업데이트${NC}"
    for i in {1..2}; do
        product_id="PROD_00$i"
        message="{
            \"productId\": \"$product_id\",
            \"warehouseId\": \"WH_0$i\",
            \"quantity\": $((i * 5)),
            \"operation\": \"REMOVE\",
            \"timestamp\": $(get_timestamp)
        }"
        compressed_msg=$(echo $message | tr -d '\n' | tr -s ' ')
        send_json "inventory-updates" "$compressed_msg" "$product_id"
        echo "   ✅ 재고 감소: $product_id (-$((i * 5)))"
        sleep 0.3
    done
    
    # 낮은 재고 경고 트리거
    echo -e "\n${GREEN}3. 낮은 재고 경고 트리거${NC}"
    product_id="PROD_LOW"
    # 먼저 재고 추가
    add_msg="{
        \"productId\": \"$product_id\",
        \"warehouseId\": \"WH_01\",
        \"quantity\": 15,
        \"operation\": \"ADD\",
        \"timestamp\": $(get_timestamp)
    }"
    send_json "inventory-updates" "$(echo $add_msg | tr -d '\n' | tr -s ' ')" "$product_id"
    echo "   ✅ 초기 재고 설정: $product_id (+15)"
    sleep 0.5
    
    # 대량 감소로 경고 트리거
    remove_msg="{
        \"productId\": \"$product_id\",
        \"warehouseId\": \"WH_01\",
        \"quantity\": 12,
        \"operation\": \"REMOVE\",
        \"timestamp\": $(get_timestamp)
    }"
    send_json "inventory-updates" "$(echo $remove_msg | tr -d '\n' | tr -s ' ')" "$product_id"
    echo "   ✅ 대량 재고 감소: $product_id (-12) → 경고 예상"
}

# 통합 시나리오 테스트
test_integrated_scenario() {
    echo -e "\n${BLUE}==== 통합 시나리오 테스트 ====${NC}"
    echo -e "${YELLOW}시뮬레이션: 실시간 주문 처리 및 재고 관리${NC}"
    
    # Step 1: 재고 초기화
    echo -e "\n${GREEN}Step 1: 재고 초기화${NC}"
    products=("LAPTOP" "PHONE" "TABLET")
    for i in "${!products[@]}"; do
        product="${products[$i]}"
        quantity=$((20 + i * 10))
        message="{
            \"productId\": \"$product\",
            \"warehouseId\": \"WH_MAIN\",
            \"quantity\": $quantity,
            \"operation\": \"ADD\",
            \"timestamp\": $(get_timestamp)
        }"
        send_json "inventory-updates" "$(echo $message | tr -d '\n' | tr -s ' ')" "$product"
        echo "   ✅ $product 재고 설정: +$quantity"
        sleep 0.3
    done
    
    # Step 2: 연속 주문 생성
    echo -e "\n${GREEN}Step 2: 연속 주문 처리${NC}"
    for i in {1..5}; do
        product="${products[$((RANDOM % 3))]}"
        order_id="ORDER_FLOW_$(get_timestamp)"
        quantity=$((RANDOM % 5 + 1))
        price=$((RANDOM % 1000 + 500))
        
        order_msg="{
            \"orderId\": \"$order_id\",
            \"customerId\": \"CUST_FLOW_$i\",
            \"productId\": \"$product\",
            \"quantity\": $quantity,
            \"price\": $price,
            \"timestamp\": $(get_timestamp),
            \"status\": \"NEW\"
        }"
        send_json "orders" "$(echo $order_msg | tr -d '\n' | tr -s ' ')" "$order_id"
        echo "   ✅ 주문 생성: $order_id ($product x$quantity)"
        
        # 동시에 재고 차감
        inv_msg="{
            \"productId\": \"$product\",
            \"warehouseId\": \"WH_MAIN\",
            \"quantity\": $quantity,
            \"operation\": \"REMOVE\",
            \"timestamp\": $(get_timestamp)
        }"
        send_json "inventory-updates" "$(echo $inv_msg | tr -d '\n' | tr -s ' ')" "$product"
        echo "   📦 재고 차감: $product -$quantity"
        
        sleep 0.7
    done
}

# 부하 테스트
test_load() {
    echo -e "\n${BLUE}==== 부하 테스트 ====${NC}"
    echo -e "${YELLOW}100개의 메시지를 빠르게 전송${NC}"
    
    start_time=$(date +%s)
    
    for i in {1..100}; do
        order_id="LOAD_TEST_$(get_timestamp)_$i"
        message="{
            \"orderId\": \"$order_id\",
            \"customerId\": \"CUST_LOAD_$((i % 10))\",
            \"productId\": \"PROD_$((i % 20))\",
            \"quantity\": $((RANDOM % 10 + 1)),
            \"price\": $((RANDOM % 1000 + 100)),
            \"timestamp\": $(get_timestamp),
            \"status\": \"NEW\"
        }"
        send_json "orders" "$(echo $message | tr -d '\n' | tr -s ' ')" "$order_id" &
        
        # 10개마다 진행 상황 표시
        if [ $((i % 10)) -eq 0 ]; then
            echo -n "."
        fi
    done
    
    wait
    end_time=$(date +%s)
    duration=$((end_time - start_time))
    
    echo -e "\n${GREEN}✅ 부하 테스트 완료${NC}"
    echo "   전송된 메시지: 100개"
    echo "   소요 시간: ${duration}초"
}

# 메뉴 표시
show_menu() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${GREEN}   Kafka Streams Topology 테스트${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo "1) Order Processing 토폴로지 테스트"
    echo "2) Inventory Tracking 토폴로지 테스트"
    echo "3) 통합 시나리오 테스트"
    echo "4) 부하 테스트 (100 messages)"
    echo "5) 모든 테스트 실행"
    echo "0) 종료"
    echo -e "${BLUE}========================================${NC}"
}

# 메인 실행
if [ "$1" == "--all" ]; then
    echo -e "${GREEN}🚀 모든 테스트 실행${NC}"
    test_order_processing
    test_inventory_tracking
    test_integrated_scenario
    test_load
    echo -e "\n${GREEN}🎉 모든 테스트 완료!${NC}"
    exit 0
fi

while true; do
    show_menu
    read -p "선택: " choice
    
    case $choice in
        1) test_order_processing ;;
        2) test_inventory_tracking ;;
        3) test_integrated_scenario ;;
        4) test_load ;;
        5)
            test_order_processing
            test_inventory_tracking
            test_integrated_scenario
            test_load
            ;;
        0)
            echo -e "\n${GREEN}👋 종료합니다.${NC}"
            exit 0
            ;;
        *) echo -e "${RED}잘못된 선택입니다.${NC}" ;;
    esac
done