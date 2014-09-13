package com.kernel5.dotvpn.rest;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class InfoRequest {

    @JsonProperty(value = "token")
    public String token;

}
