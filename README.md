## Hệ Thống Đấu Giá Trực Tuyến - Nhóm 9 

> Đại học Công nghệ, ĐHQGHN  
> Xây dựng hệ thống đấu giá trực tuyến theo kiến trúc Client–Server, tương tự mô hình eBay Auctions.

---

## 📋 Mô tả bài toán & Phạm vi hệ thống

Hệ thống cho phép nhiều người dùng cùng tham gia cạnh tranh giá để mua sản phẩm trong một khoảng thời gian xác định. Người bán đăng sản phẩm, người mua đặt giá cạnh tranh, và giá bán cuối cùng được xác định tự động khi phiên kết thúc.

| Thành phần | Vai trò |
|---|---|
| **Server** | Xử lý nghiệp vụ, quản lý phiên đấu giá, lưu trữ dữ liệu vào MySQL |
| **Client (JavaFX)** | Giao diện đồ họa cho Bidder và Seller, kết nối với Server qua TCP Socket |
| **Database** | MySQL — lưu người dùng, phiên đấu giá, lịch sử bid |
| **LAN Discovery** | Client tự động dò IP Server trong cùng mạng Wi-Fi/LAN qua UDP Broadcast |

Giao tiếp Client–Server sử dụng **JSON qua TCP Socket** (cổng `5000`). Server tự động dò tìm bởi Client qua **UDP Broadcast** (cổng `8888`), không cần nhập IP thủ công.

---

## 🛠️ Công nghệ sử dụng

| Hạng mục | Chi tiết |
|---|---|
| Ngôn ngữ | Java 17+ |
| GUI | JavaFX (FXML) |
| JSON | Google Gson |
| Database | MySQL 8.x |
| JDBC Driver | MySQL Connector/J |
| Build tool | Maven (module `vn.edu.uet.daugia`) |
| Unit Test | JUnit 5 (Jupiter) |

---

## ⚙️ Yêu cầu cài đặt & Môi trường

