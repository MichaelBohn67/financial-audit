package de.bohnottensen.financialaudit.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestContextFilterTest {

    private RequestContextFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RequestContextFilter();
        MDC.clear();
    }

    @Test
    void shouldPassThroughValidRequestIdAndSetHeadersAndMDC() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-ID", "custom-request-id-123");
        request.setRemoteAddr("127.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        AtomicReference<String> capturedSourceIp = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            capturedRequestId.set(MDC.get("requestId"));
            capturedSourceIp.set(MDC.get("sourceIp"));
        };

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader("X-Request-ID")).isEqualTo("custom-request-id-123");
        assertThat(capturedRequestId.get()).isEqualTo("custom-request-id-123");
        assertThat(capturedSourceIp.get()).isEqualTo("127.0.0.1");
        assertThat(MDC.get("sourceIp")).isNull();
    }

    @Test
    void shouldGenerateUUIDWhenRequestIdIsNull() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedRequestId.set(MDC.get("requestId"));

        filter.doFilterInternal(request, response, chain);

        String generatedId = response.getHeader("X-Request-ID");
        assertThat(generatedId).isNotBlank();
        assertThat(capturedRequestId.get()).isEqualTo(generatedId);
    }

    @Test
    void shouldGenerateUUIDWhenRequestIdIsBlank() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-ID", "   ");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedRequestId.set(MDC.get("requestId"));

        filter.doFilterInternal(request, response, chain);

        String generatedId = response.getHeader("X-Request-ID");
        assertThat(generatedId).isNotBlank().isNotEqualTo("   ");
        assertThat(capturedRequestId.get()).isEqualTo(generatedId);
    }

    @Test
    void shouldGenerateUUIDWhenRequestIdExceeds100Chars() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String longId = "a".repeat(101);
        request.addHeader("X-Request-ID", longId);
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedRequestId.set(MDC.get("requestId"));

        filter.doFilterInternal(request, response, chain);

        String generatedId = response.getHeader("X-Request-ID");
        assertThat(generatedId).isNotBlank().isNotEqualTo(longId);
        assertThat(capturedRequestId.get()).isEqualTo(generatedId);
    }

    @Test
    void shouldAcceptRequestIdWhenExactly100Chars() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String exact100Id = "a".repeat(100);
        request.addHeader("X-Request-ID", exact100Id);
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedRequestId.set(MDC.get("requestId"));

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader("X-Request-ID")).isEqualTo(exact100Id);
        assertThat(capturedRequestId.get()).isEqualTo(exact100Id);
    }

    @Test
    void shouldExtractFirstIpFromForwardedForHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", " 203.0.113.195 , 70.41.3.18, 150.172.238.178 ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedSourceIp = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedSourceIp.set(MDC.get("sourceIp"));

        filter.doFilterInternal(request, response, chain);

        assertThat(capturedSourceIp.get()).isEqualTo("203.0.113.195");
    }

    @Test
    void shouldFallbackToRemoteAddrWhenForwardedForIsBlank() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "   ");
        request.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedSourceIp = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedSourceIp.set(MDC.get("sourceIp"));

        filter.doFilterInternal(request, response, chain);

        assertThat(capturedSourceIp.get()).isEqualTo("192.168.1.50");
    }

    @Test
    void shouldCleanUpMDCSourceIpEvenIfChainThrowsException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain throwingChain = (req, res) -> {
            throw new RuntimeException("Downstream error");
        };

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, throwingChain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Downstream error");

        assertThat(MDC.get("sourceIp")).isNull();
    }
}
