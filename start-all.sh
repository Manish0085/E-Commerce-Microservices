#!/bin/bash

# ═══════════════════════════════════════════════════════
#  START ALL SERVICES
#  Includes: Eureka + 6 business services + API Gateway
# ═══════════════════════════════════════════════════════

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_DIR="/home/vidit/Desktop/Microservices"
LOGS_DIR="$PROJECT_DIR/logs"

mkdir -p "$LOGS_DIR"

# ─── Helper function to check if a service is UP ───────
wait_for_service() {
    local name=$1
    local url=$2
    local max_attempts=30
    local attempt=0

    while [ $attempt -lt $max_attempts ]; do
        if curl -s --max-time 2 "$url" > /dev/null 2>&1; then
            return 0
        fi
        sleep 2
        attempt=$((attempt + 1))
    done
    return 1
}

# ─── STEP 1: Docker MySQL Containers ────────────────────
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  STEP 1: Starting Docker MySQL Containers${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

docker start \
    product-mysql \
    inventory-mysql \
    customer-mysql \
    order-mysql \
    billing-mysql \
    payment-mysql \
    2>/dev/null

sleep 5
echo -e "${GREEN}✅ Docker containers starting${NC}"
echo ""

# ─── STEP 2: Eureka Server (FIRST - others depend on it) ─
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  STEP 2: Starting Eureka Server (waits 30s)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

cd "$PROJECT_DIR/eureka-server"
nohup ./mvnw spring-boot:run > "$LOGS_DIR/eureka.log" 2>&1 &
EUREKA_PID=$!
echo -e "${YELLOW}  PID: $EUREKA_PID${NC}"

echo -e "${YELLOW}  Waiting for Eureka to be ready...${NC}"
if wait_for_service "eureka" "http://localhost:8761"; then
    echo -e "${GREEN}✅ Eureka Server is UP${NC}"
else
    echo -e "${RED}❌ Eureka Server failed to start - check logs/eureka.log${NC}"
    exit 1
fi
echo ""

# ─── STEP 3: Business Services (parallel startup) ───────
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  STEP 3: Starting 6 Business Services${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

BUSINESS_SERVICES=(
    "product-service"
    "inventory-service"
    "customer-service"
    "order-service"
    "billing-service"
    "payment-service"
)

for service in "${BUSINESS_SERVICES[@]}"; do
    echo -e "${YELLOW}  Starting $service...${NC}"
    cd "$PROJECT_DIR/$service"
    nohup ./mvnw spring-boot:run > "$LOGS_DIR/$service.log" 2>&1 &
    echo -e "${YELLOW}    PID: $!${NC}"
done

echo ""
echo -e "${YELLOW}  Waiting 45 seconds for services to register with Eureka...${NC}"
sleep 45
echo ""

# ─── STEP 4: API Gateway (LAST - needs services registered) ─
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  STEP 4: Starting API Gateway${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

cd "$PROJECT_DIR/api-gateway"
nohup ./mvnw spring-boot:run > "$LOGS_DIR/api-gateway.log" 2>&1 &
GATEWAY_PID=$!
echo -e "${YELLOW}  PID: $GATEWAY_PID${NC}"

echo -e "${YELLOW}  Waiting for Gateway to be ready...${NC}"
if wait_for_service "gateway" "http://localhost:8080/actuator/health"; then
    echo -e "${GREEN}✅ API Gateway is UP${NC}"
else
    echo -e "${RED}⚠️  API Gateway still starting - check logs/api-gateway.log${NC}"
fi
echo ""

# ─── FINAL STATUS ───────────────────────────────────────
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  ALL SERVICES STARTED!${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BLUE}ENDPOINTS:${NC}"
echo "  Eureka Dashboard:  http://localhost:8761"
echo "  API Gateway:       http://localhost:8080"
echo "  Product Service:   http://localhost:8081"
echo "  Inventory Service: http://localhost:8082"
echo "  Customer Service:  http://localhost:8083"
echo "  Order Service:     http://localhost:8084"
echo "  Billing Service:   http://localhost:8085"
echo "  Payment Service:   http://localhost:8086"
echo ""
echo -e "${BLUE}QUICK TESTS:${NC}"
echo "  ./status.sh                                    (check all services)"
echo "  ./test-e2e.sh                                  (run end-to-end tests)"
echo "  curl http://localhost:8080/api/v1/products     (test via Gateway)"
echo ""
echo -e "${BLUE}LOGS:${NC}"
echo "  tail -f $LOGS_DIR/<service-name>.log"
echo ""
echo -e "${BLUE}TO STOP:${NC}"
echo "  ./stop-all.sh"
echo ""
