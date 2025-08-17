package org.acme.constants;

public enum ServerResultStatus {
    DONE("done"),
    STOP("stop");

    private final String value;
    ServerResultStatus(String value) { this.value = value; }
    public String value() { return value; }
}
