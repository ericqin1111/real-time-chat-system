package csu.web.mypetstore.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

public class Account  {
    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private String status;
    private String address1;
    private String address2;
    private String city;
    private String state;
    private String zip;
    private String country;
    private String phone;
    private String favouriteCategoryId;
    private String languagePreference;
    private boolean listOption;
    private boolean bannerOption;
    private String bannerName;


    public Account() {
    }

    public Account(String username, String password, String email, String firstName, String lastName, String status, String address1, String address2, String city, String state, String zip, String country, String phone, String favouriteCategoryId, String languagePreference, boolean listOption, boolean bannerOption, String bannerName) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
        this.address1 = address1;
        this.address2 = address2;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.country = country;
        this.phone = phone;
        this.favouriteCategoryId = favouriteCategoryId;
        this.languagePreference = languagePreference;
        this.listOption = listOption;
        this.bannerOption = bannerOption;
        this.bannerName = bannerName;
    }

    /**
     * 获取
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 获取
     * @return email
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置
     * @param email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取
     * @return firstName
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * 设置
     * @param firstName
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * 获取
     * @return lastName
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * 设置
     * @param lastName
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
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
     * @return address1
     */
    public String getAddress1() {
        return address1;
    }

    /**
     * 设置
     * @param address1
     */
    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    /**
     * 获取
     * @return address2
     */
    public String getAddress2() {
        return address2;
    }

    /**
     * 设置
     * @param address2
     */
    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    /**
     * 获取
     * @return city
     */
    public String getCity() {
        return city;
    }

    /**
     * 设置
     * @param city
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * 获取
     * @return state
     */
    public String getState() {
        return state;
    }

    /**
     * 设置
     * @param state
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * 获取
     * @return zip
     */
    public String getZip() {
        return zip;
    }

    /**
     * 设置
     * @param zip
     */
    public void setZip(String zip) {
        this.zip = zip;
    }

    /**
     * 获取
     * @return country
     */
    public String getCountry() {
        return country;
    }

    /**
     * 设置
     * @param country
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * 获取
     * @return phone
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 设置
     * @param phone
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 获取
     * @return favouriteCategoryId
     */
    public String getFavouriteCategoryId() {
        return favouriteCategoryId;
    }

    /**
     * 设置
     * @param favouriteCategoryId
     */
    public void setFavouriteCategoryId(String favouriteCategoryId) {
        this.favouriteCategoryId = favouriteCategoryId;
    }

    /**
     * 获取
     * @return languagePreference
     */
    public String getLanguagePreference() {
        return languagePreference;
    }

    /**
     * 设置
     * @param languagePreference
     */
    public void setLanguagePreference(String languagePreference) {
        this.languagePreference = languagePreference;
    }

    /**
     * 获取
     * @return listOption
     */
    public boolean isListOption() {
        return listOption;
    }

    /**
     * 设置
     * @param listOption
     */
    public void setListOption(boolean listOption) {
        this.listOption = listOption;
    }

    /**
     * 获取
     * @return bannerOption
     */
    public boolean isBannerOption() {
        return bannerOption;
    }

    /**
     * 设置
     * @param bannerOption
     */
    public void setBannerOption(boolean bannerOption) {
        this.bannerOption = bannerOption;
    }

    /**
     * 获取
     * @return bannerName
     */
    public String getBannerName() {
        return bannerName;
    }

    /**
     * 设置
     * @param bannerName
     */
    public void setBannerName(String bannerName) {
        this.bannerName = bannerName;
    }

    public String toString() {
        return "Account{username = " + username + ", password = " + password + ", email = " + email + ", firstName = " + firstName + ", lastName = " + lastName + ", status = " + status + ", address1 = " + address1 + ", address2 = " + address2 + ", city = " + city + ", state = " + state + ", zip = " + zip + ", country = " + country + ", phone = " + phone + ", favouriteCategoryId = " + favouriteCategoryId + ", languagePreference = " + languagePreference + ", listOption = " + listOption + ", bannerOption = " + bannerOption + ", bannerName = " + bannerName + "}";
    }
}
