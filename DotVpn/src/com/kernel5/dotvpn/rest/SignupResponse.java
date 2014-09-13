package com.kernel5.dotvpn.rest;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class SignupResponse {

    @JsonProperty(value = "code")
    public int code;

    @JsonProperty(value = "token")
    public String token;

}
