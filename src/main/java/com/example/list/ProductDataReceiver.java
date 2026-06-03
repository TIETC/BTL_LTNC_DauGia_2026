package com.example.list;
import com.google.gson.JsonObject;
// Giao diện đánh dấu: Các Controller nào muốn nhận dữ liệu sản phẩm thì phải implements nó
public interface ProductDataReceiver { void setProductDetails(JsonObject product); }