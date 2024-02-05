package fr.enimaloc.esportline.api.smash;

public record Location(String country, String state, String city, String cCountry, String cState, String cCity) {
    public String effectiveCountry() {
        return country == null || country.isBlank() ? cCountry : country;
    }

    public String effectiveState() {
        return state == null || state.isBlank() ? cState : state;
    }

    public String effectiveCity() {
        return city == null || city.isBlank() ? cCity : city;
    }

    @Override
    public String toString() {
        return effectiveCountry() + ", " + effectiveState() + ", " + effectiveCity();
    }
}
