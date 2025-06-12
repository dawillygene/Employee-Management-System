#!/bin/bash

# Test script to login and check leave page with pagination
echo "Testing Leave Page with Pagination..."

# Step 1: Get the login page to extract CSRF token
echo "Step 1: Getting login page..."
curl -c cookies.txt -s http://localhost:8080/login > login_page.html

# Extract CSRF token from the login page
csrf_token=$(grep -o 'name="_csrf" value="[^"]*"' login_page.html | sed 's/name="_csrf" value="//' | sed 's/"//')
echo "CSRF Token: $csrf_token"

# Step 2: Login with admin credentials
echo "Step 2: Logging in as admin..."
login_response=$(curl -b cookies.txt -c cookies.txt -s -w "%{http_code}" \
  -d "username=admin&password=admin123&_csrf=$csrf_token" \
  -X POST http://localhost:8080/login)

echo "Login response code: $login_response"

# Step 3: Test leave page with different page sizes
echo "Step 3: Testing leave page with size=5..."
leave_response_5=$(curl -b cookies.txt -s -w "%{http_code}" -o leave_size_5.html http://localhost:8080/leave?size=5)
echo "Leave page (size=5) response code: $leave_response_5"

echo "Step 4: Testing leave page with size=10..."
leave_response_10=$(curl -b cookies.txt -s -w "%{http_code}" -o leave_size_10.html http://localhost:8080/leave?size=10)
echo "Leave page (size=10) response code: $leave_response_10"

# Check if the pages contain the expected pagination content
echo "Step 5: Checking pagination content..."
if grep -q "5 per page" leave_size_5.html; then
    echo "✓ Page size dropdown found in leave page"
else
    echo "✗ Page size dropdown NOT found in leave page"
fi

if grep -q "Showing" leave_size_5.html && grep -q "results" leave_size_5.html; then
    echo "✓ Pagination info found in leave page"
else
    echo "✗ Pagination info NOT found in leave page"
fi

# Clean up
rm -f login_page.html leave_size_5.html leave_size_10.html

echo "Test completed!"
