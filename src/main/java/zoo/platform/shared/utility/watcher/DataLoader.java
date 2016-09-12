package zoo.platform.shared.utility.watcher;

import com.google.common.io.CharSource;

import java.io.IOException;
import java.util.Optional;

@FunctionalInterface
public interface DataLoader {
    Optional<CharSource> loadIfChanged() throws IOException;
}
