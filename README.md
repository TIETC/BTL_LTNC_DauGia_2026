## Hệ thống đấu giá trực tuyến - Nhóm 9 

## Mô tả bài toán & Phạm vi hệ thống

Hệ thống cho phép nhiều người dùng cùng tham gia cạnh tranh giá để mua sản phẩm trong một khoảng thời gian xác định. Người bán đăng sản phẩm, người mua đặt giá cạnh tranh, và giá bán cuối cùng được xác định tự động khi phiên kết thúc.

| Thành phần | Vai trò |
|---|---|
| **Server** | Xử lý nghiệp vụ, quản lý phiên đấu giá, lưu trữ dữ liệu vào MySQL |
| **Client (JavaFX)** | Giao diện đồ họa cho Bidder và Seller, kết nối với Server qua TCP Socket |
| **Database** | MySQL — lưu người dùng, phiên đấu giá, lịch sử bid |
| **LAN Discovery** | Client tự động dò IP Server trong cùng mạng Wi-Fi/LAN qua UDP Broadcast |

Giao tiếp Client–Server sử dụng **JSON qua TCP Socket** (cổng `5000`). Server tự động dò tìm bởi Client qua **UDP Broadcast** (cổng `8888`), không cần nhập IP thủ công.

---

##  Công nghệ sử dụng

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
## Yêu Cầu Cài Đặt

Trước khi chạy, đảm bảo máy đã cài đủ:

