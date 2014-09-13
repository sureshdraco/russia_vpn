package com.kernel5.dotvpn.rest;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class SignupRequest {

    @JsonProperty(value = "email")
    public String email;

    @JsonProperty(value = "passwd")
    public String passwd;

    @JsonProperty(value = "name")
    public String name;

    @JsonProperty(value = "lastName")
    public String lastName;

}
