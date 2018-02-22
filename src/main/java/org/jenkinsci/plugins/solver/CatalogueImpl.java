package org.jenkinsci.plugins.solver;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * {@link Catalogue} implementation getting plugin metadata from update center.
 * Note: as update center (at time writing) don't publish metadata as separate artifacts, we have to download the full HPI to get it's MANIFEST.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class CatalogueImpl implements Catalogue {

    protected final File root;

    private Map<String, Plugin> map = new HashMap<>();

    public CatalogueImpl(File root) {
        this.root = root;
    }

    public Plugin findPlugin(String shortname, String version) throws IOException {

        final String key = shortname + ':' + version;
        Plugin plugin = map.get(key);
        if (plugin == null) {
            File f = new File(root, shortname + "/MANIFEST-" + version + ".MF");
            if (!f.exists()) {
                System.out.println("Downloading metadata for "+key);
                f.getParentFile().mkdirs();
                // TODO get metadata exposed in update center as a dedicated artifact

                String url = "https://updates.jenkins.io/download/plugins/" + shortname + "/" + version + "/" + shortname + ".hpi";
                HttpURLConnection conn;
                while (true) {

                    conn = (HttpURLConnection) new URL(url).openConnection();
                    switch (conn.getResponseCode()) {
                        case HttpURLConnection.HTTP_MOVED_PERM:
                        case HttpURLConnection.HTTP_MOVED_TEMP:
                            url = conn.getHeaderField("Location");
                            continue;
                    }
                    break;
                }

                try (InputStream in = conn.getInputStream();
                     ZipInputStream zin = new ZipInputStream(in);
                     OutputStream out = new FileOutputStream(f)) {
                    ZipEntry ze;
                    while ((ze = zin.getNextEntry()) != null) {
                        if (ze.getName().equals("META-INF/MANIFEST.MF")) {
                            byte[] buffer = new byte[9000];
                            int len;
                            while ((len = zin.read(buffer)) != -1) {
                                out.write(buffer, 0, len);
                            }
                            break;
                        }
                    }
                }

            }
            plugin = Plugin.fromManifest(new Manifest(FileUtils.openInputStream(f)), null);
            map.put(key, plugin);
        }
        return plugin;
    }


    public Plugin findPlugin(PluginSpec spec) throws IOException {
        return findPlugin(spec.shortName, spec.version.toString());
    }
}
