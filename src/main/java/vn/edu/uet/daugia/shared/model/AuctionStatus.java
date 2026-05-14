package vn.edu.uet.daugia.shared.model;

public enum AuctionStatus {
    OPEN,      // Phiên vừa tạo, chưa bắt đầu
    RUNNING,   // Đang diễn ra, nhận bid
    FINISHED,  // Hết thời gian, có người thắng
    PAID,      // Người thắng đã thanh toán
    CANCELED   // Bị hủy (không có bid hoặc admin hủy)
}