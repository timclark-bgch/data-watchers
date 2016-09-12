package zoo.platform.shared.utility.watcher;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class FileLoader implements DataLoader {
    private final Path path;
    private final AtomicReference<HashCode> lastRead = new AtomicReference<>(null);

    public FileLoader(final String file) {
        this.path = Paths.get(file);
    }

    @Override
    public Optional<CharSource> loadIfChanged() throws IOException {
        final HashCode currentHash = hash(path);

        if (currentHash.equals(lastRead.getAndSet(currentHash))) {
            return Optional.empty();
        }

        return Optional.of(Files.asCharSource(path.toFile(), Charsets.UTF_8));
    }

    public static HashCode hash(final Path path) throws IOException {
        return Files.hash(path.toFile(), Hashing.md5());
    }
}