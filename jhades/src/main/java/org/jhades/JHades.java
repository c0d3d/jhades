package org.jhades;

import java.io.PrintStream;
import java.net.URL;
import java.util.List;
import org.jhades.model.ClasspathEntry;
import org.jhades.model.ClasspathResource;
import org.jhades.model.ClasspathResourceVersion;
import org.jhades.model.ClazzLoader;
import org.jhades.model.JarPair;
import org.jhades.reports.DuplicatesReport;
import org.jhades.service.ClasspathScanner;

/**
 *
 * See jHades documentation for how to use these commands - http://jhades.org
 *
 */
public class JHades {

    /**
     * A {@link PrintStream} that jHades will write all its output to.
     */
    private final PrintStream out;

    /**
     * Constructs jHades such that any report output will be written to {@code out}.
     * @param out The {@link PrintStream} to write to.
     */
    public JHades(PrintStream out) {
        this.out = out;
    }

    /**
     * Creates a jHades that will write its output to {@link System#out}.
     * @see #JHades(PrintStream)
     */
    public JHades() {
        this(System.out);
    }

    private ClasspathScanner scanner = new ClasspathScanner();

    public JHades printClassLoaderNames() {

        out.println("\n>> jHades printClassLoaders >> Printing classloader class names (ordered from child to parent):\n");

        List<ClazzLoader> classLoaders = scanner.findAllClassLoaders();
        boolean notSupportedFound = false;

        for (ClazzLoader classLoader : classLoaders) {
            if (classLoader.isSupported()) {
                out.println(classLoader.getName());
            } else {
                notSupportedFound = true;
                out.println(classLoader.getName() + " - NOT SUPORTED");
            }
        }
        endCommand(classLoaders.size() > 0);

        if (notSupportedFound) {
            out.println("Note: NOT SUPPORTED class loader means that any classes loaded by such a classloader will not be found on any jHades queries. \n");
        }

        out.flush();
        return this;
    }

    public JHades dumpClassloaderInfo() {

        out.println("\n>> jHades printClassLoaders >> Printing all classloader available info (from the class loader toString(), ordered from child to parent):\n");

        List<ClazzLoader> classLoaders = scanner.findAllClassLoaders();
        boolean notSupportedFound = false;

        for (ClazzLoader classLoader : classLoaders) {
            if (classLoader.isSupported()) {
                out.println("\n>>> Dumping available info for classloader " + classLoader.getName() + "\n");
                out.println(classLoader.getDetails());
            } else {
                notSupportedFound = true;
                out.println(classLoader.getName() + " - NOT SUPORTED");
            }
        }
        endCommand(classLoaders.size() > 0);

        if (notSupportedFound) {
            out.println("Note: NOT SUPPORTED class loader means that any classes loaded by such a classloader will not be found on any jHades queries. \n");
        }

        out.flush();
        return this;
    }

    public JHades printClasspath() {

        out.println("\n>> jHades printClasspath >> Printing all class folder and jars on the classpath:\n");

        List<ClasspathEntry> classpathEntries = scanner.findAllClasspathEntries();
        ClazzLoader clazzLoader = null;

        for (ClasspathEntry entry : classpathEntries) {
            if (entry.getClassLoader() != null && !entry.getClassLoader().equals(clazzLoader)) {
                out.println(); // line break between class loaders
                clazzLoader = entry.getClassLoader();
            }
            out.println(entry.getClassLoaderName() + " - " + entry.getUrl());
        }

        endCommand(classpathEntries.size() > 0);
        out.flush();
        return this;
    }

    public JHades findResource(String resource) {

        if (resource == null) {
            throw new IllegalArgumentException("Resource path cannot be null.");
        }

        out.println(">> jHades printResourcePath >> searching for " + resource + "\n");

        List<URL> allVersions = scanner.findAllResourceVersions(resource);
        boolean resultsFound = allVersions != null && allVersions.size() > 0;

        out.println("All versions:\n");
        for (URL version : allVersions) {
            out.println(version);
        }

        URL currentVersion = scanner.findCurrentResourceVersion(resource);

        if (resultsFound && currentVersion != null) {
            out.println("\nCurrent version being used: \n\n" + currentVersion);
        }

        endCommand(resultsFound);
        out.flush();
        return this;

    }

    public JHades findClassByName(String classFullyQualifiedName) {
        if (classFullyQualifiedName == null) {
            throw new IllegalArgumentException("Class name cannot be null.");
        }

        String resourceName = classFullyQualifiedName.replaceAll("\\.", "/") + ".class";

        return findResource(resourceName);
    }

    public JHades findClass(Class clazz) {

        if (clazz == null) {
            throw new IllegalArgumentException("Class name cannot be null.");
        }

        out.println(">> jHades searchClass >> Searching for class: " + clazz.getCanonicalName() + "\n");

        ClasspathResource foundClass = scanner.findClass(clazz);

        for (ClasspathResourceVersion version : foundClass.getResourceFileVersions()) {
            out.println(version.getClasspathEntry().getUrl() + foundClass.getName() + " size = " + version.getFileSize());
        }

        endCommand(foundClass != null);
        out.flush();
        return this;
    }

    public JHades findByRegex(String search) {

        if (search == null || search.isEmpty()) {
            throw new IllegalArgumentException("search string cannot be null or empty.");
        }

        out.println(">> jHades search >> Searching for resorce using search string: " + search + "\n");

        List<ClasspathResource> classpathResources = scanner.findByRegex(search);

        boolean resultsFound = classpathResources != null && classpathResources.size() > 0;

        if (resultsFound) {
            out.println("\nResults Found:\n");
            for (ClasspathResource classpathResource : classpathResources) {
                out.println(classpathResource.getName());
            }
        }

        endCommand(resultsFound);
        out.flush();
        return this;
    }

    public JHades multipleClassVersionsReport() {
        multipleClassVersionsReport(true);
        return this;
    }

    public JHades multipleClassVersionsReport(boolean excludeSameSizeDups) {
        List<ClasspathResource> resourcesWithDuplicates = scanner.findAllResourcesWithDuplicates(excludeSameSizeDups);

        DuplicatesReport report = new DuplicatesReport(resourcesWithDuplicates);
        report.print(out);

        out.flush();
        return this;
    }

    public JHades overlappingJarsReport() {
        out.println("\n>> jHades - scanning classpath for overlapping jars: \n");

        List<JarPair> jarOverlapReportLines = scanner.findOverlappingJars();

        for (JarPair jarOverlapReportLine : jarOverlapReportLines) {
            String reportLine = jarOverlapReportLine.getJar1().getUrl() + " overlaps with \n" + jarOverlapReportLine.getJar2().getUrl()
                    + " - total overlapping classes: " + jarOverlapReportLine.getDupClassesTotal() + " - ";
            if (jarOverlapReportLine.getJar1().getClassLoader().equals(jarOverlapReportLine.getJar2().getClassLoader())) {
                reportLine += "same classloader ! This is an ERROR!\n";
            } else {
                reportLine += "different classloaders.\n";
            }
            out.println(reportLine);
        }

        endCommand(jarOverlapReportLines.size() > 0);
        out.flush();
        return this;
    }

    private void endCommand(boolean resultsFound) {
        if (!resultsFound) {
            out.println("No results found.\n");
        } else {
            out.println("");
        }
        out.flush();
    }
}
