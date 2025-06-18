# API Testing Commands for Database Triggers

## Prerequisites
- Start your Spring Boot application
- Login as ADMIN user
- Get authentication token/session

## 1. Check Trigger Status
```bash
curl -X GET "http://localhost:8080/api/admin/triggers/status" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

## 2. Test All Triggers Automatically
```bash
curl -X POST "http://localhost:8080/api/admin/triggers/test" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

## 3. Test User-to-Employee Trigger
```bash
curl -X POST "http://localhost:8080/api/admin/triggers/test/user-to-employee" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=api.test1&email=api.test1@company.com"
```

## 4. Test Employee-to-User Trigger
```bash
curl -X POST "http://localhost:8080/api/admin/triggers/test/employee-to-user" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "employeeId=API001&fullName=API Test User&email=api.test2@company.com"
```

## 5. Reinitialize Triggers (if needed)
```bash
curl -X POST "http://localhost:8080/api/admin/triggers/reinitialize" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

## Alternative: Using Browser/Postman

### Base URL: `http://localhost:8080`

### Login first to get session:
```
POST /auth/login
{
  "username": "admin",
  "password": "admin123"
}
```

### Then use the trigger endpoints:
- `GET /api/admin/triggers/status`
- `POST /api/admin/triggers/test`
- `POST /api/admin/triggers/reinitialize`
