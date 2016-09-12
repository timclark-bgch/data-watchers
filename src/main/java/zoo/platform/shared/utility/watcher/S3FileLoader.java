package zoo.platform.shared.utility.watcher;

import com.amazonaws.services.s3.AmazonS3;
import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class S3FileLoader implements DataLoader {
    private final String bucket;
    private final String key;
    private final AmazonS3 s3client;

    private final AtomicReference<String> lastRead = new AtomicReference<>(null);

    public S3FileLoader(final String bucket, final String key, final AmazonS3 s3client) {
        this.bucket = bucket;
        this.key = key;
        this.s3client = s3client;
    }

    @Override
    public Optional<CharSource> loadIfChanged() throws IOException {
        final String currentEtag = etag();

        if (currentEtag.equals(lastRead.getAndSet(currentEtag))) {
            return Optional.empty();
        }

        return Optional.of(byteSource().asCharSource(Charsets.UTF_8));
    }

    private ByteSource byteSource() {
        return new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
                return s3client.getObject(bucket, key).getObjectContent();
            }
        };
    }

    private String etag() {
        return s3client.getObjectMetadata(bucket, key).getETag();
    }
}