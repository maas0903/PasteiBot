/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pasteibot;

/**
 *
 * @author Marius
 */

public final class IssPosition {
    public final String message;
    public final Iss_position iss_position;
    public final long timestamp;

    public IssPosition(String message, Iss_position iss_position, long timestamp){
        this.message = message;
        this.iss_position = iss_position;
        this.timestamp = timestamp;
    }

    public static final class Iss_position {
        public final String longitude;
        public final String latitude;

        public Iss_position(String longitude, String latitude){
            this.longitude = longitude;
            this.latitude = latitude;
        }
    }
}    

