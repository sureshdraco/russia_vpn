package com.kernel5.dotvpn.rest;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class InfoResponse {

    @JsonProperty(value = "code")
    public int code;

    @JsonProperty(value = "login")
    public String login;

    @JsonProperty(value = "email")
    public String email;

    @JsonProperty(value = "ip")
    public String ip;

    @JsonProperty(value = "balance")
    public int balance;

    @JsonProperty(value = "balanceSuffix")
    public String balanceSuffix;

}
