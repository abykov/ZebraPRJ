package com.example.zebraprj.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection =  "userproperties")
@Schema(description = "User property information")
public class UserProperty {
    @Id
    @Schema(description = "Unique identifier of the user")
    private String userId;

    @Schema(description = "Home address of the user")
    private String address;

    @Schema(description = "Organisation the user belongs to")
    private String organisation;

    @Schema(description = "User's favourite colour")
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
