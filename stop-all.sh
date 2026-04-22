#!/bin/bash

# ═══════════════════════════════════════════════════════
#  STOP ALL SERVICES
# ═══════════════════════════════════════════════════════

YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  STOPPING ALL SPRING BOOT SERVICES${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

# Count running services first
RUNNING=$(pgrep -f "spring-boot:run" | wc -l)

if [ "$RUNNING" -eq 0 ]; then
    echo -e "${YELLOW}No Spring Boot services running${NC}"
else
    echo -e "${YELLOW}Stopping $RUNNING service(s) gracefully...${NC}"
    pkill -f "spring-boot:run" 2>/dev/null

    # Wait for graceful shutdown
    sleep 5

    # Force kill if any still running
    STILL_RUNNING=$(pgrep -f "spring-boot:run" | wc -l)
    if [ "$STILL_RUNNING" -gt 0 ]; then
        echo -e "${YELLOW}Force killing $STILL_RUNNING stubborn process(es)...${NC}"
        pkill -9 -f "spring-boot:run" 2>/dev/null
        sleep 2
    fi

    echo -e "${GREEN}✅ All Spring Boot services stopped${NC}"
fi

echo ""

# ─── Check if user wants to stop Docker containers ──────
echo -e "${YELLOW}Stop Docker MySQL containers as well? (y/n)${NC}"
read -r answer

if [ "$answer" = "y" ] || [ "$answer" = "Y" ]; then
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  STOPPING DOCKER CONTAINERS${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

    docker stop \
        product-mysql \
        inventory-mysql \
        customer-mysql \
        order-mysql \
        billing-mysql \
        payment-mysql \
        2>/dev/null

    echo -e "${GREEN}✅ Docker containers stopped${NC}"
fi

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  SHUTDOWN COMPLETE${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo ""