- **JDK 17** trở lên — [tải tại đây](https://adoptium.net/)
- **JavaFX SDK 17+** — [tải tại đây](https://gluonhq.com/products/javafx/)
- **MySQL Server 8.x** — [tải tại đây](https://dev.mysql.com/downloads/mysql/)
- **Maven 3.8+** hoặc **IDE hỗ trợ Maven** (IntelliJ IDEA, Eclipse, VS Code)

### Cấu hình Database

Tạo database và bảng bằng cách chạy script sau trong MySQL:

```sql
CREATE DATABASE auction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auction_db;

CREATE TABLE users (
    username    VARCHAR(50)  PRIMARY KEY,
    password    VARCHAR(255) NOT NULL,
    email       VARCHAR(100),
    role        VARCHAR(10)  NOT NULL DEFAULT 'BIDDER',
    balance     DOUBLE       DEFAULT 0,
    shop_name   VARCHAR(100),
    is_active   BOOLEAN      DEFAULT TRUE
);

CREATE TABLE auctions (
    id            VARCHAR(50)  PRIMARY KEY,
    itemName      VARCHAR(200) NOT NULL,
    description   TEXT,
    sellerName    VARCHAR(50),
    startPrice    DOUBLE       NOT NULL,
    currentPrice  DOUBLE       DEFAULT 0,
    max_price     DOUBLE       DEFAULT 0,
    startTime     VARCHAR(50),
    endTime       VARCHAR(50),
    status        VARCHAR(20)  DEFAULT 'RUNNING',
    winner        VARCHAR(50),
    image_url     TEXT
);

CREATE TABLE bids (
    id         INT          AUTO_INCREMENT PRIMARY KEY,
    auctionId  VARCHAR(50)  NOT NULL,
    bidderId   VARCHAR(50)  NOT NULL,
    price      DOUBLE       NOT NULL,
    bidTime    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
```

### Cấu hình kết nối Database

Mở file `src/main/java/vn/edu/uet/daugia/server/dao/DatabaseConnection.java` và sửa thông tin phù hợp:

```java
private static final String URL      = "jdbc:mysql://localhost:3306/auction_db";
private static final String USER     = "root";       // ← đổi thành username MySQL của bạn
private static final String PASSWORD = "123456";     // ← đổi thành mật khẩu MySQL của bạn
```

---

## 📁 Cấu Trúc Thư Mục

```
src/main/java/vn/edu/uet/daugia/
│
├── client/                         # Phía Client (JavaFX)
│   ├── AuctionClient.java          # Điểm khởi động Client
│   ├── controller/                 # Các màn hình giao diện
│   │   ├── LoginController.java
│   │   ├── RegisterController.java
│   │   ├── AuctionListController.java
│   │   ├── BiddingRoomController.java
│   │   ├── ProductDetailController.java
│   │   ├── ProductCardController.java
│   │   ├── SellerDashboardController.java
│   │   └── AdminDashboardController.java
│   ├── network/
│   │   ├── NetworkClient.java      # Kết nối TCP tới Server
│   │   └── ClientDiscovery.java    # Tự dò IP Server qua UDP
│   ├── model/
│   │   ├── BidHistoryRow.java
│   │   └── Product.java
│   └── util/
│       ├── AlertUtil.java
│       ├── SceneManager.java
│       └── SessionManager.java
│
├── server/                         # Phía Server
│   ├── AuctionServer.java          # Điểm khởi động Server
│   ├── dao/
│   │   └── DatabaseConnection.java
│   ├── service/
│   │   ├── AuctionManager.java     # Quản lý phiên trong RAM
│   │   ├── AuctionService.java     # Nghiệp vụ đấu giá
│   │   ├── AuctionObserver.java    # Interface Observer
│   │   └── UserManager.java
│   ├── controller/
│   │   ├── ClientHandler.java      # Xử lý kết nối từng Client
│   │   └── ServerDiscovery.java    # Lắng nghe UDP Broadcast
│   └── admin/
│       ├── AdminService.java       # Nghiệp vụ Admin
│       └── AdminHandler.java       # Router lệnh Admin
│
└── shared/                         # Dùng chung Client + Server
    ├── model/
    │   ├── Auction.java
    │   ├── AuctionStatus.java
    │   ├── BidMessage.java
    │   ├── BidTransaction.java
    │   ├── LoginMessage.java
    │   ├── RegisterMessage.java
    │   ├── entity/
    │   │   └── Entity.java
    │   ├── item/                   # Factory Method – Item
    │   │   ├── Item.java
    │   │   ├── ItemType.java
    │   │   ├── ItemFactory.java
    │   │   ├── ItemFactoryProvider.java
    │   │   ├── Electronics.java / ElectronicsFactory.java
    │   │   ├── Art.java / ArtFactory.java
    │   │   └── Vehicle.java / VehicleFactory.java
    │   └── user/                   # Factory Method – User
    │       ├── User.java
    │       ├── UserRole.java
    │       ├── UserFactory.java
    │       ├── UserFactoryProvider.java
    │       ├── Bidder.java / BidderFactory.java
    │       ├── Seller.java / SellerFactory.java
    │       └── Admin.java / AdminFactory.java
    └── exception/
        ├── AuctionClosedException.java
        └── InvalidBidException.java
```

---

## ▶️ Cách Chạy Chương Trình

> **Quan trọng:** Luôn khởi động **Server trước**, sau đó mới chạy **Client**.

### Bước 1 — Build project

```bash
# Linux / macOS
./mvnw clean package -DskipTests

# Windows
mvnw.cmd clean package -DskipTests
```

Hoặc dùng IDE: **Build > Build Project** (IntelliJ), **Project > Build All** (Eclipse).

---

### Bước 2 — Chạy Server

**Linux / macOS:**
```bash
java -cp target/daugia-1.0.jar vn.edu.uet.daugia.server.AuctionServer
```

**Windows:**
```bat
java -cp target\daugia-1.0.jar vn.edu.uet.daugia.server.AuctionServer
```

**Chạy trực tiếp từ IDE:** Run class `AuctionServer.java`

Khi Server khởi động thành công, console sẽ hiện:
```
=== HỆ THỐNG ĐẤU GIÁ SERVER ĐANG KHỞI ĐỘNG ===
Đang mở cổng 5000 và chờ Client kết nối...
```

---

### Bước 3 — Chạy Client

**Linux / macOS:**
```bash
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp target/daugia-1.0.jar \
     vn.edu.uet.daugia.client.AuctionClient
```

**Windows:**
```bat
java --module-path C:\path\to\javafx-sdk\lib ^
     --add-modules javafx.controls,javafx.fxml ^
     -cp target\daugia-1.0.jar ^
     vn.edu.uet.daugia.client.AuctionClient
```

> Thay `/path/to/javafx-sdk` bằng đường dẫn thực tế JavaFX SDK trên máy bạn.

**Chạy từ IDE:** Run class `AuctionClient.java` (IDE thường tự xử lý JavaFX module path).

Client sẽ tự động dò tìm Server trong cùng mạng LAN qua UDP Broadcast. Nếu không tìm thấy, tự kết nối về `127.0.0.1:5000`.

---

### Chạy nhiều Client cùng lúc

Mở thêm terminal và lặp lại Bước 3. Mỗi terminal là một Client độc lập — các bid sẽ được cập nhật real-time cho tất cả Client đang kết nối.

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

## 📌 Lưu ý

- Server và Client có thể chạy trên cùng một máy (dùng `localhost`) hoặc các máy khác nhau trong cùng mạng LAN.
- Cổng TCP: **5000**, cổng UDP Discovery: **8888** — đảm bảo tường lửa cho phép hai cổng này.
- Nếu dùng mạng Wi-Fi trường/công ty có chặn UDP Broadcast, Client sẽ tự fallback về `127.0.0.1`.
