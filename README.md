## Hệ thống đấu giá trực tuyến - Nhóm 9 

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

Quá trình khởi chạy được chia làm 3 bước: Chuẩn bị CSDL, Chạy Server và Chạy Client.

Bước 1: Khởi tạo Cơ sở dữ liệu (MySQL)
Mở MySQL Workbench hoặc Terminal.

Chạy đoạn script SQL sau để tạo CSDL auction_db và các bảng cần thiết:
```sql

CREATE DATABASE IF NOT EXISTS auction_db;
USE auction_db;

CREATE TABLE users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role VARCHAR(20) NOT NULL,
    balance DOUBLE DEFAULT 1000000
);

CREATE TABLE auctions (
    id VARCHAR(50) PRIMARY KEY,
    itemName VARCHAR(255) NOT NULL,
    description TEXT,
    sellerName VARCHAR(50),
    startPrice DOUBLE NOT NULL,
    currentPrice DOUBLE DEFAULT NULL,
    startTime DATETIME NOT NULL,
    endTime DATETIME NOT NULL,
    status VARCHAR(20) DEFAULT 'OPEN',
    image_url VARCHAR(500),
    max_price DOUBLE DEFAULT 0,
    winner VARCHAR(50)
);

CREATE TABLE bids (
    id INT AUTO_INCREMENT PRIMARY KEY,
    auctionId VARCHAR(50) NOT NULL,
    bidderId VARCHAR(50) NOT NULL,
    price DOUBLE NOT NULL,
    bidTime DATETIME NOT NULL
);
```
### Bước 2: Cấu hình kết nối
```
Mở file src/vn/edu/uet/daugia/database/DatabaseConnection.java, kiểm tra và cấu hình lại thông tin đăng nhập MySQL cho khớp với máy của thầy/cô:

private static final String URL = "jdbc:mysql://localhost:3306/auction_db";
private static final String USER = "root";       // <-- Thay đổi nếu cần
private static final String PASSWORD = "123456"; // <-- Thay đổi nếu cần
```

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

>  **Chạy nhiều Client cùng lúc:** Mở thêm terminal và chạy `mvn javafx:run` thêm lần nữa để mô phỏng nhiều người dùng đồng thời.

---

### Bước 3: Chạy Unit Test

```bash
# Chạy tất cả test
mvn test

# Xem kết quả chi tiết
mvn test -Dsurefire.useFile=false
```

---

## ✅ Danh sách chức năng đã hoàn thành

### Chức năng bắt buộc

| # | Chức năng | Trạng thái |
|---|---|---|
| 1 | Đăng ký tài khoản (Bidder / Seller) | ✅ Hoàn thành |
| 2 | Đăng nhập, phân quyền theo vai trò | ✅ Hoàn thành |
| 3 | Seller: Tạo phiên đấu giá (tên, mô tả, giá khởi điểm, thời gian, ảnh URL, giá mua đứt) | ✅ Hoàn thành |
| 4 | Seller: Sửa thông tin phiên đang RUNNING | ✅ Hoàn thành |
| 5 | Seller: Xóa phiên đấu giá | ✅ Hoàn thành |
| 6 | Seller Dashboard: Xem tất cả phiên đã tạo (mọi trạng thái) kèm người dẫn đầu / người thắng | ✅ Hoàn thành |
| 7 | Bidder: Xem danh sách phiên đang RUNNING | ✅ Hoàn thành |
| 8 | Bidder: Xem chi tiết sản phẩm | ✅ Hoàn thành |
| 9 | Bidder: Đặt giá — kiểm tra hợp lệ (phải cao hơn giá hiện tại) | ✅ Hoàn thành |
| 10 | Bidder: Mua đứt sản phẩm khi đặt giá đạt `max_price` | ✅ Hoàn thành |
| 11 | Admin: Bảng điều khiển (Dashboard) xem thống kê tổng quan hệ thống (tổng user, tổng số phiên, trạng thái các phiên) | ✅ Hoàn thành |
| 12 | Admin: Xem danh sách toàn bộ phiên đấu giá trên hệ thống (bất kể trạng thái OPEN, RUNNING, FINISHED, CANCELED) | ✅ Hoàn thành |
| 13 | Admin: Quản lý phiên đấu giá (Hủy phiên đang chạy - CANCELED, Xóa vĩnh viễn phiên và lịch sử bid) | ✅ Hoàn thành |
| 14 | Admin: Quản lý tài khoản Bidder (Xem danh sách, Khóa/Mở khóa tài khoản `is_active`, Xóa tài khoản) | ✅ Hoàn thành |
| 15 | Admin: Quản lý tài khoản Seller (Xem danh sách, Khóa/Mở khóa tài khoản, Xóa toàn bộ sản phẩm/phiên của Seller vi phạm) | ✅ Hoàn thành |
| 16 | Bảo mật: Phân luồng giao diện và quyền truy cập độc lập cho Admin (`AdminDashboardController`), chặn Bidder/Seller truy cập trái phép | ✅ Hoàn thành |
| 17 | Tự động đóng phiên khi hết thời gian (scheduler quét 10 giây/lần) | ✅ Hoàn thành |
| 18 | Xác định người thắng → FINISHED; không có bid → CANCELED | ✅ Hoàn thành |
| 19 | Vòng đời phiên: OPEN → RUNNING → FINISHED / CANCELED | ✅ Hoàn thành |
| 20 | Xử lý ngoại lệ: `InvalidBidException`, `AuctionClosedException`, `AuthenticationException` | ✅ Hoàn thành |
| 21 | Kiến trúc Client–Server qua TCP Socket, dữ liệu JSON | ✅ Hoàn thành |
| 22 | Client MVC: JavaFX + FXML Controller | ✅ Hoàn thành |
| 23 | Server MVC: ClientHandler → AuctionService → DatabaseConnection | ✅ Hoàn thành |


