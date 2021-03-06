package com.anachat.chatsdk.internal.model.inputdata;

import com.anachat.chatsdk.internal.model.BaseModel;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.math.BigDecimal;

/**
 * Created by lookup on 28/08/17.
 */


@DatabaseTable(tableName = "input_location_data")
public class DefaultLocation extends BaseModel {

    @DatabaseField(generatedId = true)
    private transient int id;
    @DatabaseField(columnName = "lat")
    private BigDecimal lat;
    @DatabaseField(columnName = "lng")
    private BigDecimal lng;

    public DefaultLocation() {
    }

    public DefaultLocation(BigDecimal lat, BigDecimal lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public BigDecimal getLat() {
        return lat;
    }

    public void setLat(BigDecimal lat) {
        this.lat = lat;
    }

    public BigDecimal getLng() {
        return lng;
    }

    public void setLng(BigDecimal lng) {
        this.lng = lng;
    }
}
