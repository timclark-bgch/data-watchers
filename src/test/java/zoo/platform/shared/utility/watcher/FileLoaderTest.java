package zoo.platform.shared.utility.watcher;

import com.google.common.io.CharSource;
import com.google.common.io.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class FileLoaderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void throwsIOExceptionForNonExistentFile() throws IOException {
        // given
        final File file = folder.newFile("immediately_deleted");
        final String fileName = file.getAbsolutePath();
        final FileLoader loader = new FileLoader(fileName);

        // when
        java.nio.file.Files.delete(file.toPath());
        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        final Throwable throwable = catchThrowable(loader::loadIfChanged);

        // then
        assertThat(throwable).isInstanceOf(IOException.class);
        assertThat(throwable).hasMessageContaining(fileName);
    }

    @Test
    public void fileAlwaysAvailableOnFirstCall() throws IOException {
        // given
        final File file = folder.newFile("loadsFileTest.txt");
        final FileLoader loader = new FileLoader(file.getAbsolutePath());

        // when
        final Optional<CharSource> charSource = loader.loadIfChanged();

        // then
        assertThat(charSource).isNotEmpty();
    }

    @Test
    public void fileNotAvailableIfNoChange() throws IOException {
        // given
        final File file = folder.newFile("changedFileTest.txt");
        final FileLoader loader = new FileLoader(file.getAbsolutePath());

        // when
        final Optional<CharSource> firstAccess = loader.loadIfChanged();
        final Optional<CharSource> secondAccess = loader.loadIfChanged();

        // then
        assertThat(firstAccess).isNotEmpty();
        assertThat(secondAccess).isEmpty();
    }

    @Test
    public void fileAvailableIfChanged() throws IOException {
        // given
        final File file = folder.newFile("changedFileTest.txt");
        final FileLoader loader = new FileLoader(file.getAbsolutePath());

        // when
        final Optional<CharSource> firstAccess = loader.loadIfChanged();
        Files.write("testing 123".getBytes(), file);
        final Optional<CharSource> secondAccess = loader.loadIfChanged();

        // then
        assertThat(firstAccess).isNotEmpty();
        assertThat(secondAccess).isNotEmpty();
    }

}