package vn.edu.uet.daugia.client.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** Một dòng trong bảng lịch sử đặt giá (phòng đấu giá). */
public class BidHistoryRow {
    private final StringProperty bidder = new SimpleStringProperty();
    private final StringProperty bidTime = new SimpleStringProperty();
    private final StringProperty priceText = new SimpleStringProperty();
    private final double price;

    public BidHistoryRow(String bidder, String bidTime, double price) {
        this.bidder.set(bidder);
        this.bidTime.set(bidTime);
        this.price = price;
        this.priceText.set(String.format("%,.0f VNĐ", price));
    }

    public StringProperty bidderProperty() { return bidder; }
    public StringProperty bidTimeProperty() { return bidTime; }
    public StringProperty priceTextProperty() { return priceText; }
    public double getPrice() { return price; }
}
