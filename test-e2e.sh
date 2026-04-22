#!/bin/bash

# ═══════════════════════════════════════════════════════
#  SPARE PARTS SHOP — END-TO-END AUTOMATED TESTS
# ═══════════════════════════════════════════════════════

set -e  # Exit on any error

# ─── Service URLs ───────────────────────────────────────
PRODUCT_URL="http://localhost:8081"
INVENTORY_URL="http://localhost:8082"
CUSTOMER_URL="http://localhost:8083"
ORDER_URL="http://localhost:8084"
BILLING_URL="http://localhost:8085"

# ─── Colors for output ──────────────────────────────────
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# ─── Counters ───────────────────────────────────────────
TESTS_PASSED=0
TESTS_FAILED=0

# ─── Helper Functions ───────────────────────────────────

print_header() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
}

print_test() {
    echo ""
    echo -e "${YELLOW}▶ TEST: $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
    TESTS_PASSED=$((TESTS_PASSED + 1))
}

print_failure() {
    echo -e "${RED}❌ $1${NC}"
    TESTS_FAILED=$((TESTS_FAILED + 1))
}

print_info() {
    echo -e "   $1"
}

check_service() {
    local url=$1
    local name=$2
    if curl -s --max-time 3 "$url/actuator/health" > /dev/null 2>&1; then
        return 0
    elif curl -s --max-time 3 "$url" > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# ─── Pre-flight Checks ──────────────────────────────────

print_header "PRE-FLIGHT: Checking Services"

print_test "Checking Product Service ($PRODUCT_URL)"
if check_service "$PRODUCT_URL"; then
    print_success "Product Service is UP"
else
    print_failure "Product Service is DOWN. Start it and retry."
    exit 1
fi

print_test "Checking Inventory Service ($INVENTORY_URL)"
if check_service "$INVENTORY_URL"; then
    print_success "Inventory Service is UP"
else
    print_failure "Inventory Service is DOWN. Start it and retry."
    exit 1
fi

print_test "Checking Customer Service ($CUSTOMER_URL)"
if check_service "$CUSTOMER_URL"; then
    print_success "Customer Service is UP"
else
    print_failure "Customer Service is DOWN. Start it and retry."
    exit 1
fi

print_test "Checking Order Service ($ORDER_URL)"
if check_service "$ORDER_URL"; then
    print_success "Order Service is UP"
else
    print_failure "Order Service is DOWN. Start it and retry."
    exit 1
fi

print_test "Checking Billing Service ($BILLING_URL)"
if check_service "$BILLING_URL"; then
    print_success "Billing Service is UP"
else
    print_failure "Billing Service is DOWN. Start it and retry."
    exit 1
fi

# ─── TEST 1: Create Product ─────────────────────────────

print_header "TEST 1: Product Service"

print_test "Create product"
RANDOM_PART="BOS-OF-$RANDOM"
PRODUCT_RESPONSE=$(curl -s -X POST "$PRODUCT_URL/api/v1/products" \
    -H "Content-Type: application/json" \
    -d "{
        \"name\": \"Oil Filter Premium\",
        \"description\": \"Automated test product\",
        \"brand\": \"Bosch\",
        \"category\": \"FILTER\",
        \"partNumber\": \"$RANDOM_PART\",
        \"price\": 250.00,
        \"vehicleCompatibility\": \"Maruti Swift\"
    }")

PRODUCT_ID=$(echo "$PRODUCT_RESPONSE" | jq -r '.data.id // empty')

if [ -z "$PRODUCT_ID" ]; then
    print_failure "Failed to create product"
    echo "$PRODUCT_RESPONSE" | jq
    exit 1
fi
print_success "Product created"
print_info "ID: $PRODUCT_ID"
print_info "Part Number: $RANDOM_PART"

print_test "Get product by ID"
GET_RESPONSE=$(curl -s "$PRODUCT_URL/api/v1/products/$PRODUCT_ID")
FETCHED_NAME=$(echo "$GET_RESPONSE" | jq -r '.data.name')

if [ "$FETCHED_NAME" = "Oil Filter Premium" ]; then
    print_success "Product fetched successfully"
else
    print_failure "Product fetch failed"
    echo "$GET_RESPONSE" | jq
fi

# ─── TEST 2: Create Customer ────────────────────────────

print_header "TEST 2: Customer Service"

print_test "Create RETAIL customer"
RANDOM_PHONE="9$RANDOM$RANDOM"
RANDOM_PHONE="${RANDOM_PHONE:0:10}"
CUSTOMER_RESPONSE=$(curl -s -X POST "$CUSTOMER_URL/api/v1/customers" \
    -H "Content-Type: application/json" \
    -d "{
        \"name\": \"Auto Test Customer\",
        \"email\": \"test_$RANDOM@example.com\",
        \"phone\": \"$RANDOM_PHONE\",
        \"address\": \"Delhi\",
        \"city\": \"Delhi\",
        \"customerType\": \"RETAIL\"
    }")

CUSTOMER_ID=$(echo "$CUSTOMER_RESPONSE" | jq -r '.data.id // empty')

if [ -z "$CUSTOMER_ID" ]; then
    print_failure "Failed to create customer"
    echo "$CUSTOMER_RESPONSE" | jq
    exit 1
fi
print_success "RETAIL customer created"
print_info "ID: $CUSTOMER_ID"
print_info "Phone: $RANDOM_PHONE"

print_test "Create WHOLESALE customer"
WHOLESALE_PHONE="8$RANDOM$RANDOM"
WHOLESALE_PHONE="${WHOLESALE_PHONE:0:10}"
WHOLESALE_RESPONSE=$(curl -s -X POST "$CUSTOMER_URL/api/v1/customers" \
    -H "Content-Type: application/json" \
    -d "{
        \"name\": \"Sharma Motors\",
        \"email\": \"sharma_$RANDOM@motors.com\",
        \"phone\": \"$WHOLESALE_PHONE\",
        \"address\": \"Karol Bagh\",
        \"city\": \"Delhi\",
        \"customerType\": \"WHOLESALE\",
        \"businessName\": \"Sharma Motors\",
        \"gstNumber\": \"07AABCU$RANDOM\",
        \"creditLimit\": 50000.00
    }")

WHOLESALE_ID=$(echo "$WHOLESALE_RESPONSE" | jq -r '.data.id // empty')

if [ -z "$WHOLESALE_ID" ]; then
    print_failure "Failed to create wholesale customer"
    echo "$WHOLESALE_RESPONSE" | jq
    exit 1
fi
print_success "WHOLESALE customer created"
print_info "ID: $WHOLESALE_ID"

# ─── TEST 3: Create Inventory ────────────────────────────

print_header "TEST 3: Inventory Service"

print_test "Create inventory for product"
INVENTORY_RESPONSE=$(curl -s -X POST "$INVENTORY_URL/api/v1/inventory" \
    -H "Content-Type: application/json" \
    -d "{
        \"productId\": \"$PRODUCT_ID\",
        \"stockQuantity\": 100,
        \"reorderLevel\": 10,
        \"reorderQuantity\": 50,
        \"warehouseLocation\": \"Rack-A-12\",
        \"unitOfMeasure\": \"PIECE\"
    }")

INVENTORY_ID=$(echo "$INVENTORY_RESPONSE" | jq -r '.data.id // empty')

if [ -z "$INVENTORY_ID" ]; then
    print_failure "Failed to create inventory"
    echo "$INVENTORY_RESPONSE" | jq
    exit 1
fi
print_success "Inventory created with 100 units"

print_test "Check stock availability (5 units)"
CHECK_RESPONSE=$(curl -s "$INVENTORY_URL/api/v1/inventory/product/$PRODUCT_ID/check?quantity=5")
AVAILABLE=$(echo "$CHECK_RESPONSE" | jq -r '.data')

if [ "$AVAILABLE" = "true" ]; then
    print_success "Stock available for 5 units"
else
    print_failure "Stock check failed"
fi

print_test "Check excess stock (99999 units)"
EXCESS_RESPONSE=$(curl -s "$INVENTORY_URL/api/v1/inventory/product/$PRODUCT_ID/check?quantity=99999")
EXCESS_AVAILABLE=$(echo "$EXCESS_RESPONSE" | jq -r '.data')

if [ "$EXCESS_AVAILABLE" = "false" ]; then
    print_success "Correctly rejected excess quantity"
else
    print_failure "Excess stock check failed"
fi

# ─── TEST 4: Create Order (SAGA PATTERN!) ────────────────

print_header "TEST 4: Order Service (Saga Pattern)"

print_test "Create order with 5 units"
ORDER_RESPONSE=$(curl -s -X POST "$ORDER_URL/api/v1/orders" \
    -H "Content-Type: application/json" \
    -d "{
        \"customerId\": \"$CUSTOMER_ID\",
        \"paymentMode\": \"CASH\",
        \"orderItems\": [
            {
                \"productId\": \"$PRODUCT_ID\",
                \"quantity\": 5
            }
        ],
        \"notes\": \"Automated test order\"
    }")

ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.data.id // empty')
ORDER_NUMBER=$(echo "$ORDER_RESPONSE" | jq -r '.data.orderNumber // empty')
ORDER_STATUS=$(echo "$ORDER_RESPONSE" | jq -r '.data.status // empty')
ORDER_TOTAL=$(echo "$ORDER_RESPONSE" | jq -r '.data.totalAmount // empty')

