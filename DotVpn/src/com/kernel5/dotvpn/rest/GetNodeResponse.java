package com.kernel5.dotvpn.rest;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.lang.Override;
import java.lang.String;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class GetNodeResponse {

    @JsonProperty(value = "code")
    public int code;

    @JsonProperty(value = "ip")
    public String ip;

    @JsonProperty(value = "node")
    public String node;

    @Override
    public String toString() {
        return "InfoResponse{" +
                "node=" + node +
                ", ip=" + ip +
                ", code=" + code +
                '}';
    }
}
