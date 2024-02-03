package fr.enimaloc.esportline.api.smash;

public record Location(String country, String state, String city, String cCountry, String cState, String cCity) {
    public String effectiveCountry() {
        return country == null ? cCountry : country;
    }

    public String effectiveState() {
        return state == null ? cState : state;
    }

    public String effectiveCity() {
        return city == null ? cCity : city;
    }
}