if [ -z "$ORDER_ID" ]; then
    print_failure "Failed to create order"
    echo "$ORDER_RESPONSE" | jq
    exit 1
fi
print_success "Order created: $ORDER_NUMBER"
print_info "Status: $ORDER_STATUS"
print_info "Total: $ORDER_TOTAL"

if [ "$ORDER_STATUS" = "CONFIRMED" ]; then
    print_success "Order status is CONFIRMED"
else
    print_failure "Expected CONFIRMED, got $ORDER_STATUS"
fi

# Verify total is 1250 (5 x 250)
if [ "$ORDER_TOTAL" = "1250.00" ]; then
    print_success "Total amount is correct (1250.00)"
else
    print_failure "Expected 1250.00, got $ORDER_TOTAL"
fi

print_test "Verify stock was reduced"
sleep 1
STOCK_RESPONSE=$(curl -s "$INVENTORY_URL/api/v1/inventory/product/$PRODUCT_ID")
CURRENT_STOCK=$(echo "$STOCK_RESPONSE" | jq -r '.data.stockQuantity')

if [ "$CURRENT_STOCK" = "95" ]; then
    print_success "Stock correctly reduced: 100 → 95"
else
    print_failure "Expected 95, got $CURRENT_STOCK"
fi

print_test "Try order with insufficient stock (should fail)"
FAIL_RESPONSE=$(curl -s -X POST "$ORDER_URL/api/v1/orders" \
    -H "Content-Type: application/json" \
    -d "{
        \"customerId\": \"$CUSTOMER_ID\",
        \"paymentMode\": \"CASH\",
        \"orderItems\": [
            {\"productId\": \"$PRODUCT_ID\", \"quantity\": 99999}
        ]
    }")

