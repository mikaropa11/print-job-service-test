package com.adobe.printservice.render;

import com.adobe.printservice.exception.TransientRenderingException;
import com.adobe.printservice.model.Job;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulatedRendererTest {

    @Test
    void render_withZeroFailureRate_returnsSimulatedOutput() {
        SimulatedRenderer renderer = new SimulatedRenderer(Duration.ZERO, 0);
        Job job = new Job();
        job.setId("job-success");

        assertThat(renderer.render(job)).isEqualTo("Rendered output for job job-success");
    }

    @Test
    void render_withFullFailureRate_throwsTransientRenderingException() {
        SimulatedRenderer renderer = new SimulatedRenderer(Duration.ZERO, 1);

        assertThatThrownBy(() -> renderer.render(new Job()))
                .isInstanceOf(TransientRenderingException.class)
                .hasMessage("Simulated transient rendering failure");
    }

}
