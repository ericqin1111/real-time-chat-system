package csu.web.mypetstore.domain;

import jakarta.servlet.http.HttpSession;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

public class Cart  {

    private  Map<String, CartItem> itemMap = Collections.synchronizedMap(new HashMap<String, CartItem>());
    private  List<CartItem> itemList = new ArrayList<CartItem>();


    public Cart() {
    }

    public Cart(Map<String, CartItem> itemMap, List<CartItem> itemList) {
        this.itemMap = itemMap;
        this.itemList = itemList;
    }

    public String toString() {
        return "Cart{itemMap = " + itemMap + ", itemList = " + itemList + "}";
    }
}
