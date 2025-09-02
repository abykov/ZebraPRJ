package com.example.zebraprj.model;

import jakarta.persistence.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection =  "userproperties")
public class UserProperty {
    @Id
    private String userId;
    private String address;
    private String organisation;
    private String favouriteColour;

    public UserProperty() {
    }

    public UserProperty(String userId, String address, String organisation, String favouriteColour) {
        this.userId = userId;
        this.address = address;
        this.organisation = organisation;
        this.favouriteColour = favouriteColour;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getFavouriteColour() {
        return favouriteColour;
    }

    public void setFavouriteColour(String favouriteColour) {
        this.favouriteColour = favouriteColour;
    }
}
