/* Copyright (c) Jython Developers */
package org.python;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;

import org.python.core.CodeFlag;

/**
 * Jython version information.
 *
 * The version number and build information are loaded from the
 * version.properties file, located in this class file's
 * directory. version.properties is generated by ant.
 */
public class Version {

    /** The current version of Jython. */
    public static String PY_VERSION;

    /** Tokenized version. */
    public static int PY_MAJOR_VERSION;
    public static int PY_MINOR_VERSION;
    public static int PY_MICRO_VERSION;
    public static int PY_RELEASE_LEVEL;
    public static int PY_RELEASE_SERIAL;

    /** Timestamp of the current build. */
    public static String DATE;
    public static String TIME;

    /** Current Git branch {@code git name-rev --name-only HEAD}. */
    private static String GIT_BRANCH;

    /** Current Git tag {@code git describe --all --always --dirty}, e.g. 'tip'. */
    private static String GIT_TAG;

    /** Current Git global revision id {@code git rev-parse --short HEAD}. */
    private static String GIT_VERSION;

    /** The flags that are set by default in a code object. */
    private static final Collection<CodeFlag> defaultCodeFlags = Arrays.asList(
            CodeFlag.CO_NESTED, CodeFlag.CO_GENERATOR_ALLOWED, CodeFlag.CO_FUTURE_WITH_STATEMENT);

    static {
        loadProperties();
    }

    /**
     * Load the version information from the properties file.
     */
    private static void loadProperties() {
        boolean loaded = false;
        final String versionProperties = "/org/python/version.properties";
        InputStream in = Version.class.getResourceAsStream(versionProperties);
        if (in != null) {
            try {
                Properties properties = new Properties();
                properties.load(in);
                loaded = true;
                PY_VERSION = properties.getProperty("jython.version");
                PY_MAJOR_VERSION = Integer.valueOf(properties.getProperty("jython.major_version"));
                PY_MINOR_VERSION = Integer.valueOf(properties.getProperty("jython.minor_version"));
                PY_MICRO_VERSION = Integer.valueOf(properties.getProperty("jython.micro_version"));
                PY_RELEASE_LEVEL = Integer.valueOf(properties.getProperty("jython.release_level"));
                PY_RELEASE_SERIAL = Integer.valueOf(properties.getProperty("jython.release_serial"));
                DATE = properties.getProperty("jython.build.date");
                TIME = properties.getProperty("jython.build.time");
                GIT_BRANCH = properties.getProperty("jython.build.git_branch");
                GIT_TAG = properties.getProperty("jython.build.git_tag");
                GIT_VERSION = properties.getProperty("jython.build.git_version");
            } catch (IOException ioe) {
                System.err.println("There was a problem loading ".concat(versionProperties)
                        .concat(":"));
                ioe.printStackTrace();
            } finally {
                try {
                    in.close();
                } catch (IOException ioe) {
                    // ok
                }
            }
        }
        if (!loaded) {
            // fail with a meaningful exception (cannot use Py exceptions here)
            throw new RuntimeException("unable to load ".concat(versionProperties));
        }
    }

    /**
     * Return the current git version number. May be an empty string on environments that
     * can't determine it.
     */
    public static String getGitVersion() {
        return GIT_VERSION;
    }

    /**
     * Return the current git identifier name, either the current branch or tag.
     */
    public static String getGitIdentifier() {
        return "".equals(GIT_TAG) || "undefined".equals(GIT_TAG) ? GIT_BRANCH : GIT_TAG;
    }

    /**
     * Return the current build information, including revision and timestamp.
     */
    public static String getBuildInfo() {
        String revision = getGitVersion();
        String sep = "".equals(revision) ? "" : ":";
        String gitId = getGitIdentifier();
        return String.format("%s%s%s, %.20s, %.9s", gitId, sep, revision, DATE, TIME);
    }

    /**
     * Describe the current Java VM.
     */
    public static String getVM() {
        return String.format("[%s (%s)]", System.getProperty("java.vm.name"),
                             System.getProperty("java.vm.vendor"));
    }

    /**
     * Return the Python version, including compiler (or in our case,
     * the Java VM).
     */
    public static String getVersion() {
        return String.format("%.80s (%.80s)\n%.80s", PY_VERSION, getBuildInfo(), getVM());
    }

    public static Set<CodeFlag> getDefaultCodeFlags() {
        return EnumSet.copyOf(defaultCodeFlags);
    }
}
