package csu.web.mypetstore.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

public class CartItem implements Serializable {

    private Item item;
    private int quantity;
    private boolean inStock;
    private BigDecimal total;

    public CartItem() {
    }

    public CartItem(Item item, int quantity, boolean inStock, BigDecimal total) {
        this.item = item;
        this.quantity = quantity;
        this.inStock = inStock;
        this.total = total;
    }

    /**
     * 获取
     * @return item
     */
    public Item getItem() {
        return item;
    }

    /**
     * 设置
     * @param item
     */
    public void setItem(Item item) {
        this.item = item;
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

    /**
     * 获取
     * @return inStock
     */
    public boolean isInStock() {
        return inStock;
    }

    /**
     * 设置
     * @param inStock
     */
    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }

    /**
     * 获取
     * @return total
     */
    public BigDecimal getTotal() {
        return total;
    }

    /**
     * 设置
     * @param total
     */
    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String toString() {
        return "CartItem{item = " + item + ", quantity = " + quantity + ", inStock = " + inStock + ", total = " + total + "}";
    }
}