### Phần mềm bắt buộc
- **JDK 17** trở lên — [Tải tại đây](https://adoptium.net/)
- **JavaFX SDK 17+** — [Tải tại đây](https://gluonhq.com/products/javafx/) *(chỉ cần nếu không dùng Maven)*
- **Maven 3.8+** — [Tải tại đây](https://maven.apache.org/download.cgi)
- **MySQL Server 8.x** — [Tải tại đây](https://dev.mysql.com/downloads/mysql/)

### Kiểm tra cài đặt

```bash
java -version       # Yêu cầu >= 17
mvn -version        # Yêu cầu >= 3.8
mysql --version     # Yêu cầu >= 8.0
```

---

## 🗄️ Cấu hình Database

### Bước 1: Tạo database và bảng

Đăng nhập MySQL và chạy các lệnh sau:

```sql
CREATE DATABASE IF NOT EXISTS auction_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE auction_db;

CREATE TABLE users (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email    VARCHAR(255),
    role     VARCHAR(20)  NOT NULL DEFAULT 'BIDDER',
    balance  DOUBLE       DEFAULT 1000000
);

CREATE TABLE auctions (
    id           VARCHAR(50)    PRIMARY KEY,
    itemName     VARCHAR(255)   NOT NULL,
    description  TEXT,
    sellerName   VARCHAR(100)   NOT NULL,
    startPrice   DOUBLE         NOT NULL,
    currentPrice DOUBLE         DEFAULT 0,
    startTime    VARCHAR(50),
    endTime      VARCHAR(50)    NOT NULL,
    status       VARCHAR(20)    NOT NULL DEFAULT 'RUNNING',
    winner       VARCHAR(100),
    image_url    VARCHAR(500)   DEFAULT '',
    max_price    DOUBLE         DEFAULT 0
);

CREATE TABLE bids (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    auctionId VARCHAR(50)  NOT NULL,
    bidderId  VARCHAR(100) NOT NULL,
    price     DOUBLE       NOT NULL,
    bidTime   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
```

### Bước 2: Cấu hình kết nối

Mở file `src/main/java/vn/edu/uet/daugia/database/DatabaseConnection.java` và chỉnh thông tin theo môi trường của bạn:

```java
private static final String URL      = "jdbc:mysql://localhost:3306/auction_db";
private static final String USER     = "root";       // ← Thay tên user của bạn
private static final String PASSWORD = "123456";     // ← Thay mật khẩu của bạn
```

---

## 📁 Cấu trúc thư mục

```
BTL_LTNC_DauGia_2026/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── module-info.java                         # Khai báo module Java
│   │   │   └── vn/edu/uet/daugia/
│   │   │       ├── client/
│   │   │       │   ├── AuctionClient.java               # Điểm khởi động Client (JavaFX)
│   │   │       │   ├── Controller/                      # Các JavaFX Controller
│   │   │       │   │   ├── LoginController.java
│   │   │       │   │   ├── RegisterController.java
│   │   │       │   │   ├── AuctionListController.java
│   │   │       │   │   ├── BiddingRoomController.java   # Phòng đấu giá + LineChart realtime
│   │   │       │   │   ├── ProductCardController.java
│   │   │       │   │   ├── ProductDetailController.java
│   │   │       │   │   └── SellerDashboardController.java
│   │   │       │   ├── model/
│   │   │       │   │   ├── BidHistoryRow.java
│   │   │       │   │   └── Product.java
│   │   │       │   ├── network/
│   │   │       │   │   ├── NetworkClient.java           # TCP Socket tới Server
│   │   │       │   │   └── ClientDiscovery.java         # UDP Broadcast dò IP Server
│   │   │       │   └── util/
│   │   │       │       ├── AlertUtil.java
│   │   │       │       ├── AppState.java
│   │   │       │       ├── DateTimeParseUtil.java
│   │   │       │       ├── SceneManager.java
│   │   │       │       └── SessionManager.java
│   │   │       ├── server/
│   │   │       │   ├── AuctionServer.java               # Điểm khởi động Server
│   │   │       │   ├── AuctionManager.java              # Singleton quản lý phiên (RAM)
│   │   │       │   ├── AuctionObserver.java             # Interface Observer (realtime bid)
│   │   │       │   ├── AuctionService.java              # Business logic + auto-close scheduler
│   │   │       │   ├── ClientHandler.java               # Xử lý từng kết nối TCP (thread riêng)
│   │   │       │   ├── ServerDiscovery.java             # UDP Broadcast phản hồi Client
│   │   │       │   ├── UserManager.java                 # Tìm Bidder từ DB
│   │   │       │   └── TestAuctionService.java
│   │   │       ├── database/
│   │   │       │   └── DatabaseConnection.java          # Kết nối MySQL
│   │   │       └── shared/
│   │   │           ├── exception/
│   │   │           │   ├── AuctionClosedException.java
│   │   │           │   ├── AuthenticationException.java
│   │   │           │   └── InvalidBidException.java
│   │   │           └── model/
│   │   │               ├── Auction.java                 # Model chính, dùng ReentrantLock
│   │   │               ├── AuctionStatus.java           # Enum: OPEN/RUNNING/FINISHED/PAID/CANCELED
│   │   │               ├── BidMessage.java
│   │   │               ├── BidTransaction.java
│   │   │               ├── LoginMessage.java
│   │   │               ├── RegisterMessage.java
│   │   │               ├── entity/Entity.java           # Abstract base class
│   │   │               ├── item/
│   │   │               │   ├── Item.java                # Abstract
│   │   │               │   ├── Electronics.java
│   │   │               │   ├── Art.java
│   │   │               │   └── Vehicle.java
│   │   │               └── user/
│   │   │                   ├── User.java                # Abstract
│   │   │                   ├── Bidder.java
│   │   │                   ├── Seller.java
│   │   │                   └── Admin.java
│   │   └── resources/
│   │       └── view/                                    # Các file FXML
│   │           ├── Login.fxml
│   │           ├── Register.fxml
│   │           ├── AuctionList.fxml
│   │           ├── BiddingRoom.fxml
│   │           ├── ProductCard.fxml
│   │           ├── ProductDetail.fxml
│   │           └── SellerDashboard.fxml
│   └── test/
│       └── java/vn/edu/uet/daugia/shared/model/
│           └── AuctionTest.java                         # 7 test case JUnit 5
└── pom.xml
```

---

## 🚀 Hướng dẫn chạy chương trình

> ⚠️ **Quan trọng: Phải khởi động Server TRƯỚC, rồi mới chạy Client.**

### Bước 0: Clone và build dự án

```bash
# Clone repository
git clone https://github.com/<your-org>/BTL_LTNC_DauGia_2026.git
cd BTL_LTNC_DauGia_2026

# Build toàn bộ (bỏ qua test trong lần build đầu)
mvn clean package -DskipTests
```

---

### Bước 1: Khởi động Server

Server chạy trên cổng TCP **5000** và lắng nghe UDP Broadcast trên cổng **8888**.

**Windows:**
```cmd
mvn exec:java -Dexec.mainClass="vn.edu.uet.daugia.server.AuctionServer"
```

**macOS / Linux:**
```bash
mvn exec:java -Dexec.mainClass="vn.edu.uet.daugia.server.AuctionServer"
```

Hoặc chạy trực tiếp bằng JAR (sau khi build xong):

```bash
# Windows
java -cp "target/classes;target/dependency/*" vn.edu.uet.daugia.server.AuctionServer

# macOS / Linux
java -cp "target/classes:target/dependency/*" vn.edu.uet.daugia.server.AuctionServer
```

Server khởi động thành công khi bạn thấy:
```
=== HỆ THỐNG ĐẤU GIÁ SERVER ĐANG KHỞI ĐỘNG ===
[LOAD] Đã nạp X phiên RUNNING từ database.
[SCHEDULER] Đã bật bộ tự động đóng phiên (quét mỗi 10 giây).
Đang mở cổng 5000 và chờ Client kết nối...
```

---

### Bước 2: Khởi động Client (JavaFX)

Mở terminal **mới** (giữ nguyên terminal đang chạy Server), chạy:

**Windows:**
```cmd
mvn javafx:run
```

**macOS / Linux:**
```bash
mvn javafx:run
```

Client sẽ **tự động dò tìm IP Server** trong mạng LAN qua UDP Broadcast. Nếu không tìm thấy Server trên mạng, Client tự động kết nối về `127.0.0.1` (localhost).

> 💡 **Chạy nhiều Client cùng lúc:** Mở thêm terminal và chạy `mvn javafx:run` thêm lần nữa để mô phỏng nhiều người dùng đồng thời.

---

### Bước 3: Chạy Unit Test

```bash
# Chạy tất cả test
mvn test

# Xem kết quả chi tiết
mvn test -Dsurefire.useFile=false
```

---

## ✅ Danh sách chức năng đã 

### Chức năng bắt buộc

| # | Chức năng | Trạng thái |
|---|---|
| 1 | Đăng ký tài khoản (Bidder / Seller) ✅ 
| 2 | Đăng nhập, phân quyền theo vai trò ✅ 
| 3 | Seller: Tạo phiên đấu giá (tên, mô tả, giá khởi điểm, thời gian, ảnh URL, giá mua đứt) ✅
| 4 | Seller: Sửa thông tin phiên đang RUNNING ✅
| 5 | Seller: Xóa phiên đấu giá ✅
| 6 | Seller Dashboard: Xem tất cả phiên đã tạo (mọi trạng thái) kèm người dẫn đầu / người thắng ✅
| 7 | Bidder: Xem danh sách phiên đang RUNNING ✅
| 8 | Bidder: Xem chi tiết sản phẩm ✅
| 9 | Bidder: Đặt giá — kiểm tra hợp lệ (phải cao hơn giá hiện tại) ✅ 
| 10 | Bidder: Mua đứt sản phẩm khi đặt giá đạt `max_price` ✅ 
| 11 | Tự động đóng phiên khi hết thời gian (scheduler quét 10 giây/lần)✅
| 12 | Xác định người thắng → FINISHED; không có bid → CANCELED ✅ 
| 13 | Vòng đời phiên: OPEN → RUNNING → FINISHED / CANCELED ✅ 
| 14 | Xử lý ngoại lệ: `InvalidBidException`, `AuctionClosedException`, `AuthenticationException` ✅
| 15 | Kiến trúc Client–Server qua TCP Socket, dữ liệu JSON ✅
| 16 | Client MVC: JavaFX + FXML Controller ✅
| 17 | Server MVC: ClientHandler → AuctionService → DatabaseConnection ✅

### Chức năng nâng cao

| # | Chức năng | Trạng thái |
|---|---|
| 18 | **Realtime Update (Observer Pattern):** Mỗi `ClientHandler` là một `AuctionObserver` — khi có bid mới, server push JSON `NEW_BID` tới **tất cả** client đang kết nối ngay lập tức, không cần polling ✅
| 19 | **Bid History Visualization:** Biểu đồ đường (JavaFX `LineChart`) hiển thị giá đấu theo thời gian thực trong phòng đấu giá ✅
| 20 | **Concurrent Bidding an toàn:** `placeBid()` dùng `ReentrantLock` — chống race condition, lost update, đảm bảo chỉ 1 người thắng ✅
| 21 | **LAN Auto-Discovery:** Client tự dò IP Server qua UDP Broadcast (`255.255.255.255:8888`), không cần cấu hình IP thủ công ✅
| 22 | **Persistent State Recovery:** Server khởi động lại tự nạp lại các phiên RUNNING từ MySQL vào RAM, không mất dữ liệu ✅ 


---

## Tài nguyên

| Mục | Link |
|---|---|
| 📄 Báo cáo PDF | *[]* |
| 🎥 Video Demo | *[]* |
| 📁 Repository | *[]* |

---

## Thành viên nhóm

| Họ tên | MSSV |
|---|---|
| *(Dương Đăng Tuấn)* | *(25021989)* |
| *(Phạm Thành Trung)* | *(25022034)* | 
| *(Trần Anh Tuấn)* | *(25021997)* | 
| *(Hoàng Anh Quân)* | *(25021957)* |
---

## Lưu ý khi chạy

- **Firewall:** Trên Windows, khi chạy Server lần đầu, Windows Firewall có thể hỏi quyền truy cập mạng — hãy cho phép cả mạng riêng (private) lẫn mạng công cộng (public) để LAN Discovery hoạt động đúng.
- **Cùng mạng LAN:** Client và Server phải kết nối vào cùng một mạng Wi-Fi hoặc LAN. Nếu chạy cùng máy, Client tự fallback về `127.0.0.1`.
- **Cổng:** Đảm bảo cổng `5000` (TCP) và `8888` (UDP) không bị chặn hoặc đang được dùng bởi ứng dụng khác.
- **Database:** Server **phải** kết nối được MySQL trước khi Client đăng nhập. Nếu MySQL chưa chạy, Server vẫn khởi động nhưng mọi thao tác đăng nhập/đấu giá sẽ thất bại.
  
