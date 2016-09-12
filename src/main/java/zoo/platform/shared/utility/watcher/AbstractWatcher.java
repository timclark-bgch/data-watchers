package zoo.platform.shared.utility.watcher;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.io.CharSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

public abstract class AbstractWatcher<T, R> {
    private final static Logger log = LoggerFactory.getLogger(AbstractWatcher.class);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final String componentName;
    private final DataLoader loader;

    private T watched;
    private boolean fetched = false;

    protected AbstractWatcher(final String componentName, final DataLoader loader) {
        this.componentName = componentName;
        this.loader = loader;

        this.watched = fallback();
        final boolean loaded = load();
        if (!loaded) {
            log.warn("Using fallback for {}", this.componentName);
        }
    }

    protected R perform(final Function<T, R> operation) {
        try {
            lock.readLock().lock();
            return operation.apply(watched);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void watch(final ScheduledExecutorService scheduler,
                      final int delay,
                      final int period,
                      final TimeUnit timeUnit) {
        scheduler.scheduleAtFixedRate(this::loadSafely, delay, period, timeUnit);
    }

    public void monitor(final HealthCheckRegistry healthCheckRegistry) {
        healthCheckRegistry.register(componentName,
                new WatcherHealthCheck(componentName, String.format("Unable to fetch data for %s", componentName),
                        this::fetched));
    }

    private void loadSafely() {
        try {
            load();
        } catch (final Throwable t) {
            // Throwable is caught so that the scheduling thread doesn't die!
            log.error("[{}] unable to load data: {}", componentName, t.getMessage());
        }
    }

    public boolean load() {
        try {
            final Optional<CharSource> source = loader.loadIfChanged();
            if (source.isPresent()) {
                updateWatched(source.get());
            }
            fetched = true;
            return true;
        } catch (final IOException e) {
            log.error("[{}] unable to load data: {}", componentName, e.getMessage());
        }

        fetched = false;
        return false;
    }

    private boolean fetched() {
        return fetched;
    }

    private void updateWatched(final CharSource source) {
        try {
            lock.writeLock().lock();
            watched = update(source);
            log.info("[{}] updated data", componentName);
        } catch (final Throwable t) {
            log.error("[{}] unable to update data: {}", componentName, t.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected abstract T update(final CharSource source) throws IOException;

    protected abstract T fallback();

}
