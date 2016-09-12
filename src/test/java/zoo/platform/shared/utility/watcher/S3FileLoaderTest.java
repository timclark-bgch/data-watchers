package zoo.platform.shared.utility.watcher;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.common.io.CharSource;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class S3FileLoaderTest {

    private final AmazonS3 s3Client = mock(AmazonS3.class);
    private final ObjectMetadata metadata = mock(ObjectMetadata.class);
    private final S3Object s3Object = mock(S3Object.class);

    @Test
    public void fileAlwaysAvailableOnFirstCall() throws IOException {
        // given
        final String bucket = "bucket";
        final String key = "key";
        final S3FileLoader loader = new S3FileLoader(bucket, key, s3Client);

        when(s3Client.getObjectMetadata(bucket, key)).thenReturn(metadata);
        when(metadata.getETag()).thenReturn("etag-123");

        // when
        final Optional<CharSource> source = loader.loadIfChanged();

        // then
        assertThat(source).isNotEmpty();
    }

    @Test
    public void fileNotAvailableIfNoChange() throws IOException {
        // given
        final String bucket = "bucket";
        final String key = "key";
        final S3FileLoader loader = new S3FileLoader(bucket, key, s3Client);

        when(s3Client.getObjectMetadata(bucket, key)).thenReturn(metadata);
        when(metadata.getETag()).thenReturn("etag-123");

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
        final String bucket = "bucket";
        final String key = "key";
        final S3FileLoader loader = new S3FileLoader(bucket, key, s3Client);

        when(s3Client.getObjectMetadata(bucket, key)).thenReturn(metadata);
        when(metadata.getETag()).thenReturn("etag-123", "etag-456");

        // when
        final Optional<CharSource> firstAccess = loader.loadIfChanged();
        final Optional<CharSource> secondAccess = loader.loadIfChanged();

        // then
        assertThat(firstAccess).isNotEmpty();
        assertThat(secondAccess).isNotEmpty();
    }

    @Test
    public void providesValidCharSource() throws IOException {
        // given
        final String bucket = "bucket";
        final String key = "key";

        final String fileContent = "testing";
        final S3ObjectInputStream inputStream =
                new S3ObjectInputStream(new ByteArrayInputStream(fileContent.getBytes()), null);
        final S3FileLoader loader = new S3FileLoader(bucket, key, s3Client);

        when(s3Client.getObjectMetadata(bucket, key)).thenReturn(metadata);
        when(metadata.getETag()).thenReturn("etag-123");
        when(s3Client.getObject(bucket, key)).thenReturn(s3Object);
        when(s3Object.getObjectContent()).thenReturn(inputStream);

        // when
        final Optional<CharSource> source = loader.loadIfChanged();

        // then
        assertThat(source).isNotEmpty();
        if (source.isPresent()) {
            final CharSource charSource = source.get();
            assertThat(charSource).isInstanceOf(CharSource.class);
            assertThat(charSource.read()).isEqualTo(fileContent);
        }
    }

}