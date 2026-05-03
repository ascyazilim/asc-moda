package com.ascmoda.shared.security.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public final class SecurityProblemSupport {

    private SecurityProblemSupport() {
    }

    public static AuthenticationEntryPoint servletAuthenticationEntryPoint() {
        return (request, response, ex) -> writeServletProblem(
                response,
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Authentication is required"
        );
    }

    public static AccessDeniedHandler servletAccessDeniedHandler() {
        return (request, response, ex) -> writeServletProblem(
                response,
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Access is denied"
        );
    }

    public static ServerAuthenticationEntryPoint reactiveAuthenticationEntryPoint() {
        return (exchange, ex) -> writeReactiveProblem(
                exchange,
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Authentication is required"
        );
    }

    public static ServerAccessDeniedHandler reactiveAccessDeniedHandler() {
        return (exchange, ex) -> writeReactiveProblem(
                exchange,
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Access is denied"
        );
    }

    private static void writeServletProblem(HttpServletResponse response, HttpStatus status, String errorCode,
                                            String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(problemJson(status, errorCode, detail));
    }

    private static Mono<Void> writeReactiveProblem(ServerWebExchange exchange, HttpStatus status, String errorCode,
                                                   String detail) {
        byte[] body = problemJson(status, errorCode, detail).getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private static String problemJson(HttpStatus status, String errorCode, String detail) {
        return """
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s","errorCode":"%s","timestamp":"%s"}\
                """
                .formatted(status.getReasonPhrase(), status.value(), detail, errorCode, Instant.now());
    }
}
