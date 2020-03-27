package com.ctzn.springmongoreactivechat.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetSocketAddress;
import java.util.Date;

@Data
@Document
@TypeAlias("httpEvents")
public class HttpEvent {
    @Id
    private String id;
    private String remoteHost;
    private int remotePort;
    private Date timestamp = new Date();
    private String userAgent;

    static public HttpEvent fromServerHttpRequest(ServerHttpRequest request) {
        HttpEvent httpEvent = new HttpEvent();
        InetSocketAddress address = request.getRemoteAddress();
        if (address != null) {
            httpEvent.setRemoteHost(address.getHostString());
            httpEvent.setRemotePort(address.getPort());
        }
        httpEvent.setUserAgent(request.getHeaders().getFirst("User-Agent"));
        return httpEvent;
    }
}