FAIL_STATUS=$(echo "$FAIL_RESPONSE" | jq -r '.status // empty')

if [ "$FAIL_STATUS" = "400" ]; then
    print_success "Correctly rejected insufficient stock (400)"
else
    print_failure "Expected 400 error, got: $FAIL_STATUS"
fi

print_test "Try CREDIT for RETAIL customer (should fail)"
CREDIT_FAIL_RESPONSE=$(curl -s -X POST "$ORDER_URL/api/v1/orders" \
    -H "Content-Type: application/json" \
    -d "{
        \"customerId\": \"$CUSTOMER_ID\",
        \"paymentMode\": \"CREDIT\",
        \"orderItems\": [
            {\"productId\": \"$PRODUCT_ID\", \"quantity\": 1}
        ]
    }")

CREDIT_FAIL_STATUS=$(echo "$CREDIT_FAIL_RESPONSE" | jq -r '.status // empty')

if [ "$CREDIT_FAIL_STATUS" = "400" ]; then
    print_success "Correctly rejected CREDIT for RETAIL"
else
    print_failure "Expected 400 error, got: $CREDIT_FAIL_STATUS"
fi

print_test "Create CREDIT order for WHOLESALE (should succeed)"
CREDIT_ORDER_RESPONSE=$(curl -s -X POST "$ORDER_URL/api/v1/orders" \
    -H "Content-Type: application/json" \
    -d "{
        \"customerId\": \"$WHOLESALE_ID\",
        \"paymentMode\": \"CREDIT\",
        \"orderItems\": [
            {\"productId\": \"$PRODUCT_ID\", \"quantity\": 3}
        ]
    }")

