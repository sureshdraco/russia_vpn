package com.kernel5.dotvpn.rest;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class SigninRequest {

    @JsonProperty(value = "email")
    public String email;

    @JsonProperty(value = "passwd")
    public String passwd;

}
