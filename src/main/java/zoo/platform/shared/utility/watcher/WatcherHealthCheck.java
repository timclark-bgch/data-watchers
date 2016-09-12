package zoo.platform.shared.utility.watcher;

import com.codahale.metrics.health.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BooleanSupplier;

final class WatcherHealthCheck extends HealthCheck {
    private final static Logger log = LoggerFactory.getLogger(WatcherHealthCheck.class);

    private final String componentName;
    private final String message;
    private final BooleanSupplier check;

    WatcherHealthCheck(final String componentName, final String message, final BooleanSupplier check) {
        this.componentName = componentName;
        this.message = message;
        this.check = check;
    }

    @Override
    protected Result check() throws Exception {
        final Result result;
        if (check.getAsBoolean()) {
            result = Result.healthy();
        } else {
            // Even though this check has failed, the service is still healthy
            // as it has been designed with fallbacks to use when data is missing.
            result = Result.healthy(message);
        }

        // This log.debug statement is required by monitoring as it checks that logging is working.
        log.debug("{} healthCheck => {}", componentName, result);
        return result;
    }
}