CREDIT_ORDER_ID=$(echo "$CREDIT_ORDER_RESPONSE" | jq -r '.data.id // empty')
CREDIT_ORDER_NUMBER=$(echo "$CREDIT_ORDER_RESPONSE" | jq -r '.data.orderNumber // empty')

if [ -n "$CREDIT_ORDER_ID" ]; then
    print_success "CREDIT order created for WHOLESALE: $CREDIT_ORDER_NUMBER"
else
    print_failure "CREDIT order failed"
    echo "$CREDIT_ORDER_RESPONSE" | jq
fi

# ─── TEST 5: Create Invoice ──────────────────────────────

print_header "TEST 5: Billing Service"

print_test "Create invoice for CASH order"
INVOICE_RESPONSE=$(curl -s -X POST "$BILLING_URL/api/v1/invoices" \
    -H "Content-Type: application/json" \
    -d "{
        \"orderId\": \"$ORDER_ID\",
        \"orderNumber\": \"$ORDER_NUMBER\",
        \"customerId\": \"$CUSTOMER_ID\",
        \"subtotal\": 1250.00,
        \"paymentMode\": \"CASH\",
        \"notes\": \"Automated test invoice\"
    }")

INVOICE_ID=$(echo "$INVOICE_RESPONSE" | jq -r '.data.id // empty')
INVOICE_NUMBER=$(echo "$INVOICE_RESPONSE" | jq -r '.data.invoiceNumber // empty')
TAX_AMOUNT=$(echo "$INVOICE_RESPONSE" | jq -r '.data.taxAmount // empty')
GRAND_TOTAL=$(echo "$INVOICE_RESPONSE" | jq -r '.data.grandTotal // empty')
PAYMENT_STATUS=$(echo "$INVOICE_RESPONSE" | jq -r '.data.paymentStatus // empty')

if [ -z "$INVOICE_ID" ]; then
    print_failure "Failed to create invoice"
    echo "$INVOICE_RESPONSE" | jq
    exit 1
fi
print_success "Invoice created: $INVOICE_NUMBER"
print_info "Tax (18%): $TAX_AMOUNT"
print_info "Grand Total: $GRAND_TOTAL"
print_info "Status: $PAYMENT_STATUS"

if [ "$TAX_AMOUNT" = "225.00" ]; then
    print_success "Tax calculated correctly (225.00)"
else
    print_failure "Expected tax 225.00, got $TAX_AMOUNT"
fi

if [ "$GRAND_TOTAL" = "1475.00" ]; then
    print_success "Grand total correct (1475.00)"
else
    print_failure "Expected grand total 1475.00, got $GRAND_TOTAL"
fi

print_test "Record partial payment (500)"
PAY1_RESPONSE=$(curl -s -X POST "$BILLING_URL/api/v1/invoices/$INVOICE_ID/pay" \
    -H "Content-Type: application/json" \
    -d "{\"amount\": 500.00, \"paymentMode\": \"UPI\"}")

