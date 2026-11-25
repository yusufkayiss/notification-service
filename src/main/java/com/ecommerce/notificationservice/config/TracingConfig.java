package com.ecommerce.notificationservice.config;

import brave.Tracing;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.B3Propagation;
import brave.propagation.ThreadLocalCurrentTraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BravePropagator;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.Encoding;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@Configuration
public class TracingConfig {

    @Value("${management.zipkin.tracing.endpoint:http://zipkin:9411/api/v2/spans}")
    private String zipkinEndpoint;

    @Value("${spring.application.name:notification-service}")
    private String serviceName;

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public AsyncZipkinSpanHandler zipkinSpanHandler() {
        BytesMessageSender sender = new BytesMessageSender.Base(Encoding.JSON) {
            @Override
            public int messageMaxBytes() {
                return 512 * 1024;
            }

            @Override
            public void send(List<byte[]> encodedSpans) throws IOException {
                ByteArrayOutputStream payload = new ByteArrayOutputStream();
                payload.write('[');
                for (int i = 0; i < encodedSpans.size(); i++) {
                    if (i > 0) {
                        payload.write(',');
                    }
                    payload.write(encodedSpans.get(i));
                }
                payload.write(']');

                HttpURLConnection conn = (HttpURLConnection) new URL(zipkinEndpoint).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toByteArray());
                }
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    throw new IOException("Zipkin HTTP " + code);
                }
            }

            @Override
            public void close() {
            }
        };
        return AsyncZipkinSpanHandler.create(sender);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public Tracing braveTracing(AsyncZipkinSpanHandler spanHandler) {
        ThreadLocalCurrentTraceContext currentTraceContext = ThreadLocalCurrentTraceContext.newBuilder()
                .addScopeDecorator(MDCScopeDecorator.get())
                .build();

        return Tracing.newBuilder()
                .localServiceName(serviceName)
                .traceId128Bit(true)
                .currentTraceContext(currentTraceContext)
                .propagationFactory(B3Propagation.FACTORY)
                .addSpanHandler(spanHandler)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public Tracer micrometerTracer(Tracing tracing) {
        return new BraveTracer(
                tracing.tracer(),
                new BraveCurrentTraceContext(tracing.currentTraceContext()),
                new BraveBaggageManager()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public Propagator micrometerPropagator(Tracing tracing) {
        return new BravePropagator(tracing);
    }
}