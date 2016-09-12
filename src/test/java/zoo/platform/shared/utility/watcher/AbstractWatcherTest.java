package zoo.platform.shared.utility.watcher;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.io.CharSource;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AbstractWatcherTest {

    @Test
    public void provideDataFromLoader() throws IOException {
        // given
        final TestWatcher testWatcher = new TestWatcher(() -> Optional.of(CharSource.wrap("hello world")));
        testWatcher.load();

        // when
        final String res = testWatcher.perform((str) -> str);

        // then
        assertThat(res).isEqualTo("hello world");
    }

    @Test
    public void ifNoDataUseFallback() throws IOException {
        // given
        final TestWatcher testWatcher = new TestWatcher(Optional::empty);
        testWatcher.load();

        // when
        final String res = testWatcher.perform((str) -> str);

        // then
        assertThat(res).isEqualTo("fallback");
    }

    @Test
    public void ifLoaderFailsUseFallback() throws IOException {
        // given
        final TestWatcher testWatcher = new TestWatcher(() -> {
            throw new IOException("No data for you today!");
        });
        testWatcher.load();

        // when
        final String res = testWatcher.perform((str) -> str);

        // then
        assertThat(res).isEqualTo("fallback");
    }

    @Test
    public void canBeRunByScheduler() throws InterruptedException {
        // given
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final TestWatcher testWatcher = new TestWatcher(() -> Optional.of(CharSource.wrap("hello world")));

        // when
        testWatcher.watch(scheduler, 0, 250, TimeUnit.MILLISECONDS);

        TimeUnit.SECONDS.sleep(1);

        final String res = testWatcher.perform((str) -> str);

        // then
        assertThat(res).isEqualTo("hello world");
    }

    @Test
    public void registersHealthCheck()  {
        // given
        final TestWatcher testWatcher = new TestWatcher(() -> Optional.of(CharSource.wrap("hello world")));
        final HealthCheckRegistry registry = mock(HealthCheckRegistry.class);

        // when
        testWatcher.monitor(registry);

        // then
        verify(registry).register(anyString(), any(HealthCheck.class));
    }

    private static final class TestWatcher extends AbstractWatcher<String, String> {
        private TestWatcher(final DataLoader loader) {
            super("test", loader);
        }

        @Override
        protected String update(final CharSource source) throws IOException {
            return source.readFirstLine();
        }

        @Override
        protected String fallback() {
            return "fallback";
        }
    }
}