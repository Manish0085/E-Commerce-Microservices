#!/bin/bash

# ═══════════════════════════════════════════════════════
#  CHECK STATUS OF ALL SERVICES
# ═══════════════════════════════════════════════════════

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

check_service() {
    local name=$1
    local url=$2
    local response=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$url" 2>/dev/null)

    if [ "$response" = "200" ] || [ "$response" = "302" ] || [ "$response" = "404" ]; then
        echo -e "  ${GREEN}✅ $name${NC} — UP  ($url)"
        return 0
    else
        echo -e "  ${RED}❌ $name${NC} — DOWN ($url)"
        return 1
    fi
}

check_docker() {
    local name=$1
    if docker ps --format "{{.Names}}" | grep -q "^$name$"; then
        echo -e "  ${GREEN}✅ $name${NC} — running"
        return 0
    else
        echo -e "  ${RED}❌ $name${NC} — not running"
        return 1
    fi
}

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  DOCKER MYSQL CONTAINERS${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
check_docker "product-mysql"
check_docker "inventory-mysql"
check_docker "customer-mysql"
check_docker "order-mysql"
check_docker "billing-mysql"
check_docker "payment-mysql"

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  INFRASTRUCTURE SERVICES${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
check_service "Eureka Server    " "http://localhost:8761"
check_service "API Gateway      " "http://localhost:8080/actuator/health"

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  BUSINESS SERVICES${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
check_service "Product Service  " "http://localhost:8081/actuator/health"
check_service "Inventory Service" "http://localhost:8082/actuator/health"
check_service "Customer Service " "http://localhost:8083/actuator/health"
check_service "Order Service    " "http://localhost:8084/actuator/health"
check_service "Billing Service  " "http://localhost:8085/actuator/health"
check_service "Payment Service  " "http://localhost:8086/actuator/health"

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  EUREKA REGISTERED SERVICES${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

if curl -s --max-time 3 http://localhost:8761/eureka/apps > /dev/null 2>&1; then
    REGISTERED=$(curl -s http://localhost:8761/eureka/apps \
        -H "Accept: application/json" 2>/dev/null \
        | grep -oE '"name":"[^"]*"' \
        | sed 's/"name":"//g' \
        | sed 's/"//g' \
        | sort -u)

    if [ -z "$REGISTERED" ]; then
        echo -e "  ${YELLOW}⚠️  No services registered yet${NC}"
    else
        echo "$REGISTERED" | while read -r app; do
            echo -e "  ${GREEN}✅ $app${NC}"
        done
    fi
else
    echo -e "  ${RED}❌ Eureka not reachable${NC}"
fi

echo ""
