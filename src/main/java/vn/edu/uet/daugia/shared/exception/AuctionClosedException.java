package vn.edu.uet.daugia.shared.exception;

public class AuctionClosedException extends RuntimeException {
    public AuctionClosedException(String message) {
        super(message);
    }
}