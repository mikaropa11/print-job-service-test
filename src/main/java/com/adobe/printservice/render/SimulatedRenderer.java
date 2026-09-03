package com.adobe.printservice.render;

import com.adobe.printservice.exception.TransientRenderingException;
import com.adobe.printservice.model.Job;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class SimulatedRenderer implements Renderer {

    private final Duration renderingDelay;
    private final double transientFailureRate;

    public SimulatedRenderer(
            @Value("${jobs.renderer.delay}") Duration renderingDelay,
            @Value("${jobs.renderer.transient-failure-rate}") double transientFailureRate
    ) {
        this.renderingDelay = renderingDelay;
        this.transientFailureRate = transientFailureRate;
    }

    @Override
    public String render(Job job) {
        try {
            Thread.sleep(renderingDelay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TransientRenderingException("Rendering was interrupted", exception);
        }

        if (ThreadLocalRandom.current().nextDouble() < transientFailureRate) {
            throw new TransientRenderingException("Simulated transient rendering failure");
        }

        return "Rendered output for job " + job.getId();
    }
}
