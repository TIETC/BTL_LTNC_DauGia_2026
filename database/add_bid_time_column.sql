-- Chạy một lần trong MySQL (auction_db) để lưu thời gian đặt giá
USE auction_db;

-- MySQL 8+: IF NOT EXISTS. MySQL 5.7: bỏ "IF NOT EXISTS" nếu báo lỗi cú pháp.
ALTER TABLE bids
    ADD COLUMN bidTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