PAY1_STATUS=$(echo "$PAY1_RESPONSE" | jq -r '.data.paymentStatus // empty')
PAY1_PAID=$(echo "$PAY1_RESPONSE" | jq -r '.data.paidAmount // empty')

if [ "$PAY1_STATUS" = "PARTIAL" ]; then
    print_success "Partial payment recorded (500 of 1475)"
    print_info "Paid: $PAY1_PAID, Status: $PAY1_STATUS"
else
    print_failure "Expected PARTIAL, got $PAY1_STATUS"
fi

print_test "Record remaining payment (975)"
PAY2_RESPONSE=$(curl -s -X POST "$BILLING_URL/api/v1/invoices/$INVOICE_ID/pay" \
    -H "Content-Type: application/json" \
    -d "{\"amount\": 975.00, \"paymentMode\": \"UPI\"}")

PAY2_STATUS=$(echo "$PAY2_RESPONSE" | jq -r '.data.paymentStatus // empty')

if [ "$PAY2_STATUS" = "PAID" ]; then
    print_success "Invoice fully paid"
else
    print_failure "Expected PAID, got $PAY2_STATUS"
fi

print_test "Try to pay already-paid invoice (should fail)"
PAY_AGAIN_RESPONSE=$(curl -s -X POST "$BILLING_URL/api/v1/invoices/$INVOICE_ID/pay" \
    -H "Content-Type: application/json" \
    -d "{\"amount\": 100.00, \"paymentMode\": \"CASH\"}")

PAY_AGAIN_STATUS=$(echo "$PAY_AGAIN_RESPONSE" | jq -r '.status // empty')

if [ "$PAY_AGAIN_STATUS" = "400" ]; then
    print_success "Correctly rejected payment on paid invoice"
else
    print_failure "Expected 400, got $PAY_AGAIN_STATUS"
fi

# ─── TEST 6: Cancel Order (Reverse Saga) ────────────────

print_header "TEST 6: Order Cancellation (Reverse Saga)"

print_test "Cancel the CREDIT order"
CANCEL_RESPONSE=$(curl -s -X POST "$ORDER_URL/api/v1/orders/$CREDIT_ORDER_ID/cancel")
CANCEL_SUCCESS=$(echo "$CANCEL_RESPONSE" | jq -r '.success // empty')

if [ "$CANCEL_SUCCESS" = "true" ]; then
    print_success "Order cancelled"
else
    print_failure "Cancel failed"
    echo "$CANCEL_RESPONSE" | jq
fi

print_test "Verify stock was restored"
sleep 1
RESTORE_RESPONSE=$(curl -s "$INVENTORY_URL/api/v1/inventory/product/$PRODUCT_ID")
RESTORED_STOCK=$(echo "$RESTORE_RESPONSE" | jq -r '.data.stockQuantity')

# We had: 100 - 5 (first order) - 3 (credit order) = 92
# After cancel of credit order (+3): 95
if [ "$RESTORED_STOCK" = "95" ]; then
    print_success "Stock correctly restored to 95"
else
    print_failure "Expected 95, got $RESTORED_STOCK"
fi

# ─── SUMMARY ─────────────────────────────────────────────

print_header "TEST SUMMARY"

echo ""
echo -e "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests Failed: ${RED}$TESTS_FAILED${NC}"
echo ""

echo "Test Artifacts (for manual verification):"
echo "  Product ID:      $PRODUCT_ID"
echo "  Customer ID:     $CUSTOMER_ID"
echo "  Wholesale ID:    $WHOLESALE_ID"
echo "  Inventory ID:    $INVENTORY_ID"
echo "  Order ID:        $ORDER_ID"
echo "  Credit Order ID: $CREDIT_ORDER_ID"
echo "  Invoice ID:      $INVOICE_ID"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  ALL TESTS PASSED ✅${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
    exit 0
else
    echo -e "${RED}═══════════════════════════════════════════════════════${NC}"
    echo -e "${RED}  SOME TESTS FAILED ❌${NC}"
    echo -e "${RED}═══════════════════════════════════════════════════════${NC}"
    exit 1
fi
