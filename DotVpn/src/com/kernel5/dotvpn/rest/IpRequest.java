package com.kernel5.dotvpn.rest;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class IpRequest {

    @JsonProperty(value = "token")
    public String token;

    @JsonProperty(value = "srcip")
    public String srcip;

}
