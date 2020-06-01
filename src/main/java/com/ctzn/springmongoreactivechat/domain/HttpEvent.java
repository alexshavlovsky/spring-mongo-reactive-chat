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
    final private String userAgent;
    final private String remoteHost;
    final private int remotePort;
    final private Date timestamp = new Date();

    static public HttpEvent fromServerHttpRequest(ServerHttpRequest request) {
        String userAgent = request.getHeaders().getFirst("User-Agent");
        InetSocketAddress address = request.getRemoteAddress();
        return address == null ?
                new HttpEvent(userAgent, null, 0) :
                new HttpEvent(userAgent, address.getHostString(), address.getPort());
    }
}
