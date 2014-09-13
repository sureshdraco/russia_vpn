package com.kernel5.dotvpn.rest;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class IpResponse {

    @JsonProperty(value = "code")
    public int code;

    @JsonProperty(value = "ip")
    public String ip;

    @JsonProperty(value = "distance")
    public int distance;

}
