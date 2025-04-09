package csu.web.mypetstore.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

public class Item {

    private String itemId;
    private String productId;
    private BigDecimal listPrice;
    private BigDecimal unitCost;
    private int supplierId;
    private String status;
    private String attribute1;
    private String attribute2;
    private String attribute3;
    private String attribute4;
    private String attribute5;
    private Product product;
    private int quantity;


    public Item() {
    }

    public Item(String itemId, String productId, BigDecimal listPrice, BigDecimal unitCost, int supplierId, String status, String attribute1, String attribute2, String attribute3, String attribute4, String attribute5, Product product, int quantity) {
        this.itemId = itemId;
        this.productId = productId;
        this.listPrice = listPrice;
        this.unitCost = unitCost;
        this.supplierId = supplierId;
        this.status = status;
        this.attribute1 = attribute1;
        this.attribute2 = attribute2;
        this.attribute3 = attribute3;
        this.attribute4 = attribute4;
        this.attribute5 = attribute5;
        this.product = product;
        this.quantity = quantity;
    }

    /**
     * 获取
     * @return itemId
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * 设置
     * @param itemId
     */
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    /**
     * 获取
     * @return productId
     */
    public String getProductId() {
        return productId;
    }

    /**
     * 设置
     * @param productId
     */
    public void setProductId(String productId) {
        this.productId = productId;
    }

    /**
     * 获取
     * @return listPrice
     */
    public BigDecimal getListPrice() {
        return listPrice;
    }

    /**
     * 设置
     * @param listPrice
     */
    public void setListPrice(BigDecimal listPrice) {
        this.listPrice = listPrice;
    }

    /**
     * 获取
     * @return unitCost
     */
    public BigDecimal getUnitCost() {
        return unitCost;
    }

    /**
     * 设置
     * @param unitCost
     */
    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    /**
     * 获取
     * @return supplierId
     */
    public int getSupplierId() {
        return supplierId;
    }

    /**
     * 设置
     * @param supplierId
     */
    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    /**
     * 获取
     * @return status
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置
     * @param status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取
     * @return attribute1
     */
    public String getAttribute1() {
        return attribute1;
    }

    /**
     * 设置
     * @param attribute1
     */
    public void setAttribute1(String attribute1) {
        this.attribute1 = attribute1;
    }

    /**
     * 获取
     * @return attribute2
     */
    public String getAttribute2() {
        return attribute2;
    }

    /**
     * 设置
     * @param attribute2
     */
    public void setAttribute2(String attribute2) {
        this.attribute2 = attribute2;
    }

    /**
     * 获取
     * @return attribute3
     */
    public String getAttribute3() {
        return attribute3;
    }

    /**
     * 设置
     * @param attribute3
     */
    public void setAttribute3(String attribute3) {
        this.attribute3 = attribute3;
    }

    /**
     * 获取
     * @return attribute4
     */
    public String getAttribute4() {
        return attribute4;
    }

    /**
     * 设置
     * @param attribute4
     */
    public void setAttribute4(String attribute4) {
        this.attribute4 = attribute4;
    }

    /**
     * 获取
     * @return attribute5
     */
    public String getAttribute5() {
        return attribute5;
    }

    /**
     * 设置
     * @param attribute5
     */
    public void setAttribute5(String attribute5) {
        this.attribute5 = attribute5;
    }

    /**
     * 获取
     * @return product
     */
    public Product getProduct() {
        return product;
    }

    /**
     * 设置
     * @param product
     */
    public void setProduct(Product product) {
        this.product = product;
    }

    /**
     * 获取
     * @return quantity
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * 设置
     * @param quantity
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String toString() {
        return "Item{itemId = " + itemId + ", productId = " + productId + ", listPrice = " + listPrice + ", unitCost = " + unitCost + ", supplierId = " + supplierId + ", status = " + status + ", attribute1 = " + attribute1 + ", attribute2 = " + attribute2 + ", attribute3 = " + attribute3 + ", attribute4 = " + attribute4 + ", attribute5 = " + attribute5 + ", product = " + product + ", quantity = " + quantity + "}";
    }
}
