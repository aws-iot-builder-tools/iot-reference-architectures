package com.awslabs.iot.client.data;

import com.igormaznitsa.jbbp.mapper.Bin;

/**
 * Generated from JBBP script by internal JBBP Class Source Generator
 */
public class GseOpenGps10Byte {
    @Bin(path = "messageblocktype")
    private long messageBlockType;
    @Bin(path = "magicnumber")
    private long magicNumber;
    @Bin(path = "longitude")
    private long longitude;
    @Bin(path = "heading")
    private long heading;
    @Bin(path = "time")
    private long time;
    @Bin(path = "latitude")
    private long latitude;
    @Bin(path = "speed")
    private long speed;
    @Bin(path = "altitude")
    private long altitude;

    public GseOpenGps10Byte() {
    }

    public double getLatitude() {
        return (latitude / 23301.0) - 90;
    }

    public double getLongitude() {
        return (longitude / 23301.0) - 180;
    }

    public long getHeading() {
        return heading * 5;
    }

    public long getAltitude() {
        return altitude * 5;
    }

    public long getSpeed() {
        return speed;
    }

    public void setMessageBlockType(long messageBlockType) {
        this.messageBlockType = messageBlockType;
    }

    public void setMagicNumber(long magicNumber) {
        this.magicNumber = magicNumber;
    }

    public void setLongitude(long longitude) {
        this.longitude = longitude;
    }

    public void setHeading(long heading) {
        this.heading = heading;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setLatitude(long latitude) {
        this.latitude = latitude;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public void setAltitude(long altitude) {
        this.altitude = altitude;
    }
}