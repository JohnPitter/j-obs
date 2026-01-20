package io.github.jobs.infrastructure;

import io.github.jobs.application.DependencyChecker;
import io.github.jobs.domain.Dependency;
import io.github.jobs.domain.DependencyCheckResult;
import io.github.jobs.domain.DependencyStatus;
import io.github.jobs.domain.KnownDependencies;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.Manifest;

/**
 * Checks for dependencies by attempting to load their marker classes from the classpath.
 * Results are cached to avoid repeated reflection overhead.
 */
public class ClasspathDependencyChecker implements DependencyChecker {

    private static final Duration DEFAULT_CACHE_DURATION = Duration.ofMinutes(5);

    private final Duration cacheDuration;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile DependencyCheckResult cachedResult;
    private volatile Instant cacheExpiry;

    public ClasspathDependencyChecker() {
        this(DEFAULT_CACHE_DURATION);
    }

    public ClasspathDependencyChecker(Duration cacheDuration) {
        this.cacheDuration = cacheDuration;
    }

    @Override
    public DependencyCheckResult check() {
        if (isCacheValid()) {
            return cachedResult;
        }

        lock.lock();
        try {
            // Double-check after acquiring lock
            if (isCacheValid()) {
                return cachedResult;
            }

            DependencyCheckResult result = performCheck();
            updateCache(result);
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public DependencyCheckResult checkFresh() {
        lock.lock();
        try {
            invalidateCache();
            DependencyCheckResult result = performCheck();
            updateCache(result);
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void invalidateCache() {
        this.cachedResult = null;
        this.cacheExpiry = null;
    }

    private boolean isCacheValid() {
        return cachedResult != null && cacheExpiry != null && Instant.now().isBefore(cacheExpiry);
    }

    private void updateCache(DependencyCheckResult result) {
        this.cachedResult = result;
        this.cacheExpiry = Instant.now().plus(cacheDuration);
    }

    private DependencyCheckResult performCheck() {
        List<DependencyStatus> statuses = KnownDependencies.all().stream()
                .map(this::checkDependency)
                .toList();

        return DependencyCheckResult.of(statuses);
    }

    private DependencyStatus checkDependency(Dependency dependency) {
        // Try all class names (primary first, then alternatives)
        for (String className : dependency.allClassNames()) {
            try {
                Class<?> clazz = Class.forName(className);
                String version = detectVersion(clazz, dependency).orElse(null);
                return DependencyStatus.found(dependency, version);
            } catch (ClassNotFoundException e) {
                // Continue to try next class name
            } catch (Exception e) {
                return DependencyStatus.error(dependency, e.getMessage());
            }
        }
        // None of the class names were found
        return DependencyStatus.notFound(dependency);
    }

    private Optional<String> detectVersion(Class<?> clazz, Dependency dependency) {
        // Try multiple strategies to detect version

        // Strategy 1: Package implementation version
        Package pkg = clazz.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return Optional.of(pkg.getImplementationVersion());
        }

        // Strategy 2: Check MANIFEST.MF
        Optional<String> manifestVersion = getManifestVersion(clazz);
        if (manifestVersion.isPresent()) {
            return manifestVersion;
        }

        // Strategy 3: Check for version.properties (common in some libraries)
        Optional<String> propsVersion = getPropertiesVersion(clazz, dependency);
        if (propsVersion.isPresent()) {
            return propsVersion;
        }

        return Optional.empty();
    }

    private Optional<String> getManifestVersion(Class<?> clazz) {
        try {
            String classPath = clazz.getName().replace('.', '/') + ".class";
            URL classUrl = clazz.getClassLoader().getResource(classPath);
            if (classUrl == null) {
                return Optional.empty();
            }

            String urlString = classUrl.toString();
            if (urlString.startsWith("jar:")) {
                String manifestPath = urlString.substring(0, urlString.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
                try (InputStream is = new URL(manifestPath).openStream()) {
                    Manifest manifest = new Manifest(is);
                    String version = manifest.getMainAttributes().getValue("Implementation-Version");
                    if (version == null) {
                        version = manifest.getMainAttributes().getValue("Bundle-Version");
                    }
                    return Optional.ofNullable(version);
                }
            }
        } catch (IOException e) {
            // Ignore and return empty
        }
        return Optional.empty();
    }

    private Optional<String> getPropertiesVersion(Class<?> clazz, Dependency dependency) {
        // Try common version properties file locations
        String[] possiblePaths = {
                "/META-INF/maven/" + dependency.groupId() + "/" + dependency.artifactId() + "/pom.properties",
                "/" + dependency.groupId().replace('.', '/') + "/version.properties"
        };

        for (String path : possiblePaths) {
            try (InputStream is = clazz.getResourceAsStream(path)) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    String version = props.getProperty("version");
                    if (version != null) {
                        return Optional.of(version);
                    }
                }
            } catch (IOException e) {
                // Ignore and try next
            }
        }

        return Optional.empty();
    }
}