### Chức năng nâng cao

| # | Chức năng | Trạng thái |
|---|---|---|
| 24 | **Realtime Update (Observer Pattern):** Mỗi `ClientHandler` là một `AuctionObserver` — khi có bid mới, server push JSON `NEW_BID` tới **tất cả** client đang kết nối ngay lập tức, không cần polling | ✅ Hoàn thành |
| 25 | **Bid History Visualization:** Biểu đồ đường (JavaFX `LineChart`) hiển thị giá đấu theo thời gian thực trong phòng đấu giá | ✅ Hoàn thành |
| 26 | **Concurrent Bidding an toàn:** `placeBid()` dùng `ReentrantLock` — chống race condition, lost update, đảm bảo chỉ 1 người thắng | ✅ Hoàn thành |
| 27 | **LAN Auto-Discovery:** Client tự dò IP Server qua UDP Broadcast (`255.255.255.255:8888`), không cần cấu hình IP thủ công | ✅ Hoàn thành |
| 28 | **Persistent State Recovery:** Server khởi động lại tự nạp lại các phiên RUNNING từ MySQL vào RAM, không mất dữ liệu | ✅ Hoàn thành |
| 29 | **Anti- sniping:** Phiên đấu giá còn 60s mà có bidder đặt giá thì phiên tự động gia hạn 60s | Hoàn thành | 

---

## 📎 Tài nguyên

| Mục | Link |
|---|---|
| 📄 Báo cáo PDF | *[Thêm link tại đây]* |
| 🎥 Video Demo | *[Thêm link tại đây]* |
| 📁 Repository | *[Thêm link GitHub tại đây]* |

---

## 👨‍💻 Thành viên nhóm

| Họ tên | MSSV |
|---|---|
| *Dương Đăng Tuấn* | *25021989*
| *Phạm Thành Trung* | *25022034* |
| *Trần Anh Tuấn* | *25021997* | 
| *Hoàng Anh Quân* | *25021957* |

---

## ⚠️ Lưu ý khi chạy

- **Firewall:** Trên Windows, khi chạy Server lần đầu, Windows Firewall có thể hỏi quyền truy cập mạng — hãy cho phép cả mạng riêng (private) lẫn mạng công cộng (public) để LAN Discovery hoạt động đúng.
- **Cùng mạng LAN:** Client và Server phải kết nối vào cùng một mạng Wi-Fi hoặc LAN. Nếu chạy cùng máy, Client tự fallback về `127.0.0.1`.
- **Cổng:** Đảm bảo cổng `5000` (TCP) và `8888` (UDP) không bị chặn hoặc đang được dùng bởi ứng dụng khác.
- **Database:** Server **phải** kết nối được MySQL trước khi Client đăng nhập. Nếu MySQL chưa chạy, Server vẫn khởi động nhưng mọi thao tác đăng nhập/đấu giá sẽ thất bại.
