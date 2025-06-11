# Employee Management System

## 📋 Overview
A comprehensive Java-based web application built with Spring Boot and Thymeleaf for managing employee records, attendance, performance reviews, and generating reports. The system provides role-based access control and a modern, responsive user interface.

## 🚀 Features
- **Employee Management**: Complete CRUD operations for employee records
- **Authentication & Authorization**: Role-based access (Admin/HR) with Spring Security
- **Dashboard**: Real-time statistics and quick actions
- **Responsive UI**: Modern sidebar navigation with TailwindCSS
- **Database Integration**: MySQL with Spring Data JPA
- **Search & Filter**: Advanced employee search capabilities
- **Data Validation**: Comprehensive input validation with user-friendly error messages

## 🛠️ Technology Stack
- **Backend**: Spring Boot 3.5.0, Spring Security, Spring Data JPA
- **Frontend**: Thymeleaf, TailwindCSS, FontAwesome
- **Database**: MySQL 8.0
- **Build Tool**: Maven
- **Java Version**: 17+

## 📁 Project Structure
```
src/main/java/com/employeemanagementsystem/v1/
├── V1Application.java                 # Main Spring Boot application
├── config/
│   ├── SecurityConfig.java           # Spring Security configuration
│   └── DataLoader.java               # Initial data loader
├── controller/
│   ├── AuthController.java           # Authentication endpoints
│   ├── DashboardController.java      # Dashboard management
│   └── EmployeeController.java       # Employee CRUD operations
├── entity/
│   ├── User.java                     # User entity with roles
│   ├── Employee.java                 # Employee entity
│   ├── Attendance.java               # Attendance tracking
│   ├── LeaveRequest.java             # Leave management
│   └── PerformanceReview.java        # Performance evaluations
├── repository/
│   ├── UserRepository.java           # User data access
│   ├── EmployeeRepository.java       # Employee data access
│   ├── AttendanceRepository.java     # Attendance data access
│   ├── LeaveRequestRepository.java   # Leave data access
│   └── PerformanceReviewRepository.java # Performance data access
└── service/
    ├── UserService.java              # User business logic
    └── EmployeeService.java          # Employee business logic

src/main/resources/
├── application.properties            # Application configuration
└── templates/
    ├── dashboard.html                # Dashboard page
    ├── auth/
    │   ├── login.html               # Login page
    │   └── register.html            # User registration
    ├── employees/
    │   ├── list.html                # Employee listing
    │   └── form.html                # Employee add/edit form
    └── layout/
        └── base.html                # Base layout template
```

## 🔧 Setup & Installation

### Prerequisites
- Java 17 or higher
- MySQL 8.0
- Maven 3.6+

### Database Setup
1. Create MySQL database:
```sql
CREATE DATABASE EMS;
CREATE USER 'venlit'@'localhost' IDENTIFIED BY 'venlit';
GRANT ALL PRIVILEGES ON EMS.* TO 'venlit'@'localhost';
FLUSH PRIVILEGES;
```

### Application Setup
1. Clone the repository
2. Configure database connection in `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/EMS?useSSL=false&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=convertToNull
spring.datasource.username=venlit
spring.datasource.password=venlit
```

3. Build and run the application:
```bash
mvn clean compile
mvn spring-boot:run
```

4. Access the application at `http://localhost:8080`

## 👤 Default Users
The system comes with pre-configured users:

| Username | Password | Role  | Description |
|----------|----------|-------|-------------|
| admin    | admin123 | ADMIN | Full system access |
| hr       | hr123    | HR    | Employee management access |

## 🎯 Key Features

### Authentication & Security
- Spring Security integration with BCrypt password encoding
- Role-based access control (ADMIN/HR)
- Session management with automatic logout
- CSRF protection enabled

### Employee Management
- Complete CRUD operations for employee records
- Advanced search and filtering capabilities
- Employee status tracking (Active/Inactive/Terminated)
- Input validation with user-friendly error messages
- Pagination support for large datasets

### Dashboard
- Real-time employee statistics
- Quick action buttons for common operations
- Department overview
- System status indicators

### User Interface
- Modern sidebar navigation
- Responsive design with TailwindCSS
- Mobile-friendly interface
- Professional color scheme and typography
- FontAwesome icons for enhanced UX

## 🗃️ Database Schema

### Core Entities
- **users**: Authentication and authorization
- **employees**: Employee information and records
- **attendance**: Daily attendance tracking
- **leave_requests**: Leave applications and approvals
- **performance_reviews**: Employee performance evaluations

### Relationships
- User ↔ Role (Enum-based)
- Employee → Attendance (One-to-Many)
- Employee → LeaveRequest (One-to-Many)
- Employee → PerformanceReview (One-to-Many)

## 🔐 Security Features
- Password encryption using BCrypt
- Role-based page access restrictions
- CSRF protection
- Session timeout management
- Secure logout functionality

## 🎨 UI Components
- Responsive sidebar navigation
- Statistics cards with hover effects
- Data tables with search and pagination
- Modal forms for data entry
- Alert messages for user feedback
- Loading states and transitions

## 📊 Sample Data
The application includes sample data for testing:
- 2 default users (admin/hr)
- 5 sample employees across different departments
- Realistic employee data with proper relationships

## 🚦 Current Status
✅ **Completed Features:**
- Project setup and configuration
- Database design and JPA entities
- Authentication and role management
- Employee CRUD operations
- Dashboard with statistics
- Responsive UI with sidebar navigation
- Form validation and error handling
- Sample data initialization

## 🔮 Future Enhancements
- Attendance tracking module
- Leave management system
- Performance review workflow
- Report generation (PDF/Excel)
- Email notifications
- File upload for employee documents
- Advanced analytics and charts

## 🤝 Contributing
1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License
This project is licensed under the MIT License.

## 🆘 Support
For support and questions:
- Email: support@company.com
- Phone: (555) 123-4567

---
**Employee Management System v1.0** - Built with ❤️ using Spring Boot
