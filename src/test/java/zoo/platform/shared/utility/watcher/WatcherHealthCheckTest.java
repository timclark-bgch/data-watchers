package zoo.platform.shared.utility.watcher;

import com.codahale.metrics.health.HealthCheck;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WatcherHealthCheckTest {

    @Test
    public void reportsHealthyIfCheckPasses() throws Exception {
        // given
        final String message = "I am not healthy";
        final WatcherHealthCheck healthCheck = new WatcherHealthCheck("component", message, () -> true);

        // when
        final HealthCheck.Result result = healthCheck.check();

        // then
        assertThat(result.isHealthy()).isTrue();
        assertThat(result.getMessage()).isNull();
    }

    @Test
    public void reportsHealthyWithMessageIfCheckFails() throws Exception {
        // given
        final String message = "I am not healthy";
        final WatcherHealthCheck healthCheck = new WatcherHealthCheck("component", message, () -> false);

        // when
        final HealthCheck.Result result = healthCheck.check();

        // then
        assertThat(result.isHealthy()).isTrue();
        assertThat(result.getMessage()).isNotNull();
        assertThat(result.getMessage()).contains(message);
    }

